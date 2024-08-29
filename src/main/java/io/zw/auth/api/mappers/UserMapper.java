package io.zw.auth.api.mappers;

import io.zw.auth.api.dto.UserDTO;
import io.zw.auth.api.models.UserModel;
import org.mapstruct.Mapper;


@Mapper(componentModel = "cdi")
public interface UserMapper {

    UserDTO toDTO(UserModel userModel);

    UserModel toDAO(UserDTO userDTO);
}
