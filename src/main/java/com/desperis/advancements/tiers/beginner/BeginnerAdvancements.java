package com.desperis.advancements.tiers.beginner;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.BetterAdvancements;
import com.desperis.advancements.tiers.BetterAdvancement;
import com.desperis.advancements.tiers.beginner.impl.*;
import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.events.PlayerLoadingCompletedEvent;
import me.despical.commons.XMaterial;
import org.bukkit.entity.Player;

/**
 * @author Despical
 * <p>
 * Created at 17.03.2025
 */
public class BeginnerAdvancements {

	private static AdvancementTab TAB;
	public static RootAdvancement ROOT;

	public BeginnerAdvancements() {
		initializeRootAdvancement();
	}

	public static void register(BetterAdvancement advancement, Advancements advancements) {
		Advancements.registerAdvancement(advancements, advancement.getAdvancement());
	}

	private void initializeRootAdvancement() {
		var plugin = BetterAdvancements.getInstance();
		TAB = plugin.getAdvancementAPI().createAdvancementTab("beginner");

		AdvancementDisplay display = new AdvancementDisplay(XMaterial.DIRT.get(), "Better Advancements", AdvancementFrameType.TASK, true, true, 0F, 4F, "A new journey started...");
		ROOT = new RootAdvancement(TAB, "beginner_root", display, "textures/block/gray_concrete_powder.png");

		new $1FirstTool();
		new $2EntranceToMining();
		new $6FirstWeapon();
		new $3FirstHunt();
		new $4FirstMiner();
		new $5SmeltItUp();
		new $7FirstHarvest();
		new $8SurvivalEssentials();
		new $9StartDefending();
		new $10PotionBasics();
		new $11IsntItIronPick();
		new $12CookedPrey();
		new $13NotToday();
		new $14UndergroundMaster();
		new $16AcquireDiamonds();
		new $15FullOfDiamonds();
		new $17UnbreakableDefense();

		TAB.registerAdvancements(ROOT, Advancements.getAdvancements());
		TAB.getEventManager().register(TAB, PlayerLoadingCompletedEvent.class, event -> {
			Player player = event.getPlayer();

			TAB.showTab(player);
			TAB.grantRootAdvancement(player);
		});
	}


}