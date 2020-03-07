package com.untamedears.itemexchange;

import co.aikar.commands.BukkitCommandManager;
import com.google.common.base.Strings;
import com.untamedears.itemexchange.commands.SetCommand;
import com.untamedears.itemexchange.listeners.ItemExchangeListener;
import com.untamedears.itemexchange.utility.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.api.MaterialAPI;
import vg.civcraft.mc.civmodcore.itemHandling.NiceNames;

public class ItemExchangePlugin extends ACivMod {

    public static final Permission PURCHASE_PERMISSION = new Permission(
            "ITEM_EXCHANGE_GROUP_VIEW_PURCHASE", Permission.membersAndAbove(),
            "The ability to view and purchase exchanges set to this group.");

    public static final Permission ESTABLISH_PERMISSION = new Permission(
            "ITEM_EXCHANGE_GROUP_ESTABLISH", Permission.modsAndAbove(),
            "The ability to set exchanges to be exclusive to this group.");

    public static final Map<Enchantment, String> ENCHANT_ABBREVS = new HashMap<>();

    private static BukkitCommandManager commands;

    private static ItemExchangePlugin instance;

	// Blocks that can be used as exchanges, any block with an inventory
	// *should* works
	public static final List<Material> ACCEPTABLE_BLOCKS = Arrays.asList(Material.CHEST, Material.DISPENSER, Material.TRAPPED_CHEST, Material.DROPPER);
	public static final List<Material> ENCHANTABLE_ITEMS = Arrays.asList(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, 
			Material.GOLD_HELMET, Material.GOLD_CHESTPLATE, Material.GOLD_LEGGINGS, Material.GOLD_BOOTS, 
			Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, 
			Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS, 
			Material.DIAMOND_SWORD, Material.GOLD_SWORD, Material.STONE_SWORD, Material.WOOD_SWORD, 
			Material.DIAMOND_AXE, Material.GOLD_AXE, Material.STONE_AXE, Material.WOOD_AXE, 
			Material.DIAMOND_PICKAXE, Material.GOLD_PICKAXE, Material.STONE_PICKAXE, Material.WOOD_PICKAXE, 
			Material.DIAMOND_SPADE, Material.GOLD_SPADE, Material.STONE_SPADE, Material.WOOD_SPADE, 
			Material.DIAMOND_HOE, Material.GOLD_HOE, Material.STONE_HOE, Material.WOOD_HOE, 
			Material.BOW, Material.SHEARS, Material.FISHING_ROD, Material.FLINT_AND_STEEL, Material.CARROT_STICK);

	public static final ItemStack ITEM_RULE_ITEMSTACK = new ItemStack(Material.STONE_BUTTON, 1);
	public static final Material ITEM_RULE_MATERIAL = ITEM_RULE_ITEMSTACK.getType();

	@Override
	public void onEnable() {
	    instance = this;
	    super.onEnable();
        saveDefaultConfig();
		// Register Permissions
        PURCHASE_PERMISSION.register();
        ESTABLISH_PERMISSION.register();
        // Register Events
        registerEvents(new ItemExchangeListener());
        // Load Enchantment Abbreviations
        for (Enchantment enchantment : Enchantment.values()) {
            String abbrev = NiceNames.getAcronym(enchantment);
            if (!Strings.isNullOrEmpty(abbrev)) {
                ENCHANT_ABBREVS.put(enchantment, abbrev);
            }
        }
        // Load Commands
        commands = new BukkitCommandManager(this);
        commands.getCommandCompletions().registerAsyncCompletion(
                "materials", (context) -> {
                    String input = context.getInput();
                    return Arrays.stream(Material.values()).
                            filter(MaterialAPI::isValidItemMaterial).
                            map(Enum::name).
                            filter((name) -> input.isEmpty() || name.startsWith(input)).
                            collect(Collectors.toCollection(ArrayList::new));
                });
        commands.getCommandCompletions().registerAsyncCompletion(
                "types", (c) -> Arrays.asList("input", "output"));
        commands.registerCommand(SetCommand.INSTANCE);
	}

    @Override
	public void onDisable() {
	    instance = null;
        super.onDisable();
        // Load Enchantment Abbreviations
        ENCHANT_ABBREVS.clear();
        // Unload Commands
        commands.unregisterCommands();
    }

    public static ItemExchangePlugin getInstance() {
	    return instance;
    }

}
