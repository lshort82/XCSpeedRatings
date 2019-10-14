package com.company;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import org.apache.commons.math3.linear.RealMatrix;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.sql.PreparedStatement;
import java.util.*;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.sql.Types.NULL;


/**
 * Analyzes times from a specific race
 */
public class CalculateRace {

    private Regression mapping;
    private String gender;
    private String raceUrl;
    private double raceLength;
    private List<Integer> tableInd;
    private int timeIndex;
    private int nameIndex;
    private int schoolIndex;
    private List<Double> ratings = new ArrayList<>();
    private List<Runner> runners = new ArrayList<>();

    public CalculateRace (String raceUrl, double raceLength, List<Integer> tableInd, int timeIndex, int nameIndex, int schoolIndex, String gender) {
        this.raceUrl = raceUrl;
        this.raceLength = raceLength;
        this.tableInd = tableInd;
        this.timeIndex = timeIndex;
        this.nameIndex = nameIndex;
        this.schoolIndex = schoolIndex;
        this.gender = gender;
        int n = 0;
        Document doc = null;
        try {
            doc = Jsoup.connect(raceUrl).get();
        } catch (IOException e) {
        }
        int index = 1;
        List<Double> x = new ArrayList<>();
        List<Double> y1 = new ArrayList<>();
        List<Double> y2 = new ArrayList<>();
        for (Element table : doc.getElementsByTag("tbody")) {
            if (tableInd.contains(index)) {
                for (Element e : table.select("tr")) {

                    String timeString = e.select("td:nth-of-type(" + timeIndex + ")").text();
                    String[] splitTime = e.select("td:nth-of-type(" + timeIndex + ")").text().split(":");
                    if (splitTime.length == 2) {
                        Double time = (Double.parseDouble(splitTime[0]) * 60) + Double.parseDouble(splitTime[1]);
                        String[] splitName = e.select("td:nth-of-type(" + nameIndex + ")").text().split(", ");
                        String fName = splitName[1];
                        String lName = splitName[0];
                        String school = e.select("td:nth-of-type(" + schoolIndex + ")").text();
                        double existingHighestRating;
                        try {
                            PreparedStatement stmt = Populate.conn.prepareStatement(
                                    "select Max(rating) from Runner_In_Race " +
                                            "       where first_name = ? AND last_name = ?" +
                                            "       AND school = ? AND gender = ?;");
                            stmt.setString(1, fName);
                            stmt.setString(2, lName);
                            stmt.setString(3, school);
                            stmt.setString(4, gender);
                            ResultSet rs = stmt.executeQuery();
                            if (!rs.next()) {
                                existingHighestRating = -1.0;
                            } else {
                                existingHighestRating = rs.getDouble(1);
                            }
                            PreparedStatement stmt2 = Populate.conn.prepareStatement(
                                    "select agg_rating, hidden_rating from Runners " +
                                            "       where first_name = ? AND last_name = ?" +
                                            "       AND school = ? AND gender = ?;");
                            stmt2.setString(1, fName);
                            stmt2.setString(2, lName);
                            stmt2.setString(3, school);
                            stmt2.setString(4, gender);
                            ResultSet rs2 = stmt2.executeQuery();
                            double rating = 0;
                            boolean isInDB;
                            if (rs2.next()) {
                                rating = (rs2.getDouble(1) == NULL) ? rs2.getDouble(2) : rs2.getDouble(1);
                                isInDB = true;
                            } else {
                                isInDB = false;
                            }
                            if (rating != 0) {
                                n++;
                            }
                            if (!timeString.equals("DNF") && !timeString.equals("dnf")) {
                                Runner runner = new Runner(fName, lName, school, time, timeString, isInDB, existingHighestRating, rating);
                                int i = 0;
                                while (runners.size() > i && time >= runners.get(i).getTime()) {
                                    i++;
                                }
                                runners.add(i, runner);
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            index++;
        }
        double[][] xArr = new double[n][2];
        double[][] ratingArr = new double[n][1];
        double[][] timeArr = new double[n][2];
        int i = 0;
        for (Runner r : runners) {
            if (r.getAgg_rating() > 0) {
                xArr[i][0] = 1;
                xArr[i][1] = i + 1;
                ratingArr[i][0] = r.getAgg_rating();
                timeArr[i][0] = 1;
                timeArr[i][1] = r.getTime();
                i++;
            }
        }
        mapping = new Regression(timeArr, ratingArr);
    }

    /**
     * Displays data neatly in a javafx window
     *
     *
     */
    public List<Double> getRatings() {
        double x1 = runners.get(runners.size() / 2).getTime();
        double y1 = mapping.calculate(x1);
        double slope = (gender.equals("0") ? -.634196 * (5 / (raceLength)) : -.550879 * (5 / (raceLength)));
        for (int i = 0; i < runners.size(); i++) {
            double aggRating = (slope * (runners.get(i).getTime() - x1)) + y1;
            ratings.add(aggRating);
            System.out.println(runners.get(i).toString() + " " + aggRating);
        }
        return ratings;
    }

    public List<Double> modifyRatings(List<Double> rats, double constant) {
        List<Double> modifiedRats = new ArrayList<>();
        for (int i = 0; i < rats.size(); i++) {
            modifiedRats.add(rats.get(i) + constant);
        }
        return modifiedRats;
    }


    public List<Runner> getRunners() {
        return runners;
    }

}
