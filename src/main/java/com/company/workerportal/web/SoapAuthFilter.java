/*package com.company.workerportal.web;

import com.company.workerportal.security.BasicAuthValidator;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(urlPatterns = "/WorkerSoapService")
public class SoapAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String authHeader = req.getHeader("Authorization");
 
        if (BasicAuthValidator.isValid(authHeader)) {
            chain.doFilter(request, response);
        } else {
            resp.setHeader("WWW-Authenticate", "Basic realm=\"worker-portal-soap\"");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
        }
    }
}
*/