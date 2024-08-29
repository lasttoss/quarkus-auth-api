package io.zw.auth.api.dto;

import lombok.Data;

@Data
public class UserDTO {

    private String userId;

    private String socialId;

    private String displayName;
}
