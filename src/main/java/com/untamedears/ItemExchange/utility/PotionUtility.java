package com.untamedears.ItemExchange.utility;

import org.bukkit.Color;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;

public class PotionUtility {

	public static final List<PotionType> VANILLA_EFFECTS = Arrays.asList(
			PotionType.NIGHT_VISION,
			PotionType.INVISIBILITY,
			PotionType.FIRE_RESISTANCE,
			PotionType.SPEED,
			PotionType.SLOWNESS,
			PotionType.WATER_BREATHING,
			PotionType.INSTANT_HEAL,
			PotionType.INSTANT_DAMAGE,
			PotionType.REGEN,
			PotionType.STRENGTH,
			PotionType.WEAKNESS,
			PotionType.LUCK
	);

	public static final Map<PotionType, String> VANILLA_EFFECT_NAMES = new HashMap<PotionType, String>() {{
		put(PotionType.NIGHT_VISION, "Night Vision");
		put(PotionType.INVISIBILITY, "Invisibility");
		put(PotionType.JUMP, "Leaping");
		put(PotionType.FIRE_RESISTANCE, "Fire Resistance");
		put(PotionType.SPEED, "Swiftness");
		put(PotionType.SLOWNESS, "Slow");
		put(PotionType.WATER_BREATHING, "Water Breathing");
		put(PotionType.INSTANT_HEAL, "Instant Heal");
		put(PotionType.INSTANT_DAMAGE, "Instant Damage");
		put(PotionType.POISON, "Poison");
		put(PotionType.REGEN, "Regeneration");
		put(PotionType.STRENGTH, "Strength");
		put(PotionType.WEAKNESS, "Weakness");
		put(PotionType.LUCK, "Luck");
	}};

	public static class PotDurations {
		public int standard;
		public int extended;
		public int amplified;
		public PotDurations(int standard, int extended, int amplified) {
			this.standard = standard;
			this.extended = extended;
			this.amplified = amplified;
		}
		public PotDurations(int all) {
			this.standard = all;
			this.extended = all;
			this.amplified = all;
		}
	}

	public static String getEffectTypeName(PotionEffectType effectType) {
		if (effectType == null) return "";
		String[] splitName = effectType.getName().split("_", -1);
		for (int i = 0; i < splitName.length; i++) {
			String namePart = splitName[i];
			if (namePart.length() > 0) {
				splitName[i] = namePart.substring(0, 1).toUpperCase();
				if (namePart.length() > 1) {
					splitName[i] += namePart.substring(1).toLowerCase();
				}
			}
		}
		return String.join(" ", splitName);
	}

	public static String getPotionNameByEffect(PotionData potionData) {
		PotionType potionType = potionData.getType();
		boolean extended = potionData.isExtended() && potionType.isExtendable();
		boolean upgraded = potionData.isUpgraded() && potionType.isUpgradeable();
		// Uncraftable Potion
		if (potionType.equals(PotionType.UNCRAFTABLE)) {
			return "Uncraftable Potion";
		}
		// Water Bottle
		else if (potionType.equals(PotionType.WATER)) {
			return "Water Bottle";
		}
		// Mundane Potion
		else if (potionType.equals(PotionType.MUNDANE)) {
			return "Mundane Potion";
		}
		// Thick Potion
		else if (potionType.equals(PotionType.THICK)) {
			return "Thick Potion";
		}
		// Awkward Potion
		else if (potionType.equals(PotionType.AWKWARD)) {
			return "Awkward Potion";
		}
		// Night Vision
		else if (potionType.equals(PotionType.NIGHT_VISION)) {
			return "Potion of Night Vision";
		}
		// Invisibility
		else if (potionType.equals(PotionType.INVISIBILITY)) {
			return "Potion of Invisibility";
		}
		// Leaping
		else if (potionType.equals(PotionType.JUMP)) {
			if (upgraded) return "Potion of Leaping II";
			else return "Potion of Leaping";
		}
		// Fire Resistance
		else if (potionType.equals(PotionType.FIRE_RESISTANCE)) {
			return "Potion of Fire Resistance";
		}
		// Swiftness
		else if (potionType.equals(PotionType.SPEED)) {
			if (upgraded) return "Potion of Swiftness II";
			else return "Potion of Swiftness";
		}
		// Slowness
		else if (potionType.equals(PotionType.SLOWNESS)) {
			return "Potion of Slow";
		}
		// Water Breathing
		else if (potionType.equals(PotionType.WATER_BREATHING)) {
			return "Potion of Water Breathing";
		}
		// Instant Health
		else if (potionType.equals(PotionType.INSTANT_HEAL)) {
			if (upgraded) return "Potion of Healing II";
			else return "Potion of Healing";
		}
		// Harming
		else if (potionType.equals(PotionType.INSTANT_DAMAGE)) {
			if (upgraded) return "Potion of Harming II";
			else return "Potion of Harming";
		}
		// Poison
		else if (potionType.equals(PotionType.POISON)) {
			if (upgraded) return "Potion of Poison II";
			else return "Potion of Poison";
		}
		// Regeneration
		else if (potionType.equals(PotionType.REGEN)) {
			if (upgraded) return "Potion of Regeneration II";
			else return "Potion of Regeneration";
		}
		// Strength
		else if (potionType.equals(PotionType.STRENGTH)) {
			if (upgraded) return "Potion of Strength II";
			else return "Potion of Strength";
		}
		// Weakness
		else if (potionType.equals(PotionType.WEAKNESS)) {
			return "Potion of Weakness";
		}
		// Luck
		else if (potionType.equals(PotionType.LUCK)) {
			return "Potion of Luck";
		}
		// NONE OF THE ABOVE
		else {
			return "Unknown Potion";
		}
	}

	public static PotDurations getVanillaEffectLength(PotionType potion) {
		// If not a vanilla potion, return -1
		if (!VANILLA_EFFECTS.contains(potion)) return new PotDurations(-1);
		// Get the effect type
		PotionEffectType effectType = potion.getEffectType();
		// If the effect is instant, return 0
		if (effectType.isInstant()) return new PotDurations(0);
		// Go through
		switch (potion) {
			case NIGHT_VISION: return new PotDurations(180, 480, -1);
			case INVISIBILITY: return new PotDurations(180, 480, -1);
			case JUMP: return new PotDurations(180, 480, 90);
			case FIRE_RESISTANCE: return new PotDurations(180, 480, -1);
			case SPEED: return new PotDurations(180, 480, 90);
			case SLOWNESS: return new PotDurations(90, 240, -1);
			case WATER_BREATHING: return new PotDurations(180, 480, -1);
			case POISON: return new PotDurations(45, 90, 21);
			case REGEN: return new PotDurations(45, 90, 21);
			case STRENGTH: return new PotDurations(180, 480, 90);
			case WEAKNESS: return new PotDurations(90, 240, -1);
			case LUCK: return new PotDurations(300, -1, -1);
			default: return new PotDurations(-1);
		}
	}

	public static String colourToString(Color colour) {
		try {
			return Integer.toHexString(colour.asRGB());
		}
		catch (Exception e) {
			return "";
		}
	}

	public static Color stringToColour(String colour) {
		try {
			return Color.fromRGB(Integer.parseInt(colour, 16));
		}
		catch (Exception e) {
			return null;
		}
	}

	public static PotionEffect getBaseEffect(PotionMeta meta) {
		PotionData basePotion = meta.getBasePotionData();
		PotionType basePotType = basePotion.getType();
		// If base potion effect isn't vanilla, then return nothing
		if (!VANILLA_EFFECTS.contains(basePotType)) return null;
		// Get the length of this base potion effect
		PotDurations durations = getVanillaEffectLength(basePotType);
		int duration = basePotion.isExtended() ? durations.extended :
				basePotion.isUpgraded() ? durations.amplified : durations.standard;
		duration *= 20; // Convert seconds to ticks
		// Get the amplified value of this effect
		int amplifier = basePotion.isUpgraded() ? 2 : 1;
		// Get the effect type of the effect
		PotionEffectType effectType = basePotType.getEffectType();
		// Extract the colour from the type
		Color colour = effectType.getColor();
		// And return the effect
		return new PotionEffect(
				effectType, duration, amplifier,
				true,true, colour);
	}

	public static List<PotionEffect> getCustomEffects(PotionMeta meta) {
		if (meta != null && meta.hasCustomEffects()) {
			return meta.getCustomEffects();
		}
		else return new ArrayList<>();
	}

	public static List<PotionEffect> getAllEffects(PotionMeta meta) {
		PotionEffect baseEffect = getBaseEffect(meta);
		List<PotionEffect> moreEffects = getCustomEffects(meta);
		moreEffects = new ArrayList<>(moreEffects);
		if (baseEffect != null) moreEffects.add(0, baseEffect);
		return moreEffects;
	}

	public static String[] serialiseEffect(PotionEffect effect) {
		// If no effect, return empty string
		if (effect == null) return null;
		// Otherwise begin the serialisation
		String[] result = new String[6];
		result[0] = effect.getType().getName();
		result[1] = String.valueOf(effect.getAmplifier());
		result[2] = String.valueOf(effect.getDuration());
		result[3] = String.valueOf(effect.isAmbient());
		result[4] = String.valueOf(effect.hasParticles());
		result[5] = colourToString(effect.getColor());
		return result;
	}

	public static String compileSerialisedEffect(String[] serialised, String splitter) {
		// If the array is not the correct length, or no splitter, then return nothing
		if (serialised == null || serialised.length != 6 || splitter == null) return null;
		// Join the array together using the splitter string
		return String.join(splitter, serialised);
	}

	public static PotionEffect deserialiseEffect(String serialised, String splitter) {
		// If either the serialised or the splitter is not present, return nothing
		if (serialised == null || splitter == null) return null;
		// Otherwise return the effect generated by the split data
		return deserialiseEffect(serialised.split(splitter, -1));
	}

	public static PotionEffect deserialiseEffect(String[] serialised) {
		// If the array is not the correct length, then return nothing
		if (serialised == null || serialised.length != 6) return null;
		// Extract the data from the serialised array
		PotionEffectType type = PotionEffectType.getByName(serialised[0]);
		int amplifier = Integer.parseInt(serialised[1]);
		int duration = Integer.parseInt(serialised[2]);
		boolean ambient = Boolean.parseBoolean(serialised[3]);
		boolean particles = Boolean.parseBoolean(serialised[4]);
		Color colour = stringToColour(serialised[5]);
		// Return an effect generated with the extracted data
		return new PotionEffect(
				type, duration, amplifier,
				ambient, particles, colour);
	}
}
