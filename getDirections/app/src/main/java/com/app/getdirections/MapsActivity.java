package com.app.getdirections;

import androidx.annotation.NonNull;
import androidx.constraintlayout.solver.widgets.analyzer.Direct;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.media.MediaPlayer;

import android.os.Bundle;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import com.phidget22.Manager;
import com.phidget22.RFID;
import com.phidget22.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,LocationListener {
    private final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    private static final int LOCATION_REQUEST=500;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private CameraPosition cameraPosition;
    private Location lastKnownLocation;
    private static final int DEFAULT_ZOOM = 15;
    private static final int LOCATION_REFRESH_TIME=500;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final LatLng defaultLocation = new LatLng(51.6168167, -3.9440917);
    private boolean locationPermissionGranted;
    private LatLng mOrigin;
    private LatLng mDestination;
    private PlacesClient placesClient;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Compass compass;
    private float currentAzimuth;
    private SOTWFormatter sotwFormatter;
    private float curDirection;


    ArrayList<LatLng> listPoints;
    Geocoder geocoder;
    String bestProvider;
    List<Address> user = null;
    double lat = 0;
    double lng = 0;

    RFID rfid;
    Manager manager;
    int minDataInterval;

    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        setContentView(R.layout.activity_maps);

        sotwFormatter = new SOTWFormatter(this);
        setupCompass();


        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        @SuppressLint("MissingPermission") Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
        locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    // Set the map's camera position to the current location of the device.
                    lastKnownLocation = task.getResult();
                    if (lastKnownLocation != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(lastKnownLocation.getLatitude(),
                                        lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        MarkerOptions markerOptions=new MarkerOptions();
                        markerOptions.position(new LatLng(lastKnownLocation.getLatitude(),
                                lastKnownLocation.getLongitude()));
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        mMap.addMarker(markerOptions);
                    }
                } else {
                    Log.d(TAG, "Current location is null. Using defaults.");
                    Log.e(TAG, "Exception: %s", task.getException());
                    mMap.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                }



            }
        });
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        getLocationPermission();

        listPoints=new ArrayList<>();

        try{
            manager = new Manager();

            //Allow direct USB connection of Phidgets
            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
                com.phidget22.usb.Manager.Initialize(this);

            //Enable server discovery to list remote Phidgets
           // this.getSystemService(Context.NSD_SERVICE);
           // Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

            //CSCM79 Advice
            //Add a specific network server to communicate with Phidgets remotely
            Net.addServer("Asus", "172.16.187.82", 5661, "", 0);

            rfid = new RFID();

            //Enable server discovery to list remote Phidgets
            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);


            rfid.addAttachListener(new AttachListener() {
                public void onAttach(final AttachEvent attachEvent) {
                    AttachEventHandler handler = new AttachEventHandler(rfid);
                    runOnUiThread(handler);
                }
            });

            rfid.addDetachListener(new DetachListener() {
                public void onDetach(final DetachEvent detachEvent) {
                    DetachEventHandler handler = new DetachEventHandler(rfid);
                    runOnUiThread(handler);
                }
            });


            rfid.addTagListener(new RFIDTagListener() {
                public void onTag(RFIDTagEvent tagEvent) {
                    RFIDTagEventHandler handler = new RFIDTagEventHandler(rfid, tagEvent);
                    runOnUiThread(handler);
                }
            });

            rfid.addTagLostListener(new RFIDTagLostListener() {
                public void onTagLost(RFIDTagLostEvent tagLostEvent) {
                    RFIDTagLostEventHandler handler = new RFIDTagLostEventHandler(rfid, tagLostEvent);
                    runOnUiThread(handler);
                }
            });

            rfid.open(5000);
            Log.i(TAG, "****** rfid.open ********");

        }
        catch(PhidgetException e)
        {
            e.printStackTrace();
        }

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
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "start compass");
        compass.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "stop compass");
        compass.stop();
    }

    private void setupCompass() {
        compass = new Compass(this);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    private float bearing(double lat1, double lon1, double lat2, double lon2){
        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff= Math.toRadians(longitude2-longitude1);
        double y= Math.sin(longDiff)*Math.cos(latitude2);
        double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

        return (float)(Math.toDegrees(Math.atan2(y, x))+360)%360;
    }

    private int getDirection(float bearing){
        final int[] direction = {90, 180, -1, 0};
        final String bearingChar = "NESW";
        int azimuthVal = bearingChar.indexOf(sotwFormatter.format(curDirection));
        int bearingVal = bearingChar.indexOf(sotwFormatter.format(bearing));
        int dirVal = azimuthVal - bearingVal;
        if (dirVal < 0) {
            dirVal = 4 - dirVal;
        }

        return direction[dirVal];
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
                // UI updates only in UI thread
                // https://stackoverflow.com/q/11140285/444966
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        curDirection = azimuth;
                    }
                });
            }
        };
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        //get permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST);
            return;
        }


        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng latLng) {
                Log.i(TAG, "***** onMapClick *******");

                if (listPoints.size() > 1) {
                    listPoints.clear();
                    mMap.clear();
                }
                //save fist point
                listPoints.add(latLng);

                Log.i(TAG, "***** listPoints.get(0) *******"+latLng);
                //add marker
                MarkerOptions markerOptions=new MarkerOptions();
                markerOptions.position(latLng);
                if (listPoints.size() == 1) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else if (listPoints.size() == 2) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
                mMap.addMarker(markerOptions);
                //get directions
                if (listPoints.size() >= 2) {
                    //create URL to get directions
                    LatLng origin = (LatLng) listPoints.get(0);
                    LatLng dest = (LatLng) listPoints.get(1);
                    String url=getRequestUrl(origin,dest);
                    Log.i(TAG, "***** getURL.get(0) *******"+url);
                    TaskRequestDirections taskRequestDirections=new TaskRequestDirections();
                    Log.i(TAG, "***** execute URL *******"+url);
                    taskRequestDirections.execute(url);
                }
            }
        });





    }

    public void getMyLocation(String tag){
        Log.i("*****", "********** getMyLocation *****"+tag);
        if (tag.equals("01058ed1ac")){
            Log.i("*****", "********** tag matched *****");
            mDestination=new LatLng(51.619271637567465, -3.878670739028323);
        }
        else if(tag.equals("010693444f")){
            mDestination=new LatLng(51.61062436297619, -3.980074330318057);
        }
        // Getting LocationManager object from System Service LOCATION_SERVICE

        Log.i("*****", "********** getMyLocation *****");
        Log.i("*****", "********** mDestination *****"+mDestination.toString());
        MarkerOptions markerOptions=new MarkerOptions();
        markerOptions.position(mDestination);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        MapsActivity activity = MapsActivity.this;
        activity.runOnUiThread(new Runnable(){
            public void run(){
                mMap.clear();
                mMap.addMarker(markerOptions);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        mDestination, DEFAULT_ZOOM));
            }
        });



        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        bestProvider = lm.getBestProvider(criteria, false);
        Location location = lm.getLastKnownLocation(bestProvider);

        if (location == null){
            Toast.makeText(activity,"Location Not found",Toast.LENGTH_LONG).show();
        }else{
            geocoder = new Geocoder(activity);
            try {
                user = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                lat=(double)user.get(0).getLatitude();
                lng=(double)user.get(0).getLongitude();
                Log.i("*****", "********** lat,lng  *****"+String.valueOf(lat)+" "+String.valueOf(lng));

            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        
                mOrigin = new LatLng(lat, lng);
                markerOptions.position(mOrigin);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                activity.runOnUiThread(new Runnable(){
                    public void run(){
                        mMap.addMarker(markerOptions);

                Log.i("*****", "********** mOrigin *****"+mOrigin.toString());
                String url = null;
                if(mOrigin != null && mDestination != null)
                    url=getRequestUrl(mOrigin,mDestination);
                TaskRequestDirections taskRequestDirections=new TaskRequestDirections();
                taskRequestDirections.execute(url);
                Log.i("*****", "********** url *****"+url.toString());
                    }
                });

        }


    

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            Log.i("*****", "Location changed");
            mOrigin = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mOrigin,12));
        }


        @SuppressLint("MissingPermission")
        protected void onCreate(Bundle savedInstanceState) {
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, 50, (android.location.LocationListener) mLocationListener);
        }
    };

    public void zoomRoute(List<LatLng> lstLatLngRoute){
        if(mMap == null || lstLatLngRoute== null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder bounds=new LatLngBounds.Builder();
        for(LatLng latLngPoints: lstLatLngRoute){
            bounds.include(latLngPoints);
        }

        int padding=120;
        LatLngBounds latLngBounds =bounds.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,padding),600,null);
    }

    private String getRequestUrl(LatLng origin, LatLng destination) {
        String orig="origin="+origin.latitude+","+origin.longitude;
        String dest="destination="+destination.latitude+","+destination.longitude;
        String sensor="sensor=false";
        String mode="mode=walking";
        String key="key=AIzaSyAgdU63dg7YkCWTluie6ao-Pr8UQOgVN9g";
        //String key="key="+getString(R.string.google_maps_key);
        String param= orig+"&"+dest+"&"+sensor+"&"+mode+"&"+key;
        String output="json";
        String url="https://maps.googleapis.com/maps/api/directions/"+output+"?"+param;
        return url;
    }

    private String requestDirections(String requestURL) throws IOException {
        Log.i("*****", "******* requestDirections *********");
        String responseString="";
        InputStream inputStream=null;
        HttpURLConnection httpURLConnection=null;
        try{

            URL url =new URL(requestURL);
            Log.i("*****", "******* CREATE url *********");
            httpURLConnection=(HttpURLConnection)url.openConnection();
            Log.i("*****", "******* OPEN CONNECTION *********");
            httpURLConnection.setReadTimeout(15000 /* milliseconds */);
            httpURLConnection.setConnectTimeout(15000 /* milliseconds */);
            httpURLConnection.setDoInput(true);
            httpURLConnection.connect();
            Log.i("*****", "******* CONNECT *********");
            // get response
            inputStream=httpURLConnection.getInputStream();
            Log.i("*****", "******* RECEIVE INPUT *********");
            InputStreamReader inputStreamReader=new InputStreamReader(inputStream);
            BufferedReader bufferedReader =new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer=new StringBuffer();
            String line="";
            while((line=bufferedReader.readLine())!=null){
                stringBuffer.append(line);
            }
            responseString=stringBuffer.toString();

            bufferedReader.close();
            inputStreamReader.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if(inputStream != null){
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case LOCATION_REQUEST:
            if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                mMap.setMyLocationEnabled(true);
            }
            break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mOrigin = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mOrigin,12));
    }

     class RFIDTagLostEventHandler implements Runnable {
        Phidget ch;
        RFIDTagLostEvent tagLostEvent;

        public RFIDTagLostEventHandler(Phidget ch, RFIDTagLostEvent tagLostEvent) {
            this.ch = ch;
            this.tagLostEvent = tagLostEvent;
            Log.i(TAG, "****** RFIDTagEventHandler ********"+tagLostEvent.getTag().toString());
        }

        public void run() {
            //   TextView tagTxt = (TextView)findViewById(R.id.tagValue);
            //   TextView protocolTxt = (TextView)findViewById(R.id.prototcolValue);

            //   tagTxt.setText("");
            //   protocolTxt.setText("");
        }
    }

    class RFIDTagEventHandler implements Runnable{
    Phidget ch;
    RFIDTagEvent tagEvent;

    public RFIDTagEventHandler(Phidget ch, RFIDTagEvent tagEvent) {
        this.ch = ch;
        this.tagEvent = tagEvent;

        Log.i(TAG, "****** RFIDTagEventHandler ********"+tagEvent.getTag().toString());
        getMyLocation(tagEvent.getTag().toString());
        //get directions
        //if (listPoints.size() >= 2) {
            //create URL to get directions
        //    LatLng origin = (LatLng) listPoints.get(0);
        //    LatLng dest = (LatLng) listPoints.get(1);
        //    String url=getRequestUrl(origin,dest);

        //    TaskRequestDirections taskRequestDirections=new TaskRequestDirections();
        //    taskRequestDirections.execute(url);

    }

    public void run() {


    }
    }


    class ErrorEventHandler {
        Phidget ch;
        ErrorEvent errorEvent;
        public ErrorEventHandler(Phidget ch, ErrorEvent errorEvent) {
            this.ch = ch;
            this.errorEvent = errorEvent;
        }
    }



public class TaskRequestDirections extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... strings) {
            String responseString="";
            try{
                responseString = requestDirections(strings[0]);
                Log.i(TAG, "********* responseString *********"+ responseString);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Parse response Json

            TaskParser taskParser=new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>>>{
        List<List<HashMap<String, String>>> routes=null;
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject=null;


            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
                Log.i(TAG, "********* routes *********"+routes.toString());
                Log.i(TAG, "********* TaskParser *********"+routes.toString());
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            // Get route list and display in map
            ArrayList points=null;
            PolylineOptions polylineOptions=null;

            for(List<HashMap<String, String>> path: lists){
                points=new ArrayList();
                polylineOptions=new PolylineOptions();

                for (HashMap<String, String> point: path){
                    double lat=Double.parseDouble(point.get("lat"));
                    double lon=Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat,lon));
                }
                Log.i(TAG, "********* TaskParser onPostExecute*********");
                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
                zoomRoute(polylineOptions.getPoints());
            }
            if (polylineOptions!=null) {
                mMap.addPolyline(polylineOptions);
                showDirections();
            } else {
                Toast.makeText(getApplicationContext(), "Direction not found!", Toast.LENGTH_SHORT).show();
            }
        }

        protected double bearing(double startLat, double startLng, double endLat, double endLng){
            double latitude1 = Math.toRadians(startLat);
            double latitude2 = Math.toRadians(endLat);
            double longDiff= Math.toRadians(endLng - startLng);
            double y= Math.sin(longDiff)*Math.cos(latitude2);
            double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

            return (Math.toDegrees(Math.atan2(y, x))+360)%360;
        }

        protected double distance(double startLat, double startLng, double endLat, double endLng){
            Location loc1 = new Location("");
            loc1.setLatitude(startLat);
            loc1.setLongitude(startLng);

            Location loc2 = new Location("");
            loc2.setLatitude(endLat);
            loc2.setLongitude(endLng);

            float distanceInMeters = loc1.distanceTo(loc2);
            return distanceInMeters;
        }


        private LatLng getCurrentLongLat(){
            MapsActivity activity = MapsActivity.this;
            LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            bestProvider = lm.getBestProvider(criteria, false);
            Location location = lm.getLastKnownLocation(bestProvider);

            if (location == null){
                Toast.makeText(activity,"Location Not found",Toast.LENGTH_LONG).show();
            }else{
                geocoder = new Geocoder(activity);
                try {
                    user = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    lat=(double)user.get(0).getLatitude();
                    lng=(double)user.get(0).getLongitude();
                    Log.i("*****", "********** lat,lng  *****"+String.valueOf(lat)+" "+String.valueOf(lng));

                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
            LatLng latLng = new LatLng(lat, lng);
            return latLng;
        }

        private void setmarker(){
            LatLng latLng=getCurrentLongLat();
            MarkerOptions markerOptions=new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            MapsActivity activity = MapsActivity.this;
            activity.runOnUiThread(new Runnable(){
                public void run(){
                    mMap.clear();
                    mMap.addMarker(markerOptions);

            }});
        }


        @SuppressLint("MissingPermission")
        protected void showDirections() {
            List<HashMap<String, String>> route;
            int dist=0;
            int bearing=0;
            if (routes != null) {
                route=routes.get(0);
                for (int i = 0; i < route.size()-1; i++) {
                    Double lat1=Double.parseDouble(route.get(i).get("lat"));
                    Double lon1=Double.parseDouble(route.get(i).get("lon"));
                    Double lat2=Double.parseDouble(route.get(i+1).get("lat"));
                    Double lon2=Double.parseDouble(route.get(i+1).get("lon"));
                    Log.i(TAG, "********* distnace between  *********"+lat1+","+lon1+" is :"+distance(lat1,lon1,lat2,lon2));
                    Log.i(TAG, "********* bearing between  *********"+lat1+","+lon1+" is :"+bearing(lat1,lon1,lat2,lon2));

                }
                Log.i(TAG, "********* showDirections *********");





            }
        }
    }
}