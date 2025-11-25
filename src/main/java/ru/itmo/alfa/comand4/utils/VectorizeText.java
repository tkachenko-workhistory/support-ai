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
        // Создаем матрицу TF: Term Frequency
        double[][] tfMatrix = getTFMatrix(documents, vocabulary);

        // Применяем TF-IDF
        return applyTFIDF(tfMatrix, documents.size());
    }

    public double[] vectorize(String text, List<String> vocabulary) {
        // Для одного документа используем TF (без IDF, т.к. нужна статистика по корпусу)
        return getTFVector(text, vocabulary);
    }

    /**
     * Получение TF матрицы
     */
    private double[][] getTFMatrix(List<String> documents, List<String> vocabulary) {
        double[][] tfMatrix = new double[documents.size()][];

        for (int i = 0; i < documents.size(); i++) {
            tfMatrix[i] = getTFVector(documents.get(i), vocabulary);
        }

        return tfMatrix;
    }

    /**
     * Получение TF вектора для одного документа
     */
    private double[] getTFVector(String text, List<String> vocabulary) {
        double[] vector = new double[vocabulary.size()];
        List<String> words = preprocessText(text);

        // Считаем raw frequency
        for (String word : words) {
            int index = vocabulary.indexOf(word);
            if (index != -1) {
                vector[index]++;
            }
        }

        // Нормализуем по длине документа
        double docLength = Arrays.stream(vector).sum();
        if (docLength > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= docLength;
            }
        }

        return vector;
    }

    /**
     * Применение TF-IDF к TF матрице
     */
    private double[][] applyTFIDF(double[][] tfMatrix, int totalDocs) {
        int numDocs = tfMatrix.length;
        int vocabSize = tfMatrix[0].length;
        double[][] tfidfMatrix = new double[numDocs][vocabSize];

        // Вычисляем IDF для каждого слова
        double[] idf = calculateIDF(tfMatrix, totalDocs);

        // Применяем TF-IDF: TF * IDF
        for (int i = 0; i < numDocs; i++) {
            for (int j = 0; j < vocabSize; j++) {
                tfidfMatrix[i][j] = tfMatrix[i][j] * idf[j];
            }
        }

        // Нормализуем векторы (L2 норма)
        normalizeVectorsL2(tfidfMatrix);

        return tfidfMatrix;
    }

    /**
     * Вычисление IDF значений
     */
    private double[] calculateIDF(double[][] tfMatrix, int totalDocs) {
        int vocabSize = tfMatrix[0].length;
        double[] idf = new double[vocabSize];

        for (int j = 0; j < vocabSize; j++) {
            int docsWithWord = 0;
            for (int i = 0; i < tfMatrix.length; i++) {
                if (tfMatrix[i][j] > 0) {
                    docsWithWord++;
                }
            }
            // Smooth IDF: log((N + 1) / (df + 1)) + 1
            idf[j] = Math.log((double) (totalDocs + 1) / (docsWithWord + 1)) + 1;
        }

        return idf;
    }

    /**
     * Нормализация векторов по L2 норме
     */
    private void normalizeVectorsL2(double[][] vectors) {
        for (double[] vector : vectors) {
            double norm = 0.0;

            for (double value : vector) {
                norm += value * value;
            }
            norm = Math.sqrt(norm);

            if (norm > 0) {
                for (int j = 0; j < vector.length; j++) {
                    vector[j] /= norm;
                }
            }
        }
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

        text = text.toLowerCase()
                .replaceAll("[^a-zа-яё\\s]", " ") // Удаляем спецсимволы
                .replaceAll("\\s+", " ")         // Убираем лишние пробелы
                .trim();
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
            String[] r = text.split("\\s+");
            result = List.of(r);
        }

        int size = feature.getMorfology().getWordlenght();
        if (size > 0) {
            result = result.stream().filter(s -> s.length() >= size).toList();
        }

        return result;
    }

}
