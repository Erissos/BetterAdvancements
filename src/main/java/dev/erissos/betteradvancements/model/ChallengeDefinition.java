package dev.erissos.betteradvancements.model;

import java.util.Collections;
import java.util.List;

public record ChallengeDefinition(
        String id,
        String type,
        String title,
        String description,
        TriggerDefinition trigger,
        int pointsReward,
        List<RewardDefinition> rewards
) {

    public ChallengeDefinition {
        rewards = rewards == null ? List.of() : Collections.unmodifiableList(rewards);
    }
}