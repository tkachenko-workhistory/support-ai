package ru.itmo.alfa.comand4.domain.ticked.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TicketResponse {
    private int clusterId;
    private String category;
    private List<String> recommendedSolutions;
    private int expectedResolutionTime;
    private double confidence;
}
