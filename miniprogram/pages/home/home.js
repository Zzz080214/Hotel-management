const { roomTypes, notices, homeStats } = require("../../data/mock");
const {
  getRecommendRooms,
  getNoticeList,
  normalizeRoom,
  normalizeNotice
} = require("../../services/hotel");

Page({
  data: {
    heroStats: homeStats,
    roomTypes: roomTypes.slice(0, 3),
    notices,
    dataSourceText: "本地模拟数据",
    quickActions: [
      { title: "全部房型", subtitle: "看价格与库存", path: "/pages/room-list/room-list" },
      { title: "立即订房", subtitle: "快捷下单", path: "/pages/room-list/room-list" },
      { title: "联系客服", subtitle: "咨询入住", action: "phone" }
    ]
  },

  onShow() {
    this.loadHomeData();
  },

  async loadHomeData() {
    try {
      const [roomResult, noticeResult] = await Promise.all([
        getRecommendRooms(3),
        getNoticeList()
      ]);
      const normalizedRooms = (Array.isArray(roomResult) ? roomResult : []).map(normalizeRoom);
      const normalizedNotices = (Array.isArray(noticeResult) ? noticeResult : []).map(normalizeNotice);
      this.setData({
        roomTypes: normalizedRooms.slice(0, 3),
        notices: normalizedNotices,
        dataSourceText: "后端接口数据"
      });
    } catch (error) {
      this.setData({
        roomTypes: roomTypes.slice(0, 3),
        notices,
        dataSourceText: "本地模拟数据"
      });
    }
  },

  handleQuickAction(event) {
    const { path, action } = event.currentTarget.dataset;
    if (action === "phone") {
      wx.showModal({
        title: "联系客服",
        content: "前台电话：400-820-2026",
        showCancel: false
      });
      return;
    }
    wx.navigateTo({ url: path });
  },

  goRoomList() {
    wx.navigateTo({ url: "/pages/room-list/room-list" });
  },

  goBooking(event) {
    const { id } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/booking/booking?id=${id}` });
  }
});
