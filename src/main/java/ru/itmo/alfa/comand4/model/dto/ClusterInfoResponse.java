package ru.itmo.alfa.comand4.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ClusterInfoResponse {
    private int totalClusters;
    private int totalTickets;
    private double wcss;
    private List<ClusterDetails> clusters;
    private Map<String, Object> statistics = new HashMap<>();

    public ClusterInfoResponse(int totalClusters, int totalTickets, double wcss, List<ClusterDetails> clusters) {
        this.totalClusters = totalClusters;
        this.totalTickets = totalTickets;
        this.wcss = wcss;
        this.clusters = clusters;
    }
}
