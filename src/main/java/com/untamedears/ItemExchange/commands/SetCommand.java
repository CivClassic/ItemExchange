package com.untamedears.ItemExchange.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Split;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.google.common.base.Strings;
import com.untamedears.ItemExchange.ItemExchangePlugin;
import com.untamedears.ItemExchange.utility.Utilities;
import com.untamedears.ItemExchange.exceptions.ExchangeRuleParseException;
import com.untamedears.ItemExchange.utility.ExchangeRule;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.api.MaterialAPI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.group.Group;

@SuppressWarnings("unused")
@CommandAlias("ieset|ies")
public class SetCommand extends BaseCommand {

    public static final SetCommand INSTANCE = new SetCommand();

    private static final Pattern SET_ENCHANT_PATTERN = Pattern.compile("^([+?\\-])(\\w+)(\\d*)$");

    private SetCommand() { }

    @Default
    @Description("Sets a pertinent field to an exchange rule.")
    @Syntax("<field> [...values]")
    public void base(Player player) {
        throw new InvalidCommandArgument();
    }

    @Subcommand("material|mat|m|c|commonname")
    @Description("Sets the material of an exchange rule.")
    @Syntax("<material>")
    @CommandCompletion("@materials")
    public void setMaterial(Player player, String slug) {
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        Material material = Material.getMaterial(slug.toUpperCase());
        if (!MaterialAPI.isValidItemMaterial(material)) {
            throw new InvalidCommandArgument("You must enter a valid item material.");
        }
        player.sendMessage(ChatColor.GREEN + "Material successfully changed.");
        rule.setMaterial(material);
        saveExchangeRuleChanges(player, rule);
    }

    @Subcommand("amount|num|number|a")
    @Description("Sets the amount of an exchange rule.")
    @Syntax("<amount>")
    public void setAmount(Player player, int amount) {
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        if (amount <= 0) {
            throw new InvalidCommandArgument("You must enter a valid amount.");
        }
        player.sendMessage(ChatColor.GREEN + "Amount successfully changed.");
        rule.setAmount(amount);
        saveExchangeRuleChanges(player, rule);
    }

    @Subcommand("durability|d")
    @Description("Sets the durability of an exchange rule.")
    @Syntax("<durability>")
    public void setDurability(Player player, short value) {
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        if (!MaterialAPI.usesDurability(rule.getMaterial())) {
            throw new InvalidCommandArgument("Cannot set the durability for that item.");
        }
        rule.setDurability(value);
        player.sendMessage(ChatColor.GREEN + "Durability successfully changed.");
        saveExchangeRuleChanges(player, rule);
    }

    @Subcommand("allowenchantments|allowenchants")
    @Description("Allows items with unspecified enchantments to be bought and sold.")
    public void allowUnlistedEnchantments(Player player) {
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        player.sendMessage(ChatColor.GREEN + "Unlisted enchantments are now allowed.");
        rule.setUnlistedEnchantmentsAllowed(true);
        saveExchangeRuleChanges(player, rule);
    }

    @Subcommand("allowenchantments|allowenchants")
    @Description("Disallows items with unspecified enchantments to be bought and sold.")
    public void disallowUnlistedEnchantments(Player player) {
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        player.sendMessage(ChatColor.GREEN + "Unlisted enchantments are now denied.");
        rule.setUnlistedEnchantmentsAllowed(false);
        saveExchangeRuleChanges(player, rule);
    }

    @Subcommand("enchantment|e")
    @Description("Disallows items with unspecified enchantments to be bought and sold.")
    @Syntax("<+/?/-><enchantment>[level]")
    public void setEnchantment(Player player, String details) {
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        if (Strings.isNullOrEmpty(details)) {
            throw new InvalidCommandArgument("You must enter an enchantment.");
        }
        Matcher matcher = SET_ENCHANT_PATTERN.matcher(details);
        if (!matcher.matches()) {
            throw new InvalidCommandArgument("You must enter a valid instruction.");
        }
        String _enchantment = matcher.group(2);
        Enchantment enchantment = Enchantment.getByName(_enchantment.toUpperCase());
        if (enchantment == null) {
            enchantment = Utilities.getEnchantmentByAbbreviation(matcher.group(2));
        }
        if (enchantment == null) {
            throw new InvalidCommandArgument("You must enter a valid enchantment.");
        }
        switch (matcher.group(1)) {
            case "+": {
                int level = 0;
                try {
                    level = Math.max(Integer.parseInt(matcher.group(3)), 0);
                }
                catch (NumberFormatException ignored) { } // No need to error here because it'll error below
                if (level < enchantment.getStartLevel() || level > enchantment.getMaxLevel()) {
                    throw new InvalidCommandArgument("You must enter a valid level.");
                }
                rule.requireEnchantment(enchantment, level);
                rule.removeExcludedEnchantment(enchantment);
                player.sendMessage(ChatColor.GREEN + "Successfully added required enchantment.");
                break;
            }
            case "-": {
                rule.excludeEnchantment(enchantment);
                rule.removeRequiredEnchantment(enchantment);
                player.sendMessage(ChatColor.GREEN + "Successfully added excluded enchantment.");
                break;
            }
            case "?": {
                rule.removeRequiredEnchantment(enchantment);
                rule.removeExcludedEnchantment(enchantment);
                player.sendMessage(ChatColor.GREEN + "Successfully removed rules relating to enchantment.");
                break;
            }
            default: {
                throw new InvalidCommandArgument("You entered an invalid instruction.");
            }
        }
        saveExchangeRuleChanges(player, rule);
    }

    @Subcommand("displayname|n")
    @Description("Sets or resets the item's display name.")
    @Syntax("[name]")
    public void setDisplayName(Player player, @Optional String value) {
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        if (Strings.isNullOrEmpty(value)) {
            player.sendMessage(ChatColor.GREEN + "Successfully removed display name.");
            rule.setDisplayName("");
        }
        else {
            player.sendMessage(ChatColor.GREEN + "Successfully changed display name.");
            rule.setDisplayName(value);
        }
        saveExchangeRuleChanges(player, rule);
    }

    @Subcommand("lore|l")
    @Description("Sets or resets the item's lore.")
    @Syntax("[...lore]")
    public void setLore(Player player, @Optional @Split(";") String[] value) {
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        if (value == null || value.length < 1) {
            player.sendMessage(ChatColor.GREEN + "Successfully removed lore.");
            rule.setLore(new String[0]);
        }
        else {
            player.sendMessage(ChatColor.GREEN + "Successfully changed lore.");
            rule.setLore(value);
        }
        saveExchangeRuleChanges(player, rule);
    }

    @Subcommand("group")
    @Description("Sets or resets the exchange's group exclusivity.")
    @Syntax("[group]")
    public void setGroup(Player player, @Optional String value) {
        if (!Bukkit.getPluginManager().isPluginEnabled("NameLayer")) {
            throw new InvalidCommandArgument("NameLayer is not enabled.");
        }
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        if (rule.getType() != ExchangeRule.RuleType.INPUT) {
            throw new InvalidCommandArgument("You can only set that on input rules.");
        }
        if (Strings.isNullOrEmpty(value)) {
            player.sendMessage(ChatColor.GREEN + "Successfully removed Citadel group.");
            rule.setCitadelGroup(null);
            return;
        }
        Group group = GroupManager.getGroup(value);
        if (ItemExchangePlugin.ESTABLISH_PERMISSION.hasAccess(group, player)) {
            throw new InvalidCommandArgument("You must enter a group you have permissions for.");
        }
        player.sendMessage(ChatColor.GREEN + "Successfully changed Citadel group.");
        rule.setCitadelGroup(group);
        saveExchangeRuleChanges(player, rule);
    }

    @Subcommand("switchio|switch|swap|swapio|s")
    @Description("Sets the amount of an exchange rule.")
    public void switchIO(Player player) {
        ExchangeRule rule = ensureHoldingExchangeRule(player);
        player.sendMessage(ChatColor.GREEN + "Type successfully switched.");
        rule.switchIO();
        saveExchangeRuleChanges(player, rule);
    }

    private ExchangeRule ensureHoldingExchangeRule(Player player) {
        try {
            return ExchangeRule.parseRuleBlock(player.getInventory().getItemInMainHand());
        }
        catch (ExchangeRuleParseException ignored) {
            throw new InvalidCommandArgument("You must be holding an exchange rule to modify it.");
        }
    }

    private void saveExchangeRuleChanges(Player player, ExchangeRule rule) {
        player.getInventory().setItemInMainHand(rule.toItemStack());
    }

}
