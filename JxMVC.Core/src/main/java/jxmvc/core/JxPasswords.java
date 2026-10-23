package jxmvc.core;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class JxPasswords {
    private static final String ALGORITHM   = "PBKDF2WithHmacSHA256";
    private static final String PREFIX       = "pbkdf2$sha256$";
    private static final int    SALT_BYTES   = 16;
    private static final int    KEY_BITS     = 256;
    private static final int    DEFAULT_ITER = 210_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private JxPasswords() {}

    public static String hash(String plain) {
        return hash(plain, DEFAULT_ITER);
    }

    public static String hash(String plain, int iterations) {
        if (plain == null) throw new IllegalArgumentException("La contraseña no puede ser null");
        if (iterations < 1) throw new IllegalArgumentException("iterations debe ser >= 1");

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] dk = derive(plain.toCharArray(), salt, iterations, KEY_BITS);

        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return PREFIX + iterations + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(dk);
    }

    public static boolean verify(String plain, String stored) {
        if (plain == null || stored == null || !stored.startsWith(PREFIX)) return false;

        String[] parts = stored.split("\\$");
        if (parts.length != 5) return false;

        try {
            int    iterations = Integer.parseInt(parts[2]);
            byte[] salt       = Base64.getDecoder().decode(parts[3]);
            byte[] expected   = Base64.getDecoder().decode(parts[4]);
            byte[] actual     = derive(plain.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean needsRehash(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return true;
        String[] parts = stored.split("\\$");
        if (parts.length != 5) return true;
        try {
            return Integer.parseInt(parts[2]) < DEFAULT_ITER;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static byte[] derive(char[] plain, byte[] salt, int iterations, int keyBits) {
        try {
            KeySpec spec = new PBEKeySpec(plain, salt, iterations, keyBits);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 no disponible en esta JVM", e);
        } finally {
            java.util.Arrays.fill(plain, '\0');
        }
    }
}
