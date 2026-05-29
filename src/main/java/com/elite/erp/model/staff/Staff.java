package com.elite.erp.model.staff;

/**
 * Staff — Abstract base class for all staff recruitment applicants.
 *
 * OOP: Demonstrates Inheritance (Teacher, Nurse, Accountant, Librarian extend this)
 *      and Encapsulation (all fields are private, accessed via getters/setters).
 *
 * All sensitive fields (email, cvPath) are private — only accessible via
 * controlled accessors, fulfilling the Encapsulation requirement.
 */
public abstract class Staff {

    private int    id;
    private String fullName;
    private String email;       // sensitive — encapsulated
    private String phone;
    private String cvPath;      // sensitive — local file path
    private String status;
    private String roleType;    // discriminator: TEACHER, NURSE, ACCOUNTANT, LIBRARIAN

    protected Staff() {}

    protected Staff(String fullName, String email, String phone, String roleType) {
        this.fullName = fullName;
        this.email    = email;
        this.phone    = phone;
        this.roleType = roleType;
        this.status   = "PENDING";
    }

    /**
     * Returns role-specific field data as a serialized string for the `extra_data` DB column.
     * Subclasses override this to persist their unique fields.
     */
    public abstract String serializeExtraData();

    /**
     * Populates role-specific fields from the deserialized `extra_data` DB string.
     * Subclasses override this to restore their unique fields.
     */
    public abstract void deserializeExtraData(String data);

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int    getId()        { return id; }
    public void   setId(int id)  { this.id = id; }

    public String getFullName()              { return fullName; }
    public void   setFullName(String v)      { this.fullName = v; }

    public String getEmail()                 { return email; }
    public void   setEmail(String v)         { this.email = v; }

    public String getPhone()                 { return phone; }
    public void   setPhone(String v)         { this.phone = v; }

    public String getCvPath()                { return cvPath; }
    public void   setCvPath(String v)        { this.cvPath = v; }

    public String getStatus()                { return status; }
    public void   setStatus(String v)        { this.status = v; }

    public String getRoleType()              { return roleType; }
    public void   setRoleType(String v)      { this.roleType = v; }

    @Override
    public String toString() {
        return fullName + " [" + roleType + "]";
    }
}
