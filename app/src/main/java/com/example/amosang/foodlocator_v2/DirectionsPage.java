package com.example.amosang.foodlocator_v2;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DirectionsPage extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleMap.OnMyLocationButtonClickListener,LocationListener,GoogleApiClient.OnConnectionFailedListener  {


    private TextView restaurantName;
    private TextView restaurantDistance;
    private Double latitude;
    private Double longitude;
    private String connected = null;
    private int delay =15000;
    private int init_delay = 5000;
    private Handler h;
    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private Marker placeMarker = null;
    private LatLng point;
    private LatLng rest_loc;
    private ImageButton favourites;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions_page);
        h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkConnection();
                h.postDelayed(this, delay);
                //Log.d("DELAY",String.valueOf(connected));
            }
        }, init_delay);
        map =  ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
        map.setOnMyLocationButtonClickListener(this);
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setContentInsetsAbsolute(0,0);
        favourites = (ImageButton)findViewById(R.id.tb_favourites);
        favourites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent savedPage = new Intent(getApplicationContext(), SavedPage.class);
                startActivity(savedPage);
            }
        });
        initializeUI();

    }



    private void initializeUI(){
        Bundle result = getIntent().getExtras();
        Double rest_locx = result.getDouble("restaurantLocationx");
        Double rest_locy = result.getDouble("restaurantLocationy");
        String rest_name = result.getString("restaurantName");
        //Double rest_distance = result.getDouble("restaurantDistance");
        rest_loc = new LatLng(rest_locx,rest_locy);


        restaurantName = (TextView)findViewById(R.id.restName);
        restaurantDistance = (TextView)findViewById(R.id.restDistance);
        //origin.setText(latitude.toString() + "," + longitude.toString());
        restaurantName.setText(rest_name);
        //restaurantDistance.setText(rest_distance+" km from you");
        point = new LatLng(rest_locx,rest_locy);
        if(placeMarker==null){
            placeMarker = map.addMarker(new MarkerOptions()
                    .position(point));
//                    .draggable(true));
        }else{
            placeMarker.setPosition(point);
        }


    }
    private void checkConnection(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            connected = "true";

        }
        else{
            connected = "false";

        }
        if(connected.equals("false")){
            Intent begin = new Intent(getApplicationContext(),StartupPage.class);
            startActivity(begin);
        }
    }

    public static double distanceBetween(LatLng p1,LatLng p2){
        //Log.d("DISTANCE",String.valueOf(SphericalUtil.computeDistanceBetween(p1, p2)));
        return SphericalUtil.computeDistanceBetween(p1, p2);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }


    @Override
    public void onConnected(Bundle bundle) {
        mGoogleApiClient.connect();
        String msg = ""+LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        final float zoomLevel = 14;
        msg = msg.replace("Location[fused", "");
        String[] msg_split = msg.split("acc");
        String[] split_res = msg_split[0].split(",");
        latitude = Double.parseDouble(split_res[0]);
        longitude = Double.parseDouble(split_res[1]);
        LatLng l = new LatLng(latitude,longitude);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(l, zoomLevel));
        Double rest_distance = round(distanceBetween(l,rest_loc)/1000,3);
        restaurantDistance.setText(rest_distance+" km from you");
        String url = constructURL(l,point);
        Log.d("URLOUTPUT",url);
        getDirectionsJSON getDirections = new getDirectionsJSON();
        getDirections.execute(url);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }

    private String constructURL(LatLng origin, LatLng destination){
        String u_origin = "origin="+origin.latitude+","+origin.longitude;
        String u_destination = "destination="+destination.latitude+","+destination.longitude;
        String sensor = "sensor=false";
        String mode = "mode=walking";
        String output = "json";
        String params = u_origin+"&"+u_destination+"&"+sensor+"&"+mode;
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+params;
        return url;
    }


    //Fetch data from URL
    private class getDirectionsJSON extends AsyncTask<String, Void, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Parser aTask = new Parser();
            aTask.execute(s);
        }

        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try{
                data = downloadUrl(url[0]);
            }catch (Exception e){
                Log.d("EXCEPTION",e.toString());
            }
            return data;
        }
    }

    //parse JSON data
    private class Parser extends AsyncTask<String, Integer, List<List<HashMap<String,String>>>>{
        private final ProgressDialog dialog = new ProgressDialog(DirectionsPage.this);
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage("Drawing path");
            dialog.show();
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... params) {
            JSONObject anObject;
            List<List<HashMap<String,String>>>routes = null;
            try{
                anObject = new JSONObject(params[0]);
                JSONDirectionsParser parser = new JSONDirectionsParser();

                routes = parser.parse(anObject);
            }catch (Exception e){
                Log.d("EXCEPTION",e.toString());
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            ArrayList<LatLng>points = null;
            PolylineOptions polylineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            for(int i=0;i<lists.size();i++){
                points = new ArrayList<>();
                polylineOptions = new PolylineOptions();
                //fetching i-th route
                List<HashMap<String,String>>path = lists.get(i);

                //fetching all points in the route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String>point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat,lng);
                    points.add(position);
                }
                polylineOptions.addAll(points);
                polylineOptions.width(10);
                polylineOptions.color(Color.parseColor("#28dd8f"));
            }
            map.addPolyline(polylineOptions);
            dialog.dismiss();
        }
    }



    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("EXCEPTION", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }




    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_directions_page, menu);
        MenuItem item = menu.findItem(R.id.action_settings);
        item.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
