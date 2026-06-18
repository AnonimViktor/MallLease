package com.malllease.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;

public final class SessionStore {

    private static final Logger log = LoggerFactory.getLogger(SessionStore.class);

    private static final Path SESSION_DIR =
            Paths.get(System.getProperty("user.home"), ".malllease");
    private static final Path SESSION_FILE = SESSION_DIR.resolve("session.properties");
    private static final long MAX_AGE_DAYS = 30;

    // Изменение этой константы инвалидирует все существующие сессии
    private static final String HMAC_SECRET = "MallLease-v1-session-integrity-key";

    private SessionStore() {}

    public static void save(int userId, String userLogin) {
        try {
            Files.createDirectories(SESSION_DIR);
            String savedAt = Instant.now().toString();
            String login = userLogin == null ? "" : userLogin;
            String sig = sign(userId + ":" + login + ":" + savedAt);

            Properties props = new Properties();
            props.setProperty("userId",    Integer.toString(userId));
            props.setProperty("userLogin", login);
            props.setProperty("savedAt",   savedAt);
            props.setProperty("sig",       sig);
            try (var out = Files.newOutputStream(SESSION_FILE)) {
                props.store(out, "Mall Lease — saved session");
            }
        } catch (IOException e) {
            log.warn("Failed to write session file {}: {}", SESSION_FILE, e.getMessage());
        }
    }

    public static Optional<Saved> load() {
        if (!Files.isRegularFile(SESSION_FILE)) {
            return Optional.empty();
        }
        try (var in = Files.newInputStream(SESSION_FILE)) {
            Properties props = new Properties();
            props.load(in);

            String userIdRaw = props.getProperty("userId");
            String userLogin = props.getProperty("userLogin", "");
            String savedAt   = props.getProperty("savedAt", "");
            String sig       = props.getProperty("sig", "");

            if (userIdRaw == null || userIdRaw.isBlank()) {
                return Optional.empty();
            }

            int userId = Integer.parseInt(userIdRaw.trim());

            // Проверяем подпись — защита от ручного редактирования файла
            String expected = sign(userId + ":" + userLogin + ":" + savedAt);
            if (!expected.equals(sig)) {
                log.warn("Session file signature mismatch — rejecting saved session");
                clear();
                return Optional.empty();
            }

            if (!savedAt.isBlank()) {
                try {
                    Instant when = Instant.parse(savedAt);
                    if (when.isBefore(Instant.now().minusSeconds(MAX_AGE_DAYS * 86400L))) {
                        clear();
                        return Optional.empty();
                    }
                } catch (Exception e) {
                    log.debug("Malformed session timestamp '{}'; rejecting", savedAt);
                    clear();
                    return Optional.empty();
                }
            }

            return Optional.of(new Saved(userId, userLogin));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void clear() {
        try {
            Files.deleteIfExists(SESSION_FILE);
        } catch (IOException e) {
            log.warn("Failed to clear session file {}: {}", SESSION_FILE, e.getMessage());
        }
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC signing failed", e);
        }
    }

    public record Saved(int userId, String userLogin) {}
}
