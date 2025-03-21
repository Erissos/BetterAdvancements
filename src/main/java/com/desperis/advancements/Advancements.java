package com.desperis.advancements;

import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public enum Advancements {

	FIRST_TOOL,
	ENTRANCE_TO_MINING,
	FIRST_MINER,
	FIRST_WEAPON,
	SMELT_IT_UP, FIRST_HUNT, FIRST_HARVEST, SURVIVAL_ESSENTIALS, START_DEFENDING, POTION_BASICS, COOKED_PREY, ISNT_IT_IRON_PICK, NOT_TODAY, ACQUIRE_DIAMONDS, FULL_OF_DIAMONDS, UNBREAKABLE_DEFENSE, UNDERGROUND_MASTER;

	private static final Map<Advancements, BaseAdvancement> ADVANCEMENTS = new EnumMap<>(Advancements.class);

	public static void registerAdvancement(Advancements advancements, BaseAdvancement baseAdvancement) {
		ADVANCEMENTS.put(advancements, baseAdvancement);
	}

	public static BaseAdvancement getAdvancement(Advancements advancements) {
		return ADVANCEMENTS.get(advancements);
	}

	public static Set<BaseAdvancement> getAdvancements() {
		return Set.copyOf(ADVANCEMENTS.values());
	}
}
