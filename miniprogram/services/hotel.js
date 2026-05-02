const { request } = require("../utils/request");

const fallbackImages = [
  "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=900&q=80"
];

function splitFeatures(value) {
  if (Array.isArray(value)) {
    return value;
  }
  if (!value) {
    return [];
  }
  return String(value)
    .split(/[、,，|/]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function normalizeRoom(item = {}, index = 0) {
  const status = item.status || item.roomStatus || item.tagType || "steady";
  const tagMap = {
    hot: "热门",
    steady: "稳定",
    luxury: "高端",
    budget: "特惠"
  };

  return {
    id: item.id || item.roomTypeId || item.typeId || index + 1,
    name: item.name || item.roomTypeName || item.typeName || "未命名房型",
    price: Number(item.price || item.roomPrice || item.salePrice || item.amount || 0),
    area: item.area || item.roomArea || item.areaText || "32m²",
    bed: item.bed || item.bedType || "大床",
    breakfast: item.breakfast || item.breakfastInfo || "无",
    occupancy: item.occupancy || item.guestLimit || item.maxGuests || "2人",
    status,
    tag: item.tag || item.label || item.tagName || tagMap[status] || "推荐",
    image: item.image || item.coverImage || item.imageUrl || item.picUrl || fallbackImages[index % fallbackImages.length],
    summary: item.summary || item.description || item.remark || "舒适房型，适合日常入住。",
    features: splitFeatures(item.features || item.featureList || item.featureTags || item.facilities)
  };
}

function normalizeNotice(item = {}, index = 0) {
  return {
    id: item.id || item.noticeId || index + 1,
    title: item.title || item.noticeTitle || "系统公告",
    level: item.level || item.noticeLevel || "普通",
    content: item.content || item.noticeContent || item.description || ""
  };
}

function normalizeOrder(item = {}) {
  const rawStatus = item.status || item.orderStatus || item.state || "upcoming";
  const statusMap = {
    BOOKED: "upcoming",
    UNCHECKED: "upcoming",
    UPCOMING: "upcoming",
    CHECKED_IN: "staying",
    STAYING: "staying",
    FINISHED: "finished",
    COMPLETED: "finished",
    CANCELLED: "cancelled",
    CANCELED: "cancelled"
  };
  const textMap = {
    upcoming: "待入住",
    staying: "在住中",
    finished: "已完成",
    cancelled: "已取消"
  };
  const normalizedStatus = statusMap[String(rawStatus).toUpperCase()] || rawStatus;
  const checkInDate = item.checkInDate || item.startDate || item.inDate || "";
  const checkOutDate = item.checkOutDate || item.endDate || item.outDate || "";
  const dateText = checkInDate && checkOutDate ? `${checkInDate} 至 ${checkOutDate}` : (item.date || item.orderDate || "");

  return {
    id: item.id || item.orderId || item.orderNo || "未知订单",
    guest: item.guest || item.guestName || item.userName || "住客",
    roomName: item.roomName || item.roomTypeName || item.typeName || "房型待定",
    amount: Number(item.amount || item.totalAmount || item.payAmount || 0),
    date: dateText,
    roomNo: item.roomNo || "",
    status: normalizedStatus,
    statusText: item.statusText || item.orderStatusText || textMap[normalizedStatus] || "处理中"
  };
}

function normalizeProfile(item = {}) {
  const nickname = item.nickname || item.name || item.username || "酒店住客";
  const avatarText = item.avatarText || String(nickname).slice(0, 2) || "HY";
  const memberLevel = item.memberLevel || item.levelName || "悦享";
  const couponCount = item.couponCount !== undefined ? item.couponCount : (item.availableBenefits !== undefined ? item.availableBenefits : 0);
  const matchRate = item.matchRate || item.favoriteRate || "93%";

  return {
    avatarText,
    nickname,
    description: item.description || item.signature || "这里可以继续接微信头像昵称、手机号授权和会员积分。",
    stats: [
      { label: "会员等级", value: String(memberLevel) },
      { label: "待使用权益", value: String(couponCount) },
      { label: "好评匹配", value: String(matchRate) }
    ]
  };
}

function getRecommendRooms(limit = 3) {
  return request({
    url: "/wx/rooms/recommend",
    data: { limit }
  });
}

function getRoomList() {
  return request({
    url: "/wx/rooms"
  });
}

function getRoomDetail(id) {
  return request({
    url: `/wx/rooms/${id}`
  });
}

function getNoticeList() {
  return request({
    url: "/wx/notices"
  });
}

function loginWithWechat(data) {
  return request({
    url: "/wx/auth/login",
    method: "POST",
    data
  });
}

function logoutWechat() {
  return request({
    url: "/wx/auth/logout",
    method: "POST",
    auth: true
  }).catch(() => {});
}

function getMyOrders(status) {
  return request({
    url: "/wx/orders/my",
    data: status && status !== "all" ? { status } : undefined,
    auth: true
  });
}

function createOrder(data) {
  return request({
    url: "/wx/orders",
    method: "POST",
    data,
    auth: true
  });
}

function faceCheckInOrder(id) {
  return request({
    url: `/wx/orders/${encodeURIComponent(id)}/face-check-in`,
    method: "POST",
    auth: true
  });
}

function selfCheckOutOrder(id) {
  return request({
    url: `/wx/orders/${encodeURIComponent(id)}/self-check-out`,
    method: "POST",
    auth: true
  });
}

function getProfile() {
  return request({
    url: "/wx/users/profile",
    auth: true
  });
}

module.exports = {
  normalizeRoom,
  normalizeNotice,
  normalizeOrder,
  normalizeProfile,
  getRecommendRooms,
  getRoomList,
  getRoomDetail,
  getNoticeList,
  loginWithWechat,
  logoutWechat,
  getMyOrders,
  createOrder,
  faceCheckInOrder,
  selfCheckOutOrder,
  getProfile
};
