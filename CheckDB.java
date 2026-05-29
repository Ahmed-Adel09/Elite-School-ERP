import java.sql.*;

public class CheckDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/elite_erp.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("--- USERS TABLE ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
                while (rs.next()) {
                    System.out.println("ID=" + rs.getInt("id") + " | User=" + rs.getString("username") +
                                       " | Pass=" + rs.getString("password") + " | Role=" + rs.getString("role") +
                                       " | Email=" + rs.getString("email"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
