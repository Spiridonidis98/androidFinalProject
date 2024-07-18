package com.kouts.spiri.smartalert.Services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class LocationService extends Service {

    private static final long LOCATION_UPDATES_TIME = 5000; //miliseconds
    private static final int LOCATION_CODE = 0;
    private static Location location;
    private static LocationManager locationManager;
    private static LocationListener locationListener;

    @Override
    public void onCreate() {
        Log.d("SERVICE", "STARTED");

        super.onCreate();

    }

    @SuppressLint("MissingPermission") //we assume the permission check happens before the service is called, like in MainActivity
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SERVICE", "ONSTARTCOMMAND");

       // intent.getAction();//to be implemented

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String gps_provider = LocationManager.GPS_PROVIDER;

        locationListener = location ->  { //finds gps location and timestamp
            LocationService.location = location;
            Log.d("LOCATION", String.valueOf(location));
        };
            //return super.onStartCommand(intent, flags, startId);
        locationManager.requestLocationUpdates(gps_provider, LOCATION_UPDATES_TIME, 0, locationListener); //updates the location, assuming we have the permission

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
