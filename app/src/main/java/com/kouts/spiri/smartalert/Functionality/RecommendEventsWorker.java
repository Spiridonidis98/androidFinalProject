package com.kouts.spiri.smartalert.Functionality;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class RecommendEventsWorker extends Worker {

    final double RADIUS_KM = 20;
    final long ACCEPTABLE_TIME_DIFF = 2 *60*60*1000; //2 hours forwards AND backwards, for a total of 4 hours
    final int CLUSTER_NUM = 5; //there need to be at least 'CLUSTER_NUM' events that refer to the same disaster reported to recommend an alert
    final int LAST_X_DAYS = 12; // is used to get all events in the last X days
    final int TOTAL_THREADS = 4; // number of threads to be used, 1 for each event type.

    public RecommendEventsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        ArrayList<Event> recentEventsFire = new ArrayList<>();
        ArrayList<Event> recentEventsFlood = new ArrayList<>();
        ArrayList<Event> recentEventsEarthquake = new ArrayList<>();
        ArrayList<Event> recentEventsTornado = new ArrayList<>();

        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        List<Future<ArrayList<ArrayList<Event>>>> futures;
        List<Callable<ArrayList<ArrayList<Event>>>> callables = new ArrayList<>();

        CountDownLatch countDownLatch = new CountDownLatch(1); //ensures dbEventsRef listener finishes first.

        long currentTime = System.currentTimeMillis();
        String previousDate = Helper.timestampToDate(currentTime - (LAST_X_DAYS * 24 * 60 * 60 * 1000)); //days to milliseconds

        DatabaseReference dbEventsRef = FirebaseDB.getEventsReference();

        dbEventsRef.orderByChild("timestamp").startAt(previousDate).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                DataSnapshot snapshot = task.getResult();

                if (!snapshot.exists()) {
                    Log.d("RecommendedEventsWorker", "There are currently no matching data in the database");
                    countDownLatch.countDown(); //allow the other threads to start
                    return;
                }

                Event event;
                EventTypes eventType;
                for (DataSnapshot snap : snapshot.getChildren()) { //for each child in snapshot

                    event = snap.getValue(Event.class);
                    if (event == null) {
                        Log.d("RecommendedEventsWorker", "There are currently no events in the database");
                        countDownLatch.countDown(); //allow the other threads to start
                        return;
                    }

                    eventType = event.getAlertType();
                    if (eventType.equals(EventTypes.FIRE)) {
                        recentEventsFire.add(event);
                    } else if (eventType.equals(EventTypes.FLOOD)) {
                        recentEventsFlood.add(event);
                    } else if (eventType.equals(EventTypes.EARTHQUAKE)) {
                        recentEventsEarthquake.add(event);
                    } else if (eventType.equals(EventTypes.TORNADO)) {
                        recentEventsTornado.add(event);
                    }
                }
                countDownLatch.countDown(); //allow the other threads to start
            }
        });

        for (int i = 0; i < TOTAL_THREADS; i++) {

            int finalI = i;
            callables.add(() -> {
                ArrayList<ArrayList<Event>> recommendedAlertLists = new ArrayList<>();
                switch (finalI) {
                    case 0:
                        recommendedAlertLists = calculateRecommendedAlerts(recentEventsFire);
                        break;
                    case 1:
                        recommendedAlertLists = calculateRecommendedAlerts(recentEventsFlood);
                        break;
                    case 2:
                        recommendedAlertLists = calculateRecommendedAlerts(recentEventsEarthquake);
                        break;
                    case 3:
                        recommendedAlertLists = calculateRecommendedAlerts(recentEventsTornado);
                        break;
                }

                return recommendedAlertLists;
            });
        }

        try {
            countDownLatch.await(); //wait for database listener to finish
            futures = executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ArrayList<ArrayList<Event>> allAlertLists = new ArrayList<>();
        for (Future<ArrayList<ArrayList<Event>>> future: futures) {
            try {
                ArrayList<ArrayList<Event>> recommendedAlertLists = future.get();
                allAlertLists.addAll(recommendedAlertLists);
                System.out.println(recommendedAlertLists);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        //convert ArrayList<ArrayList<Event>> to JSON
        Gson gson = new Gson();
        String allRecommendedAlertListsJson = gson.toJson(allAlertLists);

        //put the JSON in a Data object
        Data outputData = new Data.Builder()
                .putString("all_recommended_alert_lists", allRecommendedAlertListsJson)
                .build();

        return Result.success(outputData);
    }


    //groups Events that refer to the same disaster and returns the groups (lists) of those elements
    //The first event of each group should be considered the center of the disaster because the group is formed with events close to it in distance and time
    private ArrayList<ArrayList<Event>> calculateRecommendedAlerts(ArrayList<Event> recentEventsOfType) {

        ArrayList<ArrayList<Event>> clusteredEventLists = new ArrayList<>();
        ArrayList<ArrayList<Event>> listsToRemove = new ArrayList<>();

        recentEventsOfType.forEach(event1 -> { //for each event1
            ArrayList<Event> clusteredEvents = new ArrayList<>();
            clusteredEvents.add(event1);

            long timestampEvent1 = Helper.dateToTimestamp(event1.getTimestamp());
            long negativeTimeDif = timestampEvent1 - ACCEPTABLE_TIME_DIFF;
            long positiveTimeDif = timestampEvent1 + ACCEPTABLE_TIME_DIFF;

            for (Event event2 : recentEventsOfType) { //loop the arraylist again

                long timestampEvent2 = Helper.dateToTimestamp(event2.getTimestamp());

                if (event1.equals(event2)) { //if it's the same event continue to next event2
                    continue;
                }

                if (! ((negativeTimeDif <= timestampEvent2) && (positiveTimeDif >= timestampEvent2))) { //checks if the events have a maximum time difference of 'ACCEPTABLE_TIME_DIFF'
                    continue;
                }

                double distance = Helper.calculateGeoDistance(event1.getLatitude(), event1.getLongitude(), event2.getLatitude(), event2.getLongitude());
                if (distance <= RADIUS_KM) { //if event2 is within the acceptable distance add it to clusteredEvents
                    clusteredEvents.add(event2);
                }
            }

            if (clusteredEvents.size() >= CLUSTER_NUM) { // size (including the original event) >= CLUSTER_NUM
                clusteredEventLists.add(clusteredEvents);
            }
        });

        clusteredEventLists.forEach(outerList -> { //for every list that clustered events
            boolean currentListRemoved = false;
            Event originalEvent = outerList.get(0);

            for (int innerListIndex = clusteredEventLists.indexOf(outerList)+1 ; innerListIndex < clusteredEventLists.size(); innerListIndex++) {  //get the indices of the next lists after this outerList
                ArrayList<Event> innerList = clusteredEventLists.get(innerListIndex);

                if (outerList.size() <= innerList.size()) { // if the list in the outer loop is smaller or than the list in the inner loop
                    if (currentListRemoved) { //if the list in the outer loop has been removed, break
                        break;
                    }
                    for (Event event: innerList) {
                        if (originalEvent.equals(event)) { //if the list in the inner loop also has the same event as the one in the outer loop
                            listsToRemove.add(outerList); //add the list in the outer loop to be removed later since the 'innerList' also has it and is larger
                            currentListRemoved = true; //mark that the list in the outer loop is to be removed
                            break;
                        }
                    }
                }
            }
            if (currentListRemoved) {
                return; //continue to next list
            }
        });

        clusteredEventLists.removeAll(listsToRemove);
        
        //return the event lists, with each containing events that refer to the same disaster
        return clusteredEventLists;
    }
}
