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

    public void addAssignment(int newClusterId) {
        assignmentCounts.put(newClusterId,
                assignmentCounts.getOrDefault(newClusterId, 0) + 1);
    }

    // Новые методы для получения статистики
    public int getCorrectAssignments() {
        return assignmentCounts.getOrDefault(originalCluster, 0);
    }

    public int getWrongAssignments() {
        return getTotalAssignments() - getCorrectAssignments();
    }

    public int getTotalAssignments() {
        return assignmentCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

}
