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
 * 报修界面控制器
 * 对应 repair-view.fxml
 */
public class RepairController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextArea descArea;
    @FXML private TextField phoneField;
    @FXML private Label errorLabel;
    @FXML private Button submitBtn;

    private static final String SERVER_URL = DormClientApp.SERVER_URL;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化报修类型
        typeCombo.getItems().addAll(
            "水电维修",
            "门窗维修", 
            "家具维修",
            "电器维修",
            "网络维修",
            "其他"
        );
        
        errorLabel.setText("");
    }

    @FXML
    private void handleSubmit() {
        String type = typeCombo.getValue();
        String description = descArea.getText().trim();
        String phone = phoneField.getText().trim();

        // 验证输入
        if (type == null || type.isEmpty()) {
            errorLabel.setText("请选择报修类型");
            return;
        }
        if (description.isEmpty()) {
            errorLabel.setText("请描述问题");
            return;
        }
        if (phone.isEmpty()) {
            errorLabel.setText("请输入联系电话");
            return;
        }

        submitBtn.setDisable(true);
        submitBtn.setText("提交中...");
        errorLabel.setText("");

        new Thread(() -> {
            try {
                // 构建JSON请求体
                String jsonBody = String.format(
                    "{\"type\":\"%s\",\"description\":\"%s\",\"contactPhone\":\"%s\"}",
                    type, description, phone
                );

                String url = SERVER_URL + "/api/orders";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = client.send(request, 
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                Platform.runLater(() -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        showAlert("报修申请提交成功！");
                        handleReset();
                        // 返回首页
                        showHome();
                    } else {
                        errorLabel.setText("提交失败：" + response.body());
                        submitBtn.setDisable(false);
                        submitBtn.setText("提交申请");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorLabel.setText("网络错误：" + e.getMessage());
                    submitBtn.setDisable(false);
                    submitBtn.setText("提交申请");
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleReset() {
        typeCombo.setValue(null);
        descArea.clear();
        phoneField.clear();
        errorLabel.setText("");
        submitBtn.setDisable(false);
        submitBtn.setText("提交申请");
    }

    @FXML
    private void showHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student-home-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showRepair() {
        // 当前已在报修页面
    }

    @FXML
    private void showProfile() {
        showAlert("个人信息功能开发中...");
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 500));
            stage.setTitle("宿舍管理系统 - 登录");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
