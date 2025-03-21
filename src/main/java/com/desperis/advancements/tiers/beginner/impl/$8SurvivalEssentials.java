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
public class $8SurvivalEssentials extends BetterAdvancement {

	public $8SurvivalEssentials() {
		super(Advancements.SURVIVAL_ESSENTIALS);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.BREAD.get(),
				"Survival Essentials",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 2,
				1.25F * 3 + 4,
				"Make a bread from wheat."
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("survival_essentials", display, Advancements.getAdvancement(Advancements.FIRST_HARVEST));

			registerEvents();
		}

		private void registerEvents() {
			/*
			// Cook meat from an animal or
			registerEvent(FurnaceExtractEvent.class, event -> {
				var isCooked = Stream.of(
						XMaterial.COOKED_BEEF, XMaterial.COOKED_CHICKEN,
						XMaterial.COOKED_SALMON, XMaterial.COOKED_COD,
						XMaterial.COOKED_MUTTON, XMaterial.COOKED_PORKCHOP,
						XMaterial.COOKED_RABBIT
				).map(XMaterial::get).anyMatch(material -> material == event.getItemType());

				if (!isCooked) return;

				incrementProgression(event.getPlayer());
			});
*/
			registerEvent(CraftItemEvent.class, event -> {
				if (!(event.getWhoClicked() instanceof Player player)) return;

				var item = event.getCurrentItem();

				if (item == null) return;

				if (item.getType() == XMaterial.BREAD.get()) {
					incrementProgression(player);
				}
			});
		}
	}
}
