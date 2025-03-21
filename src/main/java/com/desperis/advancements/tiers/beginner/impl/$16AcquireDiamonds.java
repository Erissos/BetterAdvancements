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
public class $16AcquireDiamonds extends BetterAdvancement {

	public $16AcquireDiamonds() {
		super(Advancements.ACQUIRE_DIAMONDS);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.DIAMOND.get(),
				"Acquire Diamonds",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 7,
				4F,
				"Diamonds!"
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("acquire_diamonds", display, Advancements.getAdvancement(Advancements.UNDERGROUND_MASTER));

			registerEvents();
		}

		private void registerEvents() {

		}
	}
}