const roomTypes = [
  {
    id: 1,
    name: "豪华大床房",
    price: 428,
    area: "32m²",
    bed: "1.8米大床",
    breakfast: "双早",
    occupancy: "2人",
    status: "hot",
    tag: "热门",
    image: "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80",
    summary: "高楼层景观房，适合情侣与商务单人入住。",
    features: ["景观窗", "智能电视", "独立淋浴", "免费停车"]
  },
  {
    id: 2,
    name: "商务双床房",
    price: 396,
    area: "35m²",
    bed: "双1.35米床",
    breakfast: "双早",
    occupancy: "2人",
    status: "steady",
    tag: "稳定",
    image: "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?auto=format&fit=crop&w=900&q=80",
    summary: "双床布局更适合亲子、同事出行和双人旅行。",
    features: ["静音楼层", "书桌办公", "行李架", "高速WiFi"]
  },
  {
    id: 3,
    name: "行政套房",
    price: 888,
    area: "58m²",
    bed: "1.8米大床",
    breakfast: "双早",
    occupancy: "2人",
    status: "luxury",
    tag: "高端",
    image: "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80",
    summary: "配备会客区与浴缸，适合高品质住宿和商务接待。",
    features: ["会客沙发", "浴缸", "迷你吧", "欢迎水果"]
  },
  {
    id: 4,
    name: "钟点房",
    price: 168,
    area: "28m²",
    bed: "随机分配",
    breakfast: "无",
    occupancy: "2人",
    status: "budget",
    tag: "特惠",
    image: "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=900&q=80",
    summary: "灵活短住，适合中转休息与白天临时办公。",
    features: ["4小时起订", "灵活时段", "快捷入住", "卫生安心"]
  }
];

const notices = [
  {
    id: 1,
    title: "五一假期入住温馨提示",
    level: "重要",
    content: "节假日期间入住高峰较多，建议提前在小程序完成预订与登记。"
  },
  {
    id: 2,
    title: "连住优惠活动上线",
    level: "活动",
    content: "连续入住两晚及以上可享95折，部分房型赠双早。"
  }
];

const orderList = [
  {
    id: "HT20260501001",
    wxOpenid: "dev-local-device",
    userPhone: "13800138000",
    guest: "张同学",
    roomName: "豪华大床房",
    amount: 856,
    date: "2026-05-01 至 2026-05-03",
    status: "upcoming",
    statusText: "待入住"
  },
  {
    id: "HT20260501002",
    wxOpenid: "dev-local-device",
    userPhone: "13800138001",
    guest: "赵女士",
    roomName: "豪华大床房",
    amount: 1284,
    date: "2026-05-01 至 2026-05-04",
    status: "staying",
    statusText: "在住中"
  },
  {
    id: "HT20260430001",
    wxOpenid: "dev-local-device",
    userPhone: "13800138002",
    guest: "陈女士",
    roomName: "钟点房",
    amount: 168,
    date: "2026-04-30",
    status: "finished",
    statusText: "已完成"
  }
];

const homeStats = [
  { label: "今日可订", value: "66间" },
  { label: "好评率", value: "93%" },
  { label: "入住率", value: "86%" }
];

const profileData = {
  avatarText: "HY",
  nickname: "酒店住客",
  description: "这里可以继续接微信头像昵称、手机号授权和会员积分。",
  stats: [
    { label: "会员等级", value: "悦享" },
    { label: "待使用权益", value: "2" },
    { label: "好评匹配", value: "93%" }
  ],
  menuItems: [
    { title: "入住人信息", subtitle: "提前保存常用住客资料" },
    { title: "生成房间密码", subtitle: "为在住订单生成动态门锁密码" },
    { title: "自助退房", subtitle: "一键退房并释放房间状态" },
    { title: "优惠券", subtitle: "查看活动权益与折扣" },
    { title: "联系客服", subtitle: "电话咨询与入住帮助" }
  ]
};

const LOCAL_ORDER_KEY = "demoLocalOrders";

function getRoomById(id) {
  return roomTypes.find((item) => String(item.id) === String(id)) || roomTypes[0];
}

function getStoredOrders() {
  try {
    return wx.getStorageSync(LOCAL_ORDER_KEY) || [];
  } catch (error) {
    return [];
  }
}

function saveStoredOrders(list) {
  try {
    wx.setStorageSync(LOCAL_ORDER_KEY, list);
  } catch (error) {
    return;
  }
}

function upsertLocalOrder(order) {
  const nextList = [order, ...getStoredOrders().filter((item) => String(item.id) !== String(order.id))];
  saveStoredOrders(nextList);
  return order;
}

function removeLocalOrder(id) {
  const nextList = getStoredOrders().filter((item) => String(item.id) !== String(id));
  saveStoredOrders(nextList);
  return nextList;
}

function getMergedOrders(wxOpenid) {
  if (!wxOpenid) {
    return [];
  }
  return [...getStoredOrders(), ...orderList].filter((item) => item.wxOpenid === wxOpenid);
}

function createLocalOrder(payload, room) {
  const now = new Date();
  const datePart = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, "0")}${String(now.getDate()).padStart(2, "0")}`;
  // 降级模式：用时间戳做后缀，后端真实环境为日期顺序编号
  const orderId = `HT${datePart}${String(now.getHours()).padStart(2, "0")}${String(now.getMinutes()).padStart(2, "0")}`;
  const order = {
    id: orderId,
    wxOpenid: payload.wxOpenid,
    userPhone: payload.userPhone || payload.guestPhone,
    guest: payload.guestName,
    roomName: room.name,
    amount: payload.totalAmount,
    date: `${payload.checkInDate} 至 ${payload.checkOutDate}`,
    status: "upcoming",
    statusText: "待入住"
  };
  const merged = [order, ...getStoredOrders()];
  saveStoredOrders(merged);
  return order;
}

module.exports = {
  roomTypes,
  notices,
  orderList,
  homeStats,
  profileData,
  getRoomById,
  getMergedOrders,
  createLocalOrder,
  upsertLocalOrder,
  removeLocalOrder
};
