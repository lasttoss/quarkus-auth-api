package io.zw.auth.api.constants;

public enum ApiErrorConstants {

    EXIST_USER(1, "EXIST_USER"),
    USER_NOT_FOUND(2, "USER_NOT_FOUND"),
    FAILED_TO_VERIFY_TOKEN(3, "FAILED_TO_VERIFY_TOKEN");

    private final int code;

    private final String message;

    ApiErrorConstants(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code + ": " + message;
    }
}
