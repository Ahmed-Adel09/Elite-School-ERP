package com.elite.erp.model;

/**
 * Student domain model — demonstrates OOP Encapsulation.
 * All fields are private; access only via public getters/setters.
 */
public class Student {

    // ── Private fields (Encapsulation) ──────────────────────────────────────
    private int id;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String gender;
    private String nationality;
    private String applyingForGrade;
    private String previousSchool;
    private String previousGPA;
    private String photoPath;
    private String allergies;
    private String medicalNotes;
    private String vaccinationRecordPath;
    private int applicationId;
    private boolean ageFlagged;
    private String studentEmail;  // dedicated student Gmail/email address

    // ── Constructors ─────────────────────────────────────────────────────────
    public Student() {}

    public Student(String firstName, String lastName, String dateOfBirth,
                   String gender, String nationality, String applyingForGrade) {
        this.firstName       = firstName;
        this.lastName        = lastName;
        this.dateOfBirth     = dateOfBirth;
        this.gender          = gender;
        this.nationality     = nationality;
        this.applyingForGrade = applyingForGrade;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }

    public String getFirstName()                 { return firstName; }
    public void setFirstName(String firstName)   { this.firstName = firstName; }

    public String getLastName()                  { return lastName; }
    public void setLastName(String lastName)     { this.lastName = lastName; }

    public String getFullName()                  { return firstName + " " + lastName; }

    public String getDateOfBirth()               { return dateOfBirth; }
    public void setDateOfBirth(String dob)       { this.dateOfBirth = dob; }

    public String getGender()                    { return gender; }
    public void setGender(String gender)         { this.gender = gender; }

    public String getNationality()               { return nationality; }
    public void setNationality(String nat)       { this.nationality = nat; }

    public String getApplyingForGrade()          { return applyingForGrade; }
    public void setApplyingForGrade(String g)    { this.applyingForGrade = g; }

    public String getPreviousSchool()            { return previousSchool; }
    public void setPreviousSchool(String s)      { this.previousSchool = s; }

    public String getPreviousGPA()               { return previousGPA; }
    public void setPreviousGPA(String gpa)       { this.previousGPA = gpa; }

    public String getPhotoPath()                 { return photoPath; }
    public void setPhotoPath(String photoPath)   { this.photoPath = photoPath; }

    public String getAllergies()                 { return allergies; }
    public void setAllergies(String allergies)   { this.allergies = allergies; }

    public String getMedicalNotes()              { return medicalNotes; }
    public void setMedicalNotes(String notes)    { this.medicalNotes = notes; }

    public String getVaccinationRecordPath()     { return vaccinationRecordPath; }
    public void setVaccinationRecordPath(String p) { this.vaccinationRecordPath = p; }

    public int getApplicationId()                { return applicationId; }
    public void setApplicationId(int appId)      { this.applicationId = appId; }

    public boolean isAgeFlagged()                { return ageFlagged; }
    public void setAgeFlagged(boolean flag)      { this.ageFlagged = flag; }

    public String getStudentEmail()              { return studentEmail; }
    public void setStudentEmail(String email)    { this.studentEmail = email; }

    @Override
    public String toString() {
        return "Student{id=" + id + ", name='" + getFullName() + "', grade='" + applyingForGrade + "'}";
    }
}
