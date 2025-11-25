package ru.itmo.alfa.comand4.core.util.morfology;

import lombok.AllArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Component;
import ru.itmo.alfa.comand4.configuration.FeatureToggle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class Vocabulary {

    private final FeatureToggle feature;

    private final StopWords stopWords;

    public List<String> getVocabulary(List<String> documents) {
        // Создаем словарь слов
        Map<String, Integer> wordIndex = new HashMap<>();
        List<String> vocabulary = new ArrayList<>();

        // Сначала собираем все уникальные слова из всех документов
        for (String doc : documents) {
            List<String> words = preprocessText(doc);

            for (String word : words) {
                if (!wordIndex.containsKey(word)) {
                    wordIndex.put(word, vocabulary.size());
                    vocabulary.add(word);
                }
            }
        }

        // Ограничиваем размер словаря (берем самые частые слова)
        if (vocabulary.size() > 1000) {
            vocabulary = getTopFrequentWords(documents, 1000);
            wordIndex.clear();
            for (int i = 0; i < vocabulary.size(); i++) {
                wordIndex.put(vocabulary.get(i), i);
            }
        }

        return vocabulary;
    }

    /**
     * Получить самые частые слова
     *
     * @param documents
     * @param maxWords
     * @return
     */
    private List<String> getTopFrequentWords(List<String> documents, int maxWords) {
        Map<String, Integer> wordFreq = new HashMap<>();

        // Считаем частоту всех слов
        for (String doc : documents) {
            List<String> words = preprocessText(doc);
            for (String word : words) {
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }

        // Сортируем по убыванию частоты и берем топ
        return wordFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxWords)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }


    public List<String> preprocessText(String text) {
        List<String> result;

        // Приведение к нижнему регистру, удаление лишних символов
        text = text.toLowerCase()
                .replaceAll("[^a-zа-яё0-9\\s]", " ") // Удаляем спецсимволы
                .replaceAll("\\s+", " ") // Убираем лишние пробелы
                .trim();

        // Применение стеминга, если требуется
        if (feature.getMorfology().getSteming()) {
            result = new ArrayList<>();
            try (Analyzer analyzer = new RussianAnalyzer()) {
                try (TokenStream tokenStream = analyzer.tokenStream("content", text)) {
                    CharTermAttribute attribute = tokenStream.addAttribute(CharTermAttribute.class);
                    tokenStream.reset();

                    while (tokenStream.incrementToken()) {
                        String term = attribute.toString();
                        result.add(term);
                    }

                    tokenStream.end();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            // Морфология отключена
            String[] r = text.split("\\s+");
            result = List.of(r);
        }

        // Применение ограничения на количество символов
        int size = feature.getMorfology().getWordlenght();
        if (size > 0) {
            result = result.stream().filter(s -> s.length() >= size).toList();
        }
        // Применение списка стоп-слов
        if (feature.getMorfology().getStopwords()) {
            result = result.stream().filter(v -> !stopWords.contains(v)).toList();
        }

        return result;
    }

}
