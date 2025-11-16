package ru.itmo.alfa.comand4.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TicketRequest {
    private String customerIssue;
    private String description;
}