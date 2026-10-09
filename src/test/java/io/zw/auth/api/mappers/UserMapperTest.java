package io.zw.auth.api.mappers;

import io.zw.auth.api.dto.UserDTO;
import io.zw.auth.api.models.UserModel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * What crosses the wire is decided here, so this is where a password, a hash or a session token
 * would get out if somebody added a field in a hurry.
 */
class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    /** Anything that would be a credential if it turned up in a response body. */
    private static final List<String> CREDENTIALS = List.of("password", "passwd", "hash", "secret", "salt", "token");

    @Test
    void theResponseBodyCarriesNoCredential() {
        for (Field field : UserDTO.class.getDeclaredFields()) {
            String name = field.getName().toLowerCase(Locale.ROOT);
            assertFalse(CREDENTIALS.stream().anyMatch(name::contains),
                    "UserDTO." + field.getName() + " looks like a credential and would be serialised to clients");
        }
    }

    @Test
    void thePublicFieldsSurviveTheMapping() {
        UserModel user = new UserModel("dung", "pw");
        user.setUserId("user-42");
        user.setSocialId("social-7");
        user.setDisplayName("Dung");

        UserDTO dto = mapper.toDTO(user);

        assertEquals("user-42", dto.getUserId());
        assertEquals("social-7", dto.getSocialId());
        assertEquals("Dung", dto.getDisplayName());
    }

    @Test
    void mappingBackProducesAUserThatMustNotBeInsertedAsItIs() {
        UserDTO dto = new UserDTO();
        dto.setUserId("user-42");

        UserModel back = mapper.toDAO(dto);

        // There is deliberately no password in a DTO for the mapping to copy, and the column is
        // NOT NULL, so this direction cannot be used to create an account - it exists to carry
        // an identifier around. Both facts are asserted so that neither is discovered in
        // production.
        assertNull(back.getPassword(), "a DTO has no password to map back");
        assertNull(back.getUsername(), "a DTO has no username to map back");
    }

    @Test
    void mappingNothingIsNothing() {
        assertNull(mapper.toDTO(null));
    }
}
