# WMS 故障复盘（Postmortem）

> 运维岗面试里，最能证明你「真的干过运维」的不是装了什么中间件，
> 而是你能讲清楚：**故障怎么发现的、怎么定位的、根因是什么、之后怎么防的**。
>
> 格式固定：现象 → 定位过程 → 根因 → 修复 → 改进项（防止复发）
> 不追责、只追改进项。每篇控制在 15 行以内，面试 3 分钟能讲完。

---

## 事故 1：Java 进程被系统杀掉，接口全 502

- **时间**：2026-10-XX
- **发现方式**：页面能打开，所有 /api 请求返回 502
- **定位过程**：
  1. `docker compose ps` 看到 wms-server 状态反复 Restarting
  2. `docker compose logs wms-server` 没有任何报错，直接断在最正常运行处——**没有任何 JVM 错误日志，是被外部杀掉的典型特征**
  3. `dmesg -T | grep -i kill` → 出现 `Out of memory: Killed process xxx (java)`，确认是 OOM Killer
  4. `free -h` 显示 4G 内存被 JVM 吃掉大半，系统没有缓冲空间
- **根因**：JVM 堆上限设置过大 + 实例没有 swap，内存吃紧时内核优先杀掉占用最大的 Java 进程
- **修复**：
  - 配置 2G swap 作为缓冲
  - `-Xmx` 调整为 768m，给系统留足余量
  - 加入 `-XX:+UseG1GC` 与 `MaxMetaspaceSize` 上限
- **改进项**：
  - 巡检脚本加入内存水位告警（> 90% 推送钉钉）✅ 已落地
  - 容器统一配置 `restart: unless-stopped`，被杀后能自动拉起

---

## 事故 2：前端在服务器上构建失败（esbuild 跨平台）

- **时间**：2026-10-XX
- **现象**：把 Windows 开发机上整个项目（含 `node_modules`）传到 ECS 后执行 `npm run dev` 报错：
  `node_modules/.bin/vite: 权限被拒绝`，修完权限又报 `You installed esbuild for another platform`
- **定位过程**：
  1. 权限报错来自 Windows 文件系统没有 Linux 可执行位，传过去全部丢失
  2. 手工 `chmod +x` 后报第二个错，说明是更深的问题
  3. esbuild 是**原生二进制**，Windows 装的是 `.exe`，Linux 上根本跑不了，不是权限问题
- **根因**：把开发机的 `node_modules` 跨平台搬运到服务器，同时踩了「可执行位丢失」和「原生二进制不兼容」两个坑
- **修复**：`rm -rf node_modules` 后重新 `npm ci`（**不要试图去修，直接重装**）
- **改进项**（这条直接决定了部署架构）：
  - **服务器不做构建**，只负责运行。本地构建完把 `dist` 和 `jar` 传上去
  - 服务器上不装 JDK、不装 Node，出现 `java: command not found` 是**正常的，不用管**
  - 好处：构建很吃 CPU/内存，2核4G 经不起在服务器上跑 `mvn package`

---

## 事故 3：接口全 502，但容器是 Running 状态

- **时间**：2026-10-XX
- **现象**：`docker compose ps` 显示 wms-server 是 Up，但接口全 502
- **定位过程**：
  1. `docker compose logs --tail=100 wms-server` 看到 `Communications link failure`
  2. 判断方向：不是进程没了，是**连不上数据库**
  3. 检查 RDS 白名单 → 发现 ECS 内网 IP 没加进去 / ECS 与 RDS 不在同一 VPC
- **根因**：RDS 白名单未放行 ECS 内网 IP，或两者不在同一 VPC（内网根本不通）
- **修复**：RDS 控制台把 ECS 内网 IP 加进白名单，确认同地域同 VPC
- **改进项**：
  - 健康检查升级为 `/actuator/health`，**把数据库连通性纳入健康状态** ✅ 已落地
  - 这样以后 DB 不通时，容器状态会直接显示 unhealthy，不用等用户投诉
  - 巡检脚本加入 `mysql -e "SELECT 1"` 连通性探测 ✅ 已落地

---

## 事故 4（待补充：写你真实遇到的下一个）

```
## 事故 4：
- 时间：
- 现象：
- 定位过程：
  1.
  2.
  3.
- 根因：
- 修复：
- 改进项：
```

> 面试提示：讲事故时一定要说出**你是怎么一步步排除的**，而不是直接说答案。
> 「先 A 排除一种可能，再 B 确认方向，最后 C 定位到根因」——这个思路本身就是运维能力。
