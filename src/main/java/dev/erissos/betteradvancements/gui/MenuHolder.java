package dev.erissos.betteradvancements.gui;

import dev.erissos.betteradvancements.model.Tier;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public record MenuHolder(MenuType type, Tier tier, boolean session) implements InventoryHolder {

    @Override
    public Inventory getInventory() {
        return null;
    }
}