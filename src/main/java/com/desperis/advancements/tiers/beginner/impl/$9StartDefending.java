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
public class $9StartDefending extends BetterAdvancement {

	public $9StartDefending() {
		super(Advancements.START_DEFENDING);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.IRON_LEGGINGS.get(),
				"Start Defending",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 5,
				-1.25F + 4,
				"Equip full of iron armor."
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("start_defending", display, Advancements.getAdvancement(Advancements.SMELT_IT_UP));

			registerEvents();
		}

		private void registerEvents() {
			registerEvent(PlayerArmorChangeEvent.class, event -> {
				var newItem = event.getNewItem();

				if (newItem == null) return;

				var isIronArmor = Stream.of(
						XMaterial.IRON_HELMET, XMaterial.IRON_CHESTPLATE,
						XMaterial.IRON_LEGGINGS, XMaterial.IRON_BOOTS
				).map(XMaterial::get).anyMatch(material -> material == event.getNewItem().getType());

				if (!isIronArmor) return;

				Player player = event.getPlayer();
				boolean fullIron = true;

				for (var armor : player.getInventory().getArmorContents()) {
					if (armor == null) {
						fullIron = false;
						break;
					}

					if (!armor.getType().name().contains("IRON")) {
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
