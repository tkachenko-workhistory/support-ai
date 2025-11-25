package ru.itmo.alfa.comand4.domain.ticked.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.core.service.ModelDataService;
import ru.itmo.alfa.comand4.domain.ticked.model.TicketRequest;
import ru.itmo.alfa.comand4.domain.ticked.model.TicketResponse;

import ru.itmo.alfa.comand4.core.model.ClusterProfile;
import ru.itmo.alfa.comand4.core.model.ModelData;
import ru.itmo.alfa.comand4.core.util.clustering.ClusterProfiler;
import ru.itmo.alfa.comand4.core.util.morfology.VectorizeText;

import smile.clustering.KMeans;
import java.util.List;

/**
 * Класс для обработки новых заявок
 */
@Service
public class TicketProcessor {

    private List<String> vocabulary;
    private KMeans model;
    private ClusterProfiler clusterProfiler;

    private final VectorizeText vectorizer;

    private final ModelDataService modelDataService;

    public TicketProcessor(VectorizeText vectorizer, ModelDataService modelDataService) {
        this.vectorizer = vectorizer;
        this.modelDataService = modelDataService;
    }

    @PostConstruct
    protected void init() {
        ModelData modelData = modelDataService.getModelData();

        vocabulary = modelData.getVocabulary();
        model =  modelData.getModel();
        clusterProfiler = modelData.getClusterProfiler();
    }

    public TicketResponse processNewTicket(TicketRequest request) {
        // Векторизуем новую заявку
        double[] features = vectorizer.vectorize(request.getDescription(), vocabulary);

        // Предсказываем кластер
        int clusterId = model.predict(features);

        // Получаем информацию о кластере
        ClusterProfile profile = clusterProfiler.getProfile(clusterId);

        // Рассчитываем уверенность
        double confidence = calculateConfidence(features, clusterId);

        return new TicketResponse(
                clusterId,
                profile.getCategoryName(),
                profile.getRecommendedSolutions(),
                profile.getExpectedResolutionTime(),
                confidence
        );
    }


    private double calculateConfidence(double[] features, int clusterId) {
        // Метрика уверенности: расстояние до центра кластера
        double[] centroid = model.centroids[clusterId];
        double distance = 0.0;

        for (int i = 0; i < features.length; i++) {
            distance += Math.pow(features[i] - centroid[i], 2);
        }

        return Math.max(0, 1 - Math.sqrt(distance));
    }

}
