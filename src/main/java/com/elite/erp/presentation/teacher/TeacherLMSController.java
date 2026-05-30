package com.elite.erp.presentation.teacher;

import com.elite.erp.MainApp;
import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class TeacherLMSController implements Initializable {

    // Material Upload
    @FXML private Label uploadStatus;
    @FXML private ComboBox<ComboItem> uploadClassCombo;
    @FXML private TextField uploadTitleField;
    @FXML private Label selectedFileLabel;
    private File selectedPdf;

    // Quiz Creator
    @FXML private Label quizStatus;
    @FXML private ComboBox<ComboItem> quizClassCombo;
    @FXML private TextField quizTitleField;
    @FXML private TextField quizDurationField;
    @FXML private TextField quizAvailabilityField;
    @FXML private VBox questionsContainer;

    // Group Chat
    @FXML private ComboBox<ComboItem> chatClassCombo;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInputField;
    @FXML private Label chatStatusLabel;

    // Manage Content
    @FXML private Label manageStatusLabel;
    @FXML private ListView<ComboItem> manageMaterialsList;
    @FXML private ListView<ComboItem> manageExamsList;

    private Socket chatSocket;
    private PrintWriter chatWriter;
    private int currentChatClassId = -1;

    private User currentUser;
    private int questionCounter = 0;

    private final List<ComboItem> assignedClasses = new ArrayList<>();

    public static class ComboItem {
        public final int id;
        public final String display;
        public ComboItem(int id, String display) { this.id = id; this.display = display; }
        @Override public String toString() { return display; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = MainApp.getCurrentUser();
        loadAssignedClasses();
        addQuestionFields(); // Add first question by default
        loadManageLists();
    }

    private void loadAssignedClasses() {
        if (currentUser == null) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT c.id, c.name FROM classes c " +
                "JOIN teacher_classes tc ON c.id = tc.class_id " +
                "WHERE tc.user_id = ?"
            );
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ComboItem item = new ComboItem(rs.getInt("id"), rs.getString("name"));
                assignedClasses.add(item);
                uploadClassCombo.getItems().add(item);
                quizClassCombo.getItems().add(item);
                if (chatClassCombo != null) chatClassCombo.getItems().add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Material Upload Logic ───────────────────────────────────────────────

    @FXML
    private void browsePDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF Material");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        selectedPdf = fileChooser.showOpenDialog(uploadTitleField.getScene().getWindow());
        
        if (selectedPdf != null) {
            selectedFileLabel.setText(selectedPdf.getName());
        } else {
            selectedFileLabel.setText("No file selected");
        }
    }

    @FXML
    private void uploadMaterial() {
        ComboItem selectedClass = uploadClassCombo.getValue();
        String title = uploadTitleField.getText();

        if (selectedClass == null || title.isEmpty() || selectedPdf == null) {
            uploadStatus.setText("❌ Please fill all fields and select a PDF.");
            uploadStatus.setStyle("-fx-text-fill: #FF6B6B;");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO assignments (class_id, teacher_id, title, file_path) VALUES (?, ?, ?, ?)"
            );
            ps.setInt(1, selectedClass.id);
            ps.setInt(2, currentUser.getId());
            ps.setString(3, title);
            ps.setString(4, selectedPdf.getAbsolutePath());
            ps.executeUpdate();

            uploadStatus.setText("✅ Material uploaded successfully.");
            uploadStatus.setStyle("-fx-text-fill: #4CD97B;");
            uploadTitleField.clear();
            selectedPdf = null;
            selectedFileLabel.setText("No file selected");
        } catch (Exception e) {
            e.printStackTrace();
            uploadStatus.setText("❌ Database error.");
            uploadStatus.setStyle("-fx-text-fill: #FF6B6B;");
        }
    }

    // ── Quiz Creator Logic ──────────────────────────────────────────────────

    @FXML
    private void addQuestionFields() {
        questionCounter++;
        VBox qBox = new VBox(8);
        qBox.setStyle("-fx-background-color: #0A1628; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #C9A84C; -fx-border-radius: 8;");
        
        Label lbl = new Label("Question " + questionCounter);
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        TextField qText = new TextField();
        qText.setPromptText("Enter question...");
        qText.setStyle("-fx-background-color: #162130; -fx-text-fill: white;");
        
        TextField optA = new TextField(); optA.setPromptText("Option A"); optA.setStyle("-fx-background-color: #162130; -fx-text-fill: white;");
        TextField optB = new TextField(); optB.setPromptText("Option B"); optB.setStyle("-fx-background-color: #162130; -fx-text-fill: white;");
        TextField optC = new TextField(); optC.setPromptText("Option C"); optC.setStyle("-fx-background-color: #162130; -fx-text-fill: white;");
        TextField optD = new TextField(); optD.setPromptText("Option D"); optD.setStyle("-fx-background-color: #162130; -fx-text-fill: white;");
        
        ComboBox<String> correctCombo = new ComboBox<>();
        correctCombo.getItems().addAll("A", "B", "C", "D");
        correctCombo.setPromptText("Correct Option");
        correctCombo.setStyle("-fx-background-color: #162130; -fx-text-fill: white;");
        
        qBox.getChildren().addAll(lbl, qText, optA, optB, optC, optD, correctCombo);
        questionsContainer.getChildren().add(qBox);
    }

    @FXML
    private void publishExam() {
        ComboItem selectedClass = quizClassCombo.getValue();
        String title = quizTitleField.getText();
        String durationStr = quizDurationField.getText();
        String availStr = quizAvailabilityField.getText();

        if (selectedClass == null || title.isEmpty() || durationStr.isEmpty() || availStr.isEmpty()) {
            quizStatus.setText("❌ Please fill exam details.");
            quizStatus.setStyle("-fx-text-fill: #FF6B6B;");
            return;
        }

        int duration, availabilityHours;
        try {
            duration = Integer.parseInt(durationStr);
            availabilityHours = Integer.parseInt(availStr);
        } catch (NumberFormatException e) {
            quizStatus.setText("❌ Duration/Availability must be numbers.");
            quizStatus.setStyle("-fx-text-fill: #FF6B6B;");
            return;
        }

        // ── BUG FIX: Compute expiry timestamp in Java, not via broken SQLite concatenation ──
        LocalDateTime expiry = LocalDateTime.now().plusHours(availabilityHours);
        String expiryStr = expiry.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Transaction

            // 1. Insert Exam — use plain ? for expiry_timestamp
            PreparedStatement examPs = conn.prepareStatement(
                "INSERT INTO exams (class_id, teacher_id, title, duration_minutes, expiry_timestamp) " +
                "VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            examPs.setInt(1, selectedClass.id);
            examPs.setInt(2, currentUser.getId());
            examPs.setString(3, title);
            examPs.setInt(4, duration);
            examPs.setString(5, expiryStr);
            examPs.executeUpdate();

            ResultSet rs = examPs.getGeneratedKeys();
            if (!rs.next()) throw new Exception("Failed to get exam ID.");
            int examId = rs.getInt(1);

            // 2. Insert Questions
            PreparedStatement qPs = conn.prepareStatement(
                "INSERT INTO exam_questions (exam_id, question_text, option_a, option_b, option_c, option_d, correct_option) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
            );

            for (var node : questionsContainer.getChildren()) {
                if (node instanceof VBox qBox) {
                    TextField qText = (TextField) qBox.getChildren().get(1);
                    TextField optA = (TextField) qBox.getChildren().get(2);
                    TextField optB = (TextField) qBox.getChildren().get(3);
                    TextField optC = (TextField) qBox.getChildren().get(4);
                    TextField optD = (TextField) qBox.getChildren().get(5);
                    @SuppressWarnings("unchecked")
                    ComboBox<String> correct = (ComboBox<String>) qBox.getChildren().get(6);

                    String text = qText.getText();
                    String cVal = correct.getValue();
                    if (text == null || text.isEmpty() || cVal == null) continue; // Skip invalid

                    qPs.setInt(1, examId);
                    qPs.setString(2, text);
                    qPs.setString(3, optA.getText());
                    qPs.setString(4, optB.getText());
                    qPs.setString(5, optC.getText());
                    qPs.setString(6, optD.getText());
                    qPs.setString(7, cVal);
                    qPs.addBatch();
                }
            }
            qPs.executeBatch();
            conn.commit();

            quizStatus.setText("✅ Exam created & published! Expires: " + expiryStr);
            quizStatus.setStyle("-fx-text-fill: #4CD97B;");
            
            // Reset
            quizTitleField.clear();
            quizDurationField.clear();
            quizAvailabilityField.clear();
            questionsContainer.getChildren().clear();
            questionCounter = 0;
            addQuestionFields();
            
        } catch (Exception e) {
            e.printStackTrace();
            quizStatus.setText("❌ Failed to save exam.");
            quizStatus.setStyle("-fx-text-fill: #FF6B6B;");
        }
    }

    // ── Manage Content Logic ────────────────────────────────────────────────

    @FXML
    private void loadManageLists() {
        if (currentUser == null) return;
        if (manageMaterialsList != null) manageMaterialsList.getItems().clear();
        if (manageExamsList != null) manageExamsList.getItems().clear();
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Load Materials
            PreparedStatement psMat = conn.prepareStatement("SELECT a.id, a.title, c.name as class_name FROM assignments a JOIN classes c ON a.class_id = c.id WHERE a.teacher_id = ?");
            psMat.setInt(1, currentUser.getId());
            ResultSet rsMat = psMat.executeQuery();
            while (rsMat.next()) {
                manageMaterialsList.getItems().add(new ComboItem(rsMat.getInt("id"), rsMat.getString("title") + " (" + rsMat.getString("class_name") + ")"));
            }
            
            // Load Exams
            PreparedStatement psExam = conn.prepareStatement("SELECT e.id, e.title, c.name as class_name FROM exams e JOIN classes c ON e.class_id = c.id WHERE e.teacher_id = ?");
            psExam.setInt(1, currentUser.getId());
            ResultSet rsExam = psExam.executeQuery();
            while (rsExam.next()) {
                manageExamsList.getItems().add(new ComboItem(rsExam.getInt("id"), rsExam.getString("title") + " (" + rsExam.getString("class_name") + ")"));
            }
            if (manageStatusLabel != null) {
                manageStatusLabel.setText("✅ Lists refreshed.");
                manageStatusLabel.setStyle("-fx-text-fill: #4CD97B;");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (manageStatusLabel != null) {
                manageStatusLabel.setText("❌ Failed to load lists.");
                manageStatusLabel.setStyle("-fx-text-fill: #FF6B6B;");
            }
        }
    }

    @FXML
    private void deleteSelectedMaterial() {
        ComboItem selected = manageMaterialsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            manageStatusLabel.setText("❌ Select a material to delete.");
            manageStatusLabel.setStyle("-fx-text-fill: #FF6B6B;");
            return;
        }
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM assignments WHERE id = ?")) {
            ps.setInt(1, selected.id);
            ps.executeUpdate();
            manageStatusLabel.setText("✅ Material deleted.");
            manageStatusLabel.setStyle("-fx-text-fill: #4CD97B;");
            loadManageLists();
        } catch (Exception e) {
            e.printStackTrace();
            manageStatusLabel.setText("❌ Failed to delete material.");
            manageStatusLabel.setStyle("-fx-text-fill: #FF6B6B;");
        }
    }

    @FXML
    private void deleteSelectedExam() {
        ComboItem selected = manageExamsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            manageStatusLabel.setText("❌ Select an exam to delete.");
            manageStatusLabel.setStyle("-fx-text-fill: #FF6B6B;");
            return;
        }
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psQ = conn.prepareStatement("DELETE FROM exam_questions WHERE exam_id = ?");
                 PreparedStatement psE = conn.prepareStatement("DELETE FROM exams WHERE id = ?")) {
                psQ.setInt(1, selected.id);
                psQ.executeUpdate();
                psE.setInt(1, selected.id);
                psE.executeUpdate();
                conn.commit();
                manageStatusLabel.setText("✅ Exam deleted.");
                manageStatusLabel.setStyle("-fx-text-fill: #4CD97B;");
                loadManageLists();
            }
        } catch (Exception e) {
            e.printStackTrace();
            manageStatusLabel.setText("❌ Failed to delete exam.");
            manageStatusLabel.setStyle("-fx-text-fill: #FF6B6B;");
        }
    }

    // ── Teacher Group Chat ──────────────────────────────────────────────────

    @FXML
    private void joinChatRoom() {
        ComboItem selected = chatClassCombo.getValue();
        if (selected == null) {
            if (chatStatusLabel != null) {
                chatStatusLabel.setText("❌ Please select a class first.");
                chatStatusLabel.setStyle("-fx-text-fill: #FF6B6B;");
            }
            return;
        }

        // Disconnect from any previous room
        if (chatSocket != null && !chatSocket.isClosed()) {
            try { chatSocket.close(); } catch (Exception ignored) {}
        }

        currentChatClassId = selected.id;
        chatArea.clear();
        chatArea.appendText("[System] Connecting to class " + selected.display + " chat room...\n");

        if (chatStatusLabel != null) {
            chatStatusLabel.setText("🟢 Connected to: " + selected.display);
            chatStatusLabel.setStyle("-fx-text-fill: #4CD97B;");
        }

        new Thread(() -> {
            try {
                chatSocket = new Socket("localhost", 9998);
                chatWriter = new PrintWriter(new OutputStreamWriter(chatSocket.getOutputStream()), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));

                // JOIN with role=TEACHER so server can tag messages
                chatWriter.println("JOIN|" + currentChatClassId + "|" + currentUser.getFullName()
                        + "|" + currentUser.getId() + "|TEACHER");

                // ── Load chat history from DB ──
                loadChatHistory(currentChatClassId);

                String line;
                while ((line = reader.readLine()) != null) {
                    final String msg = line;
                    Platform.runLater(() -> chatArea.appendText(msg + "\n"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    chatArea.appendText("[System] Disconnected from chat.\n");
                    if (chatStatusLabel != null) {
                        chatStatusLabel.setText("🔴 Disconnected");
                        chatStatusLabel.setStyle("-fx-text-fill: #FF6B6B;");
                    }
                });
            }
        }, "TeacherChat-Thread").start();
    }

    private void loadChatHistory(int classId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT sender_name, message, timestamp FROM chat_history " +
                "WHERE class_id = ? ORDER BY id DESC LIMIT 50");
            ps.setInt(1, classId);
            ResultSet rs = ps.executeQuery();

            List<String> history = new ArrayList<>();
            while (rs.next()) {
                String sender = rs.getString("sender_name");
                String msg    = rs.getString("message");
                String ts     = rs.getString("timestamp");
                history.add(0, "[" + ts.substring(11, 16) + "] " + sender + ": " + msg);
            }

            if (!history.isEmpty()) {
                Platform.runLater(() -> {
                    chatArea.appendText("── Chat History ────────────────────\n");
                    history.forEach(m -> chatArea.appendText(m + "\n"));
                    chatArea.appendText("── Live ────────────────────────────\n");
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void sendChatMessage() {
        if (chatWriter == null || chatSocket == null || chatSocket.isClosed()) {
            chatArea.appendText("[System] Not connected. Select a class and click Join.\n");
            return;
        }
        String msg = chatInputField.getText().trim();
        if (!msg.isEmpty()) {
            // Server will see role=TEACHER and prepend [TEACHER] tag
            chatWriter.println("MSG|" + currentUser.getFullName() + "|" + msg);
            chatInputField.clear();
        }
    }

    @FXML
    private void logout() {
        if (chatSocket != null && !chatSocket.isClosed()) {
            try { chatSocket.close(); } catch (Exception ignored) {}
        }
        MainApp.setCurrentUser(null);
        MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
    }
}
