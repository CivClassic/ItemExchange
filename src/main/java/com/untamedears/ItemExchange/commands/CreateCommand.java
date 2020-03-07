package com.untamedears.ItemExchange.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Split;
import co.aikar.commands.annotation.Syntax;
import com.google.common.base.Strings;
import com.untamedears.ItemExchange.utility.ExchangeRule;
import com.untamedears.ItemExchange.utility.ExchangeRule.RuleType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.api.MaterialAPI;

@SuppressWarnings("unused")
@CommandAlias(CreateCommand.ALIAS)
public class CreateCommand extends BaseCommand {

    public static final String ALIAS = "iecreate|iec";

    public static final CreateCommand INSTANCE = new CreateCommand();

    private CreateCommand() { }

    @Default
    @Description("Creates an exchange rule based on a shop block.")
    public void createFromShop(Player player) {
        throw new InvalidCommandArgument("Coming soon!");
//        //If no input or ouptut is specified player attempt to set up ItemExchange at the block the player is looking at
//        //The player must have citadel access to the inventory block
//        if (args.length == 0) {
//            BlockIterator iter = new BlockIterator(player,6);
//            while(iter.hasNext()) {
//                Block block = iter.next();
//                if (ItemExchangePlugin.ACCEPTABLE_BLOCKS.contains(block.getState().getType())) {
//                    PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, BlockFace.UP);
//
//                    Bukkit.getPluginManager().callEvent(event);
//
//                    if(!event.isCancelled())
//                        player.sendMessage(ItemExchange.createExchange(block.getLocation(), player));
//                    return true;
//                }
//            }
//            player.sendMessage(ChatColor.RED + "No block in view is suitable for an Item Exchange.");
//        }
    }

    @CommandAlias(CreateCommand.ALIAS)
    @Syntax("<type>")
    @Description("Creates an exchange rule based on a held item.")
    @CommandCompletion("@types")
    public void base(Player player, String type) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemAPI.isValidItem(held)) {
            throw new InvalidCommandArgument("You must be holding an item to do that.");
        }
        giveExchangeRule(player, new ExchangeRule(
                held.getType(),
                held.getAmount(),
                held.getDurability(),
                matchType(type)));
    }

    @CommandAlias(CreateCommand.ALIAS)
    @Syntax("<type> <material> [amount]")
    @Description("Sets the material of an exchange rule.")
    @CommandCompletion("@types @materials")
    public void createExchange(Player player, String type, @Split(":") String[] slug, @Default("1") int amount) {
        Material material = Material.getMaterial(slug[0].toUpperCase());
        if (!MaterialAPI.isValidItemMaterial(material)) {
            throw new InvalidCommandArgument("You must enter a valid item material.");
        }
        short durability = 0;
        if (slug.length > 1) {
            try {
                durability = Short.parseShort(slug[1]);
            }
            catch (NumberFormatException ignored) {
                durability = -1;
            }
            if (durability < 0) {
                throw new InvalidCommandArgument("You must enter a valid durability.");
            }
        }
        if (amount <= 0) {
            throw new InvalidCommandArgument("You must enter a valid amount.");
        }
        giveExchangeRule(player, new ExchangeRule(
                material,
                amount,
                durability,
                matchType(type)));
    }

    private RuleType matchType(String value) {
        if (!Strings.isNullOrEmpty(value)) {
            switch (value.toLowerCase()) {
                case "i":
                case "in":
                case "input":
                case "inputs":
                    return RuleType.INPUT;
                case "o":
                case "out":
                case "output":
                case "outputs":
                    return RuleType.OUTPUT;
            }
        }
        throw new InvalidCommandArgument("You must enter a valid exchange type.");
    }

    private void giveExchangeRule(Player player, ExchangeRule rule) {
        PlayerInventory inventory = player.getInventory();
        int index = inventory.firstEmpty();
        if (index < 0) {
            throw new InvalidCommandArgument("You need an empty slot to create an exchange rule.");
        }
        inventory.setItem(index, rule.toItemStack());
    }

}
