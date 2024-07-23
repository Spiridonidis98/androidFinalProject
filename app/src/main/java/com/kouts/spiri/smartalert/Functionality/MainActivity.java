package com.kouts.spiri.smartalert.Functionality;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.Fragments.CivilSafetyFunctionalityFragment;
import com.kouts.spiri.smartalert.Functionality.Fragments.CreateEventFragment;
import com.kouts.spiri.smartalert.Functionality.Fragments.EventStatisticsFragment;
import com.kouts.spiri.smartalert.POJOs.User;
import com.kouts.spiri.smartalert.R;
import com.kouts.spiri.smartalert.Services.LocationService;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_CODE = 0;
    private static final int ALERT_CODE = 1;
    BottomNavigationView bottomNavigationView;
    EventStatisticsFragment eventStatisticsFragment = new EventStatisticsFragment();
    CreateEventFragment createEventFragment = new CreateEventFragment();
    CivilSafetyFunctionalityFragment civilSafetyFunctionalityFragment = new CivilSafetyFunctionalityFragment();

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

        startNecessaryServices(this);

        // Get user info and update UI after retrieval
        getUserInfo(new UserInfoCallback() {
            @Override
            public void onUserInfoRetrieved(User user) {
                setupBottomNavigation(user);
            }

            @Override
            public void onError(Exception e) {
                Log.d("USER INFO ERROR", "Failed to retrieve user info", e);
                // Handle the error, maybe show a default UI or message
            }
        });
    }

    private void setupBottomNavigation(User user) {
        if (user != null) {
            Helper.setUser(user);

            // Initialize bottom navigation
            bottomNavigationView = findViewById(R.id.bottomNavigationView);

            if (user.getType() == 0) {
                getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, civilSafetyFunctionalityFragment).commit();
                bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_civil);
                bottomNavigationView.setSelectedItemId(R.id.civilSafetyFunctionality);
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, createEventFragment).commit();
                bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu);
                bottomNavigationView.setSelectedItemId(R.id.createEventFragment);
            }

            bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    if(menuItem.getItemId() ==  R.id.eventStatisticsFragment) {
                        getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, eventStatisticsFragment).commit();
                    }
                    else if(menuItem.getItemId() == R.id.createEventFragment) {
                        getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, createEventFragment).commit();
                    }
                    else if(menuItem.getItemId() == R.id.personInfo) {
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        UserView userView = UserView.newInstance();
                        userView.show(fragmentManager, "user_view");
                    }
                    else if (menuItem.getItemId() == R.id.civilSafetyFunctionality) {
                        getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, civilSafetyFunctionalityFragment).commit();
                    }
                    return true;
                }
            });
        } else {
            Log.d("USER INFO ERROR", "User is null, cannot setup bottom navigation");
        }
    }

    public void getUserInfo(UserInfoCallback callback) {
        FirebaseDB.getUserInfo(FirebaseDB.getAuth().getUid(), new FirebaseDB.FirebaseUserListener() {
            @Override
            public void onUserRetrieved(User user) {
                if (user != null) {
                    callback.onUserInfoRetrieved(user);
                } else {
                    callback.onError(new Exception("User not found"));
                }
            }

            @Override
            public void onUserAdded() {
                // Handle user added if necessary
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    // Define a callback interface for user info retrieval
    public interface UserInfoCallback {
        void onUserInfoRetrieved(User user);
        void onError(Exception e);
    }

    // Handle location permissions and start service
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_CODE ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNecessaryServices(this);
            } else {
                // Permission denied, handle accordingly
            }
        }
        if (requestCode == ALERT_CODE ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNecessaryServices(this);
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    public void startNecessaryServices(Context context) {
        boolean scheduleExactAlarmPerm = false;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d("MainActivity", "startNecessaryServices: alertService request permissions");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM}, ALERT_CODE);
            } else {
                Log.d("MainActivity", "startNecessaryServices: old version, could not start alert notification service");
            }
        } else {
            scheduleExactAlarmPerm = true;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //if permission not already granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_CODE); // Request location permission
        } else {
            Intent locationService = new Intent(this, LocationService.class);
            locationService.putExtra("permission_granted",scheduleExactAlarmPerm);
            startService(locationService);
        }
    }
}