package com.kouts.spiri.smartalert.POJOs;

import android.location.Location;

public class Address {
    private int id;
    private double lat;
    private double lon;
    private String description;

    public Address() {}
    public Address(int id, double lat, double lon, String description) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.description = description;
    }

    public int getId() {return id;}
    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getDescription() {
        return description;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
