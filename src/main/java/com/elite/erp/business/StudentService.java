package com.elite.erp.business;

import com.elite.erp.dao.AdmissionDAO;
import com.elite.erp.dao.StudentDAO;
import com.elite.erp.model.Student;
import com.elite.erp.util.Response;

import java.util.List;
import java.util.Map;

/**
 * StudentService — extends BaseService (Inheritance + Polymorphism).
 * Provides student management and statistics for dashboard display.
 */
public class StudentService extends BaseService {

    private final StudentDAO   studentDAO;
    private final AdmissionDAO admissionDAO;

    public StudentService() {
        super();
        this.studentDAO   = new StudentDAO();
        this.admissionDAO = new AdmissionDAO();
    }

    public Response<List<Student>> getAllStudents() {
        logInfo("Fetching all students");
        return studentDAO.findAll();
    }

    public Response<Student> getStudentById(int id) {
        logInfo("Fetching student #" + id);
        return studentDAO.findById(id);
    }

    public Response<Boolean> updateStudent(Student student) {
        if (isNullOrEmpty(student.getFirstName()) || isNullOrEmpty(student.getLastName())) {
            return Response.failure("Name fields cannot be empty.");
        }
        logInfo("Updating student #" + student.getId());
        return studentDAO.update(student);
    }

    public Response<Boolean> deleteStudent(int id) {
        logInfo("Deleting student #" + id);
        return studentDAO.delete(id);
    }

    /** Used by the FXGL chart to pull real enrollment data */
    public Response<Map<String, Integer>> getEnrollmentStats() {
        logInfo("Fetching enrollment statistics");
        return admissionDAO.getMonthlyEnrollmentStats();
    }

    public Response<Integer> getTotalStudentCount() {
        return studentDAO.count();
    }
}
