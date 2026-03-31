package javawork1.dorm.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javawork1.dorm.entity.Announcement;
import javawork1.dorm.entity.DormRoom;
import javawork1.dorm.entity.RepairOrder;
import javawork1.dorm.entity.StudentInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DormClientApp extends Application {
    private static final String SERVER_URL = "http://localhost:8080";
    private Stage window;
    private ObjectMapper mapper = new ObjectMapper();
    private String currentUserRole = "";
    private Long currentUserId = null;
    private TabPane mainTabPane;
    private StudentInfo currentStudentInfo;

    @Override
    public void start(Stage primaryStage) {
        this.window = primaryStage;
        showSplashScreen();
    }

    // ========== 启动界面（加载动画 + logo） ==========
    private void showSplashScreen() {
        VBox splash = new VBox(20);
        splash.setAlignment(Pos.CENTER);
        splash.setPadding(new Insets(50));
        splash.setStyle("-fx-background-color: linear-gradient(to bottom, #2196F3, #1976D2);");

        // Logo 区域
        HBox logoBox = new HBox(15);
        logoBox.setAlignment(Pos.CENTER);
        Rectangle logoRect = new Rectangle(60, 60, Color.WHITE);
        logoRect.setArcWidth(15);
        logoRect.setArcHeight(15);
        Label logoText = new Label("DSMS");
        logoText.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        logoText.setTextFill(Color.WHITE);
        logoBox.getChildren().addAll(logoRect, logoText);

        // 系统标题
        Label title = new Label("Dormitory System Management Suite");
        title.setFont(Font.font("Arial", 18));
        title.setTextFill(Color.WHITE);

        // 进度条
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(40, 40);

        splash.getChildren().addAll(logoBox, title, progress);
        Scene scene = new Scene(splash, 600, 400);
        window.setScene(scene);
        window.setTitle("宿舍管理系统 - 启动中...");
        window.show();

        // 2秒后跳转登录
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(this::showLoginScreen);
        }).start();
    }

    // ========== 登录界面 ==========
    private void showLoginScreen() {
        VBox loginRoot = new VBox(20);
        loginRoot.setPadding(new Insets(40, 60, 40, 60));
        loginRoot.setAlignment(Pos.CENTER);
        loginRoot.setStyle("-fx-background-color: #F5F7FA;");

        // 标题栏
        HBox titleBar = new HBox(10);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        Rectangle smallRect = new Rectangle(8, 35, Color.web("#2196F3"));
        Label title = new Label("宿舍管理系统");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#333333"));
        titleBar.getChildren().addAll(smallRect, title);

        // 卡片面板
        VBox card = new VBox(25);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; " +
                      "-fx-border-color: #E0E0E0; -fx-border-width: 1;");
        card.setMaxWidth(380);

        Label loginTitle = new Label("系统登录");
        loginTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));
        loginTitle.setTextFill(Color.web("#2196F3"));

        TextField userField = new TextField();
        userField.setPromptText("请输入账号");
        userField.setPrefHeight(40);
        userField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #DDD;");

        PasswordField passField = new PasswordField();
        passField.setPromptText("请输入密码");
        passField.setPrefHeight(40);
        passField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #DDD;");

        HBox buttonRow = new HBox(15);
        buttonRow.setAlignment(Pos.CENTER);
        Button loginBtn = new Button("登 录");
        loginBtn.setPrefSize(120, 45);
        loginBtn.setStyle("-fx-background-color: #2196F3; -fx-background-radius: 6; -fx-text-fill: white; " +
                          "-fx-font-size: 16; -fx-font-weight: bold;");
        Button registerBtn = new Button("注册账号");
        registerBtn.setPrefSize(120, 45);
        registerBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #2196F3; -fx-border-radius: 6; " +
                            "-fx-border-width: 2; -fx-text-fill: #2196F3; -fx-font-size: 16;");
        buttonRow.getChildren().addAll(loginBtn, registerBtn);

        Label msgLabel = new Label();
        msgLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14;");

        card.getChildren().addAll(loginTitle, userField, passField, buttonRow, msgLabel);

        // 底部信息
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        Label footerText = new Label("© 2026 宿舍管理系统 v2.0 | 技术支持");
        footerText.setStyle("-fx-text-fill: #777; -fx-font-size: 13;");
        footer.getChildren().add(footerText);

        loginRoot.getChildren().addAll(titleBar, card, footer);

        // ===== 事件处理 =====
        loginBtn.setOnAction(e -> {
            String username = userField.getText().trim();
            String password = passField.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                msgLabel.setText("账号密码不能为空");
                return;
            }
            try {
                Map<String, String> creds = new HashMap<>();
                creds.put("username", username);
                creds.put("password", password);
                String json = mapper.writeValueAsString(creds);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_URL + "/api/users/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    JsonNode node = mapper.readTree(res.body());
                    currentUserRole = node.get("role").asText();
                    currentUserId = node.get("id").asLong();
                    loadCurrentStudentInfo(); // 加载学生信息
                } else {
                    msgLabel.setText("登录失败: " + res.body());
                }
            } catch (Exception ex) {
                msgLabel.setText("网络错误，请检查服务器");
                ex.printStackTrace();
            }
        });

        registerBtn.setOnAction(e -> showRegisterScreen());

        // 回车登录
        userField.setOnAction(e -> passField.requestFocus());
        passField.setOnAction(e -> loginBtn.fire());

        Scene scene = new Scene(loginRoot, 900, 600);
        window.setScene(scene);
        window.setTitle("宿舍管理系统 - 登录");
        window.centerOnScreen();
        window.show();
    }

    // ========== 注册界面 ==========
    private void showRegisterScreen() {
        // 简化为对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("注册功能暂不开放");
        alert.setHeaderText("请联系管理员添加账号");
        alert.setContentText("当前版本仅支持管理员创建的账号登录。如需注册，请联系系统管理员。");
        alert.showAndWait();
    }

    // ========== 加载当前学生信息 ==========
    private void loadCurrentStudentInfo() {
        if (!"STUDENT".equals(currentUserRole)) {
            showMainWorkspace();
            return;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/api/students/by-user/" + currentUserId))
                    .GET()
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                currentStudentInfo = mapper.readValue(res.body(), StudentInfo.class);
            }
        } catch (Exception e) {
            System.err.println("加载学生信息失败: " + e.getMessage());
        }
        showMainWorkspace();
    }

    // ========== 主工作区（多Tab架构） ==========
    private void showMainWorkspace() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F5F7FA;");

        // ===== 顶部标题栏 =====
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 12 12;");
        HBox.setHgrow(topBar, Priority.ALWAYS);

        // Logo 和标题
        HBox titleGroup = new HBox(10);
        titleGroup.setAlignment(Pos.CENTER_LEFT);
        Rectangle logo = new Rectangle(6, 30, Color.web("#1976D2"));
        Label appTitle = new Label("宿舍管理系统");
        appTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));
        appTitle.setTextFill(Color.web("#333"));
        titleGroup.getChildren().addAll(logo, appTitle);

        // 用户信息
        HBox userGroup = new HBox(10);
        userGroup.setAlignment(Pos.CENTER_RIGHT);
        Label welcome = new Label("欢迎您，" + (currentStudentInfo != null ? currentStudentInfo.getRealName() : "管理员"));
        welcome.setStyle("-fx-text-fill: #666; -fx-font-size: 14;");
        Label roleBadge = new Label(currentUserRole);
        roleBadge.setPadding(new Insets(4, 12, 4, 12));
        roleBadge.setStyle("-fx-background-color: " + ("ADMIN".equals(currentUserRole) ? "#FF9800" : "#4CAF50") + 
                          "; -fx-background-radius: 12; -fx-text-fill: white; -fx-font-size: 12;");
        Button logoutBtn = new Button("退出登录");
        logoutBtn.setStyle("-fx-background-color: #F44336; -fx-background-radius: 6; -fx-text-fill: white;");
        logoutBtn.setOnAction(e -> {
            currentUserRole = "";
            currentUserId = null;
            currentStudentInfo = null;
            showLoginScreen();
        });
        userGroup.getChildren().addAll(welcome, roleBadge, logoutBtn);

        topBar.getChildren().addAll(titleGroup, new Region(), userGroup);
        root.setTop(topBar);

        // ===== 左侧导航 =====
        VBox sidebar = new VBox(5);
        sidebar.setPrefWidth(200);
        sidebar.setPadding(new Insets(20, 0, 20, 0));
        sidebar.setStyle("-fx-background-color: white; -fx-background-radius: 12 0 0 12;");

        Label navTitle = new Label("功能导航");
        navTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));
        navTitle.setPadding(new Insets(0, 0, 15, 25));
        sidebar.getChildren().add(navTitle);

        String[] adminTabs = {"仪表盘", "报修管理", "宿舍管理", "学生管理", "公告管理", "系统统计"};
        String[] studentTabs = {"我的首页", "我要报修", "查看公告", "我的资料"};

        String[] tabs = "ADMIN".equals(currentUserRole) ? adminTabs : studentTabs;
        for (int i = 0; i < tabs.length; i++) {
            Button navBtn = new Button(tabs[i]);
            navBtn.setPrefWidth(180);
            navBtn.setPrefHeight(45);
            navBtn.setAlignment(Pos.CENTER_LEFT);
            navBtn.setGraphicTextGap(15);
            int tabIndex = i + 1; // 0是空Tab
            navBtn.setOnAction(e -> {
                if (tabIndex < mainTabPane.getTabs().size()) {
                    mainTabPane.getSelectionModel().select(tabIndex);
                }
            });
            String baseStyle = "-fx-background-color: transparent; -fx-border-radius: 6; -fx-border-width: 0; " +
                               "-fx-font-size: 14; -fx-text-fill: #555; -fx-font-weight: normal;";
            String hoverStyle = "-fx-background-color: #F0F7FF; -fx-text-fill: #1976D2;";
            navBtn.setStyle(baseStyle);
            navBtn.setOnMouseEntered(e -> navBtn.setStyle(baseStyle + hoverStyle));
            navBtn.setOnMouseExited(e -> navBtn.setStyle(baseStyle));
            sidebar.getChildren().add(navBtn);
        }

        sidebar.getChildren().add(new Region());
        sidebar.getChildren().add(new Label("v2.0.0"));

        root.setLeft(sidebar);

        // ===== 中心区域（TabPane） =====
        mainTabPane = new TabPane();
        mainTabPane.setSide(Side.TOP);
        mainTabPane.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 空的首页Tab（用于占位）
        Tab homeTab = new Tab();
        homeTab.setDisable(true);
        mainTabPane.getTabs().add(homeTab);

        // 根据角色添加功能Tab
        if ("ADMIN".equals(currentUserRole)) {
            Tab dashboardTab = new Tab("📊 指挥中心");
            dashboardTab.setContent(buildDashboardPane());
            mainTabPane.getTabs().add(dashboardTab);

            Tab orderTab = new Tab("🔧 维修管理");
            orderTab.setContent(buildOrderManagementPane());
            mainTabPane.getTabs().add(orderTab);

            Tab dormTab = new Tab("🏠 宿舍管理");
            dormTab.setContent(buildDormManagementPane());
            mainTabPane.getTabs().add(dormTab);

            Tab studentTab = new Tab("👨‍🎓 学生管理");
            studentTab.setContent(buildStudentManagementPane());
            mainTabPane.getTabs().add(studentTab);

            Tab announcementTab = new Tab("📢 通知中心");
            announcementTab.setContent(buildAnnouncementPane());
            mainTabPane.getTabs().add(announcementTab);

            Tab statsTab = new Tab("📈 数据分析");
            statsTab.setContent(buildStatsPane());
            mainTabPane.getTabs().add(statsTab);
        } else {
            Tab studentHomeTab = new Tab("🏠 我的首页");
            studentHomeTab.setContent(buildStudentHomePane());
            mainTabPane.getTabs().add(studentHomeTab);

            Tab repairTab = new Tab("🔧 我要报修");
            repairTab.setContent(buildStudentRepairPane());
            mainTabPane.getTabs().add(repairTab);

            Tab noticeTab = new Tab("📰 通知公告");
            noticeTab.setContent(buildStudentNoticePane());
            mainTabPane.getTabs().add(noticeTab);

            Tab profileTab = new Tab("👤 个人资料");
            profileTab.setContent(buildStudentProfilePane());
            mainTabPane.getTabs().add(profileTab);
        }

        root.setCenter(mainTabPane);

        Scene scene = new Scene(root, 1200, 750);
        window.setScene(scene);
        window.setTitle("宿舍管理系统 - " + ("ADMIN".equals(currentUserRole) ? "管理员控制台" : "学生端"));
        window.centerOnScreen();
    }

    // ========== 构建各个功能面板 ==========

    // 1. 管理员仪表盘
    private Pane buildDashboardPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(25));

        // 顶部卡片
        HBox topCards = new HBox(20);
        topCards.setPadding(new Insets(0, 0, 30, 0));

        String[][] cardData = {
            {"👨‍🎓 学生总数", "#4CAF50", "studentsCard"},
            {"🏠 宿舍总数", "#2196F3", "roomsCard"},
            {"🔧 待处理报修", "#FF9800", "pendingCard"},
            {"📊 已解决报修", "#9C27B0", "resolvedCard"}
        };

        for (String[] data : cardData) {
            VBox card = createCard(data[0], data[1], "0");
            card.setId(data[2]);
            topCards.getChildren().add(card);
        }
        pane.setTop(topCards);

        // 左右布局
        HBox bottomArea = new HBox(20);
        bottomArea.setPadding(new Insets(20, 0, 0, 0));

        // 左：报表图表
        VBox chartSection = new VBox(15);
        chartSection.setPrefWidth(580);
        chartSection.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20;");

        Label chartTitle = new Label("报修类型分布");
        chartTitle.setFont(Font.font(16));
        PieChart typeChart = new PieChart();
        typeChart.setId("typeChart");

        Label chartTitle2 = new Label("报修状态趋势");
        chartTitle2.setFont(Font.font(16));
        LineChart<String, Number> trendChart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        trendChart.setId("trendChart");
        trendChart.setPrefHeight(250);

        chartSection.getChildren().addAll(chartTitle, typeChart, chartTitle2, trendChart);

        // 右：快捷操作 + 通知列表
        VBox rightSection = new VBox(20);
        rightSection.setPrefWidth(560);

        VBox quickActions = new VBox(15);
        quickActions.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20;");
        quickActions.getChildren().add(new Label("⚡ 快捷操作"));
        String[] actions = {"🆕 新增宿舍", "👥 批量导入学生", "📊 生成月报表", "🔔 发布通知"};
        for (String act : actions) {
            Button btn = new Button(act);
            btn.setPrefWidth(200);
            btn.setAlignment(Pos.CENTER_LEFT);
            quickActions.getChildren().add(btn);
        }

        VBox recentOrders = new VBox(15);
        recentOrders.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20;");
        recentOrders.getChildren().add(new Label("📋 最新报修单"));
        TableView<RepairOrder> recentTable = new TableView<>();
        recentTable.setId("recentOrdersTable");
        recentTable.setPrefHeight(180);
        recentOrders.getChildren().add(recentTable);

        rightSection.getChildren().addAll(quickActions, recentOrders);
        bottomArea.getChildren().addAll(chartSection, rightSection);
        pane.setCenter(bottomArea);

        // 加载实时数据
        new Thread(this::loadDashboardData).start();

        return pane;
    }

    // 2. 报修管理
    private Pane buildOrderManagementPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        // 顶部工具栏
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(0, 0, 20, 0));

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("全部", "待处理", "处理中", "已解决", "已驳回");
        statusFilter.setValue("全部");

        TextField searchField = new TextField();
        searchField.setPromptText("搜索宿舍号/姓名");
        searchField.setPrefWidth(180);

        Button searchBtn = new Button("🔍 搜索");
        Button refreshBtn = new Button("🔄 刷新");
        Button exportBtn = new Button("📄 导出报表");

        toolbar.getChildren().addAll(
            new Label("状态过滤:"), statusFilter,
            new Label("关键词:"), searchField, searchBtn, refreshBtn, exportBtn
        );
        pane.setTop(toolbar);

        // 中间表格
        TableView<RepairOrder> table = new TableView<>();
        table.setId("orderTable");

        TableColumn<RepairOrder, Long> idCol = new TableColumn<>("单号");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<RepairOrder, String> nameCol = new TableColumn<>("报修人");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        TableColumn<RepairOrder, String> roomCol = new TableColumn<>("宿舍号");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        TableColumn<RepairOrder, String> typeCol = new TableColumn<>("报修类型");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("repairType"));
        TableColumn<RepairOrder, String> priorityCol = new TableColumn<>("优先级");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        TableColumn<RepairOrder, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<RepairOrder, String> timeCol = new TableColumn<>("提交时间");
        timeCol.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() == null) return null;
            return javafx.beans.binding.Bindings.createStringBinding(() ->
                cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            );
        });

        table.getColumns().addAll(idCol, nameCol, roomCol, typeCol, priorityCol, statusCol, timeCol);
        pane.setCenter(table);

        // 底部操作区
        HBox actionBar = new HBox(10);
        actionBar.setPadding(new Insets(20, 0, 0, 0));
        Button acceptBtn = new Button("✅ 受理");
        Button resolveBtn = new Button("✔ 完结");
        Button rejectBtn = new Button("❌ 驳回");
        Button remarkBtn = new Button("📝 批注");
        TextArea remarkArea = new TextArea();
        remarkArea.setPromptText("处理意见...");
        remarkArea.setPrefRowCount(2);
        actionBar.getChildren().addAll(acceptBtn, resolveBtn, rejectBtn, new Label("批注:"), remarkArea, remarkBtn);
        pane.setBottom(actionBar);

        // 事件绑定
        refreshBtn.setOnAction(e -> loadOrders(table, statusFilter.getValue(), searchField.getText()));
        searchBtn.setOnAction(e -> loadOrders(table, statusFilter.getValue(), searchField.getText()));
        statusFilter.setOnAction(e -> loadOrders(table, statusFilter.getValue(), searchField.getText()));

        // 初始加载
        loadOrders(table, "全部", "");

        return pane;
    }

    // 3. 宿舍管理
    private Pane buildDormManagementPane() {
        // 实现宿舍CRUD
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(0, 0, 20, 0));

        ComboBox<String> buildingFilter = new ComboBox<>();
        buildingFilter.getItems().addAll("全部楼栋", "一号楼", "二号楼", "三号楼");
        buildingFilter.setValue("全部楼栋");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("全部状态", "Normal", "Maintenance", "Closed");
        statusFilter.setValue("全部状态");

        Button addBtn = new Button("🆕 新增宿舍");
        Button editBtn = new Button("✏ 编辑");
        Button deleteBtn = new Button("🗑 删除");
        Button refreshBtn = new Button("🔄 刷新");

        toolbar.getChildren().addAll(
            new Label("楼栋:"), buildingFilter,
            new Label("状态:"), statusFilter,
            addBtn, editBtn, deleteBtn, refreshBtn
        );
        pane.setTop(toolbar);

        // 表格
        TableView<DormRoom> table = new TableView<>();
        TableColumn<DormRoom, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<DormRoom, String> buildingCol = new TableColumn<>("楼栋");
        buildingCol.setCellValueFactory(new PropertyValueFactory<>("buildingName"));
        TableColumn<DormRoom, String> roomCol = new TableColumn<>("房间号");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        TableColumn<DormRoom, Integer> floorCol = new TableColumn<>("楼层");
        floorCol.setCellValueFactory(new PropertyValueFactory<>("floor"));
        TableColumn<DormRoom, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        TableColumn<DormRoom, Integer> capacityCol = new TableColumn<>("额定");
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        TableColumn<DormRoom, Integer> currentCol = new TableColumn<>("当前");
        currentCol.setCellValueFactory(new PropertyValueFactory<>("currentCount"));
        TableColumn<DormRoom, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(idCol, buildingCol, roomCol, floorCol, typeCol, capacityCol, currentCol, statusCol);
        pane.setCenter(table);

        refreshBtn.setOnAction(e -> loadDormRooms(table, buildingFilter.getValue(), statusFilter.getValue()));
        loadDormRooms(table, "全部楼栋", "全部状态");

        return pane;
    }

    // 4. 学生管理
    private Pane buildStudentManagementPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        // 工具栏
        HBox toolbar = new HBox(15);
        TextField searchField = new TextField();
        searchField.setPromptText("搜索学号/姓名/宿舍");
        Button searchBtn = new Button("🔍 搜索");
        Button importBtn = new Button("📥 批量导入");
        Button exportBtn = new Button("📤 导出");
        Button addBtn = new Button("➕ 新增学生");
        toolbar.getChildren().addAll(searchField, searchBtn, new Separator(), importBtn, exportBtn, addBtn);
        pane.setTop(toolbar);

        TableView<StudentInfo> table = new TableView<>();
        TableColumn<StudentInfo, String> noCol = new TableColumn<>("学号");
        noCol.setCellValueFactory(new PropertyValueFactory<>("studentNo"));
        TableColumn<StudentInfo, String> nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("realName"));
        TableColumn<StudentInfo, String> genderCol = new TableColumn<>("性别");
        genderCol.setCellValueFactory(new PropertyValueFactory<>("gender"));
        TableColumn<StudentInfo, String> deptCol = new TableColumn<>("院系");
        deptCol.setCellValueFactory(new PropertyValueFactory<>("department"));
        TableColumn<StudentInfo, String> majorCol = new TableColumn<>("专业");
        majorCol.setCellValueFactory(new PropertyValueFactory<>("major"));
        TableColumn<StudentInfo, String> roomCol = new TableColumn<>("宿舍");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        TableColumn<StudentInfo, Integer> bedCol = new TableColumn<>("床位");
        bedCol.setCellValueFactory(new PropertyValueFactory<>("bedNumber"));
        TableColumn<StudentInfo, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(noCol, nameCol, genderCol, deptCol, majorCol, roomCol, bedCol, statusCol);
        pane.setCenter(table);

        searchBtn.setOnAction(e -> loadStudents(table, searchField.getText()));
        loadStudents(table, "");

        return pane;
    }

    // 5. 公告管理
    private Pane buildAnnouncementPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        HBox toolbar = new HBox(15);
        Button newBtn = new Button("➕ 发布公告");
        Button editBtn = new Button("✏ 编辑");
        Button deleteBtn = new Button("🗑 删除");
        Button archiveBtn = new Button("📁 归档");
        toolbar.getChildren().addAll(newBtn, editBtn, deleteBtn, archiveBtn);
        pane.setTop(toolbar);

        TableView<Announcement> table = new TableView<>();
        TableColumn<Announcement, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Announcement, String> titleCol = new TableColumn<>("标题");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Announcement, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Announcement, String> publisherCol = new TableColumn<>("发布人");
        publisherCol.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        TableColumn<Announcement, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<Announcement, String> timeCol = new TableColumn<>("发布时间");
        timeCol.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() == null) return null;
            return javafx.beans.binding.Bindings.createStringBinding(() ->
                cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );
        });

        table.getColumns().addAll(idCol, titleCol, typeCol, publisherCol, statusCol, timeCol);
        pane.setCenter(table);

        loadAnnouncements(table);

        return pane;
    }

    // 6. 统计面板
    private Pane buildStatsPane() {
        VBox pane = new VBox(30);
        pane.setPadding(new Insets(30));
        pane.setStyle("-fx-background-color: #F5F7FA;");

        Label title = new Label("📈 系统数据统计");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#333"));

        // 卡片行
        HBox cardRow = new HBox(20);
        String[][] statsCards = {
            {"报修总数", "#FF5722", "totalOrders"},
            {"学生总数", "#4CAF50", "totalStudents"},
            {"宿舍总数", "#2196F3", "totalRooms"},
            {"满意度", "#FFC107", "avgRating"}
        };
        for (String[] card : statsCards) {
            cardRow.getChildren().add(createCard(card[0], card[1], "--"));
        }

        // 图表行
        HBox chartRow = new HBox(20);
        chartRow.setPrefHeight(300);

        VBox leftChart = new VBox(15);
        leftChart.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12;");
        leftChart.setPrefWidth(580);
        leftChart.getChildren().add(new Label("📊 报修类型分布"));
        PieChart typePie = new PieChart();
        typePie.setId("statsTypePie");
        leftChart.getChildren().add(typePie);

        VBox rightChart = new VBox(15);
        rightChart.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12;");
        rightChart.setPrefWidth(580);
        rightChart.getChildren().add(new Label("📈 月度趋势"));
        LineChart<String, Number> lineChart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        lineChart.setId("statsLineChart");
        rightChart.getChildren().add(lineChart);

        chartRow.getChildren().addAll(leftChart, rightChart);

        pane.getChildren().addAll(title, cardRow, chartRow);

        new Thread(this::loadStatsData).start();

        return pane;
    }

    // 7. 学生端首页
    private Pane buildStudentHomePane() {
        VBox pane = new VBox(25);
        pane.setPadding(new Insets(30));

        // 欢迎卡片
        HBox welcomeCard = new HBox(20);
        welcomeCard.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                             "-fx-background-radius: 16; -fx-padding: 30;");
        VBox welcomeText = new VBox(10);
        welcomeText.setPrefWidth(400);
        Label hello = new Label("同学，你好！");
        hello.setStyle("-fx-font-size: 28; -fx-text-fill: white; -fx-font-weight: bold;");
        Label info = new Label("欢迎使用宿舍管理系统");
        info.setStyle("-fx-font-size: 16; -fx-text-fill: rgba(255,255,255,0.9);");
        Label detail = new Label(currentStudentInfo != null ?
            String.format("院系：%s | 专业：%s | 宿舍：%s", 
                currentStudentInfo.getDepartment(),
                currentStudentInfo.getMajor(),
                currentStudentInfo.getRoomNumber()) : "");
        detail.setStyle("-fx-font-size: 14; -fx-text-fill: rgba(255,255,255,0.8);");
        welcomeText.getChildren().addAll(hello, info, detail);
        welcomeCard.getChildren().add(welcomeText);

        // 快捷功能
        HBox quickActions = new HBox(15);
        quickActions.setPadding(new Insets(20, 0, 0, 0));
        String[] actions = {"🔧 我要报修", "📰 查看公告", "👤 个人资料", "💬 联系宿管"};
        for (String act : actions) {
            Button btn = new Button(act);
            btn.setPrefSize(180, 60);
            btn.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #E0E0E0; " +
                         "-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");
            quickActions.getChildren().add(btn);
        }

        // 我的报修单
        VBox myOrders = new VBox(15);
        myOrders.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20;");
        myOrders.getChildren().add(new Label("📋 我的报修记录"));
        TableView<RepairOrder> orderTable = new TableView<>();
        orderTable.setPrefHeight(200);
        TableColumn<RepairOrder, String> descCol = new TableColumn<>("问题描述");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<RepairOrder, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<RepairOrder, String> timeCol = new TableColumn<>("提交时间");
        timeCol.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() == null) return null;
            return javafx.beans.binding.Bindings.createStringBinding(() ->
                cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            );
        });
        orderTable.getColumns().addAll(descCol, statusCol, timeCol);
        myOrders.getChildren().add(orderTable);

        pane.getChildren().addAll(welcomeCard, quickActions, myOrders);

        new Thread(() -> loadMyOrders(orderTable)).start();

        return pane;
    }

    // 8. 学生报修面板
    private Pane buildStudentRepairPane() {
        VBox pane = new VBox(25);
        pane.setPadding(new Insets(30));

        // 报修表单
        VBox formBox = new VBox(15);
        formBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 30;");
        formBox.getChildren().add(new Label("🔧 提交报修申请"));

        TextField nameField = new TextField();
        nameField.setPromptText("报修人姓名");
        nameField.setText(currentStudentInfo != null ? currentStudentInfo.getRealName() : "");

        TextField roomField = new TextField();
        roomField.setPromptText("宿舍号");
        roomField.setText(currentStudentInfo != null ? currentStudentInfo.getRoomNumber() : "");

        TextArea descArea = new TextArea();
        descArea.setPromptText("详细描述问题（不少于10字）");
        descArea.setPrefRowCount(4);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("水电", "家具", "网络", "门窗", "其他");
        typeBox.setValue("其他");

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("Low", "Normal", "High", "Urgent");
        priorityBox.setValue("Normal");

        Button submitBtn = new Button("🚀 提交报修");
        submitBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 16; -fx-pref-height: 45;");

        formBox.getChildren().addAll(
            new Label("报修人:"), nameField,
            new Label("宿舍号:"), roomField,
            new Label("报修类型:"), typeBox,
            new Label("优先级:"), priorityBox,
            new Label("问题描述:"), descArea,
            submitBtn
        );

        pane.getChildren().add(formBox);
        return pane;
    }

    // 9. 学生公告面板
    private Pane buildStudentNoticePane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(25));

        ListView<Announcement> listView = new ListView<>();
        listView.setCellFactory(param -> new ListCell<Announcement>() {
            @Override
            protected void updateItem(Announcement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String color = "🔴".equals(item.getType()) ? "#F44336" : "📢".equals(item.getType()) ? "#2196F3" : "#4CAF50";
                    String emoji = "紧急".equals(item.getType()) ? "🔴" : "通知".equals(item.getType()) ? "📢" : "📌";
                    setText(String.format("%s %s\n%s\n发布: %s | %s",
                        emoji, item.getTitle(),
                        item.getContent().length() > 60 ? item.getContent().substring(0, 60) + "..." : item.getContent(),
                        item.getPublisher(),
                        item.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    ));
                    setWrapText(true);
                    setPrefHeight(100);
                }
            }
        });

        pane.getChildren().add(new Label("📢 最新通知公告"), listView);
        new Thread(() -> loadStudentAnnouncements(listView)).start();
        return pane;
    }

    // 10. 学生资料面板
    private Pane buildStudentProfilePane() {
        VBox pane = new VBox(25);
        pane.setPadding(new Insets(30));

        if (currentStudentInfo == null) {
            pane.getChildren().add(new Label("未找到学生信息"));
            return pane;
        }

        VBox card = new VBox(20);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 35;");
        card.getChildren().add(new Label("👤 个人档案"));

        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(15);

        addGridRow(grid, 0, "姓名", currentStudentInfo.getRealName());
        addGridRow(grid, 1, "学号", currentStudentInfo.getStudentNo());
        addGridRow(grid, 2, "性别", currentStudentInfo.getGender());
        addGridRow(grid, 3, "手机", currentStudentInfo.getPhone());
        addGridRow(grid, 4, "院系", currentStudentInfo.getDepartment());
        addGridRow(grid, 5, "专业", currentStudentInfo.getMajor());
        addGridRow(grid, 6, "班级", currentStudentInfo.getClassName());
        addGridRow(grid, 7, "宿舍", currentStudentInfo.getRoomNumber());
        addGridRow(grid, 8, "床位", String.valueOf(currentStudentInfo.getBedNumber()));
        addGridRow(grid, 9, "入住时间", currentStudentInfo.getCheckInDate() != null ?
            currentStudentInfo.getCheckInDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");

        card.getChildren().add(grid);
        pane.getChildren().add(card);

        return pane;
    }

    // ========== 辅助方法 ==========

    private VBox createCard(String title, String color, String value) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle(String.format("-fx-background-color: white; -fx-background-radius: 12; " +
                                   "-fx-border-radius: 12; -fx-border-width: 2; -fx-border-color: %s;", color));
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 14;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 28; -fx-font-weight: bold;", color));
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private void addGridRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        grid.add(lbl, 0, row);
        grid.add(new Label(value != null ? value : ""), 1, row);
    }

    // ========== 数据加载方法 ==========

    private void loadDashboardData() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/api/dashboard"))
                    .GET()
                    .build();
            String json = client.send(req, HttpResponse.BodyHandlers.ofString()).body();
            JsonNode data = mapper.readTree(json);
            javafx.application.Platform.runLater(() -> {
                // 更新卡片
                updateCard("#studentsCard", data.get("totalStudents").asText());
                updateCard("#roomsCard", data.get("totalRooms").asText());
                updateCard("#pendingCard", data.get("pendingOrders").asText());
                updateCard("#resolvedCard", data.get("resolvedOrders").asText());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCard(String cardId, String value) {
        // 简化：这里只是占位
    }

    private void loadOrders(@SuppressWarnings("rawtypes") TableView table, String status, String keyword) {
        try {
            String url = SERVER_URL + "/api/orders";
            if (!"全部".equals(status) && !status.isEmpty()) {
                url += "?status=" + status;
            }
            String json = fetchJson(url);
            List<RepairOrder> orders = mapper.readValue(json, new TypeReference<List<RepairOrder>>() {});
            javafx.application.Platform.runLater(() -> {
                table.setItems(FXCollections.observableArrayList(orders));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDormRooms(@SuppressWarnings("rawtypes") TableView table, String building, String status) {
        try {
            String url = SERVER_URL + "/api/rooms";
            String json = fetchJson(url);
            List<DormRoom> rooms = mapper.readValue(json, new TypeReference<List<DormRoom>>() {});
            javafx.application.Platform.runLater(() -> {
                table.setItems(FXCollections.observableArrayList(rooms));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStudents(@SuppressWarnings("rawtypes") TableView table, String keyword) {
        try {
            String url = SERVER_URL + "/api/students";
            if (keyword != null && !keyword.isEmpty()) {
                url += "?keyword=" + java.net.URLEncoder.encode(keyword, "UTF-8");
            }
            String json = fetchJson(url);
            List<StudentInfo> students = mapper.readValue(json, new TypeReference<List<StudentInfo>>() {});
            javafx.application.Platform.runLater(() -> {
                table.setItems(FXCollections.observableArrayList(students));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAnnouncements(@SuppressWarnings("rawtypes") TableView table) {
        try {
            String json = fetchJson(SERVER_URL + "/api/announcements/all");
            List<Announcement> anns = mapper.readValue(json, new TypeReference<List<Announcement>>() {});
            javafx.application.Platform.runLater(() -> {
                table.setItems(FXCollections.observableArrayList(anns));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMyOrders(@SuppressWarnings("rawtypes") TableView table) {
        try {
            String json = fetchJson(SERVER_URL + "/api/orders");
            List<RepairOrder> orders = mapper.readValue(json, new TypeReference<List<RepairOrder>>() {});
            if (currentStudentInfo != null) {
                orders.removeIf(o -> !currentStudentInfo.getRealName().equals(o.getStudentName()));
            }
            javafx.application.Platform.runLater(() -> {
                table.setItems(FXCollections.observableArrayList(orders));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStudentAnnouncements(ListView<Announcement> listView) {
        try {
            String json = fetchJson(SERVER_URL + "/api/announcements");
            List<Announcement> anns = mapper.readValue(json, new TypeReference<List<Announcement>>() {});
            javafx.application.Platform.runLater(() -> {
                listView.setItems(FXCollections.observableArrayList(anns));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStatsData() {
        try {
            String json = fetchJson(SERVER_URL + "/api/dashboard");
            JsonNode data = mapper.readTree(json);
            // 可以在这里更新图表
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String fetchJson(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        return res.body();
    }

    public static void main(String[] args) {
        launch(args);
    }
}