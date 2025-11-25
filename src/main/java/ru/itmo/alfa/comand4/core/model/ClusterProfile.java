package ru.itmo.alfa.comand4.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class ClusterProfile {
    private String categoryName;
    private List<String> keywords;
    private List<String> recommendedSolutions;
    private int expectedResolutionTime;
}