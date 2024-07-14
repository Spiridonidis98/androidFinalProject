package com.kouts.spiri.smartalert.Assistance;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

public class UserLocation {

    public interface LocationCallBackListener {
        void onLocationChanged(Location location);
    }
    private static final long LOCATION_UPDATES_TIME = 3000; //miliseconds
    public static final int LOCATION_CODE = 0;
    private final Context context;
    private LocationCallBackListener locationCallBackListener;

    public static Location location;
    private final String GPS_PROVIDER =  LocationManager.GPS_PROVIDER;
    private static LocationManager locationManager;
    private Activity activity;

    LocationListener locationListener = location -> { //finds gps location and timestamp
        this.location = location;
        if(this.locationCallBackListener != null) {
            this.locationCallBackListener.onLocationChanged(this.location);
        }
    };
    public UserLocation(Context context, Activity activity , LocationCallBackListener locationCallBackListener) {
        this.context = context;
        this.activity = activity;
        this.locationCallBackListener = locationCallBackListener;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.activity,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_CODE); //actually asks the user for the permission


            //Permission not granted
            return;
        }
        this.locationManager.requestLocationUpdates(GPS_PROVIDER, LOCATION_UPDATES_TIME, 0, locationListener); //updates the location, assuming we have the permission
    }

    public void stopLocationUpdates() {
        this.locationManager.removeUpdates(locationListener);
    }

    public Location getLocation() {
        return location;
    }

}
