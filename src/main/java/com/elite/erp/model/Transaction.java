package com.elite.erp.model;

/**
 * Transaction model — represents an InstaPay payment attempt.
 * Demonstrates Encapsulation: all fields private with getters/setters.
 */
public class Transaction {

    private int    id;
    private int    applicationId;
    private String ipaReference;   // the reference code entered by the parent
    private double amount;
    private String status;         // PENDING | VERIFIED | FAILED
    private String verifiedAt;

    public Transaction() { this.amount = 200.0; this.status = "PENDING"; }

    public Transaction(int applicationId, String ipaReference) {
        this();
        this.applicationId = applicationId;
        this.ipaReference  = ipaReference;
    }

    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }

    public int    getApplicationId()               { return applicationId; }
    public void   setApplicationId(int id)         { this.applicationId = id; }

    public String getIpaReference()                { return ipaReference; }
    public void   setIpaReference(String ref)      { this.ipaReference = ref; }

    public double getAmount()                      { return amount; }
    public void   setAmount(double amount)         { this.amount = amount; }

    public String getStatus()                      { return status; }
    public void   setStatus(String status)         { this.status = status; }

    public String getVerifiedAt()                  { return verifiedAt; }
    public void   setVerifiedAt(String at)         { this.verifiedAt = at; }

    @Override
    public String toString() {
        return "Transaction{id=" + id + ", ref='" + ipaReference + "', status=" + status + "}";
    }
}
