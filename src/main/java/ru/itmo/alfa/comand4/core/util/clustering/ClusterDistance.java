package ru.itmo.alfa.comand4.core.util.clustering;

import java.util.Arrays;

public class ClusterDistance {

    /**
     * Вычисляет среднее расстояние от указанной точки до всех других точек
     * в её собственном кластере (исключая саму точку).
     *
     * @param features   матрица признаков
     * @param labels   массив меток кластеров для каждой точки
     * @param pointIndex индекс точки, для которой вычисляется расстояние
     * @return среднее расстояние до других точек в том же кластере, или 0 если точка единственная в кластере
     */
    public static double calculateAverageDistanceToOwnCluster(double[][] features, int[] labels, int pointIndex) {
        int cluster = labels[pointIndex];
        double sum = 0.0;
        int count = 0;

        for (int i = 0; i < features.length; i++) {
            if (labels[i] == cluster && i != pointIndex) {
                sum += distance(features[pointIndex], features[i]);
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    /**
     * Вычисляет среднее расстояние от указанной точки до всех точек в целевом кластере.
     *
     * @param features      матрица признаков
     * @param labels      массив меток кластеров для каждой точки
     * @param pointIndex    индекс исходной точки, от которой измеряются расстояния
     * @param targetCluster целевой кластер, до точек которого измеряются расстояния
     * @return среднее расстояние до точек целевого кластера, или Double.MAX_VALUE если целевой кластер пуст
     */
    public static double calculateAverageDistanceToSpecificCluster(double[][] features, int[] labels, int pointIndex, int targetCluster) {
        double sum = 0.0;
        int count = 0;

        for (int i = 0; i < features.length; i++) {
            if (labels[i] == targetCluster) {
                sum += distance(features[pointIndex], features[i]);
                count++;
            }
        }

        return count > 0 ? sum / count : Double.MAX_VALUE;
    }

    /**
     * Вычисляет среднее расстояние от указанной точки до ближайшего соседнего кластера (исключая её собственный кластер).
     *
     * @param features   матрица признаков [n_samples][n_features]
     * @param labels   массив меток кластеров для каждой точки
     * @param pointIndex индекс точки, для которой вычисляется расстояние
     * @return среднее расстояние до ближайшего соседнего кластера, или 0 если есть только один кластер
     */
    public static double calculateAverageDistanceToNearestCluster(double[][] features, int[] labels, int pointIndex) {
        int ownCluster = labels[pointIndex];
        double minAvgDistance = Double.MAX_VALUE;

        // Находим все уникальные кластеры
        int[] uniqueClusters = Arrays.stream(labels).distinct().toArray();

        for (int cluster : uniqueClusters) {
            if (cluster != ownCluster) {
                double avgDistance = calculateAverageDistanceToSpecificCluster(features, labels, pointIndex, cluster);
                if (avgDistance < minAvgDistance) {
                    minAvgDistance = avgDistance;
                }
            }
        }

        return minAvgDistance < Double.MAX_VALUE ? minAvgDistance : 0.0;
    }

    public static double distance(double[] a, double[] b) {
        return manhattanDistance(a, b);
    }

    /**
     * Вычисляет евклидово расстояние между двумя точками в многомерном пространстве.
     *
     * @param a первая точка
     * @param b вторая точка
     *
     * @return евклидово расстояние между точками
     *
     * @throws IllegalArgumentException если точки имеют разную размерность
     */
    private static double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * Манхэттенское расстояние (L1 норма) - более устойчиво к выбросам
     */
    private static double manhattanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum;
    }
}
