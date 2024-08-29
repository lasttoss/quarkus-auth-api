package io.zw.auth.api.resources;

import io.quarkus.security.Authenticated;
import io.zw.auth.api.services.UserService;
import io.zw.auth.api.dto.ResponseDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.RestResponse;

@Authenticated
@Path("/users")
public class UserResource {

    @Inject
    UserService userService;

    @GET
    @RolesAllowed({"USER"})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/me")
    public RestResponse<ResponseDTO> me(@Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();
        ResponseDTO response = userService.getUserInfo(userId);
        if (response.getStatus() == RestResponse.Status.CONFLICT.getStatusCode()) {
            return RestResponse.ResponseBuilder.create(RestResponse.Status.CONFLICT, response).build();
        }
        return RestResponse.ResponseBuilder.ok(response).build();
    }
}
