package com.example.amosang.foodlocator_v2;


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
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.example.amosang.library.*;
import org.joda.time.LocalDate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ResultPage extends AppCompatActivity {




    private Toolbar toolbar;
    private ImageButton favourites;
    private SwipeMenuListView menuListView;
    private ArrayList<GooglePlace> placeList = new ArrayList<>();
    private ArrayList<GooglePlace> resultList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        final TextView instructions = (TextView)findViewById(R.id.tv_instructions);
        instructions.setText("Swipe Left to access menu options");
        instructions.setBackgroundColor(Color.parseColor("#28dd8f"));
        instructions.setGravity(Gravity.CENTER);
        Handler instructionHandler = new Handler();
        instructionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                instructions.setVisibility(View.GONE);
            }
        },5000);

        initializeUI();
    }

    private void initializeUI(){

        //ListView lv = (ListView)findViewById(R.id.resultLV);
        menuListView = (SwipeMenuListView)findViewById(R.id.resultLV);
        ResultAdapter rs;
        rs = new ResultAdapter(getApplicationContext(),R.layout.listrow);
        //lv.setAdapter(rs);
        menuListView.setAdapter(rs);

        Intent mainClass = getIntent();
        placeList = mainClass.getParcelableArrayListExtra("results");
        Collections.sort(placeList, new Comparator<GooglePlace>() {
            @Override
            public int compare(GooglePlace lhs, GooglePlace rhs) {
                return Double.compare(lhs.getLocation(), rhs.getLocation());
            }
        });

        for(int i=0;i<placeList.size();i++) {
            String restName = placeList.get(i).getName();
            String restRating = placeList.get(i).getRating();
            Double restDistance = placeList.get(i).getLocation();
            LatLng restLocation = placeList.get(i).getRest_location();
            String ld = placeList.get(i).getLd();

            if(restDistance < 3){
                GooglePlace gp = new GooglePlace(restName, restRating,restDistance,restLocation,ld);
                //resultList.add(new GooglePlace(restName, restRating,restDistance,restLocation,ld));
                rs.add(gp);

            }
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
               favouriteItem.setBackground(new ColorDrawable(Color.parseColor("#FCE835")));
               favouriteItem.setWidth(dp2px(90));
               favouriteItem.setTitle("Like");
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
                        favourite(position);
                        break;
                }
                return false;
            }
        });





        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setContentInsetsAbsolute(0, 0);
        favourites = (ImageButton)findViewById(R.id.tb_favourites);
        favourites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent savedPage = new Intent(getApplicationContext(), SavedPage.class);
                startActivity(savedPage);
            }
        });


    }


    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private void open(int position){
        String restName = placeList.get(position).getName();
        String restRating = placeList.get(position).getRating();
        LatLng restLocation = placeList.get(position).getRest_location();
        Double restDistance = placeList.get(position).getLocation();
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

    private void favourite(int position){
        String restName = placeList.get(position).getName();
        String restRating = placeList.get(position).getRating();
        LatLng restLocation = placeList.get(position).getRest_location();
        LocalDate ld = LocalDate.now();
        //distance is always recalculated
        String result = restName + "|" + restRating + "|" + ld + "|" + restLocation + "\n";
        Toast.makeText(getApplicationContext(), restName + " added to Favourites", Toast.LENGTH_SHORT).show();
        try {
            FileOutputStream fOut = openFileOutput("cust_history", MODE_APPEND);
            fOut.write(result.getBytes());
        } catch (IOException e) {
            Log.d("IOEXCEPTION", e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_result_page, menu);
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
