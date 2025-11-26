package ru.itmo.alfa.comand4.domain.clusterinfo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.core.model.ModelData;
import ru.itmo.alfa.comand4.core.util.clustering.ClusterDistance;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.SimilarityMatrix;
import smile.clustering.KMeans;

@Service
@RequiredArgsConstructor
public class ClusterSimilarityService {

    public SimilarityMatrix calculateClusterSimilarity(ModelData modelData) {
        KMeans kmeans = modelData.getModel();
        double[][] features = modelData.getFeatures();
        int[] labels = kmeans.y;

        int k = kmeans.k;
        double[][] similarityMatrix = new double[k][k];
        String[] clusterLabels = new String[k];

        for (int i = 0; i < k; i++) {
            clusterLabels[i] = "Кл." + i;
            for (int j = 0; j < k; j++) {
                if (i == j) {
                    similarityMatrix[i][j] = 1; // Диагональ
                } else {
                    double similarity = calculateSilhouetteBasedSimilarity(features, labels, i, j);
                    similarityMatrix[i][j] = similarity;
                }
            }
        }

        return new SimilarityMatrix(similarityMatrix, clusterLabels);
    }

    private double calculateSilhouetteBasedSimilarity(double[][] features, int[] labels, int clusterA, int clusterB) {
        double totalSimilarity = 0.0;
        int count = 0;

        // Для каждой точки в кластере A
        for (int i = 0; i < features.length; i++) {
            if (labels[i] == clusterA) {
                double a = ClusterDistance.calculateAverageDistanceToOwnCluster(features, labels, i);
                double b = ClusterDistance.calculateAverageDistanceToSpecificCluster(features, labels, i, clusterB);

                if (a > 0 && b > 0 && b < Double.MAX_VALUE) {
                    double maxAB = Math.max(a, b);
                    double silhouette = (b - a) / maxAB;  // Диапазон: [-1, 1]

                    // Преобразуем в [0, 1] для тепловой карты
                    double similarity = (silhouette + 1) / 2;
                    totalSimilarity += similarity;
                    count++;
                }
            }
        }

        return count > 0 ? totalSimilarity / count : 0.0;
    }
}
