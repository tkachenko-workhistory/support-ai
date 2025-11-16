package ru.itmo.alfa.comand4.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimilarTicket {
    private String ticketId;
    private String issue;
    private String solution;
    private int resolutionTime;
}
