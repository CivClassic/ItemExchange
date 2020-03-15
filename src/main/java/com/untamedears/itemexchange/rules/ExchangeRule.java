package com.untamedears.itemexchange.rules;

import com.google.common.base.Strings;
import com.untamedears.itemexchange.ItemExchangePlugin;
import com.untamedears.itemexchange.rules.additional.BookAdditional;
import com.untamedears.itemexchange.rules.additional.EnchantStorageAdditional;
import com.untamedears.itemexchange.rules.additional.PotionAdditional;
import com.untamedears.itemexchange.rules.additional.RepairAdditional;
import com.untamedears.itemexchange.rules.interfaces.ExchangeData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.untamedears.itemexchange.utility.Utilities;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.api.EnchantNames;
import vg.civcraft.mc.civmodcore.api.InventoryAPI;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.api.MaterialAPI;
import vg.civcraft.mc.civmodcore.itemHandling.NiceNames;
import vg.civcraft.mc.civmodcore.serialization.NBTCompound;
import vg.civcraft.mc.civmodcore.serialization.NBTSerializable;
import vg.civcraft.mc.civmodcore.serialization.NBTSerialization;
import vg.civcraft.mc.civmodcore.util.NullCoalescing;
import vg.civcraft.mc.namelayer.group.Group;
import static vg.civcraft.mc.civmodcore.util.NullCoalescing.chain;

public final class ExchangeRule extends ExchangeData {

    public enum Type {
        INPUT, OUTPUT, BROKEN
    }

    public static final String CATEGORY_SPACER = "&&&r";
    public static final String SECONDARY_SPACER = "&&r";
    public static final String TERTIARY_SPACER = "&r";

    public static final short NEW = 0;
    public static final short ANY = -1;
    public static final short USED = -2;
    public static final short ERROR = -99;

    public ExchangeRule() {
        this.nbt.setInteger("version", 2);
    }

    @Override
    public boolean isValid() {
        if (!Utilities.contains(getType(), Type.INPUT, Type.OUTPUT)) {
            return false;
        }
        if (!MaterialAPI.isValidItemMaterial(getMaterial())) {
            return false;
        }
        if (getAmount() < 1) {
            return false;
        }
        return true;
    }

    @Override
    public void trace(ItemStack item) {
        setMaterial(item.getType());
        if (MaterialAPI.hasDurability(item.getType())) {
            if (item.getDurability() > 0) {
                setDurability(USED);
            }
            else {
                setDurability(NEW);
            }
        }
        else if (MaterialAPI.hasDiscriminator(item.getType())) {
            setDurability(item.getDurability());
        }
        setAmount(item.getAmount());
        ItemAPI.handleItemMeta(item, (meta) -> {
            if (meta.hasDisplayName()) {
                setDisplayName(meta.getDisplayName());
            }
            if (meta.hasLore()) {
                setLore(meta.getLore());
            }
            if (meta.hasEnchants()) {
                setRequiredEnchants(meta.getEnchants());
            }
            return false;
        });
        switch (item.getType()) {
            case WRITTEN_BOOK:
                setExtra(BookAdditional.fromItem(item));
                break;
            case ENCHANTED_BOOK:
                setExtra(EnchantStorageAdditional.fromItem(item));
                break;
            case POTION:
            case SPLASH_POTION:
            case LINGERING_POTION:
                setExtra(PotionAdditional.fromItem(item));
                break;
            default:
                if (MaterialAPI.hasDurability(item.getType())) {
                    setExtra(RepairAdditional.fromItem(item));
                }
                break;
        }
    }

    @Override
    public boolean conforms(ItemStack item) {
        // If not the same material, return false
        Material material = getMaterial();
        if (!Objects.equals(material, item.getType())) {
            return false;
        }
        // If not the same durability, return false
        short durability = getDurability();
        if (item.getDurability() < 0) {
            return false;
        }
        if (MaterialAPI.hasDurability(material)) {
            if (durability == USED) {
                if (item.getDurability() == 0) {
                    return false;
                }
            }
            else if (durability == ANY) {
            }
            else {
                if (item.getDurability() != durability) {
                    return false;
                }
            }
        }
        // If the item has no amount, return false
        if (item.getAmount() <= 0) {
            return false;
        }
        // Check meta stuff
        boolean[] conforms = { false };
        ItemAPI.handleItemMeta(item, (meta) -> {
            Map<Enchantment, Integer> ruleEnchants = getRequiredEnchants();
            Map<Enchantment, Integer> metaEnchants = meta.getEnchants();
            if (!Utilities.conformsRequiresEnchants(ruleEnchants, metaEnchants)) {
                return false;
            }
            Set<Enchantment> exclEnchants = getExcludedEnchants();
            if (!exclEnchants.isEmpty()) {
                if (!Collections.disjoint(metaEnchants.keySet(), exclEnchants)) {
                    return false;
                }
            }
            if (!isIgnoringDisplayName()) {
                if (!Objects.equals(meta.getDisplayName(), getDisplayName())) {
                    return false;
                }
            }
            List<String> metaLore = meta.hasLore() ? meta.getLore() : Collections.emptyList();
            List<String> mustLore = getLore();
            if (metaLore.size() != mustLore.size()) {
                return false;
            }
            for (int i = 0; i < mustLore.size(); i++) {
                if (!Objects.equals(metaLore.get(i), mustLore.get(i))) {
                    return false;
                }
            }
            conforms[0] = true;
            return false;
        });
        return conforms[0];
    }

    /**
     * Gets the details title of this exchange rule.
     *
     * @return Return this exchange rule's details title.
     *
     * @apiNote This is essentially the first line of {@link ExchangeRule#getRuleDetails()} but needs to be separate
     *              so that when creating a rule item, the item's display name is set to the title, and the remainder
     *              of the details is set to the item's lore.
     */
    private String getRuleTitle() {
        String title = "" + ChatColor.YELLOW;
        switch (getType()) {
            case INPUT:
                title += "Input";
                break;
            case OUTPUT:
                title += "Output";
                break;
            default:
                title += "Broken";
                break;
        }
        return title + " " + ChatColor.WHITE + getAmount() + " " + getListing();
    }

    private List<String> getRuleDetails() {
        List<String> info = new ArrayList<>();
        if (isIgnoringDisplayName()) {
            info.add(ChatColor.GOLD + "Ignoring display name");
        }
        if (ItemExchangePlugin.CAN_ENCHANT.contains(getMaterial())) {
            for (Map.Entry<Enchantment, Integer> requiredEnchant : getRequiredEnchants().entrySet()) {
                if (requiredEnchant.getValue() == ANY) {
                    info.add(ChatColor.AQUA +
                            EnchantNames.findByEnchantment(requiredEnchant.getKey()).getDisplayName());
                }
                else {
                    info.add(ChatColor.AQUA +
                            EnchantNames.findByEnchantment(requiredEnchant.getKey()).getDisplayName() + " " +
                            requiredEnchant.getValue());
                }
            }
            for (Enchantment excludedEnchant : getExcludedEnchants()) {
                info.add(ChatColor.RED + "!" + EnchantNames.findByEnchantment(excludedEnchant).getDisplayName());
            }
            if (isAllowingUnlistedEnchants()) {
                info.add(ChatColor.GREEN + "Other enchantments allowed");
            }
        }
        for (String line : getLore()) {
            if (!Strings.isNullOrEmpty(line)) {
                info.add("" + ChatColor.DARK_PURPLE + ChatColor.ITALIC + line);
            }
        }
        if (MaterialAPI.hasDurability(getMaterial())) {
            switch (getDurability()) {
                case ExchangeRule.ANY:
                    info.add(ChatColor.GOLD + "Any durability");
                    break;
                case ExchangeRule.USED:
                    info.add(ChatColor.GOLD + "Used only");
                    break;
                default:
                    break;
            }
        }
        NullCoalescing.exists(getGroup(), (group) -> {
            info.add(ChatColor.RED + "Restricted to " + group.getName());
        });
        return info;
    }

    @Override
    public List<String> getDisplayedInfo() {
        List<String> info = new ArrayList<>();
        info.add(getRuleTitle());
        info.addAll(getRuleDetails());
        return info;
    }

    public Type getType() {
        String raw = this.nbt.getString("type");
        return raw == null ? Type.BROKEN :
                "INPUT".equalsIgnoreCase(raw) ? Type.INPUT :
                "OUTPUT".equalsIgnoreCase(raw) ? Type.OUTPUT :
                Type.BROKEN;
    }

    public void setType(Type type) {
        checkLocked();
        if (type == null) {
            this.nbt.remove("type");
        }
        else {
            this.nbt.setString("type", type.name());
        }
    }

    public void switchIO() {
        checkLocked();
        switch (getType()) {
            case INPUT:
                setType(Type.OUTPUT);
                break;
            case OUTPUT:
                setType(Type.INPUT);
                break;
            default:
                break;
        }
    }

    public String getListing() {
        String listing = this.nbt.getString("listing");
        if (Strings.isNullOrEmpty(listing)) {
            Material material = getMaterial();
            boolean discriminator = MaterialAPI.hasDiscriminator(material);
            short durability = discriminator ? getDurability() : 0;
            String niceName = NiceNames.getName(new ItemStack(material, 1, durability));
            if (!Strings.isNullOrEmpty(niceName)) {
                return niceName;
            }
            niceName = material + (discriminator ? ":" + durability : "");
            String displayName = getDisplayName();
            if (!Strings.isNullOrEmpty(displayName)) {
                niceName += " " + ChatColor.WHITE + ChatColor.ITALIC + "\"" +
                        displayName +  ChatColor.WHITE + ChatColor.ITALIC + "\"";
            }
            return niceName;
        }
        return listing;
    }

    public void setListing(String listing) {
        checkLocked();
        this.nbt.setString("listing", listing);
    }

    public Material getMaterial() {
        Material material = MaterialAPI.getMaterial(this.nbt.getString("material"));
        if (material == null) {
            return Material.AIR;
        }
        return material;
    }

    public void setMaterial(Material material) {
        checkLocked();
        this.nbt.setString("material", chain(material::name));
    }

    public short getDurability() {
        return this.nbt.getShort("durability");
    }

    public void setDurability(short durability) {
        checkLocked();
        if (durability != 0) {
            this.nbt.setShort("durability", durability);
        }
        else {
            this.nbt.remove("durability");
        }
    }

    public int getAmount() {
        return this.nbt.getInteger("amount");
    }

    public void setAmount(int amount) {
        checkLocked();
        this.nbt.setInteger("amount", amount);
    }

    public Map<Enchantment, Integer> getRequiredEnchants() {
        return Arrays.
                stream(this.nbt.getCompoundArray("requiredEnchants")).
                collect(Collectors.toMap((nbt) ->
                        Enchantment.getByName(nbt.getString("enchant")),
                        (nbt) -> nbt.getInteger("level")));
    }

    public void setRequiredEnchants(Map<Enchantment, Integer> requiredEnchants) {
        checkLocked();
        this.nbt.setCompoundArray("requiredEnchants", chain(() -> requiredEnchants.entrySet().stream().
                map(entry -> new NBTCompound() {{
                    setString("enchant", chain(() -> entry.getKey().getName()));
                    setInteger("level", entry.getValue());
                }}).
                toArray(NBTCompound[]::new)));
    }

    public Set<Enchantment> getExcludedEnchants() {
        return Arrays.stream(nbt.getStringArray("excludedEnchants")).
                map(Enchantment::getByName).
                collect(Collectors.toSet());
    }

    public void setExcludedEnchants(Set<Enchantment> excludedEnchants) {
        checkLocked();
        this.nbt.setStringArray("excludedEnchants", chain(() -> excludedEnchants.stream().
                map(entry -> chain(entry::getName)).
                toArray(String[]::new)));
    }

    public boolean isAllowingUnlistedEnchants() {
        return this.nbt.getBoolean("allowingUnlistedEnchants");
    }

    public void setAllowingUnlistedEnchants(boolean allowingUnlistedEnchants) {
        checkLocked();
        this.nbt.setBoolean("allowingUnlistedEnchants", allowingUnlistedEnchants);
    }

    public String getDisplayName() {
        return this.nbt.getString("displayName");
    }

    public void setDisplayName(String displayName) {
        checkLocked();
        this.nbt.setString("displayName", displayName);
    }

    public boolean isIgnoringDisplayName() {
        return this.nbt.getBoolean("ignoringDisplayName");
    }

    public void setIgnoringDisplayName(boolean ignoringDisplayName) {
        checkLocked();
        if (ignoringDisplayName) {
            this.nbt.setBoolean("ignoringDisplayName", true);
        }
        else {
            this.nbt.remove("ignoringDisplayName");
        }
    }

    public List<String> getLore() {
        return Arrays.asList(this.nbt.getStringArray("lore"));
    }

    public void setLore(List<String> lore) {
        checkLocked();
        this.nbt.setStringArray("lore", lore == null ? null : lore.toArray(new String[0]));
    }

    public Group getGroup() {
        return Utilities.getGroupFromName(this.nbt.getString("group"));
    }

    public void setGroup(Group group) {
        checkLocked();
        if (group == null) {
            this.nbt.remove("group");
        }
        else {
            this.nbt.setString("group", group.getName());
        }
    }

    public NBTCompound getExtra() {
        return this.nbt.getCompound("extra");
    }

    public void setExtra(NBTCompound extra) {
        checkLocked();
        this.nbt.setCompound("extra", extra);
    }

    public int calculateStock(Inventory inventory) {
        if (!InventoryAPI.isValidInventory(inventory)) {
            return 0;
        }
        int amount = 0;
        for (ItemStack item : inventory.getContents()) {
            if (!ItemAPI.isValidItem(item) || !conforms(item)) {
                continue;
            }
            amount += item.getAmount();
        }
        if (amount <= 0) {
            return 0;
        }
        return Math.max(amount / getAmount(), 0);
    }

    public ItemStack[] getStock(Inventory inventory) {
        ArrayList<ItemStack> stock = new ArrayList<>();
        if (!InventoryAPI.isValidInventory(inventory)) {
            return new ItemStack[0];
        }
        int requiredAmount = getAmount();
        for (ItemStack item : inventory.getContents()) {
            if (requiredAmount <= 0) {
                break;
            }
            if (!ItemAPI.isValidItem(item)) {
                continue;
            }
            if (!conforms(item)) {
                continue;
            }
            if (item.getAmount() <= requiredAmount) {
                stock.add(item.clone());
                requiredAmount -= item.getAmount();
            }
            else {
                ItemStack clone = item.clone();
                clone.setAmount(requiredAmount);
                stock.add(clone);
                requiredAmount = 0;
            }
        }
        return stock.toArray(new ItemStack[0]);
    }

    public ItemStack toItem() {
        ItemStack item = NBTCompound.processItem(ItemExchangePlugin.RULE_ITEM.clone(), (nbt) ->
                nbt.setCompound("ExchangeRule", NBTSerialization.serialize(this)));
        ItemAPI.handleItemMeta(item, (meta) -> {
            meta.setDisplayName(getRuleTitle());
            meta.setLore(getRuleDetails());
            return true;
        });
        return item;
    }

    public static ExchangeRule fromItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        if (item.getType() != ItemExchangePlugin.RULE_ITEM.getType()) {
            return null;
        }
        NBTCompound nbt = NBTCompound.fromItem(item).getCompound("ExchangeRule");
        if (!nbt.isEmpty()) {
            NBTSerializable serializable = NBTSerialization.deserialize(nbt);
            if (serializable instanceof ExchangeRule) {
                return (ExchangeRule) serializable;
            }
        }
        // Allow Legacy Parsing
        String line = chain(() -> item.getItemMeta().getLore().get(0));
        if (!Strings.isNullOrEmpty(line)) {
            ExchangeRule rule = Utilities.parseLegacyRuleString(line);
            if (rule != null && rule.isValid()) {
                return rule;
            }
        }
        return null;
    }

}
