package ru.itmo.alfa.comand4.repository;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.alfa.comand4.model.entity.SupportTicket;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class CsvReaderService implements TicketHistory {

    MultipartFile file;

    public CsvReaderService(MultipartFile file) {
        this.file = file;
    }

    @Override
    public List<SupportTicket> getAllTicket() {
        /*try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> allRows = reader.readAll();
            return allRows.stream()
                    .skip(1)
                    .map(SupportTicket::new)
                    .collect(Collectors.toList());
        } catch (CsvException | IOException e) {
            throw new RuntimeException("Ошибка парсинга CSV: " + e.getMessage(), e);
        }*/
        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream()))
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build()) {
            List<String[]> allRows = reader.readAll();
            return allRows.stream()
                    .skip(1)
                    .map(SupportTicket::new)
                    .collect(Collectors.toList());
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }
    }

}
