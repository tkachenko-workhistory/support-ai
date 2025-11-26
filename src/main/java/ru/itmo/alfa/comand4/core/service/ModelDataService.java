package ru.itmo.alfa.comand4.core.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.itmo.alfa.comand4.configuration.FeatureToggle;
import ru.itmo.alfa.comand4.core.util.clustering.ClusterCounting;
import ru.itmo.alfa.comand4.core.util.source.CsvReader;
import ru.itmo.alfa.comand4.core.util.morfology.Vocabulary;
import ru.itmo.alfa.comand4.core.model.ModelData;
import ru.itmo.alfa.comand4.core.util.clustering.ClusterProfiler;
import ru.itmo.alfa.comand4.core.dto.SupportTicket;
import ru.itmo.alfa.comand4.core.util.morfology.VectorizeText;
import smile.clustering.KMeans;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

@Component
public class ModelDataService {

    private final FeatureToggle feature;

    private final Vocabulary vocabularyService;
    private final VectorizeText vectorizer;
    private final ClusterProfiler clusterProfiler;

    @Value("${datasource.csv.filepath}")
    private String filePath;

    public ModelDataService(FeatureToggle feature, Vocabulary vocabularyService, VectorizeText vectorizer, ClusterProfiler clusterProfiler) {
        this.feature = feature;

        this.vocabularyService = vocabularyService;
        this.vectorizer = vectorizer;
        this.clusterProfiler = clusterProfiler;
    }

    ModelData modelData;

    @PostConstruct
    protected void init() throws FileNotFoundException {
        // Загрузка данных из файла
        List<SupportTicket> tickets = CsvReader.getAllTicket(new FileReader(filePath));

        // Кластеризуемый текст
        List<String> documents = tickets.stream()
                .map(t -> t.getCustomerIssue())
                .toList();

        // Создание словаря
        List<String> vocabulary = vocabularyService.getVocabulary(documents);

        // Векторизация
        double[][] features = vectorizer.vectorize(documents, vocabulary);

        // Задаём количество кластеров
        int optimalK = 0;
        if (feature.getClustering().getCount() > 0)
            optimalK = feature.getClustering().getCount();
        else
            optimalK = ClusterCounting.findOptimalK(features);
        // Кластеризация
        KMeans model = KMeans.fit(features, optimalK);

        // Создание Базы Знаний о кластерах
        int[] clusterAssignments = model.y; // Получаем назначения кластеров
        clusterProfiler.buildFromTickets(tickets, clusterAssignments);

        this.modelData = new ModelData(model, vocabulary, clusterProfiler, features);
    }

    public ModelData getModelData() {
        return modelData;
    }
}
