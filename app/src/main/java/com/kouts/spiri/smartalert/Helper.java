package com.kouts.spiri.smartalert;

import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;


public class Helper {
    public static User user;
    public static void showMessage(View view, String title, String message){
        new AlertDialog.Builder(view.getContext()).setTitle(title).setMessage(message).setCancelable(true).show();
    }

    public static void showToast(View view, String text, int length) {
        Toast.makeText(view.getContext(), text, length).show();

    }
}
