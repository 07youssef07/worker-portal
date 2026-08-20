package com.company.workerportal.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;

public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16; // bytes

    private PasswordUtil() {
    }

    public static String hash(String plainTextPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(plainTextPassword.toCharArray(), salt);
        return HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);
    }

    public static boolean verify(String plainTextPassword, String storedHash) {
        String[] parts = storedHash.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = HexFormat.of().parseHex(parts[0]);
        byte[] expectedHash = HexFormat.of().parseHex(parts[1]);
        byte[] actualHash = pbkdf2(plainTextPassword.toCharArray(), salt);

        if (actualHash.length != expectedHash.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < actualHash.length; i++) {
            diff |= actualHash[i] ^ expectedHash[i];
        }
        return diff == 0;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }
}
