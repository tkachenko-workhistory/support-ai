package ru.itmo.alfa.comand4.core.util.clustering;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.alfa.comand4.configuration.FeatureToggle;
import ru.itmo.alfa.comand4.core.util.morfology.Vocabulary;
import ru.itmo.alfa.comand4.core.model.ClusterProfile;
import ru.itmo.alfa.comand4.core.dto.SupportTicket;
import ru.itmo.alfa.comand4.core.util.morfology.StopWords;

import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class ClusterProfiler {

    private final FeatureToggle feature;

    private final Vocabulary vocabularyService;
    private final StopWords stopWords;

    private Map<Integer, ClusterProfile> clusterProfiles = new HashMap<>();

    /**
     * Построение базы знаний из исторических данных
     */
    public void buildFromTickets(List<SupportTicket> allTickets, int[] clusterAssignments) {
        clusterProfiles.clear();

        // Группируем тикеты по кластерам
        Map<Integer, List<SupportTicket>> ticketsByCluster = new HashMap<>();
        for (int i = 0; i < allTickets.size(); i++) {
            int clusterId = clusterAssignments[i];
            ticketsByCluster
                    .computeIfAbsent(clusterId, k -> new ArrayList<>())
                    .add(allTickets.get(i));
        }

        // Строим профиль для каждого кластера
        for (Map.Entry<Integer, List<SupportTicket>> entry : ticketsByCluster.entrySet()) {
            int clusterId = entry.getKey();
            List<SupportTicket> clusterTickets = entry.getValue();

            ClusterProfile profile = buildClusterProfile(clusterTickets);
            clusterProfiles.put(clusterId, profile);
        }
    }

    public ClusterProfile getProfile(int clusterId) {
        return clusterProfiles.getOrDefault(clusterId, getDefaultProfile());
    }

    private ClusterProfile buildClusterProfile(List<SupportTicket> tickets) {
        // Анализируем проблемы
        List<String> commonIssues = analyzeCommonIssues(tickets);

        // Извлекаем решения из TechResponse
        List<String> solutions = extractSolutions(tickets);

        // Среднее время решения
        int avgTime = calculateAvgResolutionTime(tickets);

        // Название категории
        String categoryName = generateCategoryName(tickets);

        return new ClusterProfile(categoryName, commonIssues, solutions, avgTime);
    }

    private List<String> analyzeCommonIssues(List<SupportTicket> tickets) {
        Map<String, Integer> wordFreq = new HashMap<>();
        for (SupportTicket ticket : tickets) {
            String text = ticket.getCustomerIssue().toLowerCase();
            var words = vocabularyService.preprocessText(text);
            for (String word : words) {
                // Применяем словарь стоп слов
                if (feature.getMorfology().getStopwords() && stopWords.contains(word))
                    continue;

                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }

        return wordFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> extractSolutions(List<SupportTicket> tickets) {
        // Анализируем поле TechResponse для извлечения решений
        Map<String, Integer> solutionFreq = new HashMap<>();

        for (SupportTicket ticket : tickets) {
            // Разбиваем ответ на отдельные рекомендации
            String[] solutions = ticket.getTechResponse().split("[,.;]\\s*");
            for (String solution : solutions) {
                String cleanSolution = solution.trim();
                if (cleanSolution.length() > 10) {
                    solutionFreq.put(cleanSolution, solutionFreq.getOrDefault(cleanSolution, 0) + 1);
                }
            }
        }

        return solutionFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int calculateAvgResolutionTime(List<SupportTicket> tickets) {
        return (int) tickets.stream()
                .mapToInt(t -> Integer.parseInt(t.getResolutionTime().split(" ")[0]))
                .average()
                .orElse(60.0);
    }

    private String generateCategoryName(List<SupportTicket> tickets) {
        Map<String, Long> categoryCount = tickets.stream()
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getIssueCategory(),
                        Collectors.counting()
                ));

        // Форматируем: "Категория1(5), Категория2(3), ..."
        return categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> String.format("%s(%d)", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private ClusterProfile getDefaultProfile() {
        return new ClusterProfile(
                "Общие проблемы",
                List.of("error", "problem", "issue"),
                Arrays.asList("Опишите проблему подробнее", "Проверьте базовые настройки"),
                60
        );
    }
}