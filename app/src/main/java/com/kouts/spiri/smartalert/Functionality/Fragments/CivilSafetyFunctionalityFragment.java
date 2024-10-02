package com.kouts.spiri.smartalert.Functionality.Fragments;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kouts.spiri.smartalert.Assistance.Adapter;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.Background_Functions.RecommendEventsWorker;
import com.kouts.spiri.smartalert.POJOs.Alert;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.R;

import java.lang.reflect.Type;
import java.util.ArrayList;


public class CivilSafetyFunctionalityFragment extends Fragment {
    private CheckBox fireCheckbox, earthquakeCheckbox, tornadoCheckbox, floodCheckbox;
    private Button getRecommendedEvents;
    private LinearLayout reportContainer;
    private View view;
    private RecyclerView recyclerView;
    SQLiteDatabase database;
    private final float ALERT_RADIUS = 10; //kilometers
    public CivilSafetyFunctionalityFragment() {
        // Required empty public constructor
    }

    public static CivilSafetyFunctionalityFragment newInstance(String param1, String param2) {
        CivilSafetyFunctionalityFragment fragment = new CivilSafetyFunctionalityFragment();
        Bundle args = new Bundle();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_civil_safety_functionality, container, false);

        reportContainer = view.findViewById(R.id.reportContainer);

        fireCheckbox = view.findViewById(R.id.fireCheckbox);
        floodCheckbox = view.findViewById(R.id.floodCheckbox);
        earthquakeCheckbox = view.findViewById(R.id.earthquakeCheckbox);
        tornadoCheckbox = view.findViewById(R.id.tornadoCheckbox);

        database = Helper.createLocalDB(view.getContext());
        initializeCheckboxes();

        getRecommendedEvents = view.findViewById(R.id.getRecommendedEvents);
        getRecommendedEvents.setOnClickListener(this::recommendEventsWorkManager);


        return view;
    }

    public void recommendEventsWorkManager(View v) {
        reportContainer.removeAllViewsInLayout();
        updateCheckBoxValues(); //checkbox values updated in the db when user presses the "search events" button

        TextView searchResults = new TextView(v.getContext());

        WorkManager workManager = WorkManager.getInstance(v.getContext());
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RecommendEventsWorker.class)
                .build();
        workManager.enqueue(request);
        workManager.getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {

                    ArrayList<ArrayList<Event>> recommendedAlertLists = new ArrayList<>();

                    if (workInfo!=null) {

                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {

                            Data outputData = workInfo.getOutputData();
                            String recommendedAlertListsJson = outputData.getString("all_recommended_alert_lists");

                            if (recommendedAlertListsJson == null) {
                                String message = getString(R.string.recommended_events_could_not_be_retrieved);
                                Helper.showMessage(v.getContext(),"Error",message);
                                return;
                            }

                            //deserialize JSON to ArrayList<ArrayList<Event>>
                            Gson gson = new Gson();
                            Type type = new TypeToken<ArrayList<ArrayList<Event>>>() {}.getType(); //set type of object
                            recommendedAlertLists = gson.fromJson(recommendedAlertListsJson, type); //deserialize

                            if(recommendedAlertLists.isEmpty()) {
                                String message = getString(R.string.no_recommended_alerts_were_found);
                                searchResults.setText(message);
                                reportContainer.addView(searchResults);
                            }
                            else {
                                ArrayList<ArrayList<Event>> listOfFireEvents = new ArrayList<>();
                                ArrayList<ArrayList<Event>> listOfFloodEvents = new ArrayList<>();
                                ArrayList<ArrayList<Event>> listOfEarthquakeEvents = new ArrayList<>();
                                ArrayList<ArrayList<Event>> listOfTornadoEvents = new ArrayList<>();

                                for(ArrayList<Event> listOfEvents : recommendedAlertLists) {
                                    if (!listOfEvents.isEmpty()) {
                                        EventTypes alertType = listOfEvents.get(0).getAlertType(); //every event in each list should have the same EventType
                                        if (alertType.equals(EventTypes.FIRE)) {
                                            listOfFireEvents.add(listOfEvents);
                                        }
                                        else if (alertType.equals(EventTypes.FLOOD)) {
                                            listOfFloodEvents.add(listOfEvents);
                                        }
                                        else if (alertType.equals(EventTypes.EARTHQUAKE)) {
                                            listOfEarthquakeEvents.add(listOfEvents);
                                        }
                                        else if (alertType.equals(EventTypes.TORNADO)) {
                                            listOfTornadoEvents.add(listOfEvents);
                                        }
                                        else {
                                            Log.d("CivilSafetyFunctionalityFragment", "recommendEventsWorkManager: Type of list not recognized");
                                        }
                                    }
                                    else {
                                        Log.d("CivilSafetyFunctionalityFragment", "recommendEventsWorkManager: Empty list detected");
                                    }
                                }

                                ArrayList<ArrayList<Event>> listsToDisplay = new ArrayList<>();

                                if (fireCheckbox.isChecked() && !listOfFireEvents.isEmpty()) {
                                    listsToDisplay.addAll(listOfFireEvents);
                                }
                                if (floodCheckbox.isChecked() && !listOfFloodEvents.isEmpty()) {
                                    listsToDisplay.addAll(listOfFloodEvents);
                                }
                                if (earthquakeCheckbox.isChecked() && !listOfEarthquakeEvents.isEmpty()) {
                                    listsToDisplay.addAll(listOfEarthquakeEvents);
                                }
                                if (tornadoCheckbox.isChecked() && !listOfTornadoEvents.isEmpty()) {
                                    listsToDisplay.addAll(listOfTornadoEvents);
                                }

                                if (listsToDisplay.isEmpty()) {
                                    String message = getString(R.string.no_recommended_alerts_were_found);
                                    searchResults.setText(message);
                                    reportContainer.addView(searchResults);
                                }
                                else {
                                    searchResults.setText("");
                                    reportContainer.addView(searchResults);

                                    for(ArrayList<Event> listToDisplay : listsToDisplay) {
                                        addEventToLayout(listToDisplay);
                                    }
                                }
                            }

                        }
                    }
                    else {
                        String message = getString(R.string.no_recommended_alerts_were_found);
                        searchResults.setText(message);
                        reportContainer.addView(searchResults);
                    }
                    Log.d("RecommendEventsManagerActivity",recommendedAlertLists.toString() );
                });
    }

    public void addEventToLayout(ArrayList<Event> listOfEvents) {
        View eventView = LayoutInflater.from(view.getContext()).inflate(R.layout.civil_list, reportContainer, false);

        TextView eventType = eventView.findViewById(R.id.event_type);
        TextView eventCount = eventView.findViewById(R.id.event_count);
        ImageView expanView = eventView.findViewById(R.id.expandView);
        RecyclerView recyclerView = eventView.findViewById(R.id.civil_list_event_scroll);

        recyclerView.setVisibility(View.GONE);
        Button confirmButton = eventView.findViewById(R.id.confirm_button);
        Event alertEvent = listOfEvents.get(0);

        //here we confirm the creation of an alert
        confirmButton.setOnClickListener(v -> {
            int warning;
            if(listOfEvents.size() < 10) {
                warning = 1;
            }
            else {
                warning = 2;
            }
            Alert newAlert = new Alert(alertEvent.getAlertType() + " at " + alertEvent.getTimestamp(), Helper.timestampToDate(System.currentTimeMillis()), warning, alertEvent.getAlertType(), alertEvent.getLatitude(), alertEvent.getLongitude(), ALERT_RADIUS, listOfEvents);
            FirebaseDB.addAlert(newAlert, new FirebaseDB.FirebaseAlertListener() {
                @Override
                public void alertAdded() {
                    Helper.showToast(getContext(), "Alert Created", Toast.LENGTH_LONG);
                }

                @Override
                public void alertExists() {
                    Helper.showToast(getContext(), "Alert Already Exists", Toast.LENGTH_LONG);
                }
            });
        });

        //here we set the expand view functionality
        // Expand view functionality
        expanView.setOnClickListener(v -> {
            if (recyclerView.getVisibility() == View.GONE) {
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.GONE);
            }
        });


        eventType.setText(alertEvent.getAlertType() + " at " + alertEvent.getTimestamp());


        // Get the background drawable
        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(eventView.getContext(), R.drawable.circle);
        if (drawable != null) {
            // Change the color dynamically
            drawable.setColor(ContextCompat.getColor(eventView.getContext(), getColorForEventId(listOfEvents.get(0).getAlertType()))); // replace R.color.new_color with your desired color resource
            drawable.setStroke(8, getColorForEvent(alertEvent.getAlertType()));
            eventCount.setBackground(drawable);
        }
        eventCount.setText(listOfEvents.size() + "");
        GradientDrawable border = (GradientDrawable) eventView.getBackground();
        border.setStroke(8, getColorForEvent(alertEvent.getAlertType()));


        Adapter adapter = new Adapter(getContext(), listOfEvents);
        int spanCount = calculateSpanCount();
        GridLayoutManager layout = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(layout);
        recyclerView.setAdapter(adapter);

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

    private int getColorForEventId(EventTypes type) {
        switch (type) {
            case FIRE:
                return R.color.fire;
            case FLOOD:
                return R.color.flood;
            case TORNADO:
                return R.color.torando;
            case EARTHQUAKE:
                return R.color.earthquake;
            default:
                return R.color.white;
        }
    }

    private int calculateSpanCount() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int itemWidth = 600; // Adjust this value according to your item dimensions
        return screenWidth / itemWidth;
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

    //update the latest checkbox values in the db to be used for next time
    private void updateCheckBoxValues() {

        ContentValues values = new ContentValues();
        values.put("notifCheckboxFire", fireCheckbox.isChecked());
        values.put("notifCheckboxFlood", floodCheckbox.isChecked());
        values.put("notifCheckboxEarthquake", earthquakeCheckbox.isChecked());
        values.put("notifCheckboxTornado", tornadoCheckbox.isChecked());

        database.update("Preferences", values, "UID = ?", new String[]{FirebaseDB.getAuth().getUid()});
    }
}