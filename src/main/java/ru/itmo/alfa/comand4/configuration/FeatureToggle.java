package ru.itmo.alfa.comand4.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "feature")
@Getter
@Setter
public class FeatureToggle {

    private Morfology morfology;
    private Clustering clustering;

    @Getter
    @Setter
    public static class Morfology {
        private Boolean steming;
        private Boolean stopwords;
        private Integer wordlenght;
    }

    @Getter
    @Setter
    public static class Clustering {
        private Integer count;
    }
}
