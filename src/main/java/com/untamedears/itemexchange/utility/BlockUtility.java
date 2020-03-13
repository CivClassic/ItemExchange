package com.untamedears.itemexchange.utility;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.untamedears.itemexchange.DeprecatedMethods;
import com.untamedears.itemexchange.ItemExchangePlugin;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;

public class BlockUtility {

    public static final BlockFace[] cardinalFaces = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN, BlockFace.UP };

    // Get the direction a block is facing
    public static BlockFace getFacingDirection(Block block) {
        byte data = DeprecatedMethods.getBlockMeta(block);
        if ((data & 0x5) == 5) {
            return BlockFace.EAST;
        }
        else if ((data & 0x4) == 4) {
            return BlockFace.WEST;
        }
        else if ((data & 0x3) == 3) {
            return BlockFace.SOUTH;
        }
        else if ((data & 0x2) == 2) {
            return BlockFace.NORTH;
        }
        else if ((data & 0x1) == 1) {
            return BlockFace.UP;
        }
        else if (data == 0) {
            return BlockFace.DOWN;
        }
        else {
            return BlockFace.SELF;
        }
    }

    // Get the face an object is attached to
    // (Attached can mean more than just buttons)
    public static BlockFace getAttachedDirection(Block block) {
        byte data = DeprecatedMethods.getBlockMeta(block);
        if ((data & 0x5) == 5) {
            return BlockFace.UP;
        }
        else if ((data & 0x4) == 4) {
            return BlockFace.NORTH;
        }
        else if ((data & 0x3) == 3) {
            return BlockFace.SOUTH;
        }
        else if ((data & 0x2) == 2) {
            return BlockFace.WEST;
        }
        else if ((data & 0x1) == 1) {
            return BlockFace.EAST;
        }
        else if (data == 0) {
            return BlockFace.DOWN;
        }
        else {
            return BlockFace.SELF;
        }
    }

    public static void powerBlock(Block block, long time) {
        // Ensure that a timedelay exists
        if (time <= 0) {
            throw new IllegalArgumentException("You must enter an above zero time value!");
        }
        // Set the block to be powered
        BlockState b_onstate = block.getState();
        byte b_onmeta = DeprecatedMethods.getBlockMeta(block);
        DeprecatedMethods.setBlockMeta(b_onstate, (byte) (b_onmeta | 0x8));
        b_onstate.update();
        // And set the block to unpower in due time
        Bukkit.getScheduler().scheduleSyncDelayedTask(ItemExchangePlugin.getInstance(), () -> {
            BlockState b_offstate = block.getState();
            byte b_offmeta = DeprecatedMethods.getBlockMeta(block);
            DeprecatedMethods.setBlockMeta(b_offstate, (byte) (b_offmeta & ~0x8));
            b_offstate.update();
        }, time);
    }

}
