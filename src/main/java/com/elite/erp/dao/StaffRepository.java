package com.elite.erp.dao;

import com.elite.erp.model.staff.*;
import com.elite.erp.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * StaffRepository<T extends Staff> — Generic Repository pattern.
 *
 * This single class uses Java Generics to handle ALL staff types
 * (Teacher, Nurse, Accountant, Librarian) through a unified `staff_applications`
 * table with a `role_type` discriminator column and an `extra_data` TEXT column
 * for role-specific fields (serialised as key=value pairs).
 *
 * Demonstrates:
 *  - Generics: StaffRepository<T extends Staff>
 *  - Inheritance: Staff base class used as the type bound
 *  - JDBC: PreparedStatement, ResultSet, RETURN_GENERATED_KEYS
 */
public class StaffRepository<T extends Staff> {

    // ── Generic Save ─────────────────────────────────────────────────────────

    public Response<T> save(T staff) {
        String sql = """
            INSERT INTO staff_applications
              (full_name, email, phone, cv_path, role_type, extra_data, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, staff.getFullName());
            ps.setString(2, staff.getEmail());
            ps.setString(3, staff.getPhone());
            ps.setString(4, staff.getCvPath());
            ps.setString(5, staff.getRoleType());
            ps.setString(6, staff.serializeExtraData());
            ps.setString(7, staff.getStatus() != null ? staff.getStatus() : "PENDING");

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) staff.setId(keys.getInt(1));
            }
            return Response.success(staff, "Application submitted successfully.");
        } catch (SQLException e) {
            return Response.failure("Failed to save staff application: " + e.getMessage());
        }
    }

    // ── Generic FindAll — returns concrete subclass instances ─────────────────

    @SuppressWarnings("unchecked")
    public Response<List<Staff>> findAll() {
        String sql = "SELECT * FROM staff_applications ORDER BY id DESC";
        List<Staff> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Staff s = buildInstance(rs.getString("role_type"));
                if (s == null) continue;

                s.setId(rs.getInt("id"));
                s.setFullName(rs.getString("full_name"));
                s.setEmail(rs.getString("email"));
                s.setPhone(rs.getString("phone"));
                s.setCvPath(rs.getString("cv_path"));
                s.setRoleType(rs.getString("role_type"));
                s.setStatus(rs.getString("status"));
                s.deserializeExtraData(rs.getString("extra_data"));

                list.add(s);
            }
            return Response.success(list);
        } catch (SQLException e) {
            return Response.failure("Failed to fetch staff applications: " + e.getMessage());
        }
    }

    // ── FindAllPending ────────────────────────────────────────────────────────

    public Response<List<Staff>> findPending() {
        String sql = "SELECT * FROM staff_applications WHERE status='PENDING' ORDER BY id DESC";
        List<Staff> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Staff s = buildInstance(rs.getString("role_type"));
                if (s == null) continue;

                s.setId(rs.getInt("id"));
                s.setFullName(rs.getString("full_name"));
                s.setEmail(rs.getString("email"));
                s.setPhone(rs.getString("phone"));
                s.setCvPath(rs.getString("cv_path"));
                s.setRoleType(rs.getString("role_type"));
                s.setStatus(rs.getString("status"));
                s.deserializeExtraData(rs.getString("extra_data"));

                list.add(s);
            }
            return Response.success(list);
        } catch (SQLException e) {
            return Response.failure("Failed to fetch pending staff: " + e.getMessage());
        }
    }

    // ── UpdateStatus ─────────────────────────────────────────────────────────

    public Response<Boolean> updateStatus(int id, String newStatus) {
        String sql = "UPDATE staff_applications SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) {
            return Response.failure("Failed to update status: " + e.getMessage());
        }
    }

    // ── Factory: instantiate correct subclass from roleType string ────────────

    private Staff buildInstance(String roleType) {
        if (roleType == null) return null;
        return switch (roleType.toUpperCase()) {
            case "TEACHER"    -> new Teacher();
            case "NURSE"      -> new Nurse();
            case "ACCOUNTANT" -> new Accountant();
            case "LIBRARIAN"  -> new Librarian();
            default           -> null;
        };
    }
}
