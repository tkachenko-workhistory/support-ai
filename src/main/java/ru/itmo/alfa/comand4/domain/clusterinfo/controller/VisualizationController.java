package ru.itmo.alfa.comand4.domain.clusterinfo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.alfa.comand4.core.service.ModelDataService;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.SimilarityMatrix;
import ru.itmo.alfa.comand4.domain.clusterinfo.model.VizualizationMethod;
import ru.itmo.alfa.comand4.domain.clusterinfo.service.ClusterSimilarityService;
import ru.itmo.alfa.comand4.domain.clusterinfo.service.HeatmapGenerator;
import ru.itmo.alfa.comand4.domain.clusterinfo.service.VisualizationGenerator;

import java.io.IOException;

@RestController
@RequestMapping("/api/visualization")
@AllArgsConstructor
@Tag(name = "Cluster Visualization", description = "API для визуализации кластеров")
public class VisualizationController {

    private final ModelDataService modelDataService;

    private final VisualizationGenerator visualizer;
    private final HeatmapGenerator heatmapGenerator;

    private final ClusterSimilarityService similarityService;

    @GetMapping(value = "/clustering", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getClusterPlot(
            @RequestParam(defaultValue = "UMAP") VizualizationMethod vizualizationMethod
    ) {
        try {
            byte[] imageBytes = visualizer.generateClusterPlot(modelDataService.getModelData(), vizualizationMethod);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header("Content-Disposition", "inline; filename=\"clusters.png\"")
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Тепловая карта схожести кластеров",
            description = "Возвращает PNG изображение тепловой карты схожести между кластерами")
    @GetMapping(value = "/quality", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getSimilarityHeatmap() {
        try {
            var modelData = modelDataService.getModelData();
            SimilarityMatrix matrix = similarityService.calculateClusterSimilarity(modelData);

            byte[] imageBytes = heatmapGenerator.generateSimilarityHeatmap(matrix);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header("Content-Disposition", "inline; filename=\"cluster_similarity_heatmap.png\"")
                    .body(imageBytes);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}