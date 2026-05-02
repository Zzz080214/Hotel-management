# 酒店管理系统后端

这是为当前毕业设计补充的 Spring Boot 后端，默认只使用 MySQL，接口前缀为 `http://127.0.0.1:8080/api`，可以直接对接微信小程序并用 Navicat 查看数据。

## 运行

双击运行：

- `start-backend.bat`：启动后端并打开房型接口
- `stop-backend.bat`：停止 8080 端口上的后端服务

命令行运行：

```powershell
cd backend
mvn spring-boot:run
```

启动前请确保本机 MySQL 服务已开启，默认配置如下：

- 主机：`127.0.0.1`
- 端口：`3306`
- 数据库：`hotel_management`
- 用户名：`root`
- 密码：`12345678`

如果数据库还没建好，Spring Boot 会自动创建 `hotel_management`；你也可以先用 Navicat 执行 [database/hotel_management.sql](C:/Users/HY/Desktop/jiudianguanli/database/hotel_management.sql) 初始化完整演示数据。

启动后可访问：

- 小程序接口：`http://127.0.0.1:8080/api/wx/rooms`
- 管理端接口：`http://127.0.0.1:8080/api/admin/dashboard`

## Navicat 查看数据

1. 在 Navicat 新建 MySQL 连接，主机填 `127.0.0.1`，端口填 `3306`
2. 用户名填 `root`，密码填 `12345678`
3. 打开数据库 `hotel_management`
4. 重点演示的表有：`room_type`、`room_type_features`、`hotel_order`、`notice`、`user_profile`

## 已实现接口

小程序端：

- `GET /api/wx/rooms/recommend?limit=3`
- `GET /api/wx/rooms`
- `GET /api/wx/rooms/{id}`
- `GET /api/wx/notices`
- `POST /api/wx/orders`
- `GET /api/wx/orders/my?status=upcoming`
- `GET /api/wx/users/profile`

后台管理端：

- `GET /api/admin/dashboard`
- `GET /api/admin/room-types`
- `POST /api/admin/room-types`
- `PUT /api/admin/room-types/{id}`
- `DELETE /api/admin/room-types/{id}`
- `GET /api/admin/orders`
- `POST /api/admin/orders`
- `POST /api/admin/orders/{id}/check-in`
- `POST /api/admin/orders/{id}/check-out`
- `POST /api/admin/orders/{id}/cancel`
- `GET /api/admin/notices`
- `POST /api/admin/notices`
- `PUT /api/admin/notices/{id}`
- `DELETE /api/admin/notices/{id}`

所有接口默认返回统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```
