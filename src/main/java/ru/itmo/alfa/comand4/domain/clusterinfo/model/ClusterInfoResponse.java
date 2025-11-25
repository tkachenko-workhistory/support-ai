package ru.itmo.alfa.comand4.domain.clusterinfo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Schema(description = "Полная информация о результатах кластеризации обращений в техподдержку")
@Getter
@Setter
public class ClusterInfoResponse {

    @Schema(
            description = "Количество кластеров (K)",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int totalClusters;

    @Schema(
            description = "Общее количество обработанных тикетов",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int totalTickets;

    @Schema(
            description = """
                    Within-Cluster Sum of Squares (WCSS) - метрика качества кластеризации.
                    Чем меньше значение, тем компактнее кластеры.
                    Используется в методе локтя для определения оптимального K.
                    """,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private double wcss;

    @Schema(
            description = """
                    Детальная информация по каждому кластеру.
                    Кластеры отсортированы по размеру (от наибольшего к наименьшему).
                    """,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<ClusterDetails> clusters;

    @Schema(
            description = "Дополнительная статистика и метрики качества кластеризации"
    )
    private Map<String, Object> statistics = new HashMap<>();

    public ClusterInfoResponse(int totalClusters, int totalTickets, double wcss, List<ClusterDetails> clusters) {
        this.totalClusters = totalClusters;
        this.totalTickets = totalTickets;
        this.wcss = wcss;
        this.clusters = clusters;
    }
}
