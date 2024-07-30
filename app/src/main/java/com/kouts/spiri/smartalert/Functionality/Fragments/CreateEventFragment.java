package com.kouts.spiri.smartalert.Functionality.Fragments;

import static android.app.Activity.RESULT_OK;

import static com.kouts.spiri.smartalert.Assistance.Helper.timestampToDate;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.Activities.MainActivity;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.R;
import com.kouts.spiri.smartalert.Functionality.Background_Functions.LocationService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CreateEventFragment extends Fragment {
    private View view;

    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final int EXCLAMATION_THRESHOLD = 3; //minimum number of exclamations in a comment that result in a warning message

    private static final int READ_IMAGES_CODE = 1;
    private static final int REQUEST_PERMISSION = 200;
    Button addEventButton;
    Spinner spinner;
    String selectedSpinnerItem;
    EditText comment;
    ImageView fileImage, cameraImage, showImage;
    EventTypes selectedEventType;
    double currentLatitude,currentLongitude;
    long timestamp;
    Uri selectedImage;
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

        addEventButton.setOnClickListener(v -> checkEventValidity(v));

        comment = view.findViewById(R.id.editTextComments);
        fileImage = view.findViewById(R.id.openFile);
        cameraImage = view.findViewById(R.id.openCamera);
        showImage = view.findViewById(R.id.imageShower);
        spinner = view.findViewById(R.id.spinnerEventTypes);

        fileImage.setImageResource(R.drawable.file);
        cameraImage.setImageResource(R.drawable.camera);


        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        };

        selectImageListener();
        selectEventTypeListener();

        return view;
    }

    public void openCamera() {
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        selectedImage = saveBitmapToFile(imageBitmap);
                        showImage.setImageBitmap(imageBitmap); // Shows the captured image in the ImageView
                        showImage.setVisibility(View.VISIBLE);
                    }
                });
        cameraImage.setOnClickListener(l -> {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            activityResultLauncher.launch(cameraIntent);
        });
    }

    private Uri saveBitmapToFile(Bitmap bitmap) {
        File cacheDir = requireContext().getCacheDir();
        File imageFile = new File(cacheDir, "captured_image.jpg");
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(imageFile);
    }
    public void selectImageListener() {
        ActivityResultLauncher<Intent> activityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                selectedImage = result.getData().getData(); //get selected image
                                showImage.setImageURI(selectedImage); //shows the selected image in the ImageView
                                showImage.setVisibility(View.VISIBLE);
                            }
                        });

        fileImage.setOnClickListener(l -> {
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

    public void checkEventValidity(View v) {
        //get selected event type
        selectedEventType = Arrays.stream(EventTypes.values())
                .filter(eventType -> eventType.toString().equalsIgnoreCase(selectedSpinnerItem))
                .findFirst()
                .orElse(null);

        if (selectedEventType == null) {
            String message = getString(R.string.no_selected_event_type_found);
            Helper.showMessage(view.getContext(), "Error", message);
            return;
        }
        currentLatitude = LocationService.getLocationLatitude();
        currentLongitude = LocationService.getLocationLongitude();
        timestamp = LocationService.getLocationTime();

        if (currentLatitude == 0 || currentLongitude == 0 || timestamp == 0) {
            String message = getString(R.string.location_not_found_please_try_again);
            Helper.showToast(view.getContext(), message, Toast.LENGTH_LONG);
            return;
        }
        if (comment.getText().toString().trim().isEmpty()) {
            String message = getString(R.string.please_add_a_comment);
            Helper.showToast(view.getContext(), message, Toast.LENGTH_LONG);
            return;
        }

        String commentText = comment.getText().toString();
        int commentExclamations = Helper.countTextPattern(commentText, "!");

        //if there are many exclamations or all CAPS suggest that the user calls emergency services
        boolean possibleEmergency = commentExclamations >= EXCLAMATION_THRESHOLD || commentText.equals(commentText.toUpperCase());
        if (possibleEmergency) {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            String message = getString(R.string.suggest_emergency_services_call);
            builder.setTitle("Warning");
            builder.setMessage(message);

            builder.setNeutralButton("OK", (dialog, which) -> {
                if (which == DialogInterface.BUTTON_NEUTRAL) {
                    createEvent(v);
                }
            });
            builder.setOnCancelListener(dialog -> createEvent(v));
            builder.show();
            return; //stop execution since execution will continue appropriately when the user closes the AlertDialog
        }


        //if there is a long comment but no image, assume the situation is not critical and ask if the user wants to add an image before adding the event
        if (commentText.length() >= 150) {
            if (selectedImage != null) {
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Warning");
            String message = getString(R.string.proceed_without_an_image);
            builder.setMessage(message);

            String positiveText = getString(R.string.yes);
            String negativeText = getString(R.string.no);
            builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Handle Yes button click
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        createEvent(v);
                    }
                }
            });
            builder.setNegativeButton(negativeText, (dialog, which) -> {
                // Handle No button click
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    return;
                }
            });
            builder.show();
        } else { //if the comment is short simply create the event
            createEvent(v);
        }

    }
    public void createEvent(View v) {
        Event event = null;
        if (selectedImage == null) { //do not include image to Event
            event = new Event(FirebaseDB.getAuth().getUid(), selectedEventType, currentLatitude, currentLongitude, timestampToDate(timestamp), comment.getText().toString(), "");
        } else { //include selected image to Event
            String imageUUID = UUID.randomUUID().toString();
            String userUID = FirebaseDB.getAuth().getUid();
            event = new Event(userUID, selectedEventType, currentLatitude ,currentLongitude, timestampToDate(timestamp), comment.getText().toString(), imageUUID);
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

}