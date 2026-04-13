# 宿舍报修管理系统 (Dormitory Repair System)


## 极速启动 (开箱即用模式)

本项目内置了 `dev` 演示环境。无需安装 MySQL，数据将直接在计算机运行内存 (RAM) 中构建，就是用的时候别退cmd

### 前置条件
* JDK 17 或更高版本
* Maven 3.6+

### 启动指令

**1. 编译打包**
在项目根目录执行：
`mvn clean package -DskipTests`

**2. 触发降维启动 (核心)**
进入 `target` 目录，执行以下指令。系统将物理切断 MySQL 连接，凭空在内存中开辟数据空间：
`java -jar dorm-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev`
如果你有sql就去掉`--spring.profiles.active=dev`我再把sql数据传上来就行了

**3. 数据初始化**
服务启动后（提示 Tomcat started on port 8080），打开浏览器访问：
`http://localhost:8080/api/users/init`
*我用ai搞了个哈希加密，数据库里不会有实际账号密码（*

**4. 启动客户端**
运行 `MainLauncher.java` 的 main 方法即可进入工作台。

注意：请使用 `MainLauncher` 作为启动入口，它会正确加载 JavaFX 应用。不要直接运行 `DormClientApp`。