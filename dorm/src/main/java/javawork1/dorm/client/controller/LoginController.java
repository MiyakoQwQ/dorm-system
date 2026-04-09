package javawork1.dorm.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javawork1.dorm.client.DormClientApp;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

/**
 * 登录界面控制器
 * 对应 login-view.fxml
 */
public class LoginController implements Initializable {

    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;

    private static final String SERVER_URL = DormClientApp.SERVER_URL;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化角色选择
        roleCombo.getItems().addAll("管理员", "学生");
        roleCombo.setValue("学生");
        
        // 清除错误信息
        errorLabel.setText("");
        
        // 添加回车键登录
        passwordField.setOnAction(_e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String role = roleCombo.getValue();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("请输入账号和密码");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("登录中...");
        errorLabel.setText("");

        new Thread(() -> {
            try {
                String url = SERVER_URL + "/api/login?username=" + username 
                        + "&password=" + password 
                        + "&role=" + ("管理员".equals(role) ? "ADMIN" : "STUDENT");
                
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                
                HttpResponse<String> response = client.send(request, 
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        String body = response.body();
                        if (body.contains("ADMIN")) {
                            navigateToAdminHome();
                        } else if (body.contains("STUDENT")) {
                            navigateToStudentHome();
                        } else {
                            errorLabel.setText("登录失败：未知角色");
                            loginBtn.setDisable(false);
                            loginBtn.setText("登录");
                        }
                    } else {
                        errorLabel.setText("登录失败：" + response.body());
                        loginBtn.setDisable(false);
                        loginBtn.setText("登录");
                    }
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    errorLabel.setText("网络错误：" + e.getMessage());
                    loginBtn.setDisable(false);
                    loginBtn.setText("登录");
                });
            }
        }).start();
    }

    @FXML
    private void handleRegister() {
        // 创建注册窗口
        Stage registerStage = new Stage();
        registerStage.setTitle("学生注册");

        VBox root = new VBox(15);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new javafx.geometry.Insets(30));
        root.setStyle("-fx-background-color: #f5f5f5;");

        Label titleLabel = new Label("学生账号注册");
        titleLabel.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("请输入账号");
        usernameField.setMaxWidth(270);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setMaxWidth(270);

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("请确认密码");
        confirmField.setMaxWidth(270);

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        Button registerBtn = new Button("注册");
        registerBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px;");
        registerBtn.setPrefWidth(270);

        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle("-fx-font-size: 14px;");
        cancelBtn.setPrefWidth(270);

        root.getChildren().addAll(titleLabel, usernameField, passwordField, confirmField, registerBtn, cancelBtn, messageLabel);

        // 注册按钮事件
        registerBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();

            if (username.isEmpty()) {
                messageLabel.setText("账号不能为空");
                return;
            }
            if (password.isEmpty()) {
                messageLabel.setText("密码不能为空");
                return;
            }
            if (!password.equals(confirm)) {
                messageLabel.setText("两次密码不一致");
                return;
            }

            registerBtn.setDisable(true);
            registerBtn.setText("注册中...");

            new Thread(() -> {
                try {
                    String jsonBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(SERVER_URL + "/api/users/register"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("注册成功");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText("注册成功！请使用新账号登录。");
                            successAlert.showAndWait();
                            registerStage.close();
                        } else if (response.statusCode() == 409) {
                            messageLabel.setText("该用户名已被占用");
                            registerBtn.setDisable(false);
                            registerBtn.setText("注册");
                        } else {
                            messageLabel.setText("注册失败：" + response.body());
                            registerBtn.setDisable(false);
                            registerBtn.setText("注册");
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        messageLabel.setText("网络错误：" + ex.getMessage());
                        registerBtn.setDisable(false);
                        registerBtn.setText("注册");
                    });
                }
            }).start();
        });

        cancelBtn.setOnAction(e -> registerStage.close());

        Scene scene = new Scene(root, 400, 450);
        registerStage.setScene(scene);
        registerStage.centerOnScreen();
        registerStage.show();
    }

    private void navigateToAdminHome() {
        try {
            String username = usernameField.getText().trim();
            DormClientApp.setCurrentUser("ADMIN", null, username);
            DormClientApp app = new DormClientApp();
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            app.showAdminHome(stage);
        } catch (Exception e) {
            errorLabel.setText("跳转失败：" + e.getMessage());
        }
    }

    private void navigateToStudentHome() {
        try {
            // 存储当前登录用户名
            String username = usernameField.getText().trim();
            DormClientApp.setCurrentUser("STUDENT", null, username);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student-home-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("宿舍管理系统 - 学生端");
        } catch (IOException e) {
            errorLabel.setText("跳转失败：" + e.getMessage());
        }
    }
}
