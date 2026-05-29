package com.elite.erp.model.staff;

/**
 * Teacher — extends Staff with subject expertise and experience fields.
 * Demonstrates Inheritance from the Staff base class.
 */
public class Teacher extends Staff {

    private String subjectExpertise;
    private int    yearsOfExperience;

    public Teacher() {
        super();
        setRoleType("TEACHER");
    }

    public Teacher(String fullName, String email, String phone, String subject, int years) {
        super(fullName, email, phone, "TEACHER");
        this.subjectExpertise  = subject;
        this.yearsOfExperience = years;
    }

    public String getSubjectExpertise()            { return subjectExpertise; }
    public void   setSubjectExpertise(String v)    { this.subjectExpertise = v; }
    public int    getYearsOfExperience()           { return yearsOfExperience; }
    public void   setYearsOfExperience(int v)      { this.yearsOfExperience = v; }

    @Override
    public String serializeExtraData() {
        return "subject=" + subjectExpertise + ";years=" + yearsOfExperience;
    }

    @Override
    public void deserializeExtraData(String data) {
        if (data == null) return;
        for (String part : data.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0]) {
                case "subject" -> subjectExpertise  = kv[1];
                case "years"   -> yearsOfExperience = Integer.parseInt(kv[1]);
            }
        }
    }
}
