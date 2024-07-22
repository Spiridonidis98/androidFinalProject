package com.kouts.spiri.smartalert.Database;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class FirebaseDB {

    private static final FirebaseAuth auth;
    private static final FirebaseDatabase database;
    private static final DatabaseReference events;
    private static final DatabaseReference user;
    private static final DatabaseReference image;
    private static final StorageReference storageRef;
    private static final DatabaseReference alert;

    static {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        events = database.getReference("events");
        user = database.getReference("user");
        image = database.getReference("images");
        storageRef = FirebaseStorage.getInstance().getReference();
        alert = database.getReference("alert");
    }

    public static FirebaseAuth getAuth() { return auth;}

    public static StorageReference getStorageRef() { return storageRef;}

    public static DatabaseReference getEventsReference() {
        return events;
    }

    public static DatabaseReference getUserReference() {
        return user;
    }

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
                    })
                    .addOnFailureListener(e -> {
                        listener.onError(e);
                    });

        }
    }

    public interface FirebaseAlertListener {
        void alertAdded();
        void onError(Exception e);
    }
}
