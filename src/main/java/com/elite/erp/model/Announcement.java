package com.elite.erp.model;

public class Announcement {
    private int id;
    private String title;
    private String content;
    private String date;
    private String mediaPath;   // optional attached image/video path

    public Announcement() {}

    public Announcement(int id, String title, String content, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
    }

    public int    getId()                    { return id; }
    public void   setId(int id)              { this.id = id; }
    public String getTitle()                 { return title; }
    public void   setTitle(String title)     { this.title = title; }
    public String getContent()               { return content; }
    public void   setContent(String content) { this.content = content; }
    public String getDate()                  { return date; }
    public void   setDate(String date)       { this.date = date; }
    public String getMediaPath()             { return mediaPath; }
    public void   setMediaPath(String v)     { this.mediaPath = v; }

    @Override
    public String toString() { return "[" + date + "] " + title + "\n" + content; }
}
