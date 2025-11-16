package ru.itmo.alfa.comand4.utils;

import smile.clustering.KMeans;

import java.util.ArrayList;
import java.util.List;

/**
 * Метод "Локтя" для определения количества кластеров
 */
public class ClusterCounting {

    public static int findOptimalK(double[][] features) {
        List<Double> distortions = new ArrayList<>();

        // Не больше чем n/50 и не меньше 2
        int maxK = Math.max(2, features.length / 50);

        for (int k = 2; k <= maxK; k++) {
            KMeans kmeans = KMeans.fit(features, k);
            distortions.add(kmeans.distortion);
            //System.out.println("k=" + k + ", WCSS=" + kmeans.distortion);
        }

        // Находим "локоть" - точку, где уменьшение WCSS замедляется
        return findElbowPoint(distortions);
    }

    private static int findElbowPoint(List<Double> distortions) {
        // Ищем наибольшее изменение наклона
        double maxDrop = 0;
        int optimalK = 2;

        for (int i = 1; i < distortions.size() - 1; i++) {
            double prevDrop = distortions.get(i-1) - distortions.get(i);
            double nextDrop = distortions.get(i) - distortions.get(i+1);
            double dropRatio = prevDrop / nextDrop;

            if (dropRatio > maxDrop) {
                maxDrop = dropRatio;
                optimalK = i + 1;
            }
        }

        return optimalK;
    }
}
