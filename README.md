# campus-booking · 校园场地与自习室时段预约

两个人从零搭的 Java Web 实战项目。目的不是尽快上线，而是**把 Java Web 该踩的坑一个个踩完**。

## 这是什么

学生按「日期 + 场地 + 时段」查看空位并预约；管理员维护场地与开放时段。

它被选中的原因是：每一条需求都长在技术上 —— 并发争抢、状态流转、超时释放、热点查询、多层权限，一个都不缺。

## 技术栈

JDK 21 · Spring Boot 3.x · MyBatis(-Plus) · MySQL 8 · JWT · SpringDoc

详细选型和「否掉了什么」见 [ADR-0001](docs/adr/0001-tech-stack.md)。

## 本地怎么跑起来

```bash
# 1. 克隆
git clone <仓库地址>
cd campus-booking

# 2. 准备数据库
#    本地建库 campus_booking，字符集 utf8mb4
mysql -u root -p -e "CREATE DATABASE campus_booking DEFAULT CHARSET utf8mb4;"

# 3. 配置本地参数（这个文件不会入库）
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
#    然后把里面的数据库账号密码改掉

# 4. 启动
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 5. 打开接口文档
#    http://localhost:8080/swagger-ui/index.html
```

> 第 3–5 步在阶段 1 完成脚手架后才可用，现在仓库还只有规范没有代码。

## 目录结构（约定，脚手架落地后照此执行）

```
campus-booking/
├── .github/                    PR 模板、Issue 模板、CI 流水线
├── docs/
│   ├── adr/                    架构决策记录，每个技术选型一页
│   └── ...                     设计文档、ER 图、复盘
├── src/main/java/com/campus/booking/
│   ├── common/                 统一返回体、异常、工具类（谁都能用，改之前打招呼）
│   ├── auth/                   登录、JWT、拦截器
│   ├── venue/                  场地与时段
│   ├── booking/                预约核心（难点都在这里）
│   └── admin/                  后台管理
├── src/test/java/              测试与主代码同包同结构
├── CONTRIBUTING.md             ★ 协作规范，开工前必读
└── README.md
```

**按业务模块分包，不按技术层分包。** 不要有 `controller/` `service/` `mapper/` 三个大包 —— 那样每加一个功能要横跨三个包，两个人必然天天改同一批目录、天天冲突。

## 开工前必读

1. [CONTRIBUTING.md](CONTRIBUTING.md) —— 分支怎么开、提交怎么写、PR 怎么发、Review 怎么做
2. [docs/adr/0001-tech-stack.md](docs/adr/0001-tech-stack.md) —— 为什么选这套栈

## 当前进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| 0 | 仓库与协作规范 | 进行中 |
| 1 | 脚手架 + 跑通一条垂直切片 | 未开始 |
| 2 | 痛点攻坚（并发、缓存、幂等、限流） | 未开始 |
| 3 | 工程质量（测试、日志、CI、部署） | 未开始 |
| 4 | 架构演进（模块化单体 → 拆一个服务） | 未开始 |
