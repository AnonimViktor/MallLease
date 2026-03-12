package com.malllease.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private static volatile HikariDataSource dataSource;

    private Database() {}

    public static synchronized void init() {
        if (dataSource != null) {
            return;
        }
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(AppConfig.get("db.url"));
        cfg.setUsername(AppConfig.get("db.user"));
        cfg.setPassword(AppConfig.get("db.password"));
        cfg.setDriverClassName("org.postgresql.Driver");
        cfg.setMaximumPoolSize(AppConfig.getInt("db.pool.maximumPoolSize", 8));
        cfg.setMinimumIdle(AppConfig.getInt("db.pool.minimumIdle", 2));
        cfg.setConnectionTimeout(AppConfig.getLong("db.pool.connectionTimeoutMs", 10_000));
        cfg.setIdleTimeout(AppConfig.getLong("db.pool.idleTimeoutMs", 600_000));
        cfg.setMaxLifetime(AppConfig.getLong("db.pool.maxLifetimeMs", 1_800_000));
        cfg.setPoolName("mall-lease-hikari");

        dataSource = new HikariDataSource(cfg);
        log.info("HikariCP pool initialised: {}, maxPool={}",
                AppConfig.get("db.url"), cfg.getMaximumPoolSize());
    }

    public static DataSource dataSource() {
        if (dataSource == null) {
            init();
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("HikariCP pool closed.");
        }
    }
}
