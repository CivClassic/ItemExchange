package com.untamedears.itemexchange.rules;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.api.NBTCompound;
import vg.civcraft.mc.civmodcore.serialization.NBTSerializable;
import vg.civcraft.mc.civmodcore.serialization.NBTSerializationException;

public abstract class ExchangeData implements NBTSerializable {

    protected final NBTCompound nbt = new NBTCompound();

    public abstract void trace(ItemStack item);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean conforms(ItemStack item);


    public abstract List<String> getDisplayedInfo();

    @Override
    public final void serialize(NBTCompound nbt) throws NBTSerializationException {
        nbt.adopt(this.nbt);
    }

    @Override
    public final void deserialize(NBTCompound nbt) throws NBTSerializationException {
        this.nbt.adopt(nbt);
    }

    public final NBTCompound getNBT() {
        return this.nbt;
    }

}
