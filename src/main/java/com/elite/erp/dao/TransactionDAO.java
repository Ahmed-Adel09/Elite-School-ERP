package com.elite.erp.dao;

import com.elite.erp.model.Transaction;
import com.elite.erp.util.Response;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionDAO — extends GenericRepository<Transaction>.
 * Stores InstaPay payment references and verification results in SQLite.
 */
public class TransactionDAO extends GenericRepository<Transaction> {

    public TransactionDAO() { super("transactions"); }

    @Override
    protected Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("id"));
        t.setApplicationId(rs.getInt("application_id"));
        t.setIpaReference(rs.getString("ipa_reference"));
        t.setAmount(rs.getDouble("amount"));
        t.setStatus(rs.getString("status"));
        t.setVerifiedAt(rs.getString("verified_at"));
        return t;
    }

    @Override
    public Response<Transaction> save(Transaction t) {
        logOp("INSERT");
        String sql = """
            INSERT INTO transactions (application_id, ipa_reference, amount, status, verified_at)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, t.getApplicationId());
            ps.setString(2, t.getIpaReference());
            ps.setDouble(3, t.getAmount());
            ps.setString(4, t.getStatus());
            ps.setString(5, t.getVerifiedAt() != null ? t.getVerifiedAt()
                            : LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) t.setId(keys.getInt(1));
            }
            return Response.success(t, "Transaction saved");
        } catch (SQLException e) {
            return Response.failure("Save transaction failed: " + e.getMessage());
        }
    }

    @Override
    public Response<Transaction> findById(int id) {
        logOp("SELECT by ID");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM transactions WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Response.success(mapRow(rs));
                return Response.failure("Transaction not found");
            }
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    @Override
    public Response<List<Transaction>> findAll() {
        logOp("SELECT ALL");
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM transactions ORDER BY id DESC")) {
            while (rs.next()) list.add(mapRow(rs));
            return Response.success(list);
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    @Override
    public Response<Boolean> update(Transaction t) {
        logOp("UPDATE");
        String sql = "UPDATE transactions SET status=?, verified_at=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getStatus());
            ps.setString(2, LocalDateTime.now().toString());
            ps.setInt   (3, t.getId());
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    @Override
    public Response<Boolean> delete(int id) {
        logOp("DELETE");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE id=?")) {
            ps.setInt(1, id);
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    /** Get verified transaction by application ID */
    public Response<Transaction> findByApplicationId(int appId) {
        logOp("SELECT by APP ID");
        String sql = "SELECT * FROM transactions WHERE application_id=? AND status='VERIFIED' LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Response.success(mapRow(rs));
                return Response.failure("No verified transaction for app #" + appId);
            }
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }
}
