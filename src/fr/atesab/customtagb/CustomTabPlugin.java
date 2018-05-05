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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

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
	 */
	public static final String CHANNEL_NAME = "CustomTab";
	/**
	 * Rate between every tab change in tick (1/20s)
	 */
	public static final long REFRESH_RATE = 1000L;
	private static String footer = "";
	private static String header = "";
	private static boolean bungeecord = false;
	private static Map<String, Function<Player, String>> textOptions = new HashMap<>();

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
	 */
	public static boolean isBungeecord() {
		return bungeecord;
	}

	/**
	 * get the raw local text footer
	 * 
	 * @return raw footer
	 */
	public static String getLocalFooter() {
		return footer;
	}

	/**
	 * get the raw local text header
	 * 
	 * @return raw header
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
	 */
	public static String getOptionnedText(String raw, Player player) {
		for (String key : textOptions.keySet())
			raw = raw.replaceAll("%" + key + "%", textOptions.get(key).apply(player));
		return raw;
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

	Map<String, Function<Player, String>> getTextOptions() {
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
	 * @param name
	 *            name of the option to add
	 * @param option
	 *            text option associated with the specified name
	 * @throws IllegalArgumentException
	 *             if the name does contain a non-alphanumerics characters
	 */
	public static void registerTextOption(String name, Function<Player, String> option) {
		if (!name.matches("[A-Za-z0-9\\_]*"))
			throw new IllegalArgumentException("name can only contain alphanumerics characters");
		textOptions.put(name, option);
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
		textOptions.put("localdisplayname", p -> p.getDisplayName());
		textOptions.put("localname", p -> p.getName());
		textOptions.put("worldname", p -> p.getWorld().getName());
		textOptions.put("localmotd", p -> Bukkit.getMotd());
		textOptions.put("localversion", p -> Bukkit.getBukkitVersion());
		textOptions.put("localservername", p -> Bukkit.getServerName());
		textOptions.put("localmaxplayer", p -> String.valueOf(Bukkit.getMaxPlayers()));
		textOptions.put("localplayerscounts", p -> String.valueOf(Bukkit.getOnlinePlayers().size()));
		textOptions.put("x", p -> String.valueOf(p.getLocation().getBlockX()));
		textOptions.put("y", p -> String.valueOf(p.getLocation().getBlockY()));
		textOptions.put("z", p -> String.valueOf(p.getLocation().getBlockZ()));
		textOptions.put("px", p -> String.valueOf(p.getLocation().getX()));
		textOptions.put("py", p -> String.valueOf(p.getLocation().getY()));
		textOptions.put("pz", p -> String.valueOf(p.getLocation().getZ()));
		textOptions.put("lping", p -> {
			int ping = 0;
			try {
				ping = (int) getField("ping",
						getMethod("getHandle", getCraftBukkitClass("entity.CraftPlayer").cast(p)));
			} catch (Exception e) {
			}
			return String.valueOf(ping);
		});
		textOptions.put("lcping", p -> {
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
		textOptions.put("tps", p -> {
			try {
				Object server = getNMSClass("MinecraftServer").getMethod("getServer").invoke(null);
				double tps = ((double[]) server.getClass().getField("recentTps").get(server))[0];
				return tps > 20.0D ? "*20.0" : significantNumbers(tps, 3);
			} catch (Exception e) {
				return "";
			}
		});
		textOptions.put("ldate", p -> new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
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
