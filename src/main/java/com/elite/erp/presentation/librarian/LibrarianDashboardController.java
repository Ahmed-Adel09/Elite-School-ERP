package com.elite.erp.presentation.librarian;

import com.elite.erp.MainApp;
import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.fxgl.LibraryFXGLChart;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class LibrarianDashboardController implements Initializable {

    // Inventory Tab
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField categoryField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField stockField;
    @FXML private Label filePathLabel;
    @FXML private TableView<BookModel> booksTable;
    @FXML private TableColumn<BookModel, Number> colId;
    @FXML private TableColumn<BookModel, String> colTitle;
    @FXML private TableColumn<BookModel, String> colAuthor;
    @FXML private TableColumn<BookModel, String> colCat;
    @FXML private TableColumn<BookModel, String> colType;
    @FXML private TableColumn<BookModel, String> colStock;

    // Loan Desk Tab
    @FXML private TableView<LoanModel> loansTable;
    @FXML private TableColumn<LoanModel, Number> colLoanId;
    @FXML private TableColumn<LoanModel, String> colBookTitle;
    @FXML private TableColumn<LoanModel, String> colStudent;
    @FXML private TableColumn<LoanModel, String> colReqDate;
    @FXML private TableColumn<LoanModel, String> colStatus;

    // Analytics Tab
    @FXML private StackPane fxglChartPane;

    private String selectedFilePath = "";
    private final ObservableList<BookModel> bookList = FXCollections.observableArrayList();
    private final ObservableList<LoanModel> loanList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeCombo.getItems().addAll("PHYSICAL", "DIGITAL");
        typeCombo.getSelectionModel().selectFirst();

        setupInventoryTable();
        setupLoanTable();
        
        loadBooks();
        loadPendingLoans();

        // Embed FXGL
        LibraryFXGLChart.getInstance().embedInto(fxglChartPane);

        // Start Overdue Scanner
        startOverdueScanner();
    }

    private void setupInventoryTable() {
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id));
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title));
        colAuthor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().author));
        colCat.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().category));
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type));
        colStock.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().availableStock + " / " + data.getValue().totalStock));
        booksTable.setItems(bookList);
    }

    private void setupLoanTable() {
        colLoanId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id));
        colBookTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bookTitle));
        colStudent.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().studentName));
        colReqDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().reqDate));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
        loansTable.setItems(loanList);
    }

    @FXML
    private void browseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Digital Book File");
        File file = chooser.showOpenDialog(MainApp.getPrimaryStage());
        if (file != null) {
            selectedFilePath = file.getAbsolutePath();
            filePathLabel.setText("✅ " + file.getName());
        }
    }

    @FXML
    private void addBook() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO library_books (title, author, category, type, file_path, total_stock, available_stock) VALUES (?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setString(1, titleField.getText());
            ps.setString(2, authorField.getText());
            ps.setString(3, categoryField.getText());
            ps.setString(4, typeCombo.getValue());
            ps.setString(5, selectedFilePath);
            
            int stock = 0;
            if ("PHYSICAL".equals(typeCombo.getValue())) {
                try { stock = Integer.parseInt(stockField.getText()); } catch (Exception ignored) {}
            }
            ps.setInt(6, stock);
            ps.setInt(7, stock); // initial available == total
            
            ps.executeUpdate();
            
            titleField.clear(); authorField.clear(); categoryField.clear(); stockField.clear();
            selectedFilePath = ""; filePathLabel.setText("No file selected");
            
            loadBooks();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void deleteBook() {
        BookModel selected = booksTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM library_books WHERE id = ?");
            ps.setInt(1, selected.id);
            ps.executeUpdate();
            loadBooks();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadBooks() {
        bookList.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM library_books ORDER BY id DESC");
            while (rs.next()) {
                BookModel b = new BookModel();
                b.id = rs.getInt("id");
                b.title = rs.getString("title");
                b.author = rs.getString("author");
                b.category = rs.getString("category");
                b.type = rs.getString("type");
                b.totalStock = rs.getInt("total_stock");
                b.availableStock = rs.getInt("available_stock");
                bookList.add(b);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadPendingLoans() {
        loanList.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT bl.id, lb.title, u.full_name, bl.request_date, bl.status " +
                "FROM book_loans bl " +
                "JOIN library_books lb ON bl.book_id = lb.id " +
                "JOIN users u ON bl.student_id = u.id " +
                "WHERE bl.status = 'PENDING'"
            );
            while (rs.next()) {
                LoanModel lm = new LoanModel();
                lm.id = rs.getInt("id");
                lm.bookTitle = rs.getString("title");
                lm.studentName = rs.getString("full_name");
                lm.reqDate = rs.getString("request_date");
                lm.status = rs.getString("status");
                loanList.add(lm);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void approveLoan() {
        LoanModel selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE book_loans SET status = 'BORROWED' WHERE id = ?");
            ps.setInt(1, selected.id);
            ps.executeUpdate();
            loadPendingLoans();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Loan Approved! Status is now BORROWED.", ButtonType.OK);
            alert.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void startOverdueScanner() {
        Thread scanner = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // Check every 10 seconds for testing
                    try (Connection conn = DatabaseManager.getConnection()) {
                        ResultSet rs = conn.createStatement().executeQuery(
                            "SELECT bl.id, u.full_name, lb.title " +
                            "FROM book_loans bl " +
                            "JOIN users u ON bl.student_id = u.id " +
                            "JOIN library_books lb ON bl.book_id = lb.id " +
                            "WHERE bl.status = 'BORROWED' AND bl.due_date < datetime('now')"
                        );
                        while (rs.next()) {
                            int loanId = rs.getInt("id");
                            String student = rs.getString("full_name");
                            String title = rs.getString("title");
                            
                            // Send overdue socket message
                            sendOverdueBroadcast(student, title);
                            
                            // Mark as OVERDUE in DB to avoid spamming
                            PreparedStatement ps = conn.prepareStatement("UPDATE book_loans SET status = 'OVERDUE' WHERE id = ?");
                            ps.setInt(1, loanId);
                            ps.executeUpdate();
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        scanner.setDaemon(true);
        scanner.start();
    }

    private void sendOverdueBroadcast(String student, String title) {
        try (Socket socket = new Socket("localhost", 9997);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            out.println("OVERDUE|Alert for " + student + ": Your physical book '" + title + "' is overdue!");
        } catch (Exception ignored) {}
    }

    @FXML
    private void logout() {
        MainApp.logout();
    }

    // Models for TableView
    public static class BookModel {
        int id, totalStock, availableStock;
        String title, author, category, type;
    }
    public static class LoanModel {
        int id;
        String bookTitle, studentName, reqDate, status;
    }
}
