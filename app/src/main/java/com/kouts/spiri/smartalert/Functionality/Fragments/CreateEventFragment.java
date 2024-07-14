package com.kouts.spiri.smartalert.Functionality.Fragments;

import static android.app.Activity.RESULT_OK;

import static com.kouts.spiri.smartalert.Assistance.Helper.timestampToDate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Assistance.UserLocation;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.MainActivity;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.R;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CreateEventFragment extends Fragment implements UserLocation.LocationCallBackListener{
    private View view;

    private static final int READ_IMAGES_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 22;
    private static final int REQUEST_PERMISSION = 200;
    Button addEventButton;
    Spinner spinner;
    String selectedSpinnerItem;
    EditText comment;
    ImageView image;
    double currentLongitude, currentLatitude;
    long timestamp;
    Uri selectedImage;
    private Location location;
    UserLocation userLocation;
    public CreateEventFragment() {
        // Required empty public constructor
    }

    public static CreateEventFragment newInstance(String param1, String param2) {
        CreateEventFragment fragment = new CreateEventFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_create_event, container, false);


        Helper.validateCurrentUser(view.getContext());

        addEventButton = view.findViewById(R.id.buttonSubmitEvent);

        addEventButton.setOnClickListener(v -> createEvent(v));

        comment = view.findViewById(R.id.editTextComments);
        image = view.findViewById(R.id.imageViewEventImage);
        spinner = view.findViewById(R.id.spinnerEventTypes);

        image.setImageResource(R.drawable.camera);

        selectImageListener();
        selectEventTypeListener();

        userLocation = new UserLocation(view.getContext(),this.getActivity(),this);

        userLocation.startLocationUpdates();

        this.location = userLocation.getLocation();
//        startLocationUpdates();

        return view;
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
        Helper.addOptionsToSpinner(view.getContext(), R.array.spinnerEventTypes, android.R.layout.simple_spinner_dropdown_item, spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //when an item is selected get that item
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSpinnerItem = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    public void createEvent(View v) {
        //get selected event type
        EventTypes selectedEventType = Arrays.stream(EventTypes.values())
                .filter(eventType -> eventType.toString().equalsIgnoreCase(selectedSpinnerItem))
                .findFirst()
                .orElse(null);

        if (selectedEventType == null) {
            Helper.showMessage(view.getContext(), "Error", "No selected event type found");
            return;
        }
        if (this.location == null) {
            Helper.showToast(view.getContext(), "Location not found, please try again", Toast.LENGTH_LONG);
            return;
        }
        if (comment.getText().toString().trim().isEmpty()) {
            Helper.showToast(view.getContext(), "Please add a comment", Toast.LENGTH_LONG);
            return;
        }

        Event event = null;
        if (selectedImage == null) { //do not include image to Event
            event = new Event(FirebaseDB.getAuth().getUid(), selectedEventType, currentLongitude, currentLatitude, timestampToDate(timestamp), comment.getText().toString(), "");
        } else { //include selected image to Event
            String imageUUID = UUID.randomUUID().toString();
            String userUID = FirebaseDB.getAuth().getUid();
            event = new Event(userUID, selectedEventType, this.location.getLongitude(), this.location.getLatitude(), timestampToDate(this.location.getTime()), comment.getText().toString(), imageUUID);
            uploadImageToFirebase(selectedImage, imageUUID, userUID);
        }
        FirebaseDB.addEvent(event, new FirebaseDB.FirebaseEventListener() {
            @Override
            public void onEventsRetrieved(List<Event> event) {};
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


    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    //Functionality to take pictures from the camera
    //here we check for permissions


}