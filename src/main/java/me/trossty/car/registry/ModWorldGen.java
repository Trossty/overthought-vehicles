package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.world.OilDepositFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Oil deposits in the overworld. The feature's placement is in the data pack,
 * and data/overthought_vehicles/neoforge/biome_modifier adds it to every overworld biome.
 */
public final class ModWorldGen {

    public static final Feature<NoneFeatureConfiguration> OIL_DEPOSIT = Registry.register(
            BuiltInRegistries.FEATURE, CarMain.id("oil_deposit"), new OilDepositFeature(NoneFeatureConfiguration.CODEC));

    public static final ResourceKey<PlacedFeature> OIL_DEPOSIT_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, CarMain.id("oil_deposit"));

    private ModWorldGen() {
    }

    /** Forces class loading so the feature above is registered. */
    public static void register() {
    }
}
