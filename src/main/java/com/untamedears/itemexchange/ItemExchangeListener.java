package com.untamedears.itemexchange;

import com.untamedears.itemexchange.events.IETransactionEvent;
import com.untamedears.itemexchange.rules.BulkExchangeRule;
import com.untamedears.itemexchange.rules.ExchangeRule;
import com.untamedears.itemexchange.rules.ShopRule;
import com.untamedears.itemexchange.rules.TradeRule;
import com.untamedears.itemexchange.utility.Utilities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.api.EntityAPI;
import vg.civcraft.mc.civmodcore.api.InventoryAPI;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.util.NullCoalescing;
import vg.civcraft.mc.namelayer.group.Group;

public class ItemExchangeListener implements Listener {

    private static final long TIME_BETWEEN_CLICKS = 200L;

    private final Map<Player, Long> playerInteractionCooldowns = new Hashtable<>();
    private final Map<Player, Inventory> shopRecord = new HashMap<>();
    private final Map<Player, Integer> ruleIndex = new HashMap<>();

    /**
     * Responds when a player interacts with a shop
     */
    @EventHandler
    public void playerInteractionEvent(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // This was here before because apparently it's a fix to
        // a double event triggering issue. Huh.
        if (player == null) {
            return;
        }
        // Interaction must be a block punch
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        // Block must be a supported block type
        if (!ItemExchangePlugin.SHOP_BLOCKS.contains(event.getClickedBlock().getType())) {
            return;
        }
        // Limit player interactions to once per 200ms
        long now = System.currentTimeMillis();
        long pre = this.playerInteractionCooldowns.getOrDefault(player, 0L);
        if (now - pre < TIME_BETWEEN_CLICKS) {
            return;
        }
        this.playerInteractionCooldowns.put(player, now);
        // Attempt to create parse a shop from the inventory
        Inventory inventory = NullCoalescing.chain(() ->
                ((InventoryHolder) event.getClickedBlock().getState()).getInventory());
        ShopRule shop = ShopRule.getShopFromInventory(inventory);
        if (shop == null) {
            return;
        }
        // Check if the player has interacted with this specific shop before
        // If not then switch over to this shop and display its catalogue
        boolean justBrowsing = false;
        boolean shouldCycle = true;
        if (!this.shopRecord.containsKey(player) ||
                !inventory.equals(this.shopRecord.get(player)) ||
                !this.ruleIndex.containsKey(player)) {
            this.shopRecord.put(player, inventory);
            this.ruleIndex.put(player, 0);
            justBrowsing = true;
            shouldCycle = false;
        }
        // If the player is holding nothing, just browse
        if (!ItemAPI.isValidItem(event.getItem())) {
            justBrowsing = true;
        }
        // Attempt to get the trade from the shop
        shop.setCurrentTradeIndex(this.ruleIndex.getOrDefault(player, 0));
        TradeRule trade = shop.getCurrentTrade();
        if (trade == null || !trade.isValid()) {
            this.ruleIndex.remove(player);
            return;
        }
        ExchangeRule inputRule = trade.getInput();
        ExchangeRule outputRule = trade.getOutput();
        // Check if the input is limited to a group, and if so whether the viewer
        // has permission to purchase from that group.
        Group group = inputRule.getGroup();
        if (group != null) {
            if (!ItemExchangePlugin.PURCHASE_PERMISSION.hasAccess(group, player)) {
                justBrowsing = true;
            }
        }
        // If the player's hand is empty or holding the wrong item, just scroll
        // through the catalogue.
        if (justBrowsing || !inputRule.conforms(event.getItem())) {
            if (shouldCycle) {
                trade = shop.cycleTrades(!player.isSneaking());
                if (trade == null) {
                    this.ruleIndex.remove(player);
                    return;
                }
                this.ruleIndex.put(player, shop.getCurrentTradeIndex());
            }
            shop.presentShopToPlayer(player);
            return;
        }
        // Check that the buyer has enough of the inputs
        ItemStack[] inputItems = inputRule.getStock(player.getInventory());
        if (inputItems.length < 1) {
            player.sendMessage(ChatColor.RED + "You don't have enough of the input.");
            return;
        }
        // Check that the shop has enough of the outputs if needed
        ItemStack[] outputItems = new ItemStack[0];
        if (trade.hasOutput()) {
            outputItems = outputRule.getStock(inventory);
            if (outputItems.length < 1) {
                player.sendMessage(ChatColor.RED + "Shop does not have enough in stock.");
                return;
            }
        }
        // Attempt to transfer the items between the shop and the buyer
        boolean successfulTransfer;
        if (trade.hasOutput()) {
            successfulTransfer = InventoryAPI.safelyTradeBetweenInventories(
                    player.getInventory(),
                    inputItems,
                    inventory,
                    outputItems);
        }
        else {
            successfulTransfer = InventoryAPI.safelyTransactBetweenInventories(
                    player.getInventory(),
                    inputItems,
                    inventory);
        }
        if (!successfulTransfer) {
            player.sendMessage(ChatColor.RED + "Could not complete that transaction!");
            return;
        }
        // Power buttons button directly behind *this* chest
        Block shopChest = event.getClickedBlock();
        Utilities.successfulTransactionButton(event.getClickedBlock());
        Block otherChestBlock = Utilities.getOtherDoubleChestBlock(shopChest);
        if (otherChestBlock != null) {
            Utilities.successfulTransactionButton(otherChestBlock);
        }
        trade.lock();
        Bukkit.getServer().getPluginManager().callEvent(new IETransactionEvent(player, inventory, trade,
                inputItems, outputItems));
        if (trade.hasOutput()) {
            player.sendMessage(ChatColor.GREEN + "Successful exchange!");
        }
        else {
            player.sendMessage(ChatColor.GREEN + "Successful donation!");
        }
    }

    /**
     * Allow players to craft bulk rule items
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the clicker is a player
        if (!EntityAPI.isPlayer(event.getWhoClicked())) {
            return;
        }
        // Check if the inventory is a crafting matrix
        CraftingInventory inventory = InventoryAPI.getCraftingInventory(event.getView().getTopInventory());
        if (inventory == null) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(ItemExchangePlugin.getInstance(), () -> {
            List<ExchangeRule> rules = new ArrayList<>();
            for (ItemStack item : inventory.getMatrix()) {
                if (!ItemAPI.isValidItem(item)) {
                    continue;
                }
                ExchangeRule rule = ExchangeRule.fromItem(item);
                if (rule != null) {
                    rules.add(rule);
                }
                else {
                    BulkExchangeRule bulk = BulkExchangeRule.fromItem(item);
                    if (bulk != null) {
                        rules.addAll(bulk.getRules());
                    }
                }
            }
            if (rules.isEmpty()) {
                inventory.setResult(null);
            }
            else {
                BulkExchangeRule bulk = new BulkExchangeRule();
                bulk.setRules(rules);
                inventory.setResult(bulk.toItem());
            }
            for (Player viewer : InventoryAPI.getViewingPlayers(inventory)) {
                viewer.updateInventory();
            }
        });
    }

    /**
     * If the player drops a bulk rule item, then split it into its constituent rules.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item drop = event.getItemDrop();
        BulkExchangeRule bulk = BulkExchangeRule.fromItem(drop.getItemStack());
        if (bulk != null) {
            for (ExchangeRule rule : bulk.getRules()) {
                Item dropped = drop.getWorld().dropItem(drop.getLocation(), rule.toItem());
                dropped.setVelocity(drop.getVelocity());
            }
            drop.remove();
        }
    }

    /**
     * Prevent rule items from being moved
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != ItemExchangePlugin.RULE_ITEM.getType()) {
            return;
        }
        if (Utilities.isExchangeRule(item)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent rule items from being picked up by hoppers
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (event.getInventory().getType() != InventoryType.HOPPER) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() != ItemExchangePlugin.RULE_ITEM.getType()) {
            return;
        }
        if (Utilities.isExchangeRule(item)) {
            event.setCancelled(true);
        }
    }

}
