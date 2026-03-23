package dev.erissos.betteradvancements.model;

public enum TriggerType {
    JOIN,
    BLOCK_BREAK,
    ITEM_CRAFT,
    MOB_KILL,
    PLAYER_KILL,
    EXPLORE_BIOME,
    DISTANCE_WALK,
    FISH,
    ENCHANT,
    SMELT,
    BREED,
    TAME,
    PLAYTIME,
    ITEM_CONSUME,
    COMMAND,
    CUSTOM;

    public static TriggerType fromString(String value) {
        return TriggerType.valueOf(value.toUpperCase());
    }
}