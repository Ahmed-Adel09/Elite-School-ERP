package com.elite.erp.presentation.student;

import com.elite.erp.MainApp;
import com.elite.erp.dao.DatabaseManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LockdownExamController implements Initializable {

    public static final boolean DEBUG_MODE = true; // Bypass fullscreen & always on top if true

    @FXML private Label timerLabel;
    @FXML private Label examTitleLabel;
    @FXML private Label questionCountLabel;
    @FXML private Label questionTextLabel;
    
    @FXML private RadioButton optARadio;
    @FXML private RadioButton optBRadio;
    @FXML private RadioButton optCRadio;
    @FXML private RadioButton optDRadio;
    
    private ToggleGroup optionsGroup;

    private int examId;
    private int remainingSeconds;
    private Timeline timeline;
    
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    
    private Stage stage;
    private boolean isSubmitted = false;

    private static class Question {
        int id; String text, optA, optB, optC, optD, correct;
        String selectedOpt = null;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        optionsGroup = new ToggleGroup();
        optARadio.setToggleGroup(optionsGroup);
        optBRadio.setToggleGroup(optionsGroup);
        optCRadio.setToggleGroup(optionsGroup);
        optDRadio.setToggleGroup(optionsGroup);

        optionsGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && questions.size() > 0) {
                RadioButton rb = (RadioButton) newVal;
                String selectedId = rb.getId();
                String choice = selectedId.replace("opt", "").replace("Radio", ""); // "A", "B", "C", or "D"
                questions.get(currentIndex).selectedOpt = choice;
            }
        });
        
        // Defer stage setup until it is shown
        Platform.runLater(this::setupLockdownStage);
    }

    public void initExam(int examId, String title, int durationMins) {
        this.examId = examId;
        this.remainingSeconds = durationMins * 60;
        examTitleLabel.setText(title);
        
        loadQuestions();
        if (!questions.isEmpty()) {
            displayQuestion(0);
            startTimer();
        } else {
            questionTextLabel.setText("No questions found for this exam.");
        }
    }

    private void setupLockdownStage() {
        if (timerLabel.getScene() != null && timerLabel.getScene().getWindow() instanceof Stage) {
            stage = (Stage) timerLabel.getScene().getWindow();
            
            if (!DEBUG_MODE) {
                stage.initStyle(StageStyle.UNDECORATED);
                stage.setFullScreen(true);
                stage.setAlwaysOnTop(true);
                
                // Anti-Cheat Focus Listener
                stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused && !isSubmitted) {
                        submitWithPenalty();
                    }
                });
                
                // Prevent closing via OS
                stage.setOnCloseRequest(e -> {
                    if (!isSubmitted) e.consume();
                });
            }
        }
    }

    private void loadQuestions() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM exam_questions WHERE exam_id = ?");
            ps.setInt(1, examId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.id = rs.getInt("id");
                q.text = rs.getString("question_text");
                q.optA = rs.getString("option_a");
                q.optB = rs.getString("option_b");
                q.optC = rs.getString("option_c");
                q.optD = rs.getString("option_d");
                q.correct = rs.getString("correct_option");
                questions.add(q);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;
        currentIndex = index;
        Question q = questions.get(index);
        
        questionCountLabel.setText("Question " + (index + 1) + " of " + questions.size());
        questionTextLabel.setText(q.text);
        
        optARadio.setText(q.optA);
        optBRadio.setText(q.optB);
        optCRadio.setText(q.optC);
        optDRadio.setText(q.optD);
        
        optARadio.setSelected("A".equals(q.selectedOpt));
        optBRadio.setSelected("B".equals(q.selectedOpt));
        optCRadio.setSelected("C".equals(q.selectedOpt));
        optDRadio.setSelected("D".equals(q.selectedOpt));
    }

    @FXML private void prevQuestion() { displayQuestion(currentIndex - 1); }
    @FXML private void nextQuestion() { displayQuestion(currentIndex + 1); }

    private void startTimer() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            int m = remainingSeconds / 60;
            int s = remainingSeconds % 60;
            timerLabel.setText(String.format("Time Remaining: %02d:%02d", m, s));
            
            if (remainingSeconds <= 0) {
                timeline.stop();
                submitExam();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void submitWithPenalty() {
        if (isSubmitted) return;
        isSubmitted = true;
        if (timeline != null) timeline.stop();
        
        // Log Cheating Attempt to SQLite
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO chat_history (class_id, sender_name, message) VALUES (?, ?, ?)");
            ps.setInt(1, -1); // System log
            ps.setString(2, "SYSTEM");
            ps.setString(3, "CHEATING ATTEMPT: User " + MainApp.getCurrentUser().getFullName() + " lost window focus during exam " + examId);
            ps.executeUpdate();
        } catch (Exception ignored) {}

        Alert alert = new Alert(Alert.AlertType.ERROR, "Exam terminated! Focus lost. This attempt has been logged.", ButtonType.OK);
        alert.showAndWait();
        closeAndReturn();
    }

    @FXML
    private void submitExam() {
        if (isSubmitted) return;
        isSubmitted = true;
        if (timeline != null) timeline.stop();

        // ── Calculate score: 1 point per correct answer ──
        int correct = 0;
        for (Question q : questions) {
            if (q.correct != null && q.correct.equals(q.selectedOpt)) correct++;
        }
        int total = questions.size();

        // ── Persist score to grades table ──
        saveExamScore(correct, total);

        String msg = String.format(
            "Exam submitted!\n\n✅ Score: %d / %d (%.0f%%)\n\nCheck your Grades tab to see the result.",
            correct, total, total > 0 ? (correct * 100.0 / total) : 0.0);
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle("Exam Complete");
        alert.setHeaderText("Well done!");
        alert.showAndWait();
        closeAndReturn();
    }

    /** Saves the exam result into the grades table so it shows in the student's grade view. */
    private void saveExamScore(int correct, int total) {
        com.elite.erp.model.User user = MainApp.getCurrentUser();
        if (user == null) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            // Find the student record id by email
            PreparedStatement findStudent = conn.prepareStatement(
                "SELECT id FROM students WHERE student_email = ?");
            findStudent.setString(1, user.getEmail());
            ResultSet sr = findStudent.executeQuery();
            if (!sr.next()) return;
            int studentId = sr.getInt(1);

            // Find exam title
            PreparedStatement findExam = conn.prepareStatement(
                "SELECT title FROM exams WHERE id = ?");
            findExam.setInt(1, examId);
            ResultSet er = findExam.executeQuery();
            String examTitle = er.next() ? er.getString("title") : "Exam #" + examId;

            // Insert into grades: score = correct answers, full mark = total questions
            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO grades (student_id, subject, score, date) VALUES (?, ?, ?, date('now','localtime'))");
            ins.setInt(1, studentId);
            ins.setString(2, "📝 " + examTitle + " [/" + total + "]");
            ins.setDouble(3, correct);
            ins.executeUpdate();

            System.out.println("[Exam] Score saved: " + correct + "/" + total + " for student " + user.getFullName());
        } catch (Exception e) {
            System.err.println("[Exam] Failed to save score: " + e.getMessage());
        }
    }

    private void closeAndReturn() {
        if (stage != null) stage.close();
        MainApp.switchScene("/com/elite/erp/fxml/StudentDashboard.fxml");
        if (MainApp.getPrimaryStage() != null) {
            MainApp.getPrimaryStage().show();
        }
    }
}
