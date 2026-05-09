const { getRoomById, createLocalOrder, findIdCardBookingConflict } = require("../../data/mock");
const { getRoomDetail, createOrder, payOrder, normalizeRoom, loginWithDemoDevice } = require("../../services/hotel");
const { getCurrentUser, getOrCreateDeviceId, saveWxSession } = require("../../utils/session");
const {
  NO_COUPON_ID,
  PAYMENT_STATUS_PAID,
  PAYMENT_STATUS_PENDING,
  normalizeStayNights,
  calculateBookingPricing,
  buildBookingOrderPayload
} = require("../../utils/booking-calculator");
const { readCouponList, lockCouponForOrder, markCouponUsed } = require("../../utils/coupons");
const { readGuestList, getPreferredGuest, isValidIdCard, normalizeIdCard } = require("../../utils/guest");

function normalizePhone(value) {
  return String(value || "").replace(/\D/g, "").slice(0, 11);
}

Page({
  data: {
    room: null,
    stayNights: 1,
    totalPrice: 0,
    pricing: calculateBookingPricing({ room: null, stayNights: 1 }),
    couponList: [],
    availableCoupons: [],
    selectedCouponId: "",
    paymentStatus: PAYMENT_STATUS_PAID,
    contactName: "",
    contactPhone: "",
    contactIdCard: "",
    savedGuestName: "",
    savedGuestPhone: "",
    savedGuestIdCard: "",
    savedGuestLabel: ""
  },

  onLoad(options) {
    this.loadGuestInfo();
    this.loadCoupons();
    this.loadRoomDetail(options.id);
  },

  onShow() {
    this.loadGuestInfo();
    this.loadCoupons();
  },

  loadGuestInfo() {
    const currentUser = getCurrentUser();
    const preferredGuest = getPreferredGuest(readGuestList());
    const nextData = {
      savedGuestName: "",
      savedGuestPhone: "",
      savedGuestIdCard: "",
      savedGuestLabel: ""
    };

    if (preferredGuest) {
      const guestName = String(preferredGuest.name || "").trim();
      const guestPhone = normalizePhone(preferredGuest.phone);
      const guestIdCard = normalizeIdCard(preferredGuest.idCard);
      nextData.savedGuestName = guestName;
      nextData.savedGuestPhone = guestPhone;

      if (isValidIdCard(guestIdCard)) {
        nextData.savedGuestIdCard = guestIdCard;
        nextData.savedGuestLabel = guestName ? `已带入入住人：${guestName}` : "已带入住人信息";
        nextData.contactName = guestName || (currentUser && currentUser.nickname) || "微信用户";
        nextData.contactPhone = guestPhone;
        nextData.contactIdCard = guestIdCard;
        this.setData(nextData);
        return;
      }

      nextData.contactName = this.data.contactName || guestName || (currentUser && currentUser.nickname) || "微信用户";
      nextData.contactPhone = this.data.contactPhone || guestPhone;
      nextData.contactIdCard = this.data.contactIdCard || "";
      this.setData(nextData);
      return;
    }

    nextData.contactName = this.data.contactName || (currentUser && currentUser.nickname) || "微信用户";
    nextData.contactPhone = this.data.contactPhone || "";
    nextData.contactIdCard = this.data.contactIdCard || "";
    this.setData(nextData);
  },

  loadCoupons() {
    const couponList = readCouponList();
    this.setData({ couponList });
    this.refreshPricing(this.data.selectedCouponId);
  },

  async loadRoomDetail(id) {
    try {
      const room = normalizeRoom(await getRoomDetail(id));
      this.setData({
        room
      });
      this.refreshPricing(this.data.selectedCouponId);
    } catch (error) {
      const room = getRoomById(id);
      this.setData({
        room
      });
      this.refreshPricing(this.data.selectedCouponId);
    }
  },

  handleInput(event) {
    const { field } = event.currentTarget.dataset;
    let value = event.detail.value;
    if (field === "contactPhone") {
      value = normalizePhone(value);
    }
    if (field === "contactIdCard") {
      value = normalizeIdCard(value);
    }
    this.setData({
      [field]: value
    });
  },

  handleNightChange(event) {
    const stayNights = normalizeStayNights(event.detail.value);
    this.setData({
      stayNights
    });
    this.refreshPricing(this.data.selectedCouponId);
  },

  handleCouponSelect(event) {
    const { id } = event.currentTarget.dataset;
    this.refreshPricing(id || NO_COUPON_ID);
  },

  handlePaymentModeChange(event) {
    const { status } = event.currentTarget.dataset;
    this.setData({
      paymentStatus: status === PAYMENT_STATUS_PENDING ? PAYMENT_STATUS_PENDING : PAYMENT_STATUS_PAID
    });
  },

  refreshPricing(preferredCouponId) {
    if (!this.data.room) {
      return;
    }

    const selectedCouponId = preferredCouponId === undefined ? this.data.selectedCouponId : preferredCouponId;
    let pricing = calculateBookingPricing({
      room: this.data.room,
      stayNights: this.data.stayNights,
      couponList: this.data.couponList,
      selectedCouponId
    });

    if (selectedCouponId && selectedCouponId !== NO_COUPON_ID && !pricing.selectedCoupon) {
      pricing = calculateBookingPricing({
        room: this.data.room,
        stayNights: this.data.stayNights,
        couponList: this.data.couponList
      });
    }

    this.setData({
      pricing,
      availableCoupons: pricing.eligibleCoupons,
      selectedCouponId: pricing.selectedCouponId,
      totalPrice: pricing.payableAmount
    });
  },

  async submitOrder() {
    let currentUser = getCurrentUser();
    if (!currentUser || !currentUser.token || !currentUser.openid) {
      wx.showToast({
        title: "请先到“我的”页面登录",
        icon: "none"
      });
      return;
    }

    const preferredGuest = getPreferredGuest(readGuestList());
    const preferredGuestName = preferredGuest ? String(preferredGuest.name || "").trim() : "";
    const preferredGuestPhone = preferredGuest ? normalizePhone(preferredGuest.phone) : "";
    const preferredGuestIdCard = preferredGuest ? normalizeIdCard(preferredGuest.idCard) : "";
    const hasSavedGuestIdCard = isValidIdCard(preferredGuestIdCard);
    const contactName = this.data.contactName || preferredGuestName || (currentUser && currentUser.nickname) || "微信用户";
    const contactPhone = normalizePhone(this.data.contactPhone || preferredGuestPhone);
    const contactIdCard = hasSavedGuestIdCard ? preferredGuestIdCard : normalizeIdCard(this.data.contactIdCard);
    if (!contactName || !contactPhone) {
      wx.showToast({
        title: "请先填写联系人信息",
        icon: "none"
      });
      return;
    }

    if (!hasSavedGuestIdCard && !contactIdCard) {
      wx.showToast({
        title: "请先填写身份证号",
        icon: "none"
      });
      return;
    }

    if (!/^1\d{10}$/.test(contactPhone)) {
      wx.showToast({
        title: "请输入11位手机号",
        icon: "none"
      });
      this.setData({ contactPhone });
      return;
    }

    if (!hasSavedGuestIdCard && !/^\d{17}[\dX]$/.test(contactIdCard)) {
      wx.showToast({
        title: "请输入18位身份证号",
        icon: "none"
      });
      this.setData({ contactIdCard });
      return;
    }

    const payload = buildBookingOrderPayload({
      room: this.data.room,
      contactName,
      contactPhone,
      contactIdCard,
      currentUser,
      stayNights: this.data.stayNights,
      couponList: this.data.couponList,
      selectedCouponId: this.data.selectedCouponId,
      paymentStatus: this.data.paymentStatus
    });

    const conflictOrder = findIdCardBookingConflict(payload);
    if (conflictOrder) {
      wx.showModal({
        title: "无法重复预订",
        content: `该身份证在 ${conflictOrder.checkInDate || conflictOrder.date} 已有订单，同一天不能重复预订两个房间。`,
        showCancel: false
      });
      return;
    }

    try {
      let refreshedUser = currentUser;
      let order = null;
      try {
        order = await createOrder(payload);
      } catch (error) {
        if (!/登录|token|401/i.test(error.message || "")) {
          throw error;
        }
        const session = await loginWithDemoDevice(refreshedUser.openid || getOrCreateDeviceId());
        saveWxSession(session);
        refreshedUser = getCurrentUser();
        order = await createOrder({
          ...payload,
          wxOpenid: refreshedUser.openid,
          userPhone: refreshedUser.openid
        });
      }
      if (payload.paymentStatus === PAYMENT_STATUS_PAID) {
        order = await payOrder(order.id);
      }
      if (payload.paymentStatus === PAYMENT_STATUS_PAID) {
        markCouponUsed(payload.couponId, order.id);
      } else {
        lockCouponForOrder(payload.couponId, order.id);
      }
      this.loadCoupons();
      wx.showModal({
        title: "预订成功",
        content: `订单号：${order.id}\n${payload.paymentStatus === PAYMENT_STATUS_PAID ? "已支付" : "待支付"}金额 ¥${order.totalAmount || payload.totalAmount} 已同步到酒店后台。`,
        showCancel: false,
        success: () => {
          wx.switchTab({
            url: "/pages/orders/orders"
          });
        }
      });
    } catch (error) {
      if (/Remote API disabled/i.test(error.message || "")) {
        const order = createLocalOrder(payload, this.data.room);
        if (payload.paymentStatus === PAYMENT_STATUS_PAID) {
          markCouponUsed(payload.couponId, order.id);
        } else {
          lockCouponForOrder(payload.couponId, order.id);
        }
        this.loadCoupons();
        wx.showModal({
          title: "预订成功",
          content: `订单号：${order.id}\n本地演示订单已按${payload.paymentStatus === PAYMENT_STATUS_PAID ? "已支付" : "待支付"}金额 ¥${payload.totalAmount} 保存。`,
          showCancel: false,
          success: () => {
            wx.switchTab({
              url: "/pages/orders/orders"
            });
          }
        });
        return;
      }
      wx.showModal({
        title: "预订失败",
        content: error.message || "订单未创建也不会同步到酒店后台，请稍后重试。",
        showCancel: false
      });
    }
  }
});
