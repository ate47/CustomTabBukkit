package fr.atesab.customtagb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class SCAddLine extends SubCommand {
	private CustomTabPlugin plugin;

	public SCAddLine(CustomTabPlugin plugin) {
		super("addline", "ctp.gtab.addline", "Add a line", "a");
		this.plugin = plugin;
	}

	@Override
	public boolean execute(CommandSender sender, String[] args, String main, CommandType type) {
		if (args.length < 2 || !args[0].matches("[0-9]+"))
			return false;
		int line = Integer.valueOf(args[0]).intValue() - 1;
		String[] lines = (type == CommandType.footer ? CustomTabPlugin.getLocalFooter()
				: type == CommandType.header ? CustomTabPlugin.getLocalHeader() : "").split("\n");
		if (line >= 0 && line <= lines.length) {
			String[] newLines = new String[lines.length + 1];
			System.arraycopy(lines, 0, newLines, 0, line);
			System.arraycopy(lines, line, newLines, line + 1, lines.length - line);
			newLines[line] = buildString(args, 1);
			switch (type) {
			case footer:
				plugin.setFooter(buildString(newLines, 0, "\n"));
				break;
			case header:
				plugin.setHeader(buildString(newLines, 0, "\n"));
				break;
			}
			try {
				plugin.savePluginConfig();
				CustomTabPlugin.sendText(sender, new ComponentBuilder("Line ").color(ChatColor.GREEN).append(String.valueOf(line + 1))
						.color(ChatColor.YELLOW).append(" added.").color(ChatColor.GREEN).create());
			} catch (IOException e) {
				CustomTabPlugin.sendText(sender, 
						new ComponentBuilder("An error occurred while saving config.").color(ChatColor.RED).create());
				e.printStackTrace();
			}
		} else
			CustomTabPlugin.sendText(sender, new ComponentBuilder("This is not a valid line.").color(ChatColor.RED).create());
		return true;
	}

	@Override
	public String getUsage(CommandSender sender) {
		return super.getUsage(sender) + " <line> <text>";
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String[] args, CommandType type) {
		List<String> list = new ArrayList<>();
		plugin.getTextOptions().stream().filter(om -> om.canBeTabbed()).forEach(om -> om.getUsage());
		return list;
	}
}
