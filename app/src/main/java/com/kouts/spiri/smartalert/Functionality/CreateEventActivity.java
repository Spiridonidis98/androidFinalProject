package com.kouts.spiri.smartalert.Functionality;

import static com.kouts.spiri.smartalert.Assistance.Helper.timestampToDate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.POJOs.User;
import com.kouts.spiri.smartalert.R;

import java.util.Arrays;

public class CreateEventActivity extends AppCompatActivity {

    FirebaseDatabase databaseFirebase;
    private static final long LOCATION_UPDATES_TIME = 3000; //miliseconds
    int LOCATION_CODE = 123;
    Button addEventButton;
    Spinner spinner;
    String selectedSpinnerItem;
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

        if (FirebaseDB.getAuth().getCurrentUser() == null) { //if user not found go to login screen
            Helper.showToast(this,"Please log in", Toast.LENGTH_LONG);
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        databaseFirebase = FirebaseDatabase.getInstance();
        dbRef = databaseFirebase.getReference();

        addEventButton = findViewById(R.id.buttonSubmitEvent);
        comment = findViewById(R.id.editTextComments);
        image = findViewById(R.id.editTextImage);
        spinner = findViewById(R.id.spinnerEventTypes);


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager = getSystemService(LocationManager.class);

        Helper.addOptionsToSpinner(this.getApplicationContext(), R.array.spinnerEventTypes, android.R.layout.simple_spinner_dropdown_item, spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //when an item is selected get that item
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSpinnerItem = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        startLocationUpdates();
    }

    LocationListener locationListener = location -> { //finds gps location and movement speed
        currentLongitude = location.getLongitude();
        currentLatitude = location.getLatitude();
        timestamp = location.getTime();
    };

    public void createEvent(View view) {
        if (currentLongitude != 0 && currentLatitude!=0) {

            EventTypes selectedEventType = Arrays.stream(EventTypes.values())
                    .filter(eventType -> eventType.toString().equalsIgnoreCase(selectedSpinnerItem))
                    .findFirst()
                    .orElse(null);

            if (selectedEventType != null) {
                Event event = new Event(selectedEventType, currentLongitude, currentLatitude, timestampToDate(timestamp), comment.getText().toString(), image.getText().toString());

                FirebaseDB.addEvent(event, new FirebaseDB.FirebaseEventListener() {
                    @Override
                    public void onEventRetrieved(Event event) {
                        return;
                    }

                    @Override
                    public void onEventAdded() {
                        Helper.showToast(view.getContext(), "Event submitted successfully", Toast.LENGTH_LONG);
                        Intent intent = new Intent(view.getContext(), MainActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(Exception e) {
                        Helper.showMessage(view.getContext(), "Error", "Unknown error occurred. Event could not be submitted");
                    }
                });
            }
            else {
                Helper.showMessage(this, "Error", "No selected event type found");
            }
        }
        else {
            Helper.showToast(this,"Location not found, please try again", Toast.LENGTH_LONG); //not sure if this.getCurrentFocus() works
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