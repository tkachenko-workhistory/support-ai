package ru.itmo.alfa.comand4.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class StopWordsUtil {

    Set<String> words = new HashSet<>();

    public StopWordsUtil() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("morfology/stopwords.txt");
        InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        for (String line; (line = reader.readLine()) != null;) {
            if (line.isBlank()) {
                continue;
            }
            var result = VectorizeText.preprocessText(line);
            // Словарь стоп слов содержит по одному слову в каждой строке, поэтому берём только индекс 0
            if (result.length == 1) {
                words.add(result[0]);
            }
        }
    }

    public boolean contains(String word) {
        return words.contains(word);
    }
}
