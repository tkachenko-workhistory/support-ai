package ru.itmo.alfa.comand4.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.alfa.comand4.model.dto.TicketRequest;
import ru.itmo.alfa.comand4.model.dto.TicketResponse;
import ru.itmo.alfa.comand4.service.TicketProcessor;

@RestController
@RequestMapping("/api/processing")
@AllArgsConstructor
public class TickedProcessingController {

    private final TicketProcessor ticketProcessor;

    @PostMapping("/analyze")
    public ResponseEntity<TicketResponse> analyzeTicket(
            @RequestBody TicketRequest ticketRequest
    ) {
        // ToDo: Валидация входных данных

        // Обрабатываем заявку
        TicketResponse ticketResponse = ticketProcessor.processNewTicket(ticketRequest);
        return ResponseEntity.ok(ticketResponse);
    }
}

