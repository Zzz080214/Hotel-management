const { getMergedOrders } = require("../../data/mock");
const { getMyOrders, normalizeOrder } = require("../../services/hotel");
const { getCurrentUser, getCurrentOpenid, getOrCreateDeviceId } = require("../../utils/session");

function mergeOrderList(primaryOrders, localOrders) {
  const map = new Map();
  primaryOrders.forEach((item) => map.set(String(item.id), item));
  localOrders.forEach((item) => map.set(String(item.id), item));
  return Array.from(map.values());
}

Page({
  data: {
    tabs: [
      { key: "all", text: "全部" },
      { key: "upcoming", text: "待入住" },
      { key: "staying", text: "在住中" },
      { key: "finished", text: "已完成" }
    ],
    activeTab: "all",
    dataSourceText: "本地模拟数据",
    currentUser: null,
    orders: [],
    visibleOrders: []
  },

  onShow() {
    this.loadOrders();
  },

  async loadOrders() {
    const currentUser = getCurrentUser();
    const currentOpenid = getCurrentOpenid();
    const localOrders = [currentOpenid, getOrCreateDeviceId()]
      .filter(Boolean)
      .flatMap((openid, index, all) => all.indexOf(openid) === index ? getMergedOrders(openid) : [])
      .map(normalizeOrder);
    if (!currentOpenid) {
      this.applyFilter(this.data.activeTab, localOrders);
      this.setData({
        currentUser: null,
        orders: localOrders,
        dataSourceText: localOrders.length ? "本地演示数据" : "请先到“我的”页面登录后查看订单"
      });
      return;
    }

    try {
      const result = await getMyOrders(this.data.activeTab);
      const list = Array.isArray(result)
        ? result
        : Array.isArray(result.records)
          ? result.records
          : [];
      const backendOrders = list.map(normalizeOrder);
      const orders = mergeOrderList(backendOrders, localOrders);
      this.applyFilter(this.data.activeTab, orders);
      this.setData({
        currentUser,
        orders,
        dataSourceText: "后端接口数据"
      });
    } catch (error) {
      const orders = localOrders;
      this.applyFilter(this.data.activeTab, orders);
      this.setData({
        currentUser,
        orders,
        dataSourceText: "本地模拟数据"
      });
    }
  },

  switchTab(event) {
    const { key } = event.currentTarget.dataset;
    this.applyFilter(key, this.data.orders);
  },

  applyFilter(key, sourceList) {
    const visibleOrders = key === "all"
      ? sourceList
      : sourceList.filter((item) => item.status === key);
    this.setData({
      activeTab: key,
      visibleOrders
    });
  },

  showOrderDetail(event) {
    const { id } = event.currentTarget.dataset;
    wx.showModal({
      title: "订单详情",
      content: `这里可以继续接订单详情页。\n当前订单号：${id}`,
      showCancel: false
    });
  },

  startFaceCheckin(event) {
    const { id, guest, roomName, amount, date } = event.currentTarget.dataset;
    const query = [
      `id=${encodeURIComponent(id)}`,
      `guest=${encodeURIComponent(guest || "")}`,
      `roomName=${encodeURIComponent(roomName || "")}`,
      `amount=${encodeURIComponent(amount || 0)}`,
      `date=${encodeURIComponent(date || "")}`
    ].join("&");
    wx.navigateTo({
      url: `/pages/face-checkin/face-checkin?${query}`
    });
  }
});
