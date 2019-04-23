package com.company;
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
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/com", "root", "Alla_S@cramento");
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
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/com", "root", "Alla_S@cramento");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        populateDB(mensTeams);
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
            String url = sc.next();

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
        int runNumber= Integer.parseInt(url.replaceAll("[^0-9]", ""));
        try {
            Statement stmt = conn.createStatement();
            String check = "select * from harriers where id = " + runNumber;
            ResultSet rs = stmt.executeQuery(check);
            if (!rs.next()) {
                addRunner(row, runNumber, time, school);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds the runner to the database
     *
     * @param row in the website
     * @param id unique id
     * @param time 5k PR, if available
     * @param school of the runner
     */
    public static void addRunner(Element row, int id, double time, String school) {
        String runner = row.select("td:nth-of-type(1)").text();
        int rating = (int) Math.round(mensRecord / time * 1000);
        rating = (rating == -1) ? 0 : rating;
        String fName;
        String lName;
        String[] splitString;
        if (runner.contains(", ")) {
            splitString = runner.split(", ");
            fName = splitString[1].replaceAll("[^A-Za-z]", "");
            lName = splitString[0].replaceAll("[^A-Za-z]", "");
        } else if (runner.contains(",")) {
            splitString = runner.split(",");
            fName = splitString[1].replaceAll("[^A-Za-z]", "");
            lName = splitString[0].replaceAll("[^A-Za-z]", "");
        } else {
            splitString = runner.split(" ");
            fName = splitString[0];
            lName = splitString[1];
        }
        try {
            PreparedStatement stmt = conn.prepareStatement("insert into harriers "
                    + "(id, firstName, lastName, rating, team) "
                    + "values (?, ?, ?, ?, ?)");
            stmt.setInt(1, id);
            stmt.setString(2, fName);
            stmt.setString(3, lName);
            stmt.setInt(4, rating);
            stmt.setString(5, school);
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
