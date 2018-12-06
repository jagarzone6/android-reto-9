package com.example.jorge.reto9;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceDetectionClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceDetectionClient mPlaceDetectionClient;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private Bitmap resizedBitmap;
    private Bitmap resizedBitmapHospital;
    private RequestQueue requstQueue;
    private final String temp = "AIzaSyACOaMdbVu25gJFDBBzaoam4y6abjTFWDE";
    private final String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=" + temp;
    private Button resetButton;

    private SharedPreferences mPrefs;

    private String mRadius;
    private int status;
    public Place placea;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mRadius = mPrefs.getString("mRadius", "2500");

        requstQueue = Volley.newRequestQueue(this);
        resetButton = (Button) findViewById(R.id.current_loc);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocationNearbyPlaces();
            }
        });

        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("restaurant", "drawable", getPackageName()));
        Bitmap imageBitmapH = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("hospital", "drawable", getPackageName()));
        resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, 100, 100, false);
        resizedBitmapHospital = Bitmap.createScaledBitmap(imageBitmapH, 100, 100, false);
        // Construct a PlaceDetectionClient.
        //mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mPlaceDetectionClient = Places.
                getPlaceDetectionClient(this, null);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                placea = place;
                status =1;
                searchNearbyPlacesToGivenLocation(placea.getLatLng(), (String) placea.getName());
                resetButton.setClickable(true);
                resetButton.setBackgroundColor(Color.parseColor("#ff0099cc"));


            }

            @Override
            public void onError(Status status) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(status==0){
        getDeviceLocationNearbyPlaces();
        }else {
            searchNearbyPlacesToGivenLocation(placea.getLatLng(), (String) placea.getName());
        }
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
        getLocationPermission();
        // Get the current location of the device and set the position of the map.
        getDeviceLocationNearbyPlaces();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocationNearbyPlaces() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        status =0;
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            LatLng loc = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                            searchNearbyPlacesToGivenLocation(loc, "My current location");
                            resetButton.setClickable(false);
                            resetButton.setBackgroundColor(Color.GRAY);
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);

                        }
                    }
                });
            } else {
                getLocationPermission();
                getDeviceLocationNearbyPlaces();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void addPlacesToMap(JSONArray results, LatLng current, String currentName, Boolean hospital) throws JSONException {
        MarkerOptions mo = new MarkerOptions();

        if(!hospital) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(current).title(currentName));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15.0f));
            mo.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
        } else {
            mo.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmapHospital));
        }



        for (int i = 0; i < results.length(); i++) {
            JSONObject place = (JSONObject) results.get(i);
            mo.position(
                    new LatLng(
                            place.getJSONObject("geometry").getJSONObject("location").getDouble("lat"),
                            place.getJSONObject("geometry").getJSONObject("location").getDouble("lng")
                    )
            ).title(place.getString("name"));

            mMap.addMarker(mo);
        }


    }

    private void searchNearbyPlacesToGivenLocation(final LatLng location, final String name) {
        mRadius = mPrefs.getString("mRadius", "2500");
        String req_url = url + "&radius="+mRadius+"&type=restaurant&location="
                + location.latitude + "," + location.longitude;
        getData(req_url, new VolleyCallback() {
            @Override
            public void notifySuccess(JSONObject result) throws JSONException {
                System.out.println(result.toString());
                JSONArray places = result.getJSONArray("results");
                addPlacesToMap(places, location, name,false);
                String req_url2 = url + "&radius="+mRadius+"&type=hospital&location="
                        + location.latitude + "," + location.longitude;
                getData(req_url2, new VolleyCallback() {
                    @Override
                    public void notifySuccess(JSONObject result) throws JSONException {
                        System.out.println(result.toString());
                        JSONArray places = result.getJSONArray("results");
                        addPlacesToMap(places, location, name,true);
                    }

                    @Override
                    public void notifyError(VolleyError error) {
                        System.out.println("ERROR: " + error.toString());
                    }
                });
            }

            @Override
            public void notifyError(VolleyError error) {
                System.out.println("ERROR: " + error.toString());
            }
        });
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public void getData(String url, final VolleyCallback mResultCallback) {

        JsonObjectRequest jsonobj = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (mResultCallback != null) {
                            try {
                                mResultCallback.notifySuccess(response);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (mResultCallback != null) {
                            mResultCallback.notifyError(error);
                        }
                    }
                }
        ) {
            //here I want to post data to sever
        };
        requstQueue.add(jsonobj);

    }

    public interface VolleyCallback {
        void notifySuccess(JSONObject result) throws JSONException;

        void notifyError(VolleyError error);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        super.onCreateOptionsMenu(menu);
        return true;

    }
    static final int DIALOG_QUIT_ID = 1;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.quit:
                showDialog(DIALOG_QUIT_ID);
                return true;
            case R.id.preferences:
                startActivityForResult(new Intent(this, Settings.class), 0);
                return true;

        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(id) {
            case DIALOG_QUIT_ID:
// Create the quit confirmation dialog
                builder.setMessage("Are you sure you want to quit ?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                MapsActivity.this.finish();
                            }
                        })
                        .setNegativeButton("No", null);
                dialog = builder.create();
                break;

        }
        return dialog;
    }
}
