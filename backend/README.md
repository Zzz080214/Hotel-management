# 酒店管理系统后端

这是酒店管理系统的 Spring Boot 后端，默认使用 MySQL，接口前缀为 `http://127.0.0.1:8080/api`，可对接 Vue 管理后台和微信小程序。

## 运行

双击运行：

- `start-backend.bat`：启动后端并打开房型接口
- `stop-backend.bat`：停止 8080 端口上的后端服务

命令行运行：

```powershell
cd backend
mvn spring-boot:run
```

启动前请确保本机 MySQL 服务已开启。数据库连接建议通过环境变量配置：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

如果数据库还没建好，Spring Boot 会自动创建 `hotel_management`；你也可以先用 Navicat 执行 [database/hotel_management.sql](C:/Users/HY/Desktop/jiudianguanli/database/hotel_management.sql) 初始化完整演示数据。

启动后可访问：

- 小程序接口：`http://127.0.0.1:8080/api/wx/rooms`
- 管理端接口：`http://127.0.0.1:8080/api/admin/dashboard`

## 安全配置

后台密码使用 BCrypt 保存。默认演示账号如下，首次正式部署后建议立即修改：

- `admin / admin123`
- `manager / manager123`
- `staff / staff123`

token 为带签名和过期时间的本地 JWT 风格令牌。生产部署建议设置：

- `APP_PRODUCTION=true`
- `AUTH_TOKEN_SECRET`：至少 32 位随机字符串
- `PAYMENT_CALLBACK_SECRET`：支付回调校验密钥
- `WX_DEV_LOGIN_ENABLED=false`
- `CORS_ALLOWED_ORIGIN_PATTERNS`：只允许真实前端域名

如果 `APP_PRODUCTION=true` 但仍使用默认密钥或开启微信开发登录，应用会拒绝启动。

## Navicat 查看数据

1. 在 Navicat 新建 MySQL 连接，主机填 `127.0.0.1`，端口填 `3306`
2. 用户名和密码按你的 `DB_USERNAME`、`DB_PASSWORD` 填写
3. 打开数据库 `hotel_management`
4. 重点演示的表有：`room_type`、`room_type_features`、`hotel_order`、`notice`、`user_profile`、`sys_user`

## 已实现接口

小程序端：

- `GET /api/wx/rooms/recommend?limit=3`
- `GET /api/wx/rooms`
- `GET /api/wx/rooms/{id}`
- `GET /api/wx/notices`
- `POST /api/wx/auth/login`
- `POST /api/wx/auth/logout`
- `GET /api/wx/auth/me`
- `POST /api/wx/orders`
- `GET /api/wx/orders/my?status=upcoming`
- `GET /api/wx/orders/{id}`
- `POST /api/wx/orders/{id}/pay`
- `POST /api/wx/orders/{id}/face-check-in`
- `POST /api/wx/orders/{id}/self-check-out`
- `POST /api/wx/orders/{id}/cancel`
- `GET /api/wx/users/profile`

后台管理端：

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/admin/dashboard`
- `GET /api/admin/dashboard/operations/export`
- `GET /api/admin/room-types`
- `POST /api/admin/room-types`
- `PUT /api/admin/room-types/{id}`
- `DELETE /api/admin/room-types/{id}`
- `GET /api/admin/orders`
- `POST /api/admin/orders`
- `POST /api/admin/orders/{id}/confirm-payment`
- `POST /api/admin/orders/{id}/check-in`
- `POST /api/admin/orders/{id}/check-out`
- `POST /api/admin/orders/{id}/cancel`
- `GET /api/admin/notices`
- `POST /api/admin/notices`
- `PUT /api/admin/notices/{id}`
- `DELETE /api/admin/notices/{id}`
- `GET /api/admin/users`
- `POST /api/admin/users`
- `PUT /api/admin/users/{id}`
- `POST /api/payments/wechat/callback`

## 金额与支付确认

订单金额由后端按房型价格、入住晚数和后端白名单优惠券规则重新计算，不采纳前端传入的 `totalAmount`、`originalAmount`、`discountAmount`。当前演示优惠规则：

- `coupon-new`：满 399 减 60
- `coupon-stay`：连续入住 2 晚及以上 95 折

小程序下单后订单默认为 `pending`。支付成功必须通过支付确认接口：

- 小程序演示支付：`POST /api/wx/orders/{id}/pay`
- 后台前台收款：`POST /api/admin/orders/{id}/confirm-payment`
- 支付网关回调：`POST /api/payments/wechat/callback`

支付回调需要携带 `X-Payment-Callback-Secret` 请求头，且回调金额必须等于后端订单应付金额，否则拒绝确认支付。

## 并发与库存

下单时会对房型行加数据库写锁，并在库存大于 0 时才扣减库存，避免并发下单导致库存变负数。订单号使用日期、毫秒时间和随机后缀生成，避免“当天订单数量 + 1”在并发下撞号。

所有接口默认返回统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```
