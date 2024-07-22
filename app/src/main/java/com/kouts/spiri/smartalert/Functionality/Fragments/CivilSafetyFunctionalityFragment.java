package com.kouts.spiri.smartalert.Functionality.Fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
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
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.MapsActivity;
import com.kouts.spiri.smartalert.Functionality.RecommendEventsWorker;
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
        ImageView expanView = eventView.findViewById(R.id.expandView);
        LinearLayout eventListContainer = eventView.findViewById(R.id.event_scroll_layout);

        Button confirmButton = eventView.findViewById(R.id.confirm_button);

        //here we confirm the creation of an alert
        confirmButton.setOnClickListener(v -> {
            int warning = 0;
            if(listOfEvents.size() >= 5 && listOfEvents.size() < 10) {
                warning = 1;
            }
            else if (listOfEvents.size() >= 10) {
                warning = 2;
            }
            Alert newAlert = new Alert(listOfEvents.get(0).getAlertType() + " at " + listOfEvents.get(0).getTimestamp(), listOfEvents.get(0).getTimestamp(), warning, listOfEvents.get(0).getAlertType());
            FirebaseDB.addAlert(newAlert, new FirebaseDB.FirebaseAlertListener() {
                @Override
                public void alertAdded() {
                    Helper.showToast(getContext(), "Alert Created", Toast.LENGTH_LONG);
                }

                @Override
                public void onError(Exception e) {

                }
            });
        });

        //here we set the expand view functionality
        expanView.setOnClickListener(v -> {
            if(eventListContainer.getVisibility() == View.GONE) {
                eventListContainer.setVisibility(View.VISIBLE);
                eventListContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        eventListContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        int targetHeight = eventListContainer.getMeasuredHeight();

                        ValueAnimator anim = ValueAnimator.ofInt(0, targetHeight);
                        anim.setDuration(300);
                        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                int value = (int) valueAnimator.getAnimatedValue();
                                eventListContainer.getLayoutParams().height = value;
                                eventListContainer.requestLayout();
                            }
                        });
                        anim.start();
                    }
                });

            }
            else {
                ValueAnimator anim = ValueAnimator.ofInt(eventListContainer.getHeight(), 0);
                anim.setDuration(300);
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        int value = (int) valueAnimator.getAnimatedValue();
                        eventListContainer.getLayoutParams().height = value;
                        eventListContainer.requestLayout();
                    }
                });
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        eventListContainer.setVisibility(View.GONE);
                    }
                });
                anim.start();
            }
        });


        eventType.setText(listOfEvents.get(0).getAlertType() + " at " + listOfEvents.get(0).getTimestamp());


        // Get the background drawable
        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(eventView.getContext(), R.drawable.circle);
        if (drawable != null) {
            // Change the color dynamically
            drawable.setColor(ContextCompat.getColor(eventView.getContext(), getColorForEventId(listOfEvents.get(0).getAlertType()))); // replace R.color.new_color with your desired color resource
            drawable.setStroke(8, getColorForEvent(listOfEvents.get(0).getAlertType()));
            eventCount.setBackground(drawable);
        }
        eventCount.setText(listOfEvents.size() + "");
        GradientDrawable border = (GradientDrawable) eventView.getBackground();
        border.setStroke(8, getColorForEvent(listOfEvents.get(0).getAlertType()));

        for(Event event: listOfEvents) {
            addEventToScrollView(event, eventView);
        }

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

    //here we create the list for each group to show to the expand view
    public void addEventToScrollView(Event event, View eventView) {

        LinearLayout civilreportContainer = eventView.findViewById(R.id.civilreportContainer);
        View v = LayoutInflater.from(view.getContext()).inflate(R.layout.event, civilreportContainer, false);

        v.setOnClickListener( v2 -> showEventInfo(event));
        TextView eventType = v.findViewById(R.id.event_type);
        TextView eventComment = v.findViewById(R.id.event_comment);
        ImageView mapIcon = v.findViewById(R.id.map_icon);

        eventType.setText(event.getAlertType() + " at " + event.getTimestamp());
        eventComment.setText(event.getComment());

        GradientDrawable border = (GradientDrawable) eventView.getBackground();
        border.setStroke(5, getColorForEvent(event.getAlertType()));
        getEventImage(event, mapIcon);
        civilreportContainer.addView(v);

    }
    private void getEventImage(Event event, ImageView imageView) {

        FirebaseDB.getImageFromStorage(event.getImage(), new FirebaseDB.FirebaseStorageListener() {
            @Override
            public void onImageRetrieved(Uri image) {
                event.setImageURI(image);
                Glide.with(CivilSafetyFunctionalityFragment.this)
                        .load(image)
                        .error(R.drawable.home)
                        .into(imageView);
            }

            @Override
            public void onError(Exception e) {
                Log.d("test", e.toString());
            }
        });
    }

    private void showEventInfo(Event event) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.event_info);

        //initialize eventInfo view
        ImageView closeIcon = dialog.findViewById(R.id.closeEventIcon);
        ImageView imageView = dialog.findViewById(R.id.eventImage);
        ImageView mapIcon = dialog.findViewById(R.id.eventInfoLocationIcon);

        TextView eventType = dialog.findViewById(R.id.eventInfoType);
        TextView eventTime = dialog.findViewById(R.id.eventInfoTime);
        TextView eventComment = dialog.findViewById(R.id.eventInfoComment);

        closeIcon.setOnClickListener(v -> dialog.dismiss());

        eventType.setText(event.getAlertType() + "");
        eventTime.setText(event.getTimestamp());
        eventComment.setText(event.getComment());

        Glide.with(CivilSafetyFunctionalityFragment.this)
                .load(event.getImageURI())
                .error(R.drawable.home)
                .into(imageView);

        mapIcon.setOnClickListener(v -> {
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(event.getLatitude());
            location.setLongitude(event.getLongitude());

            Intent intent = new Intent(getContext(), MapsActivity.class);
            // Create Intent using LocationUtils method
            intent.putExtra("Location", location);
            intent.putExtra("EventType", event.getAlertType());
            intent.putExtra("EventTime", event.getTimestamp());

            startActivity(intent);

        });

        dialog.show();


    }
}