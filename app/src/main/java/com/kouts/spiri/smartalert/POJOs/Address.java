package com.kouts.spiri.smartalert.POJOs;

import android.location.Location;

public class Address {
    private double lat;
    private double lon;
    private String description;

    public Address() {}
    public Address(double lat, double lon, String description) {
        this.lat = lat;
        this.lon = lon;
        this.description = description;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getDescription() {
        return description;
    }
}
