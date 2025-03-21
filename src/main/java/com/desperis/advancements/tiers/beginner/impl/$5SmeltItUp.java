package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;
import org.bukkit.event.inventory.FurnaceExtractEvent;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class $5SmeltItUp extends BetterAdvancement {

	public $5SmeltItUp() {
		super(Advancements.SMELT_IT_UP);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.IRON_INGOT.get(),
				"Smelt It Up",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 4,
				4F,
				"Mine iron ores."
		);

		return new AdvancementImpl(display);
	}

	private class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("smelt_it_up", display, root);

			registerEvents();
		}

		private void registerEvents() {
			registerEvent(FurnaceExtractEvent.class, event -> {
				if (event.getItemType() != XMaterial.IRON_INGOT.get()) return;

				incrementProgression(event.getPlayer());
			});
		}
	}
}
