package io.zw.auth.api.dto;

import lombok.Data;

@Data
public class AuthResponseDTO {

    private String accessToken;

    private String refreshToken;

    private long accessTokenExpiredTime;

    private long refreshTokenExpiredTime;

    public AuthResponseDTO() {}

    public AuthResponseDTO(String accessToken, String refreshToken, long accessTokenExpiredTime, long refreshTokenExpiredTime) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiredTime = accessTokenExpiredTime;
        this.refreshTokenExpiredTime = refreshTokenExpiredTime;
    }
}
