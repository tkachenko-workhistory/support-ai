package ru.itmo.alfa.comand4.domain.clusterinfo.service;

import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.core.model.ModelData;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.VizualizationMethod;
import smile.clustering.KMeans;
import smile.manifold.TSNE;
import smile.manifold.UMAP;
import smile.plot.swing.Canvas;
import smile.plot.swing.ScatterPlot;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class VisualizationGenerator {

    public byte[] generateClusterPlot(
            ModelData modelData,
            VizualizationMethod vizualizationMethod
    ) throws IOException {
        if (modelData == null || modelData.getModel() == null || modelData.getFeatures() == null) {
            throw new IllegalArgumentException("ModelData, модель или features не могут быть null");
        }

        KMeans kmeans = modelData.getModel();
        double[][] features = modelData.getFeatures();

        double[][] features2D;
        if (vizualizationMethod == VizualizationMethod.TSNE) {
            // Проектируем features в 2D (t-SNE проекция)
            // t-SNE специально разработан для визуализации кластеров
            TSNE tsne = new TSNE(features, 2);
            features2D = tsne.coordinates;
        } else {
            UMAP umap = UMAP.of(features,
                    15,  // k - количество соседей
                    2,      // d - размерность выхода (2 для визуализации)
                    500,    // iterations - количество итераций
                    1.0,    // learningRate - скорость обучения
                    0.1,    // minDist - минимальное расстояние между точками
                    1.0,    // spread - разброс
                    5,      // negativeSamples - количество негативных сэмплов
                    1.0     // repulsionStrength - сила отталкивания
            );
            features2D = umap.coordinates;
        }

        // Создаем метки для точек
        String[] labels = new String[features2D.length];
        int[] clusters = kmeans.y;

        for (int i = 0; i < features2D.length; i++) {
            int clusterId = clusters[i];
            labels[i] = String.format("Кластер %d", clusterId);
        }

        ScatterPlot plot = ScatterPlot.of(features2D, labels, 'o');
        Canvas canvas = plot.canvas();
        canvas.setTitle("Кластеризация обращений техподдержки");
        canvas.setAxisLabels("Компонент 1", "Компонент 2");

        return convertCanvasToByteArray(canvas, 800, 600);
    }

    private byte[] convertCanvasToByteArray(Canvas canvas, int width, int height) throws IOException {
        BufferedImage image = canvas.toBufferedImage(width, height);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }
}
