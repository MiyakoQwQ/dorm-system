package javawork1.dorm.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javawork1.dorm.entity.RepairOrder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DormClientApp extends Application {
    // 【全局中枢枢纽】：未来只要改这一个地方，全系统的网络指向就会瞬间切换！
    private static final String SERVER_URL = "http://localhost:8080";
    // 核心改造：提取全局窗口控制权，用于在“登录界面”和“工作台”之间进行物理切换
    private Stage window;
    private TableView<RepairOrder> table = new TableView<>();
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void start(Stage primaryStage) {
        this.window = primaryStage;
        showLoginScreen(); // 强制拦截：程序一启动，必须先进入安检门！
    }

    // ================= 模块一：安检门 (登录界面) =================
    // ================= 模块一：安检门 (登录界面 升级版) =================
    private void showLoginScreen() {
        VBox loginRoot = new VBox(15);
        loginRoot.setPadding(new Insets(50));
        loginRoot.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("宿舍报修系统 - 身份认证");
        titleLabel.setFont(new Font("System", 20));

        TextField userField = new TextField(); userField.setPromptText("账号"); userField.setMaxWidth(250);
        PasswordField passField = new PasswordField(); passField.setPromptText("密码"); passField.setMaxWidth(250);

        Button loginBtn = new Button("🔐 登 录");
        loginBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");

        // 【新增】：跳转到注册界面的按钮
        Button goRegisterBtn = new Button("没有账号？点此注册");
        goRegisterBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2196F3; -fx-underline: true;");
        goRegisterBtn.setOnAction(e -> showRegisterScreen()); // 状态机流转：切换到注册画面

        Label errorLabel = new Label(); errorLabel.setStyle("-fx-text-fill: red;");

        loginBtn.setOnAction(e -> {
            try {
                Map<String, String> creds = new HashMap<>();
                creds.put("username", userField.getText());
                creds.put("password", passField.getText());
                String json = mapper.writeValueAsString(creds);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/api/users/login"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    JsonNode node = mapper.readTree(res.body());
                    showMainWorkspace(node.get("role").asText());
                } else {
                    errorLabel.setText("拦截：" + res.body());
                }
            } catch (Exception ex) { errorLabel.setText("网络瘫痪"); }
        });

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.getChildren().addAll(loginBtn, goRegisterBtn);

        loginRoot.getChildren().addAll(titleLabel, userField, passField, btnBox, errorLabel);
        window.setTitle("系统登录");
        window.setScene(new Scene(loginRoot, 400, 300));
        window.show();
    }

    // ================= 模块三：人员招募 (全新注册界面) =================
    private void showRegisterScreen() {
        VBox regRoot = new VBox(15);
        regRoot.setPadding(new Insets(50));
        regRoot.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("新生报到 - 账号注册");
        titleLabel.setFont(new Font("System", 20));

        TextField userField = new TextField(); userField.setPromptText("设定账号 (如学号)"); userField.setMaxWidth(250);
        PasswordField passField = new PasswordField(); passField.setPromptText("设定密码"); passField.setMaxWidth(250);
        PasswordField confirmPassField = new PasswordField(); confirmPassField.setPromptText("确认密码"); confirmPassField.setMaxWidth(250);

        Button regBtn = new Button("📝 提 交 注 册");
        regBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        Button backBtn = new Button("返回登录");
        backBtn.setOnAction(e -> showLoginScreen()); // 状态机流转：退回登录画面

        Label msgLabel = new Label();

        regBtn.setOnAction(e -> {
            // 前端基础防呆：拦截空数据和密码不一致
            if (userField.getText().trim().isEmpty() || passField.getText().isEmpty()) {
                msgLabel.setStyle("-fx-text-fill: red;"); msgLabel.setText("账号密码不能为空！"); return;
            }
            if (!passField.getText().equals(confirmPassField.getText())) {
                msgLabel.setStyle("-fx-text-fill: red;"); msgLabel.setText("两次密码输入不一致！"); return;
            }

            try {
                Map<String, String> newUser = new HashMap<>();
                newUser.put("username", userField.getText());
                newUser.put("password", passField.getText());
                // 注意：前端哪怕这里写了 role: ADMIN，也会被后端我们刚写的安全防线无情切断！

                String json = mapper.writeValueAsString(newUser);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/api/users/register"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    msgLabel.setStyle("-fx-text-fill: green;");
                    msgLabel.setText("注册成功！请返回登录。");
                    userField.clear(); passField.clear(); confirmPassField.clear();
                } else {
                    // 这里会展示我们后端返回的 "该账号已被占用"
                    msgLabel.setStyle("-fx-text-fill: red;"); msgLabel.setText(res.body());
                }
            } catch (Exception ex) { msgLabel.setText("网络异常"); }
        });

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.getChildren().addAll(regBtn, backBtn);

        regRoot.getChildren().addAll(titleLabel, userField, passField, confirmPassField, btnBox, msgLabel);
        window.setTitle("系统注册");
        window.setScene(new Scene(regRoot, 400, 350));
    }

    // ================= 模块二：主工作台 (基于权限动态渲染) =================
    // 注意这个参数 currentUserRole，它决定了这个界面长什么样
    private void showMainWorkspace(String currentUserRole) {
        // ... 表格初始化 (保持不变) ...
        table = new TableView<>();
        table.setPrefHeight(250);
        TableColumn<RepairOrder, Long> idCol = new TableColumn<>("单号");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<RepairOrder, String> nameCol = new TableColumn<>("报修人");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        TableColumn<RepairOrder, String> roomCol = new TableColumn<>("宿舍号");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        TableColumn<RepairOrder, String> descCol = new TableColumn<>("损坏描述");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);
        TableColumn<RepairOrder, String> statusCol = new TableColumn<>("当前状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        table.getColumns().addAll(idCol, nameCol, roomCol, descCol, statusCol);

        // ... 顶部搜索区 (保持不变) ...
        TextField searchInput = new TextField();
        searchInput.setPromptText("输入宿舍号搜索...");
        Button searchButton = new Button("🔍 搜索");
        searchButton.setOnAction(e -> loadDataFromServer(searchInput.getText()));
        Button fetchButton = new Button("🔄 显示全部");
        fetchButton.setOnAction(e -> { searchInput.clear(); loadDataFromServer(null); });
        HBox topBox = new HBox(10);
        topBox.getChildren().addAll(searchInput, searchButton, fetchButton);

        // ... 数据录入区 (保持不变) ...
        TextField nameInput = new TextField(); nameInput.setPromptText("姓名");
        TextField roomInput = new TextField(); roomInput.setPromptText("宿舍号");
        TextField descInput = new TextField(); descInput.setPromptText("损坏情况");
        Button submitButton = new Button("🚀 提交新报修");
        submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        submitButton.setOnAction(e -> {
            if (nameInput.getText().trim().isEmpty() || roomInput.getText().trim().isEmpty() || descInput.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "警告：所有字段都必须填写！").showAndWait();
                return;
            }
            try {
                RepairOrder newOrder = new RepairOrder();
                newOrder.setStudentName(nameInput.getText());
                newOrder.setRoomNumber(roomInput.getText());
                newOrder.setDescription(descInput.getText());
                newOrder.setStatus("Pending");
                String jsonPayload = mapper.writeValueAsString(newOrder);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/api/orders"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    nameInput.clear(); roomInput.clear(); descInput.clear();
                    loadDataFromServer(null);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        HBox formBox = new HBox(10);
        formBox.getChildren().addAll(nameInput, roomInput, descInput, submitButton);

        // ... 管理员印章区 ...
        Button resolveButton = new Button("✅ 管理员：标记选中单据为已解决");
        resolveButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        resolveButton.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && "Pending".equals(newSelection.getStatus())) resolveButton.setDisable(false);
            else resolveButton.setDisable(true);
        });
        resolveButton.setOnAction(e -> {
            RepairOrder selectedOrder = table.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/api/orders" + selectedOrder.getId() + "/resolve"))
                            .PUT(HttpRequest.BodyPublishers.noBody()).build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) loadDataFromServer(null);
                } catch (Exception ex) { System.out.println("处理失败"); }
            }
        });
        HBox adminBox = new HBox(10);
        adminBox.getChildren().add(resolveButton);

        // 【致命权限隔离逻辑】：如果是学生身份登录，在物理内存层面彻底抹除管理员控制台！
        if ("STUDENT".equals(currentUserRole)) {
            adminBox.setVisible(false); // 隐身
            adminBox.setManaged(false); // 剥夺它的物理占位空间
        }

        // 物理总装
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(topBox, table, new Separator(), formBox, new Separator(), adminBox);

        Scene scene = new Scene(root, 750, 480);
        window.setTitle("宿舍报修系统 - 工作台 (" + currentUserRole + ")");
        window.setScene(scene); // 将窗口的画面切换为主工作台

        loadDataFromServer(null);
    }

    // ... 拉取数据的网络方法 (保持不变) ...
    private void loadDataFromServer(String keyword) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = SERVER_URL + "/api/orders";
            if (keyword != null && !keyword.trim().isEmpty()) {
                url += "?keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
            }
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            List<RepairOrder> orders = mapper.readValue(response.body(), new TypeReference<List<RepairOrder>>() {});
            table.setItems(FXCollections.observableArrayList(orders));
        } catch (Exception ex) { System.out.println("数据拉取失败"); }
    }
}