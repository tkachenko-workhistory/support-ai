package ru.itmo.alfa.comand4.core.util.source;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import ru.itmo.alfa.comand4.core.dto.SupportTicket;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class CsvReader {

    public static List<SupportTicket> getAllTicket(InputStreamReader stream) {
        try (CSVReader reader = new CSVReader(stream)) {
            List<String[]> allRows = reader.readAll();
            return allRows.stream()
                    .skip(1)
                    .map(SupportTicket::new)
                    .collect(Collectors.toList());
        } catch (CsvException | IOException e) {
            throw new RuntimeException("Ошибка парсинга CSV: " + e.getMessage(), e);
        }
        /*try (CSVReader reader = new CSVReaderBuilder(stream)
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build()) {
            List<String[]> allRows = reader.readAll();
            return allRows.stream()
                    .skip(1)
                    .map(SupportTicket::new)
                    .collect(Collectors.toList());
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }*/
    }

}
