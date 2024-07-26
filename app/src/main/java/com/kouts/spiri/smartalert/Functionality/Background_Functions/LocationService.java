package com.kouts.spiri.smartalert.Functionality.Background_Functions;

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

import java.util.concurrent.atomic.AtomicBoolean;

public class LocationService extends Service {

    private static final long LOCATION_UPDATES_TIME = 5000; //miliseconds
    private static Location location;
    private static LocationManager locationManager;
    private static LocationListener locationListener;

    @Override
    public void onCreate() {
        Log.d("SERVICE", "STARTED");

        super.onCreate();

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String gps_provider = LocationManager.GPS_PROVIDER;

        AtomicBoolean locationNull = new AtomicBoolean(true);
        locationListener = location -> { //finds gps location
            if (locationNull.get() && intent.getBooleanExtra("permission_granted", false)) {
                //if the notification service hasn't been started yet and the permission has been granted, start the service
                LocationService.location = location;
                startAlarmNotificationService();
                locationNull.set(false);
            }
            LocationService.location = location;

            Log.d("LOCATION", "location :" + location.getLatitude() + " , " + location.getLongitude());
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(gps_provider, LOCATION_UPDATES_TIME, 0, locationListener); //updates the location, assuming we have the permission
        } else {
            Log.d("LocationService", "onStartCommand: ACCESS_FINE_LOCATION permission not granted");
        }

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
    public static double getLocationLatitude() {
        if (location != null) {
            return location.getLatitude();
        }
        return 0;
    }
    public static double getLocationLongitude() {
        if (location != null) {
            return location.getLongitude();
        }
        return 0;
    }

    public static long getLocationTime() {
        if (location != null) {
            return location.getTime();
        }
        return 0;
    }
    private void startAlarmNotificationService() {
        Intent notificationServiceIntent = new Intent(this, AlertNotificationService.class);
        startService(notificationServiceIntent);
    }
}
