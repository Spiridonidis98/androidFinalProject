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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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


    public static void updateUser(User editUser, final FireBaseUpdateUserListener listener) {
        Query query = user.orderByChild("uid").equalTo(editUser.getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()) {
                    listener.onError(new Exception());
                    return;
                }
                for( DataSnapshot userSnapshot: snapshot.getChildren()) {
                    if(userSnapshot.getKey().isEmpty()) {
                        listener.onError(new Exception());
                        return;
                    }
                    DatabaseReference userRef = user.child(userSnapshot.getKey());

                    userRef.setValue(editUser).addOnSuccessListener( aVoid -> {
                        listener.onUpdateUser();
                    }).addOnFailureListener(e -> {
                        listener.onError(e);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
    public interface FireBaseUpdateUserListener {
        void onUpdateUser();
        void onError(Exception e);
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

        newUserAlertRef.orderByChild("uid").equalTo(userAlerts.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.d("addUserAlert", "onComplete: Task not successful");
                    return;
                }

                DataSnapshot dataSnapshot = task.getResult();
                if (!dataSnapshot.hasChildren()) { //there is no existing entry so push new entry
                    newUserAlertRef.push().setValue(userAlerts)
                            .addOnSuccessListener(aVoid -> {
                                //Successfully added user
                                listener.onUserAlertAdded();
                            })
                            .addOnFailureListener(e -> {
                                listener.onError(e);
                            });
                }
                else { //if there is an existing entry update it
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("alerts", userAlerts.getAlerts());
                        data.getRef().updateChildren(updates); //update the alerts of the existing entry
                    }
                }
            }
        });
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

            DatabaseReference dbAlertsRef = FirebaseDB.getAlertReference();
            CompletableFuture<Boolean> newAlertId = new CompletableFuture<>();

            //check if the alert already exists in the db
            dbAlertsRef.orderByChild("timestamp").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    DataSnapshot dataSnapshot = task.getResult();

                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Alert alert = data.getValue(Alert.class);

                        if (alert != null) {
                            if (alert.getaId().equals(newAlert.getaId())) {
                                newAlertId.complete(false); //signal that the alert already exists in the db
                                return;
                            }
                        }
                    }
                    newAlertId.complete(true); //signal that the alert doesn't exist in the db
                }
            });

            newAlertId.thenAccept((newId) -> { // use the output of the future and push alert if new.
                if (newId) {
                    DatabaseReference newAlertRef = alert.push();
                    newAlertRef.setValue(newAlert)
                            .addOnSuccessListener( aVoid -> {
                                //Successfully added user
                                listener.alertAdded();
                            });
                }
                else {
                    listener.alertExists();
                }
            });
        }
    }

    public interface FirebaseAlertListener {
        void alertAdded();
        void alertExists();
    }

    //here we fetch the user alerts
    public static void getUserAlerts(String startDate, String endDate,Boolean fireCheckbox, Boolean floodCheckbox, Boolean tornadoCheckbox, Boolean earthquakeCheckbox, final FirebaseUserAlertGetterListener listener) {

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

                    if(temp != null && temp.getAlerts() != null){
                        temp.setAlerts(isWithInRange(temp.getAlerts(), startDate, endDate, fireCheckbox, floodCheckbox, tornadoCheckbox, earthquakeCheckbox));
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

    private static ArrayList<Alert> isWithInRange(ArrayList<Alert> temp, String startDate, String endDate, Boolean fireCheckbox, Boolean floodCheckbox, Boolean tornadoCheckbox, Boolean earthquakeCheckbox) {
        try {
            ArrayList<Alert> filtered = new ArrayList<Alert>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault());
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            temp.stream().forEach( a -> {
                try {
                    Date alertDate = sdf.parse(a.getTimestamp());

                    if(alertDate != null && alertDate.after(start) && alertDate.before(end)) {
                        if((fireCheckbox && a.getEventType() == EventTypes.FIRE)
                                || (floodCheckbox && a.getEventType() == EventTypes.FLOOD)
                                || (tornadoCheckbox && a.getEventType() == EventTypes.TORNADO)
                                || (earthquakeCheckbox && a.getEventType() == EventTypes.EARTHQUAKE)) {
                            filtered.add(a);
                        }
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            });

            return filtered;


        } catch (ParseException e) {
            e.printStackTrace();
            return temp;
        }
    }
    public interface FirebaseUserAlertGetterListener {
        void onUserAlertsRetrieved(UserAlerts userAlerts);
    }

}
