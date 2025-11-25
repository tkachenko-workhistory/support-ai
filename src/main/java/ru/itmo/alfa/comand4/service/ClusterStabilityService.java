package ru.itmo.alfa.comand4.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.model.dto.ClusterMapping;
import ru.itmo.alfa.comand4.model.dto.ClusterStats;
import ru.itmo.alfa.comand4.model.dto.DataSplit;
import ru.itmo.alfa.comand4.model.dto.StabilityResult;
import ru.itmo.alfa.comand4.model.entity.SupportTicket;
import ru.itmo.alfa.comand4.utils.VectorizeText;
import smile.clustering.KMeans;
import smile.plot.swing.BarPlot;
import smile.plot.swing.Canvas;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClusterStabilityService {

    private final VectorizeText vectorizer;

    public StabilityResult evaluateStability(
            List<SupportTicket> allTickets,
            List<String> vocabulary,
            KMeans originalModel,
            int[] originalClusters
    ) {

        // Разделяем данные: 75% для обучения, 25% для теста (стратифицированно)
        DataSplit split = splitDataStratified(allTickets, originalClusters, 0.25);

        // Обучаем новую модель на 75% данных
        double[][] trainFeatures = vectorizer.vectorize(
                split.getTrainTickets().stream()
                        .map(SupportTicket::getCustomerIssue)
                        .toList(),
                vocabulary
        );

        KMeans newModel = KMeans.fit(trainFeatures, originalModel.k);

        // Кластеризуем тестовые данные (25%) новой моделью
        double[][] testFeatures = vectorizer.vectorize(
                split.getTestTickets().stream()
                        .map(SupportTicket::getCustomerIssue)
                        .toList(),
                vocabulary
        );

        int[] testClusters = new int[testFeatures.length];
        for (int i = 0; i < testFeatures.length; i++) {
            testClusters[i] = newModel.predict(testFeatures[i]);
        }

        // Сравниваем с исходными кластерами
        return compareClusterAssignments(
                split.getTestTickets(),
                split.getOriginalTestClusters(),
                testClusters,
                originalModel.k
        );
    }

    private DataSplit splitDataStratified(
            List<SupportTicket> tickets,
            int[] originalClusters,
            double testRatio
    ) {
        Map<Integer, List<Integer>> clusterIndices = new HashMap<>();

        // Группируем индексы по кластерам
        for (int i = 0; i < originalClusters.length; i++) {
            int clusterId = originalClusters[i];
            clusterIndices.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(i);
        }

        List<SupportTicket> trainTickets = new ArrayList<>();
        List<SupportTicket> testTickets = new ArrayList<>();
        List<Integer> originalTestClusters = new ArrayList<>();

        // Для каждого кластера выбираем 25% случайных элементов
        Random random = new Random(42); // Фиксируем seed для воспроизводимости

        for (Map.Entry<Integer, List<Integer>> entry : clusterIndices.entrySet()) {
            List<Integer> indices = entry.getValue();
            int testSize = (int) (indices.size() * testRatio);

            // Перемешиваем и выбираем тестовые элементы
            Collections.shuffle(indices, random);
            List<Integer> testIndices = indices.subList(0, testSize);
            List<Integer> trainIndices = indices.subList(testSize, indices.size());

            // Добавляем в соответствующие списки
            for (int idx : testIndices) {
                testTickets.add(tickets.get(idx));
                originalTestClusters.add(originalClusters[idx]);
            }
            for (int idx : trainIndices) {
                trainTickets.add(tickets.get(idx));
            }
        }

        return new DataSplit(trainTickets, testTickets, originalTestClusters);
    }

    private StabilityResult compareClusterAssignments(
            List<SupportTicket> testTickets,
            List<Integer> originalClusters,
            int[] newClusters,
            int totalClusters
    ) {

        int totalTestSamples = testTickets.size();
        int correctAssignments = 0;
        int wrongAssignments = 0;

        Map<Integer, ClusterMapping> clusterMapping = new HashMap<>();

        // Считаем соответствия между старыми и новыми кластерами
        for (int i = 0; i < totalTestSamples; i++) {
            int originalCluster = originalClusters.get(i);
            int newCluster = newClusters[i];

            if (originalCluster == newCluster) {
                correctAssignments++;
            } else {
                wrongAssignments++;
            }

            // Собираем статистику по маппингу кластеров
            clusterMapping
                    .computeIfAbsent(originalCluster, k -> new ClusterMapping(k))
                    .addAssignment(newCluster);
        }

        double accuracy = (double) correctAssignments / totalTestSamples * 100;

        return new StabilityResult(
                totalTestSamples,
                correctAssignments,
                wrongAssignments,
                accuracy,
                clusterMapping
        );
    }

    public byte[] generateStabilityHistogram(StabilityResult result) throws IOException {
        int width = 1200; // Увеличили ширину для трех столбцов
        int height = 700;
        int margin = 100;
        int chartWidth = width - 2 * margin;
        int chartHeight = height - 2 * margin;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Настройка сглаживания
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Белый фон
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Подготавливаем данные
        List<ClusterStats> stats = prepareClusterStats(result);

        // Рисуем гистограмму
        drawHistogram(g, stats, margin, chartWidth, chartHeight, width, height);

        // Рисуем заголовок
        drawTitle(g, result, width);

        // Рисуем легенду
        drawLegend(g, width, margin);

        // Рисуем общую статистику
        drawTotalStats(g, result, width, height, margin);

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }

    private void drawHistogram(Graphics2D g, List<ClusterStats> stats,
                               int margin, int chartWidth, int chartHeight,
                               int totalWidth, int totalHeight) {
        int groupWidth = chartWidth / stats.size();
        int barWidth = groupWidth / 4; // Ширина одного столбца (3 столбца + отступ)
        int maxValue = stats.stream().mapToInt(ClusterStats::getTotal).max().orElse(1);

        // Рисуем оси
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawLine(margin, margin, margin, margin + chartHeight); // Y ось
        g.drawLine(margin, margin + chartHeight, margin + chartWidth, margin + chartHeight); // X ось

        // Сетка на оси Y
        g.setColor(new Color(200, 200, 200));
        g.setStroke(new BasicStroke(1));
        int gridLines = 10;
        for (int i = 0; i <= gridLines; i++) {
            int y = margin + (int) ((double) i / gridLines * chartHeight);
            g.drawLine(margin, y, margin + chartWidth, y);

            // Подписи значений на оси Y
            g.setColor(Color.BLACK);
            int value = (int) ((double) (gridLines - i) / gridLines * maxValue);
            g.drawString(String.valueOf(value), margin - 30, y + 5);
        }

        // Рисуем столбцы для каждого кластера
        for (int i = 0; i < stats.size(); i++) {
            ClusterStats stat = stats.get(i);
            int groupStartX = margin + i * groupWidth;

            // Вычисляем высоты столбцов (переименовал переменные)
            int correctBarHeight = (int) ((double) stat.getCorrect() / maxValue * chartHeight);
            int wrongBarHeight = (int) ((double) stat.getWrong() / maxValue * chartHeight);
            int totalBarHeight = (int) ((double) stat.getTotal() / maxValue * chartHeight);

            int yBase = margin + chartHeight;

            // Столбец правильных назначений (зеленый)
            int x1 = groupStartX + barWidth;
            g.setColor(new Color(76, 175, 80, 200));
            g.fillRect(x1, yBase - correctBarHeight, barWidth, correctBarHeight);
            g.setColor(new Color(56, 155, 60));
            g.drawRect(x1, yBase - correctBarHeight, barWidth, correctBarHeight);

            // Столбец неправильных назначений (красный)
            int x2 = groupStartX + 2 * barWidth;
            g.setColor(new Color(244, 67, 54, 200));
            g.fillRect(x2, yBase - wrongBarHeight, barWidth, wrongBarHeight);
            g.setColor(new Color(224, 47, 34));
            g.drawRect(x2, yBase - wrongBarHeight, barWidth, wrongBarHeight);

            // Столбец общего количества (синий)
            int x3 = groupStartX + 3 * barWidth;
            g.setColor(new Color(33, 150, 243, 150));
            g.fillRect(x3, yBase - totalBarHeight, barWidth, totalBarHeight);
            g.setColor(new Color(13, 130, 223));
            g.drawRect(x3, yBase - totalBarHeight, barWidth, totalBarHeight);

            // Подпись кластера
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            String label = "Кластер " + stat.getClusterId();
            FontMetrics fm = g.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g.drawString(label, groupStartX + groupWidth/2 - labelWidth/2, yBase + 30);

            // Подписи значений над столбцами
            g.setFont(new Font("Arial", Font.PLAIN, 10));

            // Над зеленым столбцом
            if (stat.getCorrect() > 0) {
                g.setColor(Color.BLACK);
                String correctText = String.valueOf(stat.getCorrect());
                int textWidth = g.getFontMetrics().stringWidth(correctText);
                g.drawString(correctText, x1 + barWidth/2 - textWidth/2, yBase - correctBarHeight - 5);
            }

            // Над красным столбцом
            if (stat.getWrong() > 0) {
                g.setColor(Color.BLACK);
                String wrongText = String.valueOf(stat.getWrong());
                int textWidth = g.getFontMetrics().stringWidth(wrongText);
                g.drawString(wrongText, x2 + barWidth/2 - textWidth/2, yBase - wrongBarHeight - 5);
            }

            // Над синим столбцом
            if (stat.getTotal() > 0) {
                g.setColor(Color.BLACK);
                String totalText = String.valueOf(stat.getTotal());
                int textWidth = g.getFontMetrics().stringWidth(totalText);
                g.drawString(totalText, x3 + barWidth/2 - textWidth/2, yBase - totalBarHeight - 5);
            }

            // Процент точности для кластера
            double clusterAccuracy = stat.getTotal() > 0 ?
                    (double) stat.getCorrect() / stat.getTotal() * 100 : 0;
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            String accuracyText = String.format("%.1f%%", clusterAccuracy);
            int accWidth = g.getFontMetrics().stringWidth(accuracyText);
            g.drawString(accuracyText, groupStartX + groupWidth/2 - accWidth/2, yBase + 45);
        }

        // Подписи осей
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Кластеры", totalWidth/2 - 30, totalHeight - 40);

        // Повернутая подпись оси Y
        g.rotate(-Math.PI/2);
        g.drawString("Количество заявок", -totalHeight/2 - 50, 25);
        g.rotate(Math.PI/2);
    }

    private void drawLegend(Graphics2D g, int width, int margin) {
        int legendX = width - 200;
        int legendY = margin;

        g.setFont(new Font("Arial", Font.PLAIN, 12));

        // Зеленая легенда - правильные назначения
        g.setColor(new Color(76, 175, 80));
        g.fillRect(legendX, legendY, 15, 15);
        g.setColor(Color.BLACK);
        g.drawRect(legendX, legendY, 15, 15);
        g.drawString("Свой кластер", legendX + 25, legendY + 12);

        // Красная легенда - неправильные назначения
        g.setColor(new Color(244, 67, 54));
        g.fillRect(legendX, legendY + 25, 15, 15);
        g.setColor(Color.BLACK);
        g.drawRect(legendX, legendY + 25, 15, 15);
        g.drawString("Чужой кластер", legendX + 25, legendY + 37);

        // Синяя легенда - общее количество
        g.setColor(new Color(33, 150, 243));
        g.fillRect(legendX, legendY + 50, 15, 15);
        g.setColor(Color.BLACK);
        g.drawRect(legendX, legendY + 50, 15, 15);
        g.drawString("Всего в тесте", legendX + 25, legendY + 62);
    }

    private void drawTotalStats(Graphics2D g, StabilityResult result, int width, int height, int margin) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));

        String totalStats = String.format(
                "Общая статистика: %d заявок | Правильно: %d (%.1f%%) | Ошибки: %d",
                result.getTotalTestSamples(),
                result.getCorrectAssignments(),
                result.getAccuracy(),
                result.getWrongAssignments()
        );

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(totalStats);
        g.drawString(totalStats, (width - textWidth) / 2, height - 20);

        // Дополнительная информация
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        String info = "Тестирование устойчивости: 25% данных от каждого кластера";
        int infoWidth = g.getFontMetrics().stringWidth(info);
        g.drawString(info, (width - infoWidth) / 2, height - 5);
    }


    private List<ClusterStats> prepareClusterStats(StabilityResult result) {
        List<ClusterStats> stats = new ArrayList<>();
        List<Integer> clusterIds = new ArrayList<>(result.getClusterMapping().keySet());
        Collections.sort(clusterIds);

        for (int clusterId : clusterIds) {
            ClusterMapping mapping = result.getClusterMapping().get(clusterId);
            Map<Integer, Integer> assignments = mapping.getAssignmentCounts();

            int correct = assignments.getOrDefault(clusterId, 0);
            int total = assignments.values().stream().mapToInt(Integer::intValue).sum();
            int wrong = total - correct;

            stats.add(new ClusterStats(clusterId, correct, wrong, total));
        }

        return stats;
    }


    private void drawTitle(Graphics2D g, StabilityResult result, int width) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String title = "Устойчивость кластеризации - Точность: " +
                String.format("%.1f", result.getAccuracy()) + "%";
        FontMetrics fm = g.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g.drawString(title, (width - titleWidth) / 2, 40);
    }

}