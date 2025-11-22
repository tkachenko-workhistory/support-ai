package ru.itmo.alfa.comand4.service;

import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.model.serial.ModelData;
import smile.clustering.KMeans;
import smile.manifold.TSNE;
import smile.plot.swing.Canvas;
import smile.plot.swing.ScatterPlot;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class KMeansVisualizer {

    public byte[] generateClusterPlot(ModelData modelData) throws IOException {
        if (modelData == null || modelData.getModel() == null || modelData.getFeatures() == null) {
            throw new IllegalArgumentException("ModelData, модель или features не могут быть null");
        }

        KMeans kmeans = modelData.getModel();
        double[][] features = modelData.getFeatures();

        // Проектируем features в 2D (t-SNE проекция)
        // t-SNE специально разработан для визуализации кластеров
        TSNE tsne = new TSNE(features, 2);
        double[][] features2D = tsne.coordinates;

        // Создаем метки для точек
        String[] labels = new String[features2D.length];
        int[] clusters = kmeans.y;

        for (int i = 0; i < features2D.length; i++) {
            int clusterId = clusters[i];
            labels[i] = String.format("Кластер %d", clusterId);
        }

        // Создаем scatter plot
        ScatterPlot plot = ScatterPlot.of(features2D, labels, 'o');
        Canvas canvas = plot.canvas();

        canvas.setTitle("Кластеризация обращений техподдержки");
        canvas.setAxisLabels("Компонент 1", "Компонент 2");

        // Добавляем центроиды
        double[][] centroids2D = projectCentroidsWithTSNE(kmeans.centroids, tsne, features);
        ScatterPlot centroidsPlot = ScatterPlot.of(centroids2D, '#', Color.BLACK);
        canvas.add(centroidsPlot);

        return convertCanvasToByteArray(canvas, 800, 600);
    }

    /**
     * Преобразование точек через обученную t-SNE модель
     */
    private double[][] projectCentroidsWithTSNE(double[][] centroids, TSNE tsne, double[][] features) {
        // Находим ближайшие точки в исходном пространстве
        // и берем их t-SNE координаты
        double[][] result = new double[centroids.length][2];

        for (int i = 0; i < centroids.length; i++) {
            int nearestIndex = findNearestPointIndex(centroids[i], features);
            result[i] = tsne.coordinates[nearestIndex].clone();
        }

        return result;
    }

    private int findNearestPointIndex(double[] point, double[][] features) {
        int nearestIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < features.length; i++) {
            double distance = euclideanDistance(point, features[i]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }

    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    private byte[] convertCanvasToByteArray(Canvas canvas, int width, int height) throws IOException {
        BufferedImage image = canvas.toBufferedImage(width, height);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }
}
