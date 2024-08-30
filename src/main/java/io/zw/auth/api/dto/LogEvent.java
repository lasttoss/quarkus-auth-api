package io.zw.auth.api.dto;

import lombok.Data;

@Data
public class LogEvent {

    private String userId;

    private String key;

    private String data;

    private String metadata;

    public LogEvent() {}

    public LogEvent(String userId, String key, String data, String metadata) {
        this.userId = userId;
        this.key = key;
        this.data = data;
        this.metadata = metadata;
    }
}
