package com.elite.erp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * StudentRegistrationScreen — Programmatic JavaFX Registration Form.
 * Built using pure Java code (No FXML).
 */
public class StudentRegistrationScreen extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f4f7f6;");

        // ─── HEADER ───
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 40, 20, 40));
        header.setStyle("-fx-background-color: #001f3f;");
        
        Label title = new Label("STUDENT ADMISSION FORM");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #FFD700;");
        header.getChildren().add(title);

        // ─── FORM CONTAINER ───
        VBox form = new VBox(25);
        form.setPadding(new Insets(40, 60, 40, 60));
        form.setAlignment(Pos.TOP_LEFT);
        form.setMaxWidth(800);
        
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(20);

        // Styling for input fields
        String fieldStyle = "-fx-background-radius: 5; -fx-border-color: #ced4da; -fx-border-radius: 5; -fx-padding: 8;";

        // --- Personal Information ---
        grid.add(createLabel("Full Name:"), 0, 0);
        TextField nameField = new TextField();
        nameField.setPromptText("Enter student's full name");
        nameField.setStyle(fieldStyle);
        grid.add(nameField, 1, 0);

        grid.add(createLabel("Date of Birth:"), 0, 1);
        DatePicker dobPicker = new DatePicker();
        dobPicker.setMaxWidth(Double.MAX_VALUE);
        dobPicker.setStyle(fieldStyle);
        grid.add(dobPicker, 1, 1);

        grid.add(createLabel("Gender:"), 0, 2);
        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("Male", "Female", "Other");
        genderBox.setMaxWidth(Double.MAX_VALUE);
        genderBox.setStyle(fieldStyle);
        grid.add(genderBox, 1, 2);

        grid.add(createLabel("Email Address:"), 0, 3);
        TextField emailField = new TextField();
        emailField.setPromptText("example@school.com");
        emailField.setStyle(fieldStyle);
        grid.add(emailField, 1, 3);

        grid.add(createLabel("Grade/Class:"), 0, 4);
        ComboBox<String> gradeBox = new ComboBox<>();
        gradeBox.getItems().addAll("Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6");
        gradeBox.setMaxWidth(Double.MAX_VALUE);
        gradeBox.setStyle(fieldStyle);
        grid.add(gradeBox, 1, 4);

        // --- Parent Information ---
        grid.add(createLabel("Parent/Guardian:"), 0, 5);
        TextField parentField = new TextField();
        parentField.setStyle(fieldStyle);
        grid.add(parentField, 1, 5);

        // ─── BUTTONS ───
        HBox btnBox = new HBox(20);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(20, 0, 0, 0));

        Button btnCancel = new Button("CANCEL");
        btnCancel.setPrefSize(120, 40);
        btnCancel.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Button btnSubmit = new Button("SUBMIT ADMISSION");
        btnSubmit.setPrefSize(200, 40);
        btnSubmit.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        
        btnSubmit.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Student Application for " + nameField.getText() + " has been submitted!");
            alert.showAndWait();
        });

        btnBox.getChildren().addAll(btnCancel, btnSubmit);

        form.getChildren().addAll(grid, btnBox);

        // Center the form
        StackPane centerContainer = new StackPane(form);
        centerContainer.setPadding(new Insets(20));

        root.getChildren().addAll(header, centerContainer);

        // ─── STAGE CONFIG ───
        Scene scene = new Scene(root, 900, 700);
        primaryStage.setTitle("Vertex Academy — Student Registration");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lbl.setStyle("-fx-text-fill: #34495e;");
        return lbl;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
