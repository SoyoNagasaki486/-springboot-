# LabGuard 高校实验室安全准入与预约管理系统

本项目按论文中的功能、实体属性和业务流程实现，技术栈已升级为：

- JDK 21
- Spring Boot 3.5.15
- MySQL 5.7.44
- Vue 3 + Vite 7

完整的运行环境、环境搭建步骤、默认账号和注意事项见
[项目运行环境与部署说明](docs/项目运行环境与部署说明.md)。

## 快速启动

1. 确认本机 MySQL 5.7 服务已启动。
2. 客户运行已构建版本时，双击 `运行已打包项目.bat`。
3. 开发或重新构建时，双击 `启动项目.bat`，或在项目目录执行 `start-project.bat`。
4. 浏览器访问 <http://localhost:8080/>。

默认数据库连接：

- 数据库：`lab_safety`（首次启动自动创建）
- 用户名：`root`
- 密码：`123456`

可以使用环境变量 `DB_URL`、`DB_USER`、`DB_PASSWORD` 覆盖默认连接。

## 演示账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `1234` |
| 学生 | `student` | `1234` |
| 教师 | `teacher` | `1234` |

首次成功登录旧演示账号后，后端会自动将明文演示密码升级为 BCrypt 密文。

## 功能模块

- 用户管理：注册、登录、账号、角色和准入状态。
- 安全培训：课程发布、学习进度和完成记录。
- 准入考试：每次随机抽取 10 道题、Session 固定试卷、在线判分、成绩记录和资格审核。
- 实验室管理：位置、容量、风险等级、开放时间和运行状态。
- 预约管理：准入校验、开放状态校验、冲突检测、申请和审核。
- 设备管理：设备台账、使用登记、归还和异常转维修。
- 安全管理：危险源、违规行为、安全检查和停用整改闭环。
- 系统管理：通知公告、操作日志和统计工作台。

## 开发与测试

后端测试：

```powershell
.\mvnw.cmd test
```

前端开发：

```powershell
cd frontend
npm install
npm run dev
```

前端生产构建会输出到 `src/main/resources/static`：

```powershell
cd frontend
npm run build
```

随机组卷数量可通过 `src/main/resources/application.yml` 中的
`labguard.exam.question-count` 调整；题库少于配置数量时自动使用全部题目。
