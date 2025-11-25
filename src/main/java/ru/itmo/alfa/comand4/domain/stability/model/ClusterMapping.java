package ru.itmo.alfa.comand4.domain.stability.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ClusterMapping {
    private final int originalCluster;
    private final Map<Integer, Integer> assignmentCounts = new HashMap<>();

    public ClusterMapping(int originalCluster) {
        this.originalCluster = originalCluster;
    }

    public void addAssignment(int newCluster) {
        assignmentCounts.put(newCluster,
                assignmentCounts.getOrDefault(newCluster, 0) + 1);
    }

    public Map<String, Object> getAssignmentDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : assignmentCounts.entrySet()) {
            distribution.put("To_Cluster_" + entry.getKey(), entry.getValue());
        }
        return distribution;
    }
}
