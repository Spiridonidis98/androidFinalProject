package com.kouts.spiri.smartalert.Assistance;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.Activities.LoginActivity;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.POJOs.User;
import com.kouts.spiri.smartalert.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class Helper {
    private static final String PREFERENCE_NAME = "app_language";
    private static final String LANGUAGE_KEY = "language_key";
    private static String language = "en";
    private static User user;

    // Static method to set the user
    public static void setUser(User newUser) {
        user = newUser;
    }

    // Static method to get the current user
    public static User getUser() {
        return user;
    }

    // Static method to clear the user
    public static void clearUser() {
        user = null;
    }
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
        String message = context.getString(R.string.please_log_in);
        if (FirebaseDB.getAuth().getCurrentUser() == null) { //if user not found in authentication go to login screen
            Helper.showToast(context, message, Toast.LENGTH_LONG);
            Intent intent = new Intent(context, LoginActivity.class);
            startActivity(context,intent,null);
        } else {
            String userId = FirebaseDB.getAuth().getCurrentUser().getUid();
            FirebaseDB.getUserReference().orderByChild("uid").equalTo(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (! task.getResult().exists()) { //if user not found in realtime database go to login screen
                        Helper.showToast(context, message, Toast.LENGTH_LONG);
                        Intent intent = new Intent(context, LoginActivity.class);
                        startActivity(context, intent, null);
                    }
                }
            });
        }
    }

    //here we will return the first day of the week
    public static String getStartOfWeek() {
        Calendar calendar = Calendar.getInstance();

        // Set the first day of the week to Monday
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        while( calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_WEEK, -1);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    public static String getToday() {
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    public static String convertDateFormat(String originalDate) {
        // Split the original date string
        String[] parts = originalDate.split("/");

        // Rearrange the parts to form "yyyy-M-d" format
        String year = parts[2];
        String month = parts[1];
        String day = parts[0];

        if(month.length() == 1) {
            month = "0" + month;
        }

        if(day.length() == 1) {
            day = "0" + day;
        }

        // Join the parts with "-"
        String convertedDate = year + "-" + month + "-" + day;

        return convertedDate;
    }

    //calculate the distance between 2 points on the map
    public static double calculateGeoDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371.0; //radius of the earth in km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public static void setLanguage(String lang) {
        language = lang;
    }

    public static String getLanguage() {
        return language;
    }

    public static void setLocale(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        android.content.res.Resources resources = context.getResources();
        android.content.res.Configuration config = new android.content.res.Configuration(resources.getConfiguration());
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        saveLanguagePreference(context, lang);
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getString(LANGUAGE_KEY, "en"); // Default to English if no preference is found
    }

    private static void saveLanguagePreference(Context context, String lang) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LANGUAGE_KEY, lang);
        editor.apply();
    }

    public static int getColorForEvent(EventTypes type) {
        switch (type) {
            case FIRE:
                return Color.parseColor("#AA4203");
            case FLOOD:
                return Color.parseColor("#0000FF");
            case TORNADO:
                return Color.parseColor("#808080");
            case EARTHQUAKE:
                return Color.parseColor("#8B4513");
            default:
                return Color.parseColor("#FFFFFF");
        }
    }

    public static int calculateSpanCount(Resources resources) {
        int screenWidth = resources.getDisplayMetrics().widthPixels;
        int itemWidth = 600; // Adjust this value according to your item dimensions
        return screenWidth / itemWidth;
    }

    public static int countTextPattern(String text,String regex) {
        Pattern pattern = Pattern.compile(regex); //set the pattern based on given regex
        Matcher matcher = pattern.matcher(text); //find the pattern in the given text

        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public static SQLiteDatabase createLocalDB(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase("userPreferences.db",Context.MODE_PRIVATE,null);

        database.execSQL("Create table if not exists Preferences(" +
                "UID TEXT PRIMARY KEY," +
                "selectedEventType TEXT," +
                "notifCheckboxFire INTEGER," + //booleans: 1 symbolizes TRUE, 0 is FALSE
                "notifCheckboxFlood INTEGER," +
                "notifCheckboxEarthquake INTEGER,"+
                "notifCheckboxTornado INTEGER)");

        Cursor cursor = database.rawQuery("Select * from Preferences WHERE UID = ? LIMIT 1" , new String[]{FirebaseDB.getAuth().getUid()});
        if (! cursor.moveToFirst()) {
            String[] data = {FirebaseDB.getAuth().getUid(),"FIRE","1","1","1","1"};
            database.execSQL("Insert or ignore into Preferences values(?,?,?,?,?,?)", data);
        }
        //database.execSQL("Delete from Preferences");

        return database;
    }
}
