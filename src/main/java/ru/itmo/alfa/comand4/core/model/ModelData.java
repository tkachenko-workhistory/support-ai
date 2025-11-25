package ru.itmo.alfa.comand4.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.itmo.alfa.comand4.core.util.clustering.ClusterProfiler;
import smile.clustering.KMeans;

import java.util.List;

@AllArgsConstructor
@Getter
public class ModelData {
    public final KMeans model;
    public final List<String> vocabulary;
    public final ClusterProfiler clusterProfiler;
    public final double[][] features;
}
