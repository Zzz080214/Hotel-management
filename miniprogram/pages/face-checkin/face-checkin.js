const { faceCheckInOrder, normalizeOrder } = require("../../services/hotel");
const { upsertLocalOrder } = require("../../data/mock");
const { getCurrentOpenid, getOrCreateDeviceId } = require("../../utils/session");

const PROFILE_PENDING_PANEL_KEY = "profile_pending_panel";

function makeDemoRoomNo(orderId) {
  const seed = String(orderId || Date.now());
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) {
    hash = ((hash * 33) + seed.charCodeAt(i)) % 900;
  }
  return String(8100 + hash);
}

function decodeOption(value) {
  const text = value === undefined || value === null ? "" : String(value);
  try {
    return decodeURIComponent(text);
  } catch (error) {
    return text;
  }
}

function buildDemoStayOrder(orderId, sourceOrder = {}) {
  const roomNo = sourceOrder.roomNo || makeDemoRoomNo(orderId);
  return {
    id: sourceOrder.id || orderId,
    wxOpenid: getCurrentOpenid() || getOrCreateDeviceId(),
    guest: sourceOrder.guest || "住客",
    roomName: sourceOrder.roomName || "房型待定",
    amount: sourceOrder.amount || 0,
    date: sourceOrder.date || "今日入住",
    roomNo,
    paymentStatus: sourceOrder.paymentStatus || "paid",
    paymentStatusText: sourceOrder.paymentStatusText || "已支付",
    status: "staying",
    statusText: "在住中"
  };
}

Page({
  data: {
    orderId: "",
    orderGuest: "",
    orderRoomName: "",
    orderAmount: 0,
    orderDate: "",
    status: "ready",
    statusText: "请将面部置于识别框内",
    cameraReady: false,
    faceDetected: false,
    comparing: false,
    resultOrder: null,
    roomNo: "",
    failText: ""
  },

  authTimer: null,

  onLoad(options) {
    this.setData({
      orderId: decodeOption(options.id),
      orderGuest: decodeOption(options.guest),
      orderRoomName: decodeOption(options.roomName),
      orderAmount: Number(options.amount || 0),
      orderDate: decodeOption(options.date)
    });
  },

  onUnload() {
    this.stopAuthTimer();
  },

  onCameraReady() {
    this.setData({
      cameraReady: true
    });

    if (this.data.status === "scanning") {
      this.startAuthCountdown();
    }
  },

  onCameraError() {
    this.stopAuthTimer();
    this.setData({
      status: "failed",
      statusText: "摄像头启动失败",
      cameraReady: false,
      faceDetected: false,
      comparing: false,
      failText: "请检查摄像头权限后重试"
    });
  },

  startScan() {
    if (!this.data.orderId) {
      wx.showToast({ title: "订单号缺失", icon: "none" });
      return;
    }
    this.stopAuthTimer();
    this.setData({
      status: "scanning",
      statusText: this.data.cameraReady ? "摄像头已启动，正在采集人脸信息" : "正在启动摄像头，请保持正对屏幕",
      faceDetected: this.data.cameraReady,
      comparing: false,
      failText: ""
    });

    if (this.data.cameraReady) {
      this.startAuthCountdown();
    }
  },

  startAuthCountdown() {
    this.stopAuthTimer();
    this.setData({
      status: "scanning",
      statusText: "摄像头已启动，正在采集人脸信息",
      faceDetected: true,
      comparing: true
    });

    this.authTimer = setTimeout(() => {
      this.authTimer = null;
      this.completeFaceAuth();
    }, 4000);
  },

  completeFaceAuth() {
    this.setData({
      status: "success",
      statusText: "认证成功，已办理入住，可生成房间密码",
      comparing: true
    });

    const stayOrder = buildDemoStayOrder(this.data.orderId, {
      guest: this.data.orderGuest,
      roomName: this.data.orderRoomName,
      amount: this.data.orderAmount,
      date: this.data.orderDate
    });
    upsertLocalOrder(stayOrder);

    this.setData({
      status: "success",
      statusText: "认证成功，已办理入住，可生成房间密码",
      comparing: false,
      resultOrder: stayOrder,
      roomNo: stayOrder.roomNo
    });
    wx.showModal({
      title: "认证成功",
      content: "人脸识别已通过，订单已进入在住中。现在可以生成房间动态密码或自助退房。",
      confirmText: "生成密码",
      cancelText: "先看看",
      success: (res) => {
        if (res.confirm) {
          this.goProfilePanel("password");
        }
      }
    });

    this.syncRemoteCheckIn();
  },

  async syncRemoteCheckIn() {
    let remoteOrder = null;
    try {
      const result = await faceCheckInOrder(this.data.orderId);
      remoteOrder = normalizeOrder(result);
    } catch (error) {
      remoteOrder = null;
    }

    if (!remoteOrder || this.data.status !== "success") {
      return;
    }

    const stayOrder = buildDemoStayOrder(this.data.orderId, {
      ...remoteOrder,
      guest: remoteOrder.guest || this.data.orderGuest,
      roomName: remoteOrder.roomName || this.data.orderRoomName,
      amount: remoteOrder.amount || this.data.orderAmount,
      date: remoteOrder.date || this.data.orderDate
    });
    upsertLocalOrder(stayOrder);

    this.setData({
      resultOrder: stayOrder,
      roomNo: stayOrder.roomNo
    });
  },

  retryScan() {
    this.startScan();
  },

  handlePrimaryAction() {
    this.startScan();
  },

  goOrders() {
    wx.switchTab({
      url: "/pages/orders/orders"
    });
  },

  goPassword() {
    this.goProfilePanel("password");
  },

  goCheckout() {
    this.goProfilePanel("checkout");
  },

  goProfilePanel(panel) {
    try {
      wx.setStorageSync(PROFILE_PENDING_PANEL_KEY, panel);
    } catch (error) {
    }
    wx.switchTab({
      url: "/pages/profile/profile"
    });
  },

  stopAuthTimer() {
    if (this.authTimer) {
      clearTimeout(this.authTimer);
      this.authTimer = null;
    }
  }
});
