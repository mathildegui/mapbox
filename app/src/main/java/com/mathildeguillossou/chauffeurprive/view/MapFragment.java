package com.mathildeguillossou.chauffeurprive.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.services.android.geocoder.ui.GeocoderAutoCompleteView;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.geocoding.v5.models.CarmenFeature;
import com.mathildeguillossou.chauffeurprive.R;
import com.mathildeguillossou.chauffeurprive.model.MyPlaces;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {

    private static final float ZOOM = 16;
    private static final int PERMISSIONS_LOCATION = 0;

    private static final String LATITUDE  = "latitude";
    private static final String LONGITUDE = "longitude";

    private double latitude  = 0.0;
    private double longitude = 0.0;

    private int mEvent;
    private MapboxMap mMap;
    private Location mLastLocation;
    private ImageView MdropPinView;
    private LocationServices mLocationServices;
    private OnMenuInteractionListener mListener;

    private boolean isClicked = false;

    @BindView(R.id.mapview) MapView mapView;
    @BindView(R.id.query) GeocoderAutoCompleteView autocomplete;

    private Realm realm;

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapFragment.
     */
    public static MapFragment newInstance() {
        return new MapFragment();
    }

    public static MapFragment newInstance(double latitude, double longitude) {
        MapFragment fragment = new MapFragment();
        Bundle b = new Bundle();
        b.putDouble(LATITUDE, latitude);
        b.putDouble(LONGITUDE, longitude);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);


        if(getArguments() != null) {
            latitude = getArguments().getDouble(LATITUDE);
            longitude = getArguments().getDouble(LONGITUDE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        realm = Realm.getDefaultInstance();

        MapboxAccountManager.start(getActivity(), getString(R.string.access_token));

        initLocation();
        initMapView(savedInstanceState);
        initGeocode();
        initCenterMarker();
    }

    private void registerPlace(final String address,
                               final String city,
                               final String country,
                               final double latitude,
                               final double longitude) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {

                MyPlaces place = new MyPlaces();

                place.address    = address;
                place.city       = city;
                place.country    = country;
                place.timestramp = String.valueOf(new Date().getTime());
                place.latitude   = latitude;
                place.longitude  = longitude;


                //FIXME: implement here some timeout because in case user drag multi times it will not work
                RealmResults<MyPlaces> list = realm.where(MyPlaces.class).findAllSorted("timestramp");
                if(list != null ) {
                    if(list.size() == 15)
                        list.deleteFirstFromRealm();
                        //list.deleteLastFromRealm();

                    realm.insert(place);
                }
            }
        });
    }

    /**
     * Init the Location services
     */
    private void initLocation() {
        mLocationServices = LocationServices.getLocationServices(getActivity());
        mLastLocation = mLocationServices.getLastLocation();
    }

    /**
     * All you need to initiate the map view and handle callbacks
     * @param savedInstanceState
     */
    private void initMapView(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);

        // Add a MapboxMap
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mapboxMap.setStyleUrl(Style.LIGHT);
                mMap = mapboxMap;

                toggleGps(!mMap.isMyLocationEnabled());

                if(latitude != 0.0 && longitude != 0.0) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), ZOOM));
                    setAddress(latitude, longitude, false);
                } else if (mLastLocation != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation), ZOOM));
                    setAddress(mLastLocation.getLatitude(), mLastLocation.getLongitude(), false);
                }

                final Projection projection = mapboxMap.getProjection();
                final int width  = mapView.getMeasuredWidth();
                final int height = mapView.getMeasuredHeight();


                mMap.setOnCameraChangeListener(new MapboxMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition position) {
                        PointF centerPoint  = new PointF(width / 2, (height + MdropPinView.getHeight()) / 2);
                        LatLng centerLatLng = new LatLng(projection.fromScreenLocation(centerPoint));

                        if (mEvent == android.view.MotionEvent.ACTION_UP && isClicked) {
                            Log.d("centerLatLng", centerLatLng.toString());
                            isClicked = false;
                            setAddress(centerLatLng.getLatitude(), centerLatLng.getLongitude(), true);
                        }
                    }
                });

                mapView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        mEvent = event.getAction();
                        if(event.getAction() == MotionEvent.ACTION_DOWN) isClicked = true;
                        return false;
                    }
                });
            }
        });
    }

    /**
     * Get address from lat and long and update the autocomplete text
     * FIXME: two separate things here.
     * @param latitude The latitude to retrieve the address
     * @param longitude The longitude to retrieve the address
     */
    private void setAddress(double latitude, double longitude, boolean shouldRegister) {
        List<Address> addresses = null;

        Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    // In this sample, get just a single address.
                    1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(addresses != null && addresses.size() > 0) {
            Log.d("ADDRESS", addresses.get(0).toString());
            autocomplete.setText(addresses.get(0).getAddressLine(0)
                    + ", " + addresses.get(0).getAddressLine(1) + ", "
                    + addresses.get(0).getAddressLine(2));
            if(shouldRegister)
                registerPlace(addresses.get(0).getAddressLine(0),
                    addresses.get(0).getAddressLine(1),
                    addresses.get(0).getAddressLine(2),
                    latitude,
                    longitude);
        }
    }

    /**
     * This marker will be pinned in the middle of the map
     */
    private void initCenterMarker () {
        MdropPinView = new ImageView(getActivity());
        MdropPinView.setImageResource(R.drawable.default_marker);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        MdropPinView.setLayoutParams(params);
        mapView.addView(MdropPinView);
    }

    /**
     * Geocoding Editext - top of the screen
     * TYPE_ADDRESS here -- can be changed
     */
    private void initGeocode() {
        autocomplete.setAccessToken(MapboxAccountManager.getInstance().getAccessToken());
        autocomplete.setType(GeocodingCriteria.TYPE_ADDRESS);
        autocomplete.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void OnFeatureClick(CarmenFeature feature) {
                Position position = feature.asPosition();
                updateMap(position.getLatitude(), position.getLongitude());
            }
        });
    }

    /**
     * Update map to the dedicate location and add the marker
     * @param latitude Latitude of the position
     * @param longitude Longitude of the position
     */
    private void updateMap(double latitude, double longitude) {
        // Animate camera to geocoder result location
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(ZOOM)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
    }


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    /**
     * Check for user permissions - otherwise it will crash
     * @param enableGps is the gps location enabled
     */
    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            if (!mLocationServices.areLocationPermissionsGranted()) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                enableLocation(true);
            }
        } else {
            enableLocation(false);
        }
    }

    /**
     *
     * @param enabled
     */
    private void enableLocation(boolean enabled) {
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = mLocationServices.getLastLocation();
            if (lastLocation != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), ZOOM));
            }

            mLocationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the mMap camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), ZOOM));
                        mLocationServices.removeLocationListener(this);
                    }
                }
            });
        }

        // Enable or disable the location layer on the mMap -- No need here. We just want the pin
        //mMap.setMyLocationEnabled(enabled);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation(true);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.my_places:
                mListener.onMenuItemClick(R.id.fragment_container, PlacesFragment.newInstance());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnMenuInteractionListener)
            mListener = (OnMenuInteractionListener) context;
        else
            throw new RuntimeException(context.toString()
                    + " must implement OnMenuInteractionListener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Handle click on menu item
     */
    interface OnMenuInteractionListener {
        void onMenuItemClick(int id, Fragment frag) ;
    }

}
