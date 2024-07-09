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
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.gson.Gson;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.R;

import java.io.FileWriter;
import java.io.IOException;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button buttonTest = findViewById(R.id.buttonTest);
        TextView textViewTest = findViewById(R.id.textViewTest);

        buttonTest.setOnClickListener(click -> test(textViewTest));

    }

    public void test(TextView textViewTest) {

        Log.d("STARTING WORK MANAGER","STARTING WORK MANAGER");
        //Data data = new Data.Builder().putString("example","data").build();
        WorkManager workManager = WorkManager.getInstance(this);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MyWorker.class)
              //  .setInputData(data)
                .build();
        workManager.enqueue(request);
        workManager.getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo!=null) {
                        textViewTest.append(workInfo.getState().toString()+"\n");
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {

                            Data outputData = workInfo.getOutputData();
                            String jsonArrayListOfLists = outputData.getString("all_recommended_events");


                            // Initialize the FileWriter object
                            Log.d("OUTPUT!!!!!!!!!!!!!!!!",jsonArrayListOfLists);
                            //textView.append(workInfo.getOutputData().toString());


//                            //deserialize JSON string to ArrayList<ArrayList<Event>>
//                            Gson gson = new Gson();
//                            Type type = new TypeToken<ArrayList<ArrayList<Event>>>() {}.getType();
//                            ArrayList<ArrayList<Event>> arrayListOfLists = gson.fromJson(jsonArrayListOfLists, type);
                        }
                    }
                });
    }
}