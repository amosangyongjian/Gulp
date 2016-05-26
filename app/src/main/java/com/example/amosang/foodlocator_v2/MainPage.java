package com.example.amosang.foodlocator_v2;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class MainPage extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleMap.OnMyLocationButtonClickListener,LocationListener,GoogleApiClient.OnConnectionFailedListener  {

    private ArrayList<String> cuisine = new ArrayList<>();
    private ArrayAdapter<String> staticAdapter;
    private ArrayList<GooglePlace> resultList = new ArrayList<>();
    private Spinner optionList;
    private String chosen;
    private GoogleMap map;
    private double latitude;
    private double longitude;
    private String city;
    private String connected = null;
    private int delay =15000;
    private int init_delay = 5000;
    private Handler h;
    private GoogleApiClient mGoogleApiClient;
    private Marker placeMarker = null;
    private Toolbar toolbar;
    private ImageButton favourites;
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    private static final String KEY = "AIzaSyDu_gecHHYRGH6BizworVZXRBVTy-_QP4Y";


    public String readJSONFeed(String URL) {
        StringBuilder stringBuilder = new StringBuilder();
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(URL);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();
            } else {
                Log.d("JSON", "Failed to download file");
            }
        } catch (Exception e) {
            Log.d("readJSONFeed", e.getLocalizedMessage());
        }
        return stringBuilder.toString();
    }
    private class GetPlacesJSONFeed extends AsyncTask<String,Void,String> {
        private final ProgressDialog dialog = new ProgressDialog(MainPage.this);
        @Override
        protected String doInBackground(String... url) {
            return readJSONFeed(url[0]);
        }
        @Override
        protected void onPostExecute(String result) {
            try {
                resultList.clear();


                dialog.dismiss();

                String curr_rating;
                JSONObject jsonObject = new JSONObject(result);
                Log.d("INRESULT",result.toString());
                if(jsonObject.has("results")){
                    JSONArray jsonArray = jsonObject.getJSONArray("results");
                    for(int i=0;i<jsonArray.length();i++){
                        Log.d("OUTPUTALL",jsonArray.getJSONObject(i).toString());
                        if (jsonArray.getJSONObject(i).has("name")){
                            String curr_name = jsonArray.getJSONObject(i).optString("name");
                            String rest_rating = jsonArray.getJSONObject(i).optString("rating");
                            if(rest_rating.equals("")){
                               curr_rating = "No Rating Available";
                            }else{
                                curr_rating = rest_rating;
                            }
                            Double curr_lat = jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").optDouble("lat");
                            Double curr_long = jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").optDouble("lng");
                           // String rest_coords = curr_lat+","+curr_long+","+latitude+","+longitude;
                            LatLng p1 = new LatLng(latitude,longitude);
                            LatLng p2 = new LatLng(curr_lat,curr_long);
                            Double distance = round(distanceBetween(p1,p2)/1000,3);
                            LocalDate ld= LocalDate.now();
                            String ld_conv = ld.toString();
                            resultList.add(new GooglePlace(curr_name,curr_rating,distance,p2,ld_conv));
                               // resultList.add(new GooglePlace(jsonArray.getJSONObject(i).optString("name"),jsonArray.getJSONObject(i).optString("rating")));
                                //Log.d("JSONRESULT", jsonArray.getJSONObject(i).optString("name") + " " + jsonArray.getJSONObject(i).optString("rating") + " " + jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").optDouble("lat"));


                        }
                    }
                }
                showResults(resultList);
            } catch (Exception e) {
                Log.d("EXCEPTION", e.getLocalizedMessage());
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog.setMessage("Retrieving results");
            dialog.show();
        }
    }

    public static double distanceBetween(LatLng p1,LatLng p2){
        //Log.d("DISTANCE",String.valueOf(SphericalUtil.computeDistanceBetween(p1, p2)));
        return SphericalUtil.computeDistanceBetween(p1,p2);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

//        File dir = getFilesDir();
//        File file = new File(dir, "cust_history");
//        boolean deleted = file.delete();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);
        h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkConnection();
                h.postDelayed(this, delay);
                //Log.d("DELAY",String.valueOf(connected));
            }
        }, init_delay);
        initializeUI();
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


        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                customLoc(latLng);

            }
        });
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                customLoc(latLng);
                //geoCoder(latitude, longitude);
                placeLocator(latitude, longitude, chosen);


            }
        });
        favourites = (ImageButton)findViewById(R.id.tb_favourites);
        favourites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent savedPage = new Intent(getApplicationContext(), SavedPage.class);
                startActivity(savedPage);
            }
        });

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setContentInsetsAbsolute(0, 0);
        final TextView instructions = (TextView)findViewById(R.id.tv_instructions);
        instructions.setBackgroundColor(Color.parseColor("#28dd8f"));
        instructions.setText("Select cuisine and hold down at a point to start search");
        instructions.setGravity(Gravity.CENTER);
        Handler instructionHandler = new Handler();
        instructionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                instructions.setVisibility(View.GONE);
            }
        },5000);
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

    private void initializeUI(){
        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(getAssets().open("cuisine.txt")));
            String in = null;
            while ((in = bReader.readLine()) != null) {
                String[] split = in.split(" ");
                Collections.sort(cuisine);
                cuisine.add(split[0]);
            }
            bReader.close();
        } catch (IOException e) {
            Log.d("Exception e",e.toString());
        }

        optionList = (Spinner) findViewById(R.id.cuisineSpinner);
        staticAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, cuisine);
        staticAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        optionList.setAdapter(staticAdapter);
        //optionList.setBackgroundColor(Color.parseColor("#28dd8f"));
        optionList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                chosen = optionList.getSelectedItem().toString();
                Log.d("CHOSEN", chosen);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }



    private void placeLocator(double x,double y,String chosen){

//        String uri = "https://maps.googleapis.com/maps/api/place/search/json?location="+
//                x+","+y
//                +"&radius=200"
//                +"&type=restaurant"
//                +"&sensor=true&key="+KEY;
                String uri = "https://maps.googleapis.com/maps/api/place/textsearch/json?location="+
                x+","+y
                +"&radius=50"
                +"&query="+chosen+"restaurant" +
                        ""
                +"&sensor=true&key="+KEY;
        new GetPlacesJSONFeed().execute(uri);

    }

    private void showResults(ArrayList<GooglePlace>gp){
        Intent resultPage = new Intent(getApplicationContext(),ResultPage.class);
        resultPage.putParcelableArrayListExtra("results", gp);
        startActivity(resultPage);
    }

//    private void geoCoder(double x,double y) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        StringBuilder builder = new StringBuilder();
//        try {
//            List<Address> address = geocoder.getFromLocation(x, y, 1);
//            int maxLines = address.get(0).getMaxAddressLineIndex();
//            for (int i = 0; i < maxLines; i++) {
//                String addressStr = address.get(0).getAddressLine(i);
//                builder.append(addressStr);
//                builder.append(" ");
//            }
//            String un_city = address.get(0).getAddressLine(1);
//            String[] proc_city = un_city.split(" ");
//            String curr_city = address.get(0).getLocality();
//            String country_name = address.get(0).getCountryName();
//            city = curr_city;
//            //showResults(x,y,city);
//            Log.d("CITY",address.get(0).getLocality());
//            //String final_city = country_name+"/"+curr_city;
//            //String finaladdress = builder.toString();
//        }
//        catch(Exception e){
//            Log.d("Exception",e.toString());
//        }
//    }



    @Override
    public void onBackPressed()
    {

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


    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public boolean onMyLocationButtonClick() {
        if (mGoogleApiClient.isConnected()) {
            String msg = ""+LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            msg = msg.replace("Location[fused","");
            String[] msg_split = msg.split("acc");
            String msg_res = msg_split[0];
            String[] split_res = msg_split[0].split(",");
            latitude = Double.parseDouble(split_res[0]);
            longitude = Double.parseDouble(split_res[1]);
            Log.d("XANDY", "x : " + latitude + " y :" + longitude);
        }
        return false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    private void customLoc(LatLng point){
        String conv_point = point.toString();
        conv_point = conv_point.replace("lat/lng:","");
        conv_point = conv_point.replace("(","");
        conv_point = conv_point.replace(")", "");
        String[] splitpoint = conv_point.split(",");
        latitude = Double.parseDouble(splitpoint[0]);
        longitude = Double.parseDouble(splitpoint[1]);
        if(placeMarker==null){
            placeMarker = map.addMarker(new MarkerOptions()
                    .position(point));
//                    .draggable(true));
        }else{
            placeMarker.setPosition(point);
        }



        //Log.d("COORDINATES", "x : " + splitpoint[0] + " y : " + splitpoint[1]);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mGoogleApiClient.connect();
        String msg = ""+LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        final float zoomLevel = 16;
        msg = msg.replace("Location[fused", "");
        String[] msg_split = msg.split("acc");
        String[] split_res = msg_split[0].split(",");
        latitude = Double.parseDouble(split_res[0]);
        longitude = Double.parseDouble(split_res[1]);
        final LatLng l = new LatLng(latitude,longitude);
        //Log.d("LOADED","x : " + latitude+ " y : " + longitude);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(l, zoomLevel));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_page, menu);
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
