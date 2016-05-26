package com.example.amosang.foodlocator_v2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by amosang on 24/10/15.
 */
public class SavedAdapter extends ArrayAdapter {
    ArrayList<GooglePlace> list = new ArrayList<>();

    public SavedAdapter(Context context, int resource) {
        super(context, resource);
    }
    static class DataHandler{
        ImageView restIcon;
        TextView restName;
        TextView restRating;
        //TextView restDist;
        TextView lastVisit;
    }

    public void add(GooglePlace place) {
        super.add(place);
        list.add(place);
    }

    public GooglePlace getPlace(int position){
        return this.list.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        DataHandler dh;
        if(convertView==null){
            LayoutInflater inflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.resultlistrow,parent,false);
            dh=new DataHandler();
            //dh.restIcon = (ImageView)row.findViewById(R.id.iconID);
            dh.restName = (TextView)row.findViewById(R.id.restName);
            dh.restRating = (TextView)row.findViewById(R.id.restRating);
           // dh.restDist = (TextView)row.findViewById(R.id.restDistance);
            dh.lastVisit = (TextView)row.findViewById(R.id.lastVisit);
            row.setTag(dh);
        }else{
            dh=(DataHandler)row.getTag();
        }
        GooglePlace gp = (GooglePlace)this.getItem(position);
        //dh.restIcon.setImageResource(gp.getRestIcon());
        dh.restName.setText(gp.getName());
        dh.restRating.setText("Rating : " + gp.getRating()+"/5");
//        dh.restDist.setText("Distance from you : " + gp.getLocation()+"km");
        dh.lastVisit.setText(String.valueOf(gp.getLd()));
        return row;
    }
}
