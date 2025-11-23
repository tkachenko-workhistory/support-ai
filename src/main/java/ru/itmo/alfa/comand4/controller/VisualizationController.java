package ru.itmo.alfa.comand4.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.alfa.comand4.model.serial.ModelData;
import ru.itmo.alfa.comand4.service.KMeansVisualizer;

import java.io.IOException;

@RestController
@RequestMapping("/api/visualization")
@AllArgsConstructor
@Tag(name = "Visualization", description = "API для визуализации кластеров")
public class VisualizationController {

    private final ModelData modelData;
    private final KMeansVisualizer visualizer;

    @GetMapping(value = "/clusters", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getClusterPlot() {
        try {
            byte[] imageBytes = visualizer.generateClusterPlot(modelData);
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

}
