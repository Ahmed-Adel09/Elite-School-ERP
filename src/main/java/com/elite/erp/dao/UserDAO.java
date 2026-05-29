package com.elite.erp.dao;

import com.elite.erp.model.User;
import com.elite.erp.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO — extends GenericRepository<User>.
 * Handles authentication and user management.
 */
public class UserDAO extends GenericRepository<User> {

    public UserDAO() { super("users"); }

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        return u;
    }

    /** Authenticate a user by email + password */
    public Response<User> authenticate(String email, String password) {
        logOp("AUTHENTICATE");
        String sql = "SELECT * FROM users WHERE email=? AND password=? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Response.success(mapRow(rs), "Login successful");
                return Response.failure("Invalid email or password.", "AUTH_FAIL");
            }
        } catch (SQLException e) {
            return Response.failure("Authentication error: " + e.getMessage());
        }
    }

    /**
     * Check if a numeric application ID belongs to an APPROVED application
     * still needing password setup. Student types their Application ID in the
     * login email field on first login.
     */
    public Response<Boolean> checkInitialLogin(String appIdInput) {
        try {
            int appId = Integer.parseInt(appIdInput.trim());
            String sql = "SELECT needs_password_setup FROM applications WHERE id = ? AND status = 'APPROVED'";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, appId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt("needs_password_setup") == 1) {
                        return Response.success(true, "Redirect to Password Setup");
                    }
                }
            }
        } catch (NumberFormatException ignored) {
            // Not a numeric ID — check if it's a student_email with pending setup
            try {
                String sql = "SELECT a.needs_password_setup FROM applications a "
                    + "JOIN students s ON s.application_id = a.id "
                    + "WHERE s.student_email = ? AND a.status = 'APPROVED'";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, appIdInput.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt("needs_password_setup") == 1) {
                            return Response.success(true, "Redirect to Password Setup");
                        }
                    }
                }
            } catch (SQLException e2) {
                return Response.failure("DB Error: " + e2.getMessage());
            }
        } catch (SQLException e) {
            return Response.failure("DB Error: " + e.getMessage());
        }
        return Response.failure("Initial login check failed.");
    }

    /**
     * After successful authentication, check if the student's application still
     * has needs_password_setup = 1. Used as a second gate in LoginController.
     */
    public boolean studentNeedsPasswordSetup(String email) {
        String sql = "SELECT a.needs_password_setup FROM applications a "
            + "JOIN students s ON s.application_id = a.id "
            + "WHERE s.student_email = ? AND a.status = 'APPROVED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("needs_password_setup") == 1;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] studentNeedsPasswordSetup error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Response<User> save(User u) {
        logOp("INSERT");
        String sql = "INSERT INTO users (username, password, role, full_name, email) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getRole());
            ps.setString(4, u.getFullName());
            ps.setString(5, u.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) u.setId(keys.getInt(1)); }
            return Response.success(u, "User created");
        } catch (SQLException e) { return Response.failure("Save user failed: " + e.getMessage()); }
    }

    @Override public Response<User> findById(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Response.success(mapRow(rs));
                return Response.failure("User not found");
            }
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    @Override public Response<List<User>> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
            while (rs.next()) list.add(mapRow(rs));
            return Response.success(list);
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    @Override public Response<Boolean> update(User u) {
        String sql = "UPDATE users SET full_name=?, email=?, role=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getFullName()); ps.setString(2, u.getEmail());
            ps.setString(3, u.getRole());     ps.setInt   (4, u.getId());
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    @Override public Response<Boolean> delete(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, id);
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    /** 
     * Creates a pending user account with a NULL password. 
     * The user must set their password later via the Set Password screen.
     */
    public Response<User> createPendingUser(String role, String email, String fullName) {
        logOp("CREATE PENDING USER");
        // We set username = email for backwards compatibility with unique constraint
        String insertSql = "INSERT OR IGNORE INTO users (username, password, role, full_name, email) VALUES (?, NULL, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, role);
            ps.setString(3, fullName);
            ps.setString(4, email);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                // Brand-new user inserted
                User u = new User();
                u.setEmail(email);
                u.setRole(role);
                u.setFullName(fullName);
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) u.setId(keys.getInt(1)); }
                return Response.success(u, "Pending user created");
            }

            // ── User already exists (INSERT OR IGNORE was skipped) ──────────────
            // This happens when HR re-approves an application that was previously
            // approved (e.g. testing). Reset the password to NULL so the student
            // can go through the Set Password flow again.
            String resetSql = "UPDATE users SET password = NULL, role = ?, full_name = ? WHERE email = ?";
            try (PreparedStatement ps2 = conn.prepareStatement(resetSql)) {
                ps2.setString(1, role);
                ps2.setString(2, fullName);
                ps2.setString(3, email);
                ps2.executeUpdate();
            }
            User u = new User();
            u.setEmail(email);
            u.setRole(role);
            u.setFullName(fullName);
            return Response.success(u, "Existing user reset to pending");
        } catch (SQLException e) { return Response.failure("Create pending failed: " + e.getMessage()); }
    }

    /** 
     * Sets the password for a user ONLY if their current password is NULL.
     */
    public Response<Boolean> setPassword(String email, String newPassword) {
        logOp("SET PASSWORD");
        String sql = "UPDATE users SET password=? WHERE email=? AND password IS NULL";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, email);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                // ── Critical fix: clear the needs_password_setup flag so that
                // LoginController Gate 1 no longer redirects this student back
                // to the Set Password screen on their next login attempt.
                String clearFlag = "UPDATE applications SET needs_password_setup = 0 " +
                                   "WHERE id = (SELECT application_id FROM students WHERE student_email = ?)";
                try (PreparedStatement ps2 = conn.prepareStatement(clearFlag)) {
                    ps2.setString(1, email);
                    ps2.executeUpdate();
                } catch (SQLException ignored) {
                    // Non-fatal: flag may not exist for non-student roles
                }
                return Response.success(true, "Password set successfully.");
            }
            // Differentiate between "email not found" and "password already set"
            return Response.failure("Email not found or password already set.", "SET_PASS_FAIL");
        } catch (SQLException e) { return Response.failure("Set password failed: " + e.getMessage()); }
    }
}
