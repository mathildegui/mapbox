package com.mathildeguillossou.chauffeurprive.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
import com.mapbox.services.android.geocoder.ui.GeocoderAutoCompleteView;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.geocoding.v5.models.CarmenFeature;
import com.mathildeguillossou.chauffeurprive.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {

    private static final float ZOOM = 16;
    private static final int PERMISSIONS_LOCATION = 0;

    private MapboxMap map;
    private Location lastLocation;
    private LocationServices locationServices;
    private OnMenuInteractionListener mListener;

    @BindView(R.id.mapview) MapView mapView;
    @BindView(R.id.query) GeocoderAutoCompleteView autocomplete;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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


        MapboxAccountManager.start(getActivity(), getString(R.string.access_token));

        initLocation();
        initMapView(savedInstanceState);
        initGeocode();
    }

    private void initLocation() {
        locationServices = LocationServices.getLocationServices(getActivity());
        lastLocation = locationServices.getLastLocation();
    }

    private void initMapView(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);

        // Add a MapboxMap
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mapboxMap.setStyleUrl(Style.DARK);
                map = mapboxMap;

                toggleGps(!map.isMyLocationEnabled());

                if (lastLocation != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), ZOOM));

                    //market for the user position
                    MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                            .position(new LatLng(lastLocation));
                    map.addMarker(markerViewOptions);
                }

                map.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng point) {
                        //FIXME STEP4 - store it in DB
                        MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                                .position(point);
                        map.addMarker(markerViewOptions);
                        map.invalidate();
                    }
                });
            }
        });
    }

    private void initGeocode() {
        autocomplete.setAccessToken(MapboxAccountManager.getInstance().getAccessToken());
        autocomplete.setType(GeocodingCriteria.TYPE_ADDRESS);
        autocomplete.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void OnFeatureClick(CarmenFeature feature) {
                Position position = feature.asPosition();
                updateMap(position.getLatitude(), position.getLongitude());
                //add pin when the user select new address
                LatLng ll = new LatLng(position.getLatitude(), position.getLongitude());
                MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                        .position(ll);
                map.addMarker(markerViewOptions);
                //FIXME STEP4 - store it in DB
            }
        });
    }

    private void updateMap(double latitude, double longitude) {
        // Build marker
        map.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title("Geocoder result"));

        // Animate camera to geocoder result location
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
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

    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            if (!locationServices.areLocationPermissionsGranted()) {
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

    private void enableLocation(boolean enabled) {
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationServices.getLastLocation();
            if (lastLocation != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), ZOOM));
            }

            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), ZOOM));
                        locationServices.removeLocationListener(this);
                    }
                }
            });
        }

        // Enable or disable the location layer on the map -- No need here. We just want the pin
        //map.setMyLocationEnabled(enabled);
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
