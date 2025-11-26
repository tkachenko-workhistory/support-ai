package ru.itmo.alfa.comand4.domain.clusterinfo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "Результаты оценки качества кластеризации")
public class ClusterQuality {

    /**
     * Что измеряет: Насколько точка похожа на свой кластер vs другие
     * Основана на: Расстояния до своего и ближайшего чужого кластера
     * <p>
     * silhouette(i) = (b(i) - a(i)) / max(a(i), b(i))
     * a(i) = среднее расстояние до точек своего кластера
     * b(i) = среднее расстояние до точек ближайшего чужого кластера
     * <p>
     * > 0.7: Отличное разделение
     * 0.5-0.7: Хорошее разделение
     * 0.25-0.5: Умеренное разделение
     * < 0.25: Слабое разделение
     * < 0: Возможно неправильное количество кластеров
     */
    @Schema(description = "Silhouette Score - мера качества кластеризации от -1 до 1")
    private double silhouetteScore;

    /**
     * Что измеряет: Компактность кластеров
     * Основана на: Расстояния до своих центроидов
     * <p>
     * Сумма квадратов расстояний от каждой точки до центроида своего кластера
     * <p>
     * Чем меньше - тем лучше
     */
    @Schema(description = "Within-Cluster Sum of Squares (WCSS)")
    private double wcss;

    /**
     * Что измеряет: Разделенность кластеров
     * Основана на: Расстояния между центроидами
     * <p>
     * Сумма квадратов расстояний от центроидов кластеров до глобального центроида,
     * взвешенная по размеру кластеров
     * <p>
     * Чем выше - тем лучше
     */
    @Schema(description = "Between-Cluster Sum of Squares (BCSS)")
    private double bcss;

    @Schema(description = "Общее количество кластеров")
    private int numberOfClusters;

    @Schema(description = "Общее количество образцов")
    private int numberOfSamples;

}
