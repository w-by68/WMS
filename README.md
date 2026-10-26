# 智能仓储管理系统（WMS）

前后端分离的仓储管理系统，覆盖基础数据维护、入库/出库业务、库存锁定与流水、库存预警、数据看板与操作日志。

后端 Spring Boot 3 + MyBatis-Plus + Spring Security(JWT) + MySQL 8，前端基于 Arco Design Pro Vue（Vue 3 + TypeScript + Vite）。

---

## 功能概览

### 管理员（ADMIN）

| 模块 | 能力 |
|---|---|
| 数据看板 | 商品/库存总量、今日出入库、待审核单、未处理预警、出入库趋势、分类库存占比 |
| 用户管理 | 创建/修改/删除用户、启停账号、重置密码（系统不开放注册） |
| 基础数据 | 仓库、库位、商品分类、商品、供应商、客户 |
| 出入库审核 | 入库/出库单审核通过或驳回，通过后自动变更库存并写流水 |
| 库存盘点 | 按库位调整账面数量，记录 `ADJUST` 流水 |
| 预警规则 | 安全库存、最高库存、呆滞天数配置 |
| 预警记录 | 处理 / 忽略预警，手动触发全量扫描 |
| 操作日志 | 按操作人、模块、结果、时间查询 |

### 普通用户（USER）

| 模块 | 能力 |
|---|---|
| 首页看板 | 数据看板只读 |
| 库存查询 | 按商品/仓库筛选、只看低于安全库存、查看商品库存分布 |
| 库存预警 | 查看预警列表（含建议补货量/消化量），无处理权限 |
| 入库单 | 新建/编辑草稿、提交审核、作废、查看详情（仅本人单据） |
| 出库单 | 同上；制单时实时显示可用库存，超量标红并阻止提交 |
| 个人中心 | 查看资料、修改姓名/手机/邮箱、修改密码 |

---

## 技术栈

**后端**

- JDK 17、Maven
- Spring Boot 3.2.12（Web / Security / Validation / AOP / Actuator）
- MyBatis-Plus 3.5.12（含 `mybatis-plus-jsqlparser` 分页插件）
- MySQL 8.0（`mysql-connector-j` 8.3.0）
- JWT：jjwt 0.12.6
- 接口文档：springdoc 2.5.0（Swagger UI）
- Lombok 1.18.48（在 `annotationProcessorPaths` 中显式声明，兼容 JDK 23+）

**前端**

- Vue 3.2.40 + TypeScript 4.8 + Vite 3.2
- Arco Design Vue 2.44、Arco Design Pro Vue 模板
- Pinia、vue-router 4、axios、ECharts 5、dayjs

---

## 目录结构

```
WMS/
├── springBoot/                       # 后端 Maven 工程（com.example.wms）
│   ├── src/main/java/com/example/wms/
│   │   ├── controller/               # REST 接口
│   │   ├── service/ impl/            # 业务逻辑
│   │   ├── mapper/                   # MyBatis-Plus Mapper
│   │   ├── entity/                   # 数据库实体
│   │   ├── dto/ vo/                  # 入参 / 出参模型
│   │   ├── security/                 # JWT 过滤器、登录主体、工具类
│   │   ├── aspect/                   # 操作日志切面
│   │   ├── config/                   # Security、MyBatis-Plus 配置
│   │   ├── common/                   # 统一返回 R、分页、异常、错误码
│   │   └── enums/                    # 单据状态、出入库类型等枚举
│   └── src/main/resources/
│       ├── application.yml           # 通用配置（端口 8080、JWT、MyBatis-Plus）
│       └── application-dev.yml       # 数据源配置
├── arco/hello-arco-pro/              # 前端工程
│   ├── src/api/                      # 接口封装（axios + 拦截器）
│   ├── src/views/
│   │   ├── dashboard/                # 数据看板
│   │   ├── biz/                      # 库存查询、预警、出入库列表与制单页
│   │   ├── admin/                    # 管理端 11 个页面
│   │   ├── profile/                  # 个人中心
│   │   └── login/ exception/         # 登录页、403
│   ├── src/router/routes/modules/    # 按角色划分的路由
│   └── config/vite.config.dev.ts     # dev 端口 5173 + /api 代理到 8080
├── deploy/nginx.conf                 # 生产部署：静态托管 + /api 反代
├── 01-schema.sql                     # 建库建表脚本
├── 02-init-data.sql                  # 初始化账号与演示数据
└── 接口清单.md                        # REST API 契约
```

---

## 快速开始

### 1. 初始化数据库

MySQL 8.0，字符集 `utf8mb4`。依次执行（会先 `DROP TABLE`，请勿在已有数据的库上直接跑）：

```bash
mysql -uroot -p < 01-schema.sql
mysql -uroot -p -D wms_db < 02-init-data.sql
```

### 2. 启动后端

修改 [application-dev.yml](springBoot/src/main/resources/application-dev.yml) 中的数据库账号密码后：

```bash
cd springBoot
mvn clean package -DskipTests
java -jar target/wms-server.jar
```

- 服务端口：`8080`
- Swagger UI：http://localhost:8080/swagger-ui.html
- 启动成功标志：日志出现 `Started WmsApplication` 与 `HikariPool-1 - Start completed`

### 3. 启动前端

```bash
cd arco/hello-arco-pro
npm install
npm run dev
```

- 访问：http://localhost:5173
- 开发期由 Vite 将 `/api` 代理到 `http://localhost:8080`，前端只请求同源路径

### 4. 默认账号

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | 123456 | 管理员 |
| operator01 | 123456 | 普通用户（张仓管） |
| operator02 | 123456 | 普通用户（李出库） |

> 首次登录后请及时修改密码（个人中心支持自助修改）。

---

## 接口约定

- 统一前缀 `/api`，除 `POST /api/auth/login` 外均需请求头 `Authorization: Bearer <token>`
- 统一返回体：`{ code, message, data, timestamp }`，HTTP 状态码恒为 200，以 `code` 判断结果
- 分页返回：`data = { list, total, pageNum, pageSize }`

| code | 含义 |
|---|---|
| 200 | 成功 |
| 1001 | 参数校验失败 |
| 1002 | 未登录 / token 失效 / 用户名或密码错误 |
| 1003 | 账号已禁用 |
| 1004 | 无权限 |
| 2001 | 业务冲突（用户名/SKU/编码重复、库存不足） |
| 2002 | 库存不足 |
| 2003 | 单据状态不允许当前操作 |
| 2004 | 数据被引用，无法删除 |
| 500 | 系统异常 |

主要接口分组：`/api/auth`、`/api/users`、`/api/warehouses`、`/api/locations`、`/api/product-categories`、`/api/products`、`/api/suppliers`、`/api/customers`、`/api/inventory`、`/api/inbound-orders`、`/api/outbound-orders`、`/api/stock-transactions`、`/api/alert-rules`、`/api/alerts`、`/api/dashboard`、`/api/operation-logs`。完整字段与权限见 [接口清单.md](接口清单.md)。

---

## 核心业务规则

- **单据状态机**：`DRAFT → PENDING → APPROVED / REJECTED`；仅 `PENDING`（待审核）可作废为 `CANCELED`；仅 `DRAFT` / `REJECTED` 可编辑删除
- **出库锁定**：提交出库单时 `locked_quantity += n` 并写 `LOCK` 流水；审核通过时扣减 `quantity` 与 `locked_quantity`；驳回/作废时释放锁定（`UNLOCK`）
- **入库**：审核通过后增加库存，写 `INBOUND` 流水
- **并发控制**：库存变更基于 `version` 字段乐观锁，冲突时提示重试
- **流水可追溯**：每次库存变更记录变动前后数量、业务类型与单号
- **数据范围**：普通用户只能查看和操作自己创建的单据；主数据维护仅管理员
- **预警**：缺货（低于安全库存）、积压（高于最高库存）、呆滞（超期无出库）三类，扫描时按商品+仓库去重

---

## 打包与部署

### 后端

```bash
cd springBoot
mvn clean package -DskipTests
# 产物：target/wms-server.jar
java -jar target/wms-server.jar
```

### 前端

```bash
cd arco/hello-arco-pro
npm run build        # vue-tsc 类型检查 + Vite 构建
# 产物：dist/
```

### Nginx 反向代理

配置见 [deploy/nginx.conf](deploy/nginx.conf)，实际部署副本位于 `D:\nginx-1.30.6\conf\conf.d\wms.conf`：

| 规则 | 说明 |
|---|---|
| `location /api/` | 反代到 `http://127.0.0.1:8080`，保留原路径 |
| `root` | 指向前端 `dist` 目录 |
| `try_files $uri $uri/ /index.html` | SPA history 路由刷新不 404 |
| `gzip_static on` | 复用构建产物的 `.gz` 预压缩文件 |
| `/assets/` | 带 hash 的资源缓存 30 天，`index.html` 不缓存 |

部署步骤：

```bash
# 1. 配置纳入 nginx http 块（conf/nginx.conf 内）
include conf.d/*.conf;

# 2. 校验并启动
nginx -t
nginx

# 3. 访问
http://localhost/
```

`server_name` 需按实际域名/服务器 IP 调整；Linux 部署时同步修改 `root` 路径。

---

## 已知限制与后续项

- 运维项（统一以 `TODO(08-运维)` 标注）：日志文件滚动与采集、Actuator 端点暴露、优雅停机、Docker/CI 流水线、HTTPS 与证书
- `application-dev.yml` 中数据库账号密码为明文示例配置，生产环境应改为环境变量或外部配置中心注入
- 库位批量生成接口后端已提供，前端暂未开放入口
- 前端生产环境 `.env.production` 为空，依赖 Nginx 将 `/api` 反代到后端
