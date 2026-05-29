package com.elite.erp.business;

import com.elite.erp.dao.AdmissionDAO;
import com.elite.erp.dao.StudentDAO;
import com.elite.erp.dao.ParentDAO;
import com.elite.erp.model.AdmissionApplication;
import com.elite.erp.util.Response;

/**
 * AdmissionService — extends BaseService (Inheritance).
 * Orchestrates the 5-step wizard: validates data, persists to DB,
 * and coordinates between Student, Parent, and Application DAOs.
 */
public class AdmissionService extends BaseService {

    private final AdmissionDAO admissionDAO;
    private final StudentDAO   studentDAO;
    private final ParentDAO    parentDAO;

    public AdmissionService() {
        super();
        this.admissionDAO = new AdmissionDAO();
        this.studentDAO   = new StudentDAO();
        this.parentDAO    = new ParentDAO();
    }

    /**
     * Validates and submits a full admission application.
     * Demonstrates service layer coordination (OOP Polymorphism via DAO interfaces).
     */
    public Response<AdmissionApplication> submitApplication(AdmissionApplication app) {
        logInfo("Submitting application for: " + app.getStudent().getFullName());

        // 1. Validate parent info
        if (isNullOrEmpty(app.getParent().getFatherName()) &&
            isNullOrEmpty(app.getParent().getMotherName())) {
            return Response.failure("At least one parent name is required.", "VAL_001");
        }
        if (!isValidEmail(app.getParent().getEmail())) {
            return Response.failure("Invalid parent email address.", "VAL_002");
        }
        if (!isValidPhone(app.getParent().getPhone())) {
            return Response.failure("Invalid phone number.", "VAL_003");
        }

        // 2. Validate student info
        if (isNullOrEmpty(app.getStudent().getFirstName()) ||
            isNullOrEmpty(app.getStudent().getLastName())) {
            return Response.failure("Student first and last name are required.", "VAL_004");
        }

        // 3. Validate payment
        if (!app.isPaymentCompleted()) {
            return Response.failure("Payment must be completed before submission.", "VAL_005");
        }

        try {
            // 4. Save application record
            Response<AdmissionApplication> appResp = admissionDAO.save(app);
            if (!appResp.isSuccess()) return appResp;
            int appId = appResp.getData().getId();

            // 5. Save student with application FK
            app.getStudent().setApplicationId(appId);
            Response<com.elite.erp.model.Student> stuResp = studentDAO.save(app.getStudent());
            if (!stuResp.isSuccess()) {
                return Response.failure("Student save failed: " + stuResp.getMessage());
            }

            // 6. Save parent with application FK
            app.getParent().setApplicationId(appId);
            Response<com.elite.erp.model.Parent> parResp = parentDAO.save(app.getParent());
            if (!parResp.isSuccess()) {
                return Response.failure("Parent save failed: " + parResp.getMessage());
            }

            // 7. Increment enrollment stats for chart
            admissionDAO.incrementCurrentMonthCount();

            logInfo("Application #" + appId + " submitted successfully.");
            return Response.success(app, "Application submitted successfully! ID: " + appId);

        } catch (Exception e) {
            logError("Unexpected error during submission", e);
            return Response.failure("Unexpected error: " + e.getMessage(), "ERR_500");
        }
    }

    public Response<java.util.List<AdmissionApplication>> getAllApplications() {
        return admissionDAO.findAll();
    }

    public AdmissionDAO getAdmissionDAO() { return admissionDAO; }
}
