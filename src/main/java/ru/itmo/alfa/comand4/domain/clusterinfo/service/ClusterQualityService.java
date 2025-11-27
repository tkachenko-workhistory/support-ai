package ru.itmo.alfa.comand4.domain.clusterinfo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.core.model.ModelData;
import ru.itmo.alfa.comand4.core.util.clustering.ClusterDistance;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.ClusterQuality;
import smile.clustering.KMeans;

import java.util.Arrays;
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

        // Новые метрики сбалансированности
        int[] clusterSizes = calculateClusterSizes(labels, kmeans.k);
        double balanceIndex = calculateClusterBalanceIndex(clusterSizes);
        double giniCoefficient = calculateClusterGini(clusterSizes);

        return new ClusterQuality(
                silhouetteScore,
                wcss,
                bcss,

                kmeans.k,
                features.length,

                balanceIndex,
                giniCoefficient,
                clusterSizes
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

    /**
     * Расчет размеров кластеров
     */
    private int[] calculateClusterSizes(int[] labels, int k) {
        int[] sizes = new int[k];
        for (int label : labels) {
            if (label < k) {
                sizes[label]++;
            }
        }
        return sizes;
    }

    /**
     * Cluster Balance Index - мера сбалансированности кластеров
     * Диапазон: [0, 1], где 0 - идеально сбалансировано, 1 - все в одном кластере
     */
    private double calculateClusterBalanceIndex(int[] clusterSizes) {
        int n = Arrays.stream(clusterSizes).sum(); // Общее количество точек
        int k = clusterSizes.length; // Количество кластеров

        if (n == 0 || k <= 1)
            return 0.0;

        double idealSize = (double) n / k; // Идеальный размер кластера
        double imbalance = 0.0;

        // Сумма абсолютных отклонений от идеала
        for (int size : clusterSizes) {
            imbalance += Math.abs(size - idealSize);
        }

        // Нормализуем к [0, 1]
        return imbalance / (2 * (n - idealSize));
    }

    /**
     * Коэффициент Джини для распределения по кластерам
     * Диапазон: [0, 1], где 0 - равномерное распределение, 1 - максимальное неравенство
     */
    private double calculateClusterGini(int[] clusterSizes) {
        int n = Arrays.stream(clusterSizes).sum();
        int k = clusterSizes.length;

        if (n == 0 || k <= 1) return 0.0;

        // Сортируем размеры кластеров по возрастанию
        int[] sortedSizes = Arrays.stream(clusterSizes).sorted().toArray();

        double gini = 0.0;
        for (int i = 0; i < k; i++) {
            gini += (2 * (i + 1) - k - 1) * (double) sortedSizes[i];
        }

        return gini / (k * n);
    }

    /**
     * Генерация детального отчета о качестве кластеризации
     */
    public void generateQualityReport(ClusterQuality quality) {
        System.out.println("=== ДЕТАЛЬНЫЙ ОТЧЕТ О КАЧЕСТВЕ КЛАСТЕРИЗАЦИИ ===");
        System.out.printf("Количество кластеров: %d%n", quality.getNumberOfClusters());

        System.out.println("\n--- Метрики разделимости ---");
        System.out.printf("Silhouette Score:     %.4f %s%n",
                quality.getSilhouetteScore(), getSilhouetteAssessment(quality.getSilhouetteScore()));
        System.out.printf("WCSS:                 %.2f%n", quality.getWcss());
        System.out.printf("BCSS:                 %.2f%n", quality.getBcss());
        System.out.printf("BCSS/WCSS Ratio:      %.2f %s%n",
                quality.getBcss() / quality.getWcss(), getRatioAssessment(quality.getBcss() / quality.getWcss()));

        System.out.println("\n--- Метрики сбалансированности ---");
        System.out.printf("Balance Index:        %.4f %s%n",
                quality.getBalanceIndex(), getBalanceAssessment(quality.getBalanceIndex()));
        System.out.printf("Gini Coefficient:     %.4f %s%n",
                quality.getGiniCoefficient(), getGiniAssessment(quality.getGiniCoefficient()));
    }

    // Вспомогательные методы для оценки метрик
    private String getSilhouetteAssessment(double score) {
        if (score > 0.7) return "✓ ОТЛИЧНО";
        if (score > 0.5) return "✓ ХОРОШО";
        if (score > 0.25) return "▵ УДОВЛЕТВОРИТЕЛЬНО";
        return "✗ ПЛОХО";
    }

    private String getRatioAssessment(double ratio) {
        if (ratio > 3) return "✓ ВЫСОКАЯ РАЗДЕЛИМОСТЬ";
        if (ratio > 1) return "▵ УМЕРЕННАЯ РАЗДЕЛИМОСТЬ";
        return "✗ СЛАБАЯ РАЗДЕЛИМОСТЬ";
    }

    private String getBalanceAssessment(double balanceIndex) {
        if (balanceIndex < 0.1) return "✓ ИДЕАЛЬНО СБАЛАНСИРОВАНО";
        if (balanceIndex < 0.3) return "✓ ХОРОШО СБАЛАНСИРОВАНО";
        if (balanceIndex < 0.5) return "▵ УМЕРЕННЫЙ ДИСБАЛАНС";
        return "✗ СИЛЬНЫЙ ДИСБАЛАНС";
    }

    private String getEntropyAssessment(double entropy) {
        if (entropy > 0.8) return "✓ ВЫСОКАЯ РАВНОМЕРНОСТЬ";
        if (entropy > 0.6) return "✓ ХОРОШАЯ РАВНОМЕРНОСТЬ";
        if (entropy > 0.4) return "▵ СРЕДНЯЯ РАВНОМЕРНОСТЬ";
        return "✗ НИЗКАЯ РАВНОМЕРНОСТЬ";
    }

    private String getGiniAssessment(double gini) {
        if (gini < 0.2) return "✓ ВЫСОКАЯ РАВНОМЕРНОСТЬ";
        if (gini < 0.4) return "✓ ХОРОШАЯ РАВНОМЕРНОСТЬ";
        if (gini < 0.6) return "▵ СРЕДНЯЯ РАВНОМЕРНОСТЬ";
        return "✗ ВЫСОКОЕ НЕРАВЕНСТВО";
    }

    private String getCombinedAssessment(double score) {
        if (score > 0.7) return "✓ ОТЛИЧНОЕ КАЧЕСТВО";
        if (score > 0.5) return "✓ ХОРОШЕЕ КАЧЕСТВО";
        if (score > 0.3) return "▵ УДОВЛЕТВОРИТЕЛЬНО";
        return "✗ ТРЕБУЕТ УЛУЧШЕНИЯ";
    }

}