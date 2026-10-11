package dev.ramon.kees.shared.util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@UtilityClass
public final class HashContent {

    public static final String HASH_ALGORITHM = "SHA-256";

    public static String hash(byte[] bytes) {
        Objects.requireNonNull(bytes, "Bytes cannot be null");

        try {
            var digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    public static String hash(String content) {
        Objects.requireNonNull(content, "Content cannot be null");
        return hash(content.getBytes(StandardCharsets.UTF_8));
    }
}
