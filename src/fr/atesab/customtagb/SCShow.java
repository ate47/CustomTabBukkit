package fr.atesab.customtagb;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;

public class SCShow extends SubCommand {

	public SCShow() {
		super("show", "ctp.gtab.show", "Show tab part");
	}

	@Override
	public boolean execute(CommandSender sender, String[] args, String main, CommandType type) {
		if (args.length != 0)
			return false;
		String text = (type == CommandType.footer ? CustomTabPlugin.getLocalFooter()
				: type == CommandType.header ? CustomTabPlugin.getLocalHeader() : "");
		String[] lines = text.split("\n");
		CustomTabPlugin.sendText(sender, new ComponentBuilder(
				type.name().substring(0, 1).toUpperCase().concat(type.name().substring(1).toLowerCase())).bold(true)
						.color(ChatColor.RED)
						.event(new HoverEvent(Action.SHOW_TEXT, sender instanceof Player
								? new ComponentBuilder(CustomTabPlugin.getOptionnedText(text, (Player) sender))
										.reset().create()
								: new ComponentBuilder("Connect as a player to view render").color(ChatColor.RED)
										.create()))
						.append(" (").reset().color(ChatColor.DARK_GRAY)
						.append(lines.length + " line" + (lines.length > 1 ? "s" : "")).color(ChatColor.AQUA)
						.append(")").color(ChatColor.DARK_GRAY).append(": ").bold(true).color(ChatColor.RED).create());
		int i;
		for (i = 0; i < lines.length; i++) {
			ComponentBuilder builder = new ComponentBuilder("");
			if (sender instanceof Player)
				// add a line
				builder.append("[A]").color(ChatColor.GREEN).bold(true)
						.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
								"/" + main + " " + type.name().substring(0, 1) + " a " + (i + 1) + " "))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
										.append("Click to add a line").color(ChatColor.YELLOW).create()))
						// edit the line
						.append("[E]").color(ChatColor.AQUA).bold(true)
						.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
								"/" + main + " " + type.name().substring(0, 1) + " s " + (i + 1) + " " + lines[i]))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
										.append("Click to edit the line").color(ChatColor.YELLOW).create()))
						// delete the line
						.append("[X]").color(ChatColor.RED).bold(true)
						.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
								"/" + main + " " + type.name().substring(0, 1) + " d " + (i + 1)))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
										.append("Click to delete the line").color(ChatColor.YELLOW).create()))
						.append(" ").reset();
			builder.append(String.valueOf(i + 1)).color(ChatColor.LIGHT_PURPLE).bold(true);
			if (sender instanceof Player)
				builder.event(new HoverEvent(Action.SHOW_TEXT,
						new ComponentBuilder(CustomTabPlugin.getOptionnedText(lines[i], (Player) sender)).reset()
								.create()));
			builder.append(": ").color(ChatColor.GRAY).append(lines[i]).color(ChatColor.WHITE).bold(false);
			CustomTabPlugin.sendText(sender, builder.create());
		}
		// add a line
		if (sender instanceof Player)
			CustomTabPlugin.sendText(sender, new ComponentBuilder(">> ").reset().bold(true).color(ChatColor.DARK_GRAY)
					.append("Add a line").color(ChatColor.YELLOW).bold(true)
					.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
							"/" + main + " " + type.name() + " addline " + (i + 1) + " "))
					.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(">> ").reset().bold(true)
							.color(ChatColor.DARK_GRAY).append("Click to add a line").color(ChatColor.YELLOW).create()))
					.create());
		return true;
	}

}
