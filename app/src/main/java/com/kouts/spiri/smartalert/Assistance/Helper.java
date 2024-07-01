package com.kouts.spiri.smartalert.Assistance;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.MainActivity;
import com.kouts.spiri.smartalert.POJOs.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class Helper {
    public static User user;
    public static void showMessage(View view, String title, String message){
        new AlertDialog.Builder(view.getContext()).setTitle(title).setMessage(message).setCancelable(true).show();
    }

    public static void showToast(View view, String text, int length) {
        Toast.makeText(view.getContext(), text, length).show();
    }

    public static String timestampToDate(long timestamp) { //turn timestamp to date
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //create specific format
        return sdf.format(date);
    }
    public static long dateToTimestamp(String dateString) { //turn date to timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = null;
        try {
            date = sdf.parse(dateString);
            if (date != null) {
                return date.getTime();
            } else {
                throw new RuntimeException();
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
