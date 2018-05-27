package com.jife.stonyleverage.rocky.visitorsresort;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.Common;
import com.Model.MyPlaces;
import com.Model.Results;
import com.Remote.IGoogleAPIService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private static final int MY_PERMISSION_CODE = 1000;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleAPIClient;

    private double latitude, longitude;
    private Location mLastLocation;
    private Marker mMarker;
    private LocationRequest mLocationRequest;

    IGoogleAPIService mServices;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mServices = Common.getGoogleAPIService();

        //Request Runtime permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            checkLocationPermission();
        }

        BottomNavigationView bottomNavigationView = (BottomNavigationView)findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId())
                {
                    case R.id.action_hospital:
                        nearByPlace("hospital");
                        break;
                    case R.id.action_market:
                        nearByPlace("market");
                        break;
                    case R.id.action_restaurant:
                        nearByPlace("restaurant");
                        break;
                    case R.id.action_school:
                        nearByPlace("school");
                        break;
                        default:
                            break;
                }
                return true;
            }
        });
    }

    private void nearByPlace(final String placeType) {
        mMap.clear();
        String url = getUrl(latitude,longitude,placeType);

        mServices.getNearbyPlaces(url).enqueue(new Callback<MyPlaces>() {
            @Override
            public void onResponse(Call<MyPlaces> call, Response<MyPlaces> response) {
                if(response.isSuccessful())
                {
                    for(int i=0;i<response.body().getResults().size();i++)
                    {
                        MarkerOptions markerOptions = new MarkerOptions();
                        Results googlePlace = response.body().getResults().get(i);
                        double lat = Double.parseDouble(String.valueOf(googlePlace.getGeometry().getLocation().getLat()));
                        double lng = Double.parseDouble(String.valueOf(googlePlace.getGeometry().getLocation().getLng()));
                        String placeName = googlePlace.getName();
                        String vicinity = googlePlace.getVicinity();
                        LatLng latLng = new LatLng(lat,lng);
                        markerOptions.position(latLng);
                        markerOptions.title(placeName);
                        if (placeType.equals("hospital"))
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_local_hospital_black_24dp));
                            else if (placeType.equals("market"))
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shopping_cart_black_24dp));
                        else if (placeType.equals("restaurant"))
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_restaurant_black_24dp));
                        else if (placeType.equals("school"))
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_school_black_24dp));
                        else
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        //Move Camera
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                    }
                }
            }

            @Override
            public void onFailure(Call<MyPlaces> call, Throwable t) {

            }
        });
    }

    private String getUrl(double latitude, double longitude, String placeType) {
       StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
       googlePlacesUrl.append("location"+latitude+","+longitude);
       googlePlacesUrl.append("&radius="+1000);
       googlePlacesUrl.append("&type="+placeType);
       googlePlacesUrl.append("&sensor=true");
       googlePlacesUrl.append("&key="+getResources().getString(R.string.google_maps_key));
        Log.d("getUrl",googlePlacesUrl.toString());
        return googlePlacesUrl.toString();
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, new String[]{

                        Manifest.permission.ACCESS_FINE_LOCATION
                }, MY_PERMISSION_CODE);

            else
                ActivityCompat.requestPermissions(this, new String[]{

                        Manifest.permission.ACCESS_FINE_LOCATION
                }, MY_PERMISSION_CODE);
            return false;
        }
            else
                return true;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case MY_PERMISSION_CODE:

                {
                    if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    {
                        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        {
                            if (mGoogleAPIClient == null)
                                buildGoogleAPIClient();
                            mMap.setMyLocationEnabled(true);
                        }
                    }else
                        {
                            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                        }
                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Init Google Play Services

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                buildGoogleAPIClient();
                mMap.setMyLocationEnabled(true);
            }


        }else
            {
                buildGoogleAPIClient();
                mMap.setMyLocationEnabled(true);
            }
    }

    private synchronized void buildGoogleAPIClient() {
        mGoogleAPIClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleAPIClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIClient,mLocationRequest,this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleAPIClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if(mMarker != null)
            mMarker.remove();

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        LatLng latLng = new LatLng(latitude,longitude);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Your Position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        mMarker = mMap.addMarker(markerOptions);

        //Move Camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        if (mGoogleAPIClient != null)

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleAPIClient,this);

    }
}
