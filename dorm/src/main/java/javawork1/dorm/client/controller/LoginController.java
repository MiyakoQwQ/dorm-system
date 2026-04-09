package javawork1.dorm.client.controller;

import javafx.application.Platform;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

    private void navigateToAdminHome() {
        try {
            // 暂时使用原来的方式，后续可以创建 AdminHomeController
            DormClientApp app = new DormClientApp();
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            app.showAdminHome(stage);
        } catch (Exception e) {
            errorLabel.setText("跳转失败：" + e.getMessage());
        }
    }

    private void navigateToStudentHome() {
        try {
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
