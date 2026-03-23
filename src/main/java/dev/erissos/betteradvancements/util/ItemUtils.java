package dev.erissos.betteradvancements.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;

public final class ItemUtils {

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
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public static String colorize(String value) {
        return value == null ? "" : value.replace('&', '§');
    }

    public static Component component(String value) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(value == null ? "" : value);
    }
}