package com.elite.erp.dao;

import com.elite.erp.model.TeacherApplication;
import com.elite.erp.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeacherDAO {

    public Response<TeacherApplication> save(TeacherApplication app) {
        String sql = """
            INSERT INTO teacher_applications (full_name, email, phone, subject, experience_years, cv_path, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, app.getFullName());
            ps.setString(2, app.getEmail());
            ps.setString(3, app.getPhone());
            ps.setString(4, app.getSubject());
            ps.setInt(5, app.getExperienceYears());
            ps.setString(6, app.getCvPath());
            ps.setString(7, app.getStatus() != null ? app.getStatus() : "PENDING");
            
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    app.setId(keys.getInt(1));
                }
            }
            return Response.success(app, "Application submitted successfully.");
        } catch (SQLException e) {
            return Response.failure("Failed to submit teacher application: " + e.getMessage());
        }
    }

    public Response<List<TeacherApplication>> findAll() {
        String sql = "SELECT * FROM teacher_applications ORDER BY id DESC";
        List<TeacherApplication> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                TeacherApplication app = new TeacherApplication();
                app.setId(rs.getInt("id"));
                app.setFullName(rs.getString("full_name"));
                app.setEmail(rs.getString("email"));
                app.setPhone(rs.getString("phone"));
                app.setSubject(rs.getString("subject"));
                app.setExperienceYears(rs.getInt("experience_years"));
                app.setCvPath(rs.getString("cv_path"));
                app.setStatus(rs.getString("status"));
                list.add(app);
            }
            return Response.success(list);
        } catch (SQLException e) {
            return Response.failure("Failed to fetch applications: " + e.getMessage());
        }
    }

    public Response<Boolean> updateStatus(int id, String newStatus) {
        String sql = "UPDATE teacher_applications SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) {
            return Response.failure("Failed to update status: " + e.getMessage());
        }
    }
}
