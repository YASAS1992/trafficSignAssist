package com.example.yasas.trafficsignassist;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 10;
    public Location mLastKnownLocation = null;
    ArrayList<TrafficSign> trafficSigns;
    TextToSpeech tts;


    DatabaseReference dbTrafficSigns;
    ArrayList<Signs> signs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);



        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tts=new TextToSpeech(MapsActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if(status == TextToSpeech.SUCCESS){
                    int result=tts.setLanguage(Locale.US);
                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                    }else{
//                        ConvertTextToSpeech();
                    }
                }
            }
        });

        inializedData();


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        LatLng myLocation;
        LocationTrack locationTrack = new LocationTrack(MapsActivity.this);

        myLocation = new LatLng(locationTrack.latitude, locationTrack.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17f));

        for (int i = 0; i < trafficSigns.size(); i++) {
            addMarkers(trafficSigns.get(i),mMap);
        }


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                if (marker.getTitle().equals("STOP")){
                    ConvertTextToSpeech("Stop sign ahead");
                }else if(marker.getTitle().equals("CLOSED")){
                    ConvertTextToSpeech("Closed sign ahead");
                }else if(marker.getTitle().equals("TURN_RIGHT")){
                    ConvertTextToSpeech("turn in to right sign ahead");
                }

                return false;
            }
        });

        // Add a marker

       /* if(distance(6.967509,79.921101,mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude(),"K")<1){
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng latLng = new LatLng( 6.967509,79.921101);
            markerOptions.position(latLng);
            markerOptions.title(latLng.latitude + " : " + latLng.longitude);
            googleMap.clear();
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.addMarkers(markerOptions);
        }else{
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng latLng = new LatLng( 6.967974,79.893362);
            markerOptions.position(latLng);
            markerOptions.title(latLng.latitude + " : " + latLng.longitude);
            googleMap.clear();
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.addMarkers(markerOptions);
        }*/

    }

    private void ConvertTextToSpeech(String msg) {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void addMarkers(TrafficSign trafficSign, GoogleMap googleMap){

        LocationTrack locationTrack = new LocationTrack(MapsActivity.this);
        double latitude = locationTrack.latitude;
        double longitude = locationTrack.longitude;

        double d = distance(latitude,longitude,trafficSign.lattitude,trafficSign.Longtitude,"K");

        if(d<=1.0){
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng latLng = new LatLng( trafficSign.lattitude,trafficSign.Longtitude);
            markerOptions.position(latLng);
            markerOptions.title(trafficSign.type);

//        googleMap.clear();
//        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(trafficSign.type)
                    .icon(BitmapDescriptorFactory.fromResource(trafficSign.icon)));
        }


    }

    public void inializedData(){
        trafficSigns = new ArrayList<>();
        signs = new ArrayList<>();
        dbTrafficSigns = FirebaseDatabase.getInstance().getReference("Signs");
        dbTrafficSigns.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.v("FB",dataSnapshot.getKey());
                TrafficSign sign = dataSnapshot.getValue(TrafficSign.class);
                if(sign.type.equals("STOP")){
                    sign.icon = R.drawable.stop;
                }else if(sign.type.equals("CLOSED")){
                    sign.icon = R.drawable.closed;
                }else if(sign.type.equals("TURN_RIGHT")){
                    sign.icon = R.drawable.turn_right;
                }
                trafficSigns.add(sign);
                getDeviceLocation();
//                if (dataSnapshot.exists()) {
//                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                        String gg = snapshot.getValue(String.class);
////                        Signs sign = snapshot.getValue(Signs.class);
////                        signs.add(sign);
//                    }
//                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                TrafficSign sign = dataSnapshot.getValue(TrafficSign.class);
                if(sign.type.equals("STOP")){
                    sign.icon = R.drawable.stop;
                }else if(sign.type.equals("CLOSED")){
                    sign.icon = R.drawable.closed;
                }else if(sign.type.equals("TURN_RIGHT")){
                    sign.icon = R.drawable.turn_right;
                }
                trafficSigns.add(sign);
                getDeviceLocation();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void newMap() {
//        6.967509, 79.921101
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }

    private void getDeviceLocation() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            newMap();
            return;
        } else {
            mFusedLocationClient.getLastLocation().addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    newMap();
                }
            });
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        mLastKnownLocation = location;
                        newMap();
                    }
                }
            });
        }


    }

    public double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        Toast.makeText(MapsActivity.this, String.valueOf(distance), Toast.LENGTH_SHORT).show();

        return Math.sqrt(distance);
    }

    private double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit.equals( 'K')) {
            dist = dist * 1.609344;
        } else if (unit.equals( 'N')) {
            dist = dist * 0.8684;
        }
        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts decimal degrees to radians             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts radians to decimal degrees             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {


                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mLastKnownLocation = location;
                                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                        .findFragmentById(R.id.map);
                                mapFragment.getMapAsync(MapsActivity.this);
                            }
                        }
                    });
                }

            }
        }
    }

}
