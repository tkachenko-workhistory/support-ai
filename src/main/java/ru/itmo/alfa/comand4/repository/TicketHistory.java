package ru.itmo.alfa.comand4.repository;

import ru.itmo.alfa.comand4.model.entity.SupportTicket;

import java.util.List;

public interface TicketHistory {

    List<SupportTicket>  getAllTicket();
}
