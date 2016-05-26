package com.example.amosang.foodlocator_v2;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by amosang on 29/10/15.
 */
public class JSONDirectionsParser {

    public List<List<HashMap<String,String>>> parse(JSONObject object) {
        List<List<HashMap<String,String>>>routes = new ArrayList<List<HashMap<String,String>>>();
        JSONArray route = null;
        JSONArray legs = null;
        JSONArray steps = null;

        try {
            route = object.getJSONArray("routes");
            for(int i=0;i<route.length();i++){
                legs = ((JSONObject)route.get(i)).getJSONArray("legs");
                List path = new ArrayList<HashMap<String,String>>();

                for(int j=0;j<legs.length();j++){
                    steps = ((JSONObject)legs.get(j)).getJSONArray("steps");

                    for(int k=0;k<steps.length();k++){
                        String polyline = "";
                        polyline = (String)((JSONObject)((JSONObject) steps.get(k)).get("polyline")).get("points");
                        List<LatLng>list = decodePoly(polyline);

                        for(int l=0;l<list.size();l++){
                            HashMap<String,String> draw = new HashMap<String,String>();
                            draw.put("lat",Double.toString(((LatLng)list.get(l)).latitude));
                            draw.put("lng",Double.toString(((LatLng)list.get(l)).longitude));
                            path.add(draw);
                        }
                    }
                    routes.add(path);
                }
            }
        } catch (Exception e) {
            Log.d("EXCEPTION", e.toString());
        }
        return routes;
    }

    /**
     * Method to decode polyline points
     * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     * */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

}
