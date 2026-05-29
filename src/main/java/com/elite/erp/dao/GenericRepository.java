package com.elite.erp.dao;

import com.elite.erp.util.Response;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Abstract generic repository — demonstrates Java Generics + Inheritance.
 * All concrete DAOs extend this class, inheriting logging/timing utilities
 * while implementing the abstract mapping method (Template Method pattern).
 *
 * @param <T> Domain entity type
 */
public abstract class GenericRepository<T> implements IRepository<T> {

    protected final String tableName;

    protected GenericRepository(String tableName) {
        this.tableName = tableName;
    }

    // ── Abstract method: concrete DAOs map a ResultSet row → entity ──────────
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    // ── Shared utility: log DAO operations (Inheritance benefit) ─────────────
    protected void logOp(String operation) {
        System.out.printf("[%s][DAO] %s on table '%s'%n",
                LocalDateTime.now(), operation, tableName);
    }

    // ── Shared utility: count rows in this table ─────────────────────────────
    public Response<Integer> count() {
        logOp("COUNT");
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (var conn = DatabaseManager.getConnection();
                var stmt = conn.createStatement();
                var rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return Response.success(rs.getInt(1));
            }
            return Response.failure("Count returned no result");
        } catch (SQLException e) {
            return Response.failure("Count failed: " + e.getMessage());
        }
    }
}
