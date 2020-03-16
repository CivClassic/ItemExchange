package com.untamedears.itemexchange;

import co.aikar.commands.BukkitCommandManager;
import com.untamedears.itemexchange.commands.CreateCommand;
import com.untamedears.itemexchange.commands.SetCommand;
import com.untamedears.itemexchange.rules.BulkExchangeRule;
import com.untamedears.itemexchange.rules.ExchangeRule;
import com.untamedears.itemexchange.rules.additional.BookAdditional;
import com.untamedears.itemexchange.rules.additional.EnchantStorageAdditional;
import com.untamedears.itemexchange.rules.additional.PotionAdditional;
import com.untamedears.itemexchange.rules.additional.RepairAdditional;
import com.untamedears.itemexchange.utility.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.api.MaterialAPI;
import vg.civcraft.mc.civmodcore.util.TextUtil;

public class ItemExchangePlugin extends ACivMod {

    public static final Permission PURCHASE_PERMISSION = new Permission(
            "ITEM_EXCHANGE_GROUP_PURCHASE",
            Permission.membersAndAbove(),
            "The ability to purchase exchanges set to this group.");

    public static final Permission ESTABLISH_PERMISSION = new Permission(
            "ITEM_EXCHANGE_GROUP_ESTABLISH",
            Permission.modsAndAbove(),
            "The ability to set exchanges to be exclusive to this group.");

    private static ItemExchangePlugin instance;

    private static BukkitCommandManager commands;

    public static final Set<Material> SHOP_BLOCKS = new HashSet<>();

    public static final ItemStack RULE_ITEM = new ItemStack(Material.STONE_BUTTON);

    public static final Set<Material> CAN_ENCHANT = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();
        saveDefaultConfig();
        // Register Permissions
        PURCHASE_PERMISSION.register();
        ESTABLISH_PERMISSION.register();
        // Register Events
        registerEvent(new ItemExchangeListener());
        // Load Commands
        commands = new BukkitCommandManager(this);
        commands.getCommandCompletions().registerAsyncCompletion("materials", (context) ->
                Arrays.stream(Material.values()).
                        filter(MaterialAPI::isValidItemMaterial).
                        map(Enum::name).
                        filter((name) -> TextUtil.startsWith(name, context.getInput())).
                        collect(Collectors.toCollection(ArrayList::new)));
        commands.getCommandCompletions().registerAsyncCompletion("types", (c) -> Arrays.asList("input", "output"));
        commands.registerCommand(CreateCommand.INSTANCE);
        commands.registerCommand(SetCommand.INSTANCE);
        // Register Serializables
        registerSerializable(ExchangeRule.class);
        registerSerializable(BulkExchangeRule.class);
        registerSerializable(BookAdditional.class);
        registerSerializable(EnchantStorageAdditional.class);
        registerSerializable(PotionAdditional.class);
        registerSerializable(RepairAdditional.class);
        // Parse Config
        SHOP_BLOCKS.clear();
        for (String raw : getConfig().getStringList("supportedBlocks")) {
            Material material = MaterialAPI.getMaterial(raw);
            if (material == null) {
                warning("[Config] Could not parse material for supported block: " + raw);
                continue;
            }
            if (SHOP_BLOCKS.contains(material)) {
                warning("[Config] Supported block material duplicate: " + raw);
                continue;
            }
            info("[Config] Supported block material parsed: " + material.name());
            SHOP_BLOCKS.add(material);
        }
        if (SHOP_BLOCKS.isEmpty()) {
            warning("[Config] There are no supported blocks, try: supportedBlocks: [CHEST, TRAPPED_CHEST]");
        }
        RULE_ITEM.setType(Material.STONE_BUTTON);
        {
            String raw = getConfig().getString("ruleItem");
            Material material = MaterialAPI.getMaterial(raw);
            if (material == null) {
                warning("[Config] Could not parse material for rule item, default to STONE_BUTTON: " + raw);
            }
            else {
                info("[Config] Rule item material parsed: " + material.name());
                RULE_ITEM.setType(material);
            }
        }
        CAN_ENCHANT.clear();
        for (String raw : getConfig().getStringList("enchantables")) {
            Material material = MaterialAPI.getMaterial(raw);
            if (material == null) {
                warning("[Config] Could not parse enchantable material: " + raw);
                continue;
            }
            if (CAN_ENCHANT.contains(material)) {
                warning("[Config] Enchantable material duplicate: " + raw);
                continue;
            }
            info("[Config] Enchantable material parsed: " + material.name());
            CAN_ENCHANT.add(material);
        }
    }

    @Override
    public void onDisable() {
        // Unload Commands
        commands.unregisterCommands();
        commands = null;
        // Unload Configs
        SHOP_BLOCKS.clear();
        CAN_ENCHANT.clear();
        // Finalise Disable
        super.onDisable();
        instance = null;
    }

    public static ItemExchangePlugin getInstance() {
        return instance;
    }

}
