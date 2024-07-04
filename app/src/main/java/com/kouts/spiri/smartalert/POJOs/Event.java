package com.kouts.spiri.smartalert.POJOs;

public class Event {
    private String uid; //the id of the USER who submitted the event
    private EventTypes alertType;
    private double latitude, longitude;
    private String timestamp;
    private String comment;
    private String image; //optional

    public Event() {}

    public Event(String uid, EventTypes alertType, double latitude, double longitude, String timestamp, String comment, String image) {
        this.uid = uid;
        this.alertType = alertType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.comment = comment;
        this.image = image;
    }

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
