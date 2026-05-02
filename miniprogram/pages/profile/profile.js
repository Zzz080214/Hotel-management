const { profileData, getMergedOrders, removeLocalOrder } = require("../../data/mock");
const {
  getProfile,
  getMyOrders,
  loginWithWechat,
  logoutWechat,
  normalizeOrder,
  normalizeProfile,
  selfCheckOutOrder
} = require("../../services/hotel");
const { getCurrentUser, saveWxSession, clearCurrentUser, getOrCreateDeviceId } = require("../../utils/session");

const GUEST_KEY = "profile_guest_list";
const COUPON_KEY = "profile_coupon_list";
const PENDING_PANEL_KEY = "profile_pending_panel";

function defaultGuests() {
  return [
    {
      id: "guest-default-1",
      name: "张同学",
      phone: "13800138000",
      idCard: "4401********1234",
      isDefault: true
    }
  ];
}

function defaultCoupons() {
  return [
    {
      id: "coupon-new",
      title: "新客立减券",
      amount: "¥60",
      threshold: "满 399 可用",
      expire: "2026-05-31",
      status: "可领取"
    },
    {
      id: "coupon-stay",
      title: "连住优惠券",
      amount: "95折",
      threshold: "连续入住 2 晚可用",
      expire: "2026-06-30",
      status: "已领取"
    },
    {
      id: "coupon-breakfast",
      title: "双人早餐券",
      amount: "早餐",
      threshold: "行政套房预订可用",
      expire: "2026-05-20",
      status: "可领取"
    }
  ];
}

function readStorageList(key, fallback) {
  try {
    const stored = wx.getStorageSync(key);
    return Array.isArray(stored) && stored.length ? stored : fallback();
  } catch (error) {
    return fallback();
  }
}

function writeStorageList(key, list) {
  try {
    wx.setStorageSync(key, list);
  } catch (error) {
    return;
  }
}

function makeRoomPassword(order) {
  const seed = String(order.id || order.roomNo || Date.now());
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) {
    hash = ((hash * 31) + seed.charCodeAt(i)) % 1000000;
  }
  return String(hash).padStart(6, "0");
}

function formatExpireTime(minutes = 30) {
  const expire = new Date(Date.now() + minutes * 60 * 1000);
  const hour = String(expire.getHours()).padStart(2, "0");
  const minute = String(expire.getMinutes()).padStart(2, "0");
  return `${hour}:${minute}`;
}

function wxLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (res.code) {
          resolve(res.code);
          return;
        }
        reject(new Error("微信登录失败"));
      },
      fail: reject
    });
  });
}

function getWxProfile() {
  return new Promise((resolve) => {
    wx.getUserProfile({
      desc: "用于展示酒店会员头像昵称",
      success(res) {
        resolve(res.userInfo || {});
      },
      fail() {
        resolve({});
      }
    });
  });
}

Page({
  data: {
    dataSourceText: "本地模拟数据",
    currentUser: null,
    logging: false,
    activePanel: "",
    activePanelTitle: "",
    avatarText: profileData.avatarText,
    nickname: profileData.nickname,
    description: profileData.description,
    stats: profileData.stats,
    menuItems: profileData.menuItems,
    guestList: defaultGuests(),
    guestForm: {
      id: "",
      name: "",
      phone: "",
      idCard: ""
    },
    couponList: defaultCoupons(),
    stayOrders: [],
    currentStayOrder: null,
    roomPassword: "",
    passwordExpireAt: "",
    checkoutLoading: false,
    serviceInfo: {
      phone: "400-820-2026",
      hours: "08:00-24:00",
      address: "悦栖酒店前台"
    }
  },

  onShow() {
    this.syncCurrentUser();
    this.loadProfile();
    this.loadFeatureData();
    this.consumePendingPanel();
  },

  syncCurrentUser() {
    const currentUser = getCurrentUser();
    this.setData({
      currentUser,
      avatarText: currentUser ? String(currentUser.nickname || "微信").slice(0, 2) : profileData.avatarText,
      nickname: currentUser ? currentUser.nickname : profileData.nickname
    });
  },

  async loadProfile() {
    const currentUser = getCurrentUser();
    try {
      const profile = normalizeProfile(await getProfile());
      this.setData({
        ...profile,
        avatarText: currentUser ? String(currentUser.nickname || "微信").slice(0, 2) : profile.avatarText,
        nickname: currentUser ? currentUser.nickname : profile.nickname,
        menuItems: profileData.menuItems,
        dataSourceText: currentUser ? "后端接口数据" : "未登录，仅显示基础资料"
      });
    } catch (error) {
      this.setData({
        ...profileData,
        avatarText: currentUser ? String(currentUser.nickname || "微信").slice(0, 2) : profileData.avatarText,
        nickname: currentUser ? currentUser.nickname : profileData.nickname,
        menuItems: profileData.menuItems,
        dataSourceText: "本地模拟数据"
      });
    }
  },

  loadFeatureData() {
    const app = getApp && getApp();
    const servicePhone = app && app.globalData && app.globalData.servicePhone
      ? app.globalData.servicePhone
      : "400-820-2026";
    this.setData({
      guestList: readStorageList(GUEST_KEY, defaultGuests),
      couponList: readStorageList(COUPON_KEY, defaultCoupons),
      serviceInfo: {
        ...this.data.serviceInfo,
        phone: servicePhone
      }
    });
  },

  consumePendingPanel() {
    let pendingPanel = "";
    try {
      pendingPanel = wx.getStorageSync(PENDING_PANEL_KEY);
      wx.removeStorageSync(PENDING_PANEL_KEY);
    } catch (error) {
      pendingPanel = "";
    }
    if (pendingPanel === "password") {
      this.openPasswordPanel();
      return;
    }
    if (pendingPanel === "checkout") {
      this.openCheckoutPanel();
    }
  },

  async loginCurrentUser() {
    if (this.data.logging) return;
    this.setData({ logging: true });
    wx.showLoading({ title: "微信登录中" });
    try {
      const code = await wxLogin();
      let userInfo = {};
      try {
        userInfo = await getWxProfile();
      } catch (profileError) {
        userInfo = {};
      }
      const session = await loginWithWechat({
        code,
        nickname: userInfo.nickName || "微信用户",
        avatarUrl: userInfo.avatarUrl || "",
        devOpenid: getOrCreateDeviceId()
      });
      saveWxSession(session);
      this.syncCurrentUser();
      await this.loadProfile();
      wx.showToast({
        title: "登录成功",
        icon: "success"
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "微信登录失败",
        icon: "none"
      });
    } finally {
      wx.hideLoading();
      this.setData({ logging: false });
    }
  },

  async logoutCurrentUser() {
    await logoutWechat();
    clearCurrentUser();
    this.syncCurrentUser();
    this.loadProfile();
    wx.showToast({
      title: "已退出",
      icon: "success"
    });
  },

  showFeature(event) {
    const { title } = event.currentTarget.dataset;
    if (title === "入住人信息") {
      this.setData({
        activePanel: "guests",
        activePanelTitle: title
      });
      return;
    }
    if (title === "生成房间密码") {
      this.openPasswordPanel();
      return;
    }
    if (title === "自助退房") {
      this.openCheckoutPanel();
      return;
    }
    if (title === "优惠券") {
      this.setData({
        activePanel: "coupons",
        activePanelTitle: title
      });
      return;
    }
    if (title === "联系客服") {
      this.setData({
        activePanel: "service",
        activePanelTitle: title
      });
    }
  },

  closePanel() {
    this.setData({
      activePanel: "",
      activePanelTitle: "",
      guestForm: {
        id: "",
        name: "",
        phone: "",
        idCard: ""
      }
    });
  },

  noop() {},

  handleGuestInput(event) {
    const { field } = event.currentTarget.dataset;
    this.setData({
      [`guestForm.${field}`]: event.detail.value
    });
  },

  saveGuest() {
    const form = this.data.guestForm;
    const name = String(form.name || "").trim();
    const phone = String(form.phone || "").trim();
    const idCard = String(form.idCard || "").trim();

    if (!name) {
      wx.showToast({ title: "请输入入住人姓名", icon: "none" });
      return;
    }
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: "请输入正确手机号", icon: "none" });
      return;
    }

    const existing = this.data.guestList || [];
    const nextGuest = {
      id: form.id || `guest-${Date.now()}`,
      name,
      phone,
      idCard: idCard || "未填写",
      isDefault: form.id
        ? existing.some((item) => item.id === form.id && item.isDefault)
        : existing.length === 0
    };
    const nextList = form.id
      ? existing.map((item) => item.id === form.id ? nextGuest : item)
      : [nextGuest, ...existing];

    writeStorageList(GUEST_KEY, nextList);
    this.setData({
      guestList: nextList,
      guestForm: {
        id: "",
        name: "",
        phone: "",
        idCard: ""
      }
    });
    wx.showToast({ title: "已保存", icon: "success" });
  },

  editGuest(event) {
    const { id } = event.currentTarget.dataset;
    const guest = (this.data.guestList || []).find((item) => item.id === id);
    if (!guest) return;
    this.setData({
      guestForm: {
        id: guest.id,
        name: guest.name,
        phone: guest.phone,
        idCard: guest.idCard === "未填写" ? "" : guest.idCard
      }
    });
  },

  setDefaultGuest(event) {
    const { id } = event.currentTarget.dataset;
    const nextList = (this.data.guestList || []).map((item) => ({
      ...item,
      isDefault: item.id === id
    }));
    writeStorageList(GUEST_KEY, nextList);
    this.setData({ guestList: nextList });
    wx.showToast({ title: "已设为默认", icon: "success" });
  },

  deleteGuest(event) {
    const { id } = event.currentTarget.dataset;
    wx.showModal({
      title: "删除入住人",
      content: "确认删除这条入住人信息？",
      success: (res) => {
        if (!res.confirm) return;
        const nextList = (this.data.guestList || []).filter((item) => item.id !== id);
        if (nextList.length && !nextList.some((item) => item.isDefault)) {
          nextList[0].isDefault = true;
        }
        writeStorageList(GUEST_KEY, nextList);
        this.setData({ guestList: nextList });
      }
    });
  },

  claimCoupon(event) {
    const { id } = event.currentTarget.dataset;
    const current = (this.data.couponList || []).find((item) => item.id === id);
    if (current && current.status === "已领取") {
      wx.showToast({ title: "已在券包", icon: "none" });
      return;
    }
    const nextList = (this.data.couponList || []).map((item) => {
      if (item.id !== id || item.status === "已领取") {
        return item;
      }
      return {
        ...item,
        status: "已领取"
      };
    });
    writeStorageList(COUPON_KEY, nextList);
    this.setData({ couponList: nextList });
    wx.showToast({ title: "领取成功", icon: "success" });
  },

  async loadStayOrders(showEmptyToast = true) {
    const currentUser = getCurrentUser();
    const localOpenids = [currentUser && currentUser.openid, getOrCreateDeviceId()]
      .filter(Boolean)
      .filter((openid, index, all) => all.indexOf(openid) === index);
    try {
      if (currentUser) {
        const result = await getMyOrders("staying");
        const list = Array.isArray(result)
          ? result
          : Array.isArray(result.records)
            ? result.records
            : [];
        const stayOrders = list.map(normalizeOrder);
        if (stayOrders.length) {
          this.setData({
            stayOrders,
            currentStayOrder: stayOrders[0]
          });
          return stayOrders;
        }
      }
    } catch (error) {
      // 演示模式允许前端刷脸成功后用本地在住订单继续生成密码和退房
    }

    const localStayOrders = localOpenids
      .flatMap((openid) => getMergedOrders(openid))
      .filter((item) => item.status === "staying")
      .map(normalizeOrder);
    this.setData({
      stayOrders: localStayOrders,
      currentStayOrder: localStayOrders[0] || null
    });
    if (!localStayOrders.length && showEmptyToast) {
      wx.showToast({ title: currentUser ? "暂无在住订单" : "请先完成刷脸入住", icon: "none" });
    }
    return localStayOrders;
  },

  async openPasswordPanel() {
    this.setData({
      activePanel: "password",
      activePanelTitle: "生成房间密码",
      roomPassword: "",
      passwordExpireAt: ""
    });
    const stayOrders = await this.loadStayOrders();
    if (stayOrders.length) {
      this.refreshRoomPassword();
    }
  },

  async openCheckoutPanel() {
    this.setData({
      activePanel: "checkout",
      activePanelTitle: "自助退房"
    });
    await this.loadStayOrders();
  },

  refreshRoomPassword() {
    const order = this.data.currentStayOrder;
    if (!order) {
      wx.showToast({ title: "暂无在住订单", icon: "none" });
      return;
    }
    this.setData({
      roomPassword: makeRoomPassword(order),
      passwordExpireAt: formatExpireTime(30)
    });
  },

  copyRoomPassword() {
    if (!this.data.roomPassword) {
      wx.showToast({ title: "请先生成密码", icon: "none" });
      return;
    }
    wx.setClipboardData({
      data: this.data.roomPassword,
      success: () => {
        wx.showToast({ title: "密码已复制", icon: "success" });
      }
    });
  },

  async doSelfCheckout(event) {
    const { id } = event.currentTarget.dataset;
    if (!id || this.data.checkoutLoading) return;
    wx.showModal({
      title: "确认自助退房",
      content: "退房后房间密码将失效，订单会同步为已完成。",
      success: async (res) => {
        if (!res.confirm) return;
        this.setData({ checkoutLoading: true });
        try {
          await selfCheckOutOrder(id);
        } catch (error) {
          // 毕设演示模式：后端未同步时也允许前端完成自助退房流程
        } finally {
          removeLocalOrder(id);
          wx.showToast({ title: "退房成功", icon: "success" });
          this.setData({
            roomPassword: "",
            passwordExpireAt: ""
          });
          await this.loadStayOrders(false);
          this.setData({ checkoutLoading: false });
        }
      }
    });
  },

  callService() {
    wx.makePhoneCall({
      phoneNumber: this.data.serviceInfo.phone
    });
  },

  copyServicePhone() {
    wx.setClipboardData({
      data: this.data.serviceInfo.phone,
      success: () => {
        wx.showToast({ title: "电话已复制", icon: "success" });
      }
    });
  }
});
