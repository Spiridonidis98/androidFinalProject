package com.kouts.spiri.smartalert.POJOs;


public class Alert {

    private String aId; // alert id will be created based on the eventType and the timestamp of the first element of the group.
    private String timestamp;
    private int warning; // value of the warning importance if events.size  < 5 = 0 || events.size >=5 && events.size < 10 = 1 || events.size >= 10 = 2
    private EventTypes eventType;

    // Default no-argument constructor
    public Alert() {
    }
    public Alert(String aId, String timestamp, int warning, EventTypes eventType) {
        this.aId = aId;
        this.timestamp = timestamp;
        this.warning = warning;
        this.eventType = eventType;
    }

    // Getter methods
    public String getaId() {
        return aId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getWarning() {
        return warning;
    }

    public EventTypes getEventType() {
        return eventType;
    }
}
