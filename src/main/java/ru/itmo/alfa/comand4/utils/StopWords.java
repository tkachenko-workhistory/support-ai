package ru.itmo.alfa.comand4.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Component
public class StopWords {

    Set<String> words = new HashSet<>();

    public StopWords(VectorizeText vectorizer) throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("morfology/stopwords.txt");
        InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        for (String line; (line = reader.readLine()) != null;) {
            if (line.isBlank()) {
                continue;
            }
            var result = vectorizer.preprocessText(line);
            // Словарь стоп слов содержит по одному слову в каждой строке, поэтому берём только индекс 0
            if (result.size() == 1) {
                words.add(result.get(0));
            }
        }
    }

    public boolean contains(String word) {
        return words.contains(word);
    }
}
