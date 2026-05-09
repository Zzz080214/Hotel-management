const MS_PER_DAY = 24 * 60 * 60 * 1000;
const MIN_STAY_NIGHTS = 1;
const MAX_STAY_NIGHTS = 5;
const NO_COUPON_ID = "none";
const PAYMENT_STATUS_PAID = "paid";
const PAYMENT_STATUS_PENDING = "pending";

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function addDays(date, days) {
  return new Date(date.getTime() + days * MS_PER_DAY);
}

function normalizeStayNights(value) {
  const nights = Math.floor(Number(value));
  if (!Number.isFinite(nights)) {
    return MIN_STAY_NIGHTS;
  }
  return Math.min(MAX_STAY_NIGHTS, Math.max(MIN_STAY_NIGHTS, nights));
}

function roundMoney(value) {
  return Math.round(Number(value || 0) * 100) / 100;
}

function calculateBaseAmount(room, stayNights) {
  const price = Number(room && room.price ? room.price : 0);
  return roundMoney(price * normalizeStayNights(stayNights));
}

function isPaymentCoupon(coupon) {
  return coupon && (coupon.type === "fixed" || coupon.type === "percent");
}

function isClaimedCoupon(coupon) {
  return coupon && coupon.status === "已领取";
}

function isCouponEligible(coupon, { room, stayNights, baseAmount }) {
  if (!isPaymentCoupon(coupon) || !isClaimedCoupon(coupon)) {
    return false;
  }

  if (coupon.minAmount && baseAmount < Number(coupon.minAmount)) {
    return false;
  }

  if (coupon.minNights && normalizeStayNights(stayNights) < Number(coupon.minNights)) {
    return false;
  }

  if (Array.isArray(coupon.roomNames) && coupon.roomNames.length) {
    return coupon.roomNames.includes(room && room.name);
  }

  return true;
}

function calculateCouponDiscount(coupon, baseAmount) {
  if (!coupon) {
    return 0;
  }

  if (coupon.type === "fixed") {
    return Math.min(baseAmount, roundMoney(coupon.value));
  }

  if (coupon.type === "percent") {
    return Math.min(baseAmount, roundMoney(baseAmount * (1 - Number(coupon.value || 1))));
  }

  return 0;
}

function getEligibleCoupons({ room, stayNights, couponList = [] }) {
  const baseAmount = calculateBaseAmount(room, stayNights);
  return couponList.filter((coupon) => isCouponEligible(coupon, {
    room,
    stayNights,
    baseAmount
  }));
}

function chooseBestCoupon({ room, stayNights, couponList = [] }) {
  const baseAmount = calculateBaseAmount(room, stayNights);
  const best = getEligibleCoupons({ room, stayNights, couponList })
    .map((coupon) => ({
      coupon,
      discount: calculateCouponDiscount(coupon, baseAmount)
    }))
    .sort((a, b) => b.discount - a.discount)[0];
  return best ? best.coupon : null;
}

function calculateBookingPricing({
  room,
  stayNights,
  couponList = [],
  selectedCouponId
}) {
  const nights = normalizeStayNights(stayNights);
  const baseAmount = calculateBaseAmount(room, nights);
  const eligibleCoupons = getEligibleCoupons({ room, stayNights: nights, couponList });
  let selectedCoupon = null;

  if (selectedCouponId && selectedCouponId !== NO_COUPON_ID) {
    selectedCoupon = eligibleCoupons.find((coupon) => coupon.id === selectedCouponId) || null;
  } else if (!selectedCouponId) {
    selectedCoupon = chooseBestCoupon({ room, stayNights: nights, couponList });
  }

  const couponDiscount = roundMoney(calculateCouponDiscount(selectedCoupon, baseAmount));
  const payableAmount = roundMoney(Math.max(0, baseAmount - couponDiscount));

  return {
    roomPrice: roundMoney(room && room.price),
    stayNights: nights,
    baseAmount,
    selectedCouponId: selectedCoupon ? selectedCoupon.id : NO_COUPON_ID,
    selectedCoupon,
    couponDiscount,
    payableAmount,
    savedAmount: couponDiscount,
    eligibleCoupons
  };
}

function calculateBookingTotal(room, stayNights, couponList, selectedCouponId) {
  return calculateBookingPricing({
    room,
    stayNights,
    couponList,
    selectedCouponId
  }).payableAmount;
}

function buildBookingOrderPayload({
  room,
  contactName,
  contactPhone,
  contactIdCard,
  currentUser,
  stayNights,
  couponList = [],
  selectedCouponId,
  paymentStatus = PAYMENT_STATUS_PAID,
  startDate = new Date()
}) {
  const nights = normalizeStayNights(stayNights);
  const pricing = calculateBookingPricing({
    room,
    stayNights: nights,
    couponList,
    selectedCouponId
  });

  return {
    roomTypeId: room.id,
    roomTypeName: room.name,
    guestName: contactName,
    guestPhone: contactPhone,
    guestIdCard: contactIdCard,
    wxOpenid: currentUser.openid,
    userPhone: currentUser.openid,
    stayNights: nights,
    totalAmount: pricing.payableAmount,
    originalAmount: pricing.baseAmount,
    discountAmount: pricing.couponDiscount,
    couponId: pricing.selectedCoupon ? pricing.selectedCoupon.id : "",
    couponTitle: pricing.selectedCoupon ? pricing.selectedCoupon.title : "",
    paymentStatus,
    checkInDate: formatDate(startDate),
    checkOutDate: formatDate(addDays(startDate, nights))
  };
}

module.exports = {
  NO_COUPON_ID,
  PAYMENT_STATUS_PAID,
  PAYMENT_STATUS_PENDING,
  formatDate,
  normalizeStayNights,
  roundMoney,
  calculateBaseAmount,
  calculateBookingPricing,
  calculateBookingTotal,
  getEligibleCoupons,
  chooseBestCoupon,
  buildBookingOrderPayload
};
