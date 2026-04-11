# 宿舍管理系统课程设计版 (Dormitory System Management Suite)

## 项目概述

基于原有宿舍报修系统进行的课程设计级功能扩展，完全满足课程设计评分标准。改造后系统具备完整的前后端分离架构、多模块功能、现代UI界面、数据统计与可视化等高级特性。

## 技术栈

- **后端**: Spring Boot 3.4.5 (Java 24)、JPA、MySQL/H2
- **前端**: JavaFX 21、CSS样式、多Tab导航架构
- **安全**: BCrypt密码加密、角色权限隔离
- **通信**: HTTP/JSON、RESTful API设计
- **数据库**: MySQL（生产）、H2（开发）
- **编码**: 全栈UTF-8编码支持，中文显示无乱码

## 课程设计功能满足点（针对评分标准）

### ✅ 1. 用户/权限管理（5分）
- **角色划分**: ADMIN（宿管）、STUDENT（学生）
- **权限隔离**: 管理员与学生界面完全分离，访问功能不同
- **登录/注册**: 完整用户认证流程，密码BCrypt加密
- **信息绑定**: 学生信息扩展，包含学号、院系、专业、班级、宿舍、床位等

### ✅ 2. 前端框架（5分）
- **多级菜单**: 左侧导航栏，按角色动态显示不同功能菜单
- **系统logo**: 启动界面+顶部logo栏
- **版权信息**: 底部版权信息展示
- **区域布局**: BorderPane布局，顶部标题栏、左侧导航、中心内容区
- **可切换工作区**: Tab页签设计，支持8大管理员模块、4大学生模块
- **现代化UI**: 卡片设计、渐变背景、圆角、阴影、动态hover效果

### ✅ 3. 基本信息维护（20分） - 6+个核心模块
- **学生管理**: 学生基本信息CRUD（学号、姓名、性别、手机、院系、专业、班级、年级、宿舍、床位）
- **宿舍管理**: 宿舍基本信息CRUD（楼栋、房间号、楼层、类型、额定人数、当前人数、状态）
- **报修管理**: 扩展工单系统（报修类型、优先级、状态、处理意见、催单、评价）
- **公告管理**: 通知/公告/紧急公告CRUD
- **用户管理**: 用户认证、角色管理
- **系统配置**: 开发/生产环境切换

### ✅ 4. 复杂业务流程（15分） - 完整审批流
- **报修审批流程**: Pending → Processing → Resolved/Rejected 完整工作流
- **状态管理**: 受理、处理、完结、驳回多种状态
- **交互功能**: 学生催单（每个工单最多3次）、工单评价（1-5星+评语）
- **权限联动**: 宿舍人数自动同步，更换宿舍自动更新计数

### ✅ 5. 系统数据统计和展示（10分） - 数据可视化
- **仪表盘**: 核心数据卡片展示（学生数、宿舍数、报修数、待处理数）
- **实时刷新**: 仪表盘数据每5秒自动刷新，保持数据同步
- **图表展示**: 饼图（报修类型分布）、折线图（月度趋势）、柱状图（院系人数）
- **统计报表**: 各维度数据统计API，支持JSON导出
- **数据聚合**: 多表关联统计，实时更新

### ✅ 6. 用户体验（20分） - 现代化交互
- **启动动画**: 渐变加载界面，品牌展示
- **响应式布局**: 自适应窗口大小，组件合理排布
- **即时反馈**: 操作成功/失败提示，网络状态指示
- **可视化状态**: 工单状态色块标识，优先级标签
- **快捷键**: 回车登录，Tab导航切换
- **表单验证**: 前端预验证，防呆设计

## 项目结构

```
dorm-system/
├── dorm/                                  # 主Maven模块
│   ├── src/main/java/javawork1/dorm/
│   │   ├── client/                        # 前端JavaFX客户端
│   │   │   ├── DormClientApp.java         # 主界面，完整多Tab架构
│   │   │   └── MainLauncher.java          # 启动入口
│   │   ├── controller/                    # 后端REST控制器
│   │   │   ├── DashboardController.java   # 仪表盘统计
│   │   │   ├── OrderController.java       # 报修管理（升级版）
│   │   │   ├── DormRoomController.java    # 宿舍管理
│   │   │   ├── StudentInfoController.java # 学生管理
│   │   │   ├── AnnouncementController.java# 公告管理
│   │   │   ├── UserController.java        # 用户认证
│   │   │   └── TestController.java        # 连通测试
│   │   ├── entity/                        # JPA实体类
│   │   │   ├── RepairOrder.java           # 报修单（扩展）
│   │   │   ├── User.java                  # 用户
│   │   │   ├── DormRoom.java              # 宿舍房间
│   │   │   ├── StudentInfo.java           # 学生信息
│   │   │   └── Announcement.java          # 公告
│   │   ├── repository/                    # 数据访问层
│   │   │   ├── RepairOrderRepository.java
│   │   │   ├── UserRepository.java
│   │   │   ├── DormRoomRepository.java
│   │   │   ├── StudentInfoRepository.java
│   │   │   └── AnnouncementRepository.java
│   │   └── DormApplication.java           # Spring Boot启动类
│   └── src/main/resources/
│       ├── application.properties          # MySQL生产配置
│       └── application-dev.properties      # H2开发配置
```

## 功能模块详解

### 🏠 管理员功能（8个核心模块）

1. **📊 指挥中心** - 系统仪表盘
   - 核心数据卡片（学生/宿舍/报修统计）
   - 实时自动刷新（每5秒更新）
   - 报修类型分布饼图
   - 月度趋势折线图
   - 快捷操作入口

2. **🔧 维修管理** - 升级版报修系统
   - 完整审批流：待处理→受理→完结/驳回
   - 状态过滤、关键词搜索
   - 批量操作（受理、完结、驳回）
   - 管理员批注/处理意见

3. **🏠 宿舍管理** - 宿舍资源管理
   - 宿舍CRUD（楼栋、房间、楼层、类型）
   - 房间状态中文显示（正常/维修中/已关闭）
   - 入住人数自动统计
   - 批量初始化宿舍数据

4. **👨‍🎓 学生管理** - 学生信息管理
   - 学生基本信息CRUD
   - 院系/专业/班级分类
   - 宿舍分配、床位管理
   - 导入导出功能

5. **📢 通知中心** - 公告管理
   - 通知/公告/紧急公告发布
   - 草稿/发布/归档状态
   - 按类型分类展示
   - 批量操作（发布、归档、删除）

6. **📈 数据分析** - 统计报表
   - 多维度数据聚合
   - 图表可视化展示
   - 趋势分析
   - 数据导出

### 🎓 学生功能（4个核心模块）

1. **🏠 我的首页** - 学生个人工作台
   - 个人欢迎卡片
   - 快捷功能入口
   - 报修记录列表
   - 待办事项提醒

2. **🔧 我要报修** - 在线报修
   - 报修表单（类型、优先级、描述）
   - 个人信息自动填充
   - 提交确认
   - 历史记录查看

3. **📰 通知公告** - 公告查看
   - 最新通知列表
   - 分类展示（紧急/通知/公告）
   - 详情查看
   - 已读标记

4. **👤 个人资料** - 个人信息
   - 个人档案卡片
   - 基本信息展示
   - 宿舍分配详情
   - 入住时间记录

## 安装与运行

### 环境要求
- Java 24 JDK
- Maven 3.9+
- MySQL 8.0+ (可选，默认使用H2开发环境)
- 系统编码：UTF-8

### 快速启动

1. **克隆项目**
   ```bash
   git clone <项目地址>
   cd dorm-system/dorm
   ```

2. **启动后端服务（开发模式 - H2文件数据库）**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
   或
   ```bash
   mvn spring-boot:run
   ```
   访问 `http://localhost:8080` 查看API文档

3. **启动前端客户端（新终端）**
   ```bash
   .\mvnw.cmd exec:java
   ```
   或
   ```bash
   mvn compile exec:java -Dexec.mainClass="javawork1.dorm.client.MainLauncher"
   ```

### 默认测试账号

#### 管理员账号
- 账号: `admin`
- 密码: `123456`
- 权限: ADMIN（宿管）

#### 学生账号
- 账号: `student`
- 密码: `123456`
- 权限: STUDENT（学生）

### 初始化测试数据

1. 访问API初始化测试数据：
   ```
   GET http://localhost:8080/api/users/init     # 初始化用户账号
   GET http://localhost:8080/api/rooms/init     # 初始化宿舍数据
   GET http://localhost:8080/api/students/init  # 初始化学生数据
   GET http://localhost:8080/api/announcements/init # 初始化公告
   ```

## 课程设计使用指南

### 演示步骤（建议）

1. **系统启动展示**
   - 演示启动动画+logo
   - 展示登录界面设计

2. **角色权限演示**
   - 分别使用admin/student登录
   - 展示界面区别（管理员8模块 vs 学生4模块）

3. **核心功能演示**
   - **学生管理**: 添加/编辑/删除学生
   - **宿舍管理**: 宿舍CRUD，状态切换
   - **报修流**: 完整P→A→R流程 + 催单+评价
   - **公告管理**: 发布不同类型公告

4. **数据统计演示**
   - 打开仪表盘展示数据卡片
   - 展示图表可视化效果
   - 演示统计报表

5. **用户体验演示**
   - 展示表单验证、即时反馈
   - 演示快捷键操作
   - 展示响应式布局

### 评分要点对应证据

| 评分项 | 对应功能 | 演示路径 |
|--------|----------|----------|
| 用户权限 | 角色隔离 | admin/student登录对比界面 |
| 前端框架 | 多级菜单 | 左侧导航切换演示 |
| 基本维护 | CRUD模块 | 学生/宿舍/公告/报修CRUD |
| 业务流程 | 审批流 | 报修：提交→受理→完结 |
| 数据统计 | 图表展示 | 仪表盘饼图+折线图 |
| 用户体验 | 界面交互 | 表单验证+快捷操作+响应式 |

## API接口文档

### 核心接口概览

| 模块 | 接口前缀 | 主要端点 |
|------|----------|----------|
| 用户认证 | `/api/users` | `POST /login`, `POST /register`, `GET /init` |
| 报修管理 | `/api/orders` | `GET`, `POST`, `PUT /{id}/resolve`, `PUT /{id}/urge`, `PUT /{id}/review`, `GET /stats` |
| 宿舍管理 | `/api/rooms` | `GET`, `POST`, `PUT /{id}`, `DELETE /{id}`, `GET /init`, `GET /stats` |
| 学生管理 | `/api/students` | `GET`, `POST`, `PUT /{id}`, `DELETE /{id}`, `GET /by-user/{userId}`, `GET /stats/department` |
| 公告管理 | `/api/announcements` | `GET`, `GET /all`, `POST`, `PUT /{id}`, `DELETE /{id}`, `PUT /{id}/archive`, `GET /init` |
| 仪表盘 | `/api/dashboard` | `GET` - 全量统计数据 |

### 数据交互格式

所有请求/响应均使用JSON格式，如：

**登录请求**
```json
{
  "username": "admin",
  "password": "123456"
}
```

**报修提交**
```json
{
  "studentName": "张三",
  "roomNumber": "一101",
  "repairType": "水电",
  "priority": "High",
  "description": "宿舍漏水严重，急需处理"
}
```

## 数据库设计

### 核心表结构

1. **system_user** - 用户表
   - id, username, password(BCrypt), role

2. **student_info** - 学生信息表
   - id, user_id, student_no, real_name, gender, phone, department, major, class_name, grade, room_number, bed_number, check_in_date, status

3. **dorm_room** - 宿舍表
   - id, building_name, room_number, floor, room_type, capacity, current_count, status, remark

4. **repair_order** - 报修单表
   - id, student_name, room_number, description, repair_type, priority, status, admin_remark, urge_count, rating, review_comment, created_at, resolved_at

5. **announcement** - 公告表
   - id, title, content, publisher, type, status, created_at

## 扩展建议（可选）

1. **短信通知**: 集成短信平台，报修受理/完工通知
2. **移动端**: 开发微信小程序/APP版本
3. **考勤集成**: 链接宿舍门禁考勤系统
4. **缴费管理**: 宿舍电费/网费在线缴纳
5. **访客登记**: 访客预约登记系统

## 版权声明

© 2026 宿舍管理系统课程设计版 v2.0

仅供课程设计参考使用，不得用于商业用途。

---