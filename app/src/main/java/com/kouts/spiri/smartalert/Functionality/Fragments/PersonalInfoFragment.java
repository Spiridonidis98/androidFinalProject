package com.kouts.spiri.smartalert.Functionality.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.POJOs.User;
import com.kouts.spiri.smartalert.R;

public class PersonalInfoFragment extends BottomSheetDialogFragment {
    EditText email,name,lastname,password,confirmPassword;
    public PersonalInfoFragment() {
        // Use the custom style
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View personalInfoView =  inflater.inflate(R.layout.fragment_personal__info, container, false);

        settingHeader(personalInfoView);
        initializeInputValues(personalInfoView);

        return personalInfoView;
    }

    //here we initialize the values
    private void initializeInputValues(View view) {
        email = view.findViewById(R.id.editEmail);
        name = view.findViewById(R.id.editName);
        lastname = view.findViewById(R.id.editLastname);
        password = view.findViewById(R.id.editPassword);
        confirmPassword = view.findViewById(R.id.confirmPassword);

        email.setText(Helper.getUser().getEmail());
        name.setText(Helper.getUser().getName());
        lastname.setText(Helper.getUser().getLastname());

        Button updateButton = view.findViewById(R.id.updateButton);
        updateButton.setOnClickListener(v -> this.updateUserInfo());
    }

    private void updateUserInfo() {
        User editUser = new User(Helper.getUser().getUid(), email.getText().toString(), name.getText().toString(), lastname.getText().toString(), Helper.getUser().getType());

        FirebaseDB.updateUser(editUser, new FirebaseDB.FireBaseUpdateUserListener() {
            @Override
            public void onUpdateUser() {
                Helper.showMessage(getContext(), "Success", "User Info updated");
            }

            @Override
            public void onError(Exception e) {
                Helper.showMessage(getContext(), "Error", "User Info failed");
            }
        });
    }

    //Here we set the values for the header
    private void settingHeader(View view) {
        ImageView backButton = view.findViewById(R.id.header_back_button);
        TextView titleText = view.findViewById(R.id.header_title);

        //setting header views
        titleText.setText(R.string.personal_info);
        backButton.setOnClickListener(v -> getDialog().dismiss());
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
}