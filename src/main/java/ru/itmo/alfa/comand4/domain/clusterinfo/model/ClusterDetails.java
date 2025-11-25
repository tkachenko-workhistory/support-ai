package ru.itmo.alfa.comand4.domain.clusterinfo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Детальная информация о конкретном кластере")
@Getter
@Setter
public class ClusterDetails {

    @Schema(
            description = "Идентификатор кластера (начинается с 0)",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int clusterId;

    @Schema(
            description = "Название категории",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String categoryName;

    @Schema(
            description = "Количество тикетов в кластере. Показывает размер кластера - важный показатель сбалансированности.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int ticketCount;

    @Schema(
            description = "Процент тикетов в данном кластере от общего количества. Показывает значимость кластера в общей структуре обращений.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private double percentage;

    @Schema(
            description = "Среднее время решения обращений в кластере (в минутах). Рассчитывается на основе исторических данных.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int avgResolutionTime;

    @Schema(
            description = """
                    Топ ключевых слов, характерных для проблем в данном кластере. 
                    Показывают семантическое ядро кластера.
                    """
    )
    private List<String> topKeywords;

    @Schema(
            description = """
                    Рекомендуемые решения для проблем кластера.
                    Извлекаются из  исторических обращений.
                    """
    )
    private List<String> recommendedSolutions;

    public ClusterDetails(
            int clusterId,
            String categoryName,
            int ticketCount,
            double percentage,
            int avgResolutionTime
    ) {

        this.clusterId = clusterId;
        this.categoryName = categoryName;
        this.ticketCount = ticketCount;
        this.percentage = percentage;
        this.avgResolutionTime = avgResolutionTime;

        this.topKeywords = new ArrayList<>();
        this.recommendedSolutions = new ArrayList<>();
    }
}
