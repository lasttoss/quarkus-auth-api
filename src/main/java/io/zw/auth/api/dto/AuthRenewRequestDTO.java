package io.zw.auth.api.dto;

import lombok.Data;

@Data
public class AuthRenewRequestDTO {

    private String refreshToken;
}
