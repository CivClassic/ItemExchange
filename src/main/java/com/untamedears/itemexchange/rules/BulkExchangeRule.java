package com.untamedears.itemexchange.rules;

import com.google.common.base.Strings;
import com.untamedears.itemexchange.ItemExchangePlugin;
import com.untamedears.itemexchange.utility.Utilities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.api.NBTCompound;
import vg.civcraft.mc.civmodcore.serialization.NBTSerializable;
import vg.civcraft.mc.civmodcore.serialization.NBTSerialization;
import static vg.civcraft.mc.civmodcore.util.NullCoalescing.chain;

public final class BulkExchangeRule extends ExchangeData {

    public BulkExchangeRule() {
    }

    public boolean isValid() {
        if (getRules().isEmpty()) {
            return false;
        }
        return true;
    }

    public List<ExchangeRule> getRules() {
        return Arrays.stream(this.nbt.getCompoundArray("rules")).
                map(NBTSerialization::deserialize).
                filter((serializable) -> serializable instanceof ExchangeRule).
                map((serializable) -> (ExchangeRule) serializable).
                collect(Collectors.toCollection(ArrayList::new));
    }

    public void setRules(List<ExchangeRule> rules) {
        if (rules == null) {
            this.nbt.remove("rules");
        }
        else {
            this.nbt.setCompoundArray("rules", rules.stream().
                    filter(Objects::nonNull).
                    map((rule) -> {
                        NBTCompound nbt = new NBTCompound();
                        rule.serialize(nbt);
                        return nbt;
                    }).
                    filter((nbt) -> !nbt.isEmpty()).
                    toArray(NBTCompound[]::new));
        }
    }

    @Override
    public void trace(ItemStack item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean conforms(ItemStack item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getDisplayedInfo() {
        return new ArrayList<>();
    }

    public ItemStack toItem() {
        ItemStack item = NBTCompound.processItem(ItemExchangePlugin.RULE_ITEM.clone(), (nbt) -> nbt.setCompound("BulkExchangeRule", NBTSerialization.serialize(this)));
        ItemAPI.handleItemMeta(item, (meta) -> {
            meta.setDisplayName(ChatColor.RED + "Bulk Rule Block");
            meta.setLore(Collections.singletonList("This rule block holds " + getRules().size() + " exchange rule" + (getRules().size() == 1 ? "" : "s") + "."));
            return true;
        });
        return item;
    }

    public static BulkExchangeRule fromItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        if (item.getType() != ItemExchangePlugin.RULE_ITEM.getType()) {
            return null;
        }
        NBTCompound nbt = NBTCompound.fromItem(item).getCompound("ExchangeRule");
        if (!nbt.isEmpty()) {
            NBTSerializable serializable = NBTSerialization.deserialize(nbt);
            if (serializable instanceof BulkExchangeRule) {
                return (BulkExchangeRule) serializable;
            }
        }
        // Allow Legacy Parsing
        String line = chain(() -> item.getItemMeta().getLore().get(1));
        if (!Strings.isNullOrEmpty(line)) {
            String[] data = line.split("§&§&§&§&§r");
            // We need to do the following because CivClassics done goofed.
            String badmin = "�&�&�&�&�r";
            String[] crimes = line.split(badmin);
            if (crimes.length > data.length) {
                data = crimes;
            }
            // Just in case this "bulk" rule only has one rule, meaning that the above
            // if statement doesn't trigger and there's a trailing badmin crime.
            else {
                data[0] = data[0].replace(badmin, "");
            }
            // -- done goofed accounted for.
            List<ExchangeRule> rules = Arrays.stream(data).
                    map(Utilities::parseLegacyRuleString).
                    filter(Objects::nonNull).
                    collect(Collectors.toCollection(ArrayList::new));
            if (rules.isEmpty()) {
                return null;
            }
            BulkExchangeRule rule = new BulkExchangeRule();
            rule.setRules(rules);
            if (rule.isValid()) {
                return rule;
            }
        }
        return null;
    }

}
