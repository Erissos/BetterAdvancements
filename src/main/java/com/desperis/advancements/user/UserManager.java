package com.desperis.advancements.user;

import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public class UserManager {

	private final Map<UUID, User> users;

	public UserManager() {
		this.users = new HashMap<>();
	}

	public User addUser(Player player) {
		User user = new User(player.getUniqueId());
		users.put(player.getUniqueId(), user);

		return user;
	}

	public void removeUser(Player player) {
		users.remove(player.getUniqueId());
	}

	public User getUser(Player player) {
		User user = users.get(player.getUniqueId());

		if (user != null) {
			return user;
		}

		return addUser(player);
	}
}
