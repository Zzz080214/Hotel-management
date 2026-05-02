const { roomTypes } = require("../../data/mock");
const { getRoomList, normalizeRoom } = require("../../services/hotel");

Page({
  data: {
    tabs: [
      { key: "all", text: "全部" },
      { key: "hot", text: "热门" },
      { key: "steady", text: "商务" },
      { key: "luxury", text: "高端" },
      { key: "budget", text: "特惠" }
    ],
    activeTab: "all",
    dataSourceText: "本地模拟数据",
    roomTypes,
    visibleRooms: roomTypes
  },

  onShow() {
    this.loadRoomList();
  },

  async loadRoomList() {
    try {
      const result = await getRoomList();
      const roomList = (Array.isArray(result) ? result : []).map(normalizeRoom);
      this.applyFilter(this.data.activeTab, roomList);
      this.setData({
        roomTypes: roomList,
        dataSourceText: "后端接口数据"
      });
    } catch (error) {
      this.applyFilter(this.data.activeTab, roomTypes);
      this.setData({
        roomTypes,
        dataSourceText: "本地模拟数据"
      });
    }
  },

  switchTab(event) {
    const { key } = event.currentTarget.dataset;
    this.applyFilter(key, this.data.roomTypes);
  },

  applyFilter(key, sourceList) {
    const visibleRooms = key === "all"
      ? sourceList
      : sourceList.filter((item) => item.status === key);
    this.setData({
      activeTab: key,
      visibleRooms
    });
  },

  goBooking(event) {
    const { id } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/booking/booking?id=${id}` });
  }
});
