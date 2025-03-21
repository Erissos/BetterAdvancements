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
 * Created at 21.03.2025
 */
public class $14UndergroundMaster extends BetterAdvancement {

	public $14UndergroundMaster() {
		super(Advancements.UNDERGROUND_MASTER);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.LAPIS_ORE.get(),
				"Underground Master",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 6,
				4F,
				"Mine at least 5 different types of ores."
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("underground_master", display, Advancements.getAdvancement(Advancements.ISNT_IT_IRON_PICK), 5);

			registerEvents();
		}

		private void registerEvents() {

		}
	}
}