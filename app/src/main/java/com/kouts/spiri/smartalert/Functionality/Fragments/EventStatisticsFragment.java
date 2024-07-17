package com.kouts.spiri.smartalert.Functionality.Fragments;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.R;

import java.util.Calendar;
import java.util.List;

public class EventStatisticsFragment extends Fragment {
    private Button buttonDatePickerStart, buttonDatePickerEnd, searchButton;
    private CheckBox fireCheckbox, earthquakeCheckbox, tornadoCheckbox, floodCheckbox;
    private LinearLayout reportContainer;
    private View view;

    public EventStatisticsFragment() {
        // Required empty public constructor
    }

    public static EventStatisticsFragment newInstance() {
        return new EventStatisticsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_event_statistics, container, false);

        reportContainer = view.findViewById(R.id.reportContainer);
        searchButton = view.findViewById(R.id.getEvents);

        fireCheckbox = view.findViewById(R.id.fireCheckbox);
        floodCheckbox = view.findViewById(R.id.floodCheckbox);
        earthquakeCheckbox = view.findViewById(R.id.earthquakeCheckbox);
        tornadoCheckbox = view.findViewById(R.id.tornadoCheckbox);

        fireCheckbox.setChecked(true);
        floodCheckbox.setChecked(true);
        earthquakeCheckbox.setChecked(true);
        tornadoCheckbox.setChecked(true);

        buttonDatePickerStart = view.findViewById(R.id.buttonDatePickerStart);
        buttonDatePickerEnd = view.findViewById(R.id.buttonDatePickerEnd);

        buttonDatePickerStart.setText(Helper.getStartOfWeek());
        buttonDatePickerEnd.setText(Helper.getToday());

        buttonDatePickerStart.setOnClickListener(v -> showDatePickerDialog(buttonDatePickerStart));
        buttonDatePickerEnd.setOnClickListener(v -> showDatePickerDialog(buttonDatePickerEnd));

        searchButton.setOnClickListener(this::getEvents);

        return view;
    }

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

    public void getEvents(View v) {
        Log.e("TEST", "RUNNING");
        reportContainer.removeAllViewsInLayout();
        String startDate = Helper.convertDateFormat(buttonDatePickerStart.getText().toString()) + " 00:00:00";
        String endDate = Helper.convertDateFormat(buttonDatePickerEnd.getText().toString()) + " 23:59:59";
        FirebaseDB.getEvents(startDate, endDate, fireCheckbox.isChecked(), floodCheckbox.isChecked(), earthquakeCheckbox.isChecked(), tornadoCheckbox.isChecked(), new FirebaseDB.FirebaseEventListener() {
            @Override
            public void onEventsRetrieved(List<Event> events) {
                if (events == null || events.isEmpty()) {
                    String message = getString(R.string.no_events_found_for_the_given_criteria);
                    Helper.showMessage(v.getContext(), "Warning", message);
                    return;
                }
                fixSearchResultText(events);
                for (Event e : events) {
                    addEventToLayout(e);
                }
            }

            @Override
            public void onEventAdded() {}

            @Override
            public void onError(Exception e) {}
        });
    }

    public void fixSearchResultText(List<Event> events) {
        TextView searchResults = new TextView(view.getContext());

        int fireEventsCounter = 0;
        int floodEventsCounter = 0;
        int earthquakeEventsCounter = 0;
        int tornadoEventsCounter = 0;

        for (Event e : events) {
            switch (e.getAlertType()) {
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
        reportContainer.addView(searchResults);
    }

    public void addEventToLayout(Event event) {
        View eventView = LayoutInflater.from(view.getContext()).inflate(R.layout.event, reportContainer, false);

        TextView eventType = eventView.findViewById(R.id.event_type);
        TextView eventComment = eventView.findViewById(R.id.event_comment);
        ImageView mapIcon = eventView.findViewById(R.id.map_icon);

        eventType.setText(event.getAlertType() + " at " + event.getTimestamp());
        eventComment.setText(event.getComment());

        GradientDrawable border = (GradientDrawable) eventView.getBackground();
        border.setStroke(5, getColorForEvent(event.getAlertType()));
        reportContainer.addView(eventView);
    }

    private int getColorForEvent(EventTypes type) {
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
}
