package com.kouts.spiri.smartalert.Assistance;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.Fragments.EventStatisticsFragment;
import com.kouts.spiri.smartalert.Functionality.MapsActivity;
import com.kouts.spiri.smartalert.POJOs.Event;
import com.kouts.spiri.smartalert.POJOs.EventTypes;
import com.kouts.spiri.smartalert.R;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    private Context mContext;
    private List<Event> mData;
    private LayoutInflater mInflater;

    public Adapter(Context context, List<Event> data) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Event item = mData.get(position);
        holder.eventTypeTextView.setText(item.getAlertType() + "");
        holder.eventCommentTextView.setText(item.getComment());
        holder.eventTimeStampTextView.setText(item.getTimestamp());
        // Set image resource or load image into holder.mapIconImageView here
        GradientDrawable border = (GradientDrawable) holder.itemView.getBackground();
        border.setStroke(5, getColorForEvent(item.getAlertType()));
        getEventImage(item, holder.mapIconImageView);

        holder.itemView.setOnClickListener(v -> showEventInfo(item));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mapIconImageView;
        TextView eventTypeTextView;
        TextView eventCommentTextView;
        TextView eventTimeStampTextView;

        ViewHolder(View itemView) {
            super(itemView);
            mapIconImageView = itemView.findViewById(R.id.map_icon);
            eventTypeTextView = itemView.findViewById(R.id.event_type);
            eventCommentTextView = itemView.findViewById(R.id.event_comment);
            eventTimeStampTextView = itemView.findViewById(R.id.event_timestamp);


        }
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

    private void getEventImage(Event event, ImageView imageView) {

        FirebaseDB.getImageFromStorage(event.getImage(), new FirebaseDB.FirebaseStorageListener() {
            @Override
            public void onImageRetrieved(Uri image) {
                event.setImageURI(image);
                Glide.with(mContext)
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
        Dialog dialog = new Dialog(mContext);
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

        Glide.with(mContext)
                .load(event.getImageURI())
                .error(R.drawable.home)
                .into(imageView);

        mapIcon.setOnClickListener(v -> {
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(event.getLatitude());
            location.setLongitude(event.getLongitude());

            Intent intent = new Intent(mContext, MapsActivity.class);
            // Create Intent using LocationUtils method
            intent.putExtra("Location", location);
            intent.putExtra("EventType", event.getAlertType());
            intent.putExtra("EventTime", event.getTimestamp());

            startActivity(mContext,intent,null);

        });

        dialog.show();


    }
}