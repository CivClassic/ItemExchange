package com.untamedears.itemexchange.rules.additional;

import com.google.common.base.Strings;
import com.untamedears.itemexchange.rules.ExchangeData;
import com.untamedears.itemexchange.rules.ExchangeRule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.api.NBTCompound;
import vg.civcraft.mc.civmodcore.util.NullCoalescing;

public final class PotionAdditional extends ExchangeData {

    @Override
    public void trace(ItemStack item) {
        ItemAPI.handleItemMeta(item, (PotionMeta meta) -> {
            setPotionData(meta.getBasePotionData());
            setEffects(meta.getCustomEffects());
            if (meta.hasDisplayName()) {
                setName(meta.getDisplayName());
            }
            else {
                switch (meta.getBasePotionData().getType()) {
                    default:
                    case UNCRAFTABLE:
                        setName("Uncraftable Potion");
                        break;
                    case WATER:
                        setName("Water Bottle");
                        break;
                    case MUNDANE:
                        setName("Mundane Potion");
                        break;
                    case THICK:
                        setName("Thick Potion");
                        break;
                    case AWKWARD:
                        setName("Awkward Potion");
                        break;
                    case NIGHT_VISION:
                        setName("Potion of Night Vision");
                        break;
                    case INVISIBILITY:
                        setName("Potion of Invisibility");
                        break;
                    case JUMP:
                        setName("Potion of Leaping");
                        break;
                    case FIRE_RESISTANCE:
                        setName("Potion of Fire Resistance");
                        break;
                    case SPEED:
                        setName("Potion of Swiftness");
                        break;
                    case SLOWNESS:
                        setName("Potion of Slowness");
                        break;
                    case WATER_BREATHING:
                        setName("Potion of Water Breathing");
                        break;
                    case INSTANT_HEAL:
                        setName("Potion of Healing");
                        break;
                    case INSTANT_DAMAGE:
                        setName("Potion of Harming");
                        break;
                    case POISON:
                        setName("Potion of Poison");
                        break;
                    case REGEN:
                        setName("Potion of Regeneration");
                        break;
                    case STRENGTH:
                        setName("Potion of Strength");
                        break;
                    case WEAKNESS:
                        setName("Potion of Weakness");
                        break;
                    case LUCK:
                        setName("Potion of Luck");
                        break;
                }
            }
            return false;
        });
    }

    @Override
    public boolean conforms(ItemStack item) {
        boolean[] conforms = { false };
        ItemAPI.handleItemMeta(item, (PotionMeta meta) -> {
            if (!Objects.equals(getPotionData(), meta.getBasePotionData())) {
                return false;
            }
            List<PotionEffect> heldEffects = getEffects();
            List<PotionEffect> metaEffects = meta.getCustomEffects();
            if (metaEffects.size() != heldEffects.size()) {
                return false;
            }
            if (!metaEffects.containsAll(heldEffects)) {
                return false;
            }
            conforms[0] = true;
            return false;
        });
        return conforms[0];
    }

    @Override
    public List<String> getDisplayedInfo() {
        return Collections.singletonList(ChatColor.AQUA + "Potion Name: " + ChatColor.WHITE + getName());
    }

    public String getName() {
        String name = this.nbt.getString("name");
        if (Strings.isNullOrEmpty(name)) {
            return "Uncraftable Potion";
        }
        return name;
    }

    public void setName(String name) {
        this.nbt.setString("name", name);
    }

    public PotionData getPotionData() {
        NBTCompound nbt = this.nbt.getCompound("base");
        return new PotionData(NullCoalescing.chain(() -> PotionType.valueOf(nbt.getString("type")), PotionType.UNCRAFTABLE), nbt.getBoolean("extended"), nbt.getBoolean("upgraded"));
    }

    public void setPotionData(PotionData data) {
        this.nbt.setCompound("base", new NBTCompound() {{
            setString("type", NullCoalescing.chain(() -> data.getType().name(), PotionType.UNCRAFTABLE.name()));
            setBoolean("extended", NullCoalescing.chain(data::isExtended, false));
            setBoolean("extended", NullCoalescing.chain(data::isUpgraded, false));
        }});
    }

    public List<PotionEffect> getEffects() {
        return Arrays.stream(this.nbt.getCompoundArray("effects")).
                map((nbt) -> new PotionEffect(NullCoalescing.chain(() -> PotionEffectType.getByName(nbt.getString("type"))), nbt.getInteger("duration"), nbt.getInteger("amplifier"), nbt.getBoolean("ambient"), nbt.getBoolean("particles"))).
                collect(Collectors.toCollection(ArrayList::new));
    }

    public void setEffects(List<PotionEffect> effects) {
        if (effects == null || effects.isEmpty()) {
            this.nbt.remove("effects");
            return;
        }
        this.nbt.setCompoundArray("effects", effects.stream().map((effect) -> new NBTCompound() {{
            setString("type", NullCoalescing.chain(() -> effect.getType().getName()));
            setInteger("duration", effect.getDuration());
            setInteger("amplifier", effect.getAmplifier());
            setBoolean("ambient", effect.isAmbient());
            setBoolean("particles", effect.hasParticles());
        }}).toArray(NBTCompound[]::new));
    }

    public static NBTCompound fromItem(ItemStack item) {
        PotionAdditional additional = new PotionAdditional();
        additional.trace(item);
        return additional.getNBT();
    }

    @SuppressWarnings("deprecation")
    public static NBTCompound fromLegacy(String[] parts) {
        PotionAdditional data = new PotionAdditional();
        List<PotionEffect> effects = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            // Parse the potion name
            if (i == 0) {
                data.setName(parts[i]);
            }
            // Parse the base effect
            else if (i == 1) {
                try {
                    String[] effectData = parts[i].split(ExchangeRule.TERTIARY_SPACER);
                    int effectId = Integer.parseInt(effectData[0]);
                    boolean isExtended = Integer.parseInt(effectData[1]) == 1;
                    boolean isUpgraded = Integer.parseInt(effectData[2]) == 1;
                    switch (effectId) {
                        case 0:
                            data.setPotionData(new PotionData(PotionType.UNCRAFTABLE, isExtended, isUpgraded));
                            break;
                        case -1:
                            data.setPotionData(new PotionData(PotionType.WATER, isExtended, isUpgraded));
                            break;
                        case -2:
                            data.setPotionData(new PotionData(PotionType.MUNDANE, isExtended, isUpgraded));
                            break;
                        case -3:
                            data.setPotionData(new PotionData(PotionType.THICK, isExtended, isUpgraded));
                            break;
                        case -4:
                            data.setPotionData(new PotionData(PotionType.AWKWARD, isExtended, isUpgraded));
                            break;
                        default:
                            PotionEffectType effectType = PotionEffectType.getById(effectId);
                            PotionType potionType = PotionType.getByEffect(effectType);
                            data.setPotionData(new PotionData(potionType, isExtended, isUpgraded));
                            break;
                    }
                }
                catch (Exception error) {
                    data.setPotionData(new PotionData(PotionType.WATER, false, false));
                    Bukkit.getLogger().log(Level.WARNING, "An error occurred parsing ItemExchange potion metadata: base effect. " + "Defaulting to a water bottle.", error);
                }
            }
            // Parse any other effect
            else {
                try {
                    String[] effectData = parts[i].split(ExchangeRule.TERTIARY_SPACER);
                    int effectId = Integer.parseInt(effectData[0]);
                    int amplifier = Integer.parseInt(effectData[1]);
                    int duration = Integer.parseInt(effectData[2]);
                    boolean ambient = Integer.parseInt(effectData[3]) == 1;
                    PotionEffectType type = PotionEffectType.getById(effectId);
                    effects.add(new PotionEffect(type, duration, amplifier, ambient));
                }
                catch (Exception error) {
                    Bukkit.getLogger().log(Level.WARNING, "An error occurred parsing ItemExchange potion metadata: " + "custom effect. Skipping.", error);
                }
            }
        }
        data.setEffects(effects);
        return data.getNBT();
    }

}
