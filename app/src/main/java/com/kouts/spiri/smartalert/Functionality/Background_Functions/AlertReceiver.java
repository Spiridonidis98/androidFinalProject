package com.kouts.spiri.smartalert.Functionality.Background_Functions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlertReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) { //restarts the AlertNotificationService when it is scheduled to
        Intent serviceIntent = new Intent(context, AlertNotificationService.class);
        context.startService(serviceIntent);
    }
}
