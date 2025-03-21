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
public class $13NotToday extends BetterAdvancement {

	public $13NotToday() {
		super(Advancements.NOT_TODAY);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.SHIELD.get(),
				"Not Today, Thank You",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 6,
				-1.25F + 4,
				"Deflect a projectile with a", "Shield"
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("not_today", display, Advancements.getAdvancement(Advancements.START_DEFENDING));

			registerEvents();
		}

		private void registerEvents() {

		}
	}
}