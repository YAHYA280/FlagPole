package dev.flagpole.api.environment;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates opaque SDK keys: "fp_" prefix + 32 random bytes, base64url without padding.
 * Prefix makes keys recognisable in logs and secret scanners.
 */
@Component
public class SdkKeyGenerator {

    private static final String PREFIX = "fp_";
    private static final int RANDOM_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
