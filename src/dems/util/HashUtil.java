package dems.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** SHA-256 helpers used for file integrity checks and password hashing. */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256(String text) {
        return toHex(newDigest().digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    /** Streams the file through SHA-256 so large evidence files do not need to fit in memory. */
    public static String sha256(Path file) throws IOException {
        MessageDigest digest = newDigest();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    public static String randomSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
