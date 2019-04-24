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
import java.util.*;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Analyzes times from a specific race
 */
public class AddRace extends Application {

    private static Regression mapping;
    private static List<Double> allTimes = new ArrayList<>();
    private static List<Double> allRatings = new ArrayList<>();

    /**
     * Main method to parse race results and compare them against expected
     * outcomes based on previous performances pulled from the database.
     *
     * @param args user input
     */
    public static void main(String[] args) {
        String raceUrl = "https://www.tfrrs.org/results/xc/15036/NCAA_DI_Cross_Country_Championships";
        Document doc = null;
        try {
            doc = Jsoup.connect(raceUrl).get();
        }
        catch (IOException e){
        }
        int index = 1;
        List<Double> x = new ArrayList<>();
        List<Double> y1 = new ArrayList<>();
        List<Double> y2 = new ArrayList<>();
        for (Element e : doc.select("tr")) {
            if (index >= 322 && index <= 573) {
                double n = 0;
                String[] splitTime = e.select("td:nth-of-type(6)").text().split(":");
                Double time = (Double.parseDouble(splitTime[0]) * 60) + Double.parseDouble(splitTime[1]);
                String url = e.select(":nth-of-type(1)").select("a").first().attr("abs:href");
                int runNumber= Integer.parseInt(url.replaceAll("[^0-9]", ""));
                try {
                    Statement stmt = Populate.conn.createStatement();
                    String check = "select rating from harriers where id = " + runNumber;
                    ResultSet rs = stmt.executeQuery(check);
                    double rating = 0;
                    while (rs.next()) {
                        rating = rs.getDouble(1);
                    }
                    if (rating != 0) {
                        x.add(n++);
                        y1.add(time);
                        y2.add(rating);
                    }
                    allTimes.add(time);
                    allRatings.add(rating);

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            index++;
        }
        double[][] xArr = new double[x.size()][2];
        double[][] ratingArr = new double[y1.size()][1];
        double[][] timeArr = new double[y2.size()][2];
        for (int i = 0; i < xArr.length; i++) {
            xArr[i][0] = 1;
            xArr[i][1] = i + 1;
            ratingArr[i][0] = y2.get(i);
            timeArr[i][0] = 1;
            timeArr[i][1] = y1.get(i);
        }

        mapping = new Regression(timeArr, ratingArr);
        launch();
    }

    /**
     * Displays data neatly in a javafx window
     *
     * @param stage javafx window
     */
    public void start(Stage stage) throws Exception {
        stage.setTitle("times vs rating");
        final NumberAxis xAxis = new NumberAxis(0, 210, 10);
        final NumberAxis yAxis = new NumberAxis(850, 975, 100);
        final ScatterChart<Number,Number> sc = new ScatterChart<Number,Number>(xAxis,yAxis);
        xAxis.setLabel("Place");
        yAxis.setLabel("Rating and Time");
        XYChart.Series series1 = new XYChart.Series();
        XYChart.Series series2 = new XYChart.Series();
        XYChart.Series series3 = new XYChart.Series();
        series1.setName("times");
        series2.setName("ratings");
        series3.setName("prevRatings");
        for (int i = 0; i < allTimes.size(); i++) {
            series1.getData().add(new XYChart.Data(i + 1, allTimes.get(i)));
            System.out.println(mapping.calculate(allTimes.get(i)));
            series2.getData().add(new XYChart.Data(i + 1, mapping.calculate(allTimes.get(i))));
            if (allRatings.get(i) != 0) {
                series3.getData().add(new XYChart.Data(i + 1, allRatings.get(i)));
            }
        }

        sc.getData().addAll(series1, series2, series3);
        Scene scene  = new Scene(sc, 1000, 800);
        stage.setScene(scene);
        stage.show();
    }
}
