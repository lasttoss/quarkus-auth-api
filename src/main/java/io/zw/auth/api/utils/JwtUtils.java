package io.zw.auth.api.utils;

import io.smallrye.jwt.build.Jwt;

import java.util.Arrays;
import java.util.HashSet;

public class JwtUtils {
    public static String generateToken(String userId, long issuedAt, long expired) {
        String token = Jwt.subject(userId)
                .issuedAt(issuedAt)
                .expiresAt(expired)
                .groups(new HashSet<>(Arrays.asList("USER")))
                .sign();
        return token;
    }
}