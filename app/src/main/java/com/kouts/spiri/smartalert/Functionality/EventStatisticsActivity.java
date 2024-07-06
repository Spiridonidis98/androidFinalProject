package com.kouts.spiri.smartalert.Functionality;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.R;

import java.util.ArrayList;
import java.util.Calendar;

public class EventStatisticsActivity extends AppCompatActivity {

    CheckBox checkBoxFire, checkBoxFlood, checkBoxEarthquake, checkBoxTornado, checkBoxCalendar;
    static Calendar calendar = Calendar.getInstance();
    TextView dateTextView;
    long timestampFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_statistics);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button buttonEventStatistics = findViewById(R.id.buttonViewEventStatistics);
        checkBoxFire = findViewById(R.id.checkBoxFire);
        checkBoxFlood = findViewById(R.id.checkBoxFlood);
        checkBoxEarthquake = findViewById(R.id.checkBoxEarthquake);
        checkBoxTornado = findViewById(R.id.checkBoxTornado);
        checkBoxFire.setChecked(true);
        checkBoxFlood.setChecked(true);
        checkBoxEarthquake.setChecked(true);
        checkBoxTornado.setChecked(true);

        DatabaseReference dbEventsRef = FirebaseDB.getEventsReference();

        checkBoxCalendar = findViewById(R.id.checkBoxCalendar);
        dateTextView = findViewById(R.id.dateTextView);
        dateTextView.setVisibility(View.INVISIBLE);

        checkBoxCalendar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showCalendarDialog(); //open date picker
            } else {
                dateTextView.setVisibility(View.INVISIBLE);
            }
        });


        buttonEventStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbEventsRef.get().addOnCompleteListener(task -> {
                    DataSnapshot dataSnapshot;
                    if (task.isSuccessful()) { //get snapshot
                        dataSnapshot = task.getResult();
                    }
                    else {
                        Helper.showMessage(v.getContext(),"Error","System could not receive data from database");
                        return;
                    }

                    if (! dataSnapshot.exists()) {
                        Helper.showMessage(v.getContext(), "No data recorded", "There are currently no matching data in the database");
                        return;
                    }

                    boolean showFireEvents = checkBoxFire.isChecked();
                    boolean showFloodEvents = checkBoxFlood.isChecked();
                    boolean showEarthquakeEvents = checkBoxEarthquake.isChecked();
                    boolean showTornadoEvents = checkBoxTornado.isChecked();

                    Event event;
                    EventTypes eventType;
                    long eventTimestamp;

                    ArrayList<Event> displayedEvents = new ArrayList<>();
                    int[] eventTypesCount = new int[4]; // 0 : fire, 1 : flood, 2 : Earthquake, 3 : Tornado

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) { //for each child in snapshot

                        event = snapshot.getValue(Event.class);
                        if (event == null) {
                            Helper.showMessage(v.getContext(),"No events recorded", "There are currently no events in the database");
                            return;
                        }
                        eventType = event.getAlertType();
                        eventTimestamp = Helper.dateToTimestamp(event.getTimestamp());

                        if (checkBoxCalendar.isChecked()) { //date filtering is enabled
                            if (eventTimestamp <= timestampFilter) { //event date is earlier than selected date
                                continue; //skip this event
                            }
                        }

                        //add to displayedEvents and eventTypesCount depending on type and checked filter boxes
                        if (eventType.equals(EventTypes.FIRE) && showFireEvents) {
                            displayedEvents.add(event);
                            eventTypesCount[0]++;
                        }
                        else if (eventType.equals(EventTypes.FLOOD) && showFloodEvents) {
                            displayedEvents.add(event);
                            eventTypesCount[1]++;
                        }
                        else if (eventType.equals(EventTypes.EARTHQUAKE) && showEarthquakeEvents) {
                            displayedEvents.add(event);
                            eventTypesCount[2]++;
                        }
                        else if (eventType.equals(EventTypes.TORNADO) && showTornadoEvents) {
                            displayedEvents.add(event);
                            eventTypesCount[3]++;
                        }
                    }
                    int totalDisplayedEvents = displayedEvents.size();

                    //display statistics
                    String title = "Event Statistics";
                    String message = "Total events: " + totalDisplayedEvents + "\n\n"
                            + "Fires : " + eventTypesCount[0] + "\n"
                            + "Floods : " + eventTypesCount[1] + "\n"
                            + "Earthquakes : " + eventTypesCount[2] + "\n"
                            + "Tornadoes : " + eventTypesCount[3];

                    showMessageDialog(title, message);
                });
            }
        });
    }

    private void showCalendarDialog() {
        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.calendar_view);

        CalendarView calendarView = dialog.findViewById(R.id.calendarView);
        calendarView.setMaxDate(System.currentTimeMillis());

        //listen for date selection
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            calendar.set(year,month,dayOfMonth,0,0,0);
            timestampFilter = calendar.getTime().getTime();

            dateTextView.setText("Display events after: " + Helper.timestampToDate(timestampFilter));
            dateTextView.setVisibility(View.VISIBLE);
            dialog.dismiss();
        });

        dialog.show();

        //when clicking out of the calendar without selecting a date, un-tick the checkbox
        dialog.setOnCancelListener(listener -> checkBoxCalendar.setChecked(false));
    }

    private void showMessageDialog(String title, String message) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.event_statistics_view);

        TextView titleView = dialog.findViewById(R.id.dialogTitle);
        TextView messageView = dialog.findViewById(R.id.dialogMessage);

        // Set title and message text
        titleView.setText(title);
        titleView.setTextSize(22);

        messageView.setText(message);

        // Customize message text appearance (bold, larger size)
        SpannableString spannableMessage = new SpannableString(message);
        spannableMessage.setSpan(new StyleSpan(Typeface.BOLD), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        messageView.setText(spannableMessage);
        messageView.setTextSize(20);  // Increase text size

        dialog.show();
    }
}