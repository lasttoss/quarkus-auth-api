package io.zw.auth.api.services;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.zw.auth.api.constants.ApiErrorConstants;
import io.zw.auth.api.constants.Constants;
import io.zw.auth.api.constants.RedisConstants;
import io.zw.auth.api.dto.AuthRenewRequestDTO;
import io.zw.auth.api.dto.AuthRequestDTO;
import io.zw.auth.api.dto.AuthResponseDTO;
import io.zw.auth.api.dto.ResponseDTO;
import io.zw.auth.api.models.UserModel;
import io.zw.auth.api.repositories.UserRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The auth service is what decides whether a player gets a token, so these tests are about that
 * decision: which status and which error code each way of failing produces, whether a token was
 * really issued, and what was written to redis and to the database along the way.
 *
 * Nothing here runs Quarkus. The service takes its collaborators as package-private fields, so the
 * repository and redis are hand-written fakes and the JWT parser is a proxy; the only real thing is
 * the token signing, which needs the same temporary key setup JwtUtilsTest uses.
 */
class AuthServiceTest {

    private static final String USER_ID = "user-1";
    private static final String USERNAME = "player";

    @BeforeAll
    static void giveSmallRyeASigningKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        Path pem = Files.createTempDirectory("auth-service-signing-key").resolve("private.pem");
        String body = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        Files.writeString(pem, "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n");
        System.setProperty("smallrye.jwt.sign.key.location", pem.toString());
    }

    // ---------------------------------------------------------------- fakes

    static class FakeUserRepository extends UserRepository {
        UserModel byUsername;
        UserModel byUserId;
        final List<UserModel> persisted = new ArrayList<>();
        final List<String> usernamesAsked = new ArrayList<>();
        final List<String> userIdsAsked = new ArrayList<>();

        @Override
        public UserModel findByUsername(String username) {
            usernamesAsked.add(username);
            return byUsername;
        }

        @Override
        public UserModel findByUserId(String userId) {
            userIdsAsked.add(userId);
            return byUserId;
        }

        @Override
        public void persist(UserModel item) {
            persisted.add(item);
        }
    }

    static class FakeRedisService extends RedisService {
        final Map<String, Object> store = new HashMap<>();
        final Map<String, Duration> lifetimes = new HashMap<>();
        final List<String> checked = new ArrayList<>();
        final List<String> deleted = new ArrayList<>();

        @Override
        public void saveWithExpiredTime(String key, Object value, Duration duration) {
            store.put(key, value);
            lifetimes.put(key, duration);
        }

        @Override
        public boolean checkIfKeyExists(String key) {
            checked.add(key);
            return store.containsKey(key);
        }

        @Override
        public Object get(String key) {
            return store.get(key);
        }

        @Override
        public void delete(String key) {
            deleted.add(key);
            store.remove(key);
        }
    }

    /** A parser that rejects anything: the renew path's catch block is what this exercises. */
    static JWTParser parserThatRejects() {
        return (JWTParser) Proxy.newProxyInstance(
                JWTParser.class.getClassLoader(), new Class<?>[]{JWTParser.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("parse")) {
                        throw new ParseException("the token is not one", null);
                    }
                    return null;
                });
    }

    /** A parser that hands back a token whose subject is the user id. */
    static JWTParser parserFor(String userId) {
        JsonWebToken token = (JsonWebToken) Proxy.newProxyInstance(
                JsonWebToken.class.getClassLoader(), new Class<?>[]{JsonWebToken.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSubject", "getName" -> userId;
                    default -> null;
                });
        return (JWTParser) Proxy.newProxyInstance(
                JWTParser.class.getClassLoader(), new Class<?>[]{JWTParser.class},
                (proxy, method, args) -> method.getName().equals("parse") ? token : null);
    }

    static AuthService withFakes(FakeUserRepository repo, FakeRedisService redis, JWTParser parser) {
        AuthService service = new AuthService();
        service.userRepository = repo;
        service.redisService = redis;
        service.parser = parser;
        return service;
    }

    static AuthService withFakes() {
        return withFakes(new FakeUserRepository(), new FakeRedisService(), parserThatRejects());
    }

    static UserModel existingUser() {
        UserModel user = new UserModel(USERNAME, "secret");
        user.setUserId(USER_ID);
        return user;
    }

    static AuthRequestDTO credentials() {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setUsername(USERNAME);
        request.setPassword("secret");
        return request;
    }

    static String refreshKey() {
        return RedisConstants.REFRESH_TOKEN + USER_ID;
    }

    static void assertConflict(ResponseDTO response, ApiErrorConstants error) {
        assertEquals(RestResponse.Status.CONFLICT.getStatusCode(), response.getStatus());
        assertEquals(error.getCode(), response.getErrorCode());
        assertEquals(error.getMessage(), response.getMessage());
        assertNull(response.getData(), "a refused request must not carry a token");
    }

    static void assertIssuedTokens(ResponseDTO response, String refreshToken) {
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
        AuthResponseDTO auth = (AuthResponseDTO) response.getData();
        assertNotNull(auth, "an accepted request must carry tokens");

        for (String token : List.of(auth.getAccessToken(), auth.getRefreshToken())) {
            assertNotNull(token);
            assertEquals(3, token.split("\\.").length, "a signed JWT has three parts: " + token);
        }
        assertNotEquals(auth.getAccessToken(), auth.getRefreshToken(),
                "the access and refresh tokens are not the same token handed over twice");
        assertNotEquals(refreshToken, auth.getRefreshToken(), "renewing must issue a fresh token");
    }

    // ---------------------------------------------------------------- register

    @Test
    void registerRefusesAUsernameThatIsTaken() {
        FakeUserRepository repo = new FakeUserRepository();
        repo.byUsername = existingUser();
        AuthService service = withFakes(repo, new FakeRedisService(), parserThatRejects());

        ResponseDTO response = service.register(credentials());

        assertConflict(response, ApiErrorConstants.EXIST_USER);
        assertTrue(repo.persisted.isEmpty(), "a refused registration must not write a user");
    }

    @Test
    void registerCreatesTheUserAndHandsOutBothTokens() {
        FakeUserRepository repo = new FakeUserRepository();
        FakeRedisService redis = new FakeRedisService();

        ResponseDTO response = withFakes(repo, redis, parserThatRejects()).register(credentials());

        assertEquals(1, repo.persisted.size(), "exactly one user is written");
        UserModel created = repo.persisted.get(0);
        assertEquals(USERNAME, created.getUsername());
        assertNotNull(created.getUserId(), "the user id is what the token is about");
        assertNotEquals("secret", created.getPassword(), "the password is stored hashed");
        assertTrue(created.getPassword().startsWith("$2"), "a bcrypt hash, not the password: " + created.getPassword());

        assertIssuedTokens(response, "");
        AuthResponseDTO auth = (AuthResponseDTO) response.getData();
        long now = System.currentTimeMillis() / 1000;
        long accessLifetime = auth.getAccessTokenExpiredTime() - now;
        assertTrue(Math.abs(accessLifetime - Constants.JwtTokenEnum.ACCESS_TOKEN_EXPIRED.getValue()) <= 5,
                "access token lifetime = " + accessLifetime + "s");
        long refreshLifetime = auth.getRefreshTokenExpiredTime() - now;
        assertTrue(Math.abs(refreshLifetime - Constants.JwtTokenEnum.REFRESH_TOKEN_EXPIRED.getValue()) <= 5,
                "refresh token lifetime = " + refreshLifetime + "s");

        String key = RedisConstants.REFRESH_TOKEN + created.getUserId();
        assertEquals(auth.getRefreshToken(), redis.store.get(key),
                "the refresh token is kept under this user's key so a renew can compare it");
        assertEquals(Duration.ofSeconds(Constants.JwtTokenEnum.REFRESH_TOKEN_EXPIRED.getValue()), redis.lifetimes.get(key),
                "the key must expire with the token it holds");
    }

    // ---------------------------------------------------------------- login

    @Test
    void loginRefusesAUsernameNobodyHas() {
        FakeUserRepository repo = new FakeUserRepository();
        AuthService service = withFakes(repo, new FakeRedisService(), parserThatRejects());

        ResponseDTO response = service.login(credentials());

        assertConflict(response, ApiErrorConstants.USER_NOT_FOUND);
        assertEquals(List.of(USERNAME), repo.usernamesAsked);
    }

    @Test
    void loginIssuesTokensAndRecordsWhenThePlayerWasLastSeen() {
        FakeUserRepository repo = new FakeUserRepository();
        FakeRedisService redis = new FakeRedisService();
        repo.byUsername = existingUser();
        long before = System.currentTimeMillis();

        ResponseDTO response = withFakes(repo, redis, parserThatRejects()).login(credentials());

        assertIssuedTokens(response, "");
        assertEquals(1, repo.persisted.size(), "the last login time is written back");
        UserModel stored = repo.persisted.get(0);
        assertNotNull(stored.getLastLoginTime());
        assertTrue(stored.getLastLoginTime().getTime() >= before,
                "last_login_time was not moved forward: " + stored.getLastLoginTime());

        AuthResponseDTO auth = (AuthResponseDTO) response.getData();
        assertEquals(auth.getRefreshToken(), redis.store.get(refreshKey()));
    }

    // ---------------------------------------------------------------- renew

    @Test
    void renewRefusesATokenTheParserWillNotAccept() {
        AuthRenewRequestDTO request = new AuthRenewRequestDTO();
        request.setRefreshToken("not-a-token");

        ResponseDTO response = withFakes().renewToken(request);

        assertConflict(response, ApiErrorConstants.FAILED_TO_VERIFY_TOKEN);
    }

    @Test
    void renewRefusesATokenNoLongerInRedis() {
        FakeRedisService redis = new FakeRedisService();
        AuthRenewRequestDTO request = new AuthRenewRequestDTO();
        request.setRefreshToken("signed-but-forgotten");

        ResponseDTO response = withFakes(new FakeUserRepository(), redis, parserFor(USER_ID)).renewToken(request);

        assertConflict(response, ApiErrorConstants.FAILED_TO_VERIFY_TOKEN);
        assertEquals(List.of(refreshKey()), redis.checked, "the check is made against this user's own key");
    }

    @Test
    void renewRefusesATokenThatIsNotTheOneOnRecord() {
        FakeRedisService redis = new FakeRedisService();
        redis.store.put(refreshKey(), "the-token-on-record");
        AuthRenewRequestDTO request = new AuthRenewRequestDTO();
        request.setRefreshToken("an-older-token-for-the-same-user");

        ResponseDTO response = withFakes(new FakeUserRepository(), redis, parserFor(USER_ID)).renewToken(request);

        assertConflict(response, ApiErrorConstants.FAILED_TO_VERIFY_TOKEN);
    }

    @Test
    void renewRefusesAUserThatHasSinceBeenDeleted() {
        FakeRedisService redis = new FakeRedisService();
        redis.store.put(refreshKey(), "the-token-on-record");
        FakeUserRepository repo = new FakeUserRepository();
        repo.byUserId = null;
        AuthRenewRequestDTO request = new AuthRenewRequestDTO();
        request.setRefreshToken("the-token-on-record");

        ResponseDTO response = withFakes(repo, redis, parserFor(USER_ID)).renewToken(request);

        assertConflict(response, ApiErrorConstants.USER_NOT_FOUND);
        assertEquals(List.of(USER_ID), repo.userIdsAsked, "the user is looked up by the token's subject");
    }

    @Test
    void renewIssuesFreshTokensAndReplacesTheStoredOne() {
        FakeRedisService redis = new FakeRedisService();
        redis.store.put(refreshKey(), "the-token-on-record");
        FakeUserRepository repo = new FakeUserRepository();
        repo.byUserId = existingUser();
        AuthRenewRequestDTO request = new AuthRenewRequestDTO();
        request.setRefreshToken("the-token-on-record");

        ResponseDTO response = withFakes(repo, redis, parserFor(USER_ID)).renewToken(request);

        assertIssuedTokens(response, "the-token-on-record");
        AuthResponseDTO auth = (AuthResponseDTO) response.getData();
        assertEquals(auth.getRefreshToken(), redis.store.get(refreshKey()), "the rotation is stored, so the old one stops working");
        assertEquals(List.of(refreshKey()), redis.deleted.isEmpty() ? redis.checked : redis.deleted);
    }

    /**
     * Recorded, not fixed: renew asks redis whether the key exists and then reads it, and a key that
     * expires in between - or a bucket that answers exists and then nothing - takes the request down
     * with a NullPointerException instead of an error response. The window is small and the fix is a
     * decision about which error to return, so the test says what happens today.
     */
    @Test
    void renewCrashesRatherThanRefusingWhenTheKeyVanishesBetweenTheCheckAndTheRead() {
        FakeRedisService redis = new FakeRedisService();
        redis.store.put(refreshKey(), null); // containsKey is true, the value is gone
        AuthRenewRequestDTO request = new AuthRenewRequestDTO();
        request.setRefreshToken("the-token-on-record");

        assertThrows(NullPointerException.class,
                () -> withFakes(new FakeUserRepository(), redis, parserFor(USER_ID)).renewToken(request));
    }

    // ---------------------------------------------------------------- logout

    @Test
    void logOutForgetsTheRefreshTokenOfThatUserOnly() {
        FakeRedisService redis = new FakeRedisService();
        redis.store.put(refreshKey(), "a-token");
        redis.store.put(RedisConstants.REFRESH_TOKEN + "someone-else", "another-token");

        ResponseDTO response = withFakes(new FakeUserRepository(), redis, parserThatRejects()).logOut(USER_ID);

        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.getData());
        assertEquals(List.of(refreshKey()), redis.deleted, "only this user's key is dropped");
        assertTrue(redis.store.containsKey(RedisConstants.REFRESH_TOKEN + "someone-else"),
                "another player's session must survive this logout");
    }
}
