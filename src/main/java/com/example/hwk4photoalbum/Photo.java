package com.example.hwk4photoalbum;

import android.graphics.Bitmap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class Photo {
    private String ID;
    private Bitmap picture;
    private String name;
    private String date;
    private String caption;
    private String location;
    private String details;

    public Bitmap getPicture() { return picture; }
    public void setPicture(Bitmap picture) { this.picture = picture; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getID() { return ID; }
    public void setID(String ID) { this.ID = ID; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Photo(String ID, Bitmap picture, String name, String date, String l, String c, String d) {
        this.ID = ID;
        this.picture = picture;
        this.name = name;
        this.date = date;

        // got lazy with naming
        location = l;
        caption = c;
        details = d;
    }

    public Photo() {
        ID = "-1";
        this.picture = null;
        this.name = "uhhhhh";
        date = "May 01,2022";
        location = "Unknown";
        caption = "Picture";
        details = "empty";
    }

    public static Comparator<Photo> photoComparatorAZ = new Comparator<Photo>() {
        @Override
        public int compare(Photo p1, Photo p2) {
            return p1.getName().compareToIgnoreCase(p2.getName());
        }
    };

    public static Comparator<Photo> photoDateComparatorNO = new Comparator<Photo>() {
        @Override
        public int compare(Photo p1, Photo p2) {
            SimpleDateFormat simpleDateFormat;
            String d1 = p1.getDate();
            String d2 = p2.getDate();
            simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
            try {
                Date date1 = simpleDateFormat.parse(d1);
                Date date2 = simpleDateFormat.parse(d2);
                return date1.compareTo(date2);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    };
}
