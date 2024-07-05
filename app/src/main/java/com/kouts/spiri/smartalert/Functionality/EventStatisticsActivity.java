package com.kouts.spiri.smartalert.Functionality;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.activity.EdgeToEdge;
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

public class EventStatisticsActivity extends AppCompatActivity {

    CheckBox checkBoxFire, checkBoxFlood, checkBoxEarthquake, checkBoxTornado;
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

        DatabaseReference dbEventsRef = FirebaseDB.getEventsReference();

        buttonEventStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbEventsRef.get().addOnCompleteListener(task -> {
                    DataSnapshot dataSnapshot = null;
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
                    else {
                        boolean showFireEvents = checkBoxFire.isChecked();
                        boolean showFloodEvents = checkBoxFlood.isChecked();
                        boolean showEarthquakeEvents = checkBoxEarthquake.isChecked();
                        boolean showTornadoEvents = checkBoxTornado.isChecked();

                        Event event;
                        EventTypes eventType;

                        ArrayList<Event> displayedEvents = new ArrayList<>();
                        int[] eventTypesCount = new int[4]; // 0 : fire, 1 : flood, 2 : Earthquake, 3 : Tornado

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) { //for each child in snapshot

                            event = snapshot.getValue(Event.class);
                            if (event != null) {
                                eventType = event.getAlertType();
                            }
                            else {
                                Helper.showMessage(v.getContext(),"No events recorded", "There are currently no events in the database");
                                return;
                            }

                            //add to total and eventTypesCount depending on type and checked filter boxes
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
                        
                        //TEMPORARY TEST!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        Helper.showMessage(v.getContext(),"filtering test","total = "+totalDisplayedEvents+ "\n"
                                + "fire : "+eventTypesCount[0]+"\n"
                                + "flood : "+eventTypesCount[1]+"\n"
                                + "earthquake : "+eventTypesCount[2]+"\n"
                                + "tornado : "+eventTypesCount[3]);
                    }
                });
            }
        });
    }
}