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
        double calinskiHarabasz = calculateCalinskiHarabasz(features, labels);
        double daviesBouldin = calculateDaviesBouldin(features, labels);
        double wcss = kmeans.distortion;
        double bcss = calculateBCSS(features, labels, kmeans.centroids);

        return new ClusterQuality(
                silhouetteScore,
                calinskiHarabasz,
                daviesBouldin,
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

    private double calculateAverageDistanceToOwnCluster(double[][] features, int[] labels, int pointIndex) {
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

    private double calculateAverageDistanceToNearestCluster(double[][] features, int[] labels, int pointIndex) {
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
     * Calinski-Harabasz Index
     * Чем выше значение, тем лучше разделение кластеров
     */
    private double calculateCalinskiHarabasz(double[][] features, int[] labels) {
        int n = features.length;
        int k = java.util.Arrays.stream(labels).distinct().toArray().length;

        if (n <= k || k <= 1) return 0.0;

        double bcss = calculateBCSS(features, labels, calculateGlobalCentroid(features));
        double wcss = calculateWCSS(features, labels);

        return (bcss / (k - 1)) / (wcss / (n - k));
    }

    /**
     * Davies-Bouldin Index
     * Чем меньше значение, тем лучше разделение кластеров
     */
    private double calculateDaviesBouldin(double[][] features, int[] labels) {
        int k = java.util.Arrays.stream(labels).distinct().toArray().length;
        if (k <= 1) return 0.0;

        double[][] centroids = calculateClusterCentroids(features, labels);
        double[] clusterDispersions = calculateClusterDispersions(features, labels, centroids);

        double totalDB = 0.0;

        for (int i = 0; i < k; i++) {
            double maxRatio = 0.0;
            for (int j = 0; j < k; j++) {
                if (i != j) {
                    double distance = euclideanDistance(centroids[i], centroids[j]);
                    if (distance > 0) {
                        double ratio = (clusterDispersions[i] + clusterDispersions[j]) / distance;
                        maxRatio = Math.max(maxRatio, ratio);
                    }
                }
            }
            totalDB += maxRatio;
        }

        return totalDB / k;
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

    private double calculateBCSS(double[][] features, int[] labels, double[] globalCentroid) {
        double bcss = 0.0;
        Map<Integer, Integer> clusterSizes = new HashMap<>();
        double[][] centroids = calculateClusterCentroids(features, labels);

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

    /**
     * Within-Cluster Sum of Squares
     */
    private double calculateWCSS(double[][] features, int[] labels) {
        double wcss = 0.0;
        double[][] centroids = calculateClusterCentroids(features, labels);

        for (int i = 0; i < features.length; i++) {
            int cluster = labels[i];
            double distance = euclideanDistance(features[i], centroids[cluster]);
            wcss += distance * distance;
        }

        return wcss;
    }

    // Вспомогательные методы
    private double[][] calculateClusterCentroids(double[][] features, int[] labels) {
        Map<Integer, double[]> sums = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();

        for (int i = 0; i < features.length; i++) {
            int cluster = labels[i];
            double[] point = features[i];

            if (!sums.containsKey(cluster)) {
                sums.put(cluster, new double[point.length]);
                counts.put(cluster, 0);
            }

            double[] sum = sums.get(cluster);
            for (int j = 0; j < point.length; j++) {
                sum[j] += point[j];
            }
            counts.put(cluster, counts.get(cluster) + 1);
        }

        double[][] centroids = new double[sums.size()][features[0].length];
        for (Map.Entry<Integer, double[]> entry : sums.entrySet()) {
            int cluster = entry.getKey();
            double[] sum = entry.getValue();
            int count = counts.get(cluster);

            for (int j = 0; j < sum.length; j++) {
                centroids[cluster][j] = sum[j] / count;
            }
        }

        return centroids;
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

    private double[] calculateClusterDispersions(double[][] features, int[] labels, double[][] centroids) {
        Map<Integer, Double> dispersions = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();

        for (int i = 0; i < features.length; i++) {
            int cluster = labels[i];
            double distance = euclideanDistance(features[i], centroids[cluster]);
            dispersions.put(cluster, dispersions.getOrDefault(cluster, 0.0) + distance);
            counts.put(cluster, counts.getOrDefault(cluster, 0) + 1);
        }

        double[] result = new double[centroids.length];
        for (Map.Entry<Integer, Double> entry : dispersions.entrySet()) {
            int cluster = entry.getKey();
            double dispersion = entry.getValue();
            int count = counts.get(cluster);
            result[cluster] = dispersion / count;
        }

        return result;
    }

    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }
}