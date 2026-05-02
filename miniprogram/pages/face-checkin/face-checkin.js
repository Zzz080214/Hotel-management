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
    progress: 0,
    faceDetected: false,
    comparing: false,
    resultOrder: null,
    roomNo: "",
    failText: ""
  },

  scanTimer: null,

  onLoad(options) {
    this.setData({
      orderId: options.id || "",
      orderGuest: options.guest || "",
      orderRoomName: options.roomName || "",
      orderAmount: Number(options.amount || 0),
      orderDate: options.date || ""
    });
  },

  onUnload() {
    this.stopScanTimer();
  },

  startScan() {
    if (!this.data.orderId) {
      wx.showToast({ title: "订单号缺失", icon: "none" });
      return;
    }
    this.stopScanTimer();
    this.setData({
      status: "scanning",
      statusText: "正在检测人脸轮廓",
      progress: 0,
      faceDetected: false,
      comparing: false,
      failText: ""
    });

    this.scanTimer = setInterval(() => {
      const nextProgress = Math.min(100, this.data.progress + 8);
      const faceDetected = nextProgress >= 40;
      this.setData({
        progress: nextProgress,
        faceDetected,
        statusText: faceDetected ? "已检测到人脸，正在采集特征" : "正在检测人脸轮廓"
      });
      if (nextProgress >= 100) {
        this.stopScanTimer();
        this.compareFace();
      }
    }, 260);
  },

  async compareFace() {
    this.setData({
      status: "comparing",
      statusText: "正在模拟公安联网核验",
      comparing: true
    });

    let remoteOrder = null;
    try {
      const result = await faceCheckInOrder(this.data.orderId);
      remoteOrder = normalizeOrder(result);
    } catch (error) {
      remoteOrder = null;
    }

    const stayOrder = buildDemoStayOrder(this.data.orderId, {
      ...(remoteOrder || {}),
      guest: (remoteOrder && remoteOrder.guest) || this.data.orderGuest,
      roomName: (remoteOrder && remoteOrder.roomName) || this.data.orderRoomName,
      amount: (remoteOrder && remoteOrder.amount) || this.data.orderAmount,
      date: (remoteOrder && remoteOrder.date) || this.data.orderDate
    });
    upsertLocalOrder(stayOrder);

    this.setData({
      status: "success",
      statusText: "识别成功，已办理入住，可生成房间密码",
      comparing: false,
      resultOrder: stayOrder,
      roomNo: stayOrder.roomNo
    });
    wx.showModal({
      title: "入住成功",
      content: "人脸识别已通过，订单已进入在住中。现在可以生成房间动态密码或自助退房。",
      confirmText: "生成密码",
      cancelText: "先看看",
      success: (res) => {
        if (res.confirm) {
          this.goProfilePanel("password");
        }
      }
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

  stopScanTimer() {
    if (this.scanTimer) {
      clearInterval(this.scanTimer);
      this.scanTimer = null;
    }
  }
});
