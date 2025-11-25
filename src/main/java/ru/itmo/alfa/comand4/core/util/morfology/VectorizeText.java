package ru.itmo.alfa.comand4.core.util.morfology;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@AllArgsConstructor
public class VectorizeText {

    private final Vocabulary vocabularyService;

    /**
     * Векторизация текста
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
        List<String> words = vocabularyService.preprocessText(text);

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

}
