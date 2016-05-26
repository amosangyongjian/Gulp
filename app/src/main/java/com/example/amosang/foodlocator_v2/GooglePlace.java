package com.example.amosang.foodlocator_v2;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import org.joda.time.LocalDate;

/**
 * Created by amosang on 24/10/15.
 */
public class GooglePlace implements Parcelable{
    private String name;
    private String rating;
    private double location;
    private LatLng rest_location;
    private String ld;

    public GooglePlace(String name, String rating,double location,LatLng rest_location,String ld) {
        this.name = name;
        this.rating = rating;
        this.location = location;
        this.rest_location = rest_location;
        this.ld = ld;
    }


    public String getLd() {
        return ld;
    }

    public void setLd(String ld) {
        this.ld = ld;
    }


    public LatLng getRest_location() {
        return rest_location;
    }

    public void setRest_location(LatLng rest_location) {
        this.rest_location = rest_location;
    }


    public double getLocation() {
        return location;
    }

    public void setLocation(double location) {
        this.location = location;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.rating);
        dest.writeDouble(this.location);
        dest.writeParcelable(this.rest_location, 0);
        dest.writeString(this.ld);
    }

    protected GooglePlace(Parcel in) {
        this.name = in.readString();
        this.rating = in.readString();
        this.location = in.readDouble();
        this.rest_location = in.readParcelable(LatLng.class.getClassLoader());
        this.ld = in.readString();
    }

    public static final Creator<GooglePlace> CREATOR = new Creator<GooglePlace>() {
        public GooglePlace createFromParcel(Parcel source) {
            return new GooglePlace(source);
        }

        public GooglePlace[] newArray(int size) {
            return new GooglePlace[size];
        }
    };
}
