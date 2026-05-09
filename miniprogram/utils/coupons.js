const COUPON_KEY = "profile_coupon_list";

function defaultCoupons() {
  return [
    {
      id: "coupon-new",
      title: "新客立减券",
      amount: "¥60",
      threshold: "满 399 可用",
      expire: "2026-05-31",
      status: "可领取",
      type: "fixed",
      value: 60,
      minAmount: 399
    },
    {
      id: "coupon-stay",
      title: "连住优惠券",
      amount: "95折",
      threshold: "连续入住 2 晚可用",
      expire: "2026-06-30",
      status: "已领取",
      type: "percent",
      value: 0.95,
      minNights: 2
    },
    {
      id: "coupon-breakfast",
      title: "双人早餐券",
      amount: "早餐",
      threshold: "行政套房预订可用",
      expire: "2026-05-20",
      status: "可领取",
      type: "benefit",
      value: 0,
      roomNames: ["行政套房"]
    }
  ];
}

function getWx() {
  return typeof wx === "undefined" ? null : wx;
}

function normalizeCoupon(coupon = {}) {
  const preset = defaultCoupons().find((item) => item.id === coupon.id) || {};
  return {
    ...preset,
    ...coupon,
    status: coupon.status || preset.status || "可领取"
  };
}

function readCouponList() {
  const wxApi = getWx();
  if (!wxApi) {
    return defaultCoupons();
  }

  try {
    const stored = wxApi.getStorageSync(COUPON_KEY);
    const list = Array.isArray(stored) && stored.length ? stored : defaultCoupons();
    return list.map(normalizeCoupon);
  } catch (error) {
    return defaultCoupons();
  }
}

function writeCouponList(list) {
  const wxApi = getWx();
  if (!wxApi) {
    return;
  }

  try {
    wxApi.setStorageSync(COUPON_KEY, list.map(normalizeCoupon));
  } catch (error) {
  }
}

function isPaymentCoupon(coupon) {
  return coupon && (coupon.type === "fixed" || coupon.type === "percent");
}

function isClaimedCoupon(coupon) {
  return coupon && coupon.status === "已领取";
}

function getPaymentCouponList(list = readCouponList()) {
  return list.map(normalizeCoupon).filter(isPaymentCoupon);
}

function claimCouponById(id) {
  const list = readCouponList();
  let changed = false;
  let message = "优惠券不存在";

  const nextList = list.map((item) => {
    if (item.id !== id) {
      return item;
    }

    if (item.status === "已领取") {
      message = "已在券包";
      return item;
    }

    if (item.status === "已使用" || item.status === "已锁定") {
      message = item.status === "已锁定" ? "优惠券已被订单占用" : "优惠券已使用";
      return item;
    }

    changed = true;
    message = "领取成功";
    return {
      ...item,
      status: "已领取"
    };
  });

  if (changed) {
    writeCouponList(nextList);
  }

  return {
    changed,
    message,
    list: nextList
  };
}

function lockCouponForOrder(couponId, orderId) {
  if (!couponId) {
    return readCouponList();
  }

  const now = new Date().toISOString();
  const nextList = readCouponList().map((item) => {
    if (item.id !== couponId || item.status !== "已领取") {
      return item;
    }
    return {
      ...item,
      status: "已锁定",
      lockedAt: now,
      lockedOrderId: orderId || ""
    };
  });
  writeCouponList(nextList);
  return nextList;
}

function markCouponUsed(couponId, orderId) {
  if (!couponId) {
    return readCouponList();
  }

  const now = new Date().toISOString();
  const nextList = readCouponList().map((item) => {
    if (item.id !== couponId || (item.status !== "已领取" && item.status !== "已锁定")) {
      return item;
    }

    const { lockedAt, lockedOrderId, ...rest } = item;
    return {
      ...rest,
      status: "已使用",
      usedAt: now,
      usedOrderId: orderId || lockedOrderId || ""
    };
  });
  writeCouponList(nextList);
  return nextList;
}

function returnCoupon(couponId) {
  if (!couponId) {
    return readCouponList();
  }

  const nextList = readCouponList().map((item) => {
    if (item.id !== couponId || (item.status !== "已使用" && item.status !== "已锁定")) {
      return item;
    }

    const { lockedAt, lockedOrderId, usedAt, usedOrderId, ...rest } = item;
    return {
      ...rest,
      status: "已领取",
      returnedAt: new Date().toISOString()
    };
  });
  writeCouponList(nextList);
  return nextList;
}

module.exports = {
  COUPON_KEY,
  defaultCoupons,
  normalizeCoupon,
  readCouponList,
  writeCouponList,
  isPaymentCoupon,
  isClaimedCoupon,
  getPaymentCouponList,
  claimCouponById,
  lockCouponForOrder,
  markCouponUsed,
  returnCoupon
};
