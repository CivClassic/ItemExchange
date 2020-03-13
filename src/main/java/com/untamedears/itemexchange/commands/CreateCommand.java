package com.untamedears.itemexchange.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Split;
import co.aikar.commands.annotation.Syntax;
import com.google.common.base.Strings;
import com.untamedears.itemexchange.ItemExchangePlugin;
import com.untamedears.itemexchange.rules.ExchangeRule;
import com.untamedears.itemexchange.rules.ExchangeRule.Type;
import com.untamedears.itemexchange.utility.Permission;
import com.untamedears.itemexchange.utility.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.api.MaterialAPI;
import vg.civcraft.mc.civmodcore.util.NullCoalescing;

@SuppressWarnings("unused")
@CommandAlias(CreateCommand.ALIAS)
public class CreateCommand extends BaseCommand {

    public static final String ALIAS = "iecreate|iec";

    public static final Permission CITADEL_CHEST_PERMISSION = new Permission("CHESTS");

    public static final CreateCommand INSTANCE = new CreateCommand();

    private CreateCommand() {
    }

    @Default
    @Description("Creates an exchange rule based on a shop block.")
    public void createFromShop(Player player) {
        BlockIterator ray = new BlockIterator(player, 6);
        while (ray.hasNext()) {
            Block block = ray.next();
            if (block == null || !block.getType().isBlock()) {
                continue;
            }
            if (!ItemExchangePlugin.SHOP_BLOCKS.contains(block.getType())) {
                break;
            }
            if (!CITADEL_CHEST_PERMISSION.hasAccess(Utilities.getReinforcementGroupFromBlock(block), player)) {
                throw new InvalidCommandArgument("You do not have access to that.");
            }
            Inventory inventory = NullCoalescing.chain(() -> ((InventoryHolder) block.getState()).getInventory());
            if (inventory == null) {
                throw new InvalidCommandArgument("You do not have access to that.");
            }
            ItemStack inputItem = null;
            ItemStack outputItem = null;
            for (ItemStack item : inventory.getContents()) {
                if (!ItemAPI.isValidItem(item)) {
                    continue;
                }
                if (inputItem == null) {
                    inputItem = item.clone();
                }
                else if (inputItem.isSimilar(item)) {
                    inputItem.setAmount(inputItem.getAmount() + item.getAmount());
                }
                else if (outputItem == null) {
                    outputItem = item.clone();
                }
                else if (outputItem.isSimilar(item)) {
                    outputItem.setAmount(outputItem.getAmount() + item.getAmount());
                }
                else {
                    throw new InvalidCommandArgument("Inventory should only contain two types of items!");
                }
            }
            if (inputItem == null) {
                throw new InvalidCommandArgument("Inventory should have at least one type of item.");
            }
            if (Utilities.isExchangeRule(inputItem)) {
                throw new InvalidCommandArgument("You cannot exchange rule blocks!");
            }
            ExchangeRule inputRule = new ExchangeRule();
            inputRule.setType(Type.INPUT);
            inputRule.trace(inputItem);
            if (outputItem == null) {
                Utilities.giveItemsOrDrop(inventory, inputRule.toItem());
            }
            else {
                if (Utilities.isExchangeRule(outputItem)) {
                    throw new InvalidCommandArgument("You cannot exchange rule blocks!");
                }
                ExchangeRule outputRule = new ExchangeRule();
                outputRule.setType(Type.OUTPUT);
                outputRule.trace(outputItem);
                Utilities.giveItemsOrDrop(inventory, inputRule.toItem(), outputRule.toItem());
            }
            player.sendMessage(ChatColor.GREEN + "Created exchange successfully.");
        }
        throw new InvalidCommandArgument("No block in view is a suitable shop block.");
    }

    @CommandAlias(CreateCommand.ALIAS)
    @Syntax("<type>")
    @Description("Creates an exchange rule based on a held item.")
    @CommandCompletion("@types")
    public void createFromHeld(Player player, String type) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemAPI.isValidItem(held)) {
            throw new InvalidCommandArgument("You must be holding an item to do that.");
        }
        ExchangeRule rule = new ExchangeRule();
        rule.setType(matchType(type));
        rule.trace(held);
        Utilities.givePlayerExchangeRule(player, rule);
    }

    @CommandAlias(CreateCommand.ALIAS)
    @Syntax("<type> <material> [amount]")
    @Description("Sets the material of an exchange rule.")
    @CommandCompletion("@types @materials")
    public void createFromDetails(Player player, String type, @Split(":") String[] slug, @Default("1") int amount) {
        Material material = MaterialAPI.getMaterial(slug[0]);
        if (!MaterialAPI.isValidItemMaterial(material)) {
            throw new InvalidCommandArgument("You must enter a valid item material.");
        }
        // TODO: Allow for people to NOT enter in a durability
        short durability = NullCoalescing.chain(() -> Short.parseShort(slug[1]), (short) -1);
        if (durability < 0) {
            throw new InvalidCommandArgument("You must enter a valid durability.");
        }
        if (amount <= 0) {
            throw new InvalidCommandArgument("You must enter a valid amount.");
        }
        ExchangeRule rule = new ExchangeRule();
        rule.setType(matchType(type));
        rule.setMaterial(material);
        rule.setDurability(durability);
        rule.setAmount(amount);
        Utilities.givePlayerExchangeRule(player, rule);
    }

    private Type matchType(String value) {
        if (!Strings.isNullOrEmpty(value)) {
            switch (value.toLowerCase()) {
                case "i":
                case "in":
                case "input":
                case "inputs":
                    return Type.INPUT;
                case "o":
                case "out":
                case "output":
                case "outputs":
                    return Type.OUTPUT;
            }
        }
        throw new InvalidCommandArgument("You must enter a valid exchange type.");
    }

}
