package fr.atesab.customtagb;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

public class OptionMatcher {

	private Pattern pattern;
	private String usage;
	private BiFunction<Player, Matcher, String> function;
	private BiFunction<Player, OptionMatcher, String> exampleFunction;
	private boolean canBeTabbed;

	public OptionMatcher(Pattern pattern, String usage, BiFunction<Player, Matcher, String> function,
			BiFunction<Player, OptionMatcher, String> exampleFunction, boolean canBeTabbed) {
		this.pattern = pattern;
		this.usage = usage;
		this.function = function;
		this.exampleFunction = exampleFunction;
		this.canBeTabbed = canBeTabbed;
	}

	public boolean canBeTabbed() {
		return canBeTabbed;
	}

	public BiFunction<Player, OptionMatcher, String> getExampleFunction() {
		return exampleFunction;
	}

	public BiFunction<Player, Matcher, String> getFunction() {
		return function;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public String getUsage() {
		return usage;
	}

	public void setFunction(BiFunction<Player, Matcher, String> function) {
		this.function = function;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}
}
