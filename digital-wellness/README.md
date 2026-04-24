# 数字健康助手

一个帮助你摆脱奶头乐控制的Spring Boot项目，记录手机使用情况、目标完成情况、日常活动，并提供周复盘和月复盘功能。

## 技术栈

- 后端：Spring Boot 2.5.4, Spring Security, JPA, MySQL, OpenAI Client
- 前端：React 18.2.0, Ant Design 5.0.0, Axios, Moment.js

## 功能特性

1. **手机使用情况记录**：记录每天的手机使用时间
2. **目标管理**：添加、查看和删除目标
3. **日常活动记录**：记录每天的活动情况
4. **周期总结**：记录每周和每月的总结
5. **成果记录**：记录完成的成果
6. **统计分析**：提供周复盘和月复盘的统计数据
7. **用户认证**：支持用户注册和登录
8. **AI辅助**：提供AI辅助功能

## 快速开始

### 后端启动

1. 进入项目根目录C:\Users\Dylan\Desktop\iterate\digital-wellness
2. 构建项目：`mvn clean package`
3. 运行项目：`java -jar target/digital-wellness-1.0-SNAPSHOT.jar`
4. 后端服务将在 <http://localhost:8080> 启动

### 前端启动

1. 进入前端目录：`cd frontend`
2. 或者:cd C:\Users\Dylan\Desktop\iterate\digital-wellness\frontend
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
│   │   └── DigitalWellnessApplication.java  # 应用主类
│   ├── main/resources/    # 资源文件
│   │   └── application.yml # 应用配置
├── database.sql           # 数据库脚本
├── iterate.sql            # 数据库脚本
├── mysql-schema.sql       # MySQL数据库 schema
├── pom.xml                # 后端依赖
└── README.md              # 项目说明
```

## 数据库设计

### 表结构

1. **phone\_usage**：手机使用情况表
   - id：主键
   - date：日期
   - usage\_time：使用时间
2. **goal**：目标表
   - id：主键
   - goal：目标内容
   - start\_time：开始时间
   - end\_time：完成时间/截止时间
3. **daily\_activity**：日常活动表
   - id：主键
   - date：日期
   - activity：活动内容
   - duration：持续时间
4. **period\_summary**：周期总结表
   - id：主键
   - period：周期
   - summary：总结内容
5. **achievement**：成果表
   - id：主键
   - achievement：成果内容
   - time：时间
6. **user**：用户表
   - id：主键
   - username：用户名
   - password：密码
   - role：角色

## 注意事项

- 本项目使用MySQL数据库，数据会持久化存储
- 前端使用Vite开发服务器，后端使用Spring Boot内嵌Tomcat服务器
- 本项目集成了阿里云百炼AI服务，用于提供AI辅助功能
- 本项目仅作为学习和个人使用，不建议用于生产环境

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

