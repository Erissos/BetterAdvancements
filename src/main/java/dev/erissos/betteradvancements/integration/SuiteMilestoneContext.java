package dev.erissos.betteradvancements.integration;

import java.util.LinkedHashMap;
import java.util.Map;

/** Defensive receiver validation; caller authentication and receipt deduplication belong to the bridge. */
public final class SuiteMilestoneContext {
    private static final Map<String, String> SOURCES = Map.ofEntries(
            Map.entry("animal_registered", "StableMan"), Map.entry("animal_fed", "StableMan"),
            Map.entry("animal_bred", "StableMan"), Map.entry("auction_sale", "AuctionHousePro"),
            Map.entry("auction_purchase", "AuctionHousePro"), Map.entry("business_created", "PlayerBusiness"),
            Map.entry("business_order_completed", "PlayerBusiness"), Map.entry("bounty_claim", "DynamicBounty"),
            Map.entry("worker_produced", "SmartNPCWorkers"), Map.entry("dungeon_clear", "DynamicDungeonGenerator"),
            Map.entry("boss_victory", "AdaptiveBosses"), Map.entry("event_mob_kill", "ServerEventsEngine"));
    private SuiteMilestoneContext() {}

    public static Map<String, String> validated(Map<String, String> context) {
        if (context == null || context.size() > 32) return Map.of();
        String source = context.get("source"), event = context.get("event");
        if (source == null || event == null || !source.equals(SOURCES.get(event))) return Map.of();
        long amount = positiveAmount(context.get("amount"));
        if (amount == 0) return Map.of();
        Map<String, String> safe = new LinkedHashMap<>();
        for (var entry : context.entrySet()) {
            if (entry.getKey() == null || !entry.getKey().matches("[a-z][a-z0-9_-]{0,63}")
                    || entry.getValue() == null || entry.getValue().length() > 512) return Map.of();
            safe.put(entry.getKey(), entry.getValue());
        }
        safe.put("amount", Long.toString(Math.min(Integer.MAX_VALUE, amount)));
        return Map.copyOf(safe);
    }

    public static boolean knownPair(String source, String event) {
        return source != null && event != null && source.equals(SOURCES.get(event));
    }

    public static long positiveAmount(String value) {
        try { long amount = Long.parseLong(value); return amount > 0 ? amount : 0; }
        catch (RuntimeException invalid) { return 0; }
    }
}
