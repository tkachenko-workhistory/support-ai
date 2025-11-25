package ru.itmo.alfa.comand4.domain.clusterinfo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "Информация о словаре и признаках")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VocabularyInfo {

    @Schema(
            description = "Общий размер словаря",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int vocabularySize;

    @Schema(
            description = "Самые частые слова в корпусе (топ-20). Показывают наиболее значимые термины в данных."
    )
    private List<String> topWords;

    @Schema(
            description = "Наименее частые слова в корпусе (последние 20)."
    )
    private List<String> bottomWords;
}
