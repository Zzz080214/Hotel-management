const { getMergedOrders, upsertLocalOrder } = require("../../data/mock");
const { getMyOrders, normalizeOrder, cancelOrder, payOrder } = require("../../services/hotel");
const { getCurrentUser, getCurrentOpenid, getOrCreateDeviceId } = require("../../utils/session");
const { markCouponUsed, returnCoupon } = require("../../utils/coupons");

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
    visibleOrders: [],
    cancelingOrderId: ""
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
    if (!id) {
      wx.showToast({
        title: "订单号缺失",
        icon: "none"
      });
      return;
    }
    wx.navigateTo({
      url: `/pages/order-detail/order-detail?id=${encodeURIComponent(id)}`
    });
  },

  startFaceCheckin(event) {
    const { id, guest, roomName, amount, date } = event.currentTarget.dataset;
    const order = this.data.orders.find((item) => String(item.id) === String(id));
    if (order && order.paymentStatus !== "paid") {
      wx.showToast({
        title: "请先完成支付",
        icon: "none"
      });
      return;
    }
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
  },

  async handlePay(event) {
    const { id } = event.currentTarget.dataset;
    const order = this.data.orders.find((item) => String(item.id) === String(id));
    if (!order || order.status !== "upcoming" || order.paymentStatus !== "pending") {
      return;
    }

    let paidOrder = null;
    try {
      if (!getCurrentOpenid()) {
        throw new Error("未登录，使用本地模拟支付");
      }
      paidOrder = normalizeOrder(await payOrder(order.id));
    } catch (error) {
      paidOrder = {
        ...order,
        paymentStatus: "paid",
        paymentStatusText: "已支付",
        paidAt: new Date().toISOString()
      };
    }
    upsertLocalOrder(paidOrder);
    markCouponUsed(paidOrder.couponId, paidOrder.id);

    const nextOrders = this.data.orders.map((item) => (
      String(item.id) === String(paidOrder.id) ? paidOrder : item
    ));
    this.setData({
      orders: nextOrders,
      dataSourceText: "本地模拟数据"
    });
    this.applyFilter(this.data.activeTab, nextOrders);
    wx.showToast({
      title: "支付成功",
      icon: "success"
    });
  },

  handleCancel(event) {
    const { id } = event.currentTarget.dataset;
    const order = this.data.orders.find((item) => String(item.id) === String(id));
    if (!order || order.status !== "upcoming" || this.data.cancelingOrderId) {
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

        this.setData({ cancelingOrderId: String(order.id) });
        wx.showLoading({ title: "取消中" });

        let nextOrder = null;
        let nextSourceText = "后端接口数据";
        try {
          if (!getCurrentOpenid()) {
            throw new Error("未登录，使用本地模拟取消");
          }
          nextOrder = normalizeOrder(await cancelOrder(order.id));
        } catch (error) {
          nextSourceText = "本地模拟数据";
          nextOrder = {
            ...order,
            status: "cancelled",
            statusText: "已取消",
            paymentStatus: order.paymentStatus === "paid" ? "refunded" : order.paymentStatus,
            paymentStatusText: order.paymentStatus === "paid" ? "已退款" : order.paymentStatusText,
            refundStatus: order.paymentStatus === "paid" ? "refunded" : "",
            refundStatusText: order.paymentStatus === "paid" ? "已退款" : "",
            refundAmount: order.paymentStatus === "paid" ? order.amount : 0
          };
        } finally {
          wx.hideLoading();
        }

        if (order.couponId && (order.paymentStatus === "paid" || order.paymentStatus === "pending")) {
          returnCoupon(order.couponId);
        }
        upsertLocalOrder(nextOrder);

        const nextOrders = this.data.orders.map((item) => (
          String(item.id) === String(nextOrder.id) ? nextOrder : item
        ));
        this.setData({
          orders: nextOrders,
          dataSourceText: nextSourceText,
          cancelingOrderId: ""
        });
        this.applyFilter(this.data.activeTab, nextOrders);

        wx.showToast({
          title: order.paymentStatus === "paid" ? "已退款并返券" : "已取消",
          icon: "success"
        });
      }
    });
  }
});
