package fr.atesab.customtagb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.GsonBuilder;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class CustomTabPlugin extends JavaPlugin implements PluginMessageListener {
	/**
	 * Channel used by CustomTab to send/receive Bungee tab
	 * 
	 * @since 1.1
	 */
	public static final String CHANNEL_NAME = "CustomTab";
	/**
	 * Rate between every tab change in tick (1/20s)
	 * 
	 * @since 1.1
	 */
	public static final long REFRESH_RATE = 1000L;
	private static String footer = "";
	private static String header = "";
	private static boolean bungeecord = false;
	/**
	 * Normal option name regex pattern to check if a normal text option has a valid
	 * name
	 * 
	 * @since 1.3
	 */
	public static final String NORMAL_OPTION_NAME_PATTERN = "[A-Za-z0-9\\_]*";
	private static List<OptionMatcher> textOptions = new ArrayList<>();

	private static File createDir(File dir) {
		dir.mkdirs();
		return dir;
	}

	private static File createDir(String dir) {
		return createDir(new File(dir));
	}

	private static File createFile(File file, String defaultContent) throws IOException {
		if (!file.exists()) {
			BufferedWriter bw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
			bw.write(defaultContent);
			bw.close();
		}
		return file;
	}

	private static Class<?> getCraftBukkitClass(String name) {
		return getSClass("org.bukkit.craftbukkit", name);
	}

	private static Object getField(String name, Object object) throws Exception {
		return object.getClass().getField(name).get(object);
	}

	/**
	 * @return if CustomTab asked by a Bungee proxy or not for local information
	 * @since 1.2
	 */
	public static boolean isBungeecord() {
		return bungeecord;
	}

	/**
	 * get the raw local text footer
	 * 
	 * @return raw footer
	 * @since 1.2
	 */
	public static String getLocalFooter() {
		return footer;
	}

	/**
	 * get the raw local text header
	 * 
	 * @return raw header
	 * @since 1.2
	 */
	public static String getLocalHeader() {
		return header;
	}

	private static Object getMethod(String name, Object object) throws Exception {
		return getMethod(name, object, new Class<?>[] {}, new Object[] {});
	}

	private static Object getMethod(String name, Object object, Class<?>[] parameterTypes, Object[] parameters)
			throws Exception {
		return object.getClass().getMethod(name, parameterTypes).invoke(object, parameters);
	}

	private static Class<?> getNMSClass(String name) {
		try {
			return Class.forName("net.minecraft.server."
					+ Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Replace in a raw text the Bungee text options
	 * 
	 * @param raw
	 *            raw text data
	 * @param player
	 *            player to base information
	 * @return the text with options
	 * @since 1.1
	 * @see #registerTextOption(Pattern, String, BiFunction, BiFunction, boolean) to
	 *      register new options
	 */
	public static String getOptionnedText(String raw, Player player) {
		StringBuffer buffer = new StringBuffer(raw);
		textOptions.forEach(option -> {
			Matcher matcher = option.getPattern().matcher(buffer.toString());
			buffer.setLength(0);
			while (matcher.find()) {
				String result;
				try {
					result = option.getFunction().apply(player, matcher);
				} catch (Exception e) {
					e.printStackTrace();
					result = "*error*";
				}
				matcher.appendReplacement(buffer, result);
			}
			matcher.appendTail(buffer);
		});
		return buffer.toString().replace('&', ChatColor.COLOR_CHAR);
	}

	private static Class<?> getSClass(String type, String name) {
		try {
			return Class.forName(
					type + "." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	List<OptionMatcher> getTextOptions() {
		return textOptions;
	}

	private static String loadFile(File file) throws IOException {
		String s = "";
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
		String line;
		while ((line = br.readLine()) != null)
			s += (s.isEmpty() ? "" : "\n") + line;
		br.close();
		return s;
	}

	/**
	 * Register a new text option for this plugin
	 * 
	 * @param optionMatcher
	 *            option matcher to match and evaluate the text
	 * @see #registerTextOption(OptionMatcher)
	 * @since 1.3
	 */
	public static void registerTextOption(OptionMatcher optionMatcher) {
		textOptions.removeIf(op -> op.getPattern().toString().equals(optionMatcher.getPattern().toString()));
		textOptions.add(optionMatcher);
	}

	/**
	 * Register a new text option for this plugin
	 * 
	 * @param pattern
	 *            pattern to match the option
	 * @param usage
	 *            the usage of this pattern
	 * @param option
	 *            text option associated
	 * @since 1.3
	 * @see #registerTextOption(Pattern, String, BiFunction, BiFunction, boolean)
	 */
	public static void registerTextOption(Pattern pattern, String usage, BiFunction<Player, Matcher, String> option) {
		registerTextOption(pattern, usage, option, null, false);
	}

	/**
	 * Register a new text option for this plugin
	 * 
	 * @param pattern
	 *            pattern to match the option
	 * @param usage
	 *            the usage of this pattern
	 * @param option
	 *            text option associated
	 * @param exampleFunction
	 *            the example to show in the tab command opt list
	 * @param canBeTabbed
	 *            if in the tab command the option usage can be get with tab
	 * @since 1.3
	 */

	public static void registerTextOption(Pattern pattern, String usage, BiFunction<Player, Matcher, String> option,
			BiFunction<Player, OptionMatcher, String> exampleFunction, boolean canBeTabbed) {
		registerTextOption(new OptionMatcher(pattern, usage, option, exampleFunction, canBeTabbed));
	}

	/**
	 * Register a new text option for this plugin
	 * 
	 * @param name
	 *            name of the option to add
	 * @param option
	 *            text option associated with the specified name
	 * @throws IllegalArgumentException
	 *             if the name does contain non-alphanumerics characters
	 * @since 1.1
	 * @see #registerTextOption(Pattern, String, BiFunction)
	 */
	public static void registerTextOption(String name, Function<Player, String> option) {
		if (!name.matches(NORMAL_OPTION_NAME_PATTERN))
			throw new IllegalArgumentException("name can only contain alphanumerics characters");
		String usage = "%" + name + "%";
		registerTextOption(Pattern.compile(usage), usage, (p, m) -> option.apply(p),
				(p, om) -> om.getFunction().apply(p, null), true);
	}

	/**
	 * Send a tab to a player
	 * 
	 * @param player
	 *            player to send the tab
	 * @param header
	 *            header text
	 * @param footer
	 *            footer text
	 * @since 1.1
	 */
	public static void sendTab(Player player, String footer, String header) {
		try {
			Object packet = getNMSClass("PacketPlayOutPlayerListHeaderFooter").newInstance();
			setField("a", packet, new ChatComponentBuilder(header).buildChatBaseComponent());
			setField("b", packet, new ChatComponentBuilder(footer).buildChatBaseComponent());
			getMethod("sendPacket",
					getField("playerConnection",
							getMethod("getHandle", (getCraftBukkitClass("entity.CraftPlayer").cast(player)))),
					new Class<?>[] { getNMSClass("Packet") }, new Object[] { packet });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send tab information to a proxy/player
	 * 
	 * @param plugin
	 *            the plugin who send the tab information
	 * @param player
	 *            player to send the tab
	 * @param globalFooter
	 *            global header text
	 * @param globalHeader
	 *            global footer text
	 * @throws IllegalArgumentException
	 *             if the channel {@link CustomTabPlugin#CHANNEL_NAME} isn't
	 *             registered for the plugin
	 * @see {@link CustomTabPlugin#sendTab(Player, String, String)} to send a tab
	 * @since 1.1
	 */
	public static void sendTabInformation(JavaPlugin plugin, Player player, String footer, String header) {
		if (!plugin.getServer().getMessenger().isOutgoingChannelRegistered(plugin, CHANNEL_NAME))
			throw new IllegalArgumentException("The channel isn't registered for this plugin");
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		Map<String, Object> hm = new HashMap<String, Object>();
		hm.put("footer", footer);
		hm.put("header", header);
		out.writeUTF(new GsonBuilder().create().toJson(hm));
		player.sendPluginMessage(plugin, CHANNEL_NAME, out.toByteArray());
	}

	private static void setField(String name, Object object, Object value) throws Exception {
		Field f = object.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(object, value);
	}

	private static File setFile(File file, String value) throws IOException {
		BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
		bw.write(value);
		bw.close();
		return file;
	}

	static long getSystemTimeInSecond() {
		return System.currentTimeMillis() / 1000L;
	}

	void setFooter(String footer) {
		CustomTabPlugin.footer = footer;
	}

	void setHeader(String header) {
		CustomTabPlugin.header = header;
	}

	private static String significantNumbers(double d, int n) {
		String s = String.format("%." + n + "G", d);
		if (s.contains("E+")) {
			s = String.format(Locale.US, "%.0f", Double.valueOf(String.format("%." + n + "G", d)));
		}
		return s;
	}

	void loadConfigs() throws IOException {
		File d = createDir("plugins/" + getName());
		File header = createFile(new File(d, "header.cfg"), "");
		File footer = createFile(new File(d, "footer.cfg"), "");
		if (footer.exists() && header.exists()) {
			CustomTabPlugin.footer = loadFile(footer);
			CustomTabPlugin.header = loadFile(header);
		} else
			throw new FileNotFoundException();
		bungeecord = getConfig().getBoolean("bungeecord");
		getConfig().options().copyDefaults(false);
		saveConfig();
	}

	@Override
	public void onDisable() {
		if (!bungeecord)
			getServer().getScheduler().cancelTasks(this);
		super.onDisable();
	}

	@Override
	public void onEnable() {
		getConfig().options().copyDefaults(false);
		saveConfig();
		try {
			loadConfigs();
		} catch (IOException e) {
			e.printStackTrace();
		}
		registerTextOption("localdisplayname", p -> p.getDisplayName());
		registerTextOption("localname", p -> p.getName());
		registerTextOption("worldname", p -> p.getWorld().getName());
		registerTextOption("localmotd", p -> Bukkit.getMotd());
		registerTextOption("localversion", p -> Bukkit.getBukkitVersion());
		registerTextOption("localservername", p -> Bukkit.getServerName());
		registerTextOption("localmaxplayer", p -> String.valueOf(Bukkit.getMaxPlayers()));
		registerTextOption("localplayerscounts", p -> String.valueOf(Bukkit.getOnlinePlayers().size()));
		registerTextOption("x", p -> String.valueOf(p.getLocation().getBlockX()));
		registerTextOption("y", p -> String.valueOf(p.getLocation().getBlockY()));
		registerTextOption("z", p -> String.valueOf(p.getLocation().getBlockZ()));
		registerTextOption("px", p -> String.valueOf(p.getLocation().getX()));
		registerTextOption("py", p -> String.valueOf(p.getLocation().getY()));
		registerTextOption("pz", p -> String.valueOf(p.getLocation().getZ()));
		registerTextOption("lping", p -> {
			int ping = 0;
			try {
				ping = (int) getField("ping",
						getMethod("getHandle", getCraftBukkitClass("entity.CraftPlayer").cast(p)));
			} catch (Exception e) {
			}
			return String.valueOf(ping);
		});
		registerTextOption("lcping", p -> {
			int ping = 0;
			try {
				ping = (int) getField("ping",
						getMethod("getHandle", getCraftBukkitClass("entity.CraftPlayer").cast(p)));
			} catch (Exception e) {
			}
			return (ping < 0 ? ChatColor.WHITE
					: (ping < 150 ? ChatColor.DARK_GREEN
							: (ping < 300 ? ChatColor.GREEN
									: (ping < 600 ? ChatColor.GOLD
											: (ping < 1000 ? ChatColor.RED : ChatColor.DARK_RED))))).toString()
					+ String.valueOf(ping);
		});
		registerTextOption("tps", p -> {
			try {
				Object server = getNMSClass("MinecraftServer").getMethod("getServer").invoke(null);
				double tps = ((double[]) server.getClass().getField("recentTps").get(server))[0];
				return tps > 20.0D ? "*20.0" : significantNumbers(tps, 3);
			} catch (Exception e) {
				return "";
			}
		});
		registerTextOption(Pattern.compile("%ldate((-.+)?){1}%"), "%ldate%", (p, m) -> {
			String format = m.group(1);
			return new SimpleDateFormat(format.isEmpty() ? "HH:mm:ss" : format.substring(1))
					.format(Calendar.getInstance().getTime());
		}, (p, om) -> new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()), true);
		char[] ALL_COLORS = { 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
		char[] LIGHT_COLOR = { 'a', 'b', 'c', 'd', 'e', 'f' };
		char[] HEAVY_COLORS = { '2', '3', '4', '5', '6', '7' };
		char[] FORMAT = { 'o', 'l', 'm', 'n', 'r', 'k' };

		registerTextOption(Pattern.compile("%s(wap)?(((-[a-fA-F0-9k-oK-OrR]+)?)|l(ight)?|f(ormat)?|h(eavy)?)?%"),
				"%swap%|%swap-<colors>%|%swapheavy%|%swaplight%|%swapformat%", (p, m) -> {
					String format = m.group(2);
					char[] chars = format.isEmpty() ? ALL_COLORS
							: format.toLowerCase().matches("h(eavy)?") ? HEAVY_COLORS
									: format.toLowerCase().matches("l(ight)?") ? LIGHT_COLOR
											: format.toLowerCase().matches("f(ormat)?") ? FORMAT
													: format.substring(1).toCharArray();
					return new String(
							new char[] { ChatColor.COLOR_CHAR, chars[(int) (getSystemTimeInSecond() % chars.length)] });
				});

		// I use add(0, ...) to allow options swapping
		textOptions.add(0, new OptionMatcher(Pattern.compile("%s(wap)?t(ext)?(-[0-9]+)?-(.+)?%"),
				"%swaptext(-delay)?-text1;;text2;;...%", (p, m) -> {
					String d = m.group(3);
					String[] formats = m.group(4).split(";;");
					return formats[(int) ((getSystemTimeInSecond()
							/ Math.max(1, d.isEmpty() ? 1L : Long.valueOf(d.substring(1)).longValue()))
							% formats.length)];
				}, null, false));
		if (bungeecord) {
			getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL_NAME);
			getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL_NAME, this);
		} else {
			getServer().getScheduler()
					.runTaskTimer(this,
							() -> getServer().getOnlinePlayers().forEach(p -> sendTab(p,
									getOptionnedText(CustomTabPlugin.footer.replace('&', ChatColor.COLOR_CHAR), p),
									getOptionnedText(CustomTabPlugin.header.replace('&', ChatColor.COLOR_CHAR), p))),
							0, 20);
		}
		PluginCommand pc = getServer().getPluginCommand("ltab");
		CustomTabCommand command = new CustomTabCommand(this);
		pc.setExecutor(command);
		pc.setTabCompleter(command);
		super.onEnable();
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (channel.equals(CHANNEL_NAME)) {
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(message));
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> hm = new GsonBuilder().create().fromJson(dis.readUTF(), HashMap.class);
				Player p = Bukkit.getPlayer(hm.containsKey("player") ? String.valueOf(hm.get("player")) : "");
				sendTabInformation(this, p,
						getOptionnedText((hm.containsKey("footer") ? String.valueOf(hm.get("footer")) : "")
								.replaceAll("%bukkitfootermessage%", footer), player),
						getOptionnedText((hm.containsKey("header") ? String.valueOf(hm.get("header")) : "")
								.replaceAll("%bukkitheadermessage%", header), player));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	void savePluginConfig() throws IOException {
		File d = createDir("plugins/" + getName());
		setFile(new File(d, "header.cfg"), footer);
		setFile(new File(d, "footer.cfg"), header);

		getConfig().set("bungeecord", bungeecord);
		saveConfig();
	}

	static void sendText(CommandSender sender, BaseComponent... components) {
		if (sender instanceof Player) {
			try {
				getMethod("sendMessage", getMethod("getHandle", getCraftBukkitClass("entity.CraftPlayer").cast(sender)),
						new Class<?>[] { getNMSClass("IChatBaseComponent") },
						new Object[] { getNMSClass("IChatBaseComponent").getDeclaredClasses()[0]
								.getMethod("a", String.class).invoke(null, ComponentSerializer.toString(components)) });
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (getCraftBukkitClass("command.ColouredConsoleSender").isInstance(sender)) {
			sender.sendMessage(TranslatableComponent.toLegacyText(components));
		} else {
			sender.sendMessage(TranslatableComponent.toPlainText(components));
		}
	}

}
