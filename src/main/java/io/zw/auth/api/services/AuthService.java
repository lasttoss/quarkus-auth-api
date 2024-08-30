package io.zw.auth.api.services;

import com.google.gson.Gson;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.zw.auth.api.constants.ApiErrorConstants;
import io.zw.auth.api.constants.Constants;
import io.zw.auth.api.constants.RedisConstants;
import io.zw.auth.api.dto.*;
import io.zw.auth.api.logs.EventLogger;
import io.zw.auth.api.models.UserModel;
import io.zw.auth.api.repositories.UserRepository;
import io.zw.auth.api.utils.JwtUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestResponse;
import org.joda.time.DateTime;

import java.time.Duration;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    RedisService redisService;

    @Inject
    JWTParser parser;

    JwtUtils jwtUtils;

    Gson gson = new Gson();

    @Transactional
    public ResponseDTO register(AuthRequestDTO request) {
        ResponseDTO response = new ResponseDTO();
        UserModel checkUser = userRepository.findByUsername(request.getUsername());
        if (checkUser != null) {
            response.setData(null);
            response.setStatus(RestResponse.Status.CONFLICT.getStatusCode());
            response.setErrorCode(ApiErrorConstants.EXIST_USER.getCode());
            response.setMessage(ApiErrorConstants.EXIST_USER.getMessage());
            return response;
        }
        UserModel user = new UserModel(request.getUsername(), request.getPassword());
        userRepository.persist(user);
        long currentTimestamp = DateTime.now().getMillis() / 1000;
        long accessTokenExpires = currentTimestamp + Constants.JwtTokenEnum.ACCESS_TOKEN_EXPIRED.getValue();
        long refreshTokenExpires = currentTimestamp + Constants.JwtTokenEnum.REFRESH_TOKEN_EXPIRED.getValue();
        String accessToken = jwtUtils.generateToken(user.getUserId(), currentTimestamp, accessTokenExpires);
        String refreshToken = jwtUtils.generateToken(user.getUserId(), currentTimestamp, refreshTokenExpires);
        AuthResponseDTO auth = new AuthResponseDTO(accessToken, refreshToken, accessTokenExpires, refreshTokenExpires);
        response.setData(auth);
        response.setStatus(RestResponse.Status.OK.getStatusCode());
        String key = RedisConstants.REFRESH_TOKEN + user.getUserId();
        redisService.saveWithExpiredTime(key, refreshToken, Duration.ofSeconds(Constants.JwtTokenEnum.REFRESH_TOKEN_EXPIRED.getValue()));
        return response;
    }

    @Transactional
    public ResponseDTO login(AuthRequestDTO request) {
        ResponseDTO response = new ResponseDTO();
        UserModel user = userRepository.findByUsername(request.getUsername());
        if (user == null) {
            response.setData(null);
            response.setStatus(RestResponse.Status.CONFLICT.getStatusCode());
            response.setErrorCode(ApiErrorConstants.USER_NOT_FOUND.getCode());
            response.setMessage(ApiErrorConstants.USER_NOT_FOUND.getMessage());
            return response;
        }
        long currentTimestamp = DateTime.now().getMillis() / 1000;
        long accessTokenExpires = currentTimestamp + Constants.JwtTokenEnum.ACCESS_TOKEN_EXPIRED.getValue();
        long refreshTokenExpires = currentTimestamp + Constants.JwtTokenEnum.REFRESH_TOKEN_EXPIRED.getValue();
        String accessToken = JwtUtils.generateToken(user.getUserId(), currentTimestamp, accessTokenExpires);
        String refreshToken = jwtUtils.generateToken(user.getUserId(), currentTimestamp, refreshTokenExpires);
        AuthResponseDTO auth = new AuthResponseDTO(accessToken, refreshToken, accessTokenExpires, refreshTokenExpires);
        response.setData(auth);
        response.setStatus(RestResponse.Status.OK.getStatusCode());
        user.setLastLoginTime(DateTime.now().toDate());
        userRepository.persist(user);
        String key = RedisConstants.REFRESH_TOKEN + user.getUserId();
        redisService.saveWithExpiredTime(key, refreshToken, Duration.ofSeconds(Constants.JwtTokenEnum.REFRESH_TOKEN_EXPIRED.getValue()));
        return response;
    }

    @Transactional
    public ResponseDTO renewToken(AuthRenewRequestDTO request) {
        ResponseDTO response = new ResponseDTO();
        try {
            // Token has already been verified, parse the token claims only
            JsonWebToken token = parser.parse(request.getRefreshToken());
            String key = RedisConstants.REFRESH_TOKEN + token.getSubject();
            if (!redisService.checkIfKeyExists(key)) {
                response.setData(null);
                response.setStatus(RestResponse.Status.CONFLICT.getStatusCode());
                response.setErrorCode(ApiErrorConstants.FAILED_TO_VERIFY_TOKEN.getCode());
                response.setMessage(ApiErrorConstants.FAILED_TO_VERIFY_TOKEN.getMessage());
                return response;
            }
            String oldToken = (String) redisService.get(key);
            if (!oldToken.equals(request.getRefreshToken())) {
                response.setData(null);
                response.setStatus(RestResponse.Status.CONFLICT.getStatusCode());
                response.setErrorCode(ApiErrorConstants.FAILED_TO_VERIFY_TOKEN.getCode());
                response.setMessage(ApiErrorConstants.FAILED_TO_VERIFY_TOKEN.getMessage());
                return response;
            }
            UserModel user = userRepository.findByUserId(token.getSubject());
            if (user == null) {
                response.setData(null);
                response.setStatus(RestResponse.Status.CONFLICT.getStatusCode());
                response.setErrorCode(ApiErrorConstants.USER_NOT_FOUND.getCode());
                response.setMessage(ApiErrorConstants.USER_NOT_FOUND.getMessage());
                return response;
            }
            long currentTimestamp = DateTime.now().getMillis() / 1000;
            long accessTokenExpires = currentTimestamp + Constants.JwtTokenEnum.ACCESS_TOKEN_EXPIRED.getValue();
            long refreshTokenExpires = currentTimestamp + Constants.JwtTokenEnum.REFRESH_TOKEN_EXPIRED.getValue();
            String accessToken = jwtUtils.generateToken(user.getUserId(), currentTimestamp, accessTokenExpires);
            String refreshToken = jwtUtils.generateToken(user.getUserId(), currentTimestamp, refreshTokenExpires);
            AuthResponseDTO auth = new AuthResponseDTO(accessToken, refreshToken, accessTokenExpires, refreshTokenExpires);
            response.setData(auth);
            response.setStatus(RestResponse.Status.OK.getStatusCode());
            redisService.saveWithExpiredTime(key, refreshToken, Duration.ofSeconds(Constants.JwtTokenEnum.REFRESH_TOKEN_EXPIRED.getValue()));
            EventLogger.writeToLog(new LogEvent(user.getUserId(), Constants.EventLoggerEnum.REGISTER_EVENT.getValue(), gson.toJson(user), ""));
            return response;
        } catch (ParseException ex) {
            response.setData(null);
            response.setStatus(RestResponse.Status.CONFLICT.getStatusCode());
            response.setErrorCode(ApiErrorConstants.FAILED_TO_VERIFY_TOKEN.getCode());
            response.setMessage(ApiErrorConstants.FAILED_TO_VERIFY_TOKEN.getMessage());
            return response;
        }
    }

    @Transactional
    public ResponseDTO logOut(String userId) {
        ResponseDTO response = new ResponseDTO();
        String key = RedisConstants.REFRESH_TOKEN + userId;
        redisService.delete(key);
        response.setData(null);
        response.setStatus(RestResponse.Status.OK.getStatusCode());
        return response;
    }
}
