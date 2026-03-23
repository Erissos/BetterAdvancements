package dev.erissos.betteradvancements.model;

import java.util.UUID;

public record LeaderboardEntry(UUID uniqueId, String name, int completed, double progression, int points, int highestTier) {
}