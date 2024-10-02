package com.kouts.spiri.smartalert.POJOs;

import java.util.ArrayList;

public class User {

    private String uid;
    private String email;
    private String name;
    private String lastname;
    private int type; // 0: politiki prostasia | 1: simple user
    private ArrayList<Address> addresses = new ArrayList<>();

    public User(){}
    public User(String uid, String email, String name, String lastname, int type) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.lastname = lastname;
        this.type = type;
    }

    public String getEmail() {
        return this.email;
    }
    public int getType() {
        return this.type;
    }
    public String getUid() {
        return this.uid;
    }
    public String getName() { return this.name; }
    public String getLastname() { return this.lastname;}

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void addAddress(Address address) {
        this.addresses.add(address);
    }

    public ArrayList<Address> getAddresses() { return this.addresses; }
}
