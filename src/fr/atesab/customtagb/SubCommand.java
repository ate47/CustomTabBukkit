package fr.atesab.customtagb;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public abstract class SubCommand implements CommandExecutor {
	public static String buildString(String[] array, int start) {
		return buildString(array, start, " ");
	}

	public static String buildString(String[] array, int start, String glue) {
		String s = "";
		for (int i = start; i < array.length; i++) {
			if (i > start)
				s += glue;
			s += array[i];
		}
		return s;
	}

	private String description;
	private final String name;
	private String permission;
	private String[] aliases;

	public SubCommand(String name, String permission, String description, String... aliases) {
		this.description = description;
		this.name = name;
		this.description = description;
		this.aliases = aliases;
	}

	public String getName() {
		return name;
	}

	public String getPermission() {
		return permission;
	}

	public String[] getAliases() {
		return aliases;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		return execute(sender, args, "", null);
	}

	public abstract boolean execute(CommandSender sender, String[] args, String main, CommandType type);

	public String getDescription() {
		return description;
	}

	public String getUsage(CommandSender sender) {
		return getName();
	}

	public List<String> onTabComplete(CommandSender sender, String[] args, CommandType type) {
		return null;
	}

	enum CommandType {
		footer, header;
	}
}