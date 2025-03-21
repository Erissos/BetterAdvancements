package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;
import org.bukkit.entity.Player;

import java.util.stream.Stream;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class $15FullOfDiamonds extends BetterAdvancement {

	public $15FullOfDiamonds() {
		super(Advancements.FULL_OF_DIAMONDS);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.DIAMOND_CHESTPLATE.get(),
				"Full Of Diamonds",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 8,
				4F - 1.25F,
				"Equip full of diamond armor."
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("full_of_dia", display, Advancements.getAdvancement(Advancements.ACQUIRE_DIAMONDS));

			registerEvents();
		}

		private void registerEvents() {
			registerEvent(PlayerArmorChangeEvent.class, event -> {
				var newItem = event.getNewItem();

				if (newItem == null) return;

				var isIronArmor = Stream.of(
						XMaterial.DIAMOND_HELMET, XMaterial.DIAMOND_CHESTPLATE,
						XMaterial.DIAMOND_LEGGINGS, XMaterial.DIAMOND_BOOTS
				).map(XMaterial::get).anyMatch(material -> material == event.getNewItem().getType());

				if (!isIronArmor) return;

				Player player = event.getPlayer();
				boolean fullIron = true;

				for (var armor : player.getInventory().getArmorContents()) {
					if (armor == null) {
						fullIron = false;
						break;
					}

					if (!armor.getType().name().contains("DIAMOND")) {
						fullIron = false;
						break;
					}
				}

				if (fullIron) {
					incrementProgression(player);
				}
			});
		}
	}
}
