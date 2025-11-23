package ru.itmo.alfa.comand4.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VocabularyInfo {
    private int vocabularySize;
    private List<String> topWords;
    private List<String> bottomWords;
}
