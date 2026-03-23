package dev.erissos.betteradvancements.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record BetterAdvancement(
        String id,
        String title,
        String description,
        Tier tier,
        String category,
        String icon,
        boolean hidden,
        int points,
        String rarity,
        TriggerDefinition trigger,
        List<String> dependencies,
        List<RewardDefinition> rewards,
        Map<String, Integer> guiPosition
) {

    public BetterAdvancement {
        dependencies = dependencies == null ? List.of() : Collections.unmodifiableList(dependencies);
        rewards = rewards == null ? List.of() : Collections.unmodifiableList(rewards);
        guiPosition = guiPosition == null ? Map.of() : Collections.unmodifiableMap(guiPosition);
    }
}