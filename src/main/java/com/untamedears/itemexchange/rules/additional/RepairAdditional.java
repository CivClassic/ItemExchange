package com.untamedears.itemexchange.rules.additional;

import com.untamedears.itemexchange.rules.interfaces.AdditionalData;
import java.util.Collections;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

/**
 * This additional represents a repair level condition.
 *
 * Positive integers and zero mean that repair level specifically, eg: RepairCost == 15
 * Negative integers mean that that repair level or lower, eg: RepairCost <= 15
 */
public final class RepairAdditional extends AdditionalData {

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void trace(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Repairable) {
            setRepairCost(((Repairable) meta).getRepairCost());
        }
    }

    @Override
    public boolean conforms(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Repairable)) {
            return false;
        }
        int ruleRepair = getRepairCost();
        int itemRepair = ((Repairable) meta).getRepairCost();
        if (ruleRepair >= 0) {
            return itemRepair == ruleRepair;
        }
        else {
            return itemRepair <= ruleRepair * -1;
        }
    }

    @Override
    public List<String> getDisplayedInfo() {
        int repairCost = getRepairCost();
        if (repairCost == 0) {
            return Collections.singletonList(ChatColor.GOLD + "Never repaired");
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

    public static AdditionalData fromItem(ItemStack item) {
        RepairAdditional additional = new RepairAdditional();
        additional.trace(item);
        return additional;
    }

}
