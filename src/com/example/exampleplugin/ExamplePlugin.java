package com.example.exampleplugin;

import java.util.function.Function;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.atesab.customtagb.CustomTabPlugin;

public class ExamplePlugin extends JavaPlugin {
	@Override
	public void onEnable() {
		// Search CustomTabBukkit
		if (getServer().getPluginManager().getPlugin("CustomTabBukkit") != null) {
			
			// Register a new text option with the name "myOption" (usable with %myOption%)
			
			CustomTabPlugin.registerTextOption("myOption", p -> p.getGameMode().name());
			
			// The same text option in a other code style
			
			CustomTabPlugin.registerTextOption("myOption", new Function<Player, String>() {
				@Override
				public String apply(Player p) {
					return p.getGameMode().name();
				}
			});
		} else
			System.err.println("CustomTabBukkit not installed");
		super.onEnable();
	}
}
