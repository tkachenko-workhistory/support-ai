package ru.itmo.alfa.comand4.domain.clusterinfo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.core.model.ModelData;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.ClusterQuality;
import smile.clustering.KMeans;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClusterQualityService {

    public ClusterQuality evaluateQuality(ModelData modelData) {
        if (modelData == null || modelData.getModel() == null || modelData.getFeatures() == null) {
            throw new IllegalArgumentException("ModelData не может быть null");
        }

        KMeans kmeans = modelData.getModel();
        double[][] features = modelData.getFeatures();
        int[] labels = kmeans.y;

        // Рассчитываем все метрики
        double silhouetteScore = calculateSilhouetteScore(features, labels);
        double wcss = kmeans.distortion;
        double bcss = calculateBCSS(features, labels, kmeans.centroids);

        return new ClusterQuality(
                silhouetteScore,
                wcss,
                bcss,
                kmeans.k,
                features.length
        );
    }

    /**
     * Silhouette Score - основная метрика качества кластеризации
     * Диапазон: [-1, 1] (чем ближе к 1, тем лучше)
     */
    private double calculateSilhouetteScore(double[][] features, int[] labels) {
        int n = features.length;
        if (n <= 1) return 0.0;

        double totalSilhouette = 0.0;
        int validSamples = 0;

        for (int i = 0; i < n; i++) {
            double a = calculateAverageDistanceToOwnCluster(features, labels, i);
            double b = calculateAverageDistanceToNearestCluster(features, labels, i);

            double maxAB = Math.max(a, b);
            if (maxAB > 0) {
                double silhouette = (b - a) / maxAB;
                totalSilhouette += silhouette;
                validSamples++;
            }
        }

        return validSamples > 0 ? totalSilhouette / validSamples : 0.0;
    }

    public double calculateAverageDistanceToOwnCluster(double[][] features, int[] labels, int pointIndex) {
        int cluster = labels[pointIndex];
        double sum = 0.0;
        int count = 0;

        for (int i = 0; i < features.length; i++) {
            if (labels[i] == cluster && i != pointIndex) {
                sum += euclideanDistance(features[pointIndex], features[i]);
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public double calculateAverageDistanceToSpecificCluster(double[][] features, int[] labels, int pointIndex, int targetCluster) {
        double sum = 0.0;
        int count = 0;

        for (int i = 0; i < features.length; i++) {
            if (labels[i] == targetCluster) {
                sum += euclideanDistance(features[pointIndex], features[i]);
                count++;
            }
        }

        return count > 0 ? sum / count : Double.MAX_VALUE;
    }

    public double calculateAverageDistanceToNearestCluster(double[][] features, int[] labels, int pointIndex) {
        int ownCluster = labels[pointIndex];
        double minAvgDistance = Double.MAX_VALUE;

        // Находим все уникальные кластеры
        int[] uniqueClusters = java.util.Arrays.stream(labels).distinct().toArray();

        for (int cluster : uniqueClusters) {
            if (cluster != ownCluster) {
                double avgDistance = calculateAverageDistanceToCluster(features, labels, pointIndex, cluster);
                if (avgDistance < minAvgDistance) {
                    minAvgDistance = avgDistance;
                }
            }
        }

        return minAvgDistance < Double.MAX_VALUE ? minAvgDistance : 0.0;
    }

    private double calculateAverageDistanceToCluster(double[][] features, int[] labels, int pointIndex, int targetCluster) {
        double sum = 0.0;
        int count = 0;

        for (int i = 0; i < features.length; i++) {
            if (labels[i] == targetCluster) {
                sum += euclideanDistance(features[pointIndex], features[i]);
                count++;
            }
        }

        return count > 0 ? sum / count : Double.MAX_VALUE;
    }

    /**
     * Between-Cluster Sum of Squares
     */
    private double calculateBCSS(double[][] features, int[] labels, double[][] centroids) {
        double[] globalCentroid = calculateGlobalCentroid(features);
        double bcss = 0.0;

        Map<Integer, Integer> clusterSizes = new HashMap<>();
        for (int label : labels) {
            clusterSizes.put(label, clusterSizes.getOrDefault(label, 0) + 1);
        }

        for (Map.Entry<Integer, Integer> entry : clusterSizes.entrySet()) {
            int clusterId = entry.getKey();
            int size = entry.getValue();
            double distance = euclideanDistance(centroids[clusterId], globalCentroid);
            bcss += size * distance * distance;
        }

        return bcss;
    }

    private double[] calculateGlobalCentroid(double[][] features) {
        int d = features[0].length;
        double[] centroid = new double[d];

        for (double[] feature : features) {
            for (int j = 0; j < d; j++) {
                centroid[j] += feature[j];
            }
        }

        for (int j = 0; j < d; j++) {
            centroid[j] /= features.length;
        }

        return centroid;
    }

    public double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }
}