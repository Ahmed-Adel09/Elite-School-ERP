package com.elite.erp.model;

/**
 * User model — for login/role management.
 * Demonstrates Encapsulation.
 */
public class User {

    private int    id;
    private String username;
    private String password;   // plain text for demo; would be hashed in production
    private String role;       // ADMIN | TEACHER | PARENT | STUDENT
    private String fullName;
    private String email;

    public User() {}

    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }

    public String getUsername()                  { return username; }
    public void   setUsername(String u)          { this.username = u; }

    public String getPassword()                  { return password; }
    public void   setPassword(String p)          { this.password = p; }

    public String getRole()                      { return role; }
    public void   setRole(String role)           { this.role = role; }

    public String getFullName()                  { return fullName; }
    public void   setFullName(String name)       { this.fullName = name; }

    public String getEmail()                     { return email; }
    public void   setEmail(String email)         { this.email = email; }

    public boolean isAdmin()   { return "ADMIN".equals(role); }
    public boolean isTeacher() { return "TEACHER".equals(role); }

    @Override public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + "}";
    }
}
