package com.desperis.advancements.tiers;

import com.desperis.advancements.Advancements;
import com.desperis.advancements.BetterAdvancements;
import com.desperis.advancements.tiers.beginner.BeginnerAdvancements;
import com.desperis.advancements.user.UserManager;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;

/**
 * @author Despical
 * <p>
 * Created at 20.03.2025
 */
public abstract class BetterAdvancement {

	protected final RootAdvancement root;
	protected final Advancements advancements;
	protected final UserManager userManager;

	public BetterAdvancement(Advancements advancements) {
		this.root = BeginnerAdvancements.ROOT;
		this.advancements = advancements;
		this.userManager = BetterAdvancements.getInstance().getUserManager();

		BeginnerAdvancements.register(this, advancements);
	}

	public abstract BaseAdvancement getAdvancement();

	public Advancements getAdvancements() {
		return advancements;
	}
}
