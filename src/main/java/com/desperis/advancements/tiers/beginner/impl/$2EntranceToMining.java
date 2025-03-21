package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.desperis.advancements.user.User;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class $2EntranceToMining extends BetterAdvancement {

	public $2EntranceToMining() {
		super(Advancements.ENTRANCE_TO_MINING);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.COBBLESTONE.get(),
				"Entrance To Mining",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 2,
				4F,
				"Mine your first stone and craft a stone pickaxe."
		);

		return new AdvancementImpl(display);
	}

	private class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("etm", display, root);

			registerEvents();
		}

		private void registerEvents() {
			registerEvent(BlockBreakEvent.class, event -> {
				if (event.getBlock().getType() != XMaterial.STONE.parseMaterial()) return;

				var player = event.getPlayer();
				var user = userManager.getUser(player);

				if (user.getProgress("etm_stone") == 1) {
					return;
				}

				user.incrementProgress("etm_stone");
			});

			registerEvent(CraftItemEvent.class, event -> {
				if (!(event.getWhoClicked() instanceof Player player)) return;

				var item = event.getCurrentItem();

				if (item == null) return;

				var user = userManager.getUser(player);

				if (item.getType() == XMaterial.STONE_PICKAXE.get()) {
					user.incrementProgress("etm_pickaxe");

					checkAdvancement(user);
				}
			});
		}

		private void checkAdvancement(User user) {
			if (user.isFinishedProgress("etm_pickaxe", "etm_stone")) {
				incrementProgression(user.getUUID());
			}
		}
	}
}
