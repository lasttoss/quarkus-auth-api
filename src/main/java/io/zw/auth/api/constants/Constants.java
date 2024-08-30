package io.zw.auth.api.constants;

import lombok.Getter;

public class Constants {

    @Getter
    public enum JwtTokenEnum{
        ACCESS_TOKEN_EXPIRED(300), REFRESH_TOKEN_EXPIRED(777600);

        private int value;

        JwtTokenEnum(int value) {
            this.value = value;
        }
    }

    @Getter
    public enum SocialTypeLogin {
        AUTH_LOGIN(0), FACEBOOK_LOGIN(1), GOOGLE_LOGIN(2), APPLE_LOGIN(3), DEVICE_LOGIN(4);

        private int value;

        SocialTypeLogin(int value) {
            this.value = value;
        }
    }

    @Getter
    public enum EventLoggerEnum {
        REGISTER_EVENT("REGISTER_EVENT");

        private String value;
        EventLoggerEnum(String value) {
            this.value = value;
        }
    }
}
