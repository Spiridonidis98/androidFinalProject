package com.kouts.spiri.smartalert.Functionality;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.R;

public class UserView extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), R.style.CustomDialog);

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.user_view, null);

        TextView titleTextView = view.findViewById(R.id.email);
        TextView nameLastname = view.findViewById(R.id.nameLastname);

        nameLastname.setText(Helper.user.getName() + " " + Helper.user.getLastname());
        Button logoutButton = view.findViewById(R.id.logout);

        titleTextView.setText(FirebaseDB.getAuth().getCurrentUser().getEmail());
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseDB.getAuth().signOut();
                Intent intent = new Intent(v.getContext(), LoginActivity.class);
                startActivity(intent);
                // Handle button click
                dismiss();
            }
        });

        Dialog dialog  = builder.setView(view).create();
        Window window = dialog.getWindow();

        if(window != null) {
            window.getAttributes().windowAnimations = R.style.CustomDialog;
            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }


        return dialog;
    }

    public static UserView newInstance() {
        return new UserView();
    }
}
