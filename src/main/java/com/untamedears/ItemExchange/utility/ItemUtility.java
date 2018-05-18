package com.untamedears.ItemExchange.utility;

import com.untamedears.ItemExchange.DeprecatedMethods;
import com.untamedears.ItemExchange.ItemExchangePlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemUtility {

	public static String getItemStackName(ItemStack itemStack) {
		ItemStack newStack = new ItemStack(itemStack.getType(), 1, itemStack.getDurability());
		if (ItemExchangePlugin.MATERIAL_NAME.containsKey(newStack)) {
			return ItemExchangePlugin.MATERIAL_NAME.get(newStack);
		}
		else if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
			return itemStack.getItemMeta().getDisplayName();
		}
		else {
			int itemId = DeprecatedMethods.getItemId(newStack);
			return itemId + ":" + newStack.getDurability();
		}
	}

	public static String getItemStackName(Material material, short durability) {
		ItemStack newStack = new ItemStack(material, 1, durability);
		return getItemStackName(newStack);
	}
	
}
