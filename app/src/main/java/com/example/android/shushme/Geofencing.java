package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Geofencing implements ResultCallback {

    private static final String TAG = Geofencing.class.getSimpleName();

    private static final float GEOFENCE_RADIUS = 50;
    private static final long GEOFENCE_TIMEOUT = TimeUnit.DAYS.toMillis(24);

    private GoogleApiClient mClient;
    private Context mContext;
    List<Geofence> mGeofenceList;
    PendingIntent mGeofencePendingIntent;

    public Geofencing(Context context, GoogleApiClient client) {
        this.mContext = context;
        this.mClient = client;
        this.mGeofencePendingIntent = null;
        this.mGeofenceList = new ArrayList<>();
    }

    public void registerAllGeofences() {
        if (mClient == null || !mClient.isConnected() || mGeofenceList == null
                || mGeofenceList.size() == 0) {
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mClient, getGeofencingRequest(), getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void unregisterAllGeofences() {
        if (mClient == null || !mClient.isConnected()) {
            return;
        }

        try {
            LocationServices.GeofencingApi.removeGeofences(
                    mClient, getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void updateGeofencesList(PlaceBuffer places) {
        if (places == null || places.getCount() == 0) return;

        for (Place place : places) {
            String placeId = place.getId();
            double placeLatitude = place.getLatLng().latitude;
            double placeLongitude = place.getLatLng().longitude;

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeId)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLatitude, placeLongitude, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            mGeofenceList.add(geofence);
        }
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.e(TAG, "Error adding/removing geofence: " + result.getStatus().toString());
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }
}
