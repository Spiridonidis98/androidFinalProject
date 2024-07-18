package com.kouts.spiri.smartalert.Functionality.Fragments;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Functionality.RecommendEventsWorker;
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

        fireCheckbox.setChecked(true);
        floodCheckbox.setChecked(true);
        earthquakeCheckbox.setChecked(true);
        tornadoCheckbox.setChecked(true);

        getRecommendedEvents = view.findViewById(R.id.getRecommendedEvents);
        getRecommendedEvents.setOnClickListener(this::recommendEventsWorkManager);


        return view;
    }

    public void recommendEventsWorkManager(View v) {
        reportContainer.removeAllViewsInLayout();
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
                                searchResults.setText("");
                                reportContainer.addView(searchResults);

                                for(ArrayList<Event> listOfEvents : recommendedAlertLists) {
                                    addEventToLayout(listOfEvents);
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

        eventType.setText(listOfEvents.get(0).getAlertType() + " at " + listOfEvents.get(0).getTimestamp());


        // Get the background drawable
        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(eventView.getContext(), R.drawable.circle);
        if (drawable != null) {
            // Change the color dynamically
            drawable.setColor(ContextCompat.getColor(eventView.getContext(),R.color.primarycolor)); // replace R.color.new_color with your desired color resource

            eventCount.setBackground(drawable);
        }
        eventCount.setText(listOfEvents.size() + "");
        GradientDrawable border = (GradientDrawable) eventView.getBackground();
        border.setStroke(8, getColorForEvent(listOfEvents.get(0).getAlertType()));
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