package com.desperis.advancements.user;

import com.desperis.advancements.BetterAdvancements;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class User {

	private static final BetterAdvancements plugin = BetterAdvancements.getInstance();

	private final UUID uuid;
	private final Map<String, Integer> progress;

	public User(UUID uuid) {
		this.uuid = uuid;
		this.progress = new HashMap<>();
	}

	public Player getPlayer() {
		return plugin.getServer().getPlayer(uuid);
	}

	public UUID getUUID() {
		return uuid;
	}

	public void setProgress(String key, int value) {
		progress.put(key, value);
	}

	public int getProgress(String key) {
		return progress.getOrDefault(key, 0);
	}

	public void incrementProgress(String key) {
		progress.computeIfAbsent(key, k -> getProgress(k) + 1);
	}

	public boolean isFinishedProgress(String... keys) {
		for (String key : keys) {
			if (getProgress(key) == 0) return false;
		}

		return true;
	}
}
