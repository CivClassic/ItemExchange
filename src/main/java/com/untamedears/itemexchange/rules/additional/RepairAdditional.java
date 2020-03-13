package com.untamedears.itemexchange.rules.additional;

import com.untamedears.itemexchange.rules.ExchangeData;
import java.util.Collections;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.api.MaterialAPI;
import vg.civcraft.mc.civmodcore.serialization.NBTCompound;

/**
 * This additional represents a repair level condition.
 *
 * Positive integers and zero mean that repair level specifically, eg: RepairCost == 15
 * Negative integers mean that that repair level or lower, eg: RepairCost <= 15
 *
 */
public final class RepairAdditional extends ExchangeData {

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void trace(ItemStack item) {
        if (MaterialAPI.usesDurability(item.getType())) {
            setRepairCost(ItemAPI.getItemRepairCost(item));
        }
    }

    @Override
    public boolean conforms(ItemStack item) {
        if (MaterialAPI.usesDurability(item.getType())) {
            return false;
        }
        int ruleRepair = getRepairCost();
        int itemRepair = ItemAPI.getItemRepairCost(item);
        if (ruleRepair >= 0) {
            return itemRepair == ruleRepair;
        }
        else {
            return itemRepair <= ruleRepair;
        }
    }

    @Override
    public List<String> getDisplayedInfo() {
        int repairCost = getRepairCost();
        if (repairCost == 0) {
            return Collections.singletonList(ChatColor.GOLD + "Mint condition");
        }
        else if (repairCost > 0) {
            return Collections.singletonList(ChatColor.GOLD + "Repair level " + (repairCost + 2));
        }
        else {
            return Collections.singletonList(ChatColor.GOLD + "Repair level " + (repairCost * -1 + 2) + " or less");
        }
    }

    public int getRepairCost() {
        return this.nbt.getInteger("repairLevel");
    }

    public void setRepairCost(int repairLevel) {
        this.nbt.setInteger("repairLevel", repairLevel);
    }

    public static NBTCompound fromItem(ItemStack item) {
        RepairAdditional additional = new RepairAdditional();
        additional.trace(item);
        return additional.getNBT();
    }

}
