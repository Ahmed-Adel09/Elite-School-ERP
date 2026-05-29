package com.elite.erp.model.staff;

/**
 * Librarian — extends Staff with library-science-specific certification fields.
 */
public class Librarian extends Staff {

    private String libraryScienceCertification;
    private String archiveManagementExperience;
    private String catalogingSystemsKnowledge; // e.g. Dewey Decimal, Library of Congress

    public Librarian() {
        super();
        setRoleType("LIBRARIAN");
    }

    public Librarian(String fullName, String email, String phone,
                     String certification, String archiveExp, String cataloging) {
        super(fullName, email, phone, "LIBRARIAN");
        this.libraryScienceCertification = certification;
        this.archiveManagementExperience = archiveExp;
        this.catalogingSystemsKnowledge  = cataloging;
    }

    public String getLibraryScienceCertification()          { return libraryScienceCertification; }
    public void   setLibraryScienceCertification(String v)  { this.libraryScienceCertification = v; }
    public String getArchiveManagementExperience()           { return archiveManagementExperience; }
    public void   setArchiveManagementExperience(String v)   { this.archiveManagementExperience = v; }
    public String getCatalogingSystemsKnowledge()            { return catalogingSystemsKnowledge; }
    public void   setCatalogingSystemsKnowledge(String v)    { this.catalogingSystemsKnowledge = v; }

    @Override
    public String serializeExtraData() {
        return "cert=" + libraryScienceCertification
             + ";archive=" + archiveManagementExperience
             + ";cataloging=" + catalogingSystemsKnowledge;
    }

    @Override
    public void deserializeExtraData(String data) {
        if (data == null) return;
        for (String part : data.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0]) {
                case "cert"       -> libraryScienceCertification = kv[1];
                case "archive"    -> archiveManagementExperience  = kv[1];
                case "cataloging" -> catalogingSystemsKnowledge   = kv[1];
            }
        }
    }
}
