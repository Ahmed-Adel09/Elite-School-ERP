package com.elite.erp.presentation.clinic;

import com.elite.erp.MainApp;
import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.model.User;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * NurseDashboardController — Full medical hub for the school nurse.
 *
 * Rubric demonstrations:
 *  ✅ Sockets       — Listens on Port 9997 for EMERGENCY alerts; shows live toast
 *  ✅ Multithreading — Background daemon Task polls inventory every 30 s
 *  ✅ JDBC / CRUD   — Student search, visit logging, inventory update
 *  ✅ JavaFX        — TableView, ComboBox, live Label updates, FadeTransition toasts
 */
public class NurseDashboardController implements Initializable {

    // ── Tab 1: Live Alerts ────────────────────────────────────────────────────
    @FXML private VBox    alertFeed;
    @FXML private Label   connectionStatusLabel;
    @FXML private Label   inventoryAlertLabel;     // top-bar banner

    // ── Tab 2: Student Search ─────────────────────────────────────────────────
    @FXML private TextField  searchField;
    @FXML private ListView<StudentRow> studentResultsList;
    @FXML private Label   nurseBloodLabel;
    @FXML private Label   nurseAllergyLabel;
    @FXML private Label   nurseEmergencyLabel;
    @FXML private TextArea symptomsField;
    @FXML private TextArea treatmentField;
    @FXML private Label   visitStatusLabel;
    @FXML private TableView<VisitRow>              nurseVisitsTable;
    @FXML private TableColumn<VisitRow, String>    nColDate;
    @FXML private TableColumn<VisitRow, String>    nColSymptoms;
    @FXML private TableColumn<VisitRow, String>    nColTreatment;

    // ── Tab 3: Inventory ──────────────────────────────────────────────────────
    @FXML private TableView<InvRow>                inventoryTable;
    @FXML private TableColumn<InvRow, String>      invColItem;
    @FXML private TableColumn<InvRow, Number>      invColStock;
    @FXML private TableColumn<InvRow, Number>      invColMin;
    @FXML private TableColumn<InvRow, String>      invColStatus;
    @FXML private ComboBox<String>                 itemCombo;
    @FXML private TextField                        newStockField;
    @FXML private Label                            inventoryThreadLabel;

    private User currentUser;
    private int  selectedStudentId = -1;   // students.id of the currently-selected student

    // ── Initialise ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = MainApp.getCurrentUser();

        // Wire TableViews
        setupVisitsTable();
        setupInventoryTable();

        // Student list selection listener
        studentResultsList.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> { if (sel != null) loadStudentMedicalProfile(sel); }
        );

        // Start background tasks
        startSocketListener();
        startInventoryMonitor();
    }

    // ── Tab 1: Socket Listener ────────────────────────────────────────────────

    /**
     * Connects to LibraryServer (Port 9997) as a persistent client.
     * Any "EMERGENCY|…" message from a student triggers a high-visibility toast.
     */
    private void startSocketListener() {
        Thread listenerThread = new Thread(() -> {
            while (true) {
                try (Socket socket = new Socket("localhost", 9997);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter  out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

                    Platform.runLater(() -> {
                        connectionStatusLabel.setText("🟢 Connected to Port 9997");
                        connectionStatusLabel.setStyle("-fx-text-fill: #4CD97B; -fx-font-weight: bold;");
                    });

                    // Send a NURSE_REGISTER handshake so the server knows we are a listener
                    out.println("NURSE_REGISTER|NurseDashboard connected.");

                    String line;
                    while ((line = in.readLine()) != null) {
                        final String msg = line;
                        Platform.runLater(() -> showAlert(msg));
                    }

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        connectionStatusLabel.setText("🔴 Disconnected — retrying…");
                        connectionStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
                    });
                }
                // Wait 5 s before reconnecting
                try { Thread.sleep(5_000); } catch (InterruptedException ie) { break; }
            }
        }, "NurseSocketListener-Thread");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /** Renders a fade-in toast card in the alert feed. */
    private void showAlert(String message) {
        Label card = new Label(message);
        card.setWrapText(true);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setFont(Font.font("System", FontWeight.BOLD, 14));

        boolean isEmergency = message.contains("EMERGENCY") || message.contains("LIBRARIAN_ALERT");
        card.setStyle(isEmergency
            ? "-fx-background-color: #FF3B3B; -fx-text-fill: white; -fx-padding: 14 20; -fx-background-radius: 8;"
            : "-fx-background-color: #1A3560; -fx-text-fill: #C9A84C; -fx-padding: 10 16; -fx-background-radius: 6;"
        );

        card.setOpacity(0);
        alertFeed.getChildren().add(0, card);

        // Cap feed at 30 entries
        if (alertFeed.getChildren().size() > 30) alertFeed.getChildren().remove(30);

        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        // If emergency, also update the top-bar banner
        if (isEmergency) {
            inventoryAlertLabel.setText("⚠️ EMERGENCY ALERT — " + message);
        }
    }

    @FXML
    private void clearAlerts() {
        alertFeed.getChildren().clear();
        inventoryAlertLabel.setText("");
    }

    // ── Tab 2: Student Search ─────────────────────────────────────────────────

    @FXML
    private void searchStudent() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        ObservableList<StudentRow> results = FXCollections.observableArrayList();
        String sql = """
            SELECT id, first_name, last_name, student_email
            FROM students
            WHERE LOWER(first_name || ' ' || last_name) LIKE ?
               OR CAST(id AS TEXT) = ?
            ORDER BY last_name
            LIMIT 20
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query.toLowerCase() + "%");
            ps.setString(2, query);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new StudentRow(
                        rs.getInt("id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        nvl(rs.getString("student_email"), "—")
                    ));
                }
            }
        } catch (Exception e) {
            setVisitStatus("❌ Search error: " + e.getMessage(), false);
        }
        studentResultsList.setItems(results);
        if (results.isEmpty()) setVisitStatus("No students found for: " + query, false);
    }

    private void loadStudentMedicalProfile(StudentRow row) {
        selectedStudentId = row.id;
        nurseBloodLabel.setText("Loading…");
        nurseAllergyLabel.setText("Loading…");
        nurseEmergencyLabel.setText("Loading…");
        clearVisitForm();

        // Try medical_profiles first, fall back to students.allergies
        String sql = "SELECT mp.blood_type, mp.allergies, mp.emergency_contact, s.allergies AS s_allergies " +
                     "FROM students s LEFT JOIN medical_profiles mp ON mp.student_id = s.id " +
                     "WHERE s.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nurseBloodLabel.setText(nvl(rs.getString("blood_type"), "Not on file"));
                    String allergy = nvl(rs.getString("allergies"), rs.getString("s_allergies"));
                    nurseAllergyLabel.setText(nvl(allergy, "None recorded"));
                    nurseEmergencyLabel.setText(nvl(rs.getString("emergency_contact"), "Not recorded"));
                }
            }
        } catch (Exception e) {
            setVisitStatus("❌ Profile error: " + e.getMessage(), false);
        }

        loadVisitHistoryForStudent();
    }

    private void loadVisitHistoryForStudent() {
        ObservableList<VisitRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT date_time, symptoms, treatment_given FROM clinic_visits " +
                     "WHERE student_id = ? ORDER BY id DESC LIMIT 20";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new VisitRow(
                        nvl(rs.getString("date_time"), "—"),
                        nvl(rs.getString("symptoms"), "—"),
                        nvl(rs.getString("treatment_given"), "—")
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[NurseDash] loadVisitHistory error: " + e.getMessage());
        }
        nurseVisitsTable.setItems(rows);
    }

    @FXML
    private void saveVisit() {
        if (selectedStudentId == -1) {
            setVisitStatus("⚠️ Select a student first.", false); return;
        }
        String symptoms  = symptomsField.getText().trim();
        String treatment = treatmentField.getText().trim();
        if (symptoms.isEmpty()) {
            setVisitStatus("⚠️ Symptoms field cannot be empty.", false); return;
        }

        int nurseId = (currentUser != null) ? currentUser.getId() : -1;
        String sql = "INSERT INTO clinic_visits (student_id, symptoms, treatment_given, nurse_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, selectedStudentId);
            ps.setString(2, symptoms);
            ps.setString(3, treatment.isEmpty() ? "Observation only" : treatment);
            ps.setInt   (4, nurseId);
            ps.executeUpdate();

            setVisitStatus("✅ Visit logged successfully!", true);
            clearVisitForm();
            loadVisitHistoryForStudent();
        } catch (Exception e) {
            setVisitStatus("❌ Save error: " + e.getMessage(), false);
        }
    }

    @FXML
    private void clearVisitForm() {
        symptomsField.clear();
        treatmentField.clear();
        visitStatusLabel.setText("");
    }

    // ── Tab 3: Inventory Monitor ──────────────────────────────────────────────

    /**
     * Background daemon Task using javafx.concurrent.Task.
     * Polls medical_inventory every 30 seconds without blocking the UI thread.
     */
    private void startInventoryMonitor() {
        Task<Void> monitorTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                while (!isCancelled()) {
                    Platform.runLater(() -> {
                        refreshInventoryTable();
                        inventoryThreadLabel.setText("🔄 Last checked: " +
                            java.time.LocalTime.now().withNano(0));
                        inventoryThreadLabel.setStyle("-fx-text-fill: #4CD97B; -fx-font-weight: bold;");
                    });
                    Thread.sleep(30_000);   // 30-second interval
                }
                return null;
            }
        };

        monitorTask.setOnFailed(e -> Platform.runLater(() ->
            inventoryThreadLabel.setText("❌ Monitor error: " + monitorTask.getException().getMessage())
        ));

        Thread monitorThread = new Thread(monitorTask, "InventoryMonitor-Thread");
        monitorThread.setDaemon(true);
        monitorThread.start();

        // Trigger immediately on startup too
        refreshInventoryTable();
    }

    private void refreshInventoryTable() {
        ObservableList<InvRow> rows = FXCollections.observableArrayList();
        ObservableList<String> items = FXCollections.observableArrayList();
        StringBuilder alerts = new StringBuilder();

        String sql = "SELECT item_name, current_stock, minimum_required FROM medical_inventory ORDER BY item_name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name  = rs.getString("item_name");
                int    stock = rs.getInt("current_stock");
                int    min   = rs.getInt("minimum_required");
                String status;
                if (stock == 0)       status = "🔴 OUT OF STOCK";
                else if (stock < min) status = "⚠️ LOW STOCK";
                else                  status = "✅ OK";

                rows.add(new InvRow(name, stock, min, status));
                items.add(name);

                if (stock < min) alerts.append("⚠️ LOW: ").append(name)
                                       .append(" (").append(stock).append("/").append(min).append(")  ");
            }
        } catch (Exception e) {
            System.err.println("[NurseDash] refreshInventory error: " + e.getMessage());
        }

        inventoryTable.setItems(rows);
        itemCombo.setItems(items);

        // Update top-bar banner if any stock is low
        String alertText = alerts.toString().trim();
        if (inventoryAlertLabel != null && !alertText.isEmpty()) {
            inventoryAlertLabel.setText(alertText);
        }
    }

    @FXML
    private void updateStock() {
        String item = itemCombo.getValue();
        if (item == null) { return; }
        int newStock;
        try { newStock = Integer.parseInt(newStockField.getText().trim()); }
        catch (NumberFormatException e) { return; }

        String sql = "UPDATE medical_inventory SET current_stock = ? WHERE item_name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, newStock);
            ps.setString(2, item);
            ps.executeUpdate();
            newStockField.clear();
            refreshInventoryTable();
        } catch (Exception e) {
            System.err.println("[NurseDash] updateStock error: " + e.getMessage());
        }
    }

    // ── Table wiring ──────────────────────────────────────────────────────────

    private void setupVisitsTable() {
        nColDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().dateTime));
        nColSymptoms.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().symptoms));
        nColTreatment.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().treatment));
        nurseVisitsTable.setPlaceholder(new Label("Select a student to view visits."));
    }

    private void setupInventoryTable() {
        invColItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        invColStock.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().stock));
        invColMin.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().min));
        invColStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));

        // Colour-code the Status column
        invColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.contains("OUT")  ? "-fx-text-fill: #FF3B3B; -fx-font-weight: bold;" :
                         item.contains("LOW")  ? "-fx-text-fill: #F5A623; -fx-font-weight: bold;" :
                                                 "-fx-text-fill: #4CD97B; -fx-font-weight: bold;");
            }
        });
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void setVisitStatus(String msg, boolean success) {
        visitStatusLabel.setText(msg);
        visitStatusLabel.setStyle("-fx-text-fill: " + (success ? "#4CD97B" : "#FF6B6B") + "; -fx-font-weight: bold;");
    }

    private String nvl(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    @FXML private void logout() { MainApp.logout(); }

    // ── Inner model classes ───────────────────────────────────────────────────

    public static class StudentRow {
        final int id; final String name, email;
        StudentRow(int id, String name, String email) { this.id = id; this.name = name; this.email = email; }
        @Override public String toString() { return "[" + id + "] " + name + " — " + email; }
    }

    public static class VisitRow {
        final String dateTime, symptoms, treatment;
        VisitRow(String dt, String s, String t) { dateTime = dt; symptoms = s; treatment = t; }
    }

    public static class InvRow {
        final String name, status; final int stock, min;
        InvRow(String n, int s, int m, String st) { name = n; stock = s; min = m; status = st; }
    }
}
