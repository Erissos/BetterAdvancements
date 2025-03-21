package com.desperis.advancements.utils;

import com.desperis.advancements.BetterAdvancements;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 17.03.2025
 */
public class Utils {

	private static final BetterAdvancements plugin = BetterAdvancements.getInstance();
	private static final FileConfiguration config = ConfigUtils.getConfig(plugin, "beginner");

	private Utils() {
	}

	public static AdvancementDisplay getDisplay(String namespace, float x, float y, AdvancementFrameType frameType) {
		String path = namespace + ".";
		Material material = XMaterial.matchXMaterial(config.getString(path + "material", "DIRT")).orElseThrow().get();
		String title = config.getString(path + "title");
		boolean showToast = config.getBoolean(path + "showToast", true);
		boolean announceChat = config.getBoolean(path + "announceChat", false);
		List<String> description = config.getStringList(path + "description");

		return new AdvancementDisplay(material, title, frameType, showToast, announceChat, x, y, description);
	}
}
