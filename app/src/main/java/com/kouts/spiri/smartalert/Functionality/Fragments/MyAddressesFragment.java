package com.kouts.spiri.smartalert.Functionality.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Assistance.NotificationAdapter;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.POJOs.Address;
import com.kouts.spiri.smartalert.R;

public class MyAddressesFragment extends BottomSheetDialogFragment implements CreateAddressFragment.OnFragmentDismissListener {

    View myAddressesFragment;
    public MyAddressesFragment() {
        // Use the custom style
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
    }

    public static MyAddressesFragment newInstance(String param1, String param2) {
        MyAddressesFragment fragment = new MyAddressesFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    //setting header view
    private void settingHeader(View view) {
        ImageView backButton = view.findViewById(R.id.header_back_button);
        TextView titleText = view.findViewById(R.id.header_title);

        //setting header views
        titleText.setText(R.string.my_locations);
        backButton.setOnClickListener(v -> getDialog().dismiss());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        myAddressesFragment = inflater.inflate(R.layout.fragment_my_addresses, container, false);

        //setting header view
        settingHeader(myAddressesFragment);

        //setting locations
        settingLocations(myAddressesFragment);

        ImageView addButton = myAddressesFragment.findViewById(R.id.add_address);
        addButton.setOnClickListener(v -> this.addNewAddress());

        return myAddressesFragment;
    }

    //open map fragment to add new location
    private void addNewAddress() {
        CreateAddressFragment createAddressFragment =CreateAddressFragment.newInstance(0, 0, 0, "", false);

        createAddressFragment.show(getChildFragmentManager(), "myAddresses");
        createAddressFragment.setOnFragmentDismissListener(this);
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
                BottomSheetBehavior behavior = (BottomSheetBehavior) params.getBehavior();
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

    //setting dynamically the locations
    private void settingLocations(View view) {
        LinearLayout addressesLayout = view.findViewById(R.id.my_saved_addresses);
        addressesLayout.removeAllViewsInLayout();

        if(Helper.getUser() != null &&  Helper.getUser().getAddresses().isEmpty()) {
            TextView noAddresses = new TextView(getContext());
            noAddresses.setText(R.string.no_addresses);
            noAddresses.setGravity(Gravity.CENTER);
            addressesLayout.addView(noAddresses);
            return;
        }

        LinearLayout my_saved_addresses = view.findViewById(R.id.my_saved_addresses);

        for(Address address: Helper.getUser().getAddresses()) {
            // Create a new LinearLayout
            LinearLayout linearAddress = new LinearLayout(getContext());

            // Set layout parameters for the new LinearLayout
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 20, 0, 0); // Optional: set margins if needed

            linearAddress.setLayoutParams(params);
            linearAddress.setOrientation(LinearLayout.HORIZONTAL);
            linearAddress.setGravity(Gravity.CENTER_VERTICAL);
            linearAddress.setPadding(20, 20, 20, 20);
            // Set the background drawable
            Drawable backgroundDrawable = ContextCompat.getDrawable(getContext(), R.drawable.left_side_color);
            linearAddress.setBackground(backgroundDrawable);

            TextView addressTextView = new TextView(getContext());
            addressTextView.setText(address.getDescription()); // Customize this to display address details as needed
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0.8f // Weight to occupy 80% of the width
            );
            addressTextView.setLayoutParams(textParams);
            linearAddress.addView(addressTextView);

            //DELETE IMAGE ICON
            ImageView deleteView = new ImageView(getContext());
            deleteView.setImageResource(R.drawable.trash);
            deleteView.setForegroundGravity(1);
            int imageSize = (int) getResources().getDimension(R.dimen.image_size);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            deleteView.setLayoutParams(imageParams);
            deleteView.setOnClickListener(v -> this.deleteLocation(address));
            linearAddress.addView(deleteView);


            linearAddress.setOnClickListener(v -> this.openMapFragment(address));
            // Add the new LinearLayout to the parent layout
            my_saved_addresses.addView(linearAddress);
        }
    }

    //here we delete the selected location
    private void deleteLocation(Address address) {
        Helper.getUser().getAddresses().remove(address);
        FirebaseDB.updateUser(Helper.getUser(), new FirebaseDB.FireBaseUpdateUserListener() {
            @Override
            public void onUpdateUser() {
                Helper.showToast(getContext(), "Address deleted successfully", Toast.LENGTH_LONG);
                settingLocations(myAddressesFragment);
            }

            @Override
            public void onError(Exception e) {
                Helper.showToast(getContext(), "Error adding new address", Toast.LENGTH_LONG);
            }
        });
    }

    //here we open the selected location
    private void openMapFragment(Address address) {
        CreateAddressFragment createAddressFragment =CreateAddressFragment.newInstance(address.getId(), address.getLat(), address.getLon(), address.getDescription(), true);
        createAddressFragment.show(getChildFragmentManager(), "myAddresses");
        createAddressFragment.setOnFragmentDismissListener(this);

    }

    @Override
    public void onFragmentDismissed() {
        settingLocations(myAddressesFragment);
    }
}