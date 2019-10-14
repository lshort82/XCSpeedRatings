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
public class AddRace {
    private static List<Double> mensAllRatings = new ArrayList<>();
    private static List<Double> womensAllRatings = new ArrayList<>();
    private static List<Runner> mensRunners = new ArrayList<>();
    private static List<Runner> womensRunners = new ArrayList<>();
    //Edit all below for each page
    private static String raceUrl = "https://www.tfrrs.org/results/xc/16300/Roy_Griak_Invitational";
    private static String raceName = "Roy Griak Invitational";
    private static String mensRaceDistance = "8k";
    private static String womensRaceDistance = "6k";
    private static double mensRaceLength = 8;
    private static double womensRaceLength = 6;
    private static String date = "2019-09-28";
    private static List<Integer> mensTableInd = new ArrayList<>(Arrays.asList(2, 6));
    private static List<Integer> womensTableInd = new ArrayList<>(Arrays.asList(4, 8));
    private static int timeIndex = 6;
    private static int nameIndex = 2;
    private static int schoolIndex = 4;

    /**
     * Main method to parse race results and compare them against expected
     * outcomes based on previous performances pulled from the database.
     *
     * @param args user input
     */
    public static void main(String[] args) throws SQLException {
        CalculateRace mensCalc = new CalculateRace(raceUrl, mensRaceLength, mensTableInd, timeIndex, nameIndex, schoolIndex, "0");
        CalculateRace womensCalc = new CalculateRace(raceUrl, womensRaceLength, womensTableInd, timeIndex, nameIndex, schoolIndex, "1");
        mensAllRatings = mensCalc.getRatings();
        System.out.println();
        System.out.println("womens:");
        womensAllRatings = womensCalc.getRatings();
        mensRunners = mensCalc.getRunners();
        womensRunners = womensCalc.getRunners();
        Scanner sc = new Scanner(System.in);
        System.out.println("Constant for men?");
        double constant = sc.nextDouble();
        if (constant != 0) {
            mensAllRatings = mensCalc.modifyRatings(mensAllRatings, constant);
        }
        System.out.println("Constant for women?");
        constant = sc.nextDouble();
        if (constant != 0) {
            womensAllRatings = mensCalc.modifyRatings(womensAllRatings, constant);
        }
        System.out.println(mensAllRatings);
        System.out.println(womensAllRatings);
        PreparedStatement statement = Populate.conn.prepareStatement(
                "INSERT INTO Race (name, mdistance, date, wdistance) VALUES (?, ?, ?, ?)" +
                        " ON DUPLICATE KEY UPDATE name = name;");
        statement.setString(1, raceName);
        statement.setString(2, mensRaceDistance);
        statement.setString(3, date);
        statement.setString(4, womensRaceDistance);
        statement.execute();
        try {
            insert("0");
            insert("1");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Displays data neatly in a javafx window
     *
     *
     */
    public static void insert(String gender) throws Exception {
        List<Runner> runners;
        List<Double> allRatings;
        if (gender.equals("0")) {
            runners = mensRunners;
            allRatings = mensAllRatings;
        } else {
            runners = womensRunners;
            allRatings = womensAllRatings;
        }
        for (int i = 0; i < allRatings.size(); i++) {
            double agg_rating = Math.max(((.65 * runners.get(i).getExistingHighestRating()) + (.35 * (allRatings.get(i)))), allRatings.get(i));
            if (!runners.get(i).isInDB()) {
                PreparedStatement statement = Populate.conn.prepareStatement(
                        "INSERT INTO Schools (name) VALUES (?)" +
                                " ON DUPLICATE KEY UPDATE name = name");
                statement.setString(1, runners.get(i).getSchool());
                statement.execute();
                PreparedStatement stmt = Populate.conn.prepareStatement(
                        "INSERT INTO Runners (gender, first_name, last_name, school, agg_rating, hidden_rating) " +
                                " VALUES (?, ?, ?, ?, ?, ?);");
                stmt.setString(1, gender);
                stmt.setString(2, runners.get(i).getFname());
                stmt.setString(3, runners.get(i).getLname());
                stmt.setString(4, runners.get(i).getSchool());
                stmt.setDouble(5, agg_rating);
                stmt.setDouble(6, NULL);
                stmt.execute();
            } else {
                PreparedStatement stmt = Populate.conn.prepareStatement(
                        "UPDATE Runners SET agg_rating = ?, school = ?" +
                                "       where first_name = ? AND last_name = ?" +
                                "       AND school = ? AND gender = ?;");
                stmt.setDouble(1, agg_rating);
                stmt.setString(2, runners.get(i).getSchool());
                stmt.setString(3, runners.get(i).getFname());
                stmt.setString(4, runners.get(i).getLname());
                stmt.setString(5, runners.get(i).getSchool());
                stmt.setString(6, gender);
                stmt.execute();
            }
            PreparedStatement stmt2 = Populate.conn.prepareStatement(
                    "INSERT INTO Runner_In_Race (race_name, gender, school, first_name, last_name, finish_time, rating, place, date) " +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                            "   ON DUPLICATE KEY UPDATE school = ?");
            stmt2.setString(1, raceName);
            stmt2.setString(2, gender);
            stmt2.setString(3, runners.get(i).getSchool());
            stmt2.setString(4, runners.get(i).getFname());
            stmt2.setString(5, runners.get(i).getLname());
            stmt2.setString(6, runners.get(i).getStringTime());
            stmt2.setDouble(7, allRatings.get(i));
            stmt2.setInt(8, (i + 1));
            stmt2.setString(9, date);
            stmt2.setString(10, runners.get(i).getSchool());
            stmt2.execute();
        }
    }
}
