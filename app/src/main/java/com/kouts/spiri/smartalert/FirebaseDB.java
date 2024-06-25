package com.kouts.spiri.smartalert;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;
import java.util.List;
public class FirebaseDB {

    private static final FirebaseAuth auth;
    private static final FirebaseDatabase database;
    private static final DatabaseReference events;
    private static final DatabaseReference user;

    static {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        events = database.getReference("events");
        user = database.getReference("user");
    }

    public static FirebaseAuth getAuth() { return auth;}

    public static void addUser(User newUser, final  FirebaseUserListener listener) {
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
}
