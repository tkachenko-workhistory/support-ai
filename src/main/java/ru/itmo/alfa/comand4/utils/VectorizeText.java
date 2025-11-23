package ru.itmo.alfa.comand4.utils;

import lombok.AllArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.itmo.alfa.comand4.configuration.FeatureToggle;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class VectorizeText {

    // ToDo: https://haifengl.github.io/nlp.html

    private final FeatureToggle feature;

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
     * Векторизация текста
     *
     * @param documents
     * @return
     */
    public double[][] vectorize(List<String> documents, List<String> vocabulary) {
        // Создаем матрицу Term Frequency
        double[][] tfMatrix = new double[documents.size()][];

        for (int i = 0; i < documents.size(); i++) {
            String doc = documents.get(i);
            tfMatrix[i] = vectorize(doc, vocabulary);
        }

        return tfMatrix;
    }

    public double[] vectorize(String text, List<String> vocabulary) {
        double[] vector = new double[vocabulary.size()];
        List<String> words = preprocessText(text);

        // Считаем TF для нового текста
        for (String word : words) {
            int index = vocabulary.indexOf(word);
            if (index != -1) {
                vector[index]++;
            }
        }

        // Нормализуем по длине документа (TF)
        double docLength = Arrays.stream(vector).sum();
        if (docLength > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= docLength;
            }
        }

        return vector;
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

        if (feature.getMorfology().getSteming()) {
            // Морфология включена
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
            String[] r = text.toLowerCase()
                    .replaceAll("[^a-zа-яё\\s]", " ") // Удаляем спецсимволы
                    .replaceAll("\\s+", " ")         // Убираем лишние пробелы
                    .trim()
                    .split("\\s+");
            result = List.of(r);
        }

        int size = feature.getMorfology().getWordlenght();
        if (size > 0) {
            result = result.stream().filter(s -> s.length() >= size).toList();
        }

        return result;
    }

}
