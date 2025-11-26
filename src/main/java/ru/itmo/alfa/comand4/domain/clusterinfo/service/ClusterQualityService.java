package ru.itmo.alfa.comand4.domain.clusterinfo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.core.model.ModelData;
import ru.itmo.alfa.comand4.core.util.clustering.ClusterDistance;
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
            double a = ClusterDistance.calculateAverageDistanceToOwnCluster(features, labels, i);
            double b = ClusterDistance.calculateAverageDistanceToNearestCluster(features, labels, i);

            double maxAB = Math.max(a, b);
            if (maxAB > 0) {
                double silhouette = (b - a) / maxAB;
                totalSilhouette += silhouette;
                validSamples++;
            }
        }

        return validSamples > 0 ? totalSilhouette / validSamples : 0.0;
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
            double distance = ClusterDistance.distance(centroids[clusterId], globalCentroid);
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

}