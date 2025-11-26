package ru.itmo.alfa.comand4.domain.clusterinfo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.alfa.comand4.core.service.ModelDataService;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.ClusterDetails;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.ClusterInfoResponse;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.ClusterQuality;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.VocabularyInfo;
import ru.itmo.alfa.comand4.core.model.ClusterProfile;
import ru.itmo.alfa.comand4.core.util.clustering.ClusterProfiler;
import ru.itmo.alfa.comand4.domain.clusterinfo.service.ClusterQualityService;
import smile.clustering.KMeans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clusters")
@AllArgsConstructor
@Tag(name = "Cluster Analysis", description = "API для анализа результатов кластеризации обращений техподдержки")
public class ClusterInfoController {

    private final ModelDataService modelDataService;
    private final ClusterQualityService qualityService;

    @Operation(
            summary = "Полная информация о кластеризации",
            description = "Возвращает детальную информацию о всех кластерах, включая метрики качества, распределение тикетов и рекомендации по обработке."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный запрос"),
            @ApiResponse(responseCode = "503", description = "Модель не загружена")
    })
    @GetMapping("/info")
    public ResponseEntity<ClusterInfoResponse> getClusterInfo() {
        try {
            var modelData = modelDataService.getModelData();

            KMeans kmeans = modelData.getModel();
            ClusterProfiler profiler = modelData.getClusterProfiler();

            // Подсчитываем распределение по кластерам
            Map<Integer, Integer> clusterSizes = calculateClusterSizes(kmeans);
            int totalTickets = clusterSizes.values().stream().mapToInt(Integer::intValue).sum();

            // Создаем детальную информацию по кластерам
            List<ClusterDetails> clusters = new ArrayList<>();
            for (int clusterId = 0; clusterId < kmeans.k; clusterId++) {
                ClusterProfile profile = profiler.getProfile(clusterId);
                int size = clusterSizes.getOrDefault(clusterId, 0);
                double percentage = totalTickets > 0 ? (double) size / totalTickets * 100 : 0;

                ClusterDetails details = new ClusterDetails(
                        clusterId,
                        profile.getCategoryName(),
                        size,
                        Math.round(percentage * 100.0) / 100.0, // Округляем до 2 знаков
                        profile.getExpectedResolutionTime()
                );

                // Добавляем ключевые слова и решения
                details.getTopKeywords().addAll(profile.getKeywords().stream()
                        .limit(5)
                        .collect(Collectors.toList()));
                details.getRecommendedSolutions().addAll(profile.getRecommendedSolutions());

                clusters.add(details);
            }

            // Сортируем по размеру кластера (от большего к меньшему)
            clusters.sort((a, b) -> Integer.compare(b.getTicketCount(), a.getTicketCount()));

            ClusterInfoResponse response = new ClusterInfoResponse(
                    kmeans.k,
                    totalTickets,
                    clusters
            );

            // Добавляем статистику
            response.getStatistics().put("avgClusterSize",
                    Math.round((double) totalTickets / kmeans.k * 100.0) / 100.0);
            response.getStatistics().put("vocabularySize",
                    modelData.getVocabulary().size());
            response.getStatistics().put("featuresDimension",
                    modelData.getFeatures()[0].length);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Информация о конкретном кластере",
            description = "Возвращает детальную информацию о конкретном кластере, включая ключевые слова, рекомендации и метрики качества."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный запрос"),
            @ApiResponse(responseCode = "400", description = "Неверный идентификатор кластера"),
            @ApiResponse(responseCode = "503", description = "Модель не загружена")
    })
    @GetMapping("/{clusterId}")
    public ResponseEntity<ClusterDetails> getClusterDetails(@PathVariable int clusterId) {
        var modelData = modelDataService.getModelData();
        try {
            KMeans kmeans = modelData.getModel();
            if (clusterId < 0 || clusterId >= kmeans.k) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            ClusterProfiler profiler = modelData.getClusterProfiler();
            ClusterProfile profile = profiler.getProfile(clusterId);

            Map<Integer, Integer> clusterSizes = calculateClusterSizes(kmeans);
            int totalTickets = clusterSizes.values().stream().mapToInt(Integer::intValue).sum();
            int size = clusterSizes.getOrDefault(clusterId, 0);
            double percentage = totalTickets > 0 ? (double) size / totalTickets * 100 : 0;

            ClusterDetails details = new ClusterDetails(
                    clusterId,
                    profile.getCategoryName(),
                    size,
                    Math.round(percentage * 100.0) / 100.0,
                    profile.getExpectedResolutionTime()
            );

            details.getTopKeywords().addAll(profile.getKeywords());
            details.getRecommendedSolutions().addAll(profile.getRecommendedSolutions());

            return ResponseEntity.ok(details);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Информация о словаре
     */
    @Operation(
            summary = "Информация о словаре",
            description = "Возвращает информацию о словаре, используемом для векторизации текста, включая самые частые и редкие термины."
    )
    @ApiResponse(responseCode = "200", description = "Успешный запрос")
    @GetMapping("/vocabulary")
    public ResponseEntity<VocabularyInfo> getVocabularyInfo() {
        var modelData = modelDataService.getModelData();

        try {
            List<String> vocabulary = modelData.getVocabulary();
            VocabularyInfo info = new VocabularyInfo();
            info.setVocabularySize(vocabulary.size());

            // Топ-20 самых частых слов
            info.setTopWords(vocabulary.stream()
                    .limit(20)
                    .collect(Collectors.toList()));

            // Последние 20 слов (наименее частые)
            info.setBottomWords(vocabulary.stream()
                    .skip(Math.max(0, vocabulary.size() - 20))
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Оценка качества кластеризации",
            description = """
                    Возвращает метрики качества кластеризации: 
                    * Silhouette Score
                    * WCSS
                    * BCSS
                    """)
    @GetMapping("/quality")
    public ResponseEntity<ClusterQuality> getQualityMetrics() {
        try {
            var modelData = modelDataService.getModelData();
            ClusterQuality metrics = qualityService.evaluateQuality(modelData);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Вспомогательный метод для подсчета размеров кластеров
     */
    private Map<Integer, Integer> calculateClusterSizes(KMeans kmeans) {
        Map<Integer, Integer> clusterSizes = new HashMap<>();
        for (int clusterId : kmeans.y) {
            clusterSizes.put(clusterId, clusterSizes.getOrDefault(clusterId, 0) + 1);
        }
        return clusterSizes;
    }
}
