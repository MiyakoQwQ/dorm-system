package com.teach.javafx.client.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import com.teach.javafx.client.DormClientApp;
import com.teach.javafx.entity.RepairOrder;

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
 * 学生首页控制器
 * 对应 student-home-view.fxml
 */
public class StudentHomeController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label pendingCount;
    @FXML private Label processingCount;
    @FXML private Label resolvedCount;
    @FXML private TableView<RepairOrder> orderTable;
    @FXML private TableColumn<RepairOrder, String> typeCol;
    @FXML private TableColumn<RepairOrder, String> descCol;
    @FXML private TableColumn<RepairOrder, String> statusCol;
    @FXML private TableColumn<RepairOrder, String> timeCol;
    @FXML private TableColumn<RepairOrder, Void> urgeCol;
    @FXML private TableColumn<RepairOrder, Void> reviewCol;

    private static final String SERVER_URL = DormClientApp.SERVER_URL;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadOrderData();
    }

    private void setupTableColumns() {
        // 报修类型列
        typeCol.setCellValueFactory(cell -> {
            String type = cell.getValue().getRepairType();
            return new SimpleStringProperty(type != null ? type : "");
        });
        typeCol.setText("报修类型");

        // 问题描述列
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        // 状态列 - 显示中文
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
            return new SimpleStringProperty(displayStatus);
        });
        
        // 时间列
        timeCol.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(
                cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            );
        });

        // 催单列
        urgeCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("催单");
            {
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 2 8;");
                btn.setOnAction(e -> {
                    RepairOrder order = getTableView().getItems().get(getIndex());
                    handleUrgeOrder(order);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    RepairOrder order = getTableView().getItems().get(getIndex());
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
        reviewCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("评价");
            {
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 2 8;");
                btn.setOnAction(e -> {
                    RepairOrder order = getTableView().getItems().get(getIndex());
                    showReviewDialog(order);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    RepairOrder order = getTableView().getItems().get(getIndex());
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
    }

    private void loadOrderData() {
        new Thread(() -> {
            try {
                String username = DormClientApp.getCurrentUsername();
                String url = SERVER_URL + "/api/orders?studentName=" + java.net.URLEncoder.encode(username, StandardCharsets.UTF_8);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() == 200) {
                    java.util.List<RepairOrder> orders = new java.util.ArrayList<>();
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    RepairOrder[] arr = mapper.readValue(response.body(), RepairOrder[].class);
                    for (RepairOrder o : arr) orders.add(o);

                    int pending = 0, processing = 0, resolved = 0;
                    for (RepairOrder o : orders) {
                        String s = o.getStatus();
                        if ("Pending".equals(s)) pending++;
                        else if ("Processing".equals(s)) processing++;
                        else if ("Resolved".equals(s)) resolved++;
                    }

                    final int p = pending, pr = processing, r = resolved;
                    Platform.runLater(() -> {
                        pendingCount.setText(String.valueOf(p));
                        processingCount.setText(String.valueOf(pr));
                        resolvedCount.setText(String.valueOf(r));
                        orderTable.getItems().setAll(orders);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleUrgeOrder(RepairOrder order) {
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
                HttpResponse<String> response = client.send(request, 
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        showAlert("催单成功！已通知管理员加快处理");
                        loadOrderData();
                    } else {
                        showAlert("催单失败: " + response.body());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("催单失败，请检查网络连接"));
                e.printStackTrace();
            }
        }).start();
    }

    private void showReviewDialog(RepairOrder order) {
        // 创建评价对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("评价工单");
        dialog.setHeaderText("为本次维修服务评分");
        
        // 创建评分选择
        ToggleGroup ratingGroup = new ToggleGroup();
        HBox ratingBox = new HBox(10);
        ratingBox.setAlignment(javafx.geometry.Pos.CENTER);
        RadioButton[] ratingBtns = new RadioButton[5];
        for (int i = 1; i <= 5; i++) {
            ratingBtns[i-1] = new RadioButton(String.valueOf(i) + "星");
            ratingBtns[i-1].setToggleGroup(ratingGroup);
            ratingBtns[i-1].setUserData(i);
            ratingBox.getChildren().add(ratingBtns[i-1]);
        }
        ratingBtns[4].setSelected(true);
        
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("请输入您的评价（可选）");
        commentArea.setPrefRowCount(3);
        
        dialog.getDialogPane().setContent(new javafx.scene.layout.VBox(10, ratingBox, commentArea));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                int rating = (int) ratingGroup.getSelectedToggle().getUserData();
                String comment = commentArea.getText().trim();
                submitReview(order.getId(), rating, comment);
            }
        });
    }

    private void submitReview(Long orderId, int rating, String comment) {
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
                HttpResponse<String> response = client.send(request, 
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        showAlert("评价提交成功！感谢您的反馈");
                        loadOrderData();
                    } else {
                        showAlert("评价失败: " + response.body());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("评价失败，请检查网络连接"));
                e.printStackTrace();
            }
        }).start();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void showHome() {
        // 当前已在首页
    }

    @FXML
    private void showRepair() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/repair-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("加载个人信息页面失败：" + e.getMessage());
        }
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
}
