package com.elite.erp.presentation.parent;

import com.elite.erp.MainApp;
import com.elite.erp.dao.AdmissionDAO;
import com.elite.erp.dao.ParentPortalDAO;
import com.elite.erp.dao.UserDAO;
import com.elite.erp.fxgl.FXGLEmbeddedChart;
import com.elite.erp.model.*;
import com.elite.erp.util.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.elite.erp.presentation.announcements.AnnouncementFeedWidget;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import com.elite.erp.business.RecommendationEngine;
import java.io.*;
import java.net.Socket;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ParentDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label studentNameLabel;
    @FXML private Label balanceLabel;
    
    @FXML private Label noCoursesLabel;
    @FXML private ComboBox<Student> studentSwitcher;
    @FXML private StackPane chartPane;
    @FXML private TableView<Grade> gradesTable;
    @FXML private TableColumn<Grade, String> colGradeDate;
    @FXML private TableColumn<Grade, String> colGradeSubject;
    @FXML private TableColumn<Grade, Number> colGradeScore;

    @FXML private ListView<SchoolEvent> eventsListView;
    @FXML private VBox announcementsContainer;

    // ── AI Grade Analyzer ─────────────────────────────────────────────────────
    @FXML private TextField mathField;
    @FXML private TextField scienceField;
    @FXML private TextField languageField;
    @FXML private TextField artsField;
    @FXML private Label     aiErrorLabel;
    @FXML private TextArea  analysisOutput;

    // ── Private DMs ───────────────────────────────────────────────────────────
    @FXML private ComboBox<User> teacherCombo;
    @FXML private Label          dmStatusLabel;
    @FXML private TextArea       dmHistoryArea;
    @FXML private TextField      dmInputField;

    private PrintWriter dmOut;
    private Socket      dmSocket;
    
    private final ParentPortalDAO portalDAO = new ParentPortalDAO();
    private final AdmissionDAO admissionDAO = new AdmissionDAO();
    private final UserDAO userDAO = new UserDAO();

    private User currentUser;
    private int linkedStudentId = -1;
    private int linkedParentId = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = MainApp.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getFullName());
            loadLinkedData();
            
            // Render FXGL Root into the StackPane dynamically
            FXGLEmbeddedChart.getInstance().embedInto(chartPane);
            
            if (announcementsContainer != null) {
                announcementsContainer.getChildren().add(new AnnouncementFeedWidget("PARENT"));
            }

            // Load Teachers for DMs
            loadTeachers();
            connectToChatServer();

            if (gradesTable != null) {
                colGradeDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate()));
                colGradeSubject.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));
                colGradeScore.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getScore()));
            }
        }
    }

    private void loadLinkedData() {
        Response<Parent> parentResp = admissionDAO.getParentByEmail(currentUser.getEmail());
        if (parentResp.isSuccess()) {
            Parent p = parentResp.getData();
            linkedParentId = p.getId();
            
            // Populate Student Switcher
            Response<List<Student>> kidsResp = portalDAO.getStudentsByParentEmail(currentUser.getEmail());
            if (kidsResp.isSuccess() && !kidsResp.getData().isEmpty()) {
                studentSwitcher.getItems().setAll(kidsResp.getData());
                studentSwitcher.getSelectionModel().selectFirst();
                refreshStudentData(studentSwitcher.getValue());
            }

            // Load Finances
            updateBalance();
            
            // Load Events
            Response<List<SchoolEvent>> eventsResp = portalDAO.getSchoolEvents();
            if (eventsResp.isSuccess()) {
                eventsListView.getItems().setAll(eventsResp.getData());
            }
        }
    }
    
    @FXML
    private void handleStudentSwitch() {
        Student selected = studentSwitcher.getValue();
        if (selected != null) {
            refreshStudentData(selected);
        }
    }

    @FXML
    private void openAnnouncements() {
        MainApp.switchScene("/com/elite/erp/fxml/AnnouncementsFeed.fxml");
    }

    private void refreshStudentData(Student s) {
        linkedStudentId = s.getId();
        studentNameLabel.setText("Child: " + s.getFullName() + " | Grade: " + s.getApplyingForGrade());
        
        // Push ID to FXGL
        FXGLEmbeddedChart.getInstance().setStudentId(linkedStudentId);

        // Check courses fallback & load table
        Response<List<Grade>> grades = portalDAO.getStudentGrades(linkedStudentId);
        if (gradesTable != null) gradesTable.getItems().clear();
        
        if (grades.isSuccess() && !grades.getData().isEmpty()) {
            noCoursesLabel.setVisible(false);
            chartPane.setVisible(true);
            if (gradesTable != null) {
                gradesTable.getItems().setAll(grades.getData());
            }
        } else {
            noCoursesLabel.setVisible(true);
            chartPane.setVisible(false);
        }
    }

    private void updateBalance() {
        if (linkedParentId > 0) {
            Response<Double> balResp = portalDAO.getCreditsBalance(linkedParentId);
            if (balResp.isSuccess()) {
                balanceLabel.setText(String.format("USD %.2f", balResp.getData()));
            }
        }
    }

    @FXML
    private void handleTopUp() {
        if (linkedParentId > 0) {
            portalDAO.updateCredits(linkedParentId, 100.0);
            updateBalance();
            showAlert("Top-Up Successful", "Added USD 100.00 to Elite Credits.");
        }
    }

    @FXML
    private void handleEnrollEvent() {
        SchoolEvent event = eventsListView.getSelectionModel().getSelectedItem();
        if (event == null) {
            showAlert("Selection Error", "Please select an event to enroll.");
            return;
        }
        if (linkedParentId > 0) {
            Response<Double> balResp = portalDAO.getCreditsBalance(linkedParentId);
            if (balResp.isSuccess()) {
                double currentBal = balResp.getData();
                if (currentBal >= event.getCost()) {
                    portalDAO.updateCredits(linkedParentId, -event.getCost());
                    updateBalance();
                    showAlert("Enrollment Successful", "Enrolled in " + event.getName() + " for USD " + event.getCost());
                } else {
                    showAlert("Insufficient Funds", "You need more Elite Credits to enroll.");
                }
            }
        }
    }

    @FXML
    private void handleRegisterChild() {
        // We will open EnrollSibling.fxml
        MainApp.switchScene("/com/elite/erp/fxml/EnrollSibling.fxml");
    }

    @FXML
    private void logout() {
        MainApp.logout();
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // ── AI Grade Analyzer ─────────────────────────────────────────────────────

    @FXML
    private void runAiAnalysis() {
        try {
            double math = Double.parseDouble(mathField.getText().trim());
            double sci  = Double.parseDouble(scienceField.getText().trim());
            double lang = Double.parseDouble(languageField.getText().trim());
            double arts = Double.parseDouble(artsField.getText().trim());

            if (math < 0 || math > 100 || sci < 0 || sci > 100 || lang < 0 || lang > 100 || arts < 0 || arts > 100) {
                aiErrorLabel.setText("⚠️ Scores must be between 0 and 100.");
                return;
            }
            aiErrorLabel.setText("");

            RecommendationEngine engine = new RecommendationEngine();
            String report = engine.analyse(math, sci, lang, arts);
            analysisOutput.setText(report);

        } catch (NumberFormatException e) {
            aiErrorLabel.setText("⚠️ Please enter valid numeric scores.");
        }
    }

    // ── Private DMs ───────────────────────────────────────────────────────────

    private void loadTeachers() {
        Response<List<User>> res = userDAO.findAll();
        if (res.isSuccess()) {
            ObservableList<User> teachers = FXCollections.observableArrayList();
            for (User u : res.getData()) {
                if ("TEACHER".equals(u.getRole())) teachers.add(u);
            }
            if (teacherCombo != null) teacherCombo.setItems(teachers);
        }
    }

    private void connectToChatServer() {
        new Thread(() -> {
            try {
                dmSocket = new Socket("localhost", 9998);
                dmOut = new PrintWriter(new OutputStreamWriter(dmSocket.getOutputStream()), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(dmSocket.getInputStream()));

                // JOIN|classId|username|user_id   (classId -1 since Parent doesn't join a class room)
                dmOut.println("JOIN|-1|" + currentUser.getFullName() + "|" + currentUser.getId());

                Platform.runLater(() -> dmStatusLabel.setText("🟢 Connected to Chat Server"));
                dmStatusLabel.setStyle("-fx-text-fill: #4CD97B;");

                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    Platform.runLater(() -> dmHistoryArea.appendText(msg + "\n"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    dmStatusLabel.setText("🔴 Disconnected");
                    dmStatusLabel.setStyle("-fx-text-fill: #FF6B6B;");
                });
            }
        }, "ParentDM-Thread").start();
    }

    @FXML
    private void sendDm() {
        User teacher = teacherCombo.getValue();
        if (teacher == null) {
            dmStatusLabel.setText("⚠️ Select a teacher first.");
            dmStatusLabel.setStyle("-fx-text-fill: #F5A623;");
            return;
        }
        String text = dmInputField.getText().trim();
        if (text.isEmpty() || dmOut == null) return;

        // PRIVATE|receiver_id|message
        dmOut.println("PRIVATE|" + teacher.getId() + "|" + text);
        dmInputField.clear();
        dmStatusLabel.setText("✅ Sent");
        dmStatusLabel.setStyle("-fx-text-fill: #4CD97B;");
    }
}
