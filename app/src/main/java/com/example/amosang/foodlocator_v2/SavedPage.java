package com.example.amosang.foodlocator_v2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amosang.library.SwipeMenu;
import com.example.amosang.library.SwipeMenuCreator;
import com.example.amosang.library.SwipeMenuItem;
import com.example.amosang.library.SwipeMenuListView;
import com.google.android.gms.maps.model.LatLng;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;


public class SavedPage extends AppCompatActivity {

    private ArrayList<GooglePlace>places = new ArrayList<>();
    private ArrayList<String>placetext = new ArrayList<>();
    private SavedAdapter sa;
    private Button clearAll;
    private Toolbar toolbar;
    private SwipeMenuListView menuListView;
    private ImageButton favourites;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_page);
//        toolbar = (Toolbar)findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayShowTitleEnabled(false);
//        toolbar.setContentInsetsAbsolute(0, 0);

        final TextView instructions = (TextView)findViewById(R.id.tv_instructions);
        instructions.setText("Swipe Left to access menu options");
        instructions.setGravity(Gravity.CENTER);
        instructions.setBackgroundColor(Color.parseColor("#28dd8f"));
        Handler instructionHandler = new Handler();
        instructionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                instructions.setVisibility(View.GONE);
            }
        }, 5000);

        initializeUI();
    }

    private void initializeUI(){

        //ListView lv = (ListView)findViewById(R.id.resultLV);
        menuListView = (SwipeMenuListView)findViewById(R.id.resultLV);
        clearAll = (Button)findViewById(R.id.btnClear);
        sa = new SavedAdapter(getApplicationContext(),R.layout.resultlistrow);
        //lv.setAdapter(sa);
        menuListView.setAdapter(sa);
        try{
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(openFileInput("cust_history")));
            String in=null;
            while((in=inputReader.readLine())!=null){
                String[] split = in.split(Pattern.quote("|"));
                String rest_name = split[0];
                String rest_rating = split[1];
                Double curr_dist = 0.0;

                split[3] = split[3].replace("lat/lng: (", "");
                split[3] = split[3].replace(")", "");
                String[] latlng = split[3].split(",");
                Double lat = Double.parseDouble(latlng[0]);
                Double lng = Double.parseDouble(latlng[1]);
                LatLng rest_location = new LatLng(lat,lng);


                String visited = "Last visited : " +split[2];

                places.add(new GooglePlace(rest_name, rest_rating, curr_dist,rest_location, visited));
                placetext.add(rest_name+rest_rating+curr_dist+rest_location+visited);
                //sa.add(place);
            }

        }catch (IOException e){
            Log.d("IOEXCEPTION",e.toString());
        }


        for(int i=0;i<places.size();i++){
            String rest_name = places.get(i).getName();
            String rest_rating = places.get(i).getRating();
            LatLng rest_location = places.get(i).getRest_location();
            Double curr_dist = 0.0;
            String visited = places.get(i).getLd();
            GooglePlace gp = new GooglePlace(rest_name,rest_rating,curr_dist,rest_location,visited);
            sa.add(gp);
        }

        SwipeMenuCreator swp = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem goItem = new SwipeMenuItem(getApplicationContext());
                goItem.setBackground(new ColorDrawable(Color.parseColor("#7feb91")));
                goItem.setWidth(dp2px(90));
                goItem.setTitle("Go");
                goItem.setTitleSize(18);
                goItem.setTitleColor(Color.WHITE);
                menu.addMenuItem(goItem);

                SwipeMenuItem favouriteItem = new SwipeMenuItem(getApplicationContext());
                favouriteItem.setBackground(new ColorDrawable(Color.parseColor("#f04e2d")));
                favouriteItem.setWidth(dp2px(90));
                favouriteItem.setTitle("Remove");
                favouriteItem.setTitleSize(18);
                favouriteItem.setTitleColor(Color.WHITE);
                //favouriteItem.setIcon(R.drawable.ic_action_important);
                menu.addMenuItem(favouriteItem);
            }
        };
        menuListView.setMenuCreator(swp);
        menuListView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index){
                    case 0:
                        open(position);
                        break;
                    case 1:
                        try{
                            removeLine(position);
                            sa.remove(sa.getItem(position));
                            sa.notifyDataSetChanged();
                        }catch(IOException e){
                            Log.d("IOEXCEPTION",e.toString());
                        }
                        break;
                }
                return false;
            }
        });

        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File dir = getFilesDir();
                File file = new File(dir, "cust_history");
                boolean deleted = file.delete();
                sa.clear();
                sa.notifyDataSetChanged();
            }
        });

    }

    private void open(int position){
        String restName = places.get(position).getName();
        String restRating = places.get(position).getRating();
        LatLng restLocation = places.get(position).getRest_location();
        Double restDistance = places.get(position).getLocation();
        Intent directions = new Intent();
        directions.setClass(getApplicationContext(),DirectionsPage.class);
        Bundle data = new Bundle();

        String restaurantLocation = restLocation.toString();
        restaurantLocation = restaurantLocation.replace("lat/lng: (","");
        restaurantLocation = restaurantLocation.replace(")","");
        String[] restSplit = restaurantLocation.split(",");
        Double restLat = Double.parseDouble(restSplit[0]);
        Double restLng = Double.parseDouble(restSplit[1]);
        data.putDouble("restaurantLocationx",restLat);
        data.putDouble("restaurantLocationy",restLng);
        data.putString("restaurantName",restName);
        data.putDouble("restaurantDistance",restDistance);
        directions.putExtras(data);
        startActivity(directions);
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    public void removeLine(int position) throws IOException{
        ArrayList<String>line = new ArrayList<>();
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(openFileInput("cust_history")));
        String in=null;
        line.clear();
        while((in=inputReader.readLine())!=null){
            line.add(in);
        }


        line.remove(position);
        Toast.makeText(getApplicationContext(),"Entry removed", Toast.LENGTH_SHORT).show();
        FileOutputStream fos = openFileOutput("cust_history", Context.MODE_PRIVATE);
        for(String aLine:line){
            try{
                String inLine = aLine+"\n";
                fos.write(inLine.getBytes());
            }catch (IOException e){
                Log.d("IOEXCEPTION",e.toString());
            }
        }
        fos.close();

    }



//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_saved_page, menu);
//        return true;
//    }

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
