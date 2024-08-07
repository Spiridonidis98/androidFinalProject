package com.kouts.spiri.smartalert.Functionality.Fragments;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Assistance.NotificationAdapter;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.POJOs.Alert;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.POJOs.UserAlerts;
import com.kouts.spiri.smartalert.R;

import java.util.Calendar;
import java.util.List;

public class NotificationListFragment extends Fragment {
    private Button buttonDatePickerStart, buttonDatePickerEnd, searchButton;
    private CheckBox fireCheckbox, earthquakeCheckbox, tornadoCheckbox, floodCheckbox;
    private View view;
    private RecyclerView recyclerView;
    SQLiteDatabase database;

    public NotificationListFragment() {
        // Required empty public constructor
    }

    public static NotificationListFragment newInstance(String param1, String param2) {
        NotificationListFragment fragment = new NotificationListFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_notification_list, container, false);

        database = Helper.createLocalDB(view.getContext());

        initializeCheckboxes();
        initializeDatepickers();
        searchButton = view.findViewById(R.id.getEvents);
        searchButton.setOnClickListener(this::getNotifications);

        recyclerView = view.findViewById(R.id.reportContainer);
        return view;
    }

    //initialize checkboxes
    private void initializeCheckboxes() {
        fireCheckbox = view.findViewById(R.id.fireCheckbox);
        floodCheckbox = view.findViewById(R.id.floodCheckbox);
        earthquakeCheckbox = view.findViewById(R.id.earthquakeCheckbox);
        tornadoCheckbox = view.findViewById(R.id.tornadoCheckbox);

        Cursor cursor = database.rawQuery("Select * from Preferences WHERE UID = ? LIMIT 1" , new String[]{FirebaseDB.getAuth().getUid()});

        if (cursor != null && cursor.moveToFirst()) { //set as default checkbox values the last values used by the user
            cursor.moveToFirst();
            Log.d("createEventFragment", "selectEventTypeListener: "+ cursor.getString(1));

            boolean fireEnabled = cursor.getInt(2) == 1;
            boolean floodEnabled = cursor.getInt(3) == 1;
            boolean earthquakeEnabled = cursor.getInt(4) == 1;
            boolean tornadoEnabled = cursor.getInt(5) == 1;

            fireCheckbox.setChecked(fireEnabled);
            floodCheckbox.setChecked(floodEnabled);
            earthquakeCheckbox.setChecked(earthquakeEnabled);
            tornadoCheckbox.setChecked(tornadoEnabled);

            cursor.close();
        } else {
            fireCheckbox.setChecked(true);
            floodCheckbox.setChecked(true);
            earthquakeCheckbox.setChecked(true);
            tornadoCheckbox.setChecked(true);
        }
    }

    //initialize dates
    private void initializeDatepickers() {
        buttonDatePickerStart = view.findViewById(R.id.buttonDatePickerStart);
        buttonDatePickerEnd = view.findViewById(R.id.buttonDatePickerEnd);

        buttonDatePickerStart.setText(Helper.getStartOfWeek());
        buttonDatePickerEnd.setText(Helper.getToday());

        buttonDatePickerStart.setOnClickListener(v -> showDatePickerDialog(buttonDatePickerStart));
        buttonDatePickerEnd.setOnClickListener(v -> showDatePickerDialog(buttonDatePickerEnd));
    }

    //datepicker functionality
    private void showDatePickerDialog(Button button) {
        final Calendar c = Calendar.getInstance();

        String[] dateParts = button.getText().toString().split("/");
        int year = Integer.parseInt(dateParts[2]);
        int month = Integer.parseInt(dateParts[1]) - 1;
        int day = Integer.parseInt(dateParts[0]);

        DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext(), R.style.MyDatePickerDialogTheme,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    button.setText(selectedDate);
                }, year, month, day);

        datePickerDialog.show();
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#0E2F51"));

    }


    //notificationList getter
    public void getNotifications(View v) {
        String startDate = Helper.convertDateFormat(buttonDatePickerStart.getText().toString()) + " 00:00:00";
        String endDate = Helper.convertDateFormat(buttonDatePickerEnd.getText().toString()) + " 23:59:59";

        FirebaseDB.getUserAlerts(startDate, endDate,fireCheckbox.isChecked(), floodCheckbox.isChecked(), tornadoCheckbox.isChecked(), earthquakeCheckbox.isChecked(), new FirebaseDB.FirebaseUserAlertGetterListener() {
            @Override
            public void onUserAlertsRetrieved(UserAlerts userAlerts) {
                if (userAlerts == null || userAlerts.getAlerts().isEmpty()) {
                    String message = getString(R.string.no_notifications_found_for_the_given_criteria);
                    Helper.showMessage(v.getContext(), "Warning", message);
                    return;
                }

                fixSearchResultText(userAlerts.getAlerts());

                NotificationAdapter adapter = new NotificationAdapter(getContext(), userAlerts.getAlerts());
                GridLayoutManager layout = new GridLayoutManager(getContext(), 1);
                recyclerView.setLayoutManager(layout);
                recyclerView.setAdapter(adapter);

            }
        });

        updateCheckBoxValues();
    }

    //update the latest checkbox values in the db to be used for next time
    private void updateCheckBoxValues() {

        ContentValues values = new ContentValues();
        values.put("notifCheckboxFire", fireCheckbox.isChecked());
        values.put("notifCheckboxFlood", floodCheckbox.isChecked());
        values.put("notifCheckboxEarthquake", earthquakeCheckbox.isChecked());
        values.put("notifCheckboxTornado", tornadoCheckbox.isChecked());

        database.update("Preferences", values, "UID = ?", new String[]{FirebaseDB.getAuth().getUid()});
    }

    public void fixSearchResultText(List<Alert> alerts) {
        TextView searchResults = new TextView(view.getContext());

        int fireEventsCounter = 0;
        int floodEventsCounter = 0;
        int earthquakeEventsCounter = 0;
        int tornadoEventsCounter = 0;

        for (Alert e : alerts) {
            switch (e.getEventType()) {
                case FIRE:
                    fireEventsCounter++;
                    break;
                case TORNADO:
                    tornadoEventsCounter++;
                    break;
                case FLOOD:
                    floodEventsCounter++;
                    break;
                case EARTHQUAKE:
                    earthquakeEventsCounter++;
                    break;
            }
        }

        StringBuilder results = new StringBuilder("Results:");

        if (fireCheckbox.isChecked()) {
            results.append(" Fires: ").append(fireEventsCounter);
        }
        if (earthquakeCheckbox.isChecked()) {
            results.append(" Earthquakes: ").append(earthquakeEventsCounter);
        }
        if (tornadoCheckbox.isChecked()) {
            results.append(" Tornadoes: ").append(tornadoEventsCounter);
        }
        if (floodCheckbox.isChecked()) {
            results.append(" Floods: ").append(floodEventsCounter);
        }

        searchResults.setText(results.toString());
    }
}