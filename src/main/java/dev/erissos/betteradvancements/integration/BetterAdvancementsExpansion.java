package dev.erissos.betteradvancements.integration;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.model.PlayerProfile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BetterAdvancementsExpansion extends PlaceholderExpansion {

    private final BetterAdvancementsPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public BetterAdvancementsExpansion(BetterAdvancementsPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ba";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(",", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "points" -> String.valueOf(profile.getPoints());
            case "season_points" -> String.valueOf(profile.getSeasonPoints());
            case "prestige" -> String.valueOf(profile.getPrestigeLevel());
            case "completed" -> String.valueOf(plugin.getAchievementManager().getCompletedCount(profile));
            case "challenges_completed" -> String.valueOf(profile.getCompletedChallenges());
            default -> null;
        };
    }
}
