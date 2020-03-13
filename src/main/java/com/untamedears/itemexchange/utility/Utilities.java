package com.untamedears.itemexchange.utility;

import co.aikar.commands.InvalidCommandArgument;
import com.google.common.base.Strings;
import com.untamedears.itemexchange.ItemExchangePlugin;
import com.untamedears.itemexchange.rules.BulkExchangeRule;
import com.untamedears.itemexchange.rules.additional.BookAdditional;
import com.untamedears.itemexchange.rules.additional.EnchantStorageAdditional;
import com.untamedears.itemexchange.rules.additional.PotionAdditional;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import com.untamedears.itemexchange.rules.ExchangeRule;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement;
import vg.civcraft.mc.civmodcore.api.BlockAPI;
import vg.civcraft.mc.civmodcore.api.EnchantAPI;
import vg.civcraft.mc.civmodcore.api.InventoryAPI;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.api.MaterialAPI;
import vg.civcraft.mc.civmodcore.util.NullCoalescing;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.group.Group;
import static vg.civcraft.mc.civmodcore.util.NullCoalescing.chain;

public final class Utilities {

    public static Group getReinforcementGroupFromBlock(Block block) {
        return NullCoalescing.chain(() ->
                ((PlayerReinforcement) Citadel.getReinforcementManager().getReinforcement(block)).getGroup());
    }

    public static void givePlayerExchangeRule(Player player, ExchangeRule rule) {
        RuntimeException error = new InvalidCommandArgument("Could not create that rule.");
        Inventory inventory = NullCoalescing.chain(() -> player.getInventory());
        if (inventory == null || rule == null) {
            throw error;
        }
        if (!InventoryAPI.safelyAddItemsToInventory(inventory, new ItemStack[] { rule.toItem() })) {
            throw error;
        }
    }

    public static ExchangeRule ensureHoldingExchangeRule(Player player) {
        RuntimeException error = new InvalidCommandArgument("You must be holding an exchange rule.");
        ItemStack held = NullCoalescing.chain(() -> player.getInventory().getItemInMainHand());
        if (!ItemAPI.isValidItem(held)) {
            throw error;
        }
        ExchangeRule rule = ExchangeRule.fromItem(held);
        if (rule == null) {
            rule = ExchangeRule.fromItem(held);
        }
        if (rule == null) {
            throw error;
        }
        return rule;
    }

    public static void replaceHoldingExchangeRule(Player player, ExchangeRule rule) {
        RuntimeException error = new InvalidCommandArgument("Could not replace that rule.");
        if (player == null || rule == null) {
            throw error;
        }
        ItemStack item = rule.toItem();
        if (item == null) {
            throw error;
        }
        player.getInventory().setItemInMainHand(item);
    }

    public static void giveItemsOrDrop(Inventory inventory, ItemStack... items) {
        if (inventory == null || items == null || items.length < 1) {
            return;
        }
        items = Arrays.stream(items).filter(Objects::nonNull).toArray(ItemStack[]::new);
        for (Map.Entry<Integer, ItemStack> entry : inventory.addItem(items).entrySet()) {
            inventory.getLocation().getWorld().dropItem(inventory.getLocation(), entry.getValue());
        }
    }

    public static Group getGroupFromName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            return null;
        }
        return NullCoalescing.chain(() -> GroupManager.getGroup(name));
    }

    public static boolean isExchangeRule(ItemStack item) {
        if (ExchangeRule.fromItem(item) != null) {
            return true;
        }
        if (BulkExchangeRule.fromItem(item) != null) {
            return true;
        }
        return false;
    }

    private static boolean preliminaryLegacyRuleStringTest(String raw) {
        if (Strings.isNullOrEmpty(raw)) {
            return false;
        }
        if (raw.startsWith("§i§&§&§&§r&i§t§e§m§&§&§&§r§") ||
                raw.startsWith("§o§&§&§&§r&i§t§e§m§&§&§&§r§") ||
                raw.startsWith("�i�&�&�&�r�i�t�e�m�&�&�&�r�") ||
                raw.startsWith("�o�&�&�&�r�i�t�e�m�&�&�&�r�")) {
            return true;
        }
        return false;
    }


    public static ExchangeRule parseLegacyRuleString(String raw) {
        if (!preliminaryLegacyRuleStringTest(raw)) {
            Bukkit.getLogger().warning("not a legacy rule");
            return null;
        }
        String[] data = raw.replaceAll("[§|�](.)", "$1").
                replaceAll("\\\\([\\\\r])", "$1").
                split(ExchangeRule.CATEGORY_SPACER);
        if (data.length < 12) {
            Bukkit.getLogger().warning("legacy rule too short");
            return null;
        }
        ExchangeRule rule = new ExchangeRule();
        // (0) Exchange Direction
        rule.setType(
                "i".equals(data[0]) ? ExchangeRule.Type.INPUT :
                "o".equals(data[0]) ? ExchangeRule.Type.OUTPUT :
                null);
        // (1) Exchange Type
        if (!data[1].equals("item")) {
            return null;
        }
        // (2) Material
        rule.setMaterial(MaterialAPI.getMaterial(data[2]));
        // (3) Durability
        if (MaterialAPI.usesDurability(rule.getMaterial())) {
            rule.setDurability(chain(() -> Short.parseShort(data[3]), (short) 0));
        }
        // (4) Amount
        rule.setAmount(chain(() -> Integer.parseInt(data[4]), 0));
        // (5, 6, 7) Enchanting Data
        if (ItemExchangePlugin.CAN_ENCHANT.contains(rule.getMaterial())) {
            // (5) Required Enchantments
            rule.setRequiredEnchants(Arrays.
                    stream(data[5].split(ExchangeRule.SECONDARY_SPACER)).
                    filter((str) -> !Strings.isNullOrEmpty(str)).
                    map(str -> str.split(ExchangeRule.TERTIARY_SPACER)).
                    collect(Collectors.toMap((str) ->
                            EnchantAPI.getEnchantment(str[0]), (str) -> Integer.parseInt(str[1]))));
            // (6) Excluded Enchantments
            rule.setExcludedEnchants(Arrays.
                    stream(data[6].split(ExchangeRule.SECONDARY_SPACER)).
                    filter((str) -> !Strings.isNullOrEmpty(str)).
                    map(EnchantAPI::getEnchantment).
                    collect(Collectors.toSet()));
            // (7) Allow Unlisted Enchantments
            rule.setAllowingUnlistedEnchants("1".equals(data[7]));
        }
        // (8) Display Name
        if (!data[8].isEmpty()) {
            rule.setDisplayName(data[8]);
        }
        // (9) Lore
        if (!data[9].isEmpty()) {
            if (data[9].contains(ExchangeRule.SECONDARY_SPACER)) {
                rule.setLore(Arrays.asList(data[9].split(ExchangeRule.SECONDARY_SPACER)));
            }
            else {
                rule.setLore(Collections.singletonList(data[9]));
            }
        }
        // (10) Additional Meta
        String[] parts = NullCoalescing.chain(() -> data[10].split(ExchangeRule.SECONDARY_SPACER), new String[0]);
        switch (rule.getMaterial()) {
            case WRITTEN_BOOK:
                rule.setExtra(BookAdditional.fromLegacy(parts));
                break;
            case ENCHANTED_BOOK:
                rule.setExtra(EnchantStorageAdditional.fromLegacy(parts));
                break;
            case POTION:
            case SPLASH_POTION:
            case LINGERING_POTION:
                rule.setExtra(PotionAdditional.fromLegacy(parts));
                break;
            default:
                break;
        }
        // (11) NameLayer Group
        if (!Strings.isNullOrEmpty(data[11])) {
            rule.setGroup(NullCoalescing.chain(() -> GroupManager.getGroup(data[11])));
        }
        return rule;
    }

    public static Block getOtherDoubleChestBlock(final Block chest) {
        BlockState c_state = chest.getState();
        // If block is not a double chest, then do nothing
        if (!(c_state instanceof Chest)) {
            return null;
        }
        // Otherwise get the locations of both sides
        Inventory c_inventory = ((Chest) c_state).getInventory();
        if (!(c_inventory instanceof DoubleChestInventory)) {
            return null;
        }
        DoubleChestInventory dc_invectory = (DoubleChestInventory) c_inventory;
        Location dc_l_location = dc_invectory.getLeftSide().getLocation();
        Location dc_r_location = dc_invectory.getRightSide().getLocation();
        // If LeftSide has the same location as the original chest, use RightSize
        if (chest.getLocation().equals(dc_l_location)) {
            return dc_r_location.getBlock();
        }
        else {
            return dc_l_location.getBlock();
        }
    }

    public static void successfulTransactionButton(Block shopChest) {
        Material sc_material = shopChest.getType();
        if (sc_material == Material.CHEST || sc_material == Material.TRAPPED_CHEST) {
            // Get the block behind the shopChest
            BlockFace sc_facing = BlockUtility.getFacingDirection(shopChest);
            BlockFace sc_behind = sc_facing.getOppositeFace();
            // Check that host block isn't a shop compatible block
            Block sc_buttonhost = shopChest.getRelative(sc_behind);
            // Loop through each cardinal direciton
            for (BlockFace hostface : BlockAPI.ALL_SIDES) {
                // Skip if direction is where the shopchest is
                if (hostface == sc_facing) {
                    continue;
                }
                // Otherwise check if block is a button, if not then skip
                Block bb_block = sc_buttonhost.getRelative(hostface);
                Material bb_material = bb_block.getType();
                if (!(bb_material == Material.STONE_BUTTON || bb_material == Material.WOOD_BUTTON)) {
                    continue;
                }
                // Check if the button is attached to the face, otherwise skip
                BlockFace bb_facing = BlockUtility.getAttachedDirection(bb_block);
                if (!(bb_facing == hostface)) {
                    continue;
                }
                // Otherwise power the button
                BlockUtility.powerBlock(bb_block, 30);
            }
        }
    }

    @SafeVarargs
    public static <T> boolean contains(T value, T... array) {
        if (array == null || array.length < 1) {
            return false;
        }
        for (T element : array) {
            if (Objects.equals(element, value)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean conformsRequiresEnchants(Map<Enchantment, Integer> ruleEnchants,
                                                   Map<Enchantment, Integer> metaEnchants) {
        if (ruleEnchants == metaEnchants) {
            return true;
        }
        if (ruleEnchants == null || metaEnchants == null) {
            return false;
        }
        if (metaEnchants.size() < ruleEnchants.size()) {
            return false;
        }
        for (Map.Entry<Enchantment, Integer> entry : ruleEnchants.entrySet()) {
            if (!metaEnchants.containsKey(entry.getKey())) {
                return false;
            }
            if (entry.getValue() != ExchangeRule.ANY) {
                if (!Objects.equals(metaEnchants.get(entry.getKey()), entry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

}
