package dev.erissos.betteradvancements.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.model.PlayerAchievementProgress;
import dev.erissos.betteradvancements.model.PlayerChallengeProgress;
import dev.erissos.betteradvancements.model.PlayerProfile;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcStorageAdapter implements StorageAdapter {

    private final ConfigManager configManager;
    private final File dataFolder;
    private HikariDataSource dataSource;

    public JdbcStorageAdapter(ConfigManager configManager, File dataFolder) {
        this.configManager = configManager;
        this.dataFolder = dataFolder;
    }

    @Override
    public void init() {
        FileConfiguration config = configManager.getDatabaseConfig();
        HikariConfig hikariConfig = new HikariConfig();
        String type = config.getString("storage.type", "sqlite");
        if ("mysql".equalsIgnoreCase(type)) {
            loadDriver("com.mysql.cj.jdbc.Driver");
            hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getString("storage.mysql.host") + ":" + config.getInt("storage.mysql.port") + "/" + config.getString("storage.mysql.database") + "?useSSL=false&characterEncoding=utf8");
            hikariConfig.setUsername(config.getString("storage.mysql.username"));
            hikariConfig.setPassword(config.getString("storage.mysql.password"));
        } else {
            loadDriver("org.sqlite.JDBC");
            File databaseFile = new File(dataFolder, config.getString("storage.sqlite.file", "data.db"));
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        }
        hikariConfig.setMaximumPoolSize(config.getInt("pool.maximum-size", 8));
        hikariConfig.setMinimumIdle(config.getInt("pool.minimum-idle", 2));
        hikariConfig.setPoolName("BetterAdvancementsPool");
        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ba_players (uuid VARCHAR(36) PRIMARY KEY, last_name VARCHAR(32), language VARCHAR(8), points INT NOT NULL, session_join BIGINT NOT NULL, last_seen BIGINT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ba_advancement_progress (uuid VARCHAR(36) NOT NULL, advancement_id VARCHAR(80) NOT NULL, progress INT NOT NULL, completed BOOLEAN NOT NULL, completed_at BIGINT, PRIMARY KEY (uuid, advancement_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ba_challenge_progress (uuid VARCHAR(36) NOT NULL, challenge_id VARCHAR(80) NOT NULL, progress INT NOT NULL, completed BOOLEAN NOT NULL, completed_at BIGINT, PRIMARY KEY (uuid, challenge_id))");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize database", exception);
        }
    }

    @Override
    public Optional<PlayerProfile> loadProfile(UUID uniqueId) {
        try (Connection connection = dataSource.getConnection()) {
            PlayerProfile profile = null;
            try (PreparedStatement statement = connection.prepareStatement("SELECT language, points, session_join, last_seen FROM ba_players WHERE uuid = ?")) {
                statement.setString(1, uniqueId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        profile = new PlayerProfile(uniqueId);
                        profile.setLanguage(resultSet.getString("language"));
                        profile.setPoints(resultSet.getInt("points"));
                        profile.setSessionJoinMillis(resultSet.getLong("session_join"));
                        profile.setLastSeen(Instant.ofEpochMilli(resultSet.getLong("last_seen")));
                    }
                }
            }
            if (profile == null) {
                return Optional.empty();
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT advancement_id, progress, completed, completed_at FROM ba_advancement_progress WHERE uuid = ?")) {
                statement.setString(1, uniqueId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Long completedAt = resultSet.getLong("completed_at");
                        profile.getAdvancementProgress().put(
                                resultSet.getString("advancement_id"),
                                new PlayerAchievementProgress(resultSet.getInt("progress"), resultSet.getBoolean("completed"), completedAt == 0L ? null : Instant.ofEpochMilli(completedAt))
                        );
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT challenge_id, progress, completed, completed_at FROM ba_challenge_progress WHERE uuid = ?")) {
                statement.setString(1, uniqueId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        PlayerChallengeProgress progress = new PlayerChallengeProgress();
                        progress.setProgress(resultSet.getInt("progress"));
                        if (resultSet.getBoolean("completed")) {
                            progress.complete();
                        }
                        profile.getChallengeProgress().put(resultSet.getString("challenge_id"), progress);
                    }
                }
            }
            return Optional.of(profile);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load profile " + uniqueId, exception);
        }
    }

    @Override
    public void saveProfile(String lastName, PlayerProfile profile) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("REPLACE INTO ba_players (uuid, last_name, language, points, session_join, last_seen) VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, profile.getUniqueId().toString());
                statement.setString(2, lastName);
                statement.setString(3, profile.getLanguage());
                statement.setInt(4, profile.getPoints());
                statement.setLong(5, profile.getSessionJoinMillis());
                statement.setLong(6, profile.getLastSeen().toEpochMilli());
                statement.executeUpdate();
            }

            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM ba_advancement_progress WHERE uuid = ?")) {
                delete.setString(1, profile.getUniqueId().toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("REPLACE INTO ba_advancement_progress (uuid, advancement_id, progress, completed, completed_at) VALUES (?, ?, ?, ?, ?)")) {
                for (var entry : profile.getAdvancementProgress().entrySet()) {
                    insert.setString(1, profile.getUniqueId().toString());
                    insert.setString(2, entry.getKey());
                    insert.setInt(3, entry.getValue().getProgress());
                    insert.setBoolean(4, entry.getValue().isCompleted());
                    insert.setLong(5, entry.getValue().getCompletedAt() == null ? 0L : entry.getValue().getCompletedAt().toEpochMilli());
                    insert.addBatch();
                }
                insert.executeBatch();
            }

            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM ba_challenge_progress WHERE uuid = ?")) {
                delete.setString(1, profile.getUniqueId().toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("REPLACE INTO ba_challenge_progress (uuid, challenge_id, progress, completed, completed_at) VALUES (?, ?, ?, ?, ?)")) {
                for (var entry : profile.getChallengeProgress().entrySet()) {
                    insert.setString(1, profile.getUniqueId().toString());
                    insert.setString(2, entry.getKey());
                    insert.setInt(3, entry.getValue().getProgress());
                    insert.setBoolean(4, entry.getValue().isCompleted());
                    insert.setLong(5, entry.getValue().getCompletedAt() == null ? 0L : entry.getValue().getCompletedAt().toEpochMilli());
                    insert.addBatch();
                }
                insert.executeBatch();
            }

            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save profile " + profile.getUniqueId(), exception);
        }
    }

    @Override
    public void deleteProfile(UUID uniqueId) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement playerDelete = connection.prepareStatement("DELETE FROM ba_players WHERE uuid = ?")) {
                playerDelete.setString(1, uniqueId.toString());
                playerDelete.executeUpdate();
            }
            try (PreparedStatement progressDelete = connection.prepareStatement("DELETE FROM ba_advancement_progress WHERE uuid = ?")) {
                progressDelete.setString(1, uniqueId.toString());
                progressDelete.executeUpdate();
            }
            try (PreparedStatement challengeDelete = connection.prepareStatement("DELETE FROM ba_challenge_progress WHERE uuid = ?")) {
                challengeDelete.setString(1, uniqueId.toString());
                challengeDelete.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete profile " + uniqueId, exception);
        }
    }

    @Override
    public List<PlayerProfile> loadAllProfiles() {
        List<PlayerProfile> profiles = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM ba_players");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                loadProfile(UUID.fromString(resultSet.getString("uuid"))).ifPresent(profiles::add);
            }
            return profiles;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load profiles", exception);
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void loadDriver(String driverClassName) {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Missing JDBC driver: " + driverClassName, exception);
        }
    }
}