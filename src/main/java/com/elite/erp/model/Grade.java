package com.elite.erp.model;

public class Grade {
    private int id;
    private int studentId;
    private String subject;
    private double score;
    private String date;

    public Grade() {}

    public Grade(int id, int studentId, String subject, double score, String date) {
        this.id = id;
        this.studentId = studentId;
        this.subject = subject;
        this.score = score;
        this.date = date;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
