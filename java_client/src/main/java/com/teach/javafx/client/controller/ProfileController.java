package com.teach.javafx.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.teach.javafx.client.DormClientApp;
import com.teach.javafx.entity.StudentInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * 个人信息控制器
 * 对应 profile-view.fxml
 */
public class ProfileController implements Initializable {

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private TextField studentNoField;
    @FXML private TextField realNameField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private TextField phoneField;
    @FXML private TextField departmentField;
    @FXML private TextField majorField;
    @FXML private TextField classNameField;
    @FXML private TextField gradeField;
    @FXML private TextField roomNumberField;
    @FXML private TextField bedNumberField;
    @FXML private Label checkInDateLabel;
    @FXML private Label statusLabel;
    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private static final String SERVER_URL = DormClientApp.SERVER_URL;
    private StudentInfo currentStudentInfo;
    private boolean isEditing = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化性别选择
        genderCombo.getItems().addAll("男", "女");
        
        // 加载个人信息
        loadProfileData();
        
        // 设置只读模式
        setEditable(false);
    }

    private void loadProfileData() {
        String username = DormClientApp.getCurrentUsername();
        usernameLabel.setText(username);
        roleLabel.setText("学生");

        new Thread(() -> {
            try {
                // 先获取用户信息
                String userUrl = SERVER_URL + "/api/users/by-username?username=" + 
                        java.net.URLEncoder.encode(username, StandardCharsets.UTF_8);
                
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest userRequest = HttpRequest.newBuilder()
                        .uri(URI.create(userUrl))
                        .GET()
                        .build();
                HttpResponse<String> userResponse = client.send(userRequest,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (userResponse.statusCode() == 200) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = 
                            new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    
                    com.teach.javafx.entity.User user = 
                            mapper.readValue(userResponse.body(), com.teach.javafx.entity.User.class);
                    
                    // 再获取学生详细信息
                    String studentUrl = SERVER_URL + "/api/students/by-user/" + user.getId();
                    HttpRequest studentRequest = HttpRequest.newBuilder()
                            .uri(URI.create(studentUrl))
                            .GET()
                            .build();
                    HttpResponse<String> studentResponse = client.send(studentRequest,
                            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    
                    if (studentResponse.statusCode() == 200) {
                        currentStudentInfo = mapper.readValue(studentResponse.body(), StudentInfo.class);
                        Platform.runLater(() -> displayStudentInfo(currentStudentInfo));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("加载个人信息失败：" + e.getMessage()));
            }
        }).start();
    }

    private void displayStudentInfo(StudentInfo info) {
        if (info == null) return;
        
        studentNoField.setText(info.getStudentNo() != null ? info.getStudentNo() : "");
        realNameField.setText(info.getRealName() != null ? info.getRealName() : "");
        genderCombo.setValue(info.getGender() != null ? info.getGender() : "男");
        phoneField.setText(info.getPhone() != null ? info.getPhone() : "");
        departmentField.setText(info.getDepartment() != null ? info.getDepartment() : "");
        majorField.setText(info.getMajor() != null ? info.getMajor() : "");
        classNameField.setText(info.getClassName() != null ? info.getClassName() : "");
        gradeField.setText(info.getGrade() != null ? String.valueOf(info.getGrade()) : "");
        roomNumberField.setText(info.getRoomNumber() != null ? info.getRoomNumber() : "");
        bedNumberField.setText(info.getBedNumber() != null ? String.valueOf(info.getBedNumber()) : "");
        
        if (info.getCheckInDate() != null) {
            checkInDateLabel.setText(info.getCheckInDate().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        } else {
            checkInDateLabel.setText("未设置");
        }
        
        statusLabel.setText(info.getStatus() != null ? info.getStatus() : "在住");
    }

    private void setEditable(boolean editable) {
        isEditing = editable;
        
        // 可编辑字段
        phoneField.setEditable(editable);
        
        // 只读字段（由管理员或系统维护）
        studentNoField.setEditable(false);
        realNameField.setEditable(false);
        genderCombo.setDisable(!editable);
        departmentField.setEditable(false);
        majorField.setEditable(false);
        classNameField.setEditable(false);
        gradeField.setEditable(false);
        roomNumberField.setEditable(false);
        bedNumberField.setEditable(false);
        
        // 按钮状态
        editButton.setVisible(!editable);
        saveButton.setVisible(editable);
        cancelButton.setVisible(editable);
    }

    @FXML
    private void handleEdit() {
        setEditable(true);
    }

    @FXML
    private void handleSave() {
        if (currentStudentInfo == null) return;
        
        // 更新可修改的字段
        currentStudentInfo.setPhone(phoneField.getText().trim());
        currentStudentInfo.setGender(genderCombo.getValue());
        
        new Thread(() -> {
            try {
                String url = SERVER_URL + "/api/students/" + currentStudentInfo.getId();
                
                com.fasterxml.jackson.databind.ObjectMapper mapper = 
                        new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                String jsonBody = mapper.writeValueAsString(currentStudentInfo);
                
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        showAlert("个人信息保存成功！");
                        setEditable(false);
                    } else {
                        showAlert("保存失败：" + response.body());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("保存失败：" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        // 重新加载数据，放弃修改
        displayStudentInfo(currentStudentInfo);
        setEditable(false);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student-home-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
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
