const { clearCurrentUser } = require("./session");

function getBaseUrl() {
  const app = getApp && getApp();
  return (app && app.globalData && app.globalData.apiBaseUrl) || "http://127.0.0.1:8080/api";
}

function isRemoteApiEnabled() {
  const app = getApp && getApp();
  return !!(app && app.globalData && app.globalData.enableRemoteApi);
}

function resolveUrl(url) {
  if (/^https?:\/\//.test(url)) {
    return url;
  }
  return `${getBaseUrl()}${url}`;
}

function unwrapResponse(body) {
  if (!body || typeof body !== "object") {
    return body;
  }

  if (!Object.prototype.hasOwnProperty.call(body, "code")) {
    return body.data !== undefined ? body.data : body;
  }

  if (body.code === 200 || body.code === 0) {
    if (body.data !== undefined) {
      return body.data;
    }
    if (body.result !== undefined) {
      return body.result;
    }
    return body;
  }

  if (body.code === 401) {
    clearCurrentUser();
    throw new Error(body.message || "登录已过期，请重新登录");
  }

  throw new Error(body.message || "接口请求失败");
}

function request(options) {
  const {
    url,
    method = "GET",
    data,
    header = {},
    auth = false
  } = options;

  return new Promise((resolve, reject) => {
    if (!isRemoteApiEnabled()) {
      reject(new Error("Remote API disabled"));
      return;
    }

    const token = wx.getStorageSync("token");
    const requestHeader = {
      "content-type": "application/json",
      ...header
    };

    if (auth && token) {
      requestHeader.Authorization = token.startsWith("Bearer ") ? token : `Bearer ${token}`;
    }

    wx.request({
      url: resolveUrl(url),
      method,
      data,
      header: requestHeader,
      timeout: 8000,
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            resolve(unwrapResponse(res.data));
          } catch (error) {
            reject(error);
          }
          return;
        }

        try {
          unwrapResponse(res.data);
        } catch (error) {
          reject(error);
          return;
        }

        reject(new Error(`HTTP ${res.statusCode}`));
      },
      fail(error) {
        reject(error);
      }
    });
  });
}

module.exports = {
  request
};
