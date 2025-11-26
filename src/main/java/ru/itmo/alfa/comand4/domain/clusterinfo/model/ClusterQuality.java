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

    /*
    * > 0.7: Отличное разделение
    * 0.5-0.7: Хорошее разделение
    * 0.25-0.5: Умеренное разделение
    * < 0.25: Слабое разделение
    * < 0: Возможно неправильное количество кластеров
    */
    @Schema(description = "Silhouette Score - мера качества кластеризации от -1 до 1")
    private double silhouetteScore;

    /*
    * Calinski-Harabasz: чем выше, тем лучше
    * Высокие значения указывают на плотные и хорошо разделенные кластеры
    */
    @Schema(description = "Calinski-Harabasz Index - отношение межкластерной дисперсии к внутрикластерной")
    private double calinskiHarabasz;

    /*
    * < 0.5: Отличное разделение
    * 0.5-1.0: Хорошее разделение
    * > 1.0: Слабое разделение
    */
    @Schema(description = "Davies-Bouldin Index - мера схожести кластеров (меньше = лучше)")
    private double daviesBouldin;

    /**
     * Сумма квадратов расстояний от каждой точки до центроида своего кластера
     */
    @Schema(description = "Within-Cluster Sum of Squares (WCSS)")
    private double wcss;

    /**
     * Сумма квадратов расстояний от центроидов кластеров до глобального центроида, взвешенная по размеру кластеров
     */
    @Schema(description = "Between-Cluster Sum of Squares (BCSS)")
    private double bcss;

    @Schema(description = "Общее количество кластеров")
    private int numberOfClusters;

    @Schema(description = "Общее количество образцов")
    private int numberOfSamples;

}
