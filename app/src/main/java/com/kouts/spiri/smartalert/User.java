package com.kouts.spiri.smartalert;

public class User {

    private String uid;
    private String email;
    private String username;
    private String name;
    private String lastname;
    private int type; // 0: politiki prostasia | 1: simple user

    public User(){}
    public User(String uid, String email, String username, String name, String lastname, int type) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.name = name;
        this.lastname = lastname;
        this.type = type;
    }

    public String getEmail() {
        return this.email;
    }
    public String getUsername() {
        return this.username;
    }
    public int getType() {
        return this.type;
    }
    public String getUid() {
        return this.uid;
    }
}
