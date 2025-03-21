package com.desperis.advancements.tiers.beginner.impl;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import me.despical.commons.XMaterial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class $3FirstHunt extends BetterAdvancement {

	public $3FirstHunt() {
		super(Advancements.FIRST_HUNT);
	}

	@Override
	public BaseAdvancement getAdvancement() {
		var display = new AdvancementDisplay(
				XMaterial.BEEF.get(),
				"First Hunt",
				AdvancementFrameType.TASK,
				true,
				true,
				1.25F * 4,
				1.25F + 4,
				"Kill a cow, sheep, pig, or chicken."
		);

		return new AdvancementImpl(display);
	}

	private static class AdvancementImpl extends BaseAdvancement {

		public AdvancementImpl(AdvancementDisplay display) {
			super("first_hunt", display, Advancements.getAdvancement(Advancements.FIRST_WEAPON));

			registerEvents();
		}

		private void registerEvents() {
			registerEvent(EntityDamageByEntityEvent.class, event -> {
				if (!(event.getDamager() instanceof Player player)) return;

				var victim = event.getEntity();

				switch (victim.getType()) {
					case COW, SHEEP, PIG, CHICKEN -> {
						var entity = (LivingEntity) victim;

						if (entity.getHealth() - event.getFinalDamage() <= 0) {
							incrementProgression(player);
						}
					}
				}
			});
		}
	}
}