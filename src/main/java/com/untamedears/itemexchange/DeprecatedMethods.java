package com.untamedears.itemexchange;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

@SuppressWarnings("deprecation")
public class DeprecatedMethods {

    public static byte getBlockMeta(Block block) {
        return block.getData();
    }

    public static void setBlockMeta(BlockState blockState, byte meta) {
        blockState.setRawData(meta);
    }

}
