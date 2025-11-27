package ru.itmo.alfa.comand4.core.util.morfology;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.alfa.comand4.configuration.FeatureToggle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Component
@AllArgsConstructor
public class StopWords {

    private Set<String> words = new HashSet<>();;
    private final FeatureToggle feature;

    @PostConstruct
    protected void init() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        // Два словаря со стемингом и без
        InputStream is;
        if (feature.getMorfology().getSteming())
            is = classloader.getResourceAsStream("morfology/stopwords-steming.txt");
        else
            is = classloader.getResourceAsStream("morfology/stopwords.txt");

        InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        for (String line; (line = reader.readLine()) != null;) {
            if (line.isBlank()) {
                continue;
            }

            // Применение ограничения на количество символов
            int size = feature.getMorfology().getWordlenght();
            if (line.length() >= size)
                words.add(line);
        }
    }

    public boolean contains(String word) {
        return words.contains(word);
    }

}
