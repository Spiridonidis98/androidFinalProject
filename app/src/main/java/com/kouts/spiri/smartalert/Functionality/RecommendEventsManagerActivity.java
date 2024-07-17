package com.kouts.spiri.smartalert.Functionality;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.R;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class RecommendEventsManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recommend_events_manager);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Helper.validateCurrentUser(this);

        Button buttonRecommendEvents = findViewById(R.id.buttonRecommendEvents);
        TextView textViewState = findViewById(R.id.textViewState);

        buttonRecommendEvents.setOnClickListener(click -> recommendEventsWorkManager(textViewState));

    }

    public void recommendEventsWorkManager(TextView textViewState) {

        WorkManager workManager = WorkManager.getInstance(this);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RecommendEventsWorker.class)
                .build();
        workManager.enqueue(request);
        workManager.getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {

                    ArrayList<ArrayList<Event>> recommendedAlertLists = new ArrayList<>();

                    if (workInfo!=null) {
                        textViewState.append(workInfo.getState()+"\n");
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {

                            Data outputData = workInfo.getOutputData();
                            String recommendedAlertListsJson = outputData.getString("all_recommended_alert_lists");

                            if (recommendedAlertListsJson == null) {
                                String message = getString(R.string.recommended_events_could_not_be_retrieved);
                                Helper.showMessage(this,"Error",message);
                                return;
                            }

                            //deserialize JSON to ArrayList<ArrayList<Event>>
                            Gson gson = new Gson();
                            Type type = new TypeToken<ArrayList<ArrayList<Event>>>() {}.getType(); //set type of object
                            recommendedAlertLists = gson.fromJson(recommendedAlertListsJson, type); //deserialize
                        }
                    }
                    Log.d("RecommendEventsManagerActivity",recommendedAlertLists.toString() );
                });
    }
}