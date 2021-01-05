package com.example.vinmod;

public class Post {
    private String id;
    private String date;
    private String title;
    private String text;
    private String userName;
    private int replyCount;

    public Post(String u, String v, String w, String x, String y, int z){
        this.id = u;
        this.date = v;
        this.title = w;
        this.text = x;
        this.userName = y;
        this.replyCount = z;
    }

    public Post(Post p){
        this.id = p.getId();
        this.date = p.getDate();
        this.title = p.getTitle();
        this.text = p.getText();
        this.userName = p.getUserName();
        this.replyCount = p.getReplyCount();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }
}
