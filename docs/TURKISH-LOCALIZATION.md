# Turkish presentation and default upgrades

The UTF-8 Turkish catalog includes concise, screen-specific navigation and help, numbered leaderboard badges, accurate reward descriptions, objective verbs, and a clear prestige-reset warning. Standard material and entity labels use native Minecraft names in the player's selected plugin language. Explicitly customized `values.*` labels retain priority.

The existing `lang-legacy/tr.yml` upgrade remains supported. The additional `lang-legacy/tr-polish.yml` snapshot identifies the previous bundled menu translations. Only string or lore-list values that exactly match a known old default are upgraded in memory. Customized values are retained, and the existing language file is not rewritten. Translation keys, placeholders, technical objective IDs and reward definitions are unchanged.

The language control remains in the settings screen and cycles through the installed plugin languages. Menu text refreshes in the selected profile language. The regular leaderboard sorts by completed advancements first; the season leaderboard displays season points.
