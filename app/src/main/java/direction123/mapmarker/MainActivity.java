package direction123.mapmarker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import direction123.mapmarker.Model.GasStation;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener,
        ValueEventListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_LOCATION = "location";
    private static final String KEY_GAS_STATION_LIST = "gas_station_list";

    private static final String GAS_STATION_SHELL = "shell";
    private static final String GAS_STATION_CHEVRON = "chevron";
    private static final Double GAS_STATION_DISTANCE = 160.934; //meter; 1 mile = 1609.34 meters

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private ArrayList<GasStation> mGasStationList;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // A default location (Sydney, Australia) when location permission is not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        // Read from the database
        mDatabase.child("gasStations").addValueEventListener(this);

        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mGasStationList = savedInstanceState.getParcelableArrayList(KEY_GAS_STATION_LIST);
        }
        setContentView(R.layout.activity_main);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        // This method is called once with the initial value and again
        // whenever data at this location is updated.
        Object objects =  dataSnapshot.getValue();
        mGasStationList = new ArrayList<>();
        if(objects != null) {
            for (Object obj : ((HashMap) objects).values()) {
                if(obj != null) {
                    HashMap hashMap = (HashMap) obj;
                    GasStation gasStation = new GasStation(
                            (String) hashMap.get("userName"),
                            (String) hashMap.get("latitude"),
                            (String) hashMap.get("longitude"),
                            (String) hashMap.get("type")
                    );
                    mGasStationList.add(gasStation);
                }
            }
        }
        displayExistingMarker();  //multiple users synch
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        // Failed to read value
        Log.w(TAG, "Failed to read value.", databaseError.toException());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            outState.putParcelableArrayList(KEY_GAS_STATION_LIST, mGasStationList);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            Toast.makeText(this, currentUser.getEmail() + " is logged in MainActivityActivity", Toast.LENGTH_SHORT).show();
            getSupportActionBar().setTitle(currentUser.getEmail());
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();

        mMap.setOnMyLocationClickListener(this);
        mMap.setOnMapLongClickListener(this);

        displayExistingMarker();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.log_out:
                logoutUser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void logoutUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, currentUser.getEmail() + " is logged in/logging out MainActivity", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void getLocationPermission() {
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
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
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "You are here", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapLongClick(final LatLng latLng) {
        Toast.makeText(this, "long click" + latLng.latitude + ": " + latLng.longitude, Toast.LENGTH_SHORT).show();
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            Location clickedLocation = new Location("clickedLocation");
                            clickedLocation.setLatitude(latLng.latitude);
                            clickedLocation.setLongitude(latLng.longitude);
                            if(mLastKnownLocation.distanceTo(clickedLocation) > GAS_STATION_DISTANCE) {
                                Toast.makeText(getApplicationContext(), "Gas station too far", Toast.LENGTH_SHORT).show();
                            } else {
                                dropMarker(clickedLocation.getLatitude(), clickedLocation.getLongitude());
                            }
                        } else {

                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void dropMarker(final double latitude, final double longitude) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialog_title)
                .setItems(R.array.gas_station, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                dropMarker(latitude, longitude, GAS_STATION_SHELL);
                                break;
                            case 1:
                                dropMarker(latitude, longitude, GAS_STATION_CHEVRON);
                                break;
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void dropMarker(double latitude, double longitude, String type) {
        switch (type) {
            case GAS_STATION_SHELL:
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.shell)));
                break;
            case GAS_STATION_CHEVRON:
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.chevron)));
                break;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        writeNewGasStation(currentUser.getEmail(), String.valueOf(latitude),
                String.valueOf(longitude), type);
    }

    private void displayExistingMarker() {
        if(mMap != null) {
            if(mGasStationList != null && mGasStationList.size() != 0) {
                for(int i = 0; i < mGasStationList.size(); i++) {
                    GasStation gasStation = mGasStationList.get(i);
                    switch (gasStation.type) {
                        case GAS_STATION_SHELL:
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(gasStation.latitude),
                                            Double.parseDouble(gasStation.longitude)))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.shell)));
                            break;
                        case GAS_STATION_CHEVRON:
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(gasStation.latitude),
                                            Double.parseDouble(gasStation.longitude)))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.chevron)));
                            break;
                    }
                }
            }
        }

    }

    private void writeNewGasStation(String userName, String latitude, String longitude, String type) {
        String key = mDatabase.child("gasStations").push().getKey();
        GasStation gasStation = new GasStation(userName, latitude, longitude, type);
        Map<String, Object> gasStationValues = gasStation.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/gasStations/" + key, gasStationValues);
        mDatabase.updateChildren(childUpdates);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Toast.makeText(this, "Marker is clicked", Toast.LENGTH_SHORT).show();
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            String query = "google.navigation:q=" + mLastKnownLocation.getLatitude()
                                    + "," + mLastKnownLocation.getLongitude();
                            Uri gmmIntentUri = Uri.parse(query);
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            startActivity(mapIntent);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
        return false;
    }
}