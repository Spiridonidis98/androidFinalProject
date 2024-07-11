package com.kouts.spiri.smartalert.Functionality;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MyWorker extends Worker {

    final double RADIUS_KM = 10;
    final long ACCEPTABLE_TIME_DIFF = 2 *60*60*1000; //2 hours
    final int CLUSTER_NUM = 5;
    final int LAST_X_DAYS = 1; // is used to get all events in the last X days
    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        AtomicBoolean gotDataFromDB = new AtomicBoolean(false);

        ArrayList<Event> recentEventsFire = new ArrayList<>();
        ArrayList<Event> recentEventsFlood = new ArrayList<>();
        ArrayList<Event> recentEventsEarthquake = new ArrayList<>();
        ArrayList<Event> recentEventsTornado = new ArrayList<>();
        int totalThreads = 4;

        CountDownLatch countDownLatch = new CountDownLatch(1); //ensures dbEventsRef addListenerForSingleValueEvent finishes first.


//        Context context = this.getApplicationContext();

        // Get current time and calculate the timestamp for 24 hours ago
        long currentTime = System.currentTimeMillis();
        String previousDate = Helper.timestampToDate(currentTime - (LAST_X_DAYS * 24 * 60 * 60 * 1000)); //days to milliseconds

        DatabaseReference dbEventsRef = FirebaseDB.getEventsReference();
        dbEventsRef.orderByChild("timestamp").startAt(previousDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("addListenerForSingleValueEvent", "just entered");

                if (!snapshot.exists()) {
                    Log.d("No data recorded", "There are currently no matching data in the database");
//                Helper.showMessage(context, "No data recorded", "There are currently no matching data in the database");
                    return;
                }


                Event event;
                EventTypes eventType;
                for (DataSnapshot snap : snapshot.getChildren()) { //for each child in snapshot

                    event = snap.getValue(Event.class);
                    if (event == null) {
                        Log.d("No events recorded", "There are currently no events in the database");
//                    Helper.showMessage(context, "No events recorded", "There are currently no events in the database");
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
              //      gotDataFromDB.set(true);
Log.d("LISTENER FINISHED","OK");
                }
                countDownLatch.countDown(); //allow the other threads to start
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                countDownLatch.countDown();
            }
        });


        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        List<Future<ArrayList<Event>>> futures;
        List<Callable<ArrayList<Event>>> callables = new ArrayList<>();


        for (int i = 0; i < totalThreads; i++) {

            int finalI = i;
            callables.add(() -> {
                ArrayList<Event> recommendedAlerts = new ArrayList<>();
                switch (finalI) {
                    case 0:
                        recommendedAlerts = calculateRecommendedAlerts(recentEventsFire);
                        break;
                    case 1:
                        recommendedAlerts = calculateRecommendedAlerts(recentEventsFlood);
                        break;
                    case 2:
                        recommendedAlerts = calculateRecommendedAlerts(recentEventsEarthquake);
                        break;
                    case 3:
                        recommendedAlerts = calculateRecommendedAlerts(recentEventsTornado);
                        break;
                }
                Log.d("CALLABLE FINISHED","OK");

                return recommendedAlerts;
            });
        }

        try {
            countDownLatch.await(); //wait for "listener for single value event" to finish
            futures = executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ArrayList<ArrayList<Event>> allRecommendedAlerts = new ArrayList<>();
        for (Future<ArrayList<Event>> future: futures) {
            try {
                ArrayList<Event> recommendedAlerts = future.get();
                allRecommendedAlerts.add(recommendedAlerts);
                System.out.println(recommendedAlerts);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Convert ArrayList<ArrayList<Event>> to JSON string
        Gson gson = new Gson();
        String allRecommendedAlertsJson = gson.toJson(allRecommendedAlerts);

        // Put the JSON string in a Data object
        Data outputData = new Data.Builder()
                .putString("all_recommended_events", allRecommendedAlertsJson)
                .build();

        return Result.success(outputData);
    }


    //groups Events that refer to the same disaster and calculates/returns which Event in each group recommends as an alert
    private ArrayList<Event> calculateRecommendedAlerts(ArrayList<Event> recentEventsOfType) {

        ArrayList<Event> sortedEvents;
        ArrayList<ArrayList<Event>> clusteredEventLists = new ArrayList<>();
        ArrayList<ArrayList<Event>> listsToRemove = new ArrayList<>();
        ArrayList<Event> recommendedAlerts = new ArrayList<>();

        sortedEvents = recentEventsOfType.stream() //sort items in list by timestamp in milliseconds and return them sorted
                .sorted(Comparator.comparingLong(event -> Helper.dateToTimestamp(event.getTimestamp())))
                .collect(Collectors.toCollection(ArrayList::new));

        sortedEvents.forEach(event1 -> { //for each event1
            ArrayList<Event> clusteredEvents = new ArrayList<>();
            clusteredEvents.add(event1);

            long timestampEvent1 = Helper.dateToTimestamp(event1.getTimestamp());

            for (Event event2 : sortedEvents) { //loop the arraylist again
                if (event1.equals(event2)) { //if it's the same event continue to next event2
                    continue;
                }

                long timestampEvent2 = Helper.dateToTimestamp(event2.getTimestamp());

                if (! (timestampEvent1 + ACCEPTABLE_TIME_DIFF >= timestampEvent2)) {
                    //the list is ordered by timestamp so if event2 is not within the acceptable time difference between events,
                    // no other event2 will be and we can move to the next event1
                    break;
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
            Log.d("LISTS NUM", String.valueOf(clusteredEventLists.size()));

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

        clusteredEventLists.forEach(list -> {
            //every list is made by points near the first one (location and time wise) and we only keep the largest similar lists
            //so we recommend the first event in each list because among other similar points, that is the one that led to the best list
            recommendedAlerts.add(list.get(0));
        });
        return recommendedAlerts;
    }
}
