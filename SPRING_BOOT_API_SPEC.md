# 小程序对接 Spring Boot 接口约定

当前小程序前端已经改成“优先调用后端接口，失败自动回退本地 mock 数据”。

## 基础地址

- 微信开发者工具本地调试默认地址：`http://127.0.0.1:8080/api`
- 真机调试时不要用 `127.0.0.1`，应改成你电脑局域网 IP，例如：`http://192.168.1.20:8080/api`
- 当前配置位置：`miniprogram/app.js`

## 返回格式

前端兼容两种格式：

1. 直接返回数组或对象
2. 统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

其中 `code` 为 `200` 或 `0` 时前端判定为成功。

## 接口列表

### 1. 推荐房型

- `GET /wx/rooms/recommend?limit=3`

返回单个房型字段建议：

```json
{
  "id": 1,
  "name": "豪华大床房",
  "price": 428,
  "area": "32m²",
  "bed": "1.8米大床",
  "breakfast": "双早",
  "occupancy": "2人",
  "status": "hot",
  "tag": "热门",
  "image": "https://...",
  "summary": "高楼层景观房",
  "features": ["景观窗", "智能电视"]
}
```

### 2. 房型列表

- `GET /wx/rooms`

### 3. 房型详情

- `GET /wx/rooms/{id}`

### 4. 公告列表

- `GET /wx/notices`

返回字段建议：

```json
{
  "id": 1,
  "title": "五一假期入住温馨提示",
  "level": "重要",
  "content": "节假日期间入住高峰较多..."
}
```

### 5. 微信登录

- `POST /wx/auth/login`

请求体：

```json
{
  "code": "wx.login 返回的 code",
  "nickname": "微信昵称",
  "avatarUrl": "头像地址",
  "devOpenid": "本地开发兜底标识"
}
```

### 6. 提交订单

- `POST /wx/orders`

请求体：

```json
{
  "roomTypeId": 1,
  "roomTypeName": "豪华大床房",
  "guestName": "张三",
  "guestPhone": "13800138000",
  "stayNights": 2,
  "totalAmount": 856,
  "checkInDate": "2026-04-30",
  "checkOutDate": "2026-05-02"
}
```

### 7. 我的订单

- `GET /wx/orders/my`
- 可选参数：`status=upcoming|staying|finished|cancelled`
- 建议需要登录 token

返回字段建议：

```json
{
  "id": "HT20260430001",
  "guest": "张三",
  "roomName": "豪华大床房",
  "amount": 856,
  "checkInDate": "2026-04-30",
  "checkOutDate": "2026-05-02",
  "status": "upcoming"
}
```

### 8. 个人中心

- `GET /wx/users/profile`
- 建议需要登录 token

返回字段建议：

```json
{
  "nickname": "酒店住客",
  "description": "欢迎再次入住",
  "orderCount": 3,
  "couponCount": 2,
  "matchRate": "93%"
}
```

## JWT 建议

如果你后端已经做了 JWT：

- 登录成功后把 token 保存到：`wx.setStorageSync("token", token)`
- 小程序请求层会自动在需要鉴权的接口上带上：

```http
Authorization: Bearer xxxxx
```

## 当前已接入页面

- 首页：房型推荐、公告
- 房型列表：全部房型
- 预订页：房型详情、提交订单
- 我的订单：订单列表
- 个人中心：用户资料
