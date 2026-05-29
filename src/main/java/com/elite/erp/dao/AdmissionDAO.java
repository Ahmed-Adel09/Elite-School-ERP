package com.elite.erp.dao;

import com.elite.erp.model.AdmissionApplication;
import com.elite.erp.model.ApplicationStatus;
import com.elite.erp.util.Response;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AdmissionDAO — extends GenericRepository<AdmissionApplication>.
 * Demonstrates Inheritance and JDBC usage.
 */
public class AdmissionDAO extends GenericRepository<AdmissionApplication> {

    public AdmissionDAO() {
        super("applications");
    }

    @Override
    protected AdmissionApplication mapRow(ResultSet rs) throws SQLException {
        AdmissionApplication app = new AdmissionApplication();
        app.setId(rs.getInt("id"));
        app.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        String dateStr = rs.getString("submission_date");
        if (dateStr != null) app.setSubmissionDate(LocalDate.parse(dateStr));
        app.setPaymentAmount(rs.getDouble("payment_amount"));
        app.setPaymentCompleted(rs.getInt("payment_completed") == 1);
        app.setLanguage(rs.getString("language"));
        return app;
    }

    @Override
    public Response<AdmissionApplication> save(AdmissionApplication app) {
        logOp("INSERT");
        String sql = """
            INSERT INTO applications (status, submission_date, payment_amount,
                                      payment_completed, language)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, app.getStatus().name());
            ps.setString(2, app.getSubmissionDate().toString());
            ps.setDouble(3, app.getPaymentAmount());
            ps.setInt   (4, app.isPaymentCompleted() ? 1 : 0);
            ps.setString(5, app.getLanguage());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) app.setId(keys.getInt(1));
            }
            return Response.success(app, "Application saved with ID " + app.getId());
        } catch (SQLException e) {
            return Response.failure("Save application failed: " + e.getMessage());
        }
    }

    @Override
    public Response<AdmissionApplication> findById(int id) {
        logOp("SELECT by ID");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM applications WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Response.success(mapRow(rs));
                return Response.failure("Application #" + id + " not found");
            }
        } catch (SQLException e) {
            return Response.failure("Find failed: " + e.getMessage());
        }
    }

    @Override
    public Response<List<AdmissionApplication>> findAll() {
        logOp("SELECT ALL");
        List<AdmissionApplication> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT * FROM applications ORDER BY id DESC")) {
            while (rs.next()) list.add(mapRow(rs));
            return Response.success(list, list.size() + " applications");
        } catch (SQLException e) {
            return Response.failure("Find all failed: " + e.getMessage());
        }
    }

    @Override
    public Response<Boolean> update(AdmissionApplication app) {
        logOp("UPDATE");
        String sql = "UPDATE applications SET status=?, payment_completed=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, app.getStatus().name());
            ps.setInt   (2, app.isPaymentCompleted() ? 1 : 0);
            ps.setInt   (3, app.getId());
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) {
            return Response.failure("Update failed: " + e.getMessage());
        }
    }

    @Override
    public Response<Boolean> delete(int id) {
        logOp("DELETE");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM applications WHERE id=?")) {
            ps.setInt(1, id);
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) {
            return Response.failure("Delete failed: " + e.getMessage());
        }
    }

    /** Fetch monthly enrollment stats for FXGL chart */
    public Response<Map<String, Integer>> getMonthlyEnrollmentStats() {
        logOp("ENROLLMENT STATS");
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = "SELECT month, count FROM enrollment_stats WHERE year=2025 ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("month"), rs.getInt("count"));
            }
            return Response.success(stats);
        } catch (SQLException e) {
            return Response.failure("Stats query failed: " + e.getMessage());
        }
    }

    /** Increment this month's enrollment count when a new application is submitted */
    public void incrementCurrentMonthCount() {
        String month = LocalDate.now().getMonth().getDisplayName(
                java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
        int year = LocalDate.now().getYear();
        String update = "UPDATE enrollment_stats SET count=count+1 WHERE month=? AND year=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, month);
            ps.setInt   (2, year);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DAO] incrementCurrentMonthCount failed: " + e.getMessage());
        }
    }

    /**
     * Sets needs_password_setup = 1 for the approved application.
     * Called after emails are sent successfully (from the Task.setOnSucceeded callback).
     */
    public Response<Boolean> markNeedsPasswordSetup(int applicationId) {
        logOp("MARK PASSWORD SETUP");
        String sql = """
            UPDATE applications
            SET status = 'APPROVED', needs_password_setup = 1
            WHERE id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            return Response.success(ps.executeUpdate() > 0,
                    "App #" + applicationId + " marked APPROVED + needs_password_setup");
        } catch (SQLException e) {
            return Response.failure("markNeedsPasswordSetup failed: " + e.getMessage());
        }
    }

    public Response<com.elite.erp.model.Parent> getParentByEmail(String email) {
        String sql = "SELECT * FROM parents WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    com.elite.erp.model.Parent p = new com.elite.erp.model.Parent();
                    p.setId(rs.getInt("id"));
                    p.setApplicationId(rs.getInt("application_id"));
                    p.setFatherName(rs.getString("father_name"));
                    p.setMotherName(rs.getString("mother_name"));
                    p.setEmail(rs.getString("email"));
                    p.setPhone(rs.getString("phone"));
                    return Response.success(p);
                }
                return Response.failure("Parent not found for email: " + email);
            }
        } catch (SQLException e) {
            return Response.failure(e.getMessage());
        }
    }

    public Response<com.elite.erp.model.Student> getStudentByApplicationId(int appId) {
        String sql = "SELECT * FROM students WHERE application_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    com.elite.erp.model.Student s = new com.elite.erp.model.Student();
                    s.setId(rs.getInt("id"));
                    s.setApplicationId(rs.getInt("application_id"));
                    s.setFirstName(rs.getString("first_name"));
                    s.setLastName(rs.getString("last_name"));
                    s.setApplyingForGrade(rs.getString("applying_for_grade"));
                    return Response.success(s);
                }
                return Response.failure("Student not found for application: " + appId);
            }
        } catch (SQLException e) {
            return Response.failure(e.getMessage());
        }
    }
}
