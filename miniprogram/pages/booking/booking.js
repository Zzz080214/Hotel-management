const { getRoomById } = require("../../data/mock");
const { getRoomDetail, createOrder, normalizeRoom } = require("../../services/hotel");
const { getCurrentUser } = require("../../utils/session");

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

Page({
  data: {
    room: null,
    stayNights: 1,
    totalPrice: 0,
    contactName: "",
    contactPhone: "",
    dataSourceText: "本地模拟数据"
  },

  onLoad(options) {
    const currentUser = getCurrentUser();
    if (currentUser) {
      this.setData({
        contactName: currentUser.nickname || "微信用户",
        contactPhone: ""
      });
    }
    this.loadRoomDetail(options.id);
  },

  async loadRoomDetail(id) {
    try {
      const room = normalizeRoom(await getRoomDetail(id));
      this.setData({
        room,
        totalPrice: room.price,
        dataSourceText: "后端接口数据"
      });
    } catch (error) {
      const room = getRoomById(id);
      this.setData({
        room,
        totalPrice: room.price,
        dataSourceText: "本地模拟数据"
      });
    }
  },

  handleInput(event) {
    const { field } = event.currentTarget.dataset;
    this.setData({
      [field]: event.detail.value
    });
  },

  handleNightChange(event) {
    const stayNights = Number(event.detail.value) + 1;
    this.setData({
      stayNights,
      totalPrice: this.data.room.price * stayNights
    });
  },

  async submitOrder() {
    const currentUser = getCurrentUser();
    if (!currentUser || !currentUser.token || !currentUser.openid) {
      wx.showToast({
        title: "请先到“我的”页面登录",
        icon: "none"
      });
      return;
    }

    if (!this.data.contactName || !this.data.contactPhone) {
      wx.showToast({
        title: "请先填写联系人信息",
        icon: "none"
      });
      return;
    }

    const today = new Date();
    const checkInDate = formatDate(today);
    const checkOutDate = formatDate(new Date(today.getTime() + this.data.stayNights * 24 * 60 * 60 * 1000));
    const payload = {
      roomTypeId: this.data.room.id,
      roomTypeName: this.data.room.name,
      guestName: this.data.contactName,
      guestPhone: this.data.contactPhone,
      wxOpenid: currentUser.openid,
      userPhone: currentUser.openid,
      stayNights: this.data.stayNights,
      totalAmount: this.data.totalPrice,
      checkInDate,
      checkOutDate
    };

    try {
      const order = await createOrder(payload);
      wx.showModal({
        title: "预订成功",
        content: `订单号：${order.id}\n已同步到酒店后台预订界面。`,
        showCancel: false,
        success: () => {
          wx.switchTab({
            url: "/pages/orders/orders"
          });
        }
      });
    } catch (error) {
      wx.showModal({
        title: "预订失败",
        content: "后端服务不可用，订单未创建也不会同步到酒店后台。请启动后端后重试。",
        showCancel: false
      });
    }
  }
});
