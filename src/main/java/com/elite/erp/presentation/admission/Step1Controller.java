package com.elite.erp.presentation.admission;

import com.elite.erp.MainApp;
import com.elite.erp.model.AdmissionApplication;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Step1Controller — Landing & Roadmap step of the Admission Wizard.
 * Shows hero banner, visual timeline (Form→Pay→Exam→Decision),
 * and a language localization toggle (EN / AR).
 */
public class Step1Controller implements Initializable {

    @FXML private Label  welcomeTitle;
    @FXML private Label  welcomeSubtitle;
    @FXML private Label  step1Label;
    @FXML private Label  step2Label;
    @FXML private Label  step3Label;
    @FXML private Label  step4Label;
    @FXML private ToggleButton langToggle;
    @FXML private VBox   heroPane;

    private static final String[][] LABELS = {
        // EN
        {"Apply to Elite International School",
         "Begin your journey to academic excellence",
         "📋 Form", "💳 Pay", "📝 Exam", "✅ Decision"},
        // AR
        {"التقديم في مدرسة النخبة الدولية",
         "ابدأ رحلتك نحو التميز الأكاديمي",
         "📋 نموذج", "💳 دفع", "📝 اختبار", "✅ قرار"}
    };

    private int langIndex = 0; // 0=EN, 1=AR

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        applyLanguage();

        FadeTransition ft = new FadeTransition(Duration.millis(700), heroPane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    @FXML
    private void toggleLanguage() {
        langIndex = langIndex == 0 ? 1 : 0;
        langToggle.setText(langIndex == 0 ? "🌐 EN | AR" : "🌐 AR | EN");
        applyLanguage();

        // Save language choice to shared application model
        AdmissionApplication app = MainApp.getCurrentApplication();
        app.setLanguage(langIndex == 0 ? "EN" : "AR");
    }

    private void applyLanguage() {
        String[] lbl = LABELS[langIndex];
        welcomeTitle.setText(lbl[0]);
        welcomeSubtitle.setText(lbl[1]);
        step1Label.setText(lbl[2]);
        step2Label.setText(lbl[3]);
        step3Label.setText(lbl[4]);
        step4Label.setText(lbl[5]);

        boolean rtl = langIndex == 1;
        welcomeTitle.setNodeOrientation(rtl
            ? javafx.geometry.NodeOrientation.RIGHT_TO_LEFT
            : javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
    }

    @FXML
    private void goNext() {
        MainApp.switchScene("/com/elite/erp/fxml/Step2.fxml");
    }

    @FXML
    private void goBack() {
        MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
    }
}
