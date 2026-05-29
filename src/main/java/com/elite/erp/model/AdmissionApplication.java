package com.elite.erp.model;

import java.time.LocalDate;

/**
 * Aggregate root combining Student + Parent for an admission application.
 * Demonstrates Encapsulation and object composition.
 */
public class AdmissionApplication {

    private int id;
    private Student student;
    private Parent parent;
    private ApplicationStatus status;
    private LocalDate submissionDate;
    private double paymentAmount;
    private boolean paymentCompleted;
    private String language; // "EN" or "AR"

    public AdmissionApplication() {
        this.student          = new Student();
        this.parent           = new Parent();
        this.status           = ApplicationStatus.PENDING;
        this.submissionDate   = LocalDate.now();
        this.paymentAmount    = 200.0;
        this.paymentCompleted = false;
        this.language         = "EN";
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public Student getStudent()                     { return student; }
    public void setStudent(Student student)         { this.student = student; }

    public Parent getParent()                       { return parent; }
    public void setParent(Parent parent)            { this.parent = parent; }

    public ApplicationStatus getStatus()            { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public LocalDate getSubmissionDate()            { return submissionDate; }
    public void setSubmissionDate(LocalDate date)   { this.submissionDate = date; }

    public double getPaymentAmount()                { return paymentAmount; }
    public void setPaymentAmount(double amount)     { this.paymentAmount = amount; }

    public boolean isPaymentCompleted()             { return paymentCompleted; }
    public void setPaymentCompleted(boolean done)   { this.paymentCompleted = done; }

    public String getLanguage()                     { return language; }
    public void setLanguage(String lang)            { this.language = lang; }

    @Override
    public String toString() {
        return "Application{id=" + id
                + ", student=" + (student != null ? student.getFullName() : "N/A")
                + ", status=" + status + "}";
    }
}
