package com.untamedears.ItemExchange.command.commands;

import com.untamedears.ItemExchange.DeprecatedMethods;
import com.untamedears.ItemExchange.ItemExchangePlugin;
import com.untamedears.ItemExchange.command.PlayerCommand;
import com.untamedears.ItemExchange.utility.ExchangeRule;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

/*
 * General command for creating either an entire ItemExchange or 
 * creating an exchange rule, given the context of the player when
 * the command is issued.
 */
public class RawCommand extends PlayerCommand {
	public RawCommand() {
		super("Print Raw");
		setDescription("Automatically creates an exchange inside the chest the player is looking at");
		setUsage("/ieraw");
		setArgumentRange(0, 0);
		setIdentifiers(new String[] { "ieraw", "ier" });
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		if (!player.isOp()) {
			player.sendMessage(ChatColor.RED + "You are not allowed to use that command!");
			return true;
		}
		ItemStack inHand = player.getInventory().getItemInMainHand();
		if(inHand == null || inHand.getType() == Material.AIR) {
			player.sendMessage(ChatColor.RED + "You are not holding anything in your hand!");
			return true;
		}
		int itemId = DeprecatedMethods.getItemId(inHand);
		int itemDur = inHand.getDurability();
		//
		StringBuilder message = new StringBuilder();
		// Show the player the id and durability
		message.append(ChatColor.WHITE);
		message.append("ID: " + DeprecatedMethods.getItemId(inHand));
		message.append(":" + inHand.getDurability());
		message.append('\n');
		// Show the player the material name, if it can be found
		if (ItemExchangePlugin.MATERIAL_NAME.containsKey(inHand)) {
			String name = ItemExchangePlugin.MATERIAL_NAME.get(inHand);
			message.append("Item: " + name).append('\n');
		}
		if (inHand.hasItemMeta()) {
			ItemMeta meta = inHand.getItemMeta();
			// Show the player the display name, if it has one
			if (meta.hasDisplayName()) {
				String name = meta.getDisplayName();
				message.append("Name: " + name).append('\n');
			}
			// Show the player the item's enchantments, if it has any
			if (meta.hasEnchants()) {
				message.append(ChatColor.AQUA);
				for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
					message.append(entry.getKey().getName()).append(" ");
					Integer level = entry.getValue();
					if (level > 0 && level - 1 < ItemExchangePlugin.NUMERALS.length) {
						message.append(ItemExchangePlugin.NUMERALS[level - 1]);
					}
					else {
						message.append(level);
					}
					message.append('\n');
				}
				message.append(ChatColor.WHITE);
			}
			// Show the player the item's lore, if it has any
			if (meta.hasLore()) {
				message.append(ChatColor.GRAY);
				for (String line : meta.getLore()) {
					if (ExchangeRule.useHiddenSpacers)
						line = ExchangeRule.unescapeString(ExchangeRule.unhideString(line));
					message.append(line).append('\n');
				}
				message.append(ChatColor.WHITE);
			}
			// Show the player if the item is unbreakable
			if (meta.isUnbreakable()) {
				message.append("Unbreakable.");
			}
		}
		//
		String result = message.toString();
		player.sendMessage(result);
		return true;
	}
}
