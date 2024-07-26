package com.kouts.spiri.smartalert.Database;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.POJOs.Alert;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.POJOs.User;
import com.kouts.spiri.smartalert.POJOs.UserAlerts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseDB {

    private static final FirebaseAuth auth;
    private static final FirebaseDatabase database;
    private static final DatabaseReference events;
    private static final DatabaseReference user;
    private static final DatabaseReference image;
    private static final StorageReference storageRef;
    private static final DatabaseReference alert;
    private static final DatabaseReference userAlert;

    static {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        events = database.getReference("events");
        user = database.getReference("user");
        image = database.getReference("images");
        storageRef = FirebaseStorage.getInstance().getReference();
        alert = database.getReference("alert");
        userAlert = database.getReference("userAlerts");
    }

    public static FirebaseAuth getAuth() { return auth;}

    public static StorageReference getStorageRef() { return storageRef;}

    public static DatabaseReference getEventsReference() {
        return events;
    }

    public static DatabaseReference getUserReference() {
        return user;
    }
    public static DatabaseReference getAlertReference() {
        return alert;
    }
    public static DatabaseReference getUserAlertRef() { return userAlert; }

    public static void addUser(User newUser, final FirebaseUserListener listener) {
        if(auth.getCurrentUser() != null) {
            DatabaseReference newUserRef = user.push();
            newUserRef.setValue(newUser)
                    .addOnSuccessListener( aVoid -> {
                        //Successfully added user
                        listener.onUserAdded();
                    })
                    .addOnFailureListener(e -> {
                        listener.onError(e);
                    });

        }
    }

    public static void addEvent(Event newEvent, final FirebaseEventListener listener) {
        if(auth.getCurrentUser() != null) {
            DatabaseReference newEventRef = events.push();
            newEventRef.setValue(newEvent)
                    .addOnSuccessListener( aVoid -> {
                        //Successfully added user
                        listener.onEventAdded();
                    })
                    .addOnFailureListener(e -> {
                        listener.onError(e);
                    });

        }
    }

    //adds new UserAlert entry if there was no previous entry for that user, or updates the old entry to include new alerts
    public static void addUserAlert(UserAlerts userAlerts, final FirebaseUserAlertListener listener) {
        DatabaseReference newUserAlertRef = userAlert;

        if (userAlerts.getAlerts().size() > 1) { //if the list has more than one alert instead of creating new entry update the old one
            newUserAlertRef.orderByChild("uid").equalTo(userAlerts.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        return;
                    }

                    DataSnapshot dataSnapshot = task.getResult();
                    for (DataSnapshot data: dataSnapshot.getChildren()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("alerts", userAlerts.getAlerts());
                        data.getRef().updateChildren(updates); //update the alerts of the existing entry
                    }
                }
            });
        }
        else { //push new entry
            newUserAlertRef.push().setValue(userAlerts)
                    .addOnSuccessListener(aVoid -> {
                        //Successfully added user
                        listener.onUserAlertAdded();
                    })
                    .addOnFailureListener(e -> {
                        listener.onError(e);
                    });
        }
    }

    public static void getEvents(String startDate, String endDate, Boolean isFireChecked, Boolean isFloodChecked, Boolean isEarthquakeChecked, Boolean isTornadoChecked, final FirebaseEventListener listener) {

        Log.e("START", startDate);
        Log.e("End", endDate);

        Query query = events.orderByChild("timestamp").endBefore(endDate).startAfter(startDate);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Event> eventsFound = new ArrayList<>();
                if (!snapshot.exists()) {
                    listener.onEventsRetrieved(null);
                    return;
                }
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Event tempEvent = snap.getValue(Event.class);

                    if(tempEvent != null &&
                           ( (isFireChecked && tempEvent.getAlertType() == EventTypes.FIRE)
                            || (isFloodChecked && tempEvent.getAlertType() == EventTypes.FLOOD)
                            || (isEarthquakeChecked && tempEvent.getAlertType() == EventTypes.EARTHQUAKE)
                            || (isTornadoChecked && tempEvent.getAlertType() == EventTypes.TORNADO))
                    ){
                        eventsFound.add(tempEvent);
                    }
                }

                listener.onEventsRetrieved(eventsFound);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public static void getUserInfo(String uid, final FirebaseUserListener listener) {
        Query query = user.orderByChild("uid").equalTo(uid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> users = new ArrayList<>();
                for(DataSnapshot snapshot: dataSnapshot.getChildren()) {

                    users.add(snapshot.getValue(User.class));
                }
                if(users.isEmpty()) {
                    listener.onUserRetrieved(null);
                    return;
                }
                listener.onUserRetrieved(users.get(0));

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    public interface FirebaseUserListener {
        void onUserRetrieved(User user);
        void onUserAdded();
        void onError(Exception e);
    }

    public interface FirebaseEventListener {
        void onEventsRetrieved(List<Event> events);
        void onEventAdded();
        void onError(Exception e);
    }

    public interface FirebaseUserAlertListener {
        void onUserAlertAdded();
        void onError(Exception e);
    }

    //here we retrieve the image for the event
    public static void getImageFromStorage(String imgId, final FirebaseStorageListener listener) {
        StorageReference imageRef = storageRef.child("images/" + imgId);


        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            listener.onImageRetrieved(uri);
        }).addOnFailureListener(exception -> {
            listener.onError(exception);
        });
    }

    public interface FirebaseStorageListener {
        void onImageRetrieved(Uri image);
        void onError(Exception e);
    }

    //here we have the implementation for the alerts
    public static void addAlert(Alert newAlert, final FirebaseAlertListener listener) {
        if(auth.getCurrentUser() != null) {
            DatabaseReference newAlertRef = alert.push();
            newAlertRef.setValue(newAlert)
                    .addOnSuccessListener( aVoid -> {
                        //Successfully added user
                        listener.alertAdded();
                    });
        }
    }

    public interface FirebaseAlertListener {
        void alertAdded();
    }

    //here we fetch the user alerts
    public static void getUserAlerts(String startDate, String endDate, final FirebaseUserAlertGetterListener listener) {

        Log.e("START", startDate);
        Log.e("End", endDate);

        Query query = userAlert.orderByChild("uid").equalTo(Helper.getUser().getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserAlerts userAlertsFound = null;
                if (!snapshot.exists()) {
                    listener.onUserAlertsRetrieved(null);
                    return;
                }
                for (DataSnapshot snap : snapshot.getChildren()) {
                    UserAlerts temp = snap.getValue(UserAlerts.class);

                    if(temp != null ){
                       userAlertsFound = temp;
                    }
                }
                listener.onUserAlertsRetrieved(userAlertsFound);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public interface FirebaseUserAlertGetterListener {
        void onUserAlertsRetrieved(UserAlerts userAlerts);
    }

}
