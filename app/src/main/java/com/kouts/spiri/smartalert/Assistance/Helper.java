package com.kouts.spiri.smartalert.Assistance;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.LoginActivity;
import com.kouts.spiri.smartalert.POJOs.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public abstract class Helper {
    public static User user;
    public static void showMessage(Context context, String title, String message){
        new AlertDialog.Builder(context).setTitle(title).setMessage(message).setCancelable(true).show();
    }

    public static void showToast(Context context, String text, int length) {
        Toast.makeText(context, text, length).show();
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

    //adds different options for selection in the spinner
    //"resourceId" defines the name of the array resource. For example "R.array.spinnerEventTypes" from the resource file "strings.xml"
    //"layoutId" defines the desired layout of the spinner an its options. E.g. "android.R.layout.simple_spinner_dropdown_item"
    public static void addOptionsToSpinner(Context context, int resourceId, int layoutId, Spinner spinner) { //add options to "Type of Event" spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context,
                resourceId,
                layoutId
        );
        //the layout to for the selection list
        adapter.setDropDownViewResource(layoutId);
        //let the spinner use the adapter
        spinner.setAdapter(adapter);
    }

    public static void validateCurrentUser(Context context) {
        if (FirebaseDB.getAuth().getUid() == null) { //if user not found go to login screen
            Helper.showToast(context, "Please log in", Toast.LENGTH_LONG);
            Intent intent = new Intent(context, LoginActivity.class);
            startActivity(context,intent,null);
        }
    }
}
