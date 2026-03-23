package dev.erissos.betteradvancements.model;

import java.util.Collections;
import java.util.Map;

public record TriggerDefinition(TriggerType type, int target, Map<String, String> conditions) {

    public TriggerDefinition {
        conditions = conditions == null ? Collections.emptyMap() : Collections.unmodifiableMap(conditions);
    }
}