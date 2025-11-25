package ru.itmo.alfa.comand4.domain.ticked.model;

import lombok.Getter;

@Getter
public class SupportTicket {

    String conversationId;
    String customerIssue;
    String techResponse;
    String resolutionTime;
    String issueCategory;
    String issueStatus;

    public SupportTicket(String[] fields) {
        this.conversationId = fields[0];
        this.customerIssue = fields[1];
        this.techResponse = fields[2];
        this.resolutionTime = fields[3];
        this.issueCategory = fields[4];
        this.issueStatus = fields[5];
    }
}
