const fs = require("fs");
const path = require("path");

const [jtlPath, htmlPath, jsonPath] = process.argv.slice(2);

if (!jtlPath || !htmlPath || !jsonPath) {
  console.error("Usage: node render-jmeter-report.js <result.jtl> <report.html> <summary.json>");
  process.exit(1);
}

function parseCsvLine(line) {
  const values = [];
  let current = "";
  let inQuotes = false;

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const next = line[index + 1];

    if (char === '"' && inQuotes && next === '"') {
      current += '"';
      index += 1;
    } else if (char === '"') {
      inQuotes = !inQuotes;
    } else if (char === "," && !inQuotes) {
      values.push(current);
      current = "";
    } else {
      current += char;
    }
  }
  values.push(current);
  return values;
}

function percentile(values, p) {
  if (!values.length) {
    return 0;
  }
  const sorted = [...values].sort((a, b) => a - b);
  const index = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
}

function average(values) {
  if (!values.length) {
    return 0;
  }
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function formatNumber(value, digits = 2) {
  return Number(value || 0).toFixed(digits);
}

function formatInt(value) {
  return Math.round(Number(value || 0)).toString();
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

const raw = fs.readFileSync(jtlPath, "utf8").trim();
const lines = raw.split(/\r?\n/).filter(Boolean);
if (lines.length < 2) {
  throw new Error(`JTL result file has no sample rows: ${jtlPath}`);
}

const headers = parseCsvLine(lines[0]);
const index = Object.fromEntries(headers.map((header, position) => [header, position]));
const samples = lines.slice(1).map((line) => {
  const row = parseCsvLine(line);
  return {
    timeStamp: Number(row[index.timeStamp]),
    elapsed: Number(row[index.elapsed]),
    label: row[index.label],
    responseCode: row[index.responseCode],
    success: row[index.success] === "true",
    bytes: Number(row[index.bytes] || 0),
    sentBytes: Number(row[index.sentBytes] || 0),
    latency: Number(row[index.Latency] || 0),
    connect: Number(row[index.Connect] || 0)
  };
});

const firstTime = Math.min(...samples.map((sample) => sample.timeStamp));
const lastTime = Math.max(...samples.map((sample) => sample.timeStamp + sample.elapsed));
const durationSec = Math.max((lastTime - firstTime) / 1000, 0.001);
const groups = new Map();

for (const sample of samples) {
  if (!groups.has(sample.label)) {
    groups.set(sample.label, []);
  }
  groups.get(sample.label).push(sample);
}

function summarize(label, rows) {
  const elapsed = rows.map((row) => row.elapsed);
  const failed = rows.filter((row) => !row.success).length;
  const bytes = rows.reduce((sum, row) => sum + row.bytes, 0);
  const sentBytes = rows.reduce((sum, row) => sum + row.sentBytes, 0);
  return {
    label,
    samples: rows.length,
    average: average(elapsed),
    median: percentile(elapsed, 50),
    line90: percentile(elapsed, 90),
    line95: percentile(elapsed, 95),
    line99: percentile(elapsed, 99),
    min: Math.min(...elapsed),
    max: Math.max(...elapsed),
    failed,
    errorRate: failed / rows.length,
    throughput: rows.length / durationSec,
    receivedKbSec: bytes / 1024 / durationSec,
    sentKbSec: sentBytes / 1024 / durationSec
  };
}

const endpointRows = Array.from(groups.entries())
  .map(([label, rows]) => summarize(label, rows))
  .sort((a, b) => a.label.localeCompare(b.label, "zh-CN"));

const total = summarize("总体", samples);
const summary = {
  tool: process.env.JMETER_VERSION || "Apache JMeter 5.6.3",
  baseUrl: process.env.BASE_URL || "http://127.0.0.1:8080",
  threads: Number(process.env.THREADS || 50),
  loops: Number(process.env.LOOPS || 20),
  rampTime: Number(process.env.RAMP_TIME || 10),
  startedAt: new Date(firstTime).toLocaleString("zh-CN", { hour12: false }),
  durationSec,
  total,
  endpoints: endpointRows
};

const tableRows = endpointRows.map((row) => `
          <tr>
            <td>${escapeHtml(row.label)}</td>
            <td>${row.samples}</td>
            <td>${formatInt(row.average)}</td>
            <td>${formatInt(row.median)}</td>
            <td>${formatInt(row.line90)}</td>
            <td>${formatInt(row.line95)}</td>
            <td>${formatInt(row.line99)}</td>
            <td>${formatInt(row.min)}</td>
            <td>${formatInt(row.max)}</td>
            <td class="${row.failed ? "bad" : "ok"}">${formatNumber(row.errorRate * 100, 2)}%</td>
            <td>${formatNumber(row.throughput)}</td>
          </tr>`).join("");

const html = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>Apache JMeter 性能测试聚合报告</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 30px;
      font-family: "Microsoft YaHei", "Segoe UI", Arial, sans-serif;
      color: #1f2937;
      background: #eef3f8;
    }
    .report {
      max-width: 1240px;
      margin: 0 auto;
      background: #fff;
      border: 1px solid #cfd8e3;
      border-radius: 8px;
      box-shadow: 0 14px 34px rgba(15, 23, 42, .12);
      overflow: hidden;
    }
    .header {
      padding: 22px 28px 20px;
      color: #fff;
      background: #21364d;
      border-bottom: 4px solid #c9954a;
    }
    h1 {
      margin: 0 0 12px;
      font-size: 28px;
      font-weight: 700;
      letter-spacing: 0;
    }
    .meta {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px 22px;
      color: rgba(255,255,255,.9);
      font-size: 14px;
    }
    .body {
      padding: 24px 28px 28px;
    }
    .kpis {
      display: grid;
      grid-template-columns: repeat(6, 1fr);
      gap: 12px;
      margin-bottom: 22px;
    }
    .kpi {
      min-height: 86px;
      padding: 14px 14px 12px;
      border: 1px solid #d7e0eb;
      border-radius: 8px;
      background: #f8fbfe;
    }
    .kpi span {
      display: block;
      margin-bottom: 7px;
      color: #64748b;
      font-size: 13px;
      white-space: nowrap;
    }
    .kpi strong {
      display: block;
      color: #111827;
      font-size: 23px;
      line-height: 1.2;
      white-space: nowrap;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      border: 1px solid #cfd8e3;
      font-size: 13px;
    }
    th {
      padding: 10px 9px;
      color: #24384e;
      background: #e7edf5;
      border-bottom: 1px solid #cfd8e3;
      text-align: right;
      white-space: nowrap;
    }
    th:first-child,
    td:first-child {
      text-align: left;
    }
    td {
      padding: 10px 9px;
      border-bottom: 1px solid #e3e9f0;
      text-align: right;
      white-space: nowrap;
    }
    tr:last-child td { border-bottom: 0; }
    .ok { color: #047857; font-weight: 700; }
    .bad { color: #b91c1c; font-weight: 700; }
    .note {
      margin-top: 16px;
      padding: 12px 14px;
      color: #4b5563;
      background: #f7fafc;
      border: 1px solid #dbe4ee;
      border-radius: 8px;
      font-size: 13px;
      line-height: 1.7;
    }
  </style>
</head>
<body>
  <main class="report">
    <section class="header">
      <h1>Apache JMeter 性能测试聚合报告</h1>
      <div class="meta">
        <div>系统名称：悦栖酒店管理系统</div>
        <div>测试工具：${escapeHtml(summary.tool)}</div>
        <div>测试地址：${escapeHtml(summary.baseUrl)}</div>
        <div>并发用户数：${summary.threads}</div>
        <div>循环次数：${summary.loops}</div>
        <div>启动时间：${summary.startedAt}</div>
      </div>
    </section>
    <section class="body">
      <div class="kpis">
        <div class="kpi"><span>总请求数</span><strong>${total.samples}</strong></div>
        <div class="kpi"><span>平均响应时间</span><strong>${formatInt(total.average)} ms</strong></div>
        <div class="kpi"><span>95% 响应时间</span><strong>${formatInt(total.line95)} ms</strong></div>
        <div class="kpi"><span>最大响应时间</span><strong>${formatInt(total.max)} ms</strong></div>
        <div class="kpi"><span>吞吐量</span><strong>${formatNumber(total.throughput)} /s</strong></div>
        <div class="kpi"><span>错误率</span><strong class="${total.failed ? "bad" : "ok"}">${formatNumber(total.errorRate * 100, 2)}%</strong></div>
      </div>

      <table>
        <thead>
          <tr>
            <th>Label</th>
            <th># Samples</th>
            <th>Average</th>
            <th>Median</th>
            <th>90% Line</th>
            <th>95% Line</th>
            <th>99% Line</th>
            <th>Min</th>
            <th>Max</th>
            <th>Error %</th>
            <th>Throughput</th>
          </tr>
        </thead>
        <tbody>${tableRows}</tbody>
      </table>

      <div class="note">
        本次测试模拟多个 Web 客户端并发访问，覆盖后台登录、小程序客房查询、小程序公告查询、后台经营看板和后台订单管理等核心接口。结果统计来源于 JMeter JTL 文件，可用于毕业设计论文中的性能测试章节。
      </div>
    </section>
  </main>
</body>
</html>`;

fs.mkdirSync(path.dirname(htmlPath), { recursive: true });
fs.writeFileSync(jsonPath, JSON.stringify(summary, null, 2), "utf8");
fs.writeFileSync(htmlPath, html, "utf8");

console.log(JSON.stringify({
  samples: total.samples,
  failed: total.failed,
  errorRate: total.errorRate,
  average: total.average,
  p95: total.line95,
  max: total.max,
  throughput: total.throughput,
  htmlPath,
  jsonPath
}, null, 2));
