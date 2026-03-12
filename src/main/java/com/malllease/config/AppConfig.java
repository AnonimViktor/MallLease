package com.malllease.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final Properties PROPS = load();

    private AppConfig() {}

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load /application.properties", e);
        }
        return props;
    }

    public static String get(String key) {
        String env = System.getenv(toEnvVar(key));
        if (env != null && !env.isBlank()) {
            return env;
        }
        return PROPS.getProperty(key);
    }

    public static String get(String key, String fallback) {
        String value = get(key);
        return value == null ? fallback : value;
    }

    public static int getInt(String key, int fallback) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static long getLong(String key, long fallback) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String toEnvVar(String key) {
        return key.toUpperCase().replace('.', '_');
    }
}
