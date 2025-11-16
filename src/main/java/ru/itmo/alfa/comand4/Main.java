package ru.itmo.alfa.comand4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        /*try {
            // Загрузка данных
            List<SupportTicket> tickets = new CsvTicketSource("C:\\Users\\Иван\\Desktop\\tech_support_dataset.csv").getAllTicket();
            TicketProcessor processor = new TicketProcessor(tickets);

            // Обработка новой заявки
            TicketRequest newTicket = new TicketRequest(
                    "Cannot connect to Wi-Fi",
                    "Laptop sees networks but cannot connect to internet"
            );

            TicketResponse response = processor.processNewTicket(newTicket);

            // Вывод результатов
            System.out.println("Категория: " + response.getCategory());
            System.out.println("Рекомендации:");
            response.getRecommendedSolutions().forEach(s -> System.out.println(" • " + s));

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }*/
    }
}