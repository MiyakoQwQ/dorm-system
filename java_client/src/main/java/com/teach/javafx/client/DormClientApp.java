package com.teach.javafx.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import com.teach.javafx.entity.Announcement;
import com.teach.javafx.entity.DormRoom;
import com.teach.javafx.entity.RepairOrder;
import com.teach.javafx.entity.StudentInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DormClientApp extends Application {
    public static final String SERVER_URL = "http://localhost:22223";
    private Stage window;
    private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static String currentUserRole = "";
    private static Long currentUserId = null;
    private static String currentUsername = "";
    private TabPane mainTabPane;
    private StudentInfo currentStudentInfo;
    
    // 仪表盘统计卡片引用
    private Label studentsCountLabel;
    private Label roomsCountLabel;
    private Label pendingCountLabel;
    private Label resolvedCountLabel;
    
    // 学生管理表格引用（用于导入后刷新）
    @SuppressWarnings("rawtypes")
    private TableView studentTableRef;
    
    // 公告管理表格引用（用于仪表盘发布后刷新）
    @SuppressWarnings("rawtypes")
    private TableView announcementTableRef;
    
    // 仪表盘定时刷新任务
    private java.util.Timer dashboardRefreshTimer;

    @Override
    public void start(Stage primaryStage) {
        this.window = primaryStage;
        showSplashScreen();
    }

    // ========== 启动界面 ==========
    private void showSplashScreen() {
        VBox splash = new VBox(20);
        splash.setAlignment(Pos.CENTER);
        splash.setPadding(new Insets(50));
        splash.setStyle("-fx-background-color: #f0f0f0;");

        // 系统标题
        Label title = new Label("宿舍管理系统");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));

        // 进度条
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(30, 30);

        splash.getChildren().addAll(title, progress);
        Scene scene = new Scene(splash, 400, 200);
        window.setScene(scene);
        window.setTitle("宿舍管理系统 - 启动中...");
        window.show();

        // 1秒后跳转登录
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(this::showLoginScreen);
        }).start();
    }

    // ========== 登录界面 ==========
    private void showLoginScreen() {
        try {
            // 使用 FXML 加载登录界面
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 400, 500);
            window.setScene(scene);
            window.setTitle("宿舍管理系统 - 登录");
            window.setResizable(false);
        } catch (Exception e) {
            e.printStackTrace();
            // 如果 FXML 加载失败，回退到代码构建的界面
            showLoginScreenLegacy();
        }
    }
    
    // ========== 登录界面（旧版代码构建，作为备用）==========
    private void showLoginScreenLegacy() {
        VBox loginRoot = new VBox(15);
        loginRoot.setPadding(new Insets(30));
        loginRoot.setAlignment(Pos.CENTER);
        loginRoot.setStyle("-fx-background-color: #e8e8e8;");

        // 标题
        Label title = new Label("宿舍管理系统");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 20));

        // 登录面板
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(25));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        panel.setMaxWidth(300);

        Label loginTitle = new Label("用户登录");
        loginTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));

        TextField userField = new TextField();
        userField.setPromptText("账号");

        PasswordField passField = new PasswordField();
        passField.setPromptText("密码");

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER);
        Button loginBtn = new Button("登录");
        Button registerBtn = new Button("注册");
        buttonRow.getChildren().addAll(loginBtn, registerBtn);

        Label msgLabel = new Label();
        msgLabel.setStyle("-fx-text-fill: red;");

        panel.getChildren().addAll(loginTitle, userField, passField, buttonRow, msgLabel);

        loginRoot.getChildren().addAll(title, panel);

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
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

                if (res.statusCode() == 200) {
                    JsonNode node = mapper.readTree(res.body());
                    currentUserRole = node.get("role").asText();
                    currentUserId = node.get("id").asLong();
                    currentUsername = username;
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
        Stage registerStage = new Stage();
        registerStage.setTitle("学生注册");
        
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f5f5f5;");
        
        Label titleLabel = new Label("学生账号注册");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");
        usernameField.setMaxWidth(250);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setMaxWidth(250);
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("请确认密码");
        confirmPasswordField.setMaxWidth(250);
        
        Label messageLabel = new Label();
        messageLabel.setTextFill(Color.RED);
        
        Button registerBtn = new Button("注册");
        registerBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        registerBtn.setPrefWidth(100);
        
        Button cancelBtn = new Button("取消");
        cancelBtn.setPrefWidth(100);
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(registerBtn, cancelBtn);
        
        root.getChildren().addAll(titleLabel, usernameField, passwordField, confirmPasswordField, messageLabel, buttonBox);
        
        // 注册按钮事件
        registerBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            
            // 验证输入
            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("用户名和密码不能为空");
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                messageLabel.setText("两次输入的密码不一致");
                return;
            }
            
            if (username.length() < 3 || username.length() > 20) {
                messageLabel.setText("用户名长度应为3-20个字符");
                return;
            }
            
            if (password.length() < 6) {
                messageLabel.setText("密码长度至少为6位");
                return;
            }
            
            // 调用注册接口
            performRegister(username, password, messageLabel, registerStage);
        });
        
        // 取消按钮事件
        cancelBtn.setOnAction(e -> registerStage.close());
        
        Scene scene = new Scene(root, 400, 350);
        registerStage.setScene(scene);
        registerStage.centerOnScreen();
        registerStage.show();
    }
    
    // 执行注册请求
    private void performRegister(String username, String password, Label messageLabel, Stage registerStage) {
        try {
            String jsonBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", 
                username.replace("\"", "\\\""), password.replace("\"", "\\\""));
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/api/users/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            
            if (response.statusCode() == 200) {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("注册成功");
                successAlert.setHeaderText(null);
                successAlert.setContentText("注册成功！请使用新账号登录。");
                successAlert.showAndWait();
                registerStage.close();
            } else if (response.statusCode() == 409) {
                messageLabel.setText("该用户名已被占用");
            } else {
                messageLabel.setText("注册失败：" + response.body());
            }
        } catch (Exception ex) {
            messageLabel.setText("网络错误：" + ex.getMessage());
        }
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
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (res.statusCode() == 200) {
                currentStudentInfo = mapper.readValue(res.body(), StudentInfo.class);
            }
        } catch (Exception e) {
            System.err.println("加载学生信息失败: " + e.getMessage());
        }
        showMainWorkspace();
    }

    // ========== 主工作区 ==========
    private void showMainWorkspace() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");

        // ===== 顶部标题栏 =====
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setStyle("-fx-background-color: #e0e0e0;");
        HBox.setHgrow(topBar, Priority.ALWAYS);

        // 标题
        Label appTitle = new Label("宿舍管理系统");
        appTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));

        // 用户信息
        HBox userGroup = new HBox(10);
        userGroup.setAlignment(Pos.CENTER_RIGHT);
        Label welcome = new Label("欢迎：" + (currentStudentInfo != null ? currentStudentInfo.getRealName() : "管理员"));
        Label roleLabel = new Label("[" + ("ADMIN".equals(currentUserRole) ? "管理员" : "学生") + "]");
        Button logoutBtn = new Button("退出");
        logoutBtn.setOnAction(e -> {
            currentUserRole = "";
            currentUserId = null;
            currentUsername = "";
            currentStudentInfo = null;
            showLoginScreen();
        });
        userGroup.getChildren().addAll(welcome, roleLabel, logoutBtn);

        topBar.getChildren().addAll(appTitle, new Region(), userGroup);
        root.setTop(topBar);

        // ===== 左侧导航 =====
        VBox sidebar = new VBox(3);
        sidebar.setPrefWidth(150);
        sidebar.setPadding(new Insets(10));
        sidebar.setStyle("-fx-background-color: #e8e8e8;");

        Label navTitle = new Label("功能菜单");
        navTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 14));
        navTitle.setPadding(new Insets(0, 0, 10, 5));
        sidebar.getChildren().add(navTitle);

        String[] adminTabs = {"仪表盘", "报修管理", "宿舍管理", "学生管理", "公告管理", "系统统计"};
        String[] studentTabs = {"我的首页", "我要报修", "通知公告", "个人资料"};

        String[] tabs = "ADMIN".equals(currentUserRole) ? adminTabs : studentTabs;
        for (int i = 0; i < tabs.length; i++) {
            Button navBtn = new Button(tabs[i]);
            navBtn.setPrefWidth(130);
            navBtn.setAlignment(Pos.CENTER_LEFT);
            int tabIndex = i + 1;
            navBtn.setOnAction(e -> {
                if (tabIndex < mainTabPane.getTabs().size()) {
                    mainTabPane.getSelectionModel().select(tabIndex);
                }
            });
            sidebar.getChildren().add(navBtn);
        }

        root.setLeft(sidebar);

        // ===== 中心区域（TabPane） =====
        mainTabPane = new TabPane();
        mainTabPane.setSide(Side.TOP);
        mainTabPane.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // 监听标签切换，控制仪表盘定时刷新
        mainTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int index = newVal.intValue();
            // 仪表盘是第2个标签（索引1），切换到仪表盘时启动定时器，离开则停止
            if (index == 1) {
                startDashboardRefreshTimer();
            } else {
                stopDashboardRefreshTimer();
            }
        });
        
        // 默认选中仪表盘标签（管理员）或学生首页（学生）
        if ("ADMIN".equals(currentUserRole)) {
            // 延迟一点再选中，确保面板已构建完成
            javafx.application.Platform.runLater(() -> {
                if (mainTabPane.getTabs().size() > 1) {
                    mainTabPane.getSelectionModel().select(1); // 选中仪表盘
                }
            });
        }

        // 空的首页Tab（用于占位）
        Tab homeTab = new Tab();
        homeTab.setDisable(true);
        mainTabPane.getTabs().add(homeTab);

        // 根据角色添加功能Tab
        if ("ADMIN".equals(currentUserRole)) {
            Tab dashboardTab = new Tab("仪表盘");
            dashboardTab.setContent(buildDashboardPane());
            mainTabPane.getTabs().add(dashboardTab);

            Tab orderTab = new Tab("报修管理");
            orderTab.setContent(buildOrderManagementPane());
            mainTabPane.getTabs().add(orderTab);

            Tab dormTab = new Tab("宿舍管理");
            dormTab.setContent(buildDormManagementPane());
            mainTabPane.getTabs().add(dormTab);

            Tab studentTab = new Tab("学生管理");
            studentTab.setContent(buildStudentManagementPane());
            mainTabPane.getTabs().add(studentTab);

            Tab announcementTab = new Tab("公告管理");
            announcementTab.setContent(buildAnnouncementPane());
            mainTabPane.getTabs().add(announcementTab);

            Tab statsTab = new Tab("数据统计");
            statsTab.setContent(buildStatsPane());
            mainTabPane.getTabs().add(statsTab);
        } else {
            Tab studentHomeTab = new Tab("我的首页");
            studentHomeTab.setContent(buildStudentHomePane());
            mainTabPane.getTabs().add(studentHomeTab);

            Tab repairTab = new Tab("我要报修");
            repairTab.setContent(buildStudentRepairPane());
            mainTabPane.getTabs().add(repairTab);

            Tab noticeTab = new Tab("通知公告");
            noticeTab.setContent(buildStudentNoticePane());
            mainTabPane.getTabs().add(noticeTab);

            Tab profileTab = new Tab("个人资料");
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
        pane.setPadding(new Insets(15));

        // 顶部统计
        HBox topStats = new HBox(15);
        topStats.setPadding(new Insets(0, 0, 15, 0));

        // 创建统计卡片并保存引用
        studentsCountLabel = createStatCard(topStats, "学生总数", "0");
        roomsCountLabel = createStatCard(topStats, "宿舍总数", "0");
        pendingCountLabel = createStatCard(topStats, "待处理报修", "0");
        resolvedCountLabel = createStatCard(topStats, "已解决报修", "0");
        pane.setTop(topStats);

        // 图表区域
        HBox chartArea = new HBox(15);
        
        VBox leftChart = new VBox(10);
        leftChart.setStyle("-fx-background-color: white; -fx-padding: 15;");
        leftChart.setPrefWidth(400);
        leftChart.getChildren().add(new Label("报修类型分布"));
        PieChart typeChart = new PieChart();
        leftChart.getChildren().add(typeChart);

        VBox rightSection = new VBox(15);
        rightSection.setPrefWidth(400);
        
        VBox quickBox = new VBox(10);
        quickBox.setStyle("-fx-background-color: white; -fx-padding: 15;");
        quickBox.getChildren().add(new Label("快捷操作"));
        String[] actions = {"新增宿舍", "导入学生", "生成报表", "发布公告"};
        for (String act : actions) {
            Button btn = new Button(act);
            btn.setOnAction(e -> {
                switch (act) {
                    case "新增宿舍":
                        showDormRoomDialog(null, null);
                        break;
                    case "导入学生":
                        showImportStudentDialog();
                        break;
                    case "生成报表":
                        showGenerateReportDialog();
                        break;
                    case "发布公告":
                        // 从仪表盘发布公告，保存后提示用户手动刷新公告管理页面
                        showAnnouncementDialog(null, null, true);
                        break;
                }
            });
            quickBox.getChildren().add(btn);
        }
        
        VBox recentBox = new VBox(10);
        recentBox.setStyle("-fx-background-color: white; -fx-padding: 15;");
        recentBox.getChildren().add(new Label("最新报修"));
        TableView<RepairOrder> recentTable = new TableView<>();
        recentTable.setPrefHeight(150);
        recentBox.getChildren().add(recentTable);
        
        rightSection.getChildren().addAll(quickBox, recentBox);
        chartArea.getChildren().addAll(leftChart, rightSection);
        pane.setCenter(chartArea);

        // 初始加载数据（定时器由标签切换监听控制）
        new Thread(this::loadDashboardData).start();
        
        return pane;
    }
    
    /** 启动仪表盘定时刷新 */
    private void startDashboardRefreshTimer() {
        // 先停止之前的定时器（如果有）
        stopDashboardRefreshTimer();
        
        dashboardRefreshTimer = new java.util.Timer("DashboardRefresh", true);
        dashboardRefreshTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                loadDashboardData();
            }
        }, 5000, 5000); // 延迟5秒后开始，每5秒执行一次
    }
    
    /** 停止仪表盘定时刷新 */
    private void stopDashboardRefreshTimer() {
        if (dashboardRefreshTimer != null) {
            dashboardRefreshTimer.cancel();
            dashboardRefreshTimer = null;
        }
    }

    // 2. 报修管理
    private Pane buildOrderManagementPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(15));

        // 工具栏
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("全部", "待处理", "处理中", "已解决", "已驳回");
        statusFilter.setValue("全部");

        TextField searchField = new TextField();
        searchField.setPromptText("搜索宿舍号/姓名");
        searchField.setPrefWidth(150);

        Button searchBtn = new Button("搜索");
        Button refreshBtn = new Button("刷新");

        toolbar.getChildren().addAll(
            new Label("状态:"), statusFilter,
            new Label("搜索:"), searchField, searchBtn, refreshBtn
        );
        pane.setTop(toolbar);

        // 表格
        TableView<RepairOrder> table = new TableView<>();
        table.setId("orderTable");

        TableColumn<RepairOrder, Long> idCol = new TableColumn<>("单号");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<RepairOrder, String> nameCol = new TableColumn<>("报修人");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        TableColumn<RepairOrder, String> buildingCol = new TableColumn<>("宿舍楼");
        buildingCol.setCellValueFactory(new PropertyValueFactory<>("building"));
        TableColumn<RepairOrder, String> roomCol = new TableColumn<>("宿舍号");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        TableColumn<RepairOrder, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("repairType"));
        TableColumn<RepairOrder, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cell -> {
            String status = cell.getValue().getStatus();
            String displayStatus;
            switch (status) {
                case "Pending": displayStatus = "待处理"; break;
                case "Processing": displayStatus = "处理中"; break;
                case "Resolved": displayStatus = "已解决"; break;
                case "Rejected": displayStatus = "已驳回"; break;
                default: displayStatus = status;
            }
            return javafx.beans.binding.Bindings.createStringBinding(() -> displayStatus);
        });
        TableColumn<RepairOrder, String> timeCol = new TableColumn<>("提交时间");
        timeCol.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() == null) return null;
            return javafx.beans.binding.Bindings.createStringBinding(() ->
                cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            );
        });

        table.getColumns().addAll(idCol, nameCol, buildingCol, roomCol, typeCol, statusCol, timeCol);
        pane.setCenter(table);

        // 操作区
        HBox actionBar = new HBox(10);
        actionBar.setPadding(new Insets(10, 0, 0, 0));
        Button editBtn = new Button("修改");
        Button acceptBtn = new Button("受理");
        Button resolveBtn = new Button("完结");
        Button rejectBtn = new Button("驳回");
        TextArea remarkArea = new TextArea();
        remarkArea.setPromptText("处理意见");
        remarkArea.setPrefRowCount(2);
        remarkArea.setPrefWidth(200);
        actionBar.getChildren().addAll(editBtn, acceptBtn, resolveBtn, rejectBtn, new Label("备注:"), remarkArea);

        // 事件绑定
        refreshBtn.setOnAction(e -> loadOrders(table, statusFilter.getValue(), searchField.getText()));
        searchBtn.setOnAction(e -> loadOrders(table, statusFilter.getValue(), searchField.getText()));
        statusFilter.setOnAction(e -> loadOrders(table, statusFilter.getValue(), searchField.getText()));

        editBtn.setOnAction(e -> {
            RepairOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择一条报修单"); return; }
            showRepairOrderDialog(selected, table, statusFilter.getValue(), searchField.getText());
        });

        acceptBtn.setOnAction(e -> {
            RepairOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择一条报修单"); return; }
            putAction(SERVER_URL + "/api/orders/" + selected.getId() + "/accept", null);
            // 延迟刷新，等待服务器处理完成
            new Thread(() -> {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> 
                    loadOrders(table, statusFilter.getValue(), searchField.getText()));
            }).start();
        });

        resolveBtn.setOnAction(e -> {
            RepairOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择一条报修单"); return; }
            putAction(SERVER_URL + "/api/orders/" + selected.getId() + "/resolve?remark=" +
                    java.net.URLEncoder.encode(remarkArea.getText(), java.nio.charset.StandardCharsets.UTF_8), null);
            new Thread(() -> {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> 
                    loadOrders(table, statusFilter.getValue(), searchField.getText()));
            }).start();
        });

        rejectBtn.setOnAction(e -> {
            RepairOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择一条报修单"); return; }
            putAction(SERVER_URL + "/api/orders/" + selected.getId() + "/reject?remark=" +
                    java.net.URLEncoder.encode(remarkArea.getText(), java.nio.charset.StandardCharsets.UTF_8), null);
            new Thread(() -> {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> 
                    loadOrders(table, statusFilter.getValue(), searchField.getText()));
            }).start();
        });

        // 初始加载
        loadOrders(table, "全部", "");

        return pane;
    }

    // 3. 宿舍管理
    private Pane buildDormManagementPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(15));

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        ComboBox<String> buildingFilter = new ComboBox<>();
        buildingFilter.getItems().addAll("全部楼栋", "一号楼", "二号楼", "三号楼");
        buildingFilter.setValue("全部楼栋");

        Button addBtn = new Button("新增");
        Button editBtn = new Button("编辑");
        Button deleteBtn = new Button("删除");
        Button refreshBtn = new Button("刷新");

        toolbar.getChildren().addAll(
            new Label("楼栋:"), buildingFilter,
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
        statusCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> {
                if (cell.getValue() == null) return "";
                String status = cell.getValue().getStatus();
                // 宿舍状态转换为中文显示
                switch (status) {
                    case "Normal": return "正常";
                    case "Maintenance": return "维修中";
                    case "Closed": return "已关闭";
                    default: return status != null ? status : "";
                }
            }));

        table.getColumns().addAll(idCol, buildingCol, roomCol, floorCol, typeCol, capacityCol, currentCol, statusCol);
        pane.setCenter(table);

        refreshBtn.setOnAction(e -> loadDormRooms(table, buildingFilter.getValue(), ""));
        buildingFilter.setOnAction(e -> loadDormRooms(table, buildingFilter.getValue(), ""));

        addBtn.setOnAction(e -> showDormRoomDialog(null, table));

        editBtn.setOnAction(e -> {
            DormRoom selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择一条宿舍记录"); return; }
            showDormRoomDialog(selected, table);
        });

        deleteBtn.setOnAction(e -> {
            DormRoom selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择要删除的宿舍"); return; }
            deleteAction(SERVER_URL + "/api/rooms/" + selected.getId());
            loadDormRooms(table, buildingFilter.getValue(), "");
        });

        loadDormRooms(table, "全部楼栋", "");

        return pane;
    }

    // 4. 学生管理
    private Pane buildStudentManagementPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(15));

        // 工具栏
        HBox toolbar = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("搜索学号/姓名/宿舍");
        searchField.setPrefWidth(150);
        Button searchBtn = new Button("搜索");
        Button refreshBtn = new Button("刷新");
        Button addBtn = new Button("新增");
        Button editBtn = new Button("编辑");
        Button deleteBtn = new Button("删除");
        toolbar.getChildren().addAll(new Label("搜索:"), searchField, searchBtn, refreshBtn, addBtn, editBtn, deleteBtn);
        pane.setTop(toolbar);

        TableView<StudentInfo> table = new TableView<>();
        
        // 使用自定义cellValueFactory确保数据正确显示
        TableColumn<StudentInfo, String> noCol = new TableColumn<>("学号");
        noCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getStudentNo() : ""));
        
        TableColumn<StudentInfo, String> nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getRealName() : ""));
        
        TableColumn<StudentInfo, String> genderCol = new TableColumn<>("性别");
        genderCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getGender() : ""));
        
        TableColumn<StudentInfo, String> deptCol = new TableColumn<>("院系");
        deptCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getDepartment() : ""));
        
        TableColumn<StudentInfo, String> majorCol = new TableColumn<>("专业");
        majorCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getMajor() : ""));
        
        TableColumn<StudentInfo, String> roomCol = new TableColumn<>("宿舍");
        roomCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getRoomNumber() : ""));
        
        TableColumn<StudentInfo, String> bedCol = new TableColumn<>("床位");
        bedCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null && cell.getValue().getBedNumber() != null 
                ? String.valueOf(cell.getValue().getBedNumber()) : ""));
        
        TableColumn<StudentInfo, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> {
                if (cell.getValue() == null) return "";
                String status = cell.getValue().getStatus();
                // 统一状态显示为中文
                if ("Active".equalsIgnoreCase(status)) return "在住";
                return status != null ? status : "";
            }));

        table.getColumns().addAll(noCol, nameCol, genderCol, deptCol, majorCol, roomCol, bedCol, statusCol);
        pane.setCenter(table);

        searchBtn.setOnAction(e -> loadStudents(table, searchField.getText()));
        refreshBtn.setOnAction(e -> loadStudents(table, ""));
        addBtn.setOnAction(e -> showStudentDialog(null, table));
        
        editBtn.setOnAction(e -> {
            StudentInfo selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择要编辑的学生"); return; }
            showStudentDialog(selected, table);
        });
        
        deleteBtn.setOnAction(e -> {
            StudentInfo selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择要删除的学生"); return; }
            
            // 确认对话框
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("确认删除");
            confirm.setHeaderText("确定要删除学生 " + selected.getRealName() + " (" + selected.getStudentNo() + ") 吗？");
            confirm.setContentText("此操作不可撤销！");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    deleteAction(SERVER_URL + "/api/students/" + selected.getId());
                    loadStudents(table, "");
                }
            });
        });
        
        // 保存表格引用，供导入后刷新使用
        studentTableRef = table;
        
        loadStudents(table, "");

        return pane;
    }

    // 5. 公告管理
    private Pane buildAnnouncementPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(15));

        HBox toolbar = new HBox(10);
        Button newBtn = new Button("新增");
        Button editBtn = new Button("编辑");
        Button deleteBtn = new Button("删除");
        Button refreshBtn = new Button("刷新");
        toolbar.getChildren().addAll(newBtn, editBtn, deleteBtn, refreshBtn);
        pane.setTop(toolbar);

        TableView<Announcement> table = new TableView<>();
        
        // 使用自定义cellValueFactory
        TableColumn<Announcement, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null && cell.getValue().getId() != null 
                ? String.valueOf(cell.getValue().getId()) : ""));
        
        TableColumn<Announcement, String> titleCol = new TableColumn<>("标题");
        titleCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getTitle() : ""));
        
        TableColumn<Announcement, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getType() : ""));
        
        TableColumn<Announcement, String> publisherCol = new TableColumn<>("发布人");
        publisherCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getPublisher() : ""));
        
        TableColumn<Announcement, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> cell.getValue() != null ? cell.getValue().getStatus() : ""));
        
        TableColumn<Announcement, String> timeCol = new TableColumn<>("发布时间");
        timeCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
            () -> {
                if (cell.getValue() == null || cell.getValue().getCreatedAt() == null) return "";
                return cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }));

        table.getColumns().addAll(idCol, titleCol, typeCol, publisherCol, statusCol, timeCol);
        pane.setCenter(table);

        // 保存表格引用，供仪表盘发布后刷新使用
        announcementTableRef = table;
        
        newBtn.setOnAction(e -> showAnnouncementDialog(null, table));

        editBtn.setOnAction(e -> {
            Announcement selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择要编辑的公告"); return; }
            showAnnouncementDialog(selected, table);
        });

        deleteBtn.setOnAction(e -> {
            Announcement selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert("请先选择要删除的公告"); return; }
            deleteAction(SERVER_URL + "/api/announcements/" + selected.getId());
            loadAnnouncements(table);
        });
        
        refreshBtn.setOnAction(e -> loadAnnouncements(table));

        loadAnnouncements(table);

        return pane;
    }

    // 统计标签引用
    private Label statsOrderCountLabel;
    private Label statsStudentCountLabel;
    private Label statsRoomCountLabel;
    private PieChart statsTypePie;
    private LineChart<String, Number> statsLineChart;

    // 6. 统计面板
    private Pane buildStatsPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(15));

        Label title = new Label("数据统计");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));

        // 统计行
        HBox statRow = new HBox(15);
        
        // 报修总数
        VBox orderBox = new VBox(5);
        orderBox.setPadding(new Insets(15));
        orderBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        orderBox.setPrefWidth(150);
        Label orderNameLabel = new Label("报修总数");
        statsOrderCountLabel = new Label("--");
        statsOrderCountLabel.setFont(Font.font(16));
        orderBox.getChildren().addAll(orderNameLabel, statsOrderCountLabel);
        
        // 学生总数
        VBox studentBox = new VBox(5);
        studentBox.setPadding(new Insets(15));
        studentBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        studentBox.setPrefWidth(150);
        Label studentNameLabel = new Label("学生总数");
        statsStudentCountLabel = new Label("--");
        statsStudentCountLabel.setFont(Font.font(16));
        studentBox.getChildren().addAll(studentNameLabel, statsStudentCountLabel);
        
        // 宿舍总数
        VBox roomBox = new VBox(5);
        roomBox.setPadding(new Insets(15));
        roomBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        roomBox.setPrefWidth(150);
        Label roomNameLabel = new Label("宿舍总数");
        statsRoomCountLabel = new Label("--");
        statsRoomCountLabel.setFont(Font.font(16));
        roomBox.getChildren().addAll(roomNameLabel, statsRoomCountLabel);
        
        statRow.getChildren().addAll(orderBox, studentBox, roomBox);

        // 图表
        HBox chartRow = new HBox(15);
        VBox leftChart = new VBox(10);
        leftChart.setStyle("-fx-background-color: white; -fx-padding: 15;");
        leftChart.setPrefWidth(400);
        leftChart.getChildren().add(new Label("报修类型分布"));
        statsTypePie = new PieChart();
        leftChart.getChildren().add(statsTypePie);

        VBox rightChart = new VBox(10);
        rightChart.setStyle("-fx-background-color: white; -fx-padding: 15;");
        rightChart.setPrefWidth(400);
        rightChart.getChildren().add(new Label("月度趋势"));
        statsLineChart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        rightChart.getChildren().add(statsLineChart);

        chartRow.getChildren().addAll(leftChart, rightChart);

        pane.getChildren().addAll(title, statRow, chartRow);

        new Thread(this::loadStatsData).start();
        return pane;
    }

    // 7. 学生端首页
    private Pane buildStudentHomePane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(15));

        // 欢迎信息
        VBox welcomeBox = new VBox(5);
        welcomeBox.setPadding(new Insets(15));
        welcomeBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        Label hello = new Label("你好，" + (currentStudentInfo != null ? currentStudentInfo.getRealName() : "同学"));
        hello.setFont(Font.font(16));
        Label info = new Label(currentStudentInfo != null ?
            String.format("院系：%s | 专业：%s | 宿舍：%s", 
                currentStudentInfo.getDepartment(),
                currentStudentInfo.getMajor(),
                currentStudentInfo.getRoomNumber()) : "");
        welcomeBox.getChildren().addAll(hello, info);

        // 快捷功能
        HBox quickActions = new HBox(10);
        String[] actions = {"我要报修", "通知公告", "个人资料"};
        for (String act : actions) {
            Button btn = new Button(act);
            btn.setOnAction(e -> {
                if ("我要报修".equals(act)) {
                    mainTabPane.getSelectionModel().select(2); // "我要报修"是第3个Tab（索引2）
                } else if ("通知公告".equals(act)) {
                    mainTabPane.getSelectionModel().select(3); // "通知公告"是第4个Tab（索引3）
                } else if ("个人资料".equals(act)) {
                    mainTabPane.getSelectionModel().select(4); // "个人资料"是第5个Tab（索引4）
                }
            });
            quickActions.getChildren().add(btn);
        }

        // 我的报修单
        VBox myOrders = new VBox(10);
        myOrders.setStyle("-fx-background-color: white; -fx-padding: 15;");
        myOrders.getChildren().add(new Label("我的报修记录"));
        TableView<RepairOrder> orderTable = new TableView<>();
        orderTable.setPrefHeight(200);
        
        TableColumn<RepairOrder, String> descCol = new TableColumn<>("问题描述");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);
        
        TableColumn<RepairOrder, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cell -> {
            String status = cell.getValue().getStatus();
            String displayStatus;
            switch (status) {
                case "Pending": displayStatus = "待处理"; break;
                case "Processing": displayStatus = "处理中"; break;
                case "Resolved": displayStatus = "已解决"; break;
                case "Rejected": displayStatus = "已驳回"; break;
                default: displayStatus = status;
            }
            return javafx.beans.binding.Bindings.createStringBinding(() -> displayStatus);
        });
        statusCol.setPrefWidth(80);
        
        TableColumn<RepairOrder, String> timeCol = new TableColumn<>("提交时间");
        timeCol.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() == null) return null;
            return javafx.beans.binding.Bindings.createStringBinding(() ->
                cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            );
        });
        timeCol.setPrefWidth(100);
        
        // 催单列
        TableColumn<RepairOrder, Void> urgeCol = new TableColumn<>("催单");
        urgeCol.setPrefWidth(80);
        urgeCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("催单");
            {
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 2 8;");
                btn.setOnAction(e -> {
                    RepairOrder order = getTableView().getItems().get(getIndex());
                    handleUrgeOrder(order, orderTable);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    RepairOrder order = getTableView().getItems().get(getIndex());
                    // 只有待处理和处理中的订单可以催单，且最多3次
                    boolean canUrge = ("Pending".equals(order.getStatus()) || "Processing".equals(order.getStatus()))
                            && order.getUrgeCount() != null && order.getUrgeCount() < 3;
                    btn.setDisable(!canUrge);
                    if (order.getUrgeCount() != null && order.getUrgeCount() > 0) {
                        btn.setText("催(" + order.getUrgeCount() + ")");
                    } else {
                        btn.setText("催单");
                    }
                    setGraphic(btn);
                }
            }
        });
        
        // 评价列
        TableColumn<RepairOrder, Void> reviewCol = new TableColumn<>("评价");
        reviewCol.setPrefWidth(80);
        reviewCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("评价");
            {
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 2 8;");
                btn.setOnAction(e -> {
                    RepairOrder order = getTableView().getItems().get(getIndex());
                    showReviewDialog(order, orderTable);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    RepairOrder order = getTableView().getItems().get(getIndex());
                    // 只有已解决的订单可以评价，且只能评价一次
                    boolean canReview = "Resolved".equals(order.getStatus()) && order.getRating() == null;
                    btn.setDisable(!canReview);
                    if (order.getRating() != null) {
                        btn.setText("★" + order.getRating());
                    } else {
                        btn.setText("评价");
                    }
                    setGraphic(btn);
                }
            }
        });
        
        orderTable.getColumns().addAll(descCol, statusCol, timeCol, urgeCol, reviewCol);
        myOrders.getChildren().add(orderTable);

        pane.getChildren().addAll(welcomeBox, quickActions, myOrders);

        new Thread(() -> loadMyOrders(orderTable)).start();
        return pane;
    }

    // 8. 学生报修面板
    private Pane buildStudentRepairPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(15));

        // 报修表单
        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(15));
        formBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        formBox.getChildren().add(new Label("提交报修"));

        TextField nameField = new TextField();
        nameField.setPromptText("报修人姓名");
        nameField.setText(currentStudentInfo != null ? currentStudentInfo.getRealName() : "");

        TextField buildingField = new TextField();
        buildingField.setPromptText("如：1号楼、A座等");

        TextField roomField = new TextField();
        roomField.setPromptText("宿舍号");
        roomField.setText(currentStudentInfo != null ? currentStudentInfo.getRoomNumber() : "");

        TextArea descArea = new TextArea();
        descArea.setPromptText("问题描述（不少于10字）");
        descArea.setPrefRowCount(3);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("水电", "家具", "网络", "门窗", "其他");
        typeBox.setValue("其他");

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("普通", "紧急");
        priorityBox.setValue("普通");

        Button submitBtn = new Button("提交报修");

        Label resultMsg = new Label();

        submitBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty() || buildingField.getText().isEmpty() || roomField.getText().isEmpty() || descArea.getText().isEmpty()) {
                resultMsg.setStyle("-fx-text-fill: red;");
                resultMsg.setText("请填写完整信息（包括宿舍楼）");
                return;
            }
            if (descArea.getText().length() < 10) {
                resultMsg.setStyle("-fx-text-fill: red;");
                resultMsg.setText("问题描述不少于10个字");
                return;
            }
            try {
                String body = String.format(
                    "{\"studentName\":\"%s\",\"building\":\"%s\",\"roomNumber\":\"%s\",\"repairType\":\"%s\",\"priority\":\"%s\",\"description\":\"%s\",\"userId\":%d}",
                    nameField.getText().replace("\"", "'"),
                    buildingField.getText().replace("\"", "'"),
                    roomField.getText().replace("\"", "'"),
                    typeBox.getValue(),
                    priorityBox.getValue(),
                    descArea.getText().replace("\"", "'").replace("\n", " "),
                    currentUserId
                );
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_URL + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    resultMsg.setStyle("-fx-text-fill: green;");
                    resultMsg.setText("报修提交成功！");
                    descArea.clear();
                } else {
                    resultMsg.setStyle("-fx-text-fill: red;");
                    resultMsg.setText("提交失败: " + res.body());
                }
            } catch (Exception ex) {
                resultMsg.setStyle("-fx-text-fill: red;");
                resultMsg.setText("网络错误");
                ex.printStackTrace();
            }
        });

        formBox.getChildren().addAll(
            new Label("报修人:"), nameField,
            new Label("宿舍楼:"), buildingField,
            new Label("宿舍号:"), roomField,
            new Label("报修类型:"), typeBox,
            new Label("优先级:"), priorityBox,
            new Label("问题描述:"), descArea,
            submitBtn, resultMsg
        );

        pane.getChildren().add(formBox);
        return pane;
    }

    // 9. 学生公告面板
    private Pane buildStudentNoticePane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(15));

        ListView<Announcement> listView = new ListView<>();
        listView.setCellFactory(param -> new ListCell<Announcement>() {
            @Override
            protected void updateItem(Announcement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("[%s] %s\n%s\n发布: %s | %s",
                        item.getType(), item.getTitle(),
                        item.getContent().length() > 50 ? item.getContent().substring(0, 50) + "..." : item.getContent(),
                        item.getPublisher(),
                        item.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    ));
                    setWrapText(true);
                    setPrefHeight(80);
                }
            }
        });

        pane.getChildren().addAll(new Label("通知公告"), listView);
        new Thread(() -> loadStudentAnnouncements(listView)).start();
        return pane;
    }

    // 10. 学生资料面板
    private Pane buildStudentProfilePane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(15));

        if (currentStudentInfo == null) {
            pane.getChildren().add(new Label("未找到学生信息"));
            return pane;
        }

        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(15));
        infoBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        infoBox.getChildren().add(new Label("个人资料"));

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);

        addGridRow(grid, 0, "姓名", currentStudentInfo.getRealName());
        addGridRow(grid, 1, "学号", currentStudentInfo.getStudentNo());
        addGridRow(grid, 2, "性别", currentStudentInfo.getGender());
        addGridRow(grid, 3, "手机", currentStudentInfo.getPhone());
        addGridRow(grid, 4, "院系", currentStudentInfo.getDepartment());
        addGridRow(grid, 5, "专业", currentStudentInfo.getMajor());
        addGridRow(grid, 6, "班级", currentStudentInfo.getClassName());
        addGridRow(grid, 7, "年级", currentStudentInfo.getGrade() != null ? currentStudentInfo.getGrade() + "级" : "");
        addGridRow(grid, 8, "宿舍", currentStudentInfo.getRoomNumber());
        addGridRow(grid, 9, "床位", currentStudentInfo.getBedNumber() != null ? currentStudentInfo.getBedNumber() + "号床" : "");
        addGridRow(grid, 10, "入住时间", currentStudentInfo.getCheckInDate() != null ?
            currentStudentInfo.getCheckInDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
        addGridRow(grid, 11, "状态", currentStudentInfo.getStatus());

        infoBox.getChildren().add(grid);
        pane.getChildren().add(infoBox);

        return pane;
    }

    // ========== 辅助方法 ==========

    private VBox createCard(String title, String color, String value) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        card.setPrefWidth(120);
        Label titleLabel = new Label(title);
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font(16));
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
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            String json = client.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8)).body();
            System.out.println("[DEBUG] Dashboard data: " + json);
            JsonNode data = mapper.readTree(json);
            javafx.application.Platform.runLater(() -> {
                // 直接更新标签
                if (studentsCountLabel != null && data.has("totalStudents")) {
                    studentsCountLabel.setText(data.get("totalStudents").asText());
                }
                if (roomsCountLabel != null && data.has("totalRooms")) {
                    roomsCountLabel.setText(data.get("totalRooms").asText());
                }
                if (pendingCountLabel != null && data.has("pendingOrders")) {
                    pendingCountLabel.setText(data.get("pendingOrders").asText());
                }
                if (resolvedCountLabel != null && data.has("resolvedOrders")) {
                    resolvedCountLabel.setText(data.get("resolvedOrders").asText());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCard(String cardId, String value) {
        // 简化：这里只是占位
    }
    
    /** 创建统计卡片并返回数值标签引用 */
    private Label createStatCard(HBox container, String title, String initialValue) {
        VBox statBox = new VBox(5);
        statBox.setPadding(new Insets(15));
        statBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        statBox.setPrefWidth(150);
        Label titleLabel = new Label(title);
        Label valueLabel = new Label(initialValue);
        valueLabel.setFont(Font.font(18));
        statBox.getChildren().addAll(titleLabel, valueLabel);
        container.getChildren().add(statBox);
        return valueLabel;
    }

    private void loadOrders(@SuppressWarnings("rawtypes") TableView table, String status, String keyword) {
        try {
            String url = SERVER_URL + "/api/orders";
            // 将中文状态转换为英文状态
            String statusParam = status;
            if ("待处理".equals(status)) statusParam = "Pending";
            else if ("处理中".equals(status)) statusParam = "Processing";
            else if ("已解决".equals(status)) statusParam = "Resolved";
            else if ("已驳回".equals(status)) statusParam = "Rejected";
            
            if (!"全部".equals(status) && !status.isEmpty()) {
                url += "?status=" + statusParam;
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                url += (url.contains("?") ? "&" : "?") + "keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
            }
            System.out.println("[DEBUG] loadOrders URL: " + url);
            String json = fetchJson(url);
            System.out.println("[DEBUG] loadOrders response: " + json);
            List<RepairOrder> orders = mapper.readValue(json, new TypeReference<List<RepairOrder>>() {});
            javafx.application.Platform.runLater(() -> {
                table.getItems().clear();
                table.setItems(FXCollections.observableArrayList(orders));
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ERROR] loadOrders failed: " + e.getMessage());
        }
    }

    private void loadDormRooms(@SuppressWarnings("rawtypes") TableView table, String building, String status) {
        try {
            String url = SERVER_URL + "/api/rooms";
            // 添加楼栋筛选参数
            if (building != null && !"全部楼栋".equals(building) && !building.isEmpty()) {
                url += "?building=" + java.net.URLEncoder.encode(building, java.nio.charset.StandardCharsets.UTF_8);
            }
            System.out.println("[DEBUG] loadDormRooms URL: " + url);
            String json = fetchJson(url);
            System.out.println("[DEBUG] loadDormRooms response: " + json);
            List<DormRoom> rooms = mapper.readValue(json, new TypeReference<List<DormRoom>>() {});
            javafx.application.Platform.runLater(() -> {
                table.getItems().clear();
                table.setItems(FXCollections.observableArrayList(rooms));
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ERROR] loadDormRooms failed: " + e.getMessage());
        }
    }

    private void loadStudents(@SuppressWarnings("rawtypes") TableView table, String keyword) {
        try {
            String url = SERVER_URL + "/api/students";
            if (keyword != null && !keyword.isEmpty()) {
                url += "?keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
            }
            System.out.println("[DEBUG] loadStudents URL: " + url);
            String json = fetchJson(url);
            System.out.println("[DEBUG] loadStudents response: " + json);
            
            // 检查是否返回了错误信息
            if (json == null || json.trim().isEmpty()) {
                System.err.println("[ERROR] Empty response from server");
                return;
            }
            
            // 尝试解析为JSON数组
            List<StudentInfo> students;
            try {
                students = mapper.readValue(json, new TypeReference<List<StudentInfo>>() {});
            } catch (Exception parseEx) {
                // 如果解析失败，可能是返回了JSON对象（如错误信息）
                JsonNode node = mapper.readTree(json);
                if (node.isObject() && node.has("message")) {
                    System.err.println("[ERROR] Server returned error: " + node.get("message").asText());
                    return;
                }
                throw parseEx;
            }
            
            System.out.println("[DEBUG] Parsed students count: " + students.size());
            final List<StudentInfo> finalStudents = students;
            javafx.application.Platform.runLater(() -> {
                table.getItems().clear();
                table.setItems(FXCollections.observableArrayList(finalStudents));
                table.refresh();
                System.out.println("[DEBUG] Table items set, count: " + table.getItems().size());
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ERROR] loadStudents failed: " + e.getMessage());
        }
    }

    private void loadAnnouncements(@SuppressWarnings("rawtypes") TableView table) {
        try {
            String url = SERVER_URL + "/api/announcements/all";
            System.out.println("[DEBUG] loadAnnouncements URL: " + url);
            String json = fetchJson(url);
            System.out.println("[DEBUG] loadAnnouncements response: " + json);
            
            if (json == null || json.trim().isEmpty()) {
                System.err.println("[ERROR] Empty response from server");
                return;
            }
            
            List<Announcement> anns;
            try {
                anns = mapper.readValue(json, new TypeReference<List<Announcement>>() {});
            } catch (Exception parseEx) {
                JsonNode node = mapper.readTree(json);
                if (node.isObject() && node.has("message")) {
                    System.err.println("[ERROR] Server returned error: " + node.get("message").asText());
                    return;
                }
                throw parseEx;
            }
            
            System.out.println("[DEBUG] Parsed announcements count: " + anns.size());
            final List<Announcement> finalAnns = anns;
            javafx.application.Platform.runLater(() -> {
                table.getItems().clear();
                table.setItems(FXCollections.observableArrayList(finalAnns));
                table.refresh();
                System.out.println("[DEBUG] Announcement table items set, count: " + table.getItems().size());
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ERROR] loadAnnouncements failed: " + e.getMessage());
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
            // 获取仪表盘基础数据
            String json = fetchJson(SERVER_URL + "/api/dashboard");
            JsonNode data = mapper.readTree(json);
            
            // 获取报修列表用于统计类型分布
            String ordersJson = fetchJson(SERVER_URL + "/api/orders");
            List<RepairOrder> orders = mapper.readValue(ordersJson, new TypeReference<List<RepairOrder>>() {});
            
            // 统计报修类型
            Map<String, Integer> typeCount = new HashMap<>();
            for (RepairOrder order : orders) {
                String type = order.getRepairType();
                if (type == null || type.isEmpty()) type = "其他";
                typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
            }
            
            // 统计月度趋势（按创建时间月份分组）
            Map<String, Integer> monthlyCount = new TreeMap<>();
            for (RepairOrder order : orders) {
                if (order.getCreatedAt() != null) {
                    String month = order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    monthlyCount.put(month, monthlyCount.getOrDefault(month, 0) + 1);
                }
            }
            
            javafx.application.Platform.runLater(() -> {
                // 更新统计数字
                if (statsOrderCountLabel != null && data.has("totalOrders")) {
                    statsOrderCountLabel.setText(data.get("totalOrders").asText());
                }
                if (statsStudentCountLabel != null && data.has("totalStudents")) {
                    statsStudentCountLabel.setText(data.get("totalStudents").asText());
                }
                if (statsRoomCountLabel != null && data.has("totalRooms")) {
                    statsRoomCountLabel.setText(data.get("totalRooms").asText());
                }
                
                // 更新饼图
                if (statsTypePie != null) {
                    statsTypePie.getData().clear();
                    for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
                        statsTypePie.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
                    }
                }
                
                // 更新折线图
                if (statsLineChart != null) {
                    statsLineChart.getData().clear();
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("报修数量");
                    for (Map.Entry<String, Integer> entry : monthlyCount.entrySet()) {
                        series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                    }
                    statsLineChart.getData().add(series);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String fetchJson(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        return res.body();
    }

    /** 通用 POST（body 为 JSON 字符串，可为 null 表示无 body） */
    private void postAction(String url, String jsonBody) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
            if (jsonBody != null && !jsonBody.isEmpty()) {
                builder.header("Content-Type", "application/json")
                       .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.POST(HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> res = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 400) {
                javafx.application.Platform.runLater(() -> showAlert("操作失败: " + res.body()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            javafx.application.Platform.runLater(() -> showAlert("网络错误，请检查服务器连接"));
        }
    }

    /** 通用 PUT 请求（用于状态更新） */
    private void putAction(String url, String jsonBody) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
            if (jsonBody != null && !jsonBody.isEmpty()) {
                builder.header("Content-Type", "application/json")
                       .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.PUT(HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> res = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("[DEBUG] putAction URL: " + url + ", Status: " + res.statusCode());
            if (res.statusCode() >= 400) {
                javafx.application.Platform.runLater(() -> showAlert("操作失败: " + res.body()));
            } else {
                javafx.application.Platform.runLater(() -> showAlert("操作成功"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            javafx.application.Platform.runLater(() -> showAlert("网络错误，请检查服务器连接"));
        }
    }

    /** 通用 DELETE */
    private void deleteAction(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();
            client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            ex.printStackTrace();
            javafx.application.Platform.runLater(() -> showAlert("删除失败，请检查网络连接"));
        }
    }

    /** 通用提示弹窗 */
    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** 催单处理 */
    private void handleUrgeOrder(RepairOrder order, TableView<RepairOrder> table) {
        if (order == null || order.getId() == null) return;
        
        new Thread(() -> {
            try {
                String url = SERVER_URL + "/api/orders/" + order.getId() + "/urge";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                
                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        showAlert("催单成功！已通知管理员加快处理");
                        // 刷新列表
                        loadMyOrders(table);
                    } else {
                        showAlert("催单失败: " + response.body());
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> showAlert("催单失败，请检查网络连接"));
                e.printStackTrace();
            }
        }).start();
    }

    /** 评价对话框 */
    private void showReviewDialog(RepairOrder order, TableView<RepairOrder> table) {
        if (order == null || order.getId() == null) return;
        
        Stage dialog = new Stage();
        dialog.setTitle("评价工单");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(javafx.geometry.Pos.CENTER);

        Label titleLabel = new Label("为本次维修服务评分");
        titleLabel.setFont(Font.font(14));

        // 评分选择
        HBox ratingBox = new HBox(10);
        ratingBox.setAlignment(javafx.geometry.Pos.CENTER);
        ToggleGroup ratingGroup = new ToggleGroup();
        RadioButton[] ratingBtns = new RadioButton[5];
        for (int i = 1; i <= 5; i++) {
            ratingBtns[i-1] = new RadioButton(String.valueOf(i));
            ratingBtns[i-1].setToggleGroup(ratingGroup);
            ratingBtns[i-1].setUserData(i);
            ratingBox.getChildren().add(ratingBtns[i-1]);
        }
        ratingBtns[4].setSelected(true); // 默认5星

        // 评价内容
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("请输入您的评价（可选）");
        commentArea.setPrefRowCount(3);
        commentArea.setPrefWidth(300);

        // 按钮
        HBox btnBox = new HBox(15);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        Button submitBtn = new Button("提交评价");
        Button cancelBtn = new Button("取消");
        
        submitBtn.setOnAction(e -> {
            int rating = (int) ratingGroup.getSelectedToggle().getUserData();
            String comment = commentArea.getText().trim();
            submitReview(order.getId(), rating, comment, table, dialog);
        });
        
        cancelBtn.setOnAction(e -> dialog.close());
        btnBox.getChildren().addAll(submitBtn, cancelBtn);

        content.getChildren().addAll(titleLabel, ratingBox, commentArea, btnBox);

        Scene scene = new Scene(content);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /** 提交评价 */
    private void submitReview(Long orderId, int rating, String comment, TableView<RepairOrder> table, Stage dialog) {
        new Thread(() -> {
            try {
                String url = SERVER_URL + "/api/orders/" + orderId + "/review?rating=" + rating;
                if (!comment.isEmpty()) {
                    url += "&comment=" + java.net.URLEncoder.encode(comment, StandardCharsets.UTF_8);
                }
                
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                
                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        showAlert("评价提交成功！感谢您的反馈");
                        dialog.close();
                        // 刷新列表
                        loadMyOrders(table);
                    } else {
                        showAlert("评价失败: " + response.body());
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> showAlert("评价失败，请检查网络连接"));
                e.printStackTrace();
            }
        }).start();
    }

    /** 宿舍新增/编辑对话框 */
    @SuppressWarnings("rawtypes")
    private void showDormRoomDialog(DormRoom room, TableView table) {
        Stage dialog = new Stage();
        dialog.setTitle(room == null ? "新增宿舍" : "编辑宿舍");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(25));

        TextField buildingField = new TextField(room != null ? room.getBuildingName() : "");
        buildingField.setPromptText("如：一号楼");
        TextField roomNoField = new TextField(room != null ? room.getRoomNumber() : "");
        roomNoField.setPromptText("如：101");
        TextField floorField = new TextField(room != null ? String.valueOf(room.getFloor()) : "");
        floorField.setPromptText("楼层数字");
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("四人间", "六人间", "双人间", "单人间");
        typeBox.setValue(room != null && room.getRoomType() != null ? room.getRoomType() : "四人间");
        TextField capacityField = new TextField(room != null ? String.valueOf(room.getCapacity()) : "4");
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("正常", "维修中", "已关闭");
        // 将英文状态转换为中文显示
        String currentStatus = room != null ? room.getStatus() : "Normal";
        String displayStatus;
        switch (currentStatus) {
            case "Normal": displayStatus = "正常"; break;
            case "Maintenance": displayStatus = "维修中"; break;
            case "Closed": displayStatus = "已关闭"; break;
            default: displayStatus = currentStatus;
        }
        statusBox.setValue(displayStatus);

        grid.add(new Label("楼栋:"), 0, 0); grid.add(buildingField, 1, 0);
        grid.add(new Label("房间号:"), 0, 1); grid.add(roomNoField, 1, 1);
        grid.add(new Label("楼层:"), 0, 2); grid.add(floorField, 1, 2);
        grid.add(new Label("类型:"), 0, 3); grid.add(typeBox, 1, 3);
        grid.add(new Label("额定人数:"), 0, 4); grid.add(capacityField, 1, 4);
        grid.add(new Label("状态:"), 0, 5); grid.add(statusBox, 1, 5);

        Button saveBtn = new Button(room == null ? "新增" : "保存");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(ev -> {
            try {
                int floor = Integer.parseInt(floorField.getText().trim());
                int capacity = Integer.parseInt(capacityField.getText().trim());
                // 将中文状态转换为英文
                String selectedStatus = statusBox.getValue();
                String apiStatus;
                switch (selectedStatus) {
                    case "正常": apiStatus = "Normal"; break;
                    case "维修中": apiStatus = "Maintenance"; break;
                    case "已关闭": apiStatus = "Closed"; break;
                    default: apiStatus = selectedStatus;
                }
                String body = String.format(
                    "{\"buildingName\":\"%s\",\"roomNumber\":\"%s\",\"floor\":%d,\"roomType\":\"%s\",\"capacity\":%d,\"status\":\"%s\"}",
                    buildingField.getText().replace("\"","'"),
                    roomNoField.getText().replace("\"","'"),
                    floor, typeBox.getValue(), capacity, apiStatus
                );
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .header("Content-Type", "application/json");
                if (room == null) {
                    reqBuilder.uri(URI.create(SERVER_URL + "/api/rooms"))
                              .POST(HttpRequest.BodyPublishers.ofString(body));
                } else {
                    reqBuilder.uri(URI.create(SERVER_URL + "/api/rooms/" + room.getId()))
                              .PUT(HttpRequest.BodyPublishers.ofString(body));
                }
                HttpResponse<String> res = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (res.statusCode() < 400) {
                    dialog.close();
                    new Thread(() -> loadDormRooms(table, "全部楼栋", "")).start();
                } else {
                    showAlert("保存失败: " + res.body());
                }
            } catch (NumberFormatException nfe) {
                showAlert("楼层和额定人数必须是数字");
            } catch (Exception ex) {
                showAlert("网络错误: " + ex.getMessage());
            }
        });

        grid.add(saveBtn, 0, 6, 2, 1);
        dialog.setScene(new Scene(grid, 380, 340));
        dialog.showAndWait();
    }

    /** 公告新增/编辑对话框 */
    @SuppressWarnings("rawtypes")
    private void showAnnouncementDialog(Announcement ann, TableView table) {
        showAnnouncementDialog(ann, table, false);
    }

    /** 公告新增/编辑对话框（带仪表盘来源标记） */
    @SuppressWarnings("rawtypes")
    private void showAnnouncementDialog(Announcement ann, TableView table, boolean fromDashboard) {
        Stage dialog = new Stage();
        dialog.setTitle(ann == null ? "发布公告" : "编辑公告");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(25));

        TextField titleField = new TextField(ann != null ? ann.getTitle() : "");
        titleField.setPromptText("公告标题");
        titleField.setPrefWidth(280);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("通知", "公告", "紧急");
        typeBox.setValue(ann != null && ann.getType() != null ? ann.getType() : "通知");

        TextArea contentArea = new TextArea(ann != null ? ann.getContent() : "");
        contentArea.setPromptText("公告内容...");
        contentArea.setPrefRowCount(5);
        contentArea.setPrefWidth(280);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Draft", "Published", "Archived");
        statusBox.setValue(ann != null && ann.getStatus() != null ? ann.getStatus() : "Published");

        grid.add(new Label("标题:"), 0, 0); grid.add(titleField, 1, 0);
        grid.add(new Label("类型:"), 0, 1); grid.add(typeBox, 1, 1);
        grid.add(new Label("内容:"), 0, 2); grid.add(contentArea, 1, 2);
        grid.add(new Label("状态:"), 0, 3); grid.add(statusBox, 1, 3);

        Button saveBtn = new Button(ann == null ? "发布" : "保存");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(ev -> {
            if (titleField.getText().isEmpty() || contentArea.getText().isEmpty()) {
                showAlert("标题和内容不能为空");
                return;
            }
            try {
                String publisher = currentStudentInfo != null ? currentStudentInfo.getRealName() : currentUsername;
                String body = String.format(
                    "{\"title\":\"%s\",\"type\":\"%s\",\"content\":\"%s\",\"status\":\"%s\",\"publisher\":\"%s\"}",
                    titleField.getText().replace("\"","'"),
                    typeBox.getValue(),
                    contentArea.getText().replace("\"","'").replace("\n"," "),
                    statusBox.getValue(),
                    publisher
                );
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .header("Content-Type", "application/json");
                if (ann == null) {
                    reqBuilder.uri(URI.create(SERVER_URL + "/api/announcements"))
                              .POST(HttpRequest.BodyPublishers.ofString(body));
                } else {
                    reqBuilder.uri(URI.create(SERVER_URL + "/api/announcements/" + ann.getId()))
                              .PUT(HttpRequest.BodyPublishers.ofString(body));
                }
                HttpResponse<String> res = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (res.statusCode() < 400) {
                    dialog.close();
                    if (table != null) {
                        new Thread(() -> loadAnnouncements(table)).start();
                    } else if (fromDashboard && announcementTableRef != null) {
                        // 从仪表盘发布，刷新公告管理表格
                        new Thread(() -> {
                            try {
                                Thread.sleep(500);
                                loadAnnouncements(announcementTableRef);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                    }
                } else {
                    showAlert("发布失败: " + res.body());
                }
            } catch (Exception ex) {
                showAlert("网络错误: " + ex.getMessage());
            }
        });

        grid.add(saveBtn, 0, 4, 2, 1);
        dialog.setScene(new Scene(grid, 440, 380));
        dialog.showAndWait();
    }

    /** 学生新增/编辑对话框 */
    @SuppressWarnings("rawtypes")
    private void showStudentDialog(StudentInfo student, TableView table) {
        Stage dialog = new Stage();
        dialog.setTitle(student == null ? "新增学生" : "编辑学生");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(25));

        TextField studentNoField = new TextField(student != null ? student.getStudentNo() : "");
        studentNoField.setPromptText("学号");
        TextField realNameField = new TextField(student != null ? student.getRealName() : "");
        realNameField.setPromptText("姓名");
        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("男", "女");
        genderBox.setValue(student != null && student.getGender() != null ? student.getGender() : "男");
        TextField phoneField = new TextField(student != null ? student.getPhone() : "");
        phoneField.setPromptText("手机号");
        TextField deptField = new TextField(student != null ? student.getDepartment() : "");
        deptField.setPromptText("院系");
        TextField majorField = new TextField(student != null ? student.getMajor() : "");
        majorField.setPromptText("专业");
        TextField classField = new TextField(student != null ? student.getClassName() : "");
        classField.setPromptText("班级");
        TextField roomField = new TextField(student != null ? student.getRoomNumber() : "");
        roomField.setPromptText("宿舍号");
        TextField bedField = new TextField(student != null ? String.valueOf(student.getBedNumber()) : "");
        bedField.setPromptText("床位号");
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("在住", "离校", "毕业");
        statusBox.setValue(student != null && student.getStatus() != null ? student.getStatus() : "在住");

        grid.add(new Label("学号:"), 0, 0); grid.add(studentNoField, 1, 0);
        grid.add(new Label("姓名:"), 0, 1); grid.add(realNameField, 1, 1);
        grid.add(new Label("性别:"), 0, 2); grid.add(genderBox, 1, 2);
        grid.add(new Label("手机:"), 0, 3); grid.add(phoneField, 1, 3);
        grid.add(new Label("院系:"), 0, 4); grid.add(deptField, 1, 4);
        grid.add(new Label("专业:"), 0, 5); grid.add(majorField, 1, 5);
        grid.add(new Label("班级:"), 0, 6); grid.add(classField, 1, 6);
        grid.add(new Label("宿舍:"), 0, 7); grid.add(roomField, 1, 7);
        grid.add(new Label("床位:"), 0, 8); grid.add(bedField, 1, 8);
        grid.add(new Label("状态:"), 0, 9); grid.add(statusBox, 1, 9);

        Button saveBtn = new Button(student == null ? "新增" : "保存");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(ev -> {
            try {
                int bedNum = bedField.getText().isEmpty() ? 0 : Integer.parseInt(bedField.getText().trim());
                String body = String.format(
                    "{\"studentNo\":\"%s\",\"realName\":\"%s\",\"gender\":\"%s\",\"phone\":\"%s\",\"department\":\"%s\",\"major\":\"%s\",\"className\":\"%s\",\"roomNumber\":\"%s\",\"bedNumber\":%d,\"status\":\"%s\"}",
                    studentNoField.getText().replace("\"","'"),
                    realNameField.getText().replace("\"","'"),
                    genderBox.getValue(),
                    phoneField.getText().replace("\"","'"),
                    deptField.getText().replace("\"","'"),
                    majorField.getText().replace("\"","'"),
                    classField.getText().replace("\"","'"),
                    roomField.getText().replace("\"","'"),
                    bedNum,
                    statusBox.getValue()
                );
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .header("Content-Type", "application/json");
                if (student == null) {
                    reqBuilder.uri(URI.create(SERVER_URL + "/api/students"))
                              .POST(HttpRequest.BodyPublishers.ofString(body));
                } else {
                    reqBuilder.uri(URI.create(SERVER_URL + "/api/students/" + student.getId()))
                              .PUT(HttpRequest.BodyPublishers.ofString(body));
                }
                HttpResponse<String> res = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (res.statusCode() < 400) {
                    dialog.close();
                    new Thread(() -> loadStudents(table, "")).start();
                } else {
                    showAlert("保存失败: " + res.body());
                }
            } catch (NumberFormatException nfe) {
                showAlert("床位号必须是数字");
            } catch (Exception ex) {
                showAlert("网络错误: " + ex.getMessage());
            }
        });

        grid.add(saveBtn, 0, 10, 2, 1);
        dialog.setScene(new Scene(grid, 400, 520));
        dialog.showAndWait();
    }

    /** 导入学生对话框 */
    private void showImportStudentDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("批量导入学生");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox box = new VBox(15);
        box.setPadding(new Insets(25));
        box.setAlignment(Pos.CENTER);

        Label hint = new Label("请粘贴学生信息，支持两种格式：");
        hint.setWrapText(true);
        
        Label format = new Label("格式1（推荐）：每行一条，逗号分隔\n2021001,张三,男,13800138000,计算机学院,软件工程,软工2101,A101,1\n\n格式2：每行一个字段（共9行一条记录）\n2021001\n张三\n男\n...");
        format.setStyle("-fx-font-family: monospace; -fx-background-color: #f5f5f5; -fx-padding: 10;");
        format.setWrapText(true);
        
        TextArea area = new TextArea();
        area.setPromptText("粘贴学生数据...");
        area.setPrefRowCount(12);
        area.setPrefWidth(500);

        Label resultLabel = new Label();
        
        Button importBtn = new Button("开始导入");
        importBtn.setOnAction(e -> {
            String text = area.getText().trim();
            if (text.isEmpty()) {
                resultLabel.setStyle("-fx-text-fill: red;");
                resultLabel.setText("请输入数据");
                return;
            }
            
            int success = 0, fail = 0;
            String[] lines = text.split("\n");
            
            // 检测格式：如果第一行包含逗号，使用格式1；否则使用格式2（每9行一条）
            boolean isCommaFormat = lines.length > 0 && lines[0].contains(",");
            
            if (isCommaFormat) {
                // 格式1：每行一条，逗号分隔
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length < 9) {
                        fail++;
                        continue;
                    }
                    try {
                        String body = String.format(
                            "{\"studentNo\":\"%s\",\"realName\":\"%s\",\"gender\":\"%s\",\"phone\":\"%s\",\"department\":\"%s\",\"major\":\"%s\",\"className\":\"%s\",\"roomNumber\":\"%s\",\"bedNumber\":%d,\"status\":\"在住\"}",
                            parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(),
                            parts[4].trim(), parts[5].trim(), parts[6].trim(), parts[7].trim(),
                            Integer.parseInt(parts[8].trim())
                        );
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(SERVER_URL + "/api/students"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(body))
                                .build();
                        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                        if (res.statusCode() < 400) success++;
                        else fail++;
                    } catch (Exception ex) {
                        fail++;
                    }
                }
            } else {
                // 格式2：每9行一条记录
                for (int i = 0; i < lines.length; i += 9) {
                    if (i + 8 >= lines.length) {
                        fail++;
                        break;
                    }
                    try {
                        String body = String.format(
                            "{\"studentNo\":\"%s\",\"realName\":\"%s\",\"gender\":\"%s\",\"phone\":\"%s\",\"department\":\"%s\",\"major\":\"%s\",\"className\":\"%s\",\"roomNumber\":\"%s\",\"bedNumber\":%d,\"status\":\"在住\"}",
                            lines[i].trim(), lines[i+1].trim(), lines[i+2].trim(), lines[i+3].trim(),
                            lines[i+4].trim(), lines[i+5].trim(), lines[i+6].trim(), lines[i+7].trim(),
                            Integer.parseInt(lines[i+8].trim())
                        );
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(SERVER_URL + "/api/students"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(body))
                                .build();
                        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                        if (res.statusCode() < 400) success++;
                        else fail++;
                    } catch (Exception ex) {
                        fail++;
                    }
                }
            }
            resultLabel.setStyle("-fx-text-fill: green;");
            resultLabel.setText(String.format("导入完成：成功 %d 条，失败 %d 条", success, fail));
            
            // 导入成功后刷新学生管理表格
            if (success > 0 && studentTableRef != null) {
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // 稍等片刻确保数据库已更新
                        loadStudents(studentTableRef, "");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });

        box.getChildren().addAll(hint, format, area, importBtn, resultLabel);
        dialog.setScene(new Scene(box, 550, 450));
        dialog.showAndWait();
    }

    /** 生成报表对话框 */
    private void showGenerateReportDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("生成统计报表");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox box = new VBox(15);
        box.setPadding(new Insets(25));
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("选择报表类型：");
        title.setFont(Font.font(16));

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("学生住宿统计", "宿舍使用情况", "报修处理统计", "公告发布统计");
        typeBox.setValue("学生住宿统计");

        Button generateBtn = new Button("生成并导出");
        TextArea preview = new TextArea();
        preview.setEditable(false);
        preview.setPrefRowCount(15);
        preview.setPrefWidth(500);

        generateBtn.setOnAction(e -> {
            String type = typeBox.getValue();
            new Thread(() -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("========== ").append(type).append(" ==========\n\n");
                    
                    HttpClient client = HttpClient.newHttpClient();
                    
                    switch (type) {
                        case "学生住宿统计":
                            HttpRequest req1 = HttpRequest.newBuilder()
                                    .uri(URI.create(SERVER_URL + "/api/students"))
                                    .GET().build();
                            String studentsJson = client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
                            List<StudentInfo> students = mapper.readValue(studentsJson, new TypeReference<List<StudentInfo>>() {});
                            sb.append("总人数：").append(students.size()).append("\n\n");
                            sb.append("学号\t\t姓名\t\t宿舍\t床位\t状态\n");
                            sb.append("----------------------------------------\n");
                            for (StudentInfo s : students) {
                                sb.append(String.format("%s\t%s\t\t%s\t%d\t%s\n",
                                    s.getStudentNo(), s.getRealName(), 
                                    s.getRoomNumber(), s.getBedNumber(), s.getStatus()));
                            }
                            break;
                            
                        case "宿舍使用情况":
                            HttpRequest req2 = HttpRequest.newBuilder()
                                    .uri(URI.create(SERVER_URL + "/api/rooms"))
                                    .GET().build();
                            String roomsJson = client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
                            List<DormRoom> rooms = mapper.readValue(roomsJson, new TypeReference<List<DormRoom>>() {});
                            sb.append("总宿舍数：").append(rooms.size()).append("\n\n");
                            sb.append("楼栋\t房间号\t类型\t容量\t当前人数\t状态\n");
                            sb.append("----------------------------------------\n");
                            for (DormRoom r : rooms) {
                                sb.append(String.format("%s\t%s\t\t%s\t%d\t%d\t\t%s\n",
                                    r.getBuildingName(), r.getRoomNumber(),
                                    r.getRoomType(), r.getCapacity(), r.getCurrentCount(), r.getStatus()));
                            }
                            break;
                            
                        case "报修处理统计":
                            HttpRequest req3 = HttpRequest.newBuilder()
                                    .uri(URI.create(SERVER_URL + "/api/orders"))
                                    .GET().build();
                            String ordersJson = client.send(req3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
                            List<RepairOrder> orders = mapper.readValue(ordersJson, new TypeReference<List<RepairOrder>>() {});
                            long pending = orders.stream().filter(o -> "待处理".equals(o.getStatus())).count();
                            long processing = orders.stream().filter(o -> "处理中".equals(o.getStatus())).count();
                            long resolved = orders.stream().filter(o -> "已解决".equals(o.getStatus())).count();
                            sb.append("总报修数：").append(orders.size()).append("\n");
                            sb.append("待处理：").append(pending).append("\n");
                            sb.append("处理中：").append(processing).append("\n");
                            sb.append("已解决：").append(resolved).append("\n\n");
                            sb.append("单号\t宿舍\t类型\t状态\t提交时间\n");
                            sb.append("----------------------------------------\n");
                            for (RepairOrder o : orders) {
                                sb.append(String.format("#%d\t%s\t%s\t%s\t%s\n",
                                    o.getId(), o.getRoomNumber(), o.getRepairType(),
                                    o.getStatus(), o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate() : ""));
                            }
                            break;
                            
                        case "公告发布统计":
                            HttpRequest req4 = HttpRequest.newBuilder()
                                    .uri(URI.create(SERVER_URL + "/api/announcements/all"))
                                    .GET().build();
                            String annJson = client.send(req4, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
                            List<Announcement> anns = mapper.readValue(annJson, new TypeReference<List<Announcement>>() {});
                            sb.append("总公告数：").append(anns.size()).append("\n\n");
                            sb.append("标题\t\t\t类型\t发布人\t状态\t发布时间\n");
                            sb.append("----------------------------------------\n");
                            for (Announcement a : anns) {
                                String titleStr = a.getTitle().length() > 12 ? a.getTitle().substring(0, 12) + "..." : a.getTitle();
                                sb.append(String.format("%s\t%s\t%s\t%s\t%s\n",
                                    titleStr, a.getType(), a.getPublisher(),
                                    a.getStatus(), a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate() : ""));
                            }
                            break;
                    }
                    
                    javafx.application.Platform.runLater(() -> {
                        preview.setText(sb.toString());
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        preview.setText("生成报表失败：" + ex.getMessage());
                    });
                }
            }).start();
        });

        box.getChildren().addAll(title, typeBox, generateBtn, new Label("预览："), preview);
        dialog.setScene(new Scene(box, 600, 500));
        dialog.showAndWait();
    }

    /** 报修单编辑对话框 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void showRepairOrderDialog(RepairOrder order, TableView table, String currentStatus, String currentKeyword) {
        Stage dialog = new Stage();
        dialog.setTitle("编辑报修单");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // 报修人（只读）
        TextField nameField = new TextField(order.getStudentName());
        nameField.setEditable(false);
        grid.add(new Label("报修人:"), 0, 0);
        grid.add(nameField, 1, 0);

        // 宿舍楼（可编辑）
        TextField buildingField = new TextField(order.getBuilding());
        grid.add(new Label("宿舍楼:"), 0, 1);
        grid.add(buildingField, 1, 1);

        // 宿舍号（可编辑）
        TextField roomField = new TextField(order.getRoomNumber());
        grid.add(new Label("宿舍号:"), 0, 2);
        grid.add(roomField, 1, 2);

        // 报修类型（可编辑）
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("水电", "家具", "网络", "门窗", "其他");
        typeBox.setValue(order.getRepairType());
        grid.add(new Label("报修类型:"), 0, 3);
        grid.add(typeBox, 1, 3);

        // 优先级（可编辑）
        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("Low", "Normal", "High", "Urgent");
        priorityBox.setValue(order.getPriority());
        grid.add(new Label("优先级:"), 0, 3);
        grid.add(priorityBox, 1, 3);

        // 状态（可编辑）
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Pending", "Processing", "Resolved", "Rejected");
        statusBox.setValue(order.getStatus());
        grid.add(new Label("状态:"), 0, 4);
        grid.add(statusBox, 1, 4);

        // 问题描述（可编辑）
        TextArea descArea = new TextArea(order.getDescription());
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);
        grid.add(new Label("问题描述:"), 0, 5);
        grid.add(descArea, 1, 5);

        // 处理意见（可编辑）
        TextArea remarkArea = new TextArea(order.getAdminRemark());
        remarkArea.setPromptText("管理员处理意见");
        remarkArea.setPrefRowCount(3);
        remarkArea.setWrapText(true);
        grid.add(new Label("处理意见:"), 0, 6);
        grid.add(remarkArea, 1, 6);

        // 保存按钮
        Button saveBtn = new Button("保存");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                // 构建更新后的报修单
                order.setBuilding(buildingField.getText().trim());
                order.setRoomNumber(roomField.getText().trim());
                order.setRepairType(typeBox.getValue());
                order.setPriority(priorityBox.getValue());
                order.setStatus(statusBox.getValue());
                order.setDescription(descArea.getText().trim());
                order.setAdminRemark(remarkArea.getText().trim());

                // 如果状态改为已解决，设置解决时间
                if ("Resolved".equals(statusBox.getValue()) && order.getResolvedAt() == null) {
                    order.setResolvedAt(java.time.LocalDateTime.now());
                }

                // 发送PUT请求更新
                String json = mapper.writeValueAsString(order);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_URL + "/api/orders/" + order.getId()))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (res.statusCode() < 400) {
                    showAlert("保存成功");
                    dialog.close();
                    // 刷新表格
                    loadOrders(table, currentStatus, currentKeyword);
                } else {
                    showAlert("保存失败: " + res.body());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("保存失败: " + ex.getMessage());
            }
        });

        Button cancelBtn = new Button("取消");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        grid.add(btnBox, 0, 7, 2, 1);

        dialog.setScene(new Scene(grid, 450, 500));
        dialog.showAndWait();
    }

    // ========== 公共方法，供 Controller 调用 ==========
    
    /** 获取主窗口 */
    public Stage getWindow() {
        return window;
    }
    
    /** 获取当前用户角色 */
    public static String getCurrentUserRole() {
        return currentUserRole;
    }
    
    /** 获取当前用户ID */
    public static Long getCurrentUserId() {
        return currentUserId;
    }
    
    /** 获取当前用户名 */
    public static String getCurrentUsername() {
        return currentUsername;
    }
    
    /** 设置当前用户信息 */
    public static void setCurrentUser(String role, Long userId, String username) {
        currentUserRole = role;
        currentUserId = userId;
        currentUsername = username;
    }
    
    /** 显示管理员首页（供 Controller 调用） */
    public void showAdminHome(Stage stage) {
        currentUserRole = "ADMIN";  // 确保设置为管理员角色
        this.window = stage;
        showMainWorkspace();
    }
    
    /** 显示学生首页（供 Controller 调用） */
    public void showStudentHome(Stage stage) {
        currentUserRole = "STUDENT";  // 确保设置为学生角色
        this.window = stage;
        showMainWorkspace();
    }

    public static void main(String[] args) {
        launch(args);
    }
}