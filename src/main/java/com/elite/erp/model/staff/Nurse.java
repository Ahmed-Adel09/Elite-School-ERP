package com.elite.erp.model.staff;

/**
 * Nurse — extends Staff with medical license and specialization fields.
 */
public class Nurse extends Staff {

    private String medicalLicenseNumber;
    private String clinicSpecialization;

    public Nurse() {
        super();
        setRoleType("NURSE");
    }

    public Nurse(String fullName, String email, String phone, String license, String specialization) {
        super(fullName, email, phone, "NURSE");
        this.medicalLicenseNumber = license;
        this.clinicSpecialization = specialization;
    }

    public String getMedicalLicenseNumber()           { return medicalLicenseNumber; }
    public void   setMedicalLicenseNumber(String v)   { this.medicalLicenseNumber = v; }
    public String getClinicSpecialization()            { return clinicSpecialization; }
    public void   setClinicSpecialization(String v)    { this.clinicSpecialization = v; }

    @Override
    public String serializeExtraData() {
        return "license=" + medicalLicenseNumber + ";specialization=" + clinicSpecialization;
    }

    @Override
    public void deserializeExtraData(String data) {
        if (data == null) return;
        for (String part : data.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0]) {
                case "license"        -> medicalLicenseNumber = kv[1];
                case "specialization" -> clinicSpecialization  = kv[1];
            }
        }
    }
}
