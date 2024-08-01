package com.kouts.spiri.smartalert.POJOs;


import java.util.ArrayList;

public class Alert {

    private String aId; // alert id will be created based on the eventType and the timestamp of the first element of the group.
    private String timestamp;
    private int warning; // value of the warning importance if events.size  < 5 = 0 || events.size >=5 && events.size < 10 = 1 || events.size >= 10 = 2
    private EventTypes eventType;
    private double latitude, longitude;
    private float radius;
    private ArrayList<Event> alertEvents;


    // Default no-argument constructor
    public Alert() {
    }
    public Alert(String aId, String timestamp, int warning, EventTypes eventType, double latitude, double longitude, float radius, ArrayList<Event> alertEvents) {
        this.aId = aId;
        this.timestamp = timestamp;
        this.warning = warning;
        this.eventType = eventType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.alertEvents = alertEvents;
    }

    // Getter methods
    public String getaId() {
        return aId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getWarning() {
        return warning;
    }

    public EventTypes getEventType() {
        return eventType;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getRadius() {
        return radius;
    }

    public ArrayList<Event> getAlertEvents() {
        return alertEvents;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "aId='" + aId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", warning=" + warning +
                ", eventType=" + eventType +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", radius=" + radius +
                '}';
    }
}
