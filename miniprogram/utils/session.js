const USER_KEY = "wx_current_user";
const TOKEN_KEY = "token";
const DEVICE_KEY = "wx_demo_device_id";
const LEGACY_DEVICE_ID = "dev-local-device";

function createDevOpenid() {
  const randomPart = Math.random().toString(36).slice(2, 10);
  return `dev-${Date.now()}-${randomPart}`;
}

function getCurrentUser() {
  try {
    const user = wx.getStorageSync(USER_KEY) || null;
    if (user && user.openid === LEGACY_DEVICE_ID) {
      clearCurrentUser();
      return null;
    }
    return user;
  } catch (error) {
    return null;
  }
}

function saveWxSession(session) {
  const current = {
    token: session && session.token ? String(session.token) : "",
    openid: session && session.openid ? String(session.openid) : "",
    nickname: session && session.nickname ? String(session.nickname) : "微信用户",
    avatarUrl: session && session.avatarUrl ? String(session.avatarUrl) : ""
  };
  try {
    wx.setStorageSync(USER_KEY, current);
    wx.setStorageSync(TOKEN_KEY, current.token);
  } catch (error) {
    return;
  }
}

function clearCurrentUser() {
  try {
    wx.removeStorageSync(USER_KEY);
    wx.removeStorageSync(TOKEN_KEY);
  } catch (error) {
    return;
  }
}

function getCurrentOpenid() {
  const user = getCurrentUser();
  return user && user.openid ? user.openid : "";
}

function getOrCreateDeviceId() {
  try {
    const stored = wx.getStorageSync(DEVICE_KEY);
    if (stored && stored !== LEGACY_DEVICE_ID) {
      return stored;
    }
    const deviceId = createDevOpenid();
    wx.setStorageSync(DEVICE_KEY, deviceId);
    return deviceId;
  } catch (error) {
    return createDevOpenid();
  }
}

module.exports = {
  getCurrentUser,
  saveWxSession,
  clearCurrentUser,
  getCurrentOpenid,
  getOrCreateDeviceId
};
