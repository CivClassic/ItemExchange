package com.untamedears.itemexchange.rules.additional;

import com.untamedears.itemexchange.rules.interfaces.ExchangeData;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.serialization.NBTCompound;
import vg.civcraft.mc.civmodcore.util.NullCoalescing;

public final class BookAdditional extends ExchangeData {

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void trace(ItemStack item) {
        ItemAPI.handleItemMeta(item, (BookMeta meta) -> {
            if (meta.hasTitle()) {
                setTitle(meta.getTitle());
            }
            if (meta.hasAuthor()) {
                setAuthor(meta.getAuthor());
            }
            if (meta.hasGeneration()) {
                setGeneration(meta.getGeneration());
            }
            if (meta.hasPages()) {
                setHasPages(true);
                setBookHash(bookHash(meta.getPages()));
            }
            return false;
        });
    }

    @Override
    public boolean conforms(ItemStack item) {
        boolean[] conforms = { false };
        ItemAPI.handleItemMeta(item, (BookMeta meta) -> {
            if (meta.hasTitle() != hasTitle()) {
                return false;
            }
            if (meta.hasAuthor() != hasAuthor()) {
                return false;
            }
            if (hasGeneration()) {
                if (meta.getGeneration() != getGeneration()) {
                    return false;
                }
            }
            if (meta.hasPages() != hasPages()) {
                return false;
            }
            if (meta.hasPages()) {
                if (bookHash(meta.getPages()) != getBookHash()) {
                    return false;
                }
            }
            conforms[0] = true;
            return false;
        });
        return conforms[0];
    }

    @Override
    public List<String> getDisplayedInfo() {
        return new ArrayList<String>() {{
            add(ChatColor.DARK_AQUA + "Title: " + ChatColor.WHITE + (hasTitle() ? getTitle() : ""));
            add(ChatColor.DARK_AQUA + "Author: " + ChatColor.GRAY + (hasAuthor() ? getAuthor() : ""));
            if (hasGeneration()) {
                add(ChatColor.DARK_AQUA + "Generation: " + ChatColor.GRAY + getGeneration().name());
            }
        }};
    }

    public boolean hasTitle() {
        return this.nbt.hasKey("title");
    }

    public String getTitle() {
        return this.nbt.getString("title");
    }

    public void setTitle(String title) {
        this.nbt.setString("title", title);
    }

    public boolean hasAuthor() {
        return this.nbt.hasKey("author");
    }

    public String getAuthor() {
        return this.nbt.getString("author");
    }

    public void setAuthor(String author) {
        this.nbt.setString("author", author);
    }

    public boolean hasGeneration() {
        return this.nbt.hasKey("generation");
    }

    public Generation getGeneration() {
        return Generation.valueOf(this.nbt.getString("generation"));
    }

    public void setGeneration(Generation generation) {
        this.nbt.setString("generation", NullCoalescing.chain(generation::name));
    }

    public boolean hasPages() {
        return this.nbt.getBoolean("hasPages");
    }

    public void setHasPages(boolean hasPages) {
        this.nbt.setBoolean("hasPages", hasPages);
    }

    public int getBookHash() {
        return this.nbt.getInteger("bookHash");
    }

    public void setBookHash(int bookHash) {
        this.nbt.setInteger("bookHash", bookHash);
    }

    public static NBTCompound fromItem(ItemStack item) {
        BookAdditional additional = new BookAdditional();
        additional.trace(item);
        return additional.getNBT();
    }

    public static NBTCompound fromLegacy(String[] parts) {
        BookAdditional additional = new BookAdditional();
        switch (parts.length) {
            case 3: {
                additional.setTitle(parts[0]);
                additional.setAuthor(parts[1]);
                additional.setHasPages(true);
                additional.setBookHash(NullCoalescing.chain(() -> Integer.parseInt(parts[2]), 0));
                break;
            }
            case 1: {
                additional.setTitle(null);
                additional.setAuthor(null);
                additional.setHasPages(true);
                additional.setBookHash(NullCoalescing.chain(() -> Integer.parseInt(parts[0]), 0));
                break;
            }
            default: {
                additional.setTitle(null);
                additional.setAuthor(null);
                additional.setHasPages(false);
                additional.setBookHash(0);
                break;
            }
        }
        return additional.getNBT();
    }

    private static int bookHash(List<String> pages) {
        if (pages == null) {
            return 0;
        }
        StringBuilder all = new StringBuilder();
        for (String page : pages) {
            all.append(page).append("\r");
        }
        return all.toString().hashCode();
    }

}
