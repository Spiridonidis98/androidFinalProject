package com.kouts.spiri.smartalert.Assistance;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kouts.spiri.smartalert.POJOs.Alert;
import com.kouts.spiri.smartalert.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder>{

    private ArrayList<Alert> alerts;
    private Context context;
    private LayoutInflater mInflater;
    public NotificationAdapter(Context context, ArrayList<Alert> data) {
        this.alerts = data;
        this.context = context;
        mInflater = LayoutInflater.from(context);
    }


    @Override
    public NotificationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.notification, parent, false);
        return new NotificationAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alert item = alerts.get(position);
        holder.eventTypeTextView.setText(item.getEventType() + " " + item.getTimestamp());
        holder.alertIcon.setImageResource(R.drawable.baseline_crisis_alert_24);
        GradientDrawable border = (GradientDrawable) holder.itemView.getBackground();
        border.setStroke(5, Helper.getColorForEvent(item.getEventType()));
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView alertIcon;
        TextView eventTypeTextView;

        ViewHolder(View itemView) {
            super(itemView);
            alertIcon = itemView.findViewById(R.id.alertImg);
            eventTypeTextView = itemView.findViewById(R.id.alert_type);
        }
    }

}
