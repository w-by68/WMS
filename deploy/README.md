# WMS 阿里云 ECS 部署手册

> 目标：把 WMS 项目部署到阿里云 ECS，通过 `http://<公网IP>` 直接访问，带真实可用的数据库。
>
> **你的环境**（已确认）：ECS 2核4G / Ubuntu 22.04.5 LTS / 内网 IP `172.21.16.180` / 数据库用阿里云 RDS MySQL / 暂无域名

---

## 零、部署架构

```
                   公网用户
                      │  http://<公网IP>:80
                      ▼
        ┌─────────────────────────────┐
        │  阿里云 ECS (2核4G Ubuntu)   │
        │                             │
        │  ┌───────────────────────┐  │
        │  │ wms-web (Nginx 容器)   │  │
        │  │  :80                  │  │
        │  │  ├ 静态资源 → dist/    │  │
        │  │  └ /api/ 反向代理 ─┐   │  │
        │  └───────────────────┼───┘  │
        │       Docker 内网     │      │
        │  ┌───────────────────▼───┐  │
        │  │ wms-server (JRE 容器) │  │
        │  │  :8080 仅内网可达      │  │
        │  └───────────┬───────────┘  │
        └──────────────┼──────────────┘
                       │ VPC 内网
                       ▼
            阿里云 RDS MySQL 8.0
            （不暴露公网，只放行 ECS 内网 IP）
```

**三个关键设计决策（面试可以直接讲）：**

| 决策 | 原因 |
|---|---|
| 后端容器**不映射端口**到宿主机，只用 Docker 内网 | 8080 在公网完全不可见，安全组只需开 80，攻击面最小 |
| 数据库用 **RDS 而非在 ECS 自建 MySQL** | 数据与计算分离，实例重建不丢数据；RDS 自带备份、监控，简历上是「云数据库运维」 |
| 前端走 **Nginx 同源代理**而不是配置 CORS | 前端 axios 的 `baseURL` 留空即可，天然无跨域，少一整类线上问题 |

---

## 一、准备工作（在阿里云控制台完成，不写代码）

### 1.1 ECS 安全组放行端口

控制台 → ECS → 实例 → 安全组 → 配置规则 → 入方向，添加：

| 端口 | 协议 | 授权对象 | 说明 |
|---|---|---|---|
| 22 | TCP | `0.0.0.0/0`（或你自己的家宽 IP，更安全） | SSH 远程连接 |
| 80 | TCP | `0.0.0.0/0` | 网站访问 |

> ⚠️ **不要**开放 3306 和 8080。数据库走内网，后端走容器内网，公网都不需要。
>
> 安全提示：22 端口如果长期对全网开放，日志里每天会有几百次暴力破解尝试。有条件就限制来源 IP。

### 1.2 查一下 ECS 的公网 IP

远程终端里执行（阿里云元数据服务，专治"我到底哪个是公网 IP"）：

```bash
curl http://100.100.100.200/latest/meta-data/public-ipv4
```

记下这个 IP，后面所有 `<公网IP>` 都指它。截图里的 `172.21.16.180` 是**内网 IP，公网访问不了**，别搞混。

### 1.3 创建 RDS MySQL 实例

控制台 → 云数据库 RDS → 创建实例：

- 版本：**MySQL 8.0**
- 系列：**基础版**（够用，省钱）
- 规格：1核2G 起
- **地域：必须和 ECS 一模一样**
- 网络：**专有网络 VPC，必须和 ECS 选同一个 VPC**
  > 这是最容易踩的坑。不同 VPC 之间内网不通，就只能走公网地址——又慢又花钱，还得单独放行白名单。

### 1.4 建库、建账号、配白名单

RDS 实例创建好后，在控制台完成三件事：

**① 创建数据库**

- 数据库名：`wms_db`
- 字符集：**`utf8mb4`**（必须！用 utf8 会导致 emoji 和部分中文报错）
- 授权账号：先随便，下一步建

**② 创建账号**

- 账号名：`wms_app`（**不要用 root**，权限最小化原则）
- 账号类型：高权限账号 / 普通账号都可以，普通账号需单独授权
- 密码：强密码，记下来

**③ 授权**

把 `wms_db` 的读写权限授予 `wms_app`。

**④ 设置白名单**

RDS → 数据安全性 → 白名单设置 → 添加 `172.21.16.180`（ECS 的内网 IP）。
> 只加内网 IP。加了内网 IP 就不要再用公网地址连接。

### 1.5 导入表结构

在**ECS 的远程终端**里执行（不用本地装 mysql 客户端，直接用 Docker 跑一个临时的）：

先把两个 sql 文件传上去（用 Workbench 右上角的上传按钮，或后面配好 SSH 后用 scp）：

```bash
# 假设传到了 /opt/wms/sql/ 下
cd /opt/wms/sql
```

然后导入。**注意：这里要填 RDS 的内网地址**，因为现在是在 ECS 上操作：

```bash
# 建表
docker run --rm -i mysql:8 mysql \
  -h rm-xxxxxxxxxxxx.mysql.rds.aliyuncs.com \
  -u wms_app -p'你的密码' \
  --default-character-set=utf8mb4 \
  wms_db < 01-schema.sql

# 初始化数据（管理员账号、基础字典等）
docker run --rm -i mysql:8 mysql \
  -h rm-xxxxxxxxxxxx.mysql.rds.aliyuncs.com \
  -u wms_app -p'你的密码' \
  --default-character-set=utf8mb4 \
  wms_db < 02-init-data.sql
```

验证一下：

```bash
docker run --rm -i mysql:8 mysql \
  -h rm-xxxxxxxxxxxx.mysql.rds.aliyuncs.com -u wms_app -p'你的密码' wms_db \
  -e "SHOW TABLES;"
```

看到表列表就说明数据库通了。**这一步通了，后面基本就没坑了。**

---

## 二、服务器初始化（在 ECS 上执行一次）

把 `deploy/` 整个目录传到 ECS 的 `/opt/wms/`（先传 `scripts/server-init.sh` 就够）：

```bash
mkdir -p /opt/wms && cd /opt/wms
```

然后跑初始化脚本。**先提醒一个必踩的坑**：在 Windows 上编辑过的 `.sh` 文件是 CRLF 换行，Linux 下会报 `$'\r': command not found`，先修掉：

```bash
sed -i 's/\r$//' server-init.sh
chmod +x server-init.sh
```

关于 Docker 镜像加速——**国内 ECS 直连 Docker Hub 大概率超时**。先去控制台拿一个专属加速地址：容器镜像服务 ACR → 镜像工具 → 镜像加速器，复制形如 `https://xxxx.mirror.aliyuncs.com` 的地址，然后：

```bash
# 带加速地址执行（推荐）
bash server-init.sh https://xxxx.mirror.aliyuncs.com

# 或者先不带，之后再配
bash server-init.sh
```

脚本做了 7 件事：更新软件源 → 装基础工具 → 设时区为东八区 → **加 2G swap 防 OOM** → 装 Docker → 配镜像加速和日志轮转 → 建目录。

> **为什么要加 swap**：4G 内存跑 JVM + Nginx + 系统，遇到构建余留进程或内存峰值时容易触发 OOM Killer 把 Java 进程直接杀掉。加 2G swap 是廉价保险，成本几乎为零。
>
> **为什么要配日志轮转**：你那台机器磁盘只有 39G。Docker 默认**不限制**容器日志大小，一个日志刷得凶的服务几周就能把磁盘写满，然后整台机器卡死——这是真实事故里排前三的原因。

验证：

```bash
docker --version          # 有版本号即可
docker compose version    # 有版本号即可
free -h                   # 确认 Swap 不是 0
```

---

## 二·补、两个高频误区（卡住过很多人，包括我）

### 误区一：在 ECS 上装 JDK 和 Node

**不需要。一个都不用装。**

这套方案里，Java 由 `eclipse-temurin:17-jre` 镜像自带，Node 只在本地做构建、服务器完全不参与编译。

所以在 ECS 上看到这两条，**都是正常的，不是环境坏了**：

```
Command 'java' not found, but can be installed with: ...
Command 'node' not found ...
```

判断环境配好没有，只看一件事：

```bash
cd /opt/wms && docker compose ps     # 两个容器都是 Up，就是对的
```

> 如果已经手快 `apt install openjdk-17-jdk-headless` 了，不用卸载，不影响运行，只是白占约 200MB 磁盘。但**别再去装 Node 了**，那是往坑里走（见误区二）。

### 误区二：在 ECS 上跑 `npm run dev`

**这个方向是错的，先停手。**

典型报错长这样：

```
> vite --config ./config/vite.config.dev.ts
sh: 第1行: /WMS/arco/hello-arco-pro/node_modules/.bin/vite: 权限被拒绝
```

**原因**：`node_modules` 是在 Windows 上装的，整个目录传到 Linux 后，Unix 的可执行权限位丢了——Windows 文件系统压根没有这个概念，zip 打包也不会保留。

**但千万不要只 `chmod +x` 修一下就算了。** `node_modules` 里除了 JS 还有一堆**平台相关的原生二进制**：

| 包 | Windows 版 | Linux 需要 |
|---|---|---|
| `esbuild` | `esbuild.exe` | `bin/esbuild`（ELF） |
| `rollup` | `@rollup/rollup-win32-x64-msvc` | `@rollup/rollup-linux-x64-gnu` |

改完权限，下一步必然报 `You installed esbuild for another platform`——白折腾一轮。

**为什么本来就不该在这台机器上跑 dev：**

1. Vite dev server + esbuild 常驻要吃 300~500MB 内存，而这台 2核4G 的机器同时还要跑 JVM 和 Nginx；
2. `config/vite.config.dev.ts` 里没配 `server.host`，Vite 默认**只监听 127.0.0.1**，从公网 IP 根本访问不到，等于白跑；
3. 配置里 `open: true` 会在无图形界面的服务器上尝试调 `xdg-open` 并报错（不致命，但看着烦）；
4. 生产机上长期挂一个开发工具，依赖多、暴露面大、进程管理混乱，是运维上的反模式。

**正确路径**：本地 `npm run build` → 把 `dist/` 传上去 → Nginx 容器托管（就是第四节方式 B 的流程）。

**如果你确实需要临时在服务器上联调**（比如想直接在公网演示），照这个来：

```bash
# 1) 确认 node 版本 —— Vite 3 需要 16/18，别用 22（版本太老跑不动新 Node）
node -v

# 2) 换国内源（ECS 直连 registry.npmjs.org 很慢）
npm config set registry https://registry.npmmirror.com

# 3) 关键一步：删掉 Windows 带过来的，重新装，不要去修权限
cd /WMS/arco/hello-arco-pro
rm -rf node_modules
npm install

# 4) 起服务，必须显式指定监听所有网卡
npm run dev -- --host 0.0.0.0
```

然后去安全组放行 5173，访问 `http://<公网IP>:5173`。

> 附带一个坑：`vite-plugin-imagemin` 安装时要下载原生二进制（gifsicle / mozjpeg / pngquant 等），国内网络容易失败卡住。真卡住了就把 `config/vite.config.prod.ts` 里 `import configImageminPlugin from './plugin/imagemin'` 和 plugins 里的引用一起注释掉，再从 devDependencies 删掉这个包。它**只在 `npm run build` 时生效**（dev 不加载），而图片压缩不属于关键功能，砍掉不影响项目跑通。

**联调结束记得关掉，别留在生产机上。** 真要长期给公网访问，走 Nginx。

---

## 三、配置环境变量（在 ECS 上）

```bash
cd /opt/wms
cp .env.example .env

# 生成 JWT 密钥（重要！不要用代码里的默认值）
openssl rand -base64 48
```

把生成出来的那串字符复制下来，然后编辑 `.env`：

```bash
vim .env
```

填四项：

```ini
DB_HOST=rm-xxxxxxxxxxxx.mysql.rds.aliyuncs.com   # RDS 内网地址
DB_USERNAME=wms_app
DB_PASSWORD=你的RDS密码
JWT_SECRET=<刚才 openssl 生成的那串>
```

> **为什么必须换 JWT_SECRET**：你的 `application.yml` 里默认值是 `wms-secret-key-2024-for-smart-warehouse-system`，写在源码里。如果代码进了 GitHub，任何人都能用这个密钥伪造一个管理员 token 直接登录你的系统。这是部署到公网前**唯一不能省**的一步。

确保权限收紧（里面有密码）：

```bash
chmod 600 /opt/wms/.env
```

---

## 四、部署上线

### 方式 A：一键脚本（推荐）

回到**你自己的电脑**，在 Git Bash 里执行：

```bash
cd D:/Documents/wby/Java/WMS

# 第一次先配 SSH 免密（只需一次）
ssh-keygen -t ed25519
ssh root@<公网IP> "mkdir -p ~/.ssh && chmod 700 ~/.ssh"
cat ~/.ssh/id_ed25519.pub | ssh root@<公网IP> "cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"

# 一键部署
bash deploy/scripts/deploy.sh <公网IP>
```

脚本会：同步生产配置 → 构建后端 jar → 构建前端 dist → 打包上传 → 重启容器 → 冒烟测试打印结果。

以后每次改完代码，重新跑这一条命令就是一次发版。

### 方式 B：手动执行（想搞清每一步时用）

**本地构建：**

```bash
cd D:/Documents/wby/Java/WMS

# 后端
cp deploy/backend/application-prod.yml springBoot/src/main/resources/
cd springBoot && mvn clean package -DskipTests
# 产物：target/wms-server.jar

# 前端
cd ../arco/hello-arco-pro && npm run build
# 产物：dist/
```

**上传**（在 Git Bash 里，公网 IP 自己替换）：

```bash
IP=<公网IP>
scp springBoot/target/wms-server.jar  root@$IP:/opt/wms/app/
scp -r arco/hello-arco-pro/dist/*     root@$IP:/opt/wms/app/dist/
scp deploy/docker-compose.yml         root@$IP:/opt/wms/
scp deploy/nginx/wms.conf             root@$IP:/opt/wms/nginx/
```

**启动**（回到 ECS 终端）：

```bash
cd /opt/wms
docker compose up -d
docker compose ps        # 两个容器都应该是 Up
```

第一次会拉取 `eclipse-temurin:17-jre`（约 250MB）和 `nginx:1.25-alpine`（约 40MB），耐心等一两分钟。

---

## 五、验证

```bash
# 看容器状态
docker compose ps

# 看后端启动日志（出现 "Started WmsApplication" 就是成功了）
docker compose logs -f wms-server

# 在 ECS 上自测
curl -I http://localhost/
curl http://localhost/api/auth/me
```

浏览器打开 `http://<公网IP>/`，用初始化数据里的管理员账号登录。

**验证清单：**

- [ ] 前端页面能打开，不是 Nginx 默认欢迎页
- [ ] 登录能成功（说明 Nginx → 后端 → RDS 整条链路通了）
- [ ] 直接访问 `http://<公网IP>/api/auth/me` 返回的是 JSON 而不是 502
- [ ] 刷新 `/inventory` 这类页面不报 404（验证 history fallback 生效）

---

## 六、排查手册

| 现象 | 原因 | 解决 |
|---|---|---|
| 浏览器打不开，一直在转圈 | 安全组没开 80 | 控制台加 80 端口入方向规则 |
| 显示 Nginx 欢迎页 | 挂载路径不对，没读到 dist | `docker exec wms-web ls /usr/share/nginx/html` 检查 |
| 页面能开，接口全 502 | 后端没起来或崩了 | `docker compose logs --tail=100 wms-server` |
| 后端日志报 `Communications link failure` | RDS 白名单没加 ECS 内网 IP，或 VPC 不同 | 检查白名单和 VPC 一致性 |
| 后端日志报 `Access denied for user` | RDS 账号密码错 / 未授权到 wms_db | 控制台重新授权 |
| 后端日志报 `Unknown database 'wms_db'` | 建库时库名打错 | 控制台确认库名 |
| 中文乱码 | 库字符集不是 utf8mb4 | 重建库为 utf8mb4，重导数据 |
| Java 进程莫名消失 | 内存不足被 OOM Killer 杀了 | `dmesg -T \| grep -i kill` 确认；已加 swap，也可调小 `-Xmx` |
| `docker pull` 卡住不动 | 镜像加速器没配 | 回第二章重配 |
| 改代码后页面没变化 | index.html 被缓存（这个已通过配置避免），或没重新构建 | 强刷 Ctrl+Shift+R；确认跑过构建 |
| 磁盘满了 | 日志没轮转 | `df -h`；`docker system prune -a` |
| `java: command not found` | **正常现象**，服务器不需要 JDK（Java 在容器镜像里） | 不用管，见「二·补 误区一」 |
| `node: command not found` | 同上，服务器不参与构建 | 不用管，见「二·补 误区一」 |
| `node_modules/.bin/vite: 权限被拒绝` | 在 Windows 装的 `node_modules` 传到了 Linux，可执行位丢了 | **别在服务器跑 dev**，见「二·补 误区二」 |
| `You installed esbuild for another platform` | 上一条硬修权限后的必然报错，原生二进制是 Windows 版 | `rm -rf node_modules` 后重装 |

---

## 七、日常运维命令

```bash
cd /opt/wms

docker compose ps                      # 看状态
docker compose logs -f wms-server      # 实时看后端日志
docker compose restart wms-server      # 只重启后端
docker compose down                    # 停全部
docker compose up -d                   # 起全部

df -h                                  # 磁盘
free -h                                # 内存
docker stats --no-stream               # 容器资源占用

# 清理无用镜像和缓存（磁盘紧张时用）
docker system prune -a
```

---

## 八、这套方案在简历/面试里怎么讲

不要写"使用阿里云部署项目"，太薄。写这三个点，每个都对应一个**你真实做过的决策**：

1. **「采用 Nginx 反向代理 + 前后端同源部署，规避跨域问题」**
   —— 被问就答：前端 axios baseURL 留空走同源，Nginx 把 `/api/` 转发到后端容器，省掉 CORS 配置和一整类预检问题。

2. **「后端容器不映射宿主机端口，仅通过 Docker 内网暴露，配合安全组最小开放」**
   —— 被问就答：公网只开 80，8080 和 3306 都不暴露，攻击面收敛。

3. **「数据库使用阿里云 RDS，与计算节点网络隔离，通过内网 VPC 通信」**
   —— 被问就答：数据和计算分离，ECS 重建不丢数据；走内网延迟低且不计流量费；RDS 白名单只放行 ECS 内网 IP。

再加一句运维细节，会很加分：**「为 4G 内存实例配置了 swap 防止 JVM 被 OOM Killer 终止，并为容器配置日志轮转避免磁盘写满」**——这两条是真做过运维的人才会想到的。

---

## 九、当前方案的已知取舍（要不要继续做，按优先级排）

现在的部署方式属于「**本地构建 + 手动上传**」，能用、够面试讲，但还不是完整 CI/CD：

| 项 | 现状 | 建议 |
|---|---|---|
| 自动化部署 | 手动跑一次脚本 | 先把项目 push 到 GitHub（现在两个目录都还没 git 仓库），再接 GitHub Actions |
| HTTPS | 无（IP 直连） | 等有域名且备案后再上 Let's Encrypt，不备案用不了 |
| 健康检查接口 | 无 actuator | 后端加 `spring-boot-starter-actuator`，暴露 `/actuator/health` |
| 监控告警 | 无 | 时间够再加 Prometheus + Grafana，优先级最低 |

**5 周冲刺判断**：先把这一版跑通、能访问、能登录，就有了"我把项目部署上线了"这个事实。CI/CD 属于锦上添花，等你确认面试里会被追问再补——不要为了部署折腾掉本该用来刷面试题的时间。

---

## 十、运维增强：备份 / 回滚 / 巡检 / 恢复演练

部署能跑通 ≠ 有运维。这一章补齐运维的四件事：**可回滚、能告警、会备份、有记录**。
下面这些脚本已经在 `scripts/` 里，跑一次 `deploy.sh` 会自动同步到 ECS 的 `/opt/wms/scripts/`。

> 操作顺序建议：10.1 → 10.2 → 10.3 → 10.4 → 10.5，全部做完约 1.5 天。
> 它们在简历里对应的条目，见「十一、这些做完能加的简历条目」。

### 10.1 初始化（在 ECS 上执行一次）

```bash
# 装 mysql 客户端（备份和巡检要用，不用装完整的 mysql-server）
apt update && apt install -y mysql-client

# 免明文密码：不这么做，密码会出现在命令行和 ps 里
cat > ~/.my.cnf <<'EOF'
[client]
host=<RDS内网地址>
user=<账号>
password=<密码>
EOF
chmod 600 ~/.my.cnf

mysql -e "SELECT 1"          # 验证连通
mkdir -p /opt/wms/backup /opt/wms/scripts /opt/wms/app/releases
```

### 10.2 备份 + 恢复演练

```bash
bash /opt/wms/scripts/backup.sh          # 手动跑一次，确认能出文件
bash /opt/wms/scripts/restore-test.sh    # 恢复演练，验证备份真的能恢复

# 每天凌晨 3 点自动备份
(crontab -l 2>/dev/null; echo "0 3 * * * /opt/wms/scripts/backup.sh >> /var/log/wms-backup.log 2>&1") | crontab -
crontab -l                               # 确认写进去了
```

**没做恢复演练的备份等于没有备份**——这句话在运维面试里很值钱。
恢复演练会恢复到临时库 `wms_restore_test`，验证完自动删除，绝不会碰生产库。

### 10.3 巡检 + 告警

1. 钉钉群 → 群设置 → 智能群助手 → 添加机器人 → 自定义
2. 安全设置选「**自定义关键词**」，关键词填 `WMS`（选"加签"还要额外算签名，没必要）
3. 复制 Webhook，填进 `scripts/check.sh` 里的 `WEBHOOK=` 那一行
4. 手动跑一次确认没报错：`bash /opt/wms/scripts/check.sh`
5. 上 cron，5 分钟一轮：

```bash
(crontab -l 2>/dev/null; echo "*/5 * * * * /opt/wms/scripts/check.sh") | crontab -
```

巡检项：首页可用性、容器存活、容器健康状态、磁盘 / 内存水位、数据库连通性、**备份文件是否新鲜**（备份任务自己挂了也能发现）。
告警做了去重：异常只推一次，服务挂一整晚不会轰炸你的手机。

### 10.4 回滚

每次 `deploy.sh` 部署前会把当前版本快照到 `/opt/wms/app/releases/`（滚动保留最近 3 份，40G 磁盘经不起无限存 jar）。

```bash
bash /opt/wms/scripts/rollback.sh
```

脚本会自动：取最近一份旧版本 → 覆盖 → 重启 → 冒烟（最多等 2 分钟）。

### 10.5 主机安全基线（执行一次）

```bash
# 1. 禁密码登录。先确认密钥能登，否则会把自己锁在门外
sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
# 2. 改端口的顺序：先在阿里云安全组放行新端口 → 再改配置 → 再重启
#    顺序反了就再也登不进去了
# sed -i 's/^#\?Port.*/Port 2222/' /etc/ssh/sshd_config
systemctl restart ssh

# 3. 防暴力破解
apt install -y fail2ban && systemctl enable --now fail2ban

# 4. 系统日志限容（journald 默认一直涨，是真实的磁盘事故来源）
sed -i 's/^#\?SystemMaxUse=.*/SystemMaxUse=200M/' /etc/systemd/journald.conf
systemctl restart systemd-journald && journalctl --vacuum-size=200M
```

### 10.6 慢日志 → 索引优化

1. RDS 控制台开启慢日志，`long_query_time = 1`
2. 正常用一段时间业务（出入库、库存查询多点几次）
3. 导出慢日志，找出 Top SQL
4. `EXPLAIN` 确认是全表扫描 → 加联合索引 → 再 `EXPLAIN` 验证
5. 把优化前后的响应时间记下来——这是简历上唯一能写的量化数字，**必须是真测的**

```sql
EXPLAIN SELECT * FROM inventory WHERE warehouse_id = 1 AND sku_id = 1001;
ALTER TABLE inventory ADD INDEX idx_wh_sku (warehouse_id, sku_id);
```

### 10.7 故障复盘

真实的踩坑经历写在 `POSTMORTEM.md`，面试前过一遍。
**讲故障时要讲你怎么一步步排除的，而不是直接说答案。**

---

## 十一、这些做完能加的简历条目

- 设计基于版本快照的发布流程，保留最近 3 个可运行版本，支持发布失败 30 秒内回滚
- 编写定时备份脚本（全量 dump + 压缩 + 7 天轮转 + 完整性校验），并完成恢复演练验证备份可用性
- 编写巡检脚本监控服务可用性、容器状态、磁盘内存水位、数据库连通性与备份时效性，异常经钉钉机器人实时告警
- 引入 Actuator 将存活探针从 TCP 端口探测升级为含数据库连通性的 HTTP 健康探测
- 完成主机安全加固：SSH 密钥登录、禁用密码认证、Fail2ban 防爆破、系统日志容量限制

**别碰**（2核4G 是真实约束，被问到时答"知道它存在，但说得出为什么现在不用"，是加分题）：
K8s、Prometheus + Grafana、ELK、蓝绿灰度、Jenkins 自建。具体说法见 `D:\Documents\WorkBuddy\2026-10-23-12-16-17\运维简历补充清单.md`。

---

## 附：文件清单

```
deploy/
├── README.md                      ← 本文档
├── docker-compose.yml             ← 两个容器的编排（传到 /opt/wms/）
├── .env.example                   ← 环境变量模板（传到 /opt/wms/，复制为 .env）
├── .gitignore                     ← 防止 .env 和产物进仓库
├── backend/
│   └── application-prod.yml       ← 生产配置（复制到 springBoot/src/main/resources/）
├── nginx/
│   └── wms.conf                   ← Nginx 配置（传到 /opt/wms/nginx/）
├── POSTMORTEM.md                  ← 故障复盘（面试素材）
└── scripts/
    ├── server-init.sh             ← 服务器初始化（在 ECS 上跑一次）
    ├── deploy.sh                  ← 本地一键构建+上传+重启（本机跑）
    ├── rollback.sh                ← 一键回滚（ECS 上跑）
    ├── backup.sh                  ← 定时备份（ECS 上跑）
    ├── restore-test.sh            ← 恢复演练（ECS 上跑）
    └── check.sh                   ← 巡检+告警（ECS 上跑）
```
