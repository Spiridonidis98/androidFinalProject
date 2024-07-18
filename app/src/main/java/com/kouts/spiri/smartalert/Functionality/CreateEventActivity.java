package com.kouts.spiri.smartalert.Functionality;

import static com.kouts.spiri.smartalert.Assistance.Helper.timestampToDate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.R;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {

    private static final long LOCATION_UPDATES_TIME = 3000; //miliseconds
    private static final int LOCATION_CODE = 0;
    private static final int READ_IMAGES_CODE = 1;
    Button addEventButton;
    Spinner spinner;
    String selectedSpinnerItem;
    EditText comment;
    ImageView image;
    LocationManager locationManager;
    double currentLongitude, currentLatitude;
    long timestamp;
    Uri selectedImage;

    LocationListener locationListener = location -> { //finds gps location and timestamp
        currentLongitude = location.getLongitude();
        currentLatitude = location.getLatitude();
        timestamp = location.getTime();
    };

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

        Helper.validateCurrentUser(this);

        addEventButton = findViewById(R.id.buttonSubmitEvent);
        comment = findViewById(R.id.editTextComments);
        image = findViewById(R.id.imageViewEventImage);
        spinner = findViewById(R.id.spinnerEventTypes);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager = getSystemService(LocationManager.class);

        selectImageListener();
        selectEventTypeListener();
        startLocationUpdates();
    }

    public void selectImageListener() {
        ActivityResultLauncher<Intent> activityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                selectedImage = result.getData().getData(); //get selected image
                                image.setImageURI(selectedImage); //shows the selected image in the ImageView
                            }
                        });

        image.setOnClickListener(l -> {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intent); //open the android view that allows users to select image
                }
        );
    }

    private void selectEventTypeListener() {
        Helper.addOptionsToSpinner(this.getApplicationContext(), R.array.spinnerEventTypes, android.R.layout.simple_spinner_dropdown_item, spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //when an item is selected get that item
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSpinnerItem = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    public void createEvent(View view) {
        //get selected event type
        EventTypes selectedEventType = Arrays.stream(EventTypes.values())
                .filter(eventType -> eventType.toString().equalsIgnoreCase(selectedSpinnerItem))
                .findFirst()
                .orElse(null);

        if (selectedEventType == null) {
            String message = getString(R.string.no_selected_event_type_found);
            Helper.showMessage(this, "Error", message);
            return;
        }
        if (currentLongitude == 0 || currentLatitude==0) {
            String message = getString(R.string.location_not_found_please_try_again);
            Helper.showToast(this, message, Toast.LENGTH_LONG);
            return;
        }
        if (comment.getText().toString().trim().isEmpty()) {
            String message = getString(R.string.please_add_a_comment);
            Helper.showToast(this, message, Toast.LENGTH_LONG);
            return;
        }

        Event event = null;
        if (selectedImage == null) { //do not include image to Event
            event = new Event(FirebaseDB.getAuth().getUid(), selectedEventType, currentLongitude, currentLatitude, timestampToDate(timestamp), comment.getText().toString(), "");
        } else { //include selected image to Event
            String imageUUID = UUID.randomUUID().toString();
            String userUID = FirebaseDB.getAuth().getUid();
            event = new Event(userUID, selectedEventType, currentLongitude, currentLatitude, timestampToDate(timestamp), comment.getText().toString(), imageUUID);
            uploadImageToFirebase(selectedImage, imageUUID, userUID);
        }
        FirebaseDB.addEvent(event, new FirebaseDB.FirebaseEventListener() {
            @Override
            public void onEventsRetrieved(List<Event> event) {};
            @Override
            public void onEventAdded() {
                String message = getString(R.string.event_submitted_successfully);
                Helper.showToast(view.getContext(), message, Toast.LENGTH_LONG);
                Intent intent = new Intent(view.getContext(), MainActivity.class);
                startActivity(intent);
            }

            @Override
            public void onError(Exception e) {
                String message = getString(R.string.unknown_error_occurred_event_could_not_be_submitted);
                Helper.showMessage(view.getContext(), "Error", message);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { //defines what happens when we the user allows or denies us permissions
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //request ACCESS_FINE_LOCATION permission
        if (requestCode == LOCATION_CODE) {
            if (ActivityCompat.checkSelfPermission(this, //checks that we have the permission IN THE MANIFEST
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATES_TIME, 0, locationListener); //updates the location, assuming we have the permission
            }
        }
        //request READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE permission depending on version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, READ_IMAGES_CODE); //actually asks the user for the permission
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_IMAGES_CODE); //actually asks the user for the permission
            }
        }
    }

    private void uploadImageToFirebase(Uri selectedImage, String ImageUUID, String userUID) {
        StorageReference storageRef = FirebaseDB.getStorageRef();
        StorageReference imageRef = storageRef.child("images/" + ImageUUID);
        imageRef.putFile(selectedImage) //upload image to storage
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("Upload image", "Image uploaded successfully: " + ImageUUID);

                    //upload image info to realtime db with the ImageUUID as id and userUID as value
                    DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("images");
                    databaseRef.child(ImageUUID).setValue(userUID)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Upload image info", "Image info uploaded successfully");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Upload image info", "Failed to upload image info", e);
                            });
                }).addOnFailureListener(e -> {
                    Log.e("Upload image", "Failed to upload image", e);
                });
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