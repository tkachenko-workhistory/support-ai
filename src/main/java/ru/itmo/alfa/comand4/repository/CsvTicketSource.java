package ru.itmo.alfa.comand4.repository;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import ru.itmo.alfa.comand4.model.entity.SupportTicket;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvTicketSource implements TicketHistory {

    private final String filename;

    public CsvTicketSource(String filename) {
        this.filename = filename;
    }

    @Override
    public List<SupportTicket> getAllTicket() {
        List<SupportTicket> tickets = new ArrayList<>();
      /* try (CSVReader reader =
                     new CSVReaderBuilder(new FileReader(filename)).withSeparator('\t').build()
        ) {
            List<String[]> allData = reader.readAll();

            // Пропускаем заголовок
            for (int i = 1; i < allData.size(); i++) {
                String[] row = allData.get(i);
                if (row.length >= 7) {
                    tickets.add(new SupportTicket(row));
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }*/

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(filename))
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build()) {
            List<String[]> allData = reader.readAll();

            // Пропускаем заголовок
            for (int i = 1; i < allData.size(); i++) {
                String[] row = allData.get(i);
                if (row.length >= 8) {
                    tickets.add(new SupportTicket(row));
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }
        return tickets;
    }
}
