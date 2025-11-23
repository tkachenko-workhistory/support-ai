package ru.itmo.alfa.comand4.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter

public class ClusterDetails {
    private int clusterId;
    private String categoryName;
    private int ticketCount;
    private double percentage;
    private int avgResolutionTime;
    private List<String> topKeywords;
    private List<String> recommendedSolutions;

    public ClusterDetails(
            int clusterId,
            String categoryName,
            int ticketCount,
            double percentage,
            int avgResolutionTime
    ) {

        this.clusterId = clusterId;
        this.categoryName = categoryName;
        this.ticketCount = ticketCount;
        this.percentage = percentage;
        this.avgResolutionTime = avgResolutionTime;

        this.topKeywords = new ArrayList<>();
        this.recommendedSolutions = new ArrayList<>();
    }
}
