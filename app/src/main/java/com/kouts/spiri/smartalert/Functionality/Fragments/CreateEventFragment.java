package com.kouts.spiri.smartalert.Functionality.Fragments;

import static android.app.Activity.RESULT_OK;

import static com.kouts.spiri.smartalert.Assistance.Helper.timestampToDate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
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
import com.kouts.spiri.smartalert.Functionality.MainActivity;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CreateEventFragment extends Fragment {
    private View view;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_PERMISSION_CAMERA = 2;
    private Uri photoURI;
    private static final int READ_IMAGES_CODE = 1;
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

        // Check for camera permission
        if (ContextCompat.checkSelfPermission(view.getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
        } else {
            // Permission already granted
            dispatchTakePictureIntent();
        }

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
                    dispatchTakePictureIntent();
//                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                    activityResultLauncher.launch(intent); //open the android view that allows users to select image
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
            String message = getString(R.string.no_selected_event_type_found);
            Helper.showMessage(view.getContext(), "Error", message);
            return;
        }
        if (this.location == null) {
            String message = getString(R.string.location_not_found_please_try_again);
            Helper.showToast(view.getContext(), message, Toast.LENGTH_LONG);
            return;
        }
        if (comment.getText().toString().trim().isEmpty()) {
            String message = getString(R.string.please_add_a_comment);
            Helper.showToast(view.getContext(), message, Toast.LENGTH_LONG);
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

    //Functionality to take pictures from the camera
    //here we check for permissions
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                dispatchTakePictureIntent();
            } else {
                // Permission denied
                Helper.showToast(this.getContext(), "Camera permission is required", Toast.LENGTH_SHORT);
            }
        }
    }
}