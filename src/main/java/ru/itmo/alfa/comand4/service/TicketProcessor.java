package ru.itmo.alfa.comand4.service;

import org.springframework.stereotype.Service;
import ru.itmo.alfa.comand4.model.dto.SimilarTicket;
import ru.itmo.alfa.comand4.model.dto.TicketRequest;
import ru.itmo.alfa.comand4.model.dto.TicketResponse;

import ru.itmo.alfa.comand4.model.serial.ClusterProfile;
import ru.itmo.alfa.comand4.model.serial.ModelData;
import ru.itmo.alfa.comand4.utils.VectorizeText;

import smile.clustering.KMeans;

import java.util.Arrays;
import java.util.List;

/**
 * Класс для обработки новых заявок
 */
@Service
public class TicketProcessor {

    private final List<String> vocabulary;
    private final KMeans model;
    private final ClusterProfiler clusterProfiler;

    public TicketProcessor(ModelData modelData) {
        this.vocabulary = modelData.getVocabulary();
        this.model = modelData.getModel();
        this.clusterProfiler = modelData.clusterProfiler;
    }

    public TicketResponse processNewTicket(TicketRequest request) {
        // Векторизуем новую заявку
        double[] features = VectorizeText.vectorize(request.getDescription(), vocabulary);

        // Предсказываем кластер
        int clusterId = model.predict(features);

        // Получаем информацию о кластере
        ClusterProfile profile = clusterProfiler.getProfile(clusterId);

        // Рассчитываем уверенность
        double confidence = calculateConfidence(features, clusterId);

        // Находим похожие решенные заявки
        List<SimilarTicket> similarTickets = findSimilarTickets(request, clusterId);

        return new TicketResponse(
                clusterId,
                profile.getCategoryName(),
                profile.getRecommendedSolutions(),
                profile.getExpectedResolutionTime(),
                confidence,
                similarTickets
        );
    }


    private double calculateConfidence(double[] features, int clusterId) {
        // Простая метрика уверенности - расстояние до центра кластера
        double[] centroid = model.centroids[clusterId];
        double distance = 0.0;

        for (int i = 0; i < features.length; i++) {
            distance += Math.pow(features[i] - centroid[i], 2);
        }

        return Math.max(0, 1 - Math.sqrt(distance));
    }

    private List<SimilarTicket> findSimilarTickets(TicketRequest request, int clusterId) {
        // ToDo: Здесь можно добавить поиск по историческим данным
        // Пока возвращаем заглушку
        return Arrays.asList(
                new SimilarTicket("CONV-0003", "Cannot connect to Wi-Fi", "Clear cache and remove unnecessary programs", 50),
                new SimilarTicket("CONV-0022", "Wi-Fi connection issues", "Verify your email settings", 15)
        );
    }
}
