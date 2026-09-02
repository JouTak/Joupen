package org.joupen.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public record OperationMetadata(String reason, String source, String initiator, String externalId) {
    public OperationMetadata {
        if (source == null || source.isBlank() || source.length() > 64) {
            throw new OperationException("invalid-metadata");
        }
        source = source.toLowerCase(Locale.ROOT);
        if (reason != null && reason.length() > 1000
                || initiator != null && initiator.length() > 128
                || externalId != null && (externalId.isBlank() || externalId.length() > 255)) {
            throw new OperationException("invalid-metadata");
        }
    }

    public String externalKey() {
        if (externalId == null) return null;
        try {
            byte[] bytes = (source + "\0" + externalId).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static OperationMetadata system(String source) {
        return new OperationMetadata(null, source, null, null);
    }
}
