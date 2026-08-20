package com.company.workerportal.security;

import com.company.workerportal.dao.UserDAO;
import com.company.workerportal.model.User;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class BasicAuthValidator {

    private static final UserDAO userDAO = new UserDAO();

    private BasicAuthValidator() {
    }

    public static boolean isValid(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            return false;
        }

        String credentials;
        try {
            String base64Credentials = authorizationHeader.substring("Basic ".length()).trim();
            credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }

        int separatorIndex = credentials.indexOf(':');
        if (separatorIndex < 0) {
            return false;
        }

        String username = credentials.substring(0, separatorIndex);
        String password = credentials.substring(separatorIndex + 1);

        User user = userDAO.findByUsername(username);
        return user != null && PasswordUtil.verify(password, user.getPasswordHash());
    }
}
