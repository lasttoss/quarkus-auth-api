package io.zw.auth.api.dto;

import lombok.Data;

@Data
public class StatusDTO {

    private int errorCode;

    private String message;

    private int status;
}
