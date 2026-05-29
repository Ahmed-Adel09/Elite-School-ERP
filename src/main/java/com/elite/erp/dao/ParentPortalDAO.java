package com.elite.erp.dao;

import com.elite.erp.model.Announcement;
import com.elite.erp.model.Grade;
import com.elite.erp.model.SchoolEvent;
import com.elite.erp.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParentPortalDAO {

    public Response<List<Grade>> getStudentGrades(int studentId) {
        String sql = "SELECT * FROM grades WHERE student_id = ?";
        List<Grade> grades = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    grades.add(new Grade(rs.getInt("id"), rs.getInt("student_id"), 
                                         rs.getString("subject"), rs.getDouble("score"), rs.getString("date")));
                }
            }
            return Response.success(grades);
        } catch (SQLException e) {
            return Response.failure("Failed to fetch grades: " + e.getMessage());
        }
    }

    public Response<Double> getCreditsBalance(int parentId) {
        String sql = "SELECT balance FROM credits WHERE parent_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Response.success(rs.getDouble("balance"));
                } else {
                    // Create account with 0 balance if it doesn't exist
                    return initializeCredits(parentId);
                }
            }
        } catch (SQLException e) {
            return Response.failure("Failed to fetch credits: " + e.getMessage());
        }
    }

    private Response<Double> initializeCredits(int parentId) {
        String sql = "INSERT INTO credits (parent_id, balance) VALUES (?, 0.0)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parentId);
            ps.executeUpdate();
            return Response.success(0.0);
        } catch (SQLException e) {
            return Response.failure(e.getMessage());
        }
    }

    public Response<Boolean> updateCredits(int parentId, double amount) {
        String sql = "UPDATE credits SET balance = balance + ? WHERE parent_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, parentId);
            int rows = ps.executeUpdate();
            return Response.success(rows > 0);
        } catch (SQLException e) {
            return Response.failure("Failed to update credits: " + e.getMessage());
        }
    }

    public Response<List<SchoolEvent>> getSchoolEvents() {
        String sql = "SELECT * FROM school_events";
        List<SchoolEvent> events = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                events.add(new SchoolEvent(rs.getInt("id"), rs.getString("name"), 
                                           rs.getDouble("cost"), rs.getString("date")));
            }
            return Response.success(events);
        } catch (SQLException e) {
            return Response.failure("Failed to fetch events: " + e.getMessage());
        }
    }

    public Response<List<Announcement>> getAnnouncements() {
        String sql = "SELECT * FROM announcements ORDER BY date DESC";
        List<Announcement> announcements = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                announcements.add(new Announcement(rs.getInt("id"), rs.getString("title"), 
                                                   rs.getString("content"), rs.getString("date")));
            }
            return Response.success(announcements);
        } catch (SQLException e) {
            return Response.failure("Failed to fetch announcements: " + e.getMessage());
        }
    }

    public Response<List<com.elite.erp.model.Student>> getStudentsByParentEmail(String email) {
        String sql = """
            SELECT s.* FROM students s
            JOIN parents p ON s.application_id = p.application_id
            WHERE p.email = ?
            """;
        List<com.elite.erp.model.Student> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.elite.erp.model.Student s = new com.elite.erp.model.Student();
                    s.setId(rs.getInt("id"));
                    s.setApplicationId(rs.getInt("application_id"));
                    s.setFirstName(rs.getString("first_name"));
                    s.setLastName(rs.getString("last_name"));
                    s.setApplyingForGrade(rs.getString("applying_for_grade"));
                    list.add(s);
                }
            }
            return Response.success(list);
        } catch (SQLException e) {
            return Response.failure("Failed to fetch linked students: " + e.getMessage());
        }
    }
}
