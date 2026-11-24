# 校园场地预约系统接口文档

> 版本：v0.1-rev1 · 编制日期：2026-11-16 · 状态：设计草案，待双方评审；已按需求 v0.1-rev1 自检。
> 本文描述计划实现的接口，**不代表接口已经可调用**。当前源码只有 Spring Boot 启动类，尚无 Controller、鉴权、统一响应或业务实现。

## 1. 项目用途与设计依据

系统以实体时段（Slot）为预约单位，学生查询场地和空闲时段、提交预约、模拟支付、取消及签到；管理员维护场地、批量排期、关闭时段、管理用户与违约、查看使用统计。一个时段同一时刻最多被一条有效预约占用，自习室也按整个时段预约，本版不提供按座位预约。

依据：[需求说明书](requirements.md)、[技术选型](adr/0001-tech-stack.md)、[建表脚本](../src/main/resources/database/1.sql)。需求为业务规则来源；下文对需求未覆盖的字段、路径和边界所作选择均为设计建议，评审通过后再实现。

机器可读契约：[openapi.json](openapi.json)，OpenAPI 3.0.3，可导入 Apifox 或 Swagger Editor。Markdown 解释业务语义，OpenAPI 定义请求、响应和字段约束；修改契约时应同时更新两份文件。Swagger UI、`/v3/api-docs` 属后续集成目标，当前项目尚未引入 SpringDoc。

## 2. 通用约定

| 项目 | 约定 |
|---|---|
| 本地服务地址 | `http://localhost:8080`（当前配置端口） |
| 业务路径前缀 | `/api/v1`（拟定） |
| 请求与响应 | `application/json`，UTF-8；GET 通过 query 传参 |
| 鉴权 | `Authorization: Bearer <accessToken>` |
| 时区 | `Asia/Shanghai`；所有业务时间按北京时间比较 |
| 日期 / 时间 | `yyyy-MM-dd` / `HH:mm:ss` |
| 日期时间 | RFC 3339，例如 `2026-11-17T10:00:00+08:00` |
| ID | JSON 字符串，十进制正整数，例如 `"101"`，避免 JavaScript 大整数精度问题 |
| 金额 | 十进制字符串，例如 `"0.00"`；客户端不得指定支付金额 |
| 分页 | `page` 从 1 开始，默认 1；`size` 默认 20，范围 1～100 |
| 排序 | 场地按 id 升序；时段按开始时间升序；预约与违约按创建时间、id 倒序 |
| 查询一致性 | 可用时段仅为查询时快照，提交预约时必须重新校验 |
| 追踪 | 服务端为每个请求生成 `traceId`，写入响应及日志，不信任客户端传入值 |

### 2.1 统一响应

所有业务接口成功或失败均返回相同的外层结构，HTTP 状态与业务错误码同时表达结果。

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "items": [],
    "total": 0,
    "page": 1,
    "size": 20
  },
  "traceId": "e1ac4b506b09472c8facfe7bba454321"
}
```

非分页接口的 `data` 为对应对象；删除场地成功时为 `null`。错误时 `data` 为 `null`，参数校验失败可返回 `{"fieldErrors":[{"field":"slotId","message":"不能为空"}]}`。列表为空是正常成功，不返回 404。

### 2.2 HTTP 状态与错误码

沿用需求约定，仅使用 HTTP 200、400、401、403、404、500，不额外引入 201、204、409。下列标为“新增建议”的码需评审后纳入需求说明书。

| HTTP | code | 含义 / 适用场景 |
|---|---|---|
| 200 | 0 | 成功，包括幂等操作返回已有结果 |
| 400 | 1001 | 参数格式、类型、范围或跨字段校验失败 |
| 401 | 2001 | 未登录、账号密码错误、令牌无效或过期 |
| 403 | 2002 | 无操作权限，含账号被管理员禁用 |
| 400 | 3001 | 时段已被其他有效预约占用 |
| 400 | 3002 | 与本人已有有效预约时间重叠 |
| 400 | 3003 | 超出每日预约次数上限 |
| 403 | 3005 | 处于禁约期内，仅限制提交预约 |
| 400 | 3004 | 时段已开始或超出预约日期范围 |
| 400 | 3006 | 当前状态不允许操作，包括已关闭时段、支付过期、取消或签到不满足条件 |
| 404 | 1004 | 资源不存在，或学生访问他人的预约（新增建议） |
| 400 | 1002 | 注册账号已存在（新增建议） |
| 500 | 5000 | 服务端异常；不回传 SQL、堆栈等内部信息（新增建议） |

校验顺序建议：认证 → 角色/账号状态 → 参数 → 资源可见性 → 状态与时间 → 业务限制 → 事务写入。同一请求同时违反多条规则时，返回先检出的错误。

### 2.3 角色、枚举与字段映射

对外使用语义枚举，数据库整数仅在持久层映射。

| 字段 | 接口值 | 当前 SQL 值 |
|---|---|---|
| role | STUDENT / ADMIN | 0 / 1（2、3 暂不开放，见第 8 节） |
| venue.type | BADMINTON / STUDY_ROOM / BASKETBALL | 0 / 1 / 2 |
| venue.status | ENABLED / DISABLED | 1 / 0 |
| slot.status | AVAILABLE / OCCUPIED / CLOSED | 0 / 1 / 2 |
| booking.status | PENDING_PAYMENT / CONFIRMED / CHECKED_IN / CANCELLED / EXPIRED / NO_SHOW | 0 / 1 / 2 / 3 / 4 / 5 |

访客指未登录请求，无须注册游客账号。注册只能生成 STUDENT，客户端不得指定 role、禁约时间或账号状态。所有 `/me/**` 的用户身份来自服务端校验后的令牌，不接受客户端指定 `userId`；管理员接口才允许筛选其他用户。

## 3. 接口目录

| 方法 | 路径（省略 `/api/v1`） | 权限 | 用途 / 对应功能 |
|---|---|---|---|
| POST | `/auth/register` | 游客 | 注册 F-01 |
| POST | `/auth/login` | 游客 | 登录 F-01 |
| GET | `/me` | 已登录 | 当前用户资料 |
| GET | `/venues` | 游客 | 场地分页 F-02 |
| GET | `/venues/{venueId}` | 游客 | 场地详情 |
| GET | `/venues/{venueId}/slots` | 游客 | 指定日期时段 F-03 |
| POST | `/bookings` | STUDENT | 提交预约 F-04 |
| GET | `/me/bookings` | STUDENT | 个人预约分页 F-10 |
| GET | `/me/bookings/{bookingId}` | STUDENT | 个人预约详情 |
| POST | `/me/bookings/{bookingId}/payment` | STUDENT | 模拟支付 F-05 |
| POST | `/me/bookings/{bookingId}/cancel` | STUDENT | 取消预约 F-07 |
| POST | `/me/bookings/{bookingId}/check-in` | STUDENT | 签到 F-08 |
| GET | `/admin/venues` | ADMIN | 查询所有场地，含停用场地 F-11 |
| GET | `/admin/venues/{venueId}/slots` | ADMIN | 查询管理范围内时段，供排期核查与关闭操作 |
| POST | `/admin/venues` | ADMIN | 新增场地 F-11 |
| PUT | `/admin/venues/{venueId}` | ADMIN | 完整修改场地 F-11 |
| DELETE | `/admin/venues/{venueId}` | ADMIN | 删除无时段、无预约引用的场地 F-11 |
| POST | `/admin/venues/{venueId}/schedules` | ADMIN | 批量生成时段 F-12 |
| POST | `/admin/slots/{slotId}/close` | ADMIN | 强制关闭时段 BR-14 |
| GET | `/admin/bookings` | ADMIN | 全部预约查询 |
| GET | `/admin/users` | ADMIN | 用户分页查询 |
| PUT | `/admin/users/{userId}/status` | ADMIN | 禁用/启用学生账号 |
| GET | `/admin/no-shows` | ADMIN | 违约记录查询 F-13 |
| PUT | `/admin/users/{userId}/booking-ban` | ADMIN | 设置/解除人工禁约 F-13 |
| GET | `/admin/statistics/venue-usage` | ADMIN | 场地使用统计 F-14 |

F-06 超时释放、F-09 违约判定、BR-12 自动禁约由后台任务触发，不开放公网任务执行接口。角色访问边界按表严格校验，ADMIN 不自动获得学生预约权限。人工禁约接口及其双来源合并规则是 F-13 的可选细化建议，需求尚未规定，不能直接当作首版必做项。

本契约描述最终目标，不改变需求第 11 节的教学实施顺序：第一阶段允许刻意保留并发缺陷，第二阶段完成抢占保护、支付与超时闭环后再验收并发正确性。F-10 未在需求的实施阶段表中分配，建议第三阶段随取消、签到一起交付，仍待评审。

## 4. 公共数据对象

`?` 表示值可为 `null`，响应字段仍保留；除特别说明外字段均返回。详细类型见 OpenAPI。

| 对象 | 字段 |
|---|---|
| User | `id`、`name`、`nickname`、`role`、`status`（ENABLED/DISABLED）、`bannedUntil?`、`createdAt`；不返回 password |
| Venue | `id`、`name`、`type`、`location`、`capacity`、`openTime`、`closeTime`、`status`、`createdAt`、`updatedAt` |
| Slot | `id`、`venueId`、`slotDate`、`startTime`、`endTime`、`status`；不返回占用者身份 |
| Booking | `id`、`bookingNo`、`userId`、`venueId`、`venueName`、`slotId`、`slotDate`、`startTime`、`endTime`、`amount`、`status`、`expireAt`、`paidAt?`、`checkedInAt?`、`cancelledAt?`、`noShowAt?`、`cancelReason?`、`createdAt` |
| NoShow | `bookingId`、`userId`、`venueId`、`slotDate`、`noShowAt` |
| VenueUsage | `venueId`、`venueName`、`totalSlots`、`closedSlots`、`bookedSlots`、`checkedInSlots`、`noShowCount`、`usageRate` |

`capacity` 仅描述物理容纳人数，不作为时段库存；时段库存固定为 1。Booking 中场地名称暂按当前名称展示，不承诺保存历史名称快照。`cancelReason` 建议取 `USER_CANCELLED` 或 `ADMIN_CLOSED`；其余状态为空。

## 5. 学生与公共接口

### 5.1 注册、登录和当前用户

**POST `/auth/register`**：请求 `name`（3～16 位 ASCII 字母、数字、下划线）、`password`（8～64 字符）、`nickname`（1～16 字符，可省略，默认“某某”）。响应 `User`，注册成功后仍需登录。不允许额外字段，账号区分大小写的存储策略见第 8 节。

**POST `/auth/login`**：请求 `name`、`password`；成功返回 `accessToken`、`tokenType`（固定 Bearer）、`expiresIn`（秒，建议 7200）、`user`（User）。密码使用不可逆哈希存储，不记录明文或令牌到日志。本版不设计刷新令牌、注销及密码重置接口；令牌过期后重新登录。每次鉴权均检查账号是否启用。

**GET `/me`**：无参数，返回 `User`，展示当前账号状态和禁约截止时间。禁约不妨碍登录、查询、支付、取消或签到；禁用账号禁止访问全部需鉴权接口。

### 5.2 查询场地

**GET `/venues`**：可选 query：`page`、`size`、`name`（名称包含匹配，1～64 字符）、`type`。返回 `Page<Venue>`，仅包含 ENABLED 场地。

**GET `/venues/{venueId}`**：返回 `Venue`；不存在或停用返回 404/1004。管理员通过管理列表查询停用场地。

**GET `/venues/{venueId}/slots`**：必填 `date`，可选 `status`。返回 `Slot[]`，默认返回该日全部时段，可用 `status=AVAILABLE` 筛选。日期须在可预约日期范围内；无排期返回空数组。AVAILABLE 仅表示未占用且未关闭，不保证未开始，提交时仍需检查时间。

### 5.3 提交预约

**POST `/bookings`**，请求：

```json
{ "slotId": "101" }
```

成功返回 `Booking`，示例（示例数据，非运行结果）：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": "9001",
    "bookingNo": "BK202611170001",
    "userId": "10",
    "venueId": "1",
    "venueName": "A区3号羽毛球场",
    "slotId": "101",
    "slotDate": "2026-11-17",
    "startTime": "10:00:00",
    "endTime": "11:00:00",
    "amount": "0.00",
    "status": "PENDING_PAYMENT",
    "expireAt": "2026-11-16T12:15:00+08:00",
    "paidAt": null,
    "checkedInAt": null,
    "cancelledAt": null,
    "noShowAt": null,
    "cancelReason": null,
    "createdAt": "2026-11-16T12:00:00+08:00"
  },
  "traceId": "e1ac4b506b09472c8facfe7bba454321"
}
```

服务端从时段读取场地、日期和时间，创建待支付预约并占用时段，过期时间为创建时间加 15 分钟。检查场地启用、时段可预约、未来 14 天范围、本人时间冲突、每日上限及禁约。抢占时段和写预约应在同一事务；相同用户对不同场地的并发请求也必须满足冲突与次数约束。

本接口不承诺请求重试幂等；客户端网络超时后先查个人预约，避免盲目重发。重复提交同一已占用时段返回 3001。禁止通过请求传入 userId、venueId、amount、status 或 expireAt。

### 5.4 查询个人预约

**GET `/me/bookings`**：可选 `page`、`size`、`status`、`dateFrom`、`dateTo`；日期按时段日期筛选，包含两端，起始不得晚于结束。返回 `Page<Booking>`。

**GET `/me/bookings/{bookingId}`**：返回本人 `Booking`。查无记录或属于他人，统一返回 404/1004。

### 5.5 模拟支付、取消与签到

以下接口不带请求体，均返回最新 `Booking`。只允许操作本人记录，状态流转集中在服务层。

| 接口 | 前置条件 | 成功效果 | 重复调用 |
|---|---|---|---|
| POST `/me/bookings/{bookingId}/payment` | PENDING_PAYMENT，且当前时间严格早于 expireAt | 转 CONFIRMED，记录 paidAt，仅生成一条有效支付记录 | CONFIRMED 或 CHECKED_IN 返回已有结果；其他终态返回 3006 |
| POST `/me/bookings/{bookingId}/cancel` | CONFIRMED，且距离开始时间满足取消窗口 | 转 CANCELLED，记录 cancelledAt、USER_CANCELLED，释放时段 | 已因 USER_CANCELLED 取消则返回已有结果；其他状态返回 3006 |
| POST `/me/bookings/{bookingId}/check-in` | CONFIRMED，且在签到窗口内 | 转 CHECKED_IN，记录 checkedInAt | CHECKED_IN 返回已有结果，不覆盖首次签到时间；其他状态返回 3006 |

模拟支付不接受卡号、交易成功标识、回调或金额，不连接真实渠道。支付和超时释放必须竞争同一状态变更条件；即使后台任务延迟，过期订单也不可支付。取消与签到窗口不满足均返回 3006。首版遵循需求状态机，待支付订单不开放主动取消，等待支付或超时释放。

## 6. 管理员接口

### 6.1 场地维护

**GET `/admin/venues`**：支持公共场地列表的 query，另支持 `status`；返回 `Page<Venue>`，默认包含所有状态。

**GET `/admin/venues/{venueId}/slots`**：必填 `date`，可选 `status`，返回 `Slot[]`，按开始时间升序。允许查询停用场地及历史、未来已生成时段，不受学生可预约日期范围限制；场地不存在返回 404/1004，无排期返回空数组。此入口使管理员能取得需要关闭的 slotId，而不依赖数据库查询或公开接口。

**POST `/admin/venues`**、**PUT `/admin/venues/{venueId}`**：请求完整的 `name`（1～64）、`type`、`location`（1～128）、`capacity`（正整数）、`openTime`、`closeTime`、`status`；返回 `Venue`。不支持跨午夜营业，须 `openTime < closeTime`。PUT 所有字段必填，不采用“省略即保持”的语义。

停用场地只阻止新查询和新预约，不自动取消已有预约；已有预约仍能支付、签到和取消。营业时间调整仅影响后续排期，不回写已生成时段。需要取消预约时使用关闭时段接口。

**DELETE `/admin/venues/{venueId}`**：仅可删除无时段、无预约引用的场地，否则 400/3006；建议有历史数据时停用。成功 `data=null`；不存在返回 404/1004。不级联删除历史记录。

### 6.2 批量排期

**POST `/admin/venues/{venueId}/schedules`**，请求示例：

```json
{
  "dateFrom": "2026-11-17",
  "dateTo": "2026-11-29",
  "daysOfWeek": [1, 2, 3, 4, 5],
  "startTime": "08:00:00",
  "endTime": "22:00:00",
  "slotMinutes": 60
}
```

所有字段必填；星期按 ISO 定义 1=周一、7=周日，数组非空且不重复。建议生成日期限定在今天至今天+13天，包含两端，且 dateFrom ≤ dateTo，与第 7 节的预约窗口建议一致；例子以今天为 2026-11-16。时间在场地营业时间内，时长为正整数分钟，区间须能被时长整除，不允许跨日。当天已开始的候选区间跳过并计入 skippedCount，避免产生无法预约的排期。具体可配置时长见第 8 节，建议首版仅允许 60。

每个候选区间按 `[开始,结束)` 判断与已有时段是否重叠；重叠则跳过，包括已关闭时段。返回 `{"createdCount":20,"skippedCount":8}`，二者之和等于按日期、星期和起止时间展开的本次候选数，重复或已开始的候选区间均只计一次跳过。重复同一请求全部跳过，不覆盖记录。停用场地拒绝排期。参数不合法整批拒绝；正常重叠为跳过；非预期数据库异常整批回滚。并发排期也不能生成重叠记录，仅当前 SQL 的唯一开始时间索引不足以保证这一点。

### 6.3 强制关闭时段

**POST `/admin/slots/{slotId}/close`**：请求 `reason`（必填，1～200 字符）。返回 `Slot`，状态 CLOSED。

按 BR-14，在同一事务内关闭时段并取消其 PENDING_PAYMENT 或 CONFIRMED 预约，记录 ADMIN_CLOSED，不计违约；保留支付历史，不真实退款。BR-09 规定已签到不可取消，而 BR-14 未明确管理员是否例外，因此本草案保守遵循 BR-09：若存在 CHECKED_IN 预约，整次关闭拒绝，返回 400/3006，不修改预约或时段；该交叉规则须双方评审后冻结。时段已经关闭时返回已有结果，不覆盖首次原因和时间。历史终态预约不变，关闭与支付、签到、释放之间须保证原子一致性。本版不设计重新开放接口。

### 6.4 全部预约、用户和违约

**GET `/admin/bookings`**：可选 `page`、`size`、`userId`、`venueId`、`status`、`dateFrom`、`dateTo`，返回 `Page<Booking>`，日期按时段日期筛选。

**GET `/admin/users`**：可选 `page`、`size`、`name`、`status`，返回 `Page<User>`；name 为账号包含匹配。不返回密码、令牌或哈希。

**PUT `/admin/users/{userId}/status`**：请求 `status`（ENABLED/DISABLED）、`reason`（1～200 字符），返回 `User`。仅操作 STUDENT，禁止禁用管理员；启用不清除禁约期限，禁用不主动取消已有预约。重复设置同一状态成功。

**GET `/admin/no-shows`**：可选 `page`、`size`、`userId`、`venueId`、`dateFrom`、`dateTo`，返回 `Page<NoShow>`；日期按时段日期筛选。违约记录由 NO_SHOW 预约投影，不开放直接新增、删除接口。

**PUT `/admin/users/{userId}/booking-ban`（可选建议，待评审）**：请求 `bannedUntil`（必传，可为 null，首次设置的非空值须晚于当前时间）、`reason`（1～200 字符），返回 `User`。非空设置人工禁约，null 解除人工禁约；仅作用于 STUDENT。有效禁约取自动禁约与人工禁约的较晚截止时间，人工解除不绕过仍有效的自动处罚。重复相同已存值时返回成功，即使重试时该截止时间已过去，不延长禁约。自动禁约计算细节见第 8 节；若不采纳人工禁约扩展，则移除此接口，仅保留 BR-12 的自动处罚与 F-13 的查询能力。

### 6.5 场地使用统计

**GET `/admin/statistics/venue-usage`**：必填 `dateFrom`、`dateTo`，可选 `venueId`；日期包含两端，最多 31 个日历日。返回 `VenueUsage[]`，有场地但无排期返回零值；指定不存在的场地返回 404/1004。

按时段日期分组、每场地一条：`totalSlots` 为生成的全部时段数；`closedSlots` 为当前 CLOSED 数；`bookedSlots` 为当前有 CONFIRMED、CHECKED_IN 或 NO_SHOW 预约的不同 slotId 数；`checkedInSlots` 为 CHECKED_IN 的不同 slotId 数；`noShowCount` 为 NO_SHOW 预约数。`usageRate = checkedInSlots / (totalSlots - closedSlots)`，分母为 0 时取 0，数值在 0～1，保留四位小数。本版统计表示查询时当前记录状态，不是不可变的历史经营报表，管理员关闭后的统计可能变化。

## 7. 状态、时间边界与验收

建议统一以下边界，冻结前需完成第 8 节评审：

- 可预约日期：`今天 ≤ slotDate ≤ 今天+13天`，且当前时间严格早于开始时间。
- 时间重叠：`A.start < B.end && B.start < A.end`，相邻时段不重叠；是否允许连续预约仍待确认。
- 每日次数暂建议按时段所属日期计算，PENDING_PAYMENT、CONFIRMED、CHECKED_IN、NO_SHOW 计数；CANCELLED、EXPIRED 不计。BR-04 仅明确“成功预约”和“取消不计”，未明确待支付、失效及按哪一天统计，以上是待确认口径，不是需求原文。计数和新建须避免并发突破上限 2。
- 支付：`now < expireAt`；超时任务按 `now >= expireAt` 处理。签到：`start-15分钟 ≤ now ≤ start+15分钟`；违约任务仅在 `now > start+15分钟` 执行。
- 用户取消建议边界为 `now ≤ start-2小时`，即恰好提前 2 小时允许；需求“以上”与“以内”在等号处重叠，须冻结确认。
- 临近开始预约仍按创建后 15 分钟到期。由于创建必须早于开始，且支付必须早于到期，合法支付必然早于开始后 15 分钟，不存在“合法支付时已经超过签到窗口”的场景。服务端必须使用一致的时钟与实际处理时刻判断，不能因后台任务延迟而允许过期支付。

| 触发方 | 状态流转 | 时段效果 |
|---|---|---|
| 学生创建 | 新建 → PENDING_PAYMENT | AVAILABLE → OCCUPIED |
| 模拟支付 | PENDING_PAYMENT → CONFIRMED | 保持 OCCUPIED |
| 超时任务 | PENDING_PAYMENT → EXPIRED | 释放为 AVAILABLE |
| 学生取消 | CONFIRMED → CANCELLED | 释放为 AVAILABLE |
| 学生签到 | CONFIRMED → CHECKED_IN | 保持 OCCUPIED |
| 违约任务 | CONFIRMED → NO_SHOW | 释放为 AVAILABLE |
| 管理员关闭 | PENDING_PAYMENT / CONFIRMED → CANCELLED；CHECKED_IN 暂拒绝关闭 | 成功时转 CLOSED，不能被释放任务改回 AVAILABLE |

已签到为需求定义的终态，不新增 COMPLETED。到时后的历史 OCCUPIED 记录不再可预约。后台任务和重复请求不得重复释放、重复记违约、覆盖首次操作时间；支付须保证一条预约只有一条有效支付记录。

接口实现后的重点验收场景：

1. 注册登录 → 查询场地/时段 → 创建 → 支付 → 签到，核对响应与数据库状态一致。
2. 50 个请求争抢同一时段，仅 1 个成功，其余 3001；按需求连续 100 轮验证。还需验证同一用户并发预约不同场地不会绕过时间冲突和每日上限。
3. 同一预约支付 10 次，支付记录仅 1 条；支付与超时任务竞争，只能有一种终态结果。
4. 未登录 401/2001，学生调管理接口 403/2002，访问他人预约 404/1004；禁用后旧令牌也无法访问鉴权接口。
5. 覆盖取消的 2 小时等号、签到两端、支付截止等号、14 天日期边界；后台任务晚执行时规则仍有效。
6. 关闭已占用时段会取消预约且不计违约，与支付及超时释放并发后仍为 CLOSED。
7. 重复排期无重复时段；部分重叠正确跳过；无数据列表返回空页；自动禁约不被重复扫描无限续期。
8. 关闭含已签到预约的时段暂返回 3006 且数据不变；管理员可查询停用场地和历史时段。
9. 按需求验证本地查询耗时低于 200ms、写入低于 500ms，并检查 traceId 日志；第三阶段补充定时对账，检查预约与时段状态一致性。

以上为计划验收项，本次文档设计未执行 HTTP、并发或业务测试。

## 8. 冻结契约前的待确认事项与实现差距

| 事项 | 当前证据 | 本文建议 / 所需工作 |
|---|---|---|
| 角色模型 | 需求只有 STUDENT/ADMIN；SQL 另有场馆管理员和游客 | 首版只开放 0/1，其他值拒绝鉴权，暂不增加权限层级 |
| 时间边界与连续预约 | 需求仍有待定问题，取消等号有歧义 | 评审第 7 节建议；连续预约建议允许，仍受每日上限约束 |
| 14 天窗口 | 需求未明确是否包含今天、采用日历日还是滚动 336 小时 | 本文建议今天至今天+13天，查询、预约、排期统一；未冻结前不能称为需求已确定规则 |
| 每日次数口径 | BR-04 只明确成功预约最多 2 次、取消不计 | 确认按预约创建日还是使用日，以及待支付、失效是否计数；第 7 节仅为暂定方案 |
| 时段时长 | 需求待定，SQL 支持任意起止 | 首版 slotMinutes 仅 60；后续放开必须同步重叠保护 |
| 模拟支付金额 | 需求待定，SQL 已有 amount | 首版金额由服务端固定为 0.00，只训练状态机，不开放定价与退款接口 |
| 支付记录 | SQL 只有预约支付时间，无支付记录表 | 需补支付记录及 bookingId 唯一约束，满足支付幂等验收 |
| 管理员初始化 | 需求待定 | 建议受控初始化脚本，注册不允许选择角色 |
| 自动禁约触发 | 需求未说明重复扫描是否延长、如何解除 | 建议按每条新违约事件统计滚动 30 天，达到 3 次时自判定时刻禁约 7 天，同一事件只处罚一次；新增违约可延长 |
| 人工与自动禁约 | 需求未明确要求人工设置任意期限，SQL 只有 banned_until | 人工禁约与双来源合并属于可选设计，有额外存储和审计成本；评审不采纳则删除接口，不影响 BR-12 自动禁约 |
| 账号禁用与审计 | SQL 没有用户 status、关闭原因、取消原因或审计表 | 补充迁移及审计记录，包含操作人、对象、原因、时间；不直接认为字段已存在 |
| 账号大小写 | SQL 未显式指定 name 排序规则 | 建议统一按不区分大小写处理登录与唯一性，注册保留原展示值；明确排序规则后冻结 |
| 账号禁用范围 | 需求只写禁用用户，未细分是否允许存量预约支付、取消和签到 | 本文暂按全部鉴权操作被阻止；可能造成无法签到并被判违约，须确认处理规则后再实现 |
| BR-14 管理员关闭 | BR-09 禁止取消已签到，BR-14 未明确例外 | 本草案暂拒绝关闭已签到的时段；如需强制取消必须先在需求中明确例外，真实退款仍暂缓 |
| 并发约束 | SQL 无有效预约唯一约束、无用户维度并发保护，时段索引仅保护相同开始时间 | 实现时补事务与一致性保护；建表成功不等于并发正确 |
| 文档与脚手架差异 | README/ADR 写 JDK21、Boot3；pom 实际 java.version=17、Boot4.1.2；数据库名 README 与 SQL 不同 | 属已有差异，本次不修改构建或数据库配置；实现前单独对齐 |

需求新增建议（人工禁约语义、字段约束、统计口径、错误码等）必须经双方评审后同步到 `requirements.md`。本文不代替需求冻结，也未改动数据库或业务代码。
