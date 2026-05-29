package com.elite.erp.model;

public class SchoolEvent {
    private int id;
    private String name;
    private double cost;
    private String date;

    public SchoolEvent() {}

    public SchoolEvent(int id, String name, double cost, String date) {
        this.id = id;
        this.name = name;
        this.cost = cost;
        this.date = date;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    @Override
    public String toString() {
        return name + " - USD " + cost + " (" + date + ")";
    }
}
