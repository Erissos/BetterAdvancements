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
public class $11IsntItIronPick extends BetterAdvancement {

	public $11IsntItIronPick() {
		super(Advancements.ISNT_IT_IRON_PICK);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.IRON_PICKAXE.get(),
				"Isn't It Iron Pick",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 5,
				4F,
				"Upgrade your pickaxe."
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("isnt_it_iron_pick", display, Advancements.getAdvancement(Advancements.SMELT_IT_UP));

			registerEvents();
		}

		private void registerEvents() {

		}
	}
}