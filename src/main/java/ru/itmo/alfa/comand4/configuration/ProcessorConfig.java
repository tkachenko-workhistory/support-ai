package ru.itmo.alfa.comand4.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itmo.alfa.comand4.model.entity.SupportTicket;
import ru.itmo.alfa.comand4.model.serial.ModelData;
import ru.itmo.alfa.comand4.repository.CsvTicketSource;
import ru.itmo.alfa.comand4.service.ClusterProfiler;
import ru.itmo.alfa.comand4.service.TicketProcessor;
import ru.itmo.alfa.comand4.utils.ClusterCounting;
import ru.itmo.alfa.comand4.utils.VectorizeText;
import smile.clustering.KMeans;

import java.util.List;

@Configuration
public class ProcessorConfig {

    @Bean
    public ModelData getModelData(@Value("${datasource.csv.filepath}") String filePath) {
        // Загрузка данных
        List<SupportTicket> tickets = new CsvTicketSource(filePath).getAllTicket();

        // Подготавливка данных
        List<String> documents = tickets.stream()
                .map(t -> t.getCustomerIssue())
                .toList();

        // Создать словарь
        List<String> vocabulary = VectorizeText.getVocabulary(documents);
        // Векторизация
        double[][] features = VectorizeText.vectorize(documents, vocabulary);

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
