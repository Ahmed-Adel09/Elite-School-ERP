package com.elite.erp.dao;

import com.elite.erp.model.Student;
import com.elite.erp.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentDAO — persists Student records including the new student_email column.
 */
public class StudentDAO extends GenericRepository<Student> {

    public StudentDAO() { super("students"); }

    @Override
    protected Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setApplicationId(rs.getInt("application_id"));
        s.setFirstName(rs.getString("first_name"));
        s.setLastName(rs.getString("last_name"));
        s.setDateOfBirth(rs.getString("date_of_birth"));
        s.setGender(rs.getString("gender"));
        s.setNationality(rs.getString("nationality"));
        s.setApplyingForGrade(rs.getString("applying_for_grade"));
        s.setPreviousSchool(rs.getString("previous_school"));
        s.setPreviousGPA(rs.getString("previous_gpa"));
        s.setPhotoPath(rs.getString("photo_path"));
        s.setAllergies(rs.getString("allergies"));
        s.setMedicalNotes(rs.getString("medical_notes"));
        s.setVaccinationRecordPath(rs.getString("vaccination_record"));
        s.setAgeFlagged(rs.getInt("age_flagged") == 1);
        // student_email — may be missing in old rows; use try/catch
        try { s.setStudentEmail(rs.getString("student_email")); }
        catch (SQLException ignored) {}
        return s;
    }

    @Override
    public Response<Student> save(Student s) {
        logOp("INSERT");
        String sql = """
            INSERT INTO students
            (application_id, first_name, last_name, date_of_birth, gender,
             nationality, applying_for_grade, previous_school, previous_gpa,
             photo_path, allergies, medical_notes, vaccination_record,
             age_flagged, student_email)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1,  s.getApplicationId());
            ps.setString(2,  s.getFirstName());
            ps.setString(3,  s.getLastName());
            ps.setString(4,  s.getDateOfBirth());
            ps.setString(5,  s.getGender());
            ps.setString(6,  s.getNationality());
            ps.setString(7,  s.getApplyingForGrade());
            ps.setString(8,  s.getPreviousSchool());
            ps.setString(9,  s.getPreviousGPA());
            ps.setString(10, s.getPhotoPath());
            ps.setString(11, s.getAllergies());
            ps.setString(12, s.getMedicalNotes());
            ps.setString(13, s.getVaccinationRecordPath());
            ps.setInt   (14, s.isAgeFlagged() ? 1 : 0);
            ps.setString(15, s.getStudentEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
            return Response.success(s, "Student saved");
        } catch (SQLException e) {
            return Response.failure("Save student failed: " + e.getMessage());
        }
    }

    @Override
    public Response<Student> findById(int id) {
        logOp("SELECT by ID");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM students WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Response.success(mapRow(rs));
                return Response.failure("Student not found");
            }
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    /** Find student by application_id (used by Admin dashboard) */
    public Response<Student> findByApplicationId(int appId) {
        logOp("SELECT by APP ID");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM students WHERE application_id=? LIMIT 1")) {
            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Response.success(mapRow(rs));
                return Response.failure("No student for app #" + appId);
            }
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    @Override
    public Response<List<Student>> findAll() {
        logOp("SELECT ALL");
        List<Student> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT * FROM students")) {
            while (rs.next()) list.add(mapRow(rs));
            return Response.success(list, list.size() + " students found");
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    @Override
    public Response<Boolean> update(Student s) {
        logOp("UPDATE");
        String sql = """
            UPDATE students SET first_name=?, last_name=?, date_of_birth=?,
            gender=?, nationality=?, applying_for_grade=?, allergies=?,
            medical_notes=?, student_email=?
            WHERE id=?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getFirstName());
            ps.setString(2, s.getLastName());
            ps.setString(3, s.getDateOfBirth());
            ps.setString(4, s.getGender());
            ps.setString(5, s.getNationality());
            ps.setString(6, s.getApplyingForGrade());
            ps.setString(7, s.getAllergies());
            ps.setString(8, s.getMedicalNotes());
            ps.setString(9, s.getStudentEmail());
            ps.setInt   (10, s.getId());
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }

    @Override
    public Response<Boolean> delete(int id) {
        logOp("DELETE");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM students WHERE id=?")) {
            ps.setInt(1, id);
            return Response.success(ps.executeUpdate() > 0);
        } catch (SQLException e) { return Response.failure(e.getMessage()); }
    }
}
