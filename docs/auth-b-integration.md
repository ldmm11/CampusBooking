# B 部分与 A 部分的合并接口

B 侧新增的文件均位于 `auth` 包，未修改 A 的用户表、注册或登录实现。合并时 A 侧需要：

1. 实现 `AuthUserLookup.findById(long)` 并注册为 Spring Bean。每次受保护请求都会查询一次，以便禁用账号的旧令牌立即失效。不存在的用户返回 `Optional.empty()`。
2. 查询结果映射为 `AuthUser`：`id` 为十进制字符串，`role` 为 `STUDENT` 或 `ADMIN`，`status` 为 `ENABLED` 或 `DISABLED`；不要包含密码。`GET /me` 原样通过统一 `ApiResponse` 返回该对象。
3. 登录验证密码和启用状态后，调用 `jwtService.issue(userId)`，响应 `accessToken`、`tokenType: Bearer`、`expiresIn: jwtService.expiresIn()` 和 `user`。注册接口无需调用 JWT。
4. 部署时配置环境变量 `AUTH_JWT_SECRET`（至少 32 个 UTF-8 字节）。未配置时每次启动使用随机密钥，重启后旧令牌失效。可通过 `AUTH_JWT_EXPIRES_IN` 调整有效秒数，默认 7200。

受保护路径为 `/me`、`/me/**`、`/admin`、`/admin/**`、`/bookings`、`/bookings/**`。缺失、损坏或过期令牌，以及用户不存在，统一为 HTTP 401 / 2001；禁用账号和非管理员访问 `/admin/**` 为 HTTP 403 / 2002。现有 OpenAPI 契约已描述这些响应，B 侧没有改动它。

当前仓库没有 A 侧用户查询 Bean，因此单独运行 B 分支时，带有效令牌的 `/me` 仍会得到 401；合并 A 侧实现后才有真实用户资料。`AuthWebTest` 用测试用户 Bean 验证了完整链路。
