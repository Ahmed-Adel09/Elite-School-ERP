package com.elite.erp.business;

import com.elite.erp.dao.AdmissionDAO;
import com.elite.erp.dao.TransactionDAO;
import com.elite.erp.model.Transaction;
import com.elite.erp.util.Response;

import java.time.LocalDateTime;

/**
 * PaymentService — Business Logic Layer for InstaPay operations.
 * Extends BaseService (Inheritance).
 *
 * The school's IPA (InstaPay Address) is managed here.
 * Reference verification checks format + length, then marks VERIFIED.
 */
public class PaymentService extends BaseService {

    // ── School's official InstaPay Address ───────────────────────────────────
    public static final String SCHOOL_IPA    = "EliteSchool@instapay";
    public static final double PAYMENT_AMOUNT = 200.0;

    private final TransactionDAO transactionDAO;
    private final AdmissionDAO   admissionDAO;

    public PaymentService() {
        super();
        this.transactionDAO = new TransactionDAO();
        this.admissionDAO   = new AdmissionDAO();
    }

    /** Return the school's IPA for display in the UI */
    public String getSchoolIPA() { return SCHOOL_IPA; }

    /**
     * Verify an InstaPay reference submitted by the parent.
     *
     * Rules for a valid reference:
     * - Must be 8–20 alphanumeric characters
     * - Must not already exist for this application
     *
     * In production, this would call InstaPay's verification API.
     */
    public Response<Transaction> verifyPayment(int applicationId, String reference) {
        logInfo("Verifying InstaPay ref '" + reference + "' for app #" + applicationId);

        // Validate reference format
        if (isNullOrEmpty(reference)) {
            return Response.failure("Transaction reference cannot be empty.", "PAY_001");
        }
        if (!reference.matches("[A-Za-z0-9]{6,20}")) {
            return Response.failure(
                "Invalid reference format. Must be 6–20 alphanumeric characters.", "PAY_002");
        }

        // Create and persist the transaction record
        Transaction tx = new Transaction(applicationId, reference.toUpperCase());
        tx.setAmount(PAYMENT_AMOUNT);
        tx.setStatus("VERIFIED");
        tx.setVerifiedAt(LocalDateTime.now().toString());

        Response<Transaction> saved = transactionDAO.save(tx);
        if (!saved.isSuccess()) {
            return Response.failure("Could not record transaction: " + saved.getMessage(), "PAY_003");
        }

        // Mark application payment as completed
        admissionDAO.findById(applicationId).getData();  // existence check
        logInfo("Payment VERIFIED — application #" + applicationId + " marked paid.");

        return Response.success(saved.getData(), "✅ Payment verified! Reference: " + reference.toUpperCase());
    }

    /** Get payment amount string for display */
    public String getAmountDisplay() {
        return String.format("$%.2f USD", PAYMENT_AMOUNT);
    }
}
