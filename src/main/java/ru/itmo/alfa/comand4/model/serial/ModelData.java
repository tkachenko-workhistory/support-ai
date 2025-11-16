package ru.itmo.alfa.comand4.model.serial;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.itmo.alfa.comand4.service.ClusterProfiler;
import smile.clustering.KMeans;

import java.util.List;

@AllArgsConstructor
@Getter
public class ModelData {
    public final KMeans model;
    public final List<String> vocabulary;
    public final ClusterProfiler clusterProfiler;
}
