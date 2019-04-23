package com.company;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;

/**
 * Class to find regression and best fit analysis for two-variable systems
 */
public class Regression {

    private RealMatrix regMatrix;
    private RealMatrix formulaMatrix;

    /**
     * Constructor to set up the Regression model
     *
     * @param x the dependent variable list
     * @param y the independent variable list
     */
    public Regression(double[][] x, double[][] y) {
        RealMatrix xMatrix = MatrixUtils.createRealMatrix(x);
        RealMatrix yMatrix = MatrixUtils.createRealMatrix(y);
        LUDecomposition lu = new LUDecomposition(xMatrix.transpose().multiply(xMatrix));
        if (lu.getDeterminant() == 0) {
            throw new IllegalArgumentException("non invertible matrix");
        }
        RealMatrix inverse = lu.getSolver().getInverse();
        formulaMatrix = inverse.multiply(xMatrix.transpose().multiply(yMatrix));
        regMatrix = xMatrix.multiply(formulaMatrix);
    }

    public double calculate(double time) {
        return MatrixUtils.createColumnRealMatrix(new double[]{1, time}).transpose().multiply(formulaMatrix).getData()[0][0];
    }

    public RealMatrix getRegMatrix() {
        return regMatrix;
    }
}
