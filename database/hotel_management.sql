CREATE DATABASE IF NOT EXISTS hotel_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE hotel_management;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS room_type_features;
DROP TABLE IF EXISTS hotel_order;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS room_type;

CREATE TABLE room_type (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '房型ID',
  name VARCHAR(80) NOT NULL COMMENT '房型名称',
  price DECIMAL(10,2) NOT NULL COMMENT '销售价格',
  area VARCHAR(40) DEFAULT NULL COMMENT '面积',
  bed VARCHAR(80) DEFAULT NULL COMMENT '床型',
  breakfast VARCHAR(40) DEFAULT NULL COMMENT '早餐',
  occupancy VARCHAR(40) DEFAULT NULL COMMENT '可住人数',
  status VARCHAR(40) DEFAULT NULL COMMENT '展示状态：hot/steady/luxury/budget',
  tag VARCHAR(40) DEFAULT NULL COMMENT '展示标签',
  image VARCHAR(500) DEFAULT NULL COMMENT '封面图',
  summary VARCHAR(500) DEFAULT NULL COMMENT '房型简介',
  total_rooms INT NOT NULL DEFAULT 0 COMMENT '总房间数',
  available_rooms INT NOT NULL DEFAULT 0 COMMENT '可售房间数',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房型表';

CREATE TABLE room_type_features (
  room_type_id BIGINT NOT NULL COMMENT '房型ID',
  features VARCHAR(120) DEFAULT NULL COMMENT '房型特色',
  KEY idx_room_type_features_room_type_id (room_type_id),
  CONSTRAINT fk_room_type_features_room_type
    FOREIGN KEY (room_type_id) REFERENCES room_type (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房型特色表';

CREATE TABLE hotel_order (
  id VARCHAR(40) NOT NULL COMMENT '订单号',
  wx_openid VARCHAR(80) DEFAULT NULL COMMENT '微信 openid',
  user_phone VARCHAR(20) DEFAULT NULL COMMENT '下单用户手机号',
  room_type_id BIGINT DEFAULT NULL COMMENT '房型ID',
  room_type_name VARCHAR(80) NOT NULL COMMENT '房型名称',
  guest_name VARCHAR(60) NOT NULL COMMENT '住客姓名',
  guest_phone VARCHAR(20) NOT NULL COMMENT '住客手机号',
  guest_id_card VARCHAR(18) NOT NULL COMMENT '住客身份证号',
  stay_nights INT NOT NULL DEFAULT 1 COMMENT '入住晚数',
  total_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  original_amount DECIMAL(10,2) DEFAULT NULL COMMENT '优惠前金额',
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠抵扣金额',
  coupon_id VARCHAR(60) DEFAULT NULL COMMENT '使用优惠券ID',
  coupon_title VARCHAR(80) DEFAULT NULL COMMENT '使用优惠券名称',
  payment_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '支付状态：pending/paid/refunded',
  paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
  payment_trade_no VARCHAR(80) DEFAULT NULL COMMENT '支付流水号',
  refund_status VARCHAR(20) DEFAULT NULL COMMENT '退款状态',
  refund_amount DECIMAL(10,2) DEFAULT NULL COMMENT '退款金额',
  refunded_at DATETIME DEFAULT NULL COMMENT '退款时间',
  check_in_date DATE NOT NULL COMMENT '入住日期',
  check_out_date DATE NOT NULL COMMENT '退房日期',
  status VARCHAR(40) NOT NULL COMMENT '订单状态：upcoming/staying/finished/cancelled',
  room_no VARCHAR(20) DEFAULT NULL COMMENT '入住房号',
  created_at DATETIME DEFAULT NULL COMMENT '创建时间',
  check_in_at DATETIME DEFAULT NULL COMMENT '办理入住时间',
  check_out_at DATETIME DEFAULT NULL COMMENT '办理退房时间',
  check_out_source VARCHAR(20) DEFAULT NULL COMMENT '退房来源：FRONT_DESK/AUTO_CHECKOUT',
  PRIMARY KEY (id),
  KEY idx_hotel_order_wx_openid (wx_openid),
  KEY idx_hotel_order_user_phone (user_phone),
  KEY idx_hotel_order_room_type_id (room_type_id),
  KEY idx_hotel_order_guest_id_card (guest_id_card),
  KEY idx_hotel_order_status (status),
  KEY idx_hotel_order_created_at (created_at),
  CONSTRAINT fk_hotel_order_room_type
    FOREIGN KEY (room_type_id) REFERENCES room_type (id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店订单表';

CREATE TABLE notice (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  title VARCHAR(120) NOT NULL COMMENT '公告标题',
  level VARCHAR(40) NOT NULL COMMENT '公告级别',
  content VARCHAR(1000) NOT NULL COMMENT '公告内容',
  published TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否发布',
  publish_date DATE DEFAULT NULL COMMENT '发布日期',
  PRIMARY KEY (id),
  KEY idx_notice_published (published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

CREATE TABLE user_profile (
  id BIGINT NOT NULL COMMENT '用户资料ID',
  nickname VARCHAR(80) NOT NULL COMMENT '昵称',
  description VARCHAR(255) DEFAULT NULL COMMENT '个人说明',
  coupon_count INT NOT NULL DEFAULT 0 COMMENT '可用优惠券数量',
  match_rate VARCHAR(20) DEFAULT NULL COMMENT '好评匹配率',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序用户资料表';

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '登录账号',
  password VARCHAR(200) NOT NULL COMMENT 'BCrypt 密码哈希',
  role VARCHAR(20) NOT NULL COMMENT '角色：ADMIN/MANAGER/STAFF',
  display_name VARCHAR(50) DEFAULT NULL COMMENT '显示名称',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台系统用户表';

INSERT INTO room_type
  (id, name, price, area, bed, breakfast, occupancy, status, tag, image, summary, total_rooms, available_rooms, enabled)
VALUES
  (1, '豪华大床房', 426.00, '32m²', '1张 1.8m×2.0m', '双早', '2人', 'hot', '热门',
   'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80',
   '高楼层景观房，适合情侣与商务单人入住。', 20, 8, 1),
  (2, '标准双床房', 396.00, '35m²', '2张 1.35m×2.0m', '双早', '2人', 'steady', '稳定',
   'https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?auto=format&fit=crop&w=900&q=80',
   '双床布局更适合朋友、同事出行和双人旅行。', 18, 10, 1),
  (3, '行政大床房', 888.00, '58m²', '1张 2.0m×2.0m', '双早', '2人', 'luxury', '高端',
   'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80',
   '加宽大床与商务会客空间，适合高品质住宿和商务接待。', 8, 3, 1),
  (4, '亲子家庭房', 168.00, '28m²', '1张 1.5m×2.0m + 1张 1.2m×2.0m', '无', '3人', 'budget', '特惠',
   'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=900&q=80',
   '一大一小床组合，适合亲子家庭入住。', 16, 9, 1);

INSERT INTO room_type_features (room_type_id, features) VALUES
  (1, '景观窗'), (1, '智能电视'), (1, '独立淋浴'), (1, '免费停车'),
  (2, '静音楼层'), (2, '书桌办公'), (2, '行李架'), (2, '高速WiFi'),
  (3, '会客沙发'), (3, '浴缸'), (3, '迷你吧'), (3, '欢迎水果'),
  (4, '亲子床型'), (4, '儿童用品'), (4, '宽敞空间'), (4, '卫生安心');

INSERT INTO notice (id, title, level, content, published, publish_date) VALUES
  (1, '五一假期入住温馨提示', '重要', '节假日期间入住高峰较多，建议提前在小程序完成预订与登记。', 1, CURDATE()),
  (2, '连住优惠活动上线', '活动', '连续入住两晚及以上可享95折，部分房型赠双早。', 1, CURDATE());

INSERT INTO hotel_order
  (id, wx_openid, user_phone, room_type_id, room_type_name, guest_name, guest_phone, guest_id_card, stay_nights, total_amount,
   original_amount, discount_amount, coupon_id, coupon_title, payment_status, paid_at, payment_trade_no,
   check_in_date, check_out_date, status, room_no, created_at, check_in_at, check_out_at, check_out_source)
VALUES
  ('HT20260430001', 'dev-local-device', '13800138000', 1, '豪华大床房', '张同学', '13800138000', '440101199901011234', 2, 809.40,
   852.00, 42.60, 'coupon-stay', '连住优惠券', 'paid', NOW(),
   'DEMO-PAY-20260430001', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'upcoming', NULL, NOW(), NULL, NULL, NULL),
  ('HT20260430022', 'dev-local-device', '13800138001', 1, '豪华大床房', '赵女士', '13800138001', '440101199202022345', 3, 1214.10,
   1278.00, 63.90, 'coupon-stay', '连住优惠券', 'paid', NOW(),
   'DEMO-PAY-20260430022', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'staying', '0806', NOW(), NOW(), NULL, NULL),
  ('HT20260429031', 'dev-local-device', '13800138002', 4, '亲子家庭房', '陈女士', '13800138002', '440101198803033456', 1, 168.00,
   168.00, 0.00, NULL, NULL, 'paid', DATE_SUB(NOW(), INTERVAL 1 DAY),
   'DEMO-PAY-20260429031', DATE_SUB(CURDATE(), INTERVAL 1 DAY), CURDATE(), 'finished', '0312', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 'FRONT_DESK');

INSERT INTO user_profile (id, nickname, description, coupon_count, match_rate) VALUES
  (1, '酒店住客', '欢迎再次入住悦栖酒店', 2, '93%');

INSERT INTO sys_user (id, username, password, role, display_name, enabled) VALUES
  (1, 'admin', '$2a$10$o9V.xrwapAiiLd.y.kamXeABqJ1xDJCSb7Y1wFygDUnkuv.menoDG', 'ADMIN', '系统管理员', 1),
  (2, 'manager', '$2a$10$ABLWXbj6.NWpCSpOC3Lqzed/xumDav1WAGOtLcaqufYEyHTQ/vUUq', 'MANAGER', '赵经理', 1),
  (3, 'staff', '$2a$10$EpmqQ/y3uuKkRs43kzaVNOBrsS6lWwst7s0aU4H6tWmjmHEPUrWfu', 'STAFF', '赵前台', 1);

SET FOREIGN_KEY_CHECKS = 1;
