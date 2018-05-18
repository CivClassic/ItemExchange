package com.untamedears.ItemExchange.utility;

import java.util.*;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;

import org.bukkit.potion.PotionData;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.group.Group;

import com.untamedears.ItemExchange.DeprecatedMethods;
import com.untamedears.ItemExchange.ItemExchangePlugin;
import com.untamedears.ItemExchange.exceptions.ExchangeRuleCreateException;
import com.untamedears.ItemExchange.exceptions.ExchangeRuleParseException;
import com.untamedears.ItemExchange.metadata.AdditionalMetadata;
import com.untamedears.ItemExchange.metadata.BookMetadata;
import com.untamedears.ItemExchange.metadata.EnchantmentStorageMetadata;
import com.untamedears.ItemExchange.metadata.PotionMetadata;

public class ExchangeRule {
	// ------------------------------------------------------------
	//					  Static Functionality
	// ------------------------------------------------------------
	private static final List<Material> NOT_SUPPORTED = Arrays.asList(
			Material.MAP,
			Material.WRITTEN_BOOK,
			Material.ENCHANTED_BOOK,
			Material.FIREWORK,
			Material.FIREWORK_CHARGE);

	public static final String ruleSpacer = "&&&&r";
	public static final String categorySpacer = "&&&r";
	public static final String secondarySpacer = "&&r";
	public static final String tertiarySpacer = "&r";

	public static final String hiddenRuleSpacer = hideString(ruleSpacer);
	public static final String hiddenCategorySpacer = hideString(categorySpacer);
	public static final String hiddenSecondarySpacer = hideString(secondarySpacer);
	public static final String hiddenTertiarySpacer = hideString(tertiarySpacer);

	public static enum RuleType { INPUT, OUTPUT }

	public static boolean useHiddenSpacers = true;
	public static String GetAppropriateSpacer(int nested) {
		if (useHiddenSpacers == true) {
			switch (nested) {
				case 0: return hiddenRuleSpacer;
				case 1: return hiddenCategorySpacer;
				case 2: return hiddenSecondarySpacer;
				case 3: return hiddenTertiarySpacer;
				default: return "";
			}
		}
		else {
			switch (nested) {
				case 0: return ruleSpacer;
				case 1: return categorySpacer;
				case 2: return secondarySpacer;
				case 3: return tertiarySpacer;
				default: return "";
			}
		}
	}

	// Will prepend a § to each character in a string
	// therefore hiding it.
	public static String hideString(String string) {
		String hiddenString = "";
		for (char character : string.toCharArray())
			hiddenString += "§" + character;
		return hiddenString;
	}

	// Assuming the string was hidden using hideString
	// it will take every other character and return.
	// "§h§e§l§l§o§ §w§o§r§l§d" => "hello world"
	public static String unhideString(String string) {
		StringBuilder result = new StringBuilder();
		char[] chars = string.toCharArray();
		for(int i = 1; i < chars.length; i += 2)
			result.append(chars[i]);
		return result.toString();
	}

	// Escapes all 'r' and '\' characters in a string
	public static String escapeString(String string) {
		return string.replaceAll("([\\\\r])", "\\\\$1");
	}

	// Unescapes all 'r' and '\' characters in a string
	public static String unescapeString(String string) {
		return string.replaceAll("\\\\([\\\\r])", "$1");
	}

	// Checks whether an item is a valid rule block by
	// checking to see if it can be successfully parsed
	public static boolean isRuleBlock(ItemStack item) {
		try {
			ExchangeRule.parseBulkRuleBlock(item);
			return true;
		}
		catch(ExchangeRuleParseException e) {
			try {
				ExchangeRule.parseRuleBlock(item);
				return true;
			}
			catch(ExchangeRuleParseException e2) {
				return false;
			}
		}
	}

	// Converts a raw rule string into an ExchangeRule
	public static ExchangeRule parseRuleString(String ruleString) throws ExchangeRuleParseException {
		try {
			ExchangeRule exchangeRule = new ExchangeRule();
			// Order in array
			// ------------------------------
			//  0 - Type (Input / Output)
			//  1 - Kind (Should be "item")
			//  2 - Item ID
			//  3 - Durability
			//  4 - Amount
			//  5 - RequiredEnchantments[]
			//  6 - ExcludedEnchantments[]
			//  7 - AllowUnlistedEnchantments
			//  8 - ItemName
			//  9 - DisplayName
			// 10 - Lore
			// 11 - AdditionalMeta
			// 12 - Group
			// ------------------------------
			// Check if the ruleBlock is correct
			String[] compiledRule = ruleString.split(GetAppropriateSpacer(1), -1);
			if (compiledRule.length != 13) {
				throw new ExchangeRuleParseException(
						"Compiled rule has incorrect length: " +
						String.valueOf(compiledRule.length));
			}
			// Get the type of rule
			String ruleTypeString = unhideString(compiledRule[0]);
				 if (ruleTypeString.equals("i")) exchangeRule.ruleType = RuleType.INPUT;
			else if (ruleTypeString.equals("o")) exchangeRule.ruleType = RuleType.OUTPUT;
			else throw new ExchangeRuleParseException("Invalid rule type");
			// Ensure that the transaction is for an item
			if (!unhideString(compiledRule[1]).equals("item"))
				throw new ExchangeRuleParseException("Invalid transaction type");
			// Get the Material by its ID (deprecated, but changing would require
			// a rewrite that will make all current rule buttons non-functional)
			int materialID = Integer.valueOf(unhideString(compiledRule[2]));
			exchangeRule.material = DeprecatedMethods.getMaterialById(materialID);
			if (exchangeRule.material == null)
				throw new ExchangeRuleParseException("Invalid material type");
			// Get the durability
			exchangeRule.durability = Short.valueOf(unhideString(compiledRule[3]));
			if (exchangeRule.durability < 0) exchangeRule.durability = 0;
			// Get the amount of items
			exchangeRule.amount = Integer.parseInt(unhideString(compiledRule[4]));
			if (exchangeRule.amount < 1) exchangeRule.amount = 1;
			// Get the required enchantments
			exchangeRule.requiredEnchantments = new HashMap<>();
			for (String compiledEnchant : compiledRule[5].split(GetAppropriateSpacer(2))) {
				// If enchant is nothing, then skip
				if (compiledEnchant.isEmpty()) continue;
				// Otherwise parse this enchantment
				String[] enchantmentData = compiledEnchant.split(GetAppropriateSpacer(3));
				Integer enchantmentId = Integer.valueOf(unhideString(enchantmentData[0]));
				Integer enchantmentLevel = Integer.valueOf(unhideString(enchantmentData[1]));
				Enchantment enchantment = DeprecatedMethods.getEnchantmentById(enchantmentId);
				exchangeRule.requiredEnchantments.put(enchantment, enchantmentLevel);
			}
			// Get the excluded enchantments
			exchangeRule.excludedEnchantments = new ArrayList<>();
			for (String compiledEnchant : compiledRule[6].split(GetAppropriateSpacer(2))) {
				// If enchant is nothing, then skip
				if (compiledEnchant.isEmpty()) continue;
				// Otherwise parse this enchantment
				String[] enchantmentData = compiledEnchant.split(GetAppropriateSpacer(3));
				Integer enchantmentId = Integer.valueOf(unhideString(enchantmentData[0]));
				Enchantment enchantment = DeprecatedMethods.getEnchantmentById(enchantmentId);
				exchangeRule.excludedEnchantments.add(enchantment);
			}
			// Get whether unlisted enchantments are allowed
			String aueString = unhideString(compiledRule[7]);
				 if (aueString.equals("0")) exchangeRule.unlistedEnchantmentsAllowed = false;
			else if (aueString.equals("1")) exchangeRule.unlistedEnchantmentsAllowed = true;
			else throw new ExchangeRuleParseException("Invalid unlisted allowance type");
			// Get the item's name
			exchangeRule.itemName = compiledRule[8];
			if (!exchangeRule.itemName.isEmpty())
				exchangeRule.itemName = unescapeString(unhideString(exchangeRule.itemName));
			// Get the item's DisplayName
			exchangeRule.displayName = compiledRule[9];
			if (!exchangeRule.displayName.isEmpty())
				exchangeRule.displayName = unescapeString(unhideString(exchangeRule.displayName));
			// Get the item's Lore
			exchangeRule.lore = new String[0];
			if (!compiledRule[10].isEmpty()) {
				exchangeRule.lore = compiledRule[10].split(GetAppropriateSpacer(2), -1);
				for(int i = 0; i < exchangeRule.lore.length; i++)
					exchangeRule.lore[i] = unhideString(unescapeString(exchangeRule.lore[i]));
			}
			// Get any additional meta
			exchangeRule.additional = null;
			if (!compiledRule[11].isEmpty()) {
				String metaString = unhideString(compiledRule[11]);
				switch (exchangeRule.material) {
					case WRITTEN_BOOK: exchangeRule.additional = BookMetadata.deserialize(metaString); break;
					case ENCHANTED_BOOK: exchangeRule.additional = EnchantmentStorageMetadata.deserialize(metaString); break;
					case POTION: exchangeRule.additional = PotionMetadata.deserialize(metaString); break;
					default: break;
				}
			}
			// Get the group
			exchangeRule.citadelGroup = null;
			if (!compiledRule[12].isEmpty())
				exchangeRule.citadelGroup = GroupManager.getGroup(unescapeString(unhideString(compiledRule[12])));
			// And return the rule
			return exchangeRule;
		}
		// If any of the above code fails, then it's an invalid rule string
		catch (Exception e) {
			throw new ExchangeRuleParseException("Unable to parse exchange rule. (" + e.getMessage() + " - " + e.getLocalizedMessage() + ")");
		}
	}

	// Extracts the raw rule string from a rule block and attempts to parse it
	public static ExchangeRule parseRuleBlock(ItemStack ruleBlock) throws ExchangeRuleParseException {
		// Uhhh, is this try catch really necessary?
		// It will just catch the generic error from the parse
		// and generates a new, identical error in its place..
		try {
			return parseRuleString(ruleBlock.getItemMeta().getLore().get(0));
		}
		catch(Exception e) {
			throw new ExchangeRuleParseException("Invalid exchange rule block. (" + e.getMessage() + ")");
		}
	}

	// Extracts the raw rule strings from a bulk rule block and attempts to parse them
	public static ExchangeRule[] parseBulkRuleBlock(ItemStack ruleBlock) throws ExchangeRuleParseException {
		try {
			// Extract the rule strings
			String[] rules = ruleBlock.getItemMeta().getLore().get(1).split(GetAppropriateSpacer(0));
			// And attempt to parse them
			List<ExchangeRule> ruleList = new ArrayList<>();
			for(String rule : rules) ruleList.add(parseRuleString(rule));
			// And return the resulting rules
			return ruleList.toArray(new ExchangeRule[0]);
			// Apparently, this part here ^ is necessary as a type cast, see below
			// https://stackoverflow.com/questions/5374311/convert-arrayliststring-to-string-array
			// Weird...
		}
		catch(Exception e) {
			throw new ExchangeRuleParseException("Invalid bulk exchange rule block. (" + e.getMessage() + ")");
		}
	}

	//
	public static ExchangeRule createRuleFromItem(ItemStack itemStack, RuleType ruleType) throws ExchangeRuleCreateException {
		ExchangeRule exchangeRule = new ExchangeRule();
		// Set basic data
		exchangeRule.material = itemStack.getType();
		exchangeRule.amount = itemStack.getAmount();
		exchangeRule.durability = itemStack.getDurability();
		exchangeRule.ruleType = ruleType;
		// Check if material is on blacklist
		if(NOT_SUPPORTED.contains(exchangeRule.material)) {
			throw new ExchangeRuleCreateException("This material is not supported.");
		}
		// Set the item name of the item stack
		exchangeRule.itemName = ItemUtility.getItemStackName(itemStack);
		if (itemStack.hasItemMeta()) {
			ItemMeta itemMeta = itemStack.getItemMeta();
			// Set the display name of the item, if it has one
			if (itemMeta.hasDisplayName())
				exchangeRule.displayName = itemMeta.getDisplayName();
			// Set the lore of the item, it it has any
			if (itemMeta.hasLore())
				exchangeRule.lore = itemMeta.getLore().toArray(new String[0]);
			// Get addition meta from specific types
			if(itemMeta instanceof BookMeta) {
				exchangeRule.additional = new BookMetadata((BookMeta) itemMeta);
			}
			else if(itemMeta instanceof EnchantmentStorageMeta) {
				exchangeRule.additional = new EnchantmentStorageMetadata((EnchantmentStorageMeta) itemMeta);
			}
			else if(itemMeta instanceof PotionMeta) {
				PotionMeta potMeta = (PotionMeta) itemMeta;
				PotionData potData = potMeta.getBasePotionData();
				exchangeRule.itemName = PotionUtility.getPotionNameByEffect(potData);
				exchangeRule.additional = new PotionMetadata(potMeta);
				exchangeRule.useDisplayNameAsItemName();
			}
			// If the item has any of the following meta types
			// the item is not supported, so error out
			if (itemMeta instanceof FireworkEffectMeta ||
				itemMeta instanceof FireworkMeta ||
				itemMeta instanceof LeatherArmorMeta ||
				itemMeta instanceof MapMeta ||
				itemMeta instanceof SkullMeta) {
				throw new ExchangeRuleCreateException(
						"This item is not yet supported by ItemExchange.");
			}
		}
		exchangeRule.requiredEnchantments = new HashMap<>();
		for (Enchantment enchantment : itemStack.getEnchantments().keySet())
			exchangeRule.requiredEnchantments.put(
					enchantment, itemStack.getEnchantments().get(enchantment));
		exchangeRule.excludedEnchantments = new ArrayList<>();
		exchangeRule.unlistedEnchantmentsAllowed = false;
		// And return the rule
		return exchangeRule;
	}

	public static ExchangeRule createRuleFromString(String[] args) throws ExchangeRuleCreateException {
		try {
			// Order in array
			// ------------------------------
			//  0 - Type (Input / Output)
			//  1 - Item ID
			//  2 - Amount
			// ------------------------------
			// Get the type of rule
			RuleType ruleType; String ruleTypeString = args[0];
				 if (ruleTypeString.equals("input"))  ruleType = RuleType.INPUT;
			else if (ruleTypeString.equals("output")) ruleType = RuleType.OUTPUT;
			else throw new ExchangeRuleParseException("Invalid rule type");
			// Basic rule data
			Material material = null;
			short durability = 0;
			int amount = 1;
			// If there are enough arguments, parse them
			if (args.length > 1) {
				String itemName = args[1].toLowerCase();
				// If using item name, get details from cache
				if (ItemExchangePlugin.NAME_MATERIAL.containsKey(itemName)) {
					ItemStack itemStack = ItemExchangePlugin.NAME_MATERIAL.get(itemName);
					material = itemStack.getType();
					durability = itemStack.getDurability();
				}
				// Otherwise try to get item by id
				else {
					String[] idData = args[1].split(":");
					int itemId = Integer.valueOf(idData[0]);
					material = DeprecatedMethods.getMaterialById(itemId);
					if (idData.length > 1) durability = Short.valueOf(idData[1]);
				}
				// Get amount, if present
				if (args.length > 2) {
					amount = Integer.valueOf(args[2]);
					if (amount < 1) amount = 1;
				}
			}
			// If no material is present, error out
			if (material == null)
				throw new ExchangeRuleParseException("No material present.");
			// Create an item stack based on the data
			ItemStack itemStack = new ItemStack(material, amount, durability);
			// And attempt to create the rule block from that item stack
			return createRuleFromItem(itemStack, ruleType);
		}
		catch (Exception e) {
			throw new ExchangeRuleCreateException(
					"Could not create an exchange rule with those parameters.");
		}
	}

	// Convert many exchange rules into one bulk exchange rule
	public static ItemStack toBulkItemStack(Collection<ExchangeRule> rules) {
		// Generate a new rule button and get its meta
		ItemStack itemStack = ItemExchangePlugin.ITEM_RULE_ITEMSTACK.clone();
		ItemMeta itemMeta = itemStack.getItemMeta();
		// Set the name
		itemMeta.setDisplayName(ChatColor.DARK_RED + "Bulk Rule Block");
		// Combine and set the individual rules
		StringBuilder compiledRules = new StringBuilder();
		Iterator<ExchangeRule> iterator = rules.iterator();
		while(iterator.hasNext()) {
			compiledRules.append(iterator.next().compileRule());
			if(iterator.hasNext()) compiledRules.append(GetAppropriateSpacer(0));
		}
		// Set the lore based on the rules
		List<String> newLore = new ArrayList<>();
		newLore.add("This rule block holds " +
				rules.size() + (rules.size() > 1 ? " exchange rules." : " exchange rule."));
		newLore.add(compiledRules.toString());
		itemMeta.setLore(newLore);
		// Save the meta and return
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	// ------------------------------------------------------------
	//					 Instance Functionality
	// ------------------------------------------------------------
	private Material 					material = null;
	private int 						amount = 0;
	private short 						durability = 0;
	private Map<Enchantment, Integer> 	requiredEnchantments = new HashMap<>();
	private List<Enchantment> 			excludedEnchantments = new ArrayList<>();
	private boolean 					unlistedEnchantmentsAllowed = false;
	private String						itemName = "";
	private String 						displayName = "";
	private String[] 					lore = new String[0];
	private RuleType 					ruleType = null;
	private AdditionalMetadata 			additional = null;
	private Group 						citadelGroup = null;

	private ExchangeRule() {}

	// Short Constructor
	public ExchangeRule(
			Material material,
			int amount,
			short durability,
			RuleType ruleType) {
		this(
			material,
			amount,
			durability,
			new HashMap<>(),
			new ArrayList<>(),
			false,
			"",
			new String[0],
			ruleType);
	}

	// Full Constructor
	public ExchangeRule(
			Material material,
			int amount,
			short durability,
			Map<Enchantment, Integer> requiredEnchantments,
			List<Enchantment> excludedEnchantments,
			boolean otherEnchantmentsAllowed,
			String displayName,
			String[] lore,
			RuleType ruleType) {
		// Set all the relevant data
		this.material = material;
		this.amount = amount;
		this.durability = durability;
		this.requiredEnchantments = requiredEnchantments;
		this.excludedEnchantments = excludedEnchantments;
		this.unlistedEnchantmentsAllowed = otherEnchantmentsAllowed;
		this.itemName = ItemUtility.getItemStackName(material, durability);
		this.displayName = displayName;
		this.lore = lore;
		this.ruleType = ruleType;
	}

	// Convert this ExchangeRule into an item version of itself
	public ItemStack toItemStack() {
		// Generate a new rule button and get its meta
		ItemStack itemStack = ItemExchangePlugin.ITEM_RULE_ITEMSTACK.clone();
		ItemMeta itemMeta = itemStack.getItemMeta();
		// Set the name
		itemMeta.setDisplayName(displayedItemStackInfo());
		// Create new lore
		List<String> newLore = new ArrayList<>();
		// Add enchantments to lore, if present
		if(ItemExchangePlugin.ENCHANTABLE_ITEMS.contains(material))
			newLore.add(displayedEnchantments());
		// Add display info to lore
		for (String line : displayedLore()) {
			newLore.add(line);
		}
		// Mention Citadel restriction, if present
		if(citadelGroup != null) {
			newLore.add(ChatColor.RED + "Restricted with Citadel.");
		}
		// If lore is still empty, just add rules, otherwise
		// prepend the rules to the lore.
		newLore.add(0, compileRule());

		// Save the meta and return
		itemMeta.setLore(newLore);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	// Encode the rules into a string
	public String compileRule() {
		// Order in array
		// ------------------------------
		//  0 - Type (Input / Output)
		//  1 - Kind (Should be "item")
		//  2 - Item ID
		//  3 - Durability
		//  4 - Amount
		//  5 - RequiredEnchantments[]
		//  6 - ExcludedEnchantments[]
		//  7 - AllowUnlistedEnchantments
		//  8 - ItemName
		//  9 - DisplayName
		// 10 - Lore
		// 11 - AdditionalMeta
		// 12 - Group
		// ------------------------------
		String compiledRule = "";
		// RuleType
		compiledRule += ruleType.equals(RuleType.INPUT) ? hideString("i") : hideString("o");
		compiledRule += GetAppropriateSpacer(1);
		// Transaction type
		compiledRule += hideString("item");
		compiledRule += GetAppropriateSpacer(1);
		// Item ID
		compiledRule += hideString(String.valueOf(DeprecatedMethods.getMaterialId(material)));
		compiledRule += GetAppropriateSpacer(1);
		// Durability
		compiledRule += hideString(String.valueOf(durability));
		compiledRule += GetAppropriateSpacer(1);
		// Amount
		compiledRule += hideString(String.valueOf(amount));
		compiledRule += GetAppropriateSpacer(1);
		// Required Enchantments
		boolean enchantable = ItemExchangePlugin.ENCHANTABLE_ITEMS.contains(material);
		if(enchantable) {
			for (Entry<Enchantment, Integer> entry : requiredEnchantments.entrySet()) {
				// Get basic enchantment data
				String enchantId = String.valueOf(DeprecatedMethods.getEnchantmentId(entry.getKey()));
				String enchantLevel = String.valueOf(entry.getValue());
				// Add to encoded string
				compiledRule += hideString(enchantId);
				compiledRule += GetAppropriateSpacer(3);
				compiledRule += hideString(enchantLevel);
				compiledRule += GetAppropriateSpacer(2);
			}
		}
		compiledRule += GetAppropriateSpacer(1);
		// Excluded Enchantments
		if(enchantable) {
			for (Enchantment entry : excludedEnchantments) {
				// Get basic enchantment data
				String enchantId = String.valueOf(DeprecatedMethods.getEnchantmentId(entry));
				// Add to encoded string
				compiledRule += hideString(enchantId);
				compiledRule += GetAppropriateSpacer(2);
			}
		}
		compiledRule += GetAppropriateSpacer(1);
		// Allow Unlisted Enchantments
		compiledRule += (unlistedEnchantmentsAllowed && enchantable) ? hideString("1") : hideString("0");
		compiledRule += GetAppropriateSpacer(1);
		// ItemName
		compiledRule += hideString(escapeString(itemName));
		compiledRule += GetAppropriateSpacer(1);
		// DisplayName
		compiledRule += hideString(escapeString(displayName));
		compiledRule += GetAppropriateSpacer(1);
		// Lore
		for (int i = 0; i < lore.length; i++) {
			compiledRule += hideString(escapeString(lore[i]));
			if (i + 1 < lore.length)
				compiledRule += GetAppropriateSpacer(2);
		}
		compiledRule += GetAppropriateSpacer(1);
		// AdditionalMeta
		if(additional != null) {
			compiledRule += hideString(additional.serialize());
		}
		compiledRule += GetAppropriateSpacer(1);
		// Citadel Group
		if(citadelGroup != null) {
			compiledRule += hideString(escapeString(citadelGroup.getName()));
		}
		// Finish and return
		return compiledRule;
	}

	// Some transaction inputs have Citadel restrictions, so if
	// a group is listed, check if the player is a member of it.
	// If no group is listed, defaults to true.
	public boolean canPlayerAccess(Player player) {
		if(this.ruleType == RuleType.INPUT) {
			if(citadelGroup != null) {
				UUID playerId = player.getUniqueId();
				return citadelGroup.isMember(playerId);
			}
		}
		return true;
	}

	// Check how many transactions can take place with
	// the current amount in inventory
	public int possibleTransactionAmount(Inventory inventory) {
		int invAmount = 0;
		for (ItemStack itemStack : inventory.getContents()) {
			if (itemStack != null && isRequiredItem(itemStack)) {
				invAmount += itemStack.getAmount();
			}
		}
		if (amount <= 0) return 0;
		return invAmount / amount;
	}

	// Check whether inventory has the minimum amount of
	// items to serve at least one transaction.
	public boolean hasEnoughOfItem(Inventory inventory) {
		return possibleTransactionAmount(inventory) > 0;
	}

	// Check whether an item fits the description in rules
	public boolean isRequiredItem(ItemStack itemStack) {
		ExchangeRule exchangeRule = null;
		try {
			exchangeRule = createRuleFromItem(itemStack, this.ruleType);
		}
		// If this errors, then it can't be the same
		catch (ExchangeRuleCreateException e) {
			return false;
		}
		// Check if null, just in case
		if (exchangeRule == null) return false;
		// Change the amount to match
		exchangeRule.amount = this.amount;
		// Now check if buttons are identical
		// If it's the same item, they should be
		String itemRule = exchangeRule.compileRule();
		String shopRule = compileRule();
		return itemRule.equals(shopRule);
	}

	// Get the shop display text for this exchange rule
	public String[] showShopDisplay(Player player) {
		List<String> display = new ArrayList<>();
		// Add all the item's vanilla info
		display.add(displayedItemStackInfo());
		// Show addition meta (books, etc)
		if (additional != null)
			display.add(additional.getDisplayedInfo());
		// Show enchantments
		if(ItemExchangePlugin.ENCHANTABLE_ITEMS.contains(material))
			display.add(displayedEnchantments());
		// Lore
		for(String line : displayedLore()) {
			display.add(line);
		}
		// Citadel group
		if(citadelGroup != null) {
			UUID playerId = player.getUniqueId();
			if(citadelGroup.isMember(playerId))
				display.add(ChatColor.GREEN + "Restricted with Citadel. You have access to this shop.");
			else
				display.add(ChatColor.RED + "Restricted with Citadel. You do not have access to this shop.");
		}
		// And return using the same weird type cast
		return display.toArray(new String[0]);
	}

	private String displayedItemStackInfo() {
		StringBuilder info = new StringBuilder();
		info
			.append(ChatColor.YELLOW)
			.append((ruleType == RuleType.INPUT ? "Input" : "Output") + " ")
			.append(ChatColor.WHITE)
			.append(amount).append(" ")
			.append(itemName);
		// Get and display item name
		if (!displayName.isEmpty())
			info.append(" \"" + displayName + "\"");
		// Return data
		return info.toString();
	}

	private String displayedEnchantments() {
		if (requiredEnchantments.size() > 0 || excludedEnchantments.size() > 0) {
			StringBuilder stringBuilder = new StringBuilder();
			for (Entry<Enchantment, Integer> entry : requiredEnchantments.entrySet()) {
				stringBuilder.append(ChatColor.GREEN);
				stringBuilder.append(ItemExchangePlugin.ENCHANTMENT_ABBRV.get(entry.getKey().getName()));
				stringBuilder.append(entry.getValue());
				stringBuilder.append(" ");
			}
			for (Enchantment enchantment : excludedEnchantments) {
				stringBuilder.append(ChatColor.RED);
				stringBuilder.append(ItemExchangePlugin.ENCHANTMENT_ABBRV.get(enchantment.getName()));
				stringBuilder.append(" ");
			}
			stringBuilder.append(unlistedEnchantmentsAllowed ? ChatColor.GREEN + "Other Enchantments Allowed." : ChatColor.RED + "Other Enchantments Disallowed");
			return stringBuilder.toString();
		}
		else {
			return unlistedEnchantmentsAllowed ? ChatColor.GREEN + "Any enchantments allowed" : ChatColor.RED + "No enchantments allowed";
		}
	}

	private String[] displayedLore() {
		if (lore.length == 0) {
			return new String[0];
		}
		else if (lore.length == 1) {
			return new String[] { ChatColor.DARK_PURPLE + lore[0] };
		}
		else {
			return new String[] { ChatColor.DARK_PURPLE + lore[0], ChatColor.DARK_PURPLE + lore[1] + "..." };
		}
	}

	// SETTERS
	public void setMaterial(Material material) {
		this.material = material;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public void setDurability(short durability) {
		this.durability = durability;
	}

	public void requireEnchantment(Enchantment enchantment, Integer level) {
		requiredEnchantments.put(enchantment, level);
	}

	public void removeRequiredEnchantment(Enchantment enchantment) {
		requiredEnchantments.remove(enchantment);
	}

	public void excludeEnchantment(Enchantment enchantment) {
		if(!excludedEnchantments.contains(enchantment))
			excludedEnchantments.add(enchantment);
	}

	public void removeExcludedEnchantment(Enchantment enchantment) {
		excludedEnchantments.remove(enchantment);
	}

	public void setUnlistedEnchantmentsAllowed(boolean allowed) {
		this.unlistedEnchantmentsAllowed = allowed;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName == null ? "" : itemName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName == null ? "" : displayName;
	}

	public void useDisplayNameAsItemName() {
		if (this.displayName != null && !this.displayName.isEmpty()) {
			this.itemName = this.displayName;
			this.displayName = "";
		}
	}


	public void setLore(String[] lore) {
		this.lore = lore;
	}

	public void switchIO() {
		ruleType = ruleType == RuleType.INPUT ? RuleType.OUTPUT : RuleType.INPUT;
	}

	public void setAdditionalMetadata(AdditionalMetadata meta) {
		this.additional = meta;
	}

	public void setCitadelGroup(Group group) {
		this.citadelGroup = group;
	}

	// GETTERS
	public int getAmount() {
		return amount;
	}

	public boolean getUnlistedEnchantmentsAllowed() {
		return unlistedEnchantmentsAllowed;
	}

	public RuleType getType() {
		return ruleType;
	}

	public Group getCitadelGroup() {
		return citadelGroup;
	}
}
