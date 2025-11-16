package ru.itmo.alfa.comand4.utils;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

public class VectorizeText {

    // ToDo: https://haifengl.github.io/nlp.html

    public static List<String> getVocabulary(List<String> documents) {
        // Создаем словарь слов
        Map<String, Integer> wordIndex = new HashMap<>();
        List<String> vocabulary = new ArrayList<>();

        // Сначала собираем все уникальные слова из всех документов
        for (String doc : documents) {
            String[] words = preprocessText(doc);

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
    public static double[][] vectorize(List<String> documents, List<String> vocabulary) {
        // Создаем матрицу Term Frequency
        double[][] tfMatrix = new double[documents.size()][];

        for (int i = 0; i < documents.size(); i++) {
            String doc = documents.get(i);
            tfMatrix[i] = vectorize(doc, vocabulary);
        }

        return tfMatrix;
    }

    public static double[] vectorize(String text, List<String> vocabulary) {
        double[] vector = new double[vocabulary.size()];
        String[] words = preprocessText(text);

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
    private static List<String> getTopFrequentWords(List<String> documents, int maxWords) {
        Map<String, Integer> wordFreq = new HashMap<>();

        // Считаем частоту всех слов
        for (String doc : documents) {
            String[] words = preprocessText(doc);
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

    private static String[] preprocessText(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-zA-Z\\s]", " ") // Удаляем спецсимволы
                .replaceAll("\\s+", " ")         // Убираем лишние пробелы
                .trim()
                .split("\\s+");
    }

}
