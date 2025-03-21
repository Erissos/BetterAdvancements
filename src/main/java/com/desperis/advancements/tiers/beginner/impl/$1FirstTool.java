package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.desperis.advancements.tiers.beginner.BeginnerAdvancements;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
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
public class $1FirstTool extends BetterAdvancement {

	public $1FirstTool() {
		super(Advancements.FIRST_TOOL);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.WOODEN_AXE.get(),
				"First Tool",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F,
				4F,
				"Craft a wooden pickaxe."
		);

		return new AdvancementImpl(display);
	}

	private class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("first_tools", display, root);

			registerEvents();
		}

		private void registerEvents() {
			registerEvent(CraftItemEvent.class, event -> {
				if (!(event.getWhoClicked() instanceof Player player)) return;

				var item = event.getCurrentItem();

				if (item == null) return;

				if (item.getType() == XMaterial.WOODEN_AXE.get()) {
					incrementProgression(player);
				}
			});
		}
	}
}
