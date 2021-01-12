package com.example.vinmod;

public class Reply {
    private String id;
    private String date;
    private String user;
    private String message;

    public Reply(String w, String x, String y, String z){
        this.id = w;
        this.date = x;
        this.user = y;
        this.message = z;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
