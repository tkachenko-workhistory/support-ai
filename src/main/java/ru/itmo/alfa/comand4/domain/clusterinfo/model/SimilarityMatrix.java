package ru.itmo.alfa.comand4.domain.clusterinfo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class SimilarityMatrix {

    private final double[][] matrix;
    private final String[] labels;

    public int getSize() {
        return matrix.length;
    }

    public double getMinValue() {
        double min = Double.MAX_VALUE;
        for (double[] row : matrix) {
            for (double val : row) {
                if (val < min) min = val;
            }
        }
        return min;
    }

    public double getMaxValue() {
        double max = Double.MIN_VALUE;
        for (double[] row : matrix) {
            for (double val : row) {
                if (val > max) max = val;
            }
        }
        return max;
    }
}
