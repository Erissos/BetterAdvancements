package com.desperis.advancements.events;

import com.desperis.advancements.BetterAdvancements;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class GeneralEvents implements Listener {

	private final BetterAdvancements plugin;

	public GeneralEvents(BetterAdvancements plugin) {
		this.plugin = plugin;

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {

	}
}
