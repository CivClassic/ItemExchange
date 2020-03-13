package com.untamedears.itemexchange.rules.additional;

import com.untamedears.itemexchange.rules.ExchangeData;
import com.untamedears.itemexchange.rules.ExchangeRule;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.javatuples.Pair;
import vg.civcraft.mc.civmodcore.api.EnchantAPI;
import vg.civcraft.mc.civmodcore.api.EnchantNames;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.serialization.NBTCompound;
import vg.civcraft.mc.civmodcore.util.NullCoalescing;
import static vg.civcraft.mc.civmodcore.util.NullCoalescing.chain;

public final class EnchantStorageAdditional extends ExchangeData {

    @Override
    public void trace(ItemStack item) {
        ItemAPI.handleItemMeta(item, (EnchantmentStorageMeta meta) -> {
            setEnchants(meta.getStoredEnchants());
            return false;
        });
    }

    @Override
    public boolean conforms(ItemStack item) {
        boolean[] conforms = { false };
        ItemAPI.handleItemMeta(item, (EnchantmentStorageMeta meta) -> {
            if (meta.hasStoredEnchants() != hasEnchants()) {
                return false;
            }
            Map<Enchantment, Integer> heldEnchants = getEnchants();
            Map<Enchantment, Integer> metaEnchants = meta.getStoredEnchants();
            if (metaEnchants.size() < heldEnchants.size()) {
                return false;
            }
            for (Map.Entry<Enchantment, Integer> entry : heldEnchants.entrySet()) {
                if (!metaEnchants.containsKey(entry.getKey())) {
                    return false;
                }
                if (entry.getValue() != ExchangeRule.ANY) {
                    if (!metaEnchants.get(entry.getKey()).equals(entry.getValue())) {
                        return false;
                    }
                }
            }
            conforms[0] = true;
            return false;
        });
        return conforms[0];
    }

    @Override
    public List<String> getDisplayedInfo() {
        if (!hasEnchants()) {
            return Collections.singletonList(ChatColor.DARK_AQUA + "No stored enchants.");
        }
        else {
            return Collections.singletonList(ChatColor.DARK_AQUA + "Stored enchants: " +
                    ChatColor.YELLOW + getEnchants().entrySet().stream().
                    filter((entry) -> EnchantAPI.isSafeEnchantment(entry.getKey(), entry.getValue())).
                    map((entry) -> NullCoalescing.chain(() ->
                            EnchantNames.findByEnchantment(entry.getKey()).getAbbreviation(), "UNKNOWN") +
                            entry.getValue()).
                    collect(Collectors.joining(" ")));
        }
    }

    public boolean hasEnchants() {
        return this.nbt.hasKey("bookEnchants");
    }

    public Map<Enchantment, Integer> getEnchants() {
        return Arrays.
                stream(this.nbt.getCompoundArray("bookEnchants")).
                collect(Collectors.toMap((nbt) ->
                        Enchantment.getByName(nbt.getString("enchant")), (nbt) -> nbt.getInteger("level")));
    }

    public void setEnchants(Map<Enchantment, Integer> enchants) {
        this.nbt.setCompoundArray("bookEnchants", chain(() -> enchants.entrySet().stream().
                map(entry -> new NBTCompound() {{
                    setString("enchant", chain(() -> entry.getKey().getName()));
                    setInteger("level", entry.getValue());
                }}).
                toArray(NBTCompound[]::new)));
    }

    public static NBTCompound fromItem(ItemStack item) {
        EnchantStorageAdditional additional = new EnchantStorageAdditional();
        additional.trace(item);
        return additional.getNBT();
    }

    public static NBTCompound fromLegacy(String[] parts) {
        EnchantStorageAdditional additional = new EnchantStorageAdditional();
        additional.setEnchants(Arrays.stream(parts).
                map((raw) -> raw.split(ExchangeRule.TERTIARY_SPACER)).
                map((raw) -> new Pair<>(
                        NullCoalescing.chain(() -> EnchantAPI.getEnchantment(raw[0])),
                        NullCoalescing.chain(() -> Integer.parseInt(raw[1]), 0))).
                filter((pair) -> EnchantAPI.isSafeEnchantment(pair.getValue0(), pair.getValue1())).
                collect(Collectors.toMap(Pair::getValue0, Pair::getValue1)));
        return additional.getNBT();
    }

}
