package com.kouts.spiri.smartalert.Functionality;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.POJOs.User;
import com.kouts.spiri.smartalert.R;

public class MainActivity extends AppCompatActivity {
    ImageView settingsButton;
    Button reportEventButton;
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

        settingsButton = findViewById(R.id.settings);
        reportEventButton = findViewById(R.id.buttonReportEvent);


        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                UserView userView = UserView.newInstance();

                userView.show(fragmentManager, "user_view");
            }
        });

        reportEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), CreateEventActivity.class);
                startActivity(intent);
            }
        });
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