package com.kouts.spiri.smartalert.Functionality.Background_Functions;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.Activities.MapsActivity;
import com.kouts.spiri.smartalert.POJOs.Alert;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.POJOs.UserAlerts;
import com.kouts.spiri.smartalert.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class AlertNotificationService extends Service {

    final int TRIGGER_TIME = 5; //check for alerts in the database AND repeats the check every X minutes
    final long RECENT_EVENT_DAYS = 90L;
    final int EXTRA_DISTANCE = 10; //extra notification radius for "worried" users
    final int WORRIED_USER_INDICATOR = 3; //the number of events of one type that the user has reported recently that indicate he's "worried" about that event type
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
        String name = "Alerts";
        String description = "Get alerts for events near you";

        NotificationChannel channel = new NotificationChannel("alert_channel", name, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        channelCreated = true;
    }

    private void checkAlerts() {
        HashMap<EventTypes,Integer> userEvents = new HashMap<>(4);
        for (EventTypes type: EventTypes.values()) { //initialize userEvents with the EventTypes
            userEvents.put(type,0);
        }

        long lastXMinutes = System.currentTimeMillis() - (TRIGGER_TIME * 60 * 1000);

        long lastXDays = System.currentTimeMillis() - (RECENT_EVENT_DAYS * 24 * 60 * 60 * 1000);

        DatabaseReference dbEventsRef = FirebaseDB.getEventsReference();

        CompletableFuture<Void> getUserEvents = new CompletableFuture<>();

        //save the number of events of each type the current user has reported recently
        dbEventsRef.orderByChild("timestamp").startAt(Helper.timestampToDate(lastXDays)).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                DataSnapshot dataSnapshot = task.getResult();

                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Event event = data.getValue(Event.class);

                    if (event.getUid().equals(FirebaseDB.getAuth().getUid())) { //check that the event was made by the current user
                        EventTypes eventType = event.getAlertType();
                        int value = userEvents.get(eventType);
                        userEvents.put(eventType,value+1); //add the user's event to the appropriate type in the hashmap
                    }
                }
                getUserEvents.complete(null); //signal that this DB call has finished and the next one can begin
            }
        });

        getUserEvents.thenRun(() -> { //ensures this DB call only happens after the first one has finished
            //get recent alerts
            FirebaseDB.getAlertReference().orderByChild("timestamp").startAt(Helper.timestampToDate(lastXMinutes)).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {

                    DataSnapshot dataSnapshot = task.getResult();
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Alert alert = data.getValue(Alert.class); //get each alert found

                        if (alert == null) {
                            continue;
                        }

                        double locationLat = LocationService.getLocationLatitude();
                        double locationLong = LocationService.getLocationLongitude();
                        double distance;
                        if (locationLat != 0 && locationLong != 0) { //user location exists and it's not the default value
                            //calculate distance between user location and alert location
                            distance = Helper.calculateGeoDistance(locationLat, locationLong, alert.getLatitude(), alert.getLongitude());
                            Log.d("AlarmNotificationService", "distance : " +distance);
                        } else {
                            Log.d("AlarmNotificationService", "default location");
                            return;
                        }

                        //if the user has reported a lot of events of the same type as the alert recently, enlarge the notification radius
                        int extraDistance = 0;
                        if (userEvents.get(alert.getEventType()) >= WORRIED_USER_INDICATOR) {
                            extraDistance = EXTRA_DISTANCE;
                        }

                        //send notification for alert near the user, if they haven't already been notified of that alert before
                        if (distance <= alert.getRadius() + extraDistance) {
                            notifyIfNewAlert(alert);
                        }
                    }
                }
            });
        });
    }

    private void notifyIfNewAlert(Alert alert) {
        FirebaseUser user = FirebaseDB.getAuth().getCurrentUser();
        String userId = null;
        if (user != null) {
            userId = user.getUid();
        } else {
            Log.d("AlertNotificationService", "examineUserAlert: user is null");
            return;
        }

        //find UserAlert DB entry for the current user
        FirebaseDB.getUserAlertRef().orderByChild("uid").equalTo(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    for (DataSnapshot userAlertSnapshot : task.getResult().getChildren()) { //get UserAlerts
                        for (DataSnapshot alertSnapshot : userAlertSnapshot.child("alerts").getChildren()) { //get Alerts
                            String existingAlertId = alertSnapshot.child("aId").getValue(String.class);
                            if (alert.getaId().equals(existingAlertId)) { //if this alert has been already added before, don't send notification or add it again
                                return;
                            }
                        }
                    }

                    //alert does not exist inside UserAlert user's alert list so send notification and add it
                    sendNotification(alert);
                    saveUserAlert(alert);
                } else {
                    Log.d("AlertNotificationService", "examineUserAlert: task not successful ");
                }
            }
        });
    }

    private void sendNotification(Alert alert) {

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

    //save or update the UserAlert entry of the current user in the DB
    private void saveUserAlert(Alert alert) {

        FirebaseUser user = FirebaseDB.getAuth().getCurrentUser();
        if (user != null) {
            FirebaseDB.getUserAlertRef().orderByChild("uid").equalTo(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.d("AlertNotificationService", "getUserAlertRef task unsuccessful");
                        return;
                    }
                    DataSnapshot dataSnapshot = task.getResult();
                    UserAlerts userAlert = null;

                    if (dataSnapshot.hasChildren()) { //if there is a UserAlert entry with this user's id already
                        for (DataSnapshot data : dataSnapshot.getChildren()) { //we assume there is only one DataSnapshot
                            userAlert = data.getValue(UserAlerts.class);
                            if (userAlert != null) {
                                userAlert.getAlerts().add(alert); //add the new alert to the list with the existing ones
                            }
                        }
                    }
                    else { //if there is no such entry in the db, create new UserAlert
                        ArrayList<Alert> alertList = new ArrayList<>();
                        alertList.add(alert);
                        userAlert = new UserAlerts(user.getUid(), alertList);
                    }

                    Log.d("AlertNotificationService", "before add user alert ");

                    if (userAlert == null) {
                        Log.d("AlertNotificationService", "userAlert is null");
                        return;
                    }
                    FirebaseDB.addUserAlert(userAlert, new FirebaseDB.FirebaseUserAlertListener() { // push/update the userAlert entry
                        @Override
                        public void onUserAlertAdded() {
                        }

                        @Override
                        public void onError(Exception e) {
                        }
                    });
                }
            });
        }
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