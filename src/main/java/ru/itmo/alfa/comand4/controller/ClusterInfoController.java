package ru.itmo.alfa.comand4.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.alfa.comand4.model.dto.ClusterDetails;
import ru.itmo.alfa.comand4.model.dto.ClusterInfoResponse;
import ru.itmo.alfa.comand4.model.dto.VocabularyInfo;
import ru.itmo.alfa.comand4.model.serial.ClusterProfile;
import ru.itmo.alfa.comand4.model.serial.ModelData;
import ru.itmo.alfa.comand4.service.ClusterProfiler;
import smile.clustering.KMeans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clusters")
@AllArgsConstructor
public class ClusterInfoController {
    private final ModelData modelData;

    @GetMapping("/info")
    public ResponseEntity<ClusterInfoResponse> getClusterInfo() {
        if (modelData == null || modelData.getModel() == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try {
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
                    Math.round(kmeans.distortion * 100.0) / 100.0,
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

    /**
     * Информация о конкретном кластере
     */
    @GetMapping("/{clusterId}")
    public ResponseEntity<ClusterDetails> getClusterDetails(@PathVariable int clusterId) {
        if (modelData == null || modelData.getModel() == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

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
    @GetMapping("/vocabulary")
    public ResponseEntity<VocabularyInfo> getVocabularyInfo() {
        if (modelData == null || modelData.getVocabulary() == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

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

    /**
     * Статистика качества кластеризации
     */
    @GetMapping("/quality")
    public ResponseEntity<Map<String, Object>> getQualityMetrics() {
        if (modelData == null || modelData.getModel() == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try {
            KMeans kmeans = modelData.getModel();
            Map<Integer, Integer> clusterSizes = calculateClusterSizes(kmeans);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalClusters", kmeans.k);
            metrics.put("totalSamples", kmeans.y.length);
            metrics.put("wcss", Math.round(kmeans.distortion * 100.0) / 100.0);
            metrics.put("avgClusterSize", Math.round((double) kmeans.y.length / kmeans.k * 100.0) / 100.0);

            // Распределение по кластерам
            metrics.put("clusterDistribution", clusterSizes);

            // Коэффициент вариации размера кластеров
            double avgSize = (double) kmeans.y.length / kmeans.k;
            double variance = clusterSizes.values().stream()
                    .mapToDouble(size -> Math.pow(size - avgSize, 2))
                    .average()
                    .orElse(0);
            double cv = Math.sqrt(variance) / avgSize;
            metrics.put("sizeVariation", Math.round(cv * 100.0) / 100.0);

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
