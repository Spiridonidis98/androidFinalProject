package com.kouts.spiri.smartalert.POJOs;

import java.util.ArrayList;

public class UserAlerts {
    private String uid;
    private ArrayList<Alert> alerts;

    public UserAlerts() { //necessary for firebase
    }

    public UserAlerts(String uid, ArrayList<Alert> alerts) {
        this.uid = uid;
        this.alerts = alerts;
    }

    public String getUid() {
        return uid;
    }

    public ArrayList<Alert> getAlerts() {
        return alerts;
    }
}
