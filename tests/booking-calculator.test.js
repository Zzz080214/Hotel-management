const assert = require("assert");

const {
  PAYMENT_STATUS_PENDING,
  normalizeStayNights,
  calculateBookingPricing,
  calculateBookingTotal,
  buildBookingOrderPayload
} = require("../miniprogram/utils/booking-calculator");

const room = {
  id: 1,
  name: "豪华大床房",
  price: 426
};

assert.strictEqual(normalizeStayNights(1), 1);
assert.strictEqual(calculateBookingTotal(room, 1), 426);

const stayCoupon = {
  id: "coupon-stay",
  title: "连住优惠券",
  type: "percent",
  value: 0.95,
  minNights: 2,
  status: "已领取"
};
const fixedCoupon = {
  id: "coupon-new",
  title: "新客立减券",
  type: "fixed",
  value: 60,
  minAmount: 399,
  status: "已领取"
};

assert.deepStrictEqual(
  calculateBookingPricing({
    room,
    stayNights: 2,
    couponList: [stayCoupon],
    selectedCouponId: "coupon-stay"
  }),
  {
    roomPrice: 426,
    stayNights: 2,
    baseAmount: 852,
    selectedCouponId: "coupon-stay",
    selectedCoupon: stayCoupon,
    couponDiscount: 42.6,
    payableAmount: 809.4,
    savedAmount: 42.6,
    eligibleCoupons: [stayCoupon]
  }
);

assert.strictEqual(
  calculateBookingPricing({
    room,
    stayNights: 2,
    couponList: [stayCoupon, fixedCoupon]
  }).selectedCouponId,
  "coupon-new"
);

const order = buildBookingOrderPayload({
  room,
  contactName: "微信用户",
  contactPhone: "13800138000",
  contactIdCard: "440101199901019999",
  currentUser: { openid: "wx-test-user" },
  stayNights: 2,
  couponList: [stayCoupon],
  selectedCouponId: "coupon-stay",
  startDate: new Date(2026, 4, 8)
});

assert.deepStrictEqual(
  {
    stayNights: order.stayNights,
    totalAmount: order.totalAmount,
    originalAmount: order.originalAmount,
    discountAmount: order.discountAmount,
    couponId: order.couponId,
    couponTitle: order.couponTitle,
    guestIdCard: order.guestIdCard,
    checkInDate: order.checkInDate,
    checkOutDate: order.checkOutDate
  },
  {
    stayNights: 2,
    totalAmount: 809.4,
    originalAmount: 852,
    discountAmount: 42.6,
    couponId: "coupon-stay",
    couponTitle: "连住优惠券",
    guestIdCard: "440101199901019999",
    checkInDate: "2026-05-08",
    checkOutDate: "2026-05-10"
  }
);

const pendingOrder = buildBookingOrderPayload({
  room,
  contactName: "微信用户",
  contactPhone: "13800138000",
  contactIdCard: "440101199901019999",
  currentUser: { openid: "wx-test-user" },
  stayNights: 1,
  paymentStatus: PAYMENT_STATUS_PENDING,
  startDate: new Date(2026, 4, 8)
});

assert.strictEqual(pendingOrder.paymentStatus, "pending");

console.log("booking-calculator tests passed");
