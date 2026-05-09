const { getMergedOrders, upsertLocalOrder, roomTypes, getRoomById } = require("../../data/mock");
const { getOrderDetail, cancelOrder, payOrder, normalizeOrder, getRoomDetail, normalizeRoom } = require("../../services/hotel");
const { getCurrentOpenid, getCurrentUser, getOrCreateDeviceId } = require("../../utils/session");
const { markCouponUsed, returnCoupon } = require("../../utils/coupons");

function decodeOption(value) {
  const text = value === undefined || value === null ? "" : String(value);
  try {
    return decodeURIComponent(text);
  } catch (error) {
    return text;
  }
}

function uniqueOpenids() {
  return [getCurrentOpenid(), getOrCreateDeviceId()].filter(Boolean).filter((openid, index, all) => all.indexOf(openid) === index);
}

function findLocalOrder(orderId) {
  return uniqueOpenids()
    .flatMap((openid) => getMergedOrders(openid))
    .map(normalizeOrder)
    .find((item) => String(item.id) === String(orderId)) || null;
}

function formatTime(value) {
  if (!value) {
    return "";
  }
  const text = String(value);
  if (!text.includes("T")) {
    return text;
  }
  return text.replace("T", " ").replace(/\.\d+Z?$/, "").slice(0, 16);
}

function buildDetailRows(order) {
  const phone = order.guestPhone || order.userPhone;
  return [
    { label: "联系人", value: order.guest },
    { label: "联系电话", value: phone },
    { label: "身份证号", value: order.guestIdCard },
    { label: "房号", value: order.roomNo },
    { label: "入住晚数", value: order.stayNights ? `${order.stayNights} 晚` : "" },
    { label: "房费原价", value: order.originalAmount && order.originalAmount !== order.amount ? `¥${order.originalAmount}` : "" },
    { label: "优惠抵扣", value: order.discountAmount ? `-¥${order.discountAmount}` : "" },
    { label: "使用优惠", value: order.couponTitle },
    { label: "支付状态", value: order.paymentStatusText },
    { label: "支付时间", value: formatTime(order.paidAt) },
    { label: "退款状态", value: order.refundStatusText },
    { label: "退款金额", value: order.refundAmount ? `¥${order.refundAmount}` : "" },
    { label: "下单时间", value: formatTime(order.createdAt) },
    { label: "入住时间", value: formatTime(order.checkInAt) },
    { label: "退房时间", value: formatTime(order.checkOutAt) },
    { label: "退房来源", value: order.checkOutSource }
  ].filter((item) => item.value);
}

function resolveRoom(room) {
  if (room && room.id && room.name && room.name !== "未命名房型") {
    return room;
  }
  return null;
}

Page({
  data: {
    currentUser: null,
    orderId: "",
    order: null,
    room: null,
    detailRows: [],
    roomImage: "",
    roomFeatures: [],
    dataSourceText: "本地模拟数据",
    loading: false,
    canceling: false,
    errorText: ""
  },

  onLoad(options) {
    const orderId = decodeOption(options.id);
    this.setData({
      orderId,
      currentUser: getCurrentUser()
    });
    this.loadDetail();
  },

  async loadDetail() {
    if (!this.data.orderId) {
      this.setData({ errorText: "订单号缺失", loading: false });
      return;
    }

    this.setData({
      loading: true,
      errorText: ""
    });

    let order = null;
    let dataSourceText = "本地模拟数据";

    try {
      if (getCurrentOpenid()) {
        order = normalizeOrder(await getOrderDetail(this.data.orderId));
        dataSourceText = "后端接口数据";
      }
    } catch (error) {
      order = null;
    }

    if (!order) {
      order = findLocalOrder(this.data.orderId);
    }

    if (!order) {
      this.setData({
        loading: false,
        errorText: "未找到该订单"
      });
      return;
    }

    let room = null;
    try {
      if (order.roomTypeId) {
        if (dataSourceText === "后端接口数据") {
          room = normalizeRoom(await getRoomDetail(order.roomTypeId));
        } else {
          room = normalizeRoom(getRoomById(order.roomTypeId));
        }
      } else {
        const matchedRoom = roomTypes.find((item) => item.name === order.roomName);
        room = matchedRoom ? normalizeRoom(matchedRoom) : null;
      }
    } catch (error) {
      room = null;
    }

    room = resolveRoom(room);

    this.setData({
      order,
      room,
      detailRows: buildDetailRows(order),
      roomImage: room ? room.image : "",
      roomFeatures: room ? (room.features || []) : [],
      dataSourceText,
      loading: false
    });
  },

  async handleCancel() {
    if (!this.data.order || this.data.order.status !== "upcoming" || this.data.canceling) {
      return;
    }

    wx.showModal({
      title: "取消订单",
      content: "确定取消这笔未入住订单吗？",
      confirmText: "确认取消",
      success: async (res) => {
        if (!res.confirm) {
          return;
        }

        this.setData({ canceling: true });
        wx.showLoading({ title: "取消中" });

        let nextOrder = null;
        let nextSourceText = "后端接口数据";
        try {
          nextOrder = normalizeOrder(await cancelOrder(this.data.order.id));
        } catch (error) {
          nextSourceText = "本地模拟数据";
          nextOrder = {
            ...this.data.order,
            status: "cancelled",
            statusText: "已取消",
            paymentStatus: this.data.order.paymentStatus === "paid" ? "refunded" : this.data.order.paymentStatus,
            paymentStatusText: this.data.order.paymentStatus === "paid" ? "已退款" : this.data.order.paymentStatusText,
            refundStatus: this.data.order.paymentStatus === "paid" ? "refunded" : "",
            refundStatusText: this.data.order.paymentStatus === "paid" ? "已退款" : "",
            refundAmount: this.data.order.paymentStatus === "paid" ? this.data.order.amount : 0
          };
        } finally {
          wx.hideLoading();
        }

        if (this.data.order.couponId && (this.data.order.paymentStatus === "paid" || this.data.order.paymentStatus === "pending")) {
          returnCoupon(this.data.order.couponId);
        }
        upsertLocalOrder(nextOrder);

        this.setData({
          order: nextOrder,
          detailRows: buildDetailRows(nextOrder),
          dataSourceText: nextSourceText,
          canceling: false
        });

        wx.hideLoading();
        wx.showToast({
          title: this.data.order.paymentStatus === "paid" ? "已退款并返券" : "已取消",
          icon: "success"
        });

        setTimeout(() => {
          wx.switchTab({
            url: "/pages/orders/orders"
          });
        }, 500);
      }
    });
  },

  async handlePay() {
    if (!this.data.order || this.data.order.status !== "upcoming" || this.data.order.paymentStatus !== "pending") {
      return;
    }

    let paidOrder = null;
    let nextSourceText = "后端接口数据";
    try {
      paidOrder = normalizeOrder(await payOrder(this.data.order.id));
    } catch (error) {
      nextSourceText = "本地模拟数据";
      paidOrder = {
        ...this.data.order,
        paymentStatus: "paid",
        paymentStatusText: "已支付",
        paidAt: new Date().toISOString()
      };
    }
    upsertLocalOrder(paidOrder);
    markCouponUsed(paidOrder.couponId, paidOrder.id);
    this.setData({
      order: paidOrder,
      detailRows: buildDetailRows(paidOrder),
      dataSourceText: nextSourceText
    });
    wx.showToast({
      title: "支付成功",
      icon: "success"
    });
  },

  handleBackToOrders() {
    wx.switchTab({
      url: "/pages/orders/orders"
    });
  }
});
