package com.kouts.spiri.smartalert;

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

    static {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        events = database.getReference("events");
    }

    public static FirebaseAuth getAuth() { return auth;}


}
