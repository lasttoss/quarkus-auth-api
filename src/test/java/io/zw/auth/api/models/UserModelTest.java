package io.zw.auth.api.models;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.zw.auth.api.constants.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
    import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A user row is the one place a password can leak from, so what the constructor writes into it
 * is worth pinning down: a hash, never the password itself.
 */
class UserModelTest {

    @Test
    void theConstructorStoresAHashAndNotThePassword() {
        UserModel user = new UserModel("dung", "correct horse battery staple");

        assertNotEquals("correct horse battery staple", user.getPassword(),
                "a database dump must not contain readable passwords");
        assertTrue(user.getPassword().startsWith("$2"),
                "bcrypt hashes are recognisable by their prefix, got " + user.getPassword());
    }

    @Test
    void theStoredHashStillVerifiesThePassword() {
        UserModel user = new UserModel("dung", "correct horse battery staple");

        assertTrue(BcryptUtil.matches("correct horse battery staple", user.getPassword()));
    }

    @Test
    void theWrongPasswordDoesNotVerify() {
        UserModel user = new UserModel("dung", "correct horse battery staple");

        assertFalse(BcryptUtil.matches("correct horse battery stapl", user.getPassword()));
    }

    @Test
    void twoUsersWithTheSamePasswordGetDifferentHashes() {
        UserModel one = new UserModel("dung", "same password");
        UserModel other = new UserModel("someone else", "same password");

        assertNotEquals(one.getPassword(), other.getPassword(),
                "without a per-user salt one cracked hash cracks every account with that password");
    }

    @Test
    void everyUserGetsItsOwnIdentity() {
        UserModel one = new UserModel("dung", "pw");
        UserModel other = new UserModel("dung", "pw");

        assertNotNull(one.getClass());
        assertTrue(one.getUserId().matches("[0-9a-f-]{36}"), "userId is a uuid, got " + one.getUserId());
        assertTrue(one.getSocialId().matches("[0-9a-f-]{36}"));
        assertNotEquals(one.getUserId(), other.getUserId(), "two accounts must not collide");
    }

    @Test
    void theTimestampsAreFilledInBecauseTheColumnsDoNotAcceptNull() {
        UserModel user = new UserModel("dung", "pw");

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertNotNull(user.getLastLoginTime());
    }

    @Test
    void aNewUserIsAnAuthLogin() {
        UserModel user = new UserModel("dung", "pw");

        assertEquals(Constants.SocialTypeLogin.AUTH_LOGIN.getValue(), user.getType());
    }
}
