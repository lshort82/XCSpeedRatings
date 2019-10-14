package com.company;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import java.sql.*;
import java.io.*;

/**
 * Adds runners and 5k PRs to the database
 */
public class Populate {
    protected static java.sql.Connection conn;
    static {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://harrierdb.cdorakeie0ns.us-east-1.rds.amazonaws.com:3306/Harriers", "lshort82", "V1a_Vancouver");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static final String mensTeams = "MensTeamUrls";
    private static final String womensTeams = "WomensTeamUrls";
    private static final double mensRecord = 788.4;
    private static final double womensRecord = 907.64;

    /**
     * Main method establishes connection to the database
     *
     * @param args input
     */
    public static void main(String[] args) throws Exception {
        populateDB(womensTeams);
        //System.out.println(Runner.getRunnerList().toString());
    }

    /**
     * Adds all the runners to the database.
     *
     * @param fileName file of list of URLs
     */
    private static void populateDB(String fileName) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (sc.hasNext()) {
            String url = "https://www.tfrrs.org/teams/" + sc.next();

            Document doc = null;
            try {
                doc = Jsoup.connect(url).get();
            }
            catch (IOException e){
                System.out.println(url);
            }
            try {
                String school = doc.select("h3").first().text();
                Iterator<Element> iter = doc.select("table").iterator();
                boolean keepGoing = true;
                Element roster = null;
                Element current = iter.next();
                while (iter.hasNext() && keepGoing) {
                    if (current.select("thead th").first().text().equals("NAME")) {
                        keepGoing = false;
                        roster = current.select("tbody").first().nextElementSibling();
                    }
                    current = iter.next();
                }
                if (roster != null) {
                    try {
                        PreparedStatement stmt = conn.prepareStatement("insert into Schools "
                                + "(name) "
                                + "values (?)");
                        stmt.setString(1, school);
                        stmt.execute();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    for (Element row : roster.select("tr")) {
                        checkRunner(row, school);
                    }
                } else {
                    System.out.println("error");
                }
            } catch (NullPointerException e) {
                System.out.println(url);
            }
        }
    }

    /**
     * Makes sure the runner isn't already in the database, and that
     * their name is standardized.
     *
     * @param row in the website
     * @param school of the runner
     */
    public static void checkRunner(Element row, String school) {
        String url = row.select(":nth-of-type(1)").select("a").first().attr("abs:href");
        double time = lookFor5k(url);
        if (time != 0)
        addRunner(row, "1", time, school);
    }

    /**
     * Adds the runner to the database
     *
     * @param row in the website
     * @param gender of the athlete
     * @param time 5k PR, if available
     * @param school of the runner
     */
    public static void addRunner(Element row, String gender, double time, String school) {
        String runner = row.select("td:nth-of-type(1)").text();
        double rating;
        if (time == 0) {
            rating = 0;
        } else {
            rating = Math.round((womensRecord / time * 1000.0) * 100.0) / 100.0;
        }
        String fName;
        String lName;
        String[] splitString;
        if (runner.contains(", ")) {
            splitString = runner.split(", ");
            fName = splitString[1];
            lName = splitString[0];
        } else if (runner.contains(",")) {
            splitString = runner.split(",");
            fName = splitString[1];
            lName = splitString[0];
        } else {
            splitString = runner.split(" ");
            fName = splitString[0];
            lName = splitString[1];
        }
        try {
            PreparedStatement stmt = conn.prepareStatement("insert into Runners "
                    + "(gender, school, first_name, last_name, agg_rating) "
                    + "values (?, ?, ?, ?, ?)");
            stmt.setString(1, gender);
            stmt.setString(2, school);
            stmt.setString(3, fName);
            stmt.setString(4, lName);
            stmt.setDouble(5, rating);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if a runner has a 5k track time
     *
     * @param url
     * @return the time, if available or 0
     */
    private static double lookFor5k(String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        }
        catch (IOException e){
        }
        Element timesTable = doc.select("table").first();
        Element times = timesTable.select("tbody").first();
        if (times != null) {
            for (Element e : times.select("td")) {
                if (e.text().equals("5000")) {
                    try {
                        return normalizeTime(e.nextElementSibling().text());
                    } catch (Exception ex) {
                        System.out.println(times);
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Turns the String time a Double time
     *
     * @param timeString the 5k time as a String
     * @return the 5k time as a Double
     * @throws NullPointerException
     * @throws NumberFormatException
     */
    private static double normalizeTime(String timeString) throws NullPointerException, NumberFormatException{
        String[] splitTime = timeString.split(":");
        Double time = (Double.parseDouble(splitTime[0]) * 60) + Double.parseDouble(splitTime[1]);
        return time;
    }
}
