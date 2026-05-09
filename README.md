# 酒店管理系统

这是一个酒店管理项目，包含 Spring Boot 后端、Vue 管理后台、微信小程序、数据库脚本和压测材料。

## 目录

- `backend/`: Spring Boot API 服务
- `vue-admin/`: Vue 管理后台
- `miniprogram/`: 微信小程序
- `database/`: MySQL 初始化脚本
- `tests/`: 小程序工具函数测试
- `load-tests/`: 压测脚本与报告产物

## 本地运行

1. 启动 MySQL，并确认数据库连接信息。
2. 启动后端：

```powershell
cd backend
mvn spring-boot:run
```

3. 启动管理后台：

```powershell
cd vue-admin
npm install
npm run dev
```

4. 用微信开发者工具打开 `miniprogram`。

## 环境变量

可参考 `.env.example`。常用变量：

- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- `AUTH_TOKEN_SECRET`: token 签名密钥，生产环境必须至少 32 位
- `APP_PRODUCTION`: 设为 `true` 后会启用生产启动检查
- `CORS_ALLOWED_ORIGIN_PATTERNS`: 允许访问 API 的前端域名
- `PAYMENT_CALLBACK_SECRET`: 支付回调校验密钥
- `WX_MINI_PROGRAM_SECRET`
- `WX_DEV_LOGIN_ENABLED`: 生产环境必须设为 `false`

## 默认账号

系统会初始化 3 个演示账号，数据库中保存的是 BCrypt 哈希：

- 管理员：`admin / admin123`
- 经理：`manager / manager123`
- 前台：`staff / staff123`

首次正式部署后建议立刻在“系统用户”页面修改密码。

## 验证命令

```powershell
cd backend
mvn test

cd ..\vue-admin
npm run build

cd ..
node .\tests\booking-calculator.test.js
```

## 交付注意

- 不要提交 `node_modules/`、`dist/`、`target/`、日志、截图和浏览器 profile。
- 如果手动执行 `database/hotel_management.sql`，会重建演示数据，请先备份已有数据库。
- 生产环境不要使用 `127.0.0.1` 作为小程序 API 地址，真机需改成服务器或局域网地址。
