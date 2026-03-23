package dev.erissos.betteradvancements.model;

public enum Tier {
    TIER_1(1, "Novice"),
    TIER_2(2, "Skilled"),
    TIER_3(3, "Expert"),
    TIER_4(4, "Master"),
    TIER_5(5, "Mythic");

    private final int weight;
    private final String displayKey;

    Tier(int weight, String displayKey) {
        this.weight = weight;
        this.displayKey = displayKey;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayKey() {
        return displayKey;
    }

    public static Tier fromString(String value) {
        return Tier.valueOf(value.toUpperCase());
    }
}