package ru.itmo.alfa.comand4.domain.clusterinfo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.SimilarityMatrix;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class HeatmapGenerator {

    public byte[] generateSimilarityHeatmap(SimilarityMatrix similarityMatrix) throws IOException {
        int size = similarityMatrix.getSize();
        int cellSize = 80;
        int margin = 100;
        int labelArea = 50;

        int width = size * cellSize + 2 * margin + labelArea;
        int height = size * cellSize + 2 * margin + labelArea;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Настройка качества
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Белый фон
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Рисуем тепловую карту
        drawHeatmap(g, similarityMatrix, margin, labelArea, cellSize);

        // Легенда
        drawLegend(g, width, height, margin, similarityMatrix);

        // Заголовок
        drawTitle(g, width);

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }

    private void drawHeatmap(Graphics2D g, SimilarityMatrix matrix, int margin, int labelArea, int cellSize) {
        int size = matrix.getSize();
        double minVal = matrix.getMinValue();
        double maxVal = matrix.getMaxValue();

        // Рисуем ячейки тепловой карты
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double value = matrix.getMatrix()[i][j];
                Color color = getSimpleMonochromeColor(value, minVal, maxVal);

                int x = margin + labelArea + j * cellSize;
                int y = margin + labelArea + i * cellSize;

                g.setColor(color);
                g.fillRect(x, y, cellSize, cellSize);

                // Границы ячеек
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(x, y, cellSize, cellSize);

                // Значения в ячейках (только если ячейка достаточно темная)
                if (value > 0.3) { // Показываем текст только на светлых ячейках
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.WHITE);
                }

                g.setFont(new Font("Arial", Font.BOLD, 10));
                String text = String.format("%.2f", value);
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g.drawString(text, x + (cellSize - textWidth) / 2, y + (cellSize + textHeight) / 2 - 2);
            }
        }

        // Подписи кластеров
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));

        for (int i = 0; i < size; i++) {
            // Вертикальные подписи (слева)
            int y = margin + labelArea + i * cellSize + cellSize / 2;
            g.drawString(matrix.getLabels()[i], margin + 10, y + 5);

            // Горизонтальные подписи (сверху)
            int x = margin + labelArea + i * cellSize + cellSize / 2;
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(matrix.getLabels()[i]);
            g.drawString(matrix.getLabels()[i], x - textWidth / 2, margin + labelArea - 10);
        }

        // Заголовки осей
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Кластеры", margin + labelArea + (size * cellSize) / 2 - 30, margin + 20);

        g.rotate(-Math.PI / 2);
        g.drawString("Кластеры", -margin - labelArea - (size * cellSize) / 2 - 30, margin - 10);
        g.rotate(Math.PI / 2);
    }

    private Color getSimpleMonochromeColor(double value, double minVal, double maxVal) {
        double normalized = (value - minVal) / (maxVal - minVal);

        // Линейный градиент: белый (0.0) → красный (1.0)
        int red = 255;
        int green = 255 - (int) (normalized * 255);
        int blue = 255 - (int) (normalized * 255);

        return new Color(red, Math.max(0, green), Math.max(0, blue));
    }

    private void drawLegend(Graphics2D g, int width, int height, int margin, SimilarityMatrix matrix) {
        int legendWidth = 300;
        int legendHeight = 30;
        int legendX = (width - legendWidth) / 2;
        int legendY = height - margin + 20;

        // Рисуем градиент легенды (белый → красный)
        for (int i = 0; i < legendWidth; i++) {
            double value = (double) i / legendWidth;
            Color color = getSimpleMonochromeColor(value, 0.0, 1.0);
            g.setColor(color);
            g.drawLine(legendX + i, legendY, legendX + i, legendY + legendHeight);
        }

        // Рамка легенды
        g.setColor(Color.BLACK);
        g.drawRect(legendX, legendY, legendWidth, legendHeight);

        // Подписи легенды
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("Несхожие", legendX - 25, legendY + legendHeight / 2 + 5);
        g.drawString("Схожие", legendX + legendWidth + 5, legendY + legendHeight / 2 + 5);

        // Значения на легенде
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        g.drawString(String.format("%.2f", matrix.getMinValue()), legendX - 15, legendY + legendHeight + 15);
        g.drawString(String.format("%.2f", matrix.getMaxValue()), legendX + legendWidth - 15, legendY + legendHeight + 15);
        g.drawString("0.5", legendX + legendWidth / 2 - 5, legendY + legendHeight + 15);

        // Заголовок легенды
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Шкала схожести", legendX + legendWidth / 2 - 40, legendY - 5);
    }

    private void drawTitle(Graphics2D g, int width) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String title = "Матрица схожести кластеров";
        FontMetrics fm = g.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g.drawString(title, (width - titleWidth) / 2, 40);

        // Подзаголовок
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        String subtitle = "Чем темнее цвет, тем выше схожесть между кластерами";
        int subtitleWidth = g.getFontMetrics().stringWidth(subtitle);
        g.drawString(subtitle, (width - subtitleWidth) / 2, 60);
    }

}
