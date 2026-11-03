# SmartSchedule - 智能日程规划助手

> **现在怎么启动**：双击桌面 `SmartSchedule` 快捷方式，或项目根目录的 `start.cmd`。不要再手动开 MySQL / 起后端 / 起前端——前端已打进 jar，数据库已是内嵌 H2。停下来用 `stop.cmd`，改了代码用 `build.cmd`。详见 `docs/启动说明.md`；任务书主线的接口与算法见 `docs/任务书_改造契约.md`。
>
> 以下为原始设计说明，其中 MySQL 与两终端启动的部分已被上述方式取代。

基于 DeepSeek + 智谱 GLM-4V 双 AI 引擎的智能日程规划 Web 应用。

## 设计理念

汲取 **Eve**（Python/飞书日程助手）和 **iterate**（Spring Boot/数字健康助手）两个项目的精华：

| 来源 | 取 | 舍 |
|------|-----|-----|
| Eve | 精力模型、贪心匹配算法、NLP 三层意图分类、积分激励 | 飞书单平台绑定、Python 异步复杂度、Celery 过度设计 |
| iterate | Spring Boot 分层架构、JPA 实体设计、AI Provider 模式 | Security 禁用、Java 8/Spring Boot 2.5、前端 JSX 无类型 |

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 2.7 + JPA + MySQL + Spring Security + JWT |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia |
| AI | DeepSeek（文本对话 + 日程生成）+ 智谱 GLM-4V（课表 OCR 识别） |

## 功能

1. **用户系统** — JWT 认证 + BCrypt 密码加密，支持自定义起床/睡觉时间与精力高峰
2. **目标管理** — 分类（学习/工作/健身/爱好/其他）、精力等级、优先级、频次设置
3. **课表管理** — 固定时间块 + OCR 图片识别课表，自动纳入日程
4. **AI 日程生成** — DeepSeek 生成结构化 JSON 日程（含休息块、任务拆分、精力匹配），贪心算法兜底
5. **任务追踪** — 完成/跳过/推迟操作，时间线按实际时间排序
6. **AI 对话** — 自然语言交互，多会话管理，支持删除历史会话
7. **积分激励** — 完成任务攒积分，10 级等级体系
8. **日程看板** — 日视图时间线，日期切换，AI 建议展示

## 快速开始

### 1. 数据库

使用 `init.sql` 一键创建数据库、表结构和种子数据：

```bash
mysql -u root -p < SmartSchedule/backend/src/main/resources/init.sql
```

> `init.sql` 会创建 `smartschedule` 数据库、7 张表，并插入演示用户和示例目标/课程。首次启动后端时 `DataInitializer` 会自动设置演示用户密码为 `demo123`。

### 2. 配置 API Key

编辑 `SmartSchedule/backend/src/main/resources/application.yml`：

```yaml
app:
  ai:
    deepseek:
      api-key: your-deepseek-api-key  # 替换为你的 DeepSeek API Key
  datasource:
    password: your_mysql_password      # 替换为你的 MySQL 密码
```

### 3. 启动后端

```bash
cd SmartSchedule/backend
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`

### 4. 启动前端

```bash
cd SmartSchedule/frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，自动代理 `/api` 到后端。

### 5. 体验

1. 打开 `http://localhost:5173`
2. 注册/登录（或使用 demo/demo123）
3. 添加目标和课表
4. 点击「生成日程」
5. 在 AI 助手中对话调整

## 项目结构

```
SmartSchedule/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/smartschedule/
│       │   ├── SmartScheduleApplication.java
│       │   ├── config/               # Security, JWT, AI, CORS, 种子数据
│       │   ├── controller/           # 6 个 REST Controller
│       │   ├── service/              # AI, Schedule, Points, User
│       │   ├── repository/           # 7 个 JPA Repository
│       │   ├── entity/               # 7 个 JPA Entity
│       │   └── dto/                  # 9 个 DTO
│       └── resources/
│           ├── application.yml
│           ├── schema.sql            # 仅建表（供参考）
│           └── init.sql              # 建表 + 种子数据（推荐使用）
├── frontend/                         # Vue 3 前端
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── router/index.ts           # 路由 + 守卫
│       ├── api/index.ts              # Axios 封装
│       ├── stores/auth.ts            # Pinia 状态
│       ├── layouts/MainLayout.vue    # 主布局
│       └── views/
│           ├── Login.vue             # 登录/注册
│           ├── Dashboard.vue         # 日程看板（核心）
│           ├── Goals.vue             # 目标管理
│           ├── Courses.vue           # 课表管理
│           ├── AiChat.vue            # AI 对话
│           └── Points.vue            # 积分中心
└── README.md
```

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| GET | `/api/health` | 健康检查 |
| GET | `/api/goals` | 目标列表 |
| POST | `/api/goals` | 创建目标 |
| DELETE | `/api/goals/{id}` | 删除目标 |
| GET | `/api/courses` | 课表列表 |
| POST | `/api/courses` | 添加课程 |
| DELETE | `/api/courses/{id}` | 删除课程 |
| POST | `/api/ocr/parse-course-table` | OCR 识别课表 |
| GET | `/api/schedule/today` | 查询日程 |
| POST | `/api/schedule/generate` | 生成日程（AI 优先） |
| PUT | `/api/schedule/tasks/{id}/status` | 更新任务状态 |
| GET | `/api/schedule/history` | 历史日程 |
| POST | `/api/ai/chat` | AI 对话 |
| GET | `/api/ai/sessions` | 会话列表 |
| GET | `/api/ai/history` | 会话历史 |
| DELETE | `/api/ai/sessions/{sessionId}` | 删除会话 |
| GET | `/api/points` | 积分总览 |
| GET | `/api/points/history` | 积分历史 |

## 数据库

7 张表：`users`, `goals`, `courses`, `daily_plans`, `daily_tasks`, `points_records`, `ai_conversations`

预置演示账户：`demo` / `demo123`（首次启动后端时由 `DataInitializer` 自动设置密码）

种子数据包含 4 个示例目标（英语四级、驾照理论、健身锻炼、课外阅读）和 3 门示例课程。

## 架构说明

### 日程生成流程

```
用户操作（看板按钮 / AI 对话）
  → ScheduleService.generateDailyPlan()
    → AiService.generateScheduleJson()   ← DeepSeek 生成结构化 JSON
      → 解析 JSON → createPlanFromAiTasks() → DailyTask 实体
    → 若 AI 失败 → generateDailyPlanGreedy() ← 贪心算法兜底
```

- **AI 优先**：DeepSeek 根据用户目标、课程、精力偏好生成完整日程 JSON，包含任务拆分、休息块、精力匹配
- **贪心兜底**：当 AI 不可用时，使用 30 分钟时间槽贪心匹配算法确保基本可用
- **目标匹配**：AI 任务通过三层模糊匹配关联到 Goal 实体，用于积分计算
