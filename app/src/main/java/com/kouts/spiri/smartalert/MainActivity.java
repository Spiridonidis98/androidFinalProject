package com.kouts.spiri.smartalert;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Helper.user == null) {
            getUserInfo(this.getCurrentFocus());

        }


    }

    public void getUserInfo(View view) {
        FirebaseDB.getUserInfo(FirebaseDB.getAuth().getUid(), new FirebaseDB.FirebaseUserListener() {
            @Override
            public void onUserRetrieved(User user) {
                Helper.user = user;
            }

            @Override
            public void onUserAdded() {

            }

            @Override
            public void onError(Exception e) {

            }
        });
    }
}