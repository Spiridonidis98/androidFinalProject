package com.kouts.spiri.smartalert.POJOs;

import android.net.Uri;

public class Event {
    private String uid; //the id of the USER who submitted the event
    private EventTypes alertType;
    private double latitude, longitude;
    private String timestamp;
    private String comment;
    private String image; //optional
    private Uri imageURI; //optional
    private int weight; //how accurate the event is considered to be

    public Event() {}

    public Event(String uid, EventTypes alertType, double latitude, double longitude, String timestamp, String comment, String image, int weight) {
        this.uid = uid;
        this.alertType = alertType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.comment = comment;
        this.image = image;
        this.weight = weight;
    }

    public void setImageURI(Uri imageURI) { this.imageURI = imageURI;}
    public Uri getImageURI(){ return this.imageURI;}
    public String getUid() {
        return uid;
    }

    public EventTypes getAlertType() {
        return alertType;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getComment() {
        return comment;
    }

    public String getImage() {
        return image;
    }
    public int getWeight() { return weight; }

    @Override
    public String toString() {
        return "Event{" +
                "uid='" + uid + '\'' +
                ", alertType=" + alertType +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", timestamp='" + timestamp + '\'' +
                ", comment='" + comment + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}
