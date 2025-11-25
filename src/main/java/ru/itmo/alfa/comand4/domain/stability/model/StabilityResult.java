package ru.itmo.alfa.comand4.domain.stability.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public class StabilityResult {
    private final int totalTestSamples;
    private final int correctAssignments;
    private final int wrongAssignments;
    private final double accuracy;
    private final Map<Integer, ClusterMapping> clusterMapping;

    public Map<String, Object> toHistogramData() {
        Map<String, Object> histogram = new HashMap<>();
        histogram.put("totalTestSamples", totalTestSamples);
        histogram.put("correctAssignments", correctAssignments);
        histogram.put("wrongAssignments", wrongAssignments);
        histogram.put("accuracy", Math.round(accuracy * 100.0) / 100.0);

        // Дополнительная статистика по кластерам
        Map<String, Object> clusterStats = new HashMap<>();
        for (ClusterMapping mapping : clusterMapping.values()) {
            clusterStats.put("Cluster_" + mapping.getOriginalCluster(),
                    mapping.getAssignmentDistribution());
        }
        histogram.put("clusterDistribution", clusterStats);

        return histogram;
    }
}