package com.kouts.spiri.smartalert.Services;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.AlertReceiver;
import com.kouts.spiri.smartalert.Functionality.MapsActivity;
import com.kouts.spiri.smartalert.POJOs.Alert;
import com.kouts.spiri.smartalert.R;

import java.util.Random;

public class AlertNotificationService extends Service {

    final int TRIGGER_TIME = 5; //check for alerts in the database AND repeats the check every X minutes

    private static boolean channelCreated = false;
    public AlertNotificationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!channelCreated) {
            createNotificationChannel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkAlerts();
        scheduleNextCheck();

        return START_REDELIVER_INTENT;
    }

    private void createNotificationChannel() {
        Log.d("AlarmNotificationService", "entered createNotificationChannel");

        String name = "Alerts";
        String description = "Get alerts for events near you";

        NotificationChannel channel = new NotificationChannel("alert_channel", name, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        channelCreated = true;
    }

    private void checkAlerts() {
        Log.d("AlarmNotificationService", "entered checkAlerts");
        long lastXMinutes = System.currentTimeMillis() - (TRIGGER_TIME * 60 * 1000);

        //get recent alerts
        FirebaseDB.getAlertReference().orderByChild("timestamp").startAt(Helper.timestampToDate(lastXMinutes)).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {

                DataSnapshot dataSnapshot = task.getResult();
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Alert alert = data.getValue(Alert.class);

                    if (alert == null) {
                        continue;
                    }

                    double locationLat = LocationService.getLocationLatitude();
                    double locationLong = LocationService.getLocationLongitude();
                    double distance;
                    if (locationLat!=0 && locationLong!=0) { //location exists and it's not the default value
                        distance = Helper.calculateGeoDistance(locationLat, locationLong, alert.getLatitude(), alert.getLongitude());
                    } else {
                        Log.d("AlarmNotificationService", "default location");
                        return;
                    }

                    //send notifications for alerts near the user
                    if (distance <= alert.getRadius()) {
                        sendNotification(alert);
                    }
                }
            }
        });
    }

    private void sendNotification(Alert alert) {
        Log.d("AlarmNotificationService", "entered sendNotification");

        //get user location
        Location alertLocation = new Location("");
        alertLocation.setLatitude(alert.getLatitude());
        alertLocation.setLongitude(alert.getLongitude());

        //create the Intent to open the map (for when the user clicks the notification)
        Intent notificationIntent = new Intent(this, MapsActivity.class);
        notificationIntent.putExtra("Location", alertLocation);
        notificationIntent.putExtra("EventType", alert.getEventType().toString());
        notificationIntent.putExtra("EventTime", alert.getTimestamp());

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        //create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "alert_channel")
                .setSmallIcon(R.drawable.announcement)
                .setContentTitle("ALERT")
                .setContentText(alert.getEventType()+" near you!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) //if the user clicks the notification the app opens the map with the location of the disaster
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(new Random().nextInt(), builder.build()); //send the notification with a random id
    }

    private void scheduleNextCheck() {

        Log.d("AlarmNotificationService", "entered scheduleNextCheck");

        //schedule the next notification check
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerTime = System.currentTimeMillis() + (TRIGGER_TIME * 60 * 1000); // repeat after X minutes

        //broadcast for the receiver to start the service again later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}