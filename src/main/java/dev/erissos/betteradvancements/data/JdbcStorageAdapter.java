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
import java.sql.DatabaseMetaData;
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
        if (!"sqlite".equalsIgnoreCase(type) && !"mysql".equalsIgnoreCase(type)) throw new IllegalArgumentException("storage.type must be sqlite or mysql");
        if ("mysql".equalsIgnoreCase(type)) {
            loadDriver("com.mysql.cj.jdbc.Driver");
            String parameters=config.getString("storage.mysql.parameters","?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            if (parameters==null || parameters.isBlank()) parameters="?characterEncoding=utf8";
            if (!parameters.startsWith("?")) parameters="?"+parameters;
            hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getString("storage.mysql.host") + ":" + config.getInt("storage.mysql.port") + "/" + config.getString("storage.mysql.database") + parameters);
            hikariConfig.setUsername(config.getString("storage.mysql.username"));
            hikariConfig.setPassword(config.getString("storage.mysql.password"));
        } else {
            loadDriver("org.sqlite.JDBC");
            File databaseFile = new File(dataFolder, config.getString("storage.sqlite.file", "data.db"));
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        }
        // SQLite has a single writer. One pooled connection also prevents read/write lock upgrades.
        boolean mysql = "mysql".equalsIgnoreCase(type);
        hikariConfig.setMaximumPoolSize(mysql ? Math.max(1,Math.min(32,config.getInt("pool.maximum-size",8))) : 1);
        hikariConfig.setMinimumIdle(mysql ? Math.max(0,Math.min(hikariConfig.getMaximumPoolSize(),config.getInt("pool.minimum-idle",2))) : 1);
        hikariConfig.setPoolName("BetterAdvancementsPool");
        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ba_players (uuid VARCHAR(36) PRIMARY KEY, last_name VARCHAR(32), language VARCHAR(8), points INT NOT NULL, season_id VARCHAR(32) NOT NULL DEFAULT 'default', season_points INT NOT NULL DEFAULT 0, season_claimed TEXT, prestige_level INT NOT NULL DEFAULT 0, session_join BIGINT NOT NULL, last_seen BIGINT NOT NULL, notify_chat BOOLEAN NOT NULL DEFAULT 1, notify_title BOOLEAN NOT NULL DEFAULT 1, notify_action_bar BOOLEAN NOT NULL DEFAULT 1, notify_boss_bar BOOLEAN NOT NULL DEFAULT 1, notify_sound BOOLEAN NOT NULL DEFAULT 1)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ba_advancement_progress (uuid VARCHAR(36) NOT NULL, advancement_id VARCHAR(80) NOT NULL, progress INT NOT NULL, completed BOOLEAN NOT NULL, completed_at BIGINT, PRIMARY KEY (uuid, advancement_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ba_challenge_progress (uuid VARCHAR(36) NOT NULL, challenge_id VARCHAR(80) NOT NULL, progress INT NOT NULL, completed BOOLEAN NOT NULL, completed_at BIGINT, completed_cycle_key VARCHAR(32), PRIMARY KEY (uuid, challenge_id))");
            ensureColumn(connection, "ba_players", "season_id", "ALTER TABLE ba_players ADD COLUMN season_id VARCHAR(32) NOT NULL DEFAULT 'default'");
            ensureColumn(connection, "ba_players", "season_points", "ALTER TABLE ba_players ADD COLUMN season_points INT NOT NULL DEFAULT 0");
            ensureColumn(connection, "ba_players", "season_claimed", "ALTER TABLE ba_players ADD COLUMN season_claimed TEXT");
            ensureColumn(connection, "ba_players", "prestige_level", "ALTER TABLE ba_players ADD COLUMN prestige_level INT NOT NULL DEFAULT 0");
            ensureColumn(connection, "ba_players", "notify_chat", "ALTER TABLE ba_players ADD COLUMN notify_chat BOOLEAN NOT NULL DEFAULT 1");
            ensureColumn(connection, "ba_players", "notify_title", "ALTER TABLE ba_players ADD COLUMN notify_title BOOLEAN NOT NULL DEFAULT 1");
            ensureColumn(connection, "ba_players", "notify_action_bar", "ALTER TABLE ba_players ADD COLUMN notify_action_bar BOOLEAN NOT NULL DEFAULT 1");
            ensureColumn(connection, "ba_players", "notify_boss_bar", "ALTER TABLE ba_players ADD COLUMN notify_boss_bar BOOLEAN NOT NULL DEFAULT 1");
            ensureColumn(connection, "ba_players", "notify_sound", "ALTER TABLE ba_players ADD COLUMN notify_sound BOOLEAN NOT NULL DEFAULT 1");
            ensureColumn(connection, "ba_challenge_progress", "completed_cycle_key", "ALTER TABLE ba_challenge_progress ADD COLUMN completed_cycle_key VARCHAR(32)");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize database", exception);
        }
    }

    @Override
    public Optional<PlayerProfile> loadProfile(UUID uniqueId) {
        try (Connection connection = dataSource.getConnection()) {
            PlayerProfile profile = null;
            try (PreparedStatement statement = connection.prepareStatement("SELECT language, points, season_id, season_points, season_claimed, prestige_level, session_join, last_seen, notify_chat, notify_title, notify_action_bar, notify_boss_bar, notify_sound FROM ba_players WHERE uuid = ?")) {
                statement.setString(1, uniqueId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        profile = new PlayerProfile(uniqueId);
                        profile.setLanguage(resultSet.getString("language"));
                        profile.setPoints(resultSet.getInt("points"));
                        profile.setSeasonId(resultSet.getString("season_id"));
                        profile.setSeasonPoints(resultSet.getInt("season_points"));
                        readClaimedRewards(profile, resultSet.getString("season_claimed"));
                        profile.setPrestigeLevel(resultSet.getInt("prestige_level"));
                        profile.setSessionJoinMillis(resultSet.getLong("session_join"));
                        profile.setLastSeen(Instant.ofEpochMilli(resultSet.getLong("last_seen")));
                        profile.setChatNotificationsEnabled(resultSet.getBoolean("notify_chat"));
                        profile.setTitleNotificationsEnabled(resultSet.getBoolean("notify_title"));
                        profile.setActionBarNotificationsEnabled(resultSet.getBoolean("notify_action_bar"));
                        profile.setBossBarNotificationsEnabled(resultSet.getBoolean("notify_boss_bar"));
                        profile.setSoundNotificationsEnabled(resultSet.getBoolean("notify_sound"));
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
            try (PreparedStatement statement = connection.prepareStatement("SELECT challenge_id, progress, completed, completed_at, completed_cycle_key FROM ba_challenge_progress WHERE uuid = ?")) {
                statement.setString(1, uniqueId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        PlayerChallengeProgress progress = new PlayerChallengeProgress();
                        progress.setProgress(resultSet.getInt("progress"));
                        progress.setCompletedCycleKey(resultSet.getString("completed_cycle_key"));
                        if (resultSet.getBoolean("completed")) {
                            progress.complete(progress.getCompletedCycleKey());
                            long time=resultSet.getLong("completed_at");
                            progress.setCompletedAt(time==0 ? null : Instant.ofEpochMilli(time));
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
            try (PreparedStatement statement = connection.prepareStatement("REPLACE INTO ba_players (uuid, last_name, language, points, season_id, season_points, season_claimed, prestige_level, session_join, last_seen, notify_chat, notify_title, notify_action_bar, notify_boss_bar, notify_sound) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, profile.getUniqueId().toString());
                statement.setString(2, lastName);
                statement.setString(3, profile.getLanguage());
                statement.setInt(4, profile.getPoints());
                statement.setString(5, profile.getSeasonId());
                statement.setInt(6, profile.getSeasonPoints());
                statement.setString(7, writeClaimedRewards(profile));
                statement.setInt(8, profile.getPrestigeLevel());
                statement.setLong(9, profile.getSessionJoinMillis());
                statement.setLong(10, profile.getLastSeen().toEpochMilli());
                statement.setBoolean(11, profile.isChatNotificationsEnabled());
                statement.setBoolean(12, profile.isTitleNotificationsEnabled());
                statement.setBoolean(13, profile.isActionBarNotificationsEnabled());
                statement.setBoolean(14, profile.isBossBarNotificationsEnabled());
                statement.setBoolean(15, profile.isSoundNotificationsEnabled());
                statement.executeUpdate();
            }

            writeChangedProgress(connection,profile);

            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save profile " + profile.getUniqueId(), exception);
        }
    }

    private void writeChangedProgress(Connection connection, PlayerProfile profile) throws SQLException {
        java.util.Map<String,java.util.List<Object>> advancements=new java.util.HashMap<>(), challenges=new java.util.HashMap<>();
        String uuid=profile.getUniqueId().toString();
        try (PreparedStatement query=connection.prepareStatement("SELECT advancement_id,progress,completed,completed_at FROM ba_advancement_progress WHERE uuid=?")) {
            query.setString(1,uuid);
            try (ResultSet rows=query.executeQuery()) { while (rows.next()) advancements.put(rows.getString(1),java.util.Arrays.asList(rows.getInt(2),rows.getBoolean(3),rows.getLong(4))); }
        }
        try (PreparedStatement query=connection.prepareStatement("SELECT challenge_id,progress,completed,completed_at,completed_cycle_key FROM ba_challenge_progress WHERE uuid=?")) {
            query.setString(1,uuid);
            try (ResultSet rows=query.executeQuery()) { while (rows.next()) challenges.put(rows.getString(1),java.util.Arrays.asList(rows.getInt(2),rows.getBoolean(3),rows.getLong(4),rows.getString(5))); }
        }
        try (PreparedStatement insert=connection.prepareStatement("REPLACE INTO ba_advancement_progress (uuid,advancement_id,progress,completed,completed_at) VALUES (?,?,?,?,?)")) {
            for (var entry:profile.getAdvancementProgress().entrySet()) {
                var value=entry.getValue(); long time=value.getCompletedAt()==null ? 0 : value.getCompletedAt().toEpochMilli();
                if (java.util.Arrays.asList(value.getProgress(),value.isCompleted(),time).equals(advancements.remove(entry.getKey()))) continue;
                insert.setString(1,uuid); insert.setString(2,entry.getKey()); insert.setInt(3,value.getProgress()); insert.setBoolean(4,value.isCompleted()); insert.setLong(5,time); insert.addBatch();
            }
            insert.executeBatch();
        }
        try (PreparedStatement insert=connection.prepareStatement("REPLACE INTO ba_challenge_progress (uuid,challenge_id,progress,completed,completed_at,completed_cycle_key) VALUES (?,?,?,?,?,?)")) {
            for (var entry:profile.getChallengeProgress().entrySet()) {
                var value=entry.getValue(); long time=value.getCompletedAt()==null ? 0 : value.getCompletedAt().toEpochMilli();
                if (java.util.Arrays.asList(value.getProgress(),value.isCompleted(),time,value.getCompletedCycleKey()).equals(challenges.remove(entry.getKey()))) continue;
                insert.setString(1,uuid); insert.setString(2,entry.getKey()); insert.setInt(3,value.getProgress()); insert.setBoolean(4,value.isCompleted()); insert.setLong(5,time); insert.setString(6,value.getCompletedCycleKey()); insert.addBatch();
            }
            insert.executeBatch();
        }
        deleteMissing(connection,"ba_advancement_progress","advancement_id",uuid,advancements.keySet());
        deleteMissing(connection,"ba_challenge_progress","challenge_id",uuid,challenges.keySet());
    }

    private void deleteMissing(Connection connection,String table,String column,String uuid,java.util.Set<String> removed) throws SQLException {
        try (PreparedStatement statement=connection.prepareStatement("DELETE FROM "+table+" WHERE uuid=? AND "+column+"=?")) {
            for (String id:removed) { statement.setString(1,uuid); statement.setString(2,id); statement.addBatch(); }
            statement.executeBatch();
        }
    }

    @Override
    public void deleteProfile(UUID uniqueId) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
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
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete profile " + uniqueId, exception);
        }
    }

    @Override
    public List<PlayerProfile> loadAllProfiles() {
        List<PlayerProfile> profiles = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM ba_players");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(UUID.fromString(resultSet.getString("uuid")));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load profiles", exception);
        }
        // Release the listing connection before loading each profile from the same pool.
        for (UUID id : ids) loadProfile(id).ifPresent(profiles::add);
        return profiles;
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public List<PlayerProfile> loadTopProfiles(int limit, String season) {
        return loadTopProfiles(limit, season, null);
    }

    @Override
    public List<PlayerProfile> loadTopProfiles(int limit, String season, java.util.Set<String> activeIds) {
        List<UUID> ids = new ArrayList<>();
        List<String> active = activeIds == null ? null : activeIds.stream().sorted().toList();
        String scope = active == null ? "" : active.isEmpty() ? "AND 1=0 "
                : "AND a.advancement_id IN (" + String.join(",", java.util.Collections.nCopies(active.size(), "?")) + ") ";
        String sql = "SELECT p.uuid FROM ba_players p LEFT JOIN ba_advancement_progress a ON a.uuid = p.uuid "
            + scope
            + (season == null ? "" : "WHERE p.season_id = ? ")
            + "GROUP BY p.uuid, p.points, p.season_points ORDER BY "
            + (season == null ? "SUM(CASE WHEN a.completed = 1 THEN 1 ELSE 0 END) DESC, p.points DESC" : "p.season_points DESC, SUM(CASE WHEN a.completed = 1 THEN 1 ELSE 0 END) DESC")
            + ", p.uuid ASC LIMIT ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (active != null) for (String id : active) statement.setString(index++, id);
            if (season != null) statement.setString(index++, season);
            statement.setInt(index, Math.max(1, Math.min(1000, limit)));
            try (ResultSet result = statement.executeQuery()) { while (result.next()) ids.add(UUID.fromString(result.getString(1))); }
        } catch (SQLException failure) { throw new IllegalStateException("Could not read leaderboard", failure); }
        List<PlayerProfile> profiles = new ArrayList<>();
        for (UUID id : ids) loadProfile(id).ifPresent(profiles::add);
        return profiles;
    }

    private void loadDriver(String driverClassName) {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Missing JDBC driver: " + driverClassName, exception);
        }
    }

    private void ensureColumn(Connection connection, String table, String column, String alterSql) throws SQLException {
        if (hasColumn(connection, table, column)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(alterSql);
        }
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, table, column)) {
            while (resultSet.next()) if (table.equalsIgnoreCase(resultSet.getString("TABLE_NAME")) && column.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) return true;
            return false;
        }
    }

    private void readClaimedRewards(PlayerProfile profile, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                profile.getClaimedSeasonRewards().add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String writeClaimedRewards(PlayerProfile profile) {
        if (profile.getClaimedSeasonRewards().isEmpty()) {
            return "";
        }
        return profile.getClaimedSeasonRewards().stream()
                .sorted()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    }
}
