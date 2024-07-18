package com.kouts.spiri.smartalert.Functionality;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

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

        startLocationService(this);

        Helper.validateCurrentUser(this);
        if (Helper.user == null) {
            getUserInfo(this.getCurrentFocus());

        }

        //for bottom navigation
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if(Helper.user.getType() == 0) {
            getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, civilSafetyFunctionalityFragment).commit();
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_civil);
            bottomNavigationView.setSelectedItemId(R.id.civilSafetyFunctionality);
        }
        else {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        if (requestCode == LOCATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService(this);
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    public void startLocationService(Context context) {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_CODE); //actually asks the user for the permission

            //Permission not granted
            return;
        }

        Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);
    }

}