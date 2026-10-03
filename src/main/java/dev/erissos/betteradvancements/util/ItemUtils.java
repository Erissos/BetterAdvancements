package dev.erissos.betteradvancements.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.regex.Pattern;

public final class ItemUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ItemUtils() {
    }

    public static ItemStack create(String materialName, String displayName, List<String> lore, boolean glow) {
        Material material = Material.matchMaterial(materialName);
        ItemStack itemStack = new ItemStack(material == null ? Material.BARRIER : material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(component(displayName));
            if (lore != null) {
                meta.lore(lore.stream().map(ItemUtils::component).toList());
            }
            if (glow) {
                meta.setEnchantmentGlintOverride(true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public static String colorize(String value) {
        return value == null ? "" : value;
    }

    public static Component component(String value) {
        return MINI_MESSAGE.deserialize(normalize(value));
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = LEGACY_HEX_PATTERN.matcher(value).replaceAll("<#$1>");
        return "<!italic>" + normalized
                .replace("<li>", "")
                .replace("</li>", "");
    }
}
