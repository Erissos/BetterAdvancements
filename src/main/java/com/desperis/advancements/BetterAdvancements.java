package com.desperis.advancements;

import com.desperis.advancements.events.GeneralEvents;
import com.desperis.advancements.tiers.beginner.BeginnerAdvancements;
import com.desperis.advancements.user.UserManager;
import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * <p>
 * Created at 16.03.2025
 */
public class BetterAdvancements extends JavaPlugin {

	private static BetterAdvancements instance;

	private AdvancementTab tab;
	private UltimateAdvancementAPI api;
	private AdvancementMain main;
	private UserManager userManager;

	private BeginnerAdvancements beginnerAdvancements;

	@Override
	public void onLoad() {
		main = new AdvancementMain(this);
		main.load();
	}

	@Override
	public void onEnable() {
		instance = this;

		main.enableInMemory();

		api = UltimateAdvancementAPI.getInstance(this);
//		api.disableVanillaAdvancements();

		userManager = new UserManager();

		beginnerAdvancements = new BeginnerAdvancements();

		new GeneralEvents(this);

//		tab = api.createAdvancementTab("your_tab_name");
//
//		AdvancementDisplay rootDisplay = new AdvancementDisplay(Material.TOTEM_OF_UNDYING, "Kendi Demircim", AdvancementFrameType.TASK, true, true, 0, 0, "Bir tam elmas", "set yap");
//		RootAdvancement root = new RootAdvancement(tab, "root1", rootDisplay, "textures/block/stone.png");
//
//		AdvancementDisplay baseDisplay = new AdvancementDisplay(Material.DIAMOND_SWORD, "Test", AdvancementFrameType.TASK, true, true, 30, 0, "Bir tam elmas", "set yap");
//		BaseAdvancement base = new BaseAdvancement("test", baseDisplay, root);
//
//		tab.registerAdvancements(root, base);
//
//		tab = api.createAdvancementTab("mytab");
//
//		RootAdvancement root = new RootAdvancement(tab, "root", new AdvancementDisplay(Material.OAK_SAPLING, "Birinci Tab", AdvancementFrameType.CHALLENGE, false, false, 0, 0), "textures/block/cobblestone.png");
//		MineAdv pickaxe = new MineAdv("pickaxe", new AdvancementDisplay(Material.STONE, "Mine Stone", AdvancementFrameType.TASK, true, true, 1.5f, 0, "Mine 10 blocks of stone."), root, 10);
//		tab.registerAdvancements(root, pickaxe);
//
//		var tab1 = api.createAdvancementTab("mytab1");
//
//		RootAdvancement root1 = new RootAdvancement(tab1, "root1", new AdvancementDisplay(Material.RED_BED, "İkinci Tab", AdvancementFrameType.CHALLENGE, false, false, 0, 0), "textures/block/copper_block.png");
//		MineAdv pickaxe1 = new MineAdv("pickaxe1", new AdvancementDisplay(Material.IRON_INGOT, "Mine Stone", AdvancementFrameType.CHALLENGE, true, true, 10f, 25f, "Mine 10 blocks of stone."), root1, 3);
//		tab1.registerAdvancements(root1, pickaxe1);
//
//		tab.automaticallyShowToPlayers();
//		tab1.automaticallyShowToPlayers();
//
////		RootAdvancement root1 = new RootAdvancement(tab, "root1", new AdvancementDisplay(Material.ACACIA_LEAVES, "Test Advancement", AdvancementFrameType.CHALLENGE, false, false, 0, 0), "textures/block/copper_block.png");
////		MineAdv pickaxe1 = new MineAdv("pickaxe1", new AdvancementDisplay(Material.DIAMOND, "Mine Stone", AdvancementFrameType.CHALLENGE, true, true, 1f, 0, "Mine 10 blocks of stone."), root, 10);
////		tab1.registerAdvancements(root1, pickaxe1);
//
//		// Show tab on player loading. Also grants the root
//
//		CommandFramework framework = new CommandFramework(this);
//		framework.registerCommands(this);
	}

	public UltimateAdvancementAPI getAdvancementAPI() {
		return api;
	}

	public UserManager getUserManager() {
		return userManager;
	}

	public BeginnerAdvancements getBeginnerAdvancements() {
		return beginnerAdvancements;
	}

	public static BetterAdvancements getInstance() {
		return instance;
	}
}