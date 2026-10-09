package io.zw.auth.api.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zw.auth.api.constants.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The access token is the only thing the game API trusts, so these tests are about the contract
 * between the two services: the claims the game API reads, and the signature it checks with the
 * public half of the key pair configured here.
 *
 * SmallRye JWT signs it, with no Quarkus running, so the signing key comes from a system
 * property pointing at a PEM file this test writes to a temporary directory. Nothing here
 * touches a database, a network or the production keys.
 */
class JwtUtilsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static KeyPair signingKeys;

    @BeforeAll
    static void giveSmallRyeASigningKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        signingKeys = generator.generateKeyPair();

        Path pem = Files.createTempDirectory("jwt-signing-key").resolve("private.pem");
        Files.writeString(pem, pemBlock(signingKeys.getPrivate().getEncoded(), "PRIVATE KEY"));
        System.setProperty("smallrye.jwt.sign.key.location", pem.toString());
    }

    @Test
    void theTokenCarriesTheUserAndTheExpiryTheCallerChose() throws Exception {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + 300;

        JsonNode claims = claimsOf(JwtUtils.generateToken("user-42", now, expiresAt));

        assertEquals("user-42", claims.get("sub").asText(), "the game API identifies the player by this");
        assertEquals(1, claims.get("groups").size(), "exactly one group: the interceptor allows USER");
        assertEquals("USER", claims.get("groups").get(0).asText());
        assertEquals(now, claims.get("iat").asLong());
        assertEquals(expiresAt, claims.get("exp").asLong(), "the lifetime is the caller's, not the signer's");
    }

    @Test
    void theSignatureVerifiesWithThePublicKeyTheGameApiHolds() throws Exception {
        String token = JwtUtils.generateToken("user-42", 1_700_000_000L, 1_700_000_300L);

        assertEquals("RS256", headerOf(token).get("alg").asText());
        assertTrue(verifies(token, signingKeys.getPublic()),
                "the game API verifies every request with this public key");
    }

    @Test
    void anotherKeyPairCannotForgeATokenThatVerifies() throws Exception {
        String token = JwtUtils.generateToken("user-42", 1_700_000_000L, 1_700_000_300L);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        assertFalse(verifies(token, generator.generateKeyPair().getPublic()),
                "a token signed by someone else must not pass verification");
    }

    @Test
    void twoUsersNeverShareAToken() {
        String one = JwtUtils.generateToken("user-1", 1_700_000_000L, 1_700_000_300L);
        String other = JwtUtils.generateToken("user-2", 1_700_000_000L, 1_700_000_300L);

        assertNotEquals(one, other);
    }

    @Test
    void theAccessTokenLifetimeStaysShortEnoughToBeWorthRevoking() {
        int seconds = Constants.JwtTokenEnum.ACCESS_TOKEN_EXPIRED.getValue();

        assertTrue(seconds > 0, "a token that never expires cannot be revoked");
        assertTrue(seconds <= 3_600,
                "an access token valid as long as the refresh token turns a stolen token into a "
                        + "permanent session; got " + seconds + "s");
    }

    // ---------------------------------------------------------------- helpers

    private static JsonNode claimsOf(String token) throws Exception {
        return JSON.readTree(decode(token.split("\\.")[1]));
    }

    private static JsonNode headerOf(String token) throws Exception {
        return JSON.readTree(decode(token.split("\\.")[0]));
    }

    private static String decode(String part) {
        // JWT strips base64 padding; the JDK decoder wants it back
        return new String(Base64.getUrlDecoder().decode(part + "=".repeat((4 - part.length() % 4) % 4)),
                StandardCharsets.UTF_8);
    }

    private static boolean verifies(String token, PublicKey key) throws Exception {
        String[] parts = token.split("\\.");
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(key);
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        return verifier.verify(Base64.getUrlDecoder().decode(parts[2] + "=".repeat((4 - parts[2].length() % 4) % 4)));
    }

    private static String pemBlock(byte[] der, String label) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }
}
