package io.zw.auth.api.resources;

import io.zw.auth.api.dto.AuthRenewRequestDTO;
import io.zw.auth.api.services.AuthService;
import io.zw.auth.api.dto.AuthRequestDTO;
import io.zw.auth.api.dto.ResponseDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/auth")
public class AuthResource {

    @Inject
    private AuthService authService;

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public RestResponse<ResponseDTO> register(AuthRequestDTO request) {
        ResponseDTO response = authService.register(request);
        if (response.getStatus() == RestResponse.Status.CONFLICT.getStatusCode()) {
            return RestResponse.ResponseBuilder.create(RestResponse.Status.CONFLICT, response).build();
        }
        return RestResponse.ResponseBuilder.ok(response).build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public RestResponse<ResponseDTO> login(AuthRequestDTO request) {
        ResponseDTO response = authService.login(request);
        if (response.getStatus() == RestResponse.Status.CONFLICT.getStatusCode()) {
            return RestResponse.ResponseBuilder.create(RestResponse.Status.CONFLICT, response).build();
        }
        return RestResponse.ResponseBuilder.ok(response).build();
    }

    @POST
    @Path("/renew")
    @Consumes(MediaType.APPLICATION_JSON)
    public RestResponse<ResponseDTO> renewToken(AuthRenewRequestDTO request) {
        ResponseDTO response = authService.renewToken(request);
        if (response.getStatus() == RestResponse.Status.CONFLICT.getStatusCode()) {
            return RestResponse.ResponseBuilder.create(RestResponse.Status.CONFLICT, response).build();
        }
        return RestResponse.ResponseBuilder.ok(response).build();
    }
}
