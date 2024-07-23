package com.kouts.spiri.smartalert.Functionality;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kouts.spiri.smartalert.Services.AlertNotificationService;

public class AlertReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, AlertNotificationService.class);
        context.startService(serviceIntent);
    }
}
