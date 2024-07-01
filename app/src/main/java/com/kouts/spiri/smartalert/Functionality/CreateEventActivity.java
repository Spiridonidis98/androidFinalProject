package com.kouts.spiri.smartalert.Functionality;

import static com.kouts.spiri.smartalert.Assistance.Helper.timestampToDate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.R;

public class CreateEventActivity extends AppCompatActivity {

    FirebaseDatabase databaseFirebase;
    private static final long LOCATION_UPDATES_TIME = 3000; //miliseconds
    int LOCATION_CODE = 123;
    Button addEventButton;
    EditText comment;
    EditText image;
    LocationManager locationManager;
    private double currentLongitude, currentLatitude;
    long timestamp;
    DatabaseReference dbRef;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (FirebaseDB.getAuth().getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        databaseFirebase = FirebaseDatabase.getInstance();
        dbRef = databaseFirebase.getReference();

        addEventButton = findViewById(R.id.buttonAddEvent);
        comment = findViewById(R.id.editTextComments);
        image = findViewById(R.id.editTextImage);

        startLocationUpdates();
    }


    LocationListener locationListener = location -> { //finds gps location and movement speed
        currentLongitude = location.getLongitude();
        currentLatitude = location.getLatitude();
        timestamp = location.getTime();
    };

    public void createEvent() {
        if (currentLongitude != 0 && currentLatitude!=0) {
            Event event = new Event(EventTypes.EARTHQUAKE, currentLongitude, currentLatitude, timestampToDate(timestamp),comment.toString(),image.toString());
            dbRef.push().setValue(event);
        }
        else {
            Helper.showToast(this.getCurrentFocus(),"Location not found, please try again", Toast.LENGTH_LONG); //not sure if this.getCurrentFocus() works
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { //defines what happens when we the user allows or denies us permissions
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode==LOCATION_CODE) {
            if (ActivityCompat.checkSelfPermission(this, //checks that we have the permission IN THE MANIFEST
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,LOCATION_UPDATES_TIME, 0, locationListener); //updates the location, assuming we have the permission
            }
        }
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, //checks that we have the permission IN THE MANIFEST
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //if we don't have it :

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_CODE); //actually asks the user for the permission
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATES_TIME, 0, locationListener); //updates the location, assuming we have the permission
    }


}