package me.trossty.car.registry;

import me.trossty.car.CarMain;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/**
 * Tags the mod reads. Both are data, so a pack can widen what the refinery
 * eats without touching the mod.
 */
public final class ModTags {

    /** What the refinery will cook down into biomass. */
    public static final TagKey<Item> REFINERY_ORGANIC =
            TagKey.of(RegistryKeys.ITEM, CarMain.id("refinery_organic"));

    /** What turns biomass into petrol. */
    public static final TagKey<Item> REFINERY_CATALYST =
            TagKey.of(RegistryKeys.ITEM, CarMain.id("refinery_catalyst"));

    /** What turns it into diesel instead: the heavy end of the barrel. */
    public static final TagKey<Item> REFINERY_DIESEL_CATALYST =
            TagKey.of(RegistryKeys.ITEM, CarMain.id("refinery_diesel_catalyst"));

    /**
     * Fluids the refinery takes as feedstock without cooking anything first.
     * This mod has no oil of its own: the tag lists other mods' crude, every
     * entry optional, and a data pack can add to it.
     */
    public static final TagKey<net.minecraft.fluid.Fluid> CRUDE_OIL =
            TagKey.of(RegistryKeys.FLUID, CarMain.id("crude_oil"));

    private ModTags() {
    }
}
