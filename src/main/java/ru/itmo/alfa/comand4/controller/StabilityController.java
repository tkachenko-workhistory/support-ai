package ru.itmo.alfa.comand4.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.alfa.comand4.model.dto.StabilityResult;
import ru.itmo.alfa.comand4.model.entity.SupportTicket;
import ru.itmo.alfa.comand4.model.serial.ModelData;
import ru.itmo.alfa.comand4.repository.CsvReaderService;
import ru.itmo.alfa.comand4.service.ClusterStabilityService;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stability")
@RequiredArgsConstructor
@Tag(name = "Model Stability", description = "API для оценки устойчивости модели кластеризации")
public class StabilityController {

    private final ModelData modelData;
    private final ClusterStabilityService stabilityService;

    @Operation(summary = "Оценка устойчивости модели",
            description = "Оценивает устойчивость кластеризации методом повторного обучения на 75% данных")
    @PostMapping(value = "/evaluate", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> evaluateStability(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            // Получаем исходные данные
            List<SupportTicket> originalTickets  = new CsvReaderService(file).getAllTicket();

            // Получаем оригинальные назначения кластеров из модели
            int[] originalClusters = modelData.getModel().y;

            // Проверяем соответствие размеров
            if (originalTickets.size() != originalClusters.length) {
                return ResponseEntity.badRequest().body(
                        Map.of("error",
                                String.format("Размеры не совпадают: файл содержит %d записей, модель обучена на %d",
                                        originalTickets.size(), originalClusters.length))
                );
            }

            StabilityResult result =
                     stabilityService.evaluateStability(
                             originalTickets,
                             modelData.getVocabulary(),
                             modelData.getModel(),
                             originalClusters);

            return ResponseEntity.ok(result.toHistogramData());
        }
        catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Ошибка оценки устойчивости: " + e.getMessage()));
        }
    }

    @Operation(summary = "Гистограмма устойчивости модели (PNG)",
            description = "Возвращает PNG изображение с гистограммой устойчивости кластеризации")
    @PostMapping(value = "/histogram", consumes = "multipart/form-data", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getStabilityHistogram(
            @RequestParam("file") MultipartFile file) {

        try {
            //  Читаем оригинальные данные из CSV файла
            List<SupportTicket> originalTickets = new CsvReaderService(file).getAllTicket();

            // Получаем оригинальные назначения кластеров из модели
            int[] originalClusters = modelData.getModel().y;

            // Проверяем соответствие размеров
            if (originalTickets.size() != originalClusters.length) {
                return ResponseEntity.badRequest().body(
                        createErrorImage(String.format(
                                "Размеры не совпадают: файл %d записей, модель %d",
                                originalTickets.size(), originalClusters.length))
                );
            }

            // Выполняем оценку устойчивости
            StabilityResult result =
                    stabilityService.evaluateStability(
                            originalTickets,
                            modelData.getVocabulary(),
                            modelData.getModel(),
                            originalClusters
                    );

            // Генерируем PNG гистограмму
            byte[] imageBytes = stabilityService.generateStabilityHistogram(result);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header("Content-Disposition", "inline; filename=\"stability_histogram.png\"")
                    .body(imageBytes);

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorImage("Ошибка чтения файла: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(createErrorImage("Ошибка оценки устойчивости: " + e.getMessage()));
        }
    }

    // Метод для создания изображения с ошибкой
    private byte[] createErrorImage(String errorMessage) {
        try {
            BufferedImage image = new BufferedImage(800, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // Белый фон
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 800, 200);

            // Красный текст ошибки
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 16));

            // Центрируем текст
            FontMetrics metrics = g.getFontMetrics();
            int x = (800 - metrics.stringWidth(errorMessage)) / 2;
            int y = 100;

            g.drawString(errorMessage, x, y);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            return baos.toByteArray();

        } catch (IOException e) {
            return new byte[0];
        }
    }

}