package com.lodge.model;

import com.lodge.util.JsonAdapters;

import java.time.LocalDateTime;

/** One row in the system audit trail (who did what, and when). */
public class AuditEntry {

    private LocalDateTime timestamp;
    private String username;
    private String action;
    private String detail;

    public AuditEntry() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditEntry(String username, String action, String detail) {
        this();
        this.username = username;
        this.action = action;
        this.detail = detail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getFormattedTimestamp() {
        return timestamp == null ? "" : timestamp.format(JsonAdapters.DATE_TIME);
    }
}
