# 酒店管理系统数据库

数据库脚本：

```text
hotel_management.sql
```

当前脚本包含：

- `room_type`：房型表
- `room_type_features`：房型特色表
- `hotel_order`：酒店订单表
- `notice`：公告表
- `user_profile`：小程序用户资料表

本机当前 MySQL 端口是 `3306`。如果你的数据库 root 密码已知，可以在当前目录执行：

```powershell
mysql --host=127.0.0.1 --port=3306 --user=root --password=你的密码 < hotel_management.sql
```

后端现在默认直接连接 MySQL/MariaDB，并会优先使用数据库 `hotel_management`。如果你想用 Navicat 导入：

1. 连接 `127.0.0.1:3306`
2. 新建或选中数据库 `hotel_management`
3. 运行 [hotel_management.sql](C:/Users/HY/Desktop/jiudianguanli/database/hotel_management.sql)
4. 再启动 [backend](C:/Users/HY/Desktop/jiudianguanli/backend)
