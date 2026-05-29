package com.elite.erp.model;

/**
 * Parent/Guardian domain model — demonstrates OOP Encapsulation.
 */
public class Parent {

    private int id;
    private String fatherName;
    private String motherName;
    private String email;
    private String phone;
    private String address;
    private String occupation;
    private boolean corporateSponsor;
    private String companyName;
    private String companyEmail;
    private double sponsorAmount;
    private int applicationId;

    public Parent() {}

    public Parent(String fatherName, String motherName, String email, String phone) {
        this.fatherName = fatherName;
        this.motherName = motherName;
        this.email      = email;
        this.phone      = phone;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getFatherName()                   { return fatherName; }
    public void setFatherName(String fatherName)    { this.fatherName = fatherName; }

    public String getMotherName()                   { return motherName; }
    public void setMotherName(String motherName)    { this.motherName = motherName; }

    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }

    public String getPhone()                        { return phone; }
    public void setPhone(String phone)              { this.phone = phone; }

    public String getAddress()                      { return address; }
    public void setAddress(String address)          { this.address = address; }

    public String getOccupation()                   { return occupation; }
    public void setOccupation(String occupation)    { this.occupation = occupation; }

    public boolean isCorporateSponsor()             { return corporateSponsor; }
    public void setCorporateSponsor(boolean b)      { this.corporateSponsor = b; }

    public String getCompanyName()                  { return companyName; }
    public void setCompanyName(String name)         { this.companyName = name; }

    public String getCompanyEmail()                 { return companyEmail; }
    public void setCompanyEmail(String e)           { this.companyEmail = e; }

    public double getSponsorAmount()                { return sponsorAmount; }
    public void setSponsorAmount(double amount)     { this.sponsorAmount = amount; }

    public int getApplicationId()                   { return applicationId; }
    public void setApplicationId(int appId)         { this.applicationId = appId; }
}
