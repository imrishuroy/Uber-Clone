package com.parse.starter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Handler;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import java.util.logging.LogRecord;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    LocationManager locationManager;

    LocationListener locationListener;

    Button callUberButton;

    Boolean requestActive = false;

     Handler handler = new Handler();

     TextView infoTextView;

     Boolean driverActive = false;

     public void checkForUpdates(){

         ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
         query.whereEqualTo("usernames",ParseUser.getCurrentUser().getUsername());
         query.whereExists("driverUsername");

         query.findInBackground(new FindCallback<ParseObject>() {
             @Override
             public void done(List<ParseObject> objects, ParseException e) {
                 if(e==null && objects.size() > 0){

                     driverActive = true;

                     ParseQuery<ParseUser> query = ParseUser.getQuery();

                     query.whereEqualTo("username",objects.get(0).getString("driverUsername"));

                     query.findInBackground(new FindCallback<ParseUser>() {
                         @Override
                         public void done(List<ParseUser> objects, ParseException e) {
                             if(e == null && objects.size() > 0){
                                 ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");

                                 if (Build.VERSION.SDK_INT < 23|| ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                     Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


                                     if (lastKnownLocation != null) {

                                         ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());


                                         Double distanceInMiles = driverLocation.distanceInMilesTo(userLocation);

                                         if (distanceInMiles < 0.01) {

                                             infoTextView.setText("Your Driver Is Arrived");


                                             ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
                                             query.whereEqualTo("usernames", ParseUser.getCurrentUser().getUsername());

                                             query.findInBackground(new FindCallback<ParseObject>() {
                                                 @Override
                                                 public void done(List<ParseObject> objects, ParseException e) {
                                                     if (e == null) {
                                                         for (ParseObject object : objects) {
                                                             object.deleteInBackground();

                                                         }
                                                     }

                                                 }
                                             });

                                             handler.postDelayed(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     infoTextView.setText("");
                                                     callUberButton.setVisibility(View.VISIBLE);      // to hide button
                                                     callUberButton.setText("Call An Uber");
                                                     requestActive = false;
                                                     driverActive = false;

                                                 }
                                             }, 5000);

                                             infoTextView.setText("Your Driver Is Arrived");


                                         } else {


                                             Double distanceOneDp = (double) Math.round(distanceInMiles * 10) / 10;


                                             infoTextView.setText("Your Driver Is " + distanceOneDp.toString() + " Miles Away");

                                             LatLng driverLocationLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());

                                             LatLng requestLocationLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                                             ArrayList<Marker> markers = new ArrayList<>();

                                             markers.clear();

                                             markers.add(mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Driver Location")));
                                             markers.add(mMap.addMarker(new MarkerOptions().position(requestLocationLatLng).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));

                                             LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                             for (Marker marker : markers) {
                                                 builder.include(marker.getPosition());
                                             }
                                             LatLngBounds bounds = builder.build();

                                             int padding = 60;
                                             CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                                             mMap.animateCamera(cu);

                                             callUberButton.setVisibility(View.INVISIBLE);      // to hide button

                                             handler.postDelayed(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     checkForUpdates();

                                                 }
                                             }, 2000);


                                         }


                                     }
                                 }

                             }
                          }
                     });


                 }


             }
         });
     }



    public  void logout(View view){
        ParseUser.logOut();

        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
    }

    public  void callUber(View view) {

        Log.i("Info", "Uber Called");

        if (requestActive) {

            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");   // Query

            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                object.deleteInBackground();
                            }
                            requestActive = false;
                            callUberButton.setText("Call An Uber");
                        }
                    }
                }
            });

        } else {


            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lasKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lasKnownLocation != null) {

                    ParseObject request = new ParseObject("Request");  // Uber request parse class
                    request.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lasKnownLocation.getLatitude(), lasKnownLocation.getLongitude());

                    request.put("location", parseGeoPoint);

                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                callUberButton.setText("Cancel Uber");
                                requestActive = true;

                              checkForUpdates();

                            }
                        }
                    });

                } else {
                    Toast.makeText(this, "Could Not Find Location", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                    Location lasKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    upDateMap(lasKnownLocation);


                }
            }

        }

    }


    public void upDateMap(Location location) {
        if (driverActive != false) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        callUberButton = findViewById(R.id.callUberButton);

        infoTextView = findViewById(R.id.infoTextView);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");   // Query

        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (objects.size() > 0) {
                        requestActive = true;
                        callUberButton.setText("Cancel Uber");

                        checkForUpdates();
                    }
                }
            }
        });

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                upDateMap(location);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if (Build.VERSION.SDK_INT < 23) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        } else {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);


            } else {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


                if (lastKnownLocation != null) {

                    upDateMap(lastKnownLocation);

                }


            }


        }

    }
}
