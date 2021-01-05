package com.example.citizensafety;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.internal.InternalTokenProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class TrackingService extends Service {
    private static final String TAG = TrackingService.class.getSimpleName();
    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Unregister the BroadcastReceiver when the notification is tapped//
            unregisterReceiver(stopReceiver);
            //Stop the Service//
            stopSelf();
        }
    };
    private double latitude=0.0, longitude=0.0,latitude1=0.0,longitude1=0.0;
    private FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mGetReference = mDatabase.getReference("gudlavalleru");
    //private DatabaseReference unsafeRefGdlv = mGetReference.child("gudlavalleru");
    private IBinder mBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildNotification();
        loginToFirebase();
        fetchFromFirebase();
        startActivity(new Intent(this, MapsActivity.class));
    }

    //Create the persistent notification//
    private void buildNotification() {
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);
        // Create the persistent notification
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.tracking_enabled_notif))
//Make this notification ongoing so it can’t be dismissed by the user//
                .setOngoing(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.tracking_enabled);
        startForeground(1, builder.build());
    }

    private void loginToFirebase() {
//Authenticate with Firebase, using the email and password we created earlier//
        String email = getString(R.string.test_email);
        String password = getString(R.string.test_password);
//Call OnCompleteListener if the user is signed in successfully//

        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
//If the user has been authenticated...//
                if (task.isSuccessful()) {
//...then call requestLocationUpdates//
                    requestLocationUpdates();
                } else {
//If sign in fails, then log the error//
                    Log.d(TAG, "Firebase authentication failed");
                }
            }
        });
    }

    //Initiate the request to track the device's location//
    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();
//Specify how often your app should request the device’s location//
        request.setInterval(10000);
//Get the most accurate location data available//
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        final String path = getString(R.string.firebase_path);
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
//If the app currently has access to the location permission...//
        if (permission == PackageManager.PERMISSION_GRANTED) {
//...then request location updates//
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
//Get a reference to the database, so your app can perform read and write operations//
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                    Location location = locationResult.getLastLocation();
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    latitude = Math.floor(latitude*1000)/1000;
                    longitude = Math.floor(longitude*1000)/1000;
                    //Toast.makeText(getApplicationContext(), (CharSequence) location, Toast.LENGTH_LONG).show();

                    /*LocationListener locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            //get the location name from latitude and longitude
                            Geocoder geocoder = new Geocoder(getApplicationContext());
                            try {
                                List<Address> addresses =
                                        geocoder.getFromLocation(latitude, longitude, 1);
                                String result = addresses.get(0).getLocality() + ":";
                                //result += addresses.get(0).getCountryName()+":";
                                //result += addresses.get(0).getPostalCode()+":";
                                //result += addresses.get(0).getAddressLine(0);
                                //Toast.makeText(getApplicationContext(),result, Toast.LENGTH_LONG).show();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onStatusChanged(String s, int i, Bundle bundle) {

                        }

                        @Override
                        public void onProviderEnabled(String s) {

                        }

                        @Override
                        public void onProviderDisabled(String s) {

                        }
                    };*/
                        //Save the location data to the database//
                    ref.setValue(location);
                }
            }, null);
        }
    }

    private void fetchFromFirebase() {
        mGetReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                /*if (dataSnapshot.exists()) {
                    HashMap<String, Object> dataMap = (HashMap<String, Object>) dataSnapshot.getValue();
                    for (String key : dataMap.keySet()) {
                        Object data = dataMap.get(key);
                        try {
                            HashMap<String, Object> userData = (HashMap<String, Object>) data;
                            latitude1 = (double) userData.get("latitude");
                            longitude1 = (double) userData.get("longitude");
                            Toast.makeText(getApplicationContext(), (int) latitude1, Toast.LENGTH_LONG).show();
                            Toast.makeText(getApplicationContext(), (int) longitude1, Toast.LENGTH_LONG).show();
                            checkLocation();
                        } catch (ClassCastException cce) {
// If the object can’t be casted into HashMap, it means that it is of type String.
                            try {
                                String mString = String.valueOf(dataMap.get(key));
                            } catch (ClassCastException cce2) {

                            }
                        }
                    }
                }*/
                latitude1 =  dataSnapshot.child("latitude").getValue(Double.class);
                longitude1 = dataSnapshot.child("longitude").getValue(Double.class);
                latitude1 = Math.floor(latitude1*1000)/1000;
                longitude1 = Math.floor(longitude1*1000)/1000;
                if(latitude == latitude1 && longitude == longitude1) {
                    checkLocation();
                }/*else{
                    Toast.makeText(getApplicationContext(), "Family Safe", Toast.LENGTH_LONG).show();
                }*/

            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                Log.e(TAG, "Failed to read app title value.", databaseError.toException());
            }
        });

    }
    private void checkLocation(){
        Toast.makeText(getApplicationContext(), "Danger", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, SafetyMeasures.class));

    }


    public class MyBinder extends Binder {
        TrackingService getService() {
            return TrackingService.this;
        }
    }

}
