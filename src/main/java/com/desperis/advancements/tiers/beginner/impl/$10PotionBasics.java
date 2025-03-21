package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class $10PotionBasics extends BetterAdvancement {

	public $10PotionBasics() {
		super(Advancements.POTION_BASICS);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.POTION.get(),
				"Potion Basics",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 3,
				1.25F * 3 + 4,
				"Make a new potion and drink it."
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("potion_basics", display, Advancements.getAdvancement(Advancements.SURVIVAL_ESSENTIALS));

			registerEvents();
		}

		private void registerEvents() {
			registerEvent(PlayerItemConsumeEvent.class, event -> {
				var item = event.getItem();

				if (item.getType() != XMaterial.POTION.get()) return;

				var potionMeta = (PotionMeta) item.getItemMeta();

				if (potionMeta.getBasePotionData().getType() == PotionType.WATER) return;

				incrementProgression(event.getPlayer());
			});
		}
	}
}
