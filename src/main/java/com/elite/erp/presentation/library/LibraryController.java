package com.elite.erp.presentation.library;

import com.elite.erp.MainApp;
import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.awt.Desktop;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class LibraryController implements Initializable {

    @FXML private GridPane booksGrid;
    @FXML private Label statusLabel;

    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = MainApp.getCurrentUser();
        loadLibraryBooks();
    }

    @FXML
    private void loadLibraryBooks() {
        booksGrid.getChildren().clear();
        int col = 0;
        int row = 0;

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM library_books ORDER BY id DESC");
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                String category = rs.getString("category");
                String type = rs.getString("type");
                String filePath = rs.getString("file_path");
                int availableStock = rs.getInt("available_stock");

                VBox bookCard = createBookCard(id, title, author, category, type, filePath, availableStock);
                booksGrid.add(bookCard, col, row);
                
                col++;
                if (col == 4) { // 4 books per row
                    col = 0;
                    row++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("❌ Failed to load books.", false);
        }
    }

    private VBox createBookCard(int id, String title, String author, String category, String type, String filePath, int stock) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #1A3560; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #C9A84C; -fx-border-radius: 8;");
        card.setPrefWidth(220);
        card.setAlignment(Pos.TOP_CENTER);

        // Thumbnail logic: If we have a cover image in same folder or just a placeholder
        // For simplicity, we use a placeholder icon if no direct cover exists
        ImageView iv = new ImageView();
        iv.setFitWidth(150);
        iv.setFitHeight(200);
        iv.setPreserveRatio(true);
        try {
            // Attempt to load a default cover or from a path (if available)
            // As per instructions, "Use setPreserveRatio(true) for book cover thumbnails"
            iv.setImage(new Image(getClass().getResourceAsStream("/com/elite/erp/images/default_book.png")));
        } catch (Exception e) {
            // Fallback if image not found
        }
        
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
        lblTitle.setWrapText(true);
        lblTitle.setAlignment(Pos.CENTER);
        
        Label lblAuthor = new Label("By: " + author);
        lblAuthor.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        
        Label lblCat = new Label(category);
        lblCat.setStyle("-fx-text-fill: #C9A84C; -fx-font-size: 11px;");

        card.getChildren().addAll(iv, lblTitle, lblAuthor, lblCat);

        if ("DIGITAL".equalsIgnoreCase(type)) {
            Button readBtn = new Button("📖 Read Now");
            readBtn.setStyle("-fx-background-color: #4CD97B; -fx-text-fill: #0A1628; -fx-font-weight: bold; -fx-cursor: hand;");
            readBtn.setMaxWidth(Double.MAX_VALUE);
            readBtn.setOnAction(e -> openDigitalBook(filePath));
            card.getChildren().add(readBtn);
        } else {
            Label lblStock = new Label("Stock: " + stock);
            lblStock.setStyle("-fx-text-fill: " + (stock > 0 ? "#4CD97B" : "#FF6B6B") + "; -fx-font-size: 12px;");
            
            Button reserveBtn = new Button("🔖 Reserve (Physical)");
            reserveBtn.setStyle("-fx-background-color: #F5A623; -fx-text-fill: #0A1628; -fx-font-weight: bold; -fx-cursor: hand;");
            reserveBtn.setMaxWidth(Double.MAX_VALUE);
            reserveBtn.setDisable(stock <= 0);
            reserveBtn.setOnAction(e -> reservePhysicalBook(id, title));
            
            card.getChildren().addAll(lblStock, reserveBtn);
        }

        return card;
    }

    private void openDigitalBook(String path) {
        if (path == null || path.isEmpty()) {
            setStatus("❌ No file path provided.", false);
            return;
        }
        try {
            File f = new File(path);
            if (f.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f);
                setStatus("✅ Opening book...", true);
            } else {
                setStatus("❌ File not found.", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("❌ Error opening file.", false);
        }
    }

    private void reservePhysicalBook(int bookId, String bookTitle) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Check stock again
            PreparedStatement chk = conn.prepareStatement("SELECT available_stock FROM library_books WHERE id = ?");
            chk.setInt(1, bookId);
            ResultSet rs = chk.executeQuery();
            if (rs.next() && rs.getInt("available_stock") > 0) {
                // 2. Reduce stock
                PreparedStatement upd = conn.prepareStatement("UPDATE library_books SET available_stock = available_stock - 1 WHERE id = ?");
                upd.setInt(1, bookId);
                upd.executeUpdate();
                
                // 3. Create Loan Record
                PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO book_loans (book_id, student_id, due_date, status) VALUES (?, ?, datetime('now', '+14 days'), 'PENDING')"
                );
                ins.setInt(1, bookId);
                ins.setInt(2, currentUser.getId());
                ins.executeUpdate();
                
                conn.commit();
                
                setStatus("✅ Reservation successful! Please collect from the Library.", true);
                
                // Send Socket Reservation Request to Librarian
                sendSocketReservationAlert(bookTitle);
                
                // Refresh UI
                loadLibraryBooks();
            } else {
                setStatus("❌ Out of stock.", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("❌ Database error during reservation.", false);
        }
    }

    private void sendSocketReservationAlert(String title) {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 9997);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
                out.println("RESERVE|User " + currentUser.getFullName() + " requested physical copy of '" + title + "'.");
            } catch (Exception ignored) {
                // Librarian server might be offline, which is fine
            }
        }).start();
    }

    private void setStatus(String msg, boolean success) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill: " + (success ? "#4CD97B" : "#FF6B6B") + "; -fx-font-weight: bold;");
        });
    }
}
