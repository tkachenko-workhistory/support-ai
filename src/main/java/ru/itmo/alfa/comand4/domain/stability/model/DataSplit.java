package ru.itmo.alfa.comand4.domain.stability.model;

import lombok.Getter;
import ru.itmo.alfa.comand4.core.dto.SupportTicket;

import java.util.List;

@Getter
public class DataSplit {

    private final List<SupportTicket> trainTickets;
    private final List<SupportTicket> testTickets;
    private final List<Integer> originalTestClusters;

    public DataSplit(
            List<SupportTicket> trainTickets,
            List<SupportTicket> testTickets,
            List<Integer> originalTestClusters
    ) {
        this.trainTickets = trainTickets;
        this.testTickets = testTickets;
        this.originalTestClusters = originalTestClusters;
    }
}