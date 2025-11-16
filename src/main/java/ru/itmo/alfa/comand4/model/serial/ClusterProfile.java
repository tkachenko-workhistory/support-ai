package ru.itmo.alfa.comand4.model.serial;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ClusterProfile {
    private String categoryName;
    private List<String> keywords;
    private List<String> recommendedSolutions;
    private int expectedResolutionTime;
}