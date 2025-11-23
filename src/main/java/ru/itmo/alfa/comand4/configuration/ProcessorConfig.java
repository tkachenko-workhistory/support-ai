package ru.itmo.alfa.comand4.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import ru.itmo.alfa.comand4.model.entity.SupportTicket;
import ru.itmo.alfa.comand4.model.serial.ModelData;
import ru.itmo.alfa.comand4.repository.CsvTicketSource;
import ru.itmo.alfa.comand4.service.ClusterProfiler;
import ru.itmo.alfa.comand4.utils.ClusterCounting;
import ru.itmo.alfa.comand4.utils.StopWords;
import ru.itmo.alfa.comand4.utils.VectorizeText;
import smile.clustering.KMeans;

import java.io.IOException;
import java.util.List;

@Configuration
public class ProcessorConfig {

    @Bean
    public ModelData getModelData(
            @Value("${datasource.csv.filepath}") String filePath,
            FeatureToggle feature,
            VectorizeText vectorizer,
            StopWords stopWords
    ) {
        // Загрузка данных
        List<SupportTicket> tickets = new CsvTicketSource(filePath).getAllTicket();

        // Подготавливка данных
        List<String> documents = tickets.stream()
                .map(t -> t.getCustomerIssue())
                .toList();

        // Создать словарь
        List<String> vocabulary = vectorizer.getVocabulary(documents);

        if (feature.getMorfology().getStopwords()) {
            // Применяем словарь стоп слов
            vocabulary = vocabulary.stream().filter(v -> !stopWords.contains(v)).toList();
        }

        // Векторизация
        double[][] features = vectorizer.vectorize(documents, vocabulary);

        // Кластеризация
        int optimalK = ClusterCounting.findOptimalK(features);
        KMeans model = KMeans.fit(features, optimalK);

        // Создание Базы Знаний о кластерах
        ClusterProfiler clusterProfiler = new ClusterProfiler();
        int[] clusterAssignments = model.y; // Получаем назначения кластеров
        clusterProfiler.buildFromTickets(tickets, clusterAssignments);

        return new ModelData(model, vocabulary, clusterProfiler, features);
    }

}
