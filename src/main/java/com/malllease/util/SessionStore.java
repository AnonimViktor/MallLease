package com.malllease.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

public final class SessionStore {

    private static final Logger log = LoggerFactory.getLogger(SessionStore.class);

    private static final Path SESSION_DIR =
            Paths.get(System.getProperty("user.home"), ".malllease");
    private static final Path SESSION_FILE = SESSION_DIR.resolve("session.properties");
    private static final long MAX_AGE_DAYS = 30;

    private SessionStore() {}

    public static void save(int userId, String userLogin) {
        try {
            Files.createDirectories(SESSION_DIR);
            Properties props = new Properties();
            props.setProperty("userId", Integer.toString(userId));
            props.setProperty("userLogin", userLogin == null ? "" : userLogin);
            props.setProperty("savedAt", Instant.now().toString());
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
            String savedAt = props.getProperty("savedAt", "");
            if (userIdRaw == null || userIdRaw.isBlank()) {
                return Optional.empty();
            }
            int userId = Integer.parseInt(userIdRaw.trim());

            if (!savedAt.isBlank()) {
                try {
                    Instant when = Instant.parse(savedAt);
                    if (when.isBefore(Instant.now().minusSeconds(MAX_AGE_DAYS * 86400))) {
                        clear();
                        return Optional.empty();
                    }
                } catch (Exception e) {
                    log.debug("Saved session has malformed timestamp '{}'; ignoring it", savedAt);
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

    public record Saved(int userId, String userLogin) {}
}
