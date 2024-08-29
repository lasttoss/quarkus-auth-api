package io.zw.auth.api.services;

import io.zw.auth.api.constants.ApiErrorConstants;
import io.zw.auth.api.dto.ResponseDTO;
import io.zw.auth.api.dto.UserDTO;
import io.zw.auth.api.mappers.UserMapper;
import io.zw.auth.api.models.UserModel;
import io.zw.auth.api.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    UserMapper userMapper;

    public List<UserModel> getAll() {
        return userRepository.getAll();
    }

    @Transactional
    public ResponseDTO getUserInfo(String userId) {
        ResponseDTO response = new ResponseDTO();
        UserModel userModel = userRepository.findByUserId(userId);
        if (userModel == null) {
            response.setStatus(RestResponse.Status.CONFLICT.getStatusCode());
            response.setMessage(ApiErrorConstants.USER_NOT_FOUND.getMessage());
            response.setErrorCode(ApiErrorConstants.USER_NOT_FOUND.getCode());
            return response;
        }
        UserDTO userDTO = userMapper.toDTO(userModel);
        response.setStatus(RestResponse.Status.OK.getStatusCode());
        response.setData(userDTO);
        return response;
    }
}
