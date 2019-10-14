package com.company;

import java.util.ArrayList;
import java.util.List;

public class Runner {

    private String fname;
    private String lname;
    private String school;
    private double time;
    private String stringTime;
    private boolean isInDB;
    private double existingHighestRating;
    private double agg_rating;

    public Runner(String fname, String lname, String school, double time, String stringTime, boolean isInDB, double existingHighestRating, double agg_rating) {
        this.fname = fname;
        this.lname = lname;
        this.school = school;
        this.time = time;
        this.stringTime = stringTime;
        this.isInDB = isInDB;
        this.existingHighestRating = existingHighestRating;
        this.agg_rating = agg_rating;
    }

    public String toString() {
        return fname + " " + lname + " " + school + " " + stringTime;
    }

    public String getFname() {
        return fname;
    }

    public String getLname() {
        return lname;
    }

    public String getSchool() {
        return school;
    }

    public double getTime() {
        return time;
    }

    public String getStringTime() {
        return stringTime;
    }

    public boolean isInDB() {
        return isInDB;
    }

    public double getExistingHighestRating() {
        return existingHighestRating;
    }

    public double getAgg_rating() {
        return agg_rating;
    }
}
