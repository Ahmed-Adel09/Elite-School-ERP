package com.elite.erp.presentation.dashboard;

import com.elite.erp.MainApp;
import com.elite.erp.business.BackgroundSyncTask;
import com.elite.erp.business.StudentService;
import com.elite.erp.model.Student;
import com.elite.erp.network.NotificationClient;
import com.elite.erp.report.EnrollmentReportApp;
import com.elite.erp.util.Response;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label                     totalStudentsLabel;
    @FXML private TableView<Student>        studentsTable;
    @FXML private TableColumn<Student, Integer> colId;
    @FXML private TableColumn<Student, String>  colFirstName;
    @FXML private TableColumn<Student, String>  colLastName;
    @FXML private TableColumn<Student, String>  colGrade;
    @FXML private TableColumn<Student, String>  colNationality;
    @FXML private ProgressBar               syncProgress;
    @FXML private Label                     syncStatus;
    @FXML private VBox                      notificationBox;
    @FXML private Label                     notificationBadge;

    private final StudentService      studentService  = new StudentService();
    private final NotificationClient  notifClient     = new NotificationClient();
    private int notifCount = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadStudents();
        connectNotificationClient();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colGrade.setCellValueFactory(new PropertyValueFactory<>("applyingForGrade"));
        colNationality.setCellValueFactory(new PropertyValueFactory<>("nationality"));
    }

    private void loadStudents() {
        Response<List<Student>> resp = studentService.getAllStudents();
        if (resp.isSuccess()) {
            ObservableList<Student> data = FXCollections.observableArrayList(resp.getData());
            studentsTable.setItems(data);
            totalStudentsLabel.setText(String.valueOf(data.size()));
        } else {
            totalStudentsLabel.setText("0");
        }
    }

    /** Start the NotificationClient socket connection — Socket Programming */
    private void connectNotificationClient() {
        notifClient.connect(msg -> {
            Label lbl = new Label("🔔 " + msg);
            lbl.setStyle("-fx-text-fill: #C9A84C; -fx-font-size: 12px; -fx-padding: 4 8;");
            lbl.setWrapText(true);
            FadeTransition ft = new FadeTransition(Duration.millis(400), lbl);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            notificationBox.getChildren().add(0, lbl);
            if (notificationBox.getChildren().size() > 6)
                notificationBox.getChildren().remove(6);
            notifCount++;
            notificationBadge.setText(String.valueOf(notifCount));
            notificationBadge.setVisible(true);
        });
    }

    /** Run BackgroundSyncTask — Multithreading requirement */
    @FXML
    private void runSync() {
        BackgroundSyncTask task = new BackgroundSyncTask();
        syncProgress.progressProperty().bind(task.progressProperty());
        syncStatus.textProperty().bind(task.messageProperty());
        syncProgress.setVisible(true);

        javafx.concurrent.Service<Void> service = new javafx.concurrent.Service<>() {
            @Override protected javafx.concurrent.Task<Void> createTask() { return task; }
        };
        service.setOnSucceeded(e -> {
            syncProgress.progressProperty().unbind();
            syncStatus.textProperty().unbind();
            loadStudents(); // refresh table after sync
        });
        service.start();
    }

    /** Open FXGL Enrollment Report in separate thread */
    @FXML
    private void openReport() {
        EnrollmentReportApp.launchInThread();
    }

    @FXML
    private void newAdmission() {
        MainApp.resetApplication();
        MainApp.switchScene("/com/elite/erp/fxml/Step1.fxml");
    }

    @FXML
    private void refreshTable() { loadStudents(); }

    @FXML
    private void openAnnouncements() {
        MainApp.switchScene("/com/elite/erp/fxml/AnnouncementsFeed.fxml");
    }

    @FXML
    private void logout() {
        notifClient.disconnect();
        MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
    }

    @FXML
    private void deleteSelected() {
        Student selected = studentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getFullName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                studentService.deleteStudent(selected.getId());
                loadStudents();
            }
        });
    }
}
