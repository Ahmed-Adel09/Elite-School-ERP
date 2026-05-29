import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import java.io.File;
import java.net.URL;

public class TestLoad {
    public static void main(String[] args) {
        Platform.startup(() -> {
            try {
                URL url1 = new File("src/main/resources/com/elite/erp/fxml/InstaPayStep.fxml").toURI().toURL();
                FXMLLoader loader1 = new FXMLLoader(url1);
                loader1.load();
                System.out.println("InstaPayStep.fxml loaded successfully!");
            } catch (Exception e) {
                System.err.println("InstaPayStep.fxml ERROR:");
                e.printStackTrace();
                Throwable cause = e.getCause();
                while(cause != null) {
                    System.err.println("Caused by: " + cause);
                    cause = cause.getCause();
                }
            }

            try {
                URL url2 = new File("src/main/resources/com/elite/erp/fxml/CreditCardView.fxml").toURI().toURL();
                FXMLLoader loader2 = new FXMLLoader(url2);
                loader2.load();
                System.out.println("CreditCardView.fxml loaded successfully!");
            } catch (Exception e) {
                System.err.println("CreditCardView.fxml ERROR:");
                e.printStackTrace();
                Throwable cause = e.getCause();
                while(cause != null) {
                    System.err.println("Caused by: " + cause);
                    cause = cause.getCause();
                }
            }
            System.exit(0);
        });
    }
}
