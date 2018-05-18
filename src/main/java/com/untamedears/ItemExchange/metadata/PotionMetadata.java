package com.untamedears.ItemExchange.metadata;

import java.util.ArrayList;
import java.util.List;

import com.untamedears.ItemExchange.ItemExchangePlugin;
import com.untamedears.ItemExchange.utility.ExchangeRule;
import com.untamedears.ItemExchange.utility.PotionUtility;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

public class PotionMetadata implements AdditionalMetadata {
	// ------------------------------------------------------------
	//					  Static Functionality
	// ------------------------------------------------------------
	public static String EffectSpacer = ExchangeRule.GetAppropriateSpacer(2);
	public static String DetailSpacer = ExchangeRule.GetAppropriateSpacer(3);

	public static PotionMetadata deserialize(String code) {
		PotionMetadata metadata = new PotionMetadata();
		String[] effects = code.split(EffectSpacer);
		for (int i = 0; i < effects.length; i++) {
			// Localise this effect
			String element = effects[i];
			// If element is empty, skip
			if (element.isEmpty()) continue;
			// Get the effect from the data
			PotionEffect effect = PotionUtility.deserialiseEffect(element, DetailSpacer);
			// If the effect is null, skip
			if (effect == null) continue;
			// If it's the 0th element, then it's the base effect
			else if (i == 0) metadata.baseEffect = effect;
			// Otherwise add to list of custom effects
			else metadata.customEffects.add(effect);
		}
		return metadata;
	}

	// ------------------------------------------------------------
	//					 Instance Functionality
	// ------------------------------------------------------------
	private PotionEffect       baseEffect;
	private List<PotionEffect> customEffects;

	private PotionMetadata() {
		this.baseEffect = null;
		this.customEffects = new ArrayList<>();
	}

	public PotionMetadata(PotionMeta meta) {
		this.baseEffect = PotionUtility.getBaseEffect(meta);
		this.customEffects = PotionUtility.getCustomEffects(meta);
	}

	public List<PotionEffect> getAllEffects() {
		List<PotionEffect> effects = new ArrayList<>(customEffects);
		if (baseEffect != null) effects.add(0, baseEffect);
		return effects;
	}

	@Override
	public String serialize() {
		StringBuilder serialized = new StringBuilder();
		// Add the base effect
		if (baseEffect != null) {
			String[] data = PotionUtility.serialiseEffect(baseEffect);
			if (data != null) {
				String encode = PotionUtility.compileSerialisedEffect(data, DetailSpacer);
				if (encode != null) {
					serialized.append(encode);
				}
			}
		}
		serialized.append(EffectSpacer);
		// Add custom effects
		for (PotionEffect effect : customEffects) {
			String[] data = PotionUtility.serialiseEffect(effect);
			if (data != null) {
				String encode = PotionUtility.compileSerialisedEffect(data, DetailSpacer);
				if (encode != null) {
					serialized.append(encode);
				}
			}
			serialized.append(EffectSpacer);

		}
		// And return
		return serialized.toString();
	}

	@Override
	public boolean matches(ItemStack item) {
		if(item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			if(meta instanceof PotionMeta) {
				List<PotionEffect> itemEffects = PotionUtility.getAllEffects((PotionMeta) meta);
				List<PotionEffect> shopEffects = getAllEffects();
				return itemEffects.equals(shopEffects);
			}
		}
		return false;
	}

	@Override
	public String getDisplayedInfo() {
		StringBuilder info = new StringBuilder().append(ChatColor.DARK_AQUA);
		for (PotionEffect effect : getAllEffects()) {
			// Add the effect name
			info.append(PotionUtility.getEffectTypeName(effect.getType())).append(" ");
			// Add the effect level
			int level = effect.getAmplifier();
			if (level > 0 && level - 1 < ItemExchangePlugin.NUMERALS.length)
				info.append(ItemExchangePlugin.NUMERALS[level - 1]);
			else
				info.append(level);
			// Add the duration
			info.append(" (");
			int duration = effect.getDuration() / 20;
			String minutes = String.valueOf(duration / 60);
			String seconds = String.valueOf(duration % 60);
			if (seconds.length() == 1) seconds = "0" + seconds;
			else if (seconds.length() == 0) seconds = "00";
			info.append(minutes + ":" + seconds);
			info.append(")\n");
		}
		return info.toString();
	}

	@Override
	public String toString() {
		StringBuilder info = new StringBuilder();
		info.append("Potion MetaData:\n");
		// Show Base Effect
		info.append("\tBase Effect: ");
		if (baseEffect == null) info.append("<none>\n");
		else {
			String[] serEff = PotionUtility.serialiseEffect(baseEffect);
			info.append(PotionUtility.compileSerialisedEffect(serEff, DetailSpacer));
			info.append('\n');
		}
		// Show Custom Effects
		info.append("\tCustom Effects: \n");
		for (PotionEffect effect : customEffects) {
			info.append("\t\t");
			String[] serEff = PotionUtility.serialiseEffect(effect);
			info.append(PotionUtility.compileSerialisedEffect(serEff, DetailSpacer));
			info.append('\n');
		}
		return info.toString();
	}
}
