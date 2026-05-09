const fs = require("fs");
const path = require("path");
const { performance } = require("perf_hooks");

const API_BASE_URL = process.env.API_BASE_URL || "http://127.0.0.1:8080/api";
const DURATION_MS = Number(process.env.DURATION_MS || 30000);
const CONCURRENCY = Number(process.env.CONCURRENCY || 40);
const REPORT_DIR = __dirname;
const STARTED_AT = new Date();

const endpoints = [
  { name: "小程序房型列表", method: "GET", path: "/wx/rooms", weight: 4 },
  { name: "小程序推荐房型", method: "GET", path: "/wx/rooms/recommend?limit=3", weight: 3 },
  { name: "小程序公告列表", method: "GET", path: "/wx/notices", weight: 2 },
  { name: "后台登录", method: "POST", path: "/auth/login", weight: 1, body: { username: "admin", password: "admin123" } },
  { name: "微信演示登录", method: "POST", path: "/wx/auth/login", weight: 1, body: () => ({
    code: `load-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    nickname: "压测用户",
    avatarUrl: "",
    devOpenid: `load-user-${Math.random().toString(36).slice(2, 10)}`
  }) }
];

let adminToken = "";
let stopAt = 0;
const results = [];

function weightedEndpoint() {
  const total = endpoints.reduce((sum, item) => sum + item.weight, 0);
  let cursor = Math.random() * total;
  for (const endpoint of endpoints) {
    cursor -= endpoint.weight;
    if (cursor <= 0) {
      return endpoint;
    }
  }
  return endpoints[0];
}

async function apiRequest(endpoint) {
  const started = performance.now();
  const headers = { "content-type": "application/json" };
  let body;

  if (endpoint.auth === "admin" && adminToken) {
    headers.Authorization = `Bearer ${adminToken}`;
  }
  if (endpoint.body) {
    body = JSON.stringify(typeof endpoint.body === "function" ? endpoint.body() : endpoint.body);
  }

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint.path}`, {
      method: endpoint.method,
      headers,
      body
    });
    const text = await response.text();
    const elapsed = performance.now() - started;
    let ok = response.ok;
    let message = "";
    try {
      const json = text ? JSON.parse(text) : null;
      if (json && typeof json === "object" && "code" in json) {
        ok = ok && (json.code === 200 || json.code === 0);
        message = json.message || "";
      }
    } catch (error) {
      message = "JSON parse failed";
      ok = false;
    }
    results.push({ name: endpoint.name, status: response.status, ok, elapsed, message });
  } catch (error) {
    results.push({ name: endpoint.name, status: 0, ok: false, elapsed: performance.now() - started, message: error.message });
  }
}

async function loginAdmin() {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ username: "admin", password: "admin123" })
  });
  const body = await response.json();
  adminToken = body && body.data && body.data.token ? body.data.token : "";
  if (!adminToken) {
    throw new Error("后台登录失败，无法获取 admin token");
  }
  endpoints.push(
    { name: "后台经营看板", method: "GET", path: "/admin/dashboard", weight: 3, auth: "admin" },
    { name: "后台订单列表", method: "GET", path: "/admin/orders", weight: 3, auth: "admin" }
  );
}

async function worker() {
  while (Date.now() < stopAt) {
    await apiRequest(weightedEndpoint());
  }
}

function percentile(values, p) {
  if (!values.length) {
    return 0;
  }
  const sorted = [...values].sort((a, b) => a - b);
  const index = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
}

function round(value, digits = 2) {
  return Number(value || 0).toFixed(digits);
}

function summarize() {
  const durationSec = (Date.now() - STARTED_AT.getTime()) / 1000;
  const groups = new Map();
  for (const item of results) {
    if (!groups.has(item.name)) {
      groups.set(item.name, []);
    }
    groups.get(item.name).push(item);
  }

  const endpointRows = Array.from(groups.entries()).map(([name, rows]) => {
    const durations = rows.map((item) => item.elapsed);
    const success = rows.filter((item) => item.ok).length;
    return {
      name,
      count: rows.length,
      success,
      failed: rows.length - success,
      successRate: rows.length ? success / rows.length : 0,
      avg: durations.reduce((sum, item) => sum + item, 0) / rows.length,
      min: Math.min(...durations),
      max: Math.max(...durations),
      p95: percentile(durations, 95),
      p99: percentile(durations, 99)
    };
  }).sort((a, b) => b.count - a.count);

  const allDurations = results.map((item) => item.elapsed);
  const success = results.filter((item) => item.ok).length;
  return {
    baseUrl: API_BASE_URL,
    startedAt: STARTED_AT.toLocaleString("zh-CN", { hour12: false }),
    durationSec,
    concurrency: CONCURRENCY,
    total: results.length,
    success,
    failed: results.length - success,
    successRate: results.length ? success / results.length : 0,
    qps: results.length / durationSec,
    avg: allDurations.reduce((sum, item) => sum + item, 0) / allDurations.length,
    min: Math.min(...allDurations),
    max: Math.max(...allDurations),
    p95: percentile(allDurations, 95),
    p99: percentile(allDurations, 99),
    endpoints: endpointRows
  };
}

function renderHtml(summary) {
  const rows = summary.endpoints.map((row) => `
      <tr>
        <td>${row.name}</td>
        <td>${row.count}</td>
        <td>${row.failed}</td>
        <td>${round(row.successRate * 100, 2)}%</td>
        <td>${round(row.avg)} ms</td>
        <td>${round(row.p95)} ms</td>
        <td>${round(row.p99)} ms</td>
        <td>${round(row.max)} ms</td>
      </tr>`).join("");

  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>悦栖酒店管理系统请求负载测试报告</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 34px;
      font-family: "Microsoft YaHei", "Segoe UI", sans-serif;
      color: #1e293b;
      background: #f5f7fb;
    }
    .report {
      max-width: 1180px;
      margin: 0 auto;
      background: #ffffff;
      border: 1px solid #dbe3ef;
      border-radius: 8px;
      box-shadow: 0 18px 46px rgba(15, 23, 42, 0.08);
      overflow: hidden;
    }
    .header {
      padding: 30px 34px 26px;
      background: linear-gradient(135deg, #0f172a 0%, #1f4f5f 52%, #c58b43 100%);
      color: white;
    }
    h1 { margin: 0 0 10px; font-size: 30px; letter-spacing: 0; }
    .meta { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 24px; color: rgba(255,255,255,.88); font-size: 14px; }
    .content { padding: 28px 34px 34px; }
    .kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 28px; }
    .kpi {
      padding: 18px;
      border: 1px solid #e3e9f3;
      border-radius: 8px;
      background: #fbfdff;
    }
    .kpi span { display: block; color: #64748b; font-size: 13px; margin-bottom: 7px; }
    .kpi strong { font-size: 25px; color: #0f172a; }
    .status-ok { color: #047857; }
    .status-bad { color: #b91c1c; }
    table {
      width: 100%;
      border-collapse: collapse;
      border: 1px solid #dce4ef;
      overflow: hidden;
      border-radius: 8px;
    }
    th, td {
      padding: 13px 14px;
      border-bottom: 1px solid #e6edf5;
      text-align: left;
      font-size: 14px;
      white-space: nowrap;
    }
    th { background: #eef4fb; color: #334155; font-weight: 700; }
    tr:last-child td { border-bottom: 0; }
    .note {
      margin-top: 18px;
      padding: 14px 16px;
      border-left: 4px solid #c58b43;
      background: #fff8ed;
      color: #5f3f18;
      font-size: 14px;
      line-height: 1.7;
    }
  </style>
</head>
<body>
  <main class="report">
    <section class="header">
      <h1>悦栖酒店管理系统请求负载测试报告</h1>
      <div class="meta">
        <div>测试地址：${summary.baseUrl}</div>
        <div>开始时间：${summary.startedAt}</div>
        <div>并发用户：${summary.concurrency}</div>
        <div>持续时间：${round(summary.durationSec, 1)} 秒</div>
      </div>
    </section>
    <section class="content">
      <div class="kpis">
        <div class="kpi"><span>总请求数</span><strong>${summary.total}</strong></div>
        <div class="kpi"><span>吞吐量</span><strong>${round(summary.qps)} req/s</strong></div>
        <div class="kpi"><span>成功率</span><strong class="${summary.failed ? "status-bad" : "status-ok"}">${round(summary.successRate * 100, 2)}%</strong></div>
        <div class="kpi"><span>P95 响应时间</span><strong>${round(summary.p95)} ms</strong></div>
        <div class="kpi"><span>平均响应时间</span><strong>${round(summary.avg)} ms</strong></div>
        <div class="kpi"><span>P99 响应时间</span><strong>${round(summary.p99)} ms</strong></div>
        <div class="kpi"><span>最大响应时间</span><strong>${round(summary.max)} ms</strong></div>
        <div class="kpi"><span>失败请求数</span><strong class="${summary.failed ? "status-bad" : "status-ok"}">${summary.failed}</strong></div>
      </div>

      <table>
        <thead>
          <tr>
            <th>接口场景</th>
            <th>请求数</th>
            <th>失败数</th>
            <th>成功率</th>
            <th>平均耗时</th>
            <th>P95</th>
            <th>P99</th>
            <th>最大耗时</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>

      <div class="note">
        本次压测覆盖小程序房型/公告、微信演示登录、后台登录、后台经营看板、后台订单列表等核心请求链路；只读接口为主，避免批量消耗客房库存。
      </div>
    </section>
  </main>
</body>
</html>`;
}

async function main() {
  console.log(`API_BASE_URL=${API_BASE_URL}`);
  console.log(`CONCURRENCY=${CONCURRENCY}, DURATION_MS=${DURATION_MS}`);
  await loginAdmin();
  stopAt = Date.now() + DURATION_MS;
  await Promise.all(Array.from({ length: CONCURRENCY }, () => worker()));
  const summary = summarize();
  const jsonPath = path.join(REPORT_DIR, "request-load-test-result.json");
  const htmlPath = path.join(REPORT_DIR, "request-load-test-report.html");
  fs.writeFileSync(jsonPath, JSON.stringify(summary, null, 2), "utf8");
  fs.writeFileSync(htmlPath, renderHtml(summary), "utf8");
  console.table(summary.endpoints.map((row) => ({
    name: row.name,
    count: row.count,
    failed: row.failed,
    successRate: `${round(row.successRate * 100, 2)}%`,
    avgMs: round(row.avg),
    p95Ms: round(row.p95),
    p99Ms: round(row.p99)
  })));
  console.log(`TOTAL=${summary.total}, FAILED=${summary.failed}, QPS=${round(summary.qps)}, P95=${round(summary.p95)}ms`);
  console.log(`REPORT_HTML=${htmlPath}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
