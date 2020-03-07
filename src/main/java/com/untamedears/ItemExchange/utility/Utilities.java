package com.untamedears.ItemExchange.utility;

import java.util.Map;
import java.util.TreeMap;
import com.google.common.base.Strings;
import com.untamedears.ItemExchange.ItemExchangePlugin;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.itemHandling.NiceNames;

public final class Utilities {

    // Roman numeral code originates from: https://stackoverflow.com/a/19759564
    private static final TreeMap<Integer, String> romanNumerals = new TreeMap<Integer, String>() {{
        put(1000, "M");
        put(900, "CM");
        put(500, "D");
        put(400, "CD");
        put(100, "C");
        put(90, "XC");
        put(50, "L");
        put(40, "XL");
        put(10, "X");
        put(9, "IX");
        put(5, "V");
        put(4, "IV");
        put(1, "I");
    }};

    public static String generateRomanNumerals(int number) {
        if (number <= 0) {
            return "";
        }
        int key = romanNumerals.floorKey(number);
        if (key == number) {
            return romanNumerals.get(number);
        }
        return romanNumerals.get(number) + generateRomanNumerals(number - key);
    }
    // END OF SEGMENT

    public static String getEnumName(@SuppressWarnings("rawtypes") Enum value) {
        if (value == null) {
            return null;
        }
        return value.name();
    }

    public static String getItemName(ItemStack item) {
        if (!ItemAPI.isValidItem(item)) {
            return "Invalid Item.";
        }
        String niceName = NiceNames.getName(item);
        if (!Strings.isNullOrEmpty(niceName)) {
            return niceName;
        }
        return item.getType() + ":" + item.getDurability();
    }

    public static Enchantment getEnchantmentByAbbreviation(String abbreviation) {
        if (Strings.isNullOrEmpty(abbreviation)) {
            return null;
        }
        for (Map.Entry<Enchantment, String> entry : ItemExchangePlugin.ENCHANT_ABBREVS.entrySet()) {
            if (abbreviation.equalsIgnoreCase(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

}
