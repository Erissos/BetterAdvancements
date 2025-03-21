package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;
import org.bukkit.event.inventory.FurnaceExtractEvent;

import java.util.stream.Stream;

/**
 * @author Despical
 * <p>
 * Created at 21.03.2025
 */
public class $12CookedPrey extends BetterAdvancement {

	public $12CookedPrey() {
		super(Advancements.COOKED_PREY);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.COOKED_BEEF.get(),
				"Cooked Prey",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 5,
				1.25F + 4,
				"Cook the meat of an animal you killed."
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("cooked_prey", display, Advancements.getAdvancement(Advancements.FIRST_HUNT), 15);

			registerEvents();
		}

		private void registerEvents() {
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
		}
	}
}