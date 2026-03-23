package dev.erissos.betteradvancements.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PlaceholderHook {

    private Method setPlaceholdersMethod;

    public PlaceholderHook() {
        setup();
    }

    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            Class<?> placeholderApiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            this.setPlaceholdersMethod = placeholderApiClass.getMethod("setPlaceholders", Player.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            this.setPlaceholdersMethod = null;
        }
    }

    public boolean isAvailable() {
        return setPlaceholdersMethod != null;
    }

    public String apply(Player player, String input) {
        if (setPlaceholdersMethod == null || input == null) {
            return input;
        }
        try {
            return (String) setPlaceholdersMethod.invoke(null, player, input);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return input;
        }
    }
}