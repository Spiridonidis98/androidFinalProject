package com.kouts.spiri.smartalert.Functionality.Fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.Background_Functions.LocationService;
import com.kouts.spiri.smartalert.POJOs.Address;
import com.kouts.spiri.smartalert.R;

public class CreateAddressFragment extends BottomSheetDialogFragment {

    private double lat;
    private double lon;
    EditText description;

    public CreateAddressFragment() {
        // Use the custom style
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
    }

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            // Get location from LocationService
            lat = LocationService.getLocationLatitude();
            lon = LocationService.getLocationLongitude();

            LatLng location;
            if (lat != 0.0 && lon != 0.0) { // Ensure the location is valid
                location = new LatLng(lat, lon);

            }
            else {
                location = new LatLng(100, 100);
            }
            googleMap.addMarker(new MarkerOptions().position(location).title("Your location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(location));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location,12f));

            googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull LatLng latLng) {
                    googleMap.clear();
                    googleMap.addMarker(new MarkerOptions().position(latLng));
                    lat = latLng.latitude;
                    lon = latLng.longitude;
                }
            });
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_address, container, false);
        settingHeader(view);

        description = view.findViewById(R.id.addressName);

        Button saveButton = view.findViewById(R.id.save_button);

        saveButton.setOnClickListener(v -> this.save_address());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.createAddressMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
                BottomSheetBehavior<?> behavior = (BottomSheetBehavior<?>) params.getBehavior();
                if (behavior != null) {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                }
            }
        }
    }

    @SuppressLint("ResourceType")
    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        Context context = getContext();
        if (context == null) return null;

        int animResId = enter ? R.anim.slide_in_up_fragment : R.anim.slide_out_down_fragment;
        return AnimatorInflater.loadAnimator(context, animResId);
    }

    //setting header view
    private void settingHeader(View view) {
        ImageView backButton = view.findViewById(R.id.header_back_button);
        //setting header views
        backButton.setOnClickListener(v -> getDialog().dismiss());
    }

    private void save_address() {
        if(description.getText().toString().isEmpty()) {
            Helper.showMessage(getContext(), "Warning", "No added description");
            return;
        }

        Address newAddress = new Address(lat, lon, description.getText().toString());

        Helper.getUser().addAddress(newAddress);

        FirebaseDB.updateUser(Helper.getUser(), new FirebaseDB.FireBaseUpdateUserListener() {
            @Override
            public void onUpdateUser() {
                Helper.showToast(getContext(), "New address added successfully", Toast.LENGTH_LONG);
                getDialog().dismiss();
            }

            @Override
            public void onError(Exception e) {
                Helper.showToast(getContext(), "Error adding new address", Toast.LENGTH_LONG);
            }
        });

    }
}
