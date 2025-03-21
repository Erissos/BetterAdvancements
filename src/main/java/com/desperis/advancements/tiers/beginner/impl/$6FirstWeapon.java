package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class $6FirstWeapon extends BetterAdvancement {

	public $6FirstWeapon() {
		super(Advancements.FIRST_WEAPON);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.STONE_SWORD.get(),
				"First Weapon",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 3,
				1.25F + 4,
				"Craft a stone sword."
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("first_weapon", display, Advancements.getAdvancement(Advancements.ENTRANCE_TO_MINING));

			registerEvents();
		}

		private void registerEvents() {
			registerEvent(CraftItemEvent.class, event -> {
				if (!(event.getWhoClicked() instanceof Player player)) return;

				var item = event.getCurrentItem();

				if (item == null) return;

				if (item.getType() == XMaterial.STONE_SWORD.get()) {
					incrementProgression(player);
				}
			});
		}
	}
}
