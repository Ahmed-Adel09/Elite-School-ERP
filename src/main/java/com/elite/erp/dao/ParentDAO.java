package com.elite.erp.dao;

import com.elite.erp.model.Parent;
import com.elite.erp.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent DAO — extends GenericRepository<Parent> (Inheritance + Generics).
 */
public class ParentDAO extends GenericRepository<Parent> {

    public ParentDAO() {
        super("parents");
    }

    @Override
    protected Parent mapRow(ResultSet rs) throws SQLException {
        Parent p = new Parent();
        p.setId(rs.getInt("id"));
        p.setApplicationId(rs.getInt("application_id"));
        p.setFatherName(rs.getString("father_name"));
        p.setMotherName(rs.getString("mother_name"));
        p.setEmail(rs.getString("email"));
        p.setPhone(rs.getString("phone"));
        p.setAddress(rs.getString("address"));
        p.setOccupation(rs.getString("occupation"));
        p.setCorporateSponsor(rs.getInt("corporate_sponsor") == 1);
        p.setCompanyName(rs.getString("company_name"));
        p.setCompanyEmail(rs.getString("company_email"));
        p.setSponsorAmount(rs.getDouble("sponsor_amount"));
        return p;
    }

    @Override
    public Response<Parent> save(Parent p) {
        logOp("INSERT");
        String sql = """
            INSERT INTO parents
            (application_id, father_name, mother_name, email, phone, address,
             occupation, corporate_sponsor, company_name, company_email, sponsor_amount)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1,  p.getApplicationId());
            ps.setString(2,  p.getFatherName());
            ps.setString(3,  p.getMotherName());
            ps.setString(4,  p.getEmail());
            ps.setString(5,  p.getPhone());
            ps.setString(6,  p.getAddress());
            ps.setString(7,  p.getOccupation());
            ps.setInt   (8,  p.isCorporateSponsor() ? 1 : 0);
            ps.setString(9,  p.getCompanyName());
            ps.setString(10, p.getCompanyEmail());
            ps.setDouble(11, p.getSponsorAmount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
            return Response.success(p, "Parent saved");
        } catch (SQLException e) {
            return Response.failure("Save parent failed: " + e.getMessage());
        }
    }

    @Override
    public Response<Parent> findById(int id) {
        logOp("SELECT by ID");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM parents WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Response.success(mapRow(rs));
                return Response.failure("Parent not found");
            }
        } catch (SQLException e) {
            return Response.failure("Find parent failed: " + e.getMessage());
        }
    }

    @Override
    public Response<List<Parent>> findAll() {
        logOp("SELECT ALL");
        List<Parent> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT * FROM parents")) {
            while (rs.next()) list.add(mapRow(rs));
            return Response.success(list);
        } catch (SQLException e) {
            return Response.failure("Find all parents failed: " + e.getMessage());
        }
    }

    @Override
    public Response<Boolean> update(Parent p) {
        logOp("UPDATE");
        String sql = "UPDATE parents SET father_name=?, mother_name=?, email=?, phone=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getFatherName());
            ps.setString(2, p.getMotherName());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getPhone());
            ps.setInt   (5, p.getId());
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) {
            return Response.failure("Update parent failed: " + e.getMessage());
        }
    }

    @Override
    public Response<Boolean> delete(int id) {
        logOp("DELETE");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM parents WHERE id=?")) {
            ps.setInt(1, id);
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) {
            return Response.failure("Delete parent failed: " + e.getMessage());
        }
    }
}
