package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class $4FirstMiner extends BetterAdvancement {

	public $4FirstMiner() {
		super(Advancements.FIRST_MINER);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.IRON_ORE.get(),
				"First Miner",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 3,
				4F,
				"Mine iron ores."
		);

		return new AdvancementImpl(display);
	}

	private class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("first_miner", display, root, 10);

			registerEvents();
		}

		private void registerEvents() {
			registerEvent(BlockBreakEvent.class, event -> {
				if (event.getBlock().getType() != XMaterial.IRON_ORE.get()) return;

				incrementProgression(event.getPlayer());
			});
		}
	}
}
