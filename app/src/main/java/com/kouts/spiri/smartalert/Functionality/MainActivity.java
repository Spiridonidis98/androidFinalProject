package com.kouts.spiri.smartalert.Functionality;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.kouts.spiri.smartalert.Functionality.Fragments.CreateEventFragment;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Functionality.Fragments.EventStatisticsFragment;
import com.kouts.spiri.smartalert.POJOs.User;
import com.kouts.spiri.smartalert.R;
import com.kouts.spiri.smartalert.Assistance.UserLocation;
public class MainActivity extends AppCompatActivity implements UserLocation.LocationCallBackListener {
    private static final int MENU_EVENT_STATISTICS = R.id.eventStatisticsFragment;
    private static final int MENU_CREATE_EVENT = R.id.createEventFragment;
    BottomNavigationView bottomNavigationView;

    EventStatisticsFragment eventStatisticsFragment = new EventStatisticsFragment();
    CreateEventFragment createEventFragment = new CreateEventFragment();
    private UserLocation userLocation;
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

        userLocation = new UserLocation(this,this,this);
        Helper.validateCurrentUser(this);
        if (Helper.user == null) {
            getUserInfo(this.getCurrentFocus());

        }
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, eventStatisticsFragment).commit();

        reportEventButton = findViewById(R.id.buttonReportEvent);
        Button buttonWorkerTest = findViewById(R.id.buttonWorkerTest);
        Button buttonEventStatistics = findViewById(R.id.buttonEventStatistics);



        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                if(menuItem.getItemId() == MENU_EVENT_STATISTICS) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, eventStatisticsFragment).commit();
                }
                else if(menuItem.getItemId() == MENU_CREATE_EVENT) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, createEventFragment).commit();
                }
                else {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    UserView userView = UserView.newInstance();

                    userView.show(fragmentManager, "user_view");
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        userLocation.startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        userLocation.stopLocationUpdates();
        reportEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), CreateEventActivity.class);
                startActivity(intent);
            }
        });
        buttonEventStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), EventStatisticsActivity.class);
                startActivity(intent);
            }
        });
        buttonWorkerTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), RecommendEventsManagerActivity.class);
                startActivity(intent);
            }
        });
    }

    public void getUserInfo(View view) {
        FirebaseDB.getUserInfo(FirebaseDB.getAuth().getUid(), new FirebaseDB.FirebaseUserListener() {
            @Override
            public void onUserRetrieved(User user) {
                if (user != null) {
                    Helper.user = user;
                }
                else {
                    Log.d("USER NOT FOUND ERROR", "User not found in the database");
                }
            }

            @Override
            public void onUserAdded() {

            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    //here we request Location Permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == UserLocation.LOCATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                userLocation.startLocationUpdates();
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }
}