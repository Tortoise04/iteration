# 数字健康助手

一个帮助你摆脱奶头乐控制的Spring Boot项目，记录手机使用情况、目标完成情况、日常活动，并提供周复盘和月复盘功能。

## 技术栈

- 后端：Spring Boot 2.5.4, Spring Security, JPA, MySQL, OpenAI Client
- 前端：React 18.2.0, Ant Design 5.0.0, Axios, Moment.js

## 功能特性

1. **手机使用情况记录**：记录每天的手机使用时间（支持按App分类）
2. **目标管理**：添加、查看和删除目标
3. **每日子任务**：为目标设置每日打卡任务
4. **日常活动记录**：记录每天的活动情况
5. **周期总结**：记录每周和每月的总结
6. **成果记录**：记录完成的成果
7. **统计分析**：提供周复盘和月复盘的统计数据
8. **用户认证**：支持用户注册和登录
9. **AI辅助**：提供AI辅助功能

## 快速开始

### 数据库初始化

1. 确保已安装 MySQL 8.0+
2. 执行数据库初始化脚本：`mysql -u root -p < database-init.sql`
3. 或在 MySQL 客户端中执行：`source database-init.sql`

### 后端启动

1. 进入项目根目录 `C:\Users\Dylan\Desktop\iterate\digital-wellness`
2. 构建项目：`mvn clean package`
3. 运行项目：`java -jar target/digital-wellness-1.0-SNAPSHOT.jar`
4. 后端服务将在 <http://localhost:8080> 启动

### 前端启动

1. 进入前端目录：`cd frontend`
2. 或者：`cd C:\Users\Dylan\Desktop\iterate\digital-wellness\frontend`
3. 安装依赖：`npm install`
4. 启动开发服务器：`npm run dev`
5. 前端服务将在 <http://localhost:5173> 启动

## API 接口

### 认证接口

- `POST /api/auth/register`：注册新用户
- `POST /api/auth/login`：用户登录

### 手机使用情况接口

- `GET /api/phone-usage`：获取所有手机使用情况
- `POST /api/phone-usage`：添加手机使用情况
- `GET /api/phone-usage/{id}`：获取指定手机使用情况
- `DELETE /api/phone-usage/{id}`：删除指定手机使用情况
- `GET /api/phone-usage/date-range`：根据日期范围获取手机使用情况

### 目标接口

- `GET /api/goals`：获取所有目标
- `POST /api/goals`：添加目标
- `GET /api/goals/{id}`：获取指定目标
- `DELETE /api/goals/{id}`：删除指定目标

### 每日子任务接口

- `GET /api/daily-subtasks`：获取所有子任务
- `POST /api/daily-subtasks`：添加子任务
- `PUT /api/daily-subtasks/{id}/complete`：完成任务打卡
- `DELETE /api/daily-subtasks/{id}`：删除子任务

### 日常活动接口

- `GET /api/daily-activities`：获取所有日常活动
- `POST /api/daily-activities`：添加日常活动
- `GET /api/daily-activities/{id}`：获取指定日常活动
- `DELETE /api/daily-activities/{id}`：删除指定日常活动

### 周期总结接口

- `GET /api/period-summaries`：获取所有周期总结
- `POST /api/period-summaries`：添加周期总结
- `GET /api/period-summaries/{id}`：获取指定周期总结
- `DELETE /api/period-summaries/{id}`：删除指定周期总结

### 成果接口

- `GET /api/achievements`：获取所有成果
- `POST /api/achievements`：添加成果
- `GET /api/achievements/{id}`：获取指定成果
- `DELETE /api/achievements/{id}`：删除指定成果

### 统计分析接口

- `GET /api/statistics/weekly`：获取周统计数据
- `GET /api/statistics/monthly`：获取月统计数据

## 项目结构

```
digital-wellness/
├── frontend/              # 前端项目
│   ├── public/            # 静态资源
│   │   ├── favicon.svg    # 网站图标
│   │   └── icons.svg      # 图标文件
│   ├── src/               # 前端源代码
│   │   ├── assets/        # 资源文件
│   │   ├── components/    # React组件
│   │   │   ├── Login.jsx  # 登录页面
│   │   │   ├── MainApp.jsx # 主应用组件
│   │   │   └── ProtectedRoute.jsx # 路由守卫
│   │   ├── utils/         # 工具函数
│   │   │   ├── request.js # HTTP请求封装
│   │   │   └── theme.js   # 主题管理
│   │   ├── styles/        # 样式文件
│   │   │   └── theme.css  # 主题样式
│   │   ├── App.jsx        # 主应用组件
│   │   ├── main.jsx       # 应用入口
│   │   └── index.css      # 全局样式
│   ├── package.json       # 前端依赖
│   ├── tsconfig.json      # TypeScript配置
│   └── vite.config.js     # Vite配置
├── src/                   # 后端源代码
│   ├── main/java/com/iterate/digitalwellness/
│   │   ├── config/        # 配置类
│   │   ├── controller/    # 控制器
│   │   ├── entity/        # 实体类
│   │   ├── repository/    # 仓库接口
│   │   ├── service/       # 服务层
│   │   │   └── impl/      # 服务实现
│   │   ├── util/          # 工具类
│   │   └── DigitalWellnessApplication.java  # 应用主类
│   ├── main/resources/    # 资源文件
│   │   └── application.yml # 应用配置
├── database-init.sql      # 完整的数据库初始化脚本（推荐使用）
├── database.sql           # 基础数据库脚本
├── iterate.sql            # 数据库脚本（Navicat导出）
├── mysql-schema.sql       # MySQL数据库schema
├── pom.xml                # 后端依赖
└── README.md              # 项目说明
```

## 数据库设计

### 表结构（共9张表）

#### 用户认证模块
1. **user**：用户表
   - id：主键
   - username：用户名
   - password：密码（加密存储）
   - role：角色（ADMIN-管理员，USER-普通用户）
   - created_at：创建时间
   - updated_at：更新时间

#### 手机使用情况模块
2. **app_preset**：常用App预设表
   - id：主键
   - app_name：App名称（预置微信、抖音、B站等常用App）
   - sort_order：排序顺序
   - is_active：是否启用
   - created_at：创建时间

3. **phone_usage**：手机使用记录表
   - id：主键
   - user_id：用户ID（关联user表）
   - date：日期
   - usage_time：使用总时长（分钟）
   - created_at：创建时间
   - updated_at：更新时间

4. **app_usage_detail**：App使用明细表
   - id：主键
   - phone_usage_id：关联的手机使用记录ID
   - app_name：App名称
   - app_preset_id：关联的预设App ID
   - usage_time：使用时长（分钟）
   - created_at：创建时间

#### 目标管理模块
5. **goal**：目标表
   - id：主键
   - user_id：用户ID（关联user表）
   - goal：目标内容
   - description：目标详细描述
   - start_time：开始时间
   - end_time：结束时间/截止时间
   - status：状态（IN_PROGRESS-进行中，COMPLETED-已完成，CANCELLED-已取消）
   - created_at：创建时间
   - updated_at：更新时间

6. **daily_subtask**：每日子任务表
   - id：主键
   - goal_id：关联的目标ID
   - task_content：任务内容
   - target_date：计划完成日期
   - is_completed：是否完成打卡
   - completion_date：实际完成日期
   - completion_note：完成备注/心得
   - created_at：创建时间
   - updated_at：更新时间

#### 日常活动与成果记录模块
7. **daily_activity**：日常活动表
   - id：主键
   - user_id：用户ID（关联user表）
   - date：活动日期
   - activity：活动内容
   - duration：持续时间（分钟）
   - location：活动地点
   - created_at：创建时间
   - updated_at：更新时间

8. **achievement**：成果/成就表
   - id：主键
   - user_id：用户ID（关联user表）
   - achievement：成果内容
   - description：成果详细描述
   - category：成果分类（LEARNING-学习，WORK-工作，HEALTH-健康，LIFE-生活等）
   - time：获得时间
   - created_at：创建时间
   - updated_at：更新时间

#### 周期总结模块
9. **period_summary**：周期总结表
   - id：主键
   - user_id：用户ID（关联user表）
   - period：周期名称
   - period_type：周期类型（WEEKLY-周总结，MONTHLY-月总结）
   - start_date：周期开始日期
   - end_date：周期结束日期
   - summary：总结内容
   - highlights：亮点/成就
   - improvements：改进计划
   - next_plan：下一步计划
   - created_at：创建时间
   - updated_at：更新时间

### 表关系图

```
user (1) ──────────< (N) phone_usage
  │                        │
  │                        └────< (N) app_usage_detail
  │                                      │
  │                                      └──── (N) app_preset
  │
  ├────────────< (N) goal
  │                    │
  │                    └────< (N) daily_subtask
  │
  ├────────────< (N) daily_activity
  │
  ├────────────< (N) achievement
  │
  └────────────< (N) period_summary
```

## 注意事项

- 本项目使用MySQL数据库，数据会持久化存储
- 首次使用请先执行 `database-init.sql` 初始化数据库
- 前端使用Vite开发服务器，后端使用Spring Boot内嵌Tomcat服务器
- 本项目集成了阿里云百炼AI服务，用于提供AI辅助功能
- 本项目仅作为学习和个人使用，不建议用于生产环境
- 默认管理员账户：用户名 `admin`，密码 `admin123`

## 配置说明

### 数据库配置

在 `src/main/resources/application.yml` 文件中配置数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/iterate?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: "root"
    password: "041222"
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 阿里云百炼配置

在 `src/main/resources/application.yml` 文件中配置阿里云百炼API信息：

```yaml
alibaba:
  bailing:
    api-key: sk-fbe5effd65ca478495cb81d27f113845
    api-url: https://ark.cn-beijing.volces.com/api/v3/chat/completions
    model: deepseek-r1-distill-qwen-7b
```

## 数据库初始化（重要）

### 使用完整版初始化脚本（推荐）

推荐使用 `database-init.sql` 脚本，它包含：

✅ 完整的9张表结构（防错设计，使用 `DROP TABLE IF EXISTS`）  
✅ 所有字段的中文注释  
✅ 合理的主键、索引、默认值设置  
✅ 预设常用App数据（微信、抖音、B站等22个App）  
✅ 默认管理员账户  

执行方式：

```bash
# 命令行执行
mysql -u root -p < database-init.sql

# 或在MySQL客户端中
source database-init.sql
```

### 防错设计说明

所有 `CREATE TABLE` 语句都使用了 `DROP TABLE IF EXISTS`，确保：
- 重新执行不会因为表已存在而报错
- 每次都是全新的数据库结构
- 方便开发测试阶段的数据库重置

### 保留旧版脚本

以下旧版脚本已保留，但**不推荐使用**：
- `database.sql`：基础版本，字段较少
- `mysql-schema.sql`：缺少部分索引和注释
- `iterate.sql`：Navicat导出格式，不包含数据

## License

本项目仅供学习和个人使用。
