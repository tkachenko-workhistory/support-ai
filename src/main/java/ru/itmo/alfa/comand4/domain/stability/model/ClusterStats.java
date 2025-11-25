package ru.itmo.alfa.comand4.domain.stability.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClusterStats {
    private final int clusterId;
    private final int correct;
    private final int wrong;
    private final int total;
}
