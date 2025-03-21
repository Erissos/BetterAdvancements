package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class $7FirstHarvest extends BetterAdvancement {

	public $7FirstHarvest() {
		super(Advancements.FIRST_HARVEST);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.WHEAT_SEEDS.get(),
				"First Harvest",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F,
				1.25F * 3 + 4,
				"Plant and grow wheat, carrots, or potatoes."
		);

		return new AdvancementImpl(display);
	}

	private class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("first_harvest", display, root);

			registerEvents();
		}

		private void registerEvents() {

		}
	}
}
