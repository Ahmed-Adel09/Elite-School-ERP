package com.elite.erp.model.staff;

/**
 * Accountant — extends Staff with CPA/CMA certification and software experience fields.
 */
public class Accountant extends Staff {

    private String cpaCmaCertification;
    private String financialSoftwareExperience;

    public Accountant() {
        super();
        setRoleType("ACCOUNTANT");
    }

    public Accountant(String fullName, String email, String phone, String cert, String software) {
        super(fullName, email, phone, "ACCOUNTANT");
        this.cpaCmaCertification         = cert;
        this.financialSoftwareExperience = software;
    }

    public String getCpaCmaCertification()              { return cpaCmaCertification; }
    public void   setCpaCmaCertification(String v)      { this.cpaCmaCertification = v; }
    public String getFinancialSoftwareExperience()       { return financialSoftwareExperience; }
    public void   setFinancialSoftwareExperience(String v){ this.financialSoftwareExperience = v; }

    @Override
    public String serializeExtraData() {
        return "cert=" + cpaCmaCertification + ";software=" + financialSoftwareExperience;
    }

    @Override
    public void deserializeExtraData(String data) {
        if (data == null) return;
        for (String part : data.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0]) {
                case "cert"     -> cpaCmaCertification         = kv[1];
                case "software" -> financialSoftwareExperience  = kv[1];
            }
        }
    }
}
