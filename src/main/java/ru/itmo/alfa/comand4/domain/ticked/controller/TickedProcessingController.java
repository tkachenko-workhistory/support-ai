package ru.itmo.alfa.comand4.domain.ticked.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.alfa.comand4.domain.ticked.model.TicketRequest;
import ru.itmo.alfa.comand4.domain.ticked.model.TicketResponse;
import ru.itmo.alfa.comand4.domain.ticked.service.TicketProcessor;

@RestController
@RequestMapping("/api/processing")
@AllArgsConstructor
@Tag(name = "Ticked Processing", description = "API для обработки запросов")
public class TickedProcessingController {

    private final TicketProcessor ticketProcessor;

    @PostMapping("/analyze")
    public ResponseEntity<TicketResponse> analyzeTicket(
            @RequestBody TicketRequest ticketRequest
    ) {
        // Обрабатываем заявку
        TicketResponse ticketResponse = ticketProcessor.processNewTicket(ticketRequest);
        return ResponseEntity.ok(ticketResponse);
    }
}

