package com.company.workerportal.rest;

import com.company.workerportal.security.BasicAuthValidator;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * Protects every endpoint under /api with HTTP Basic Authentication.
 * Reuses the same app_user accounts as the web login (e.g. admin/admin123).
 */
@Provider
public class ApiAuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (!BasicAuthValidator.isValid(authHeader)) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"worker-portal-api\"")
                            .entity(new WorkerResource.ErrorMessage("Authentication required"))
                            .build());
        }
    }
}
