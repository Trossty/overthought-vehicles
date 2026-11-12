package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.world.OilDepositFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Oil deposits in the overworld; the feature's placement is in the data pack. */
public final class ModWorldGen {

    public static final Feature<NoneFeatureConfiguration> OIL_DEPOSIT = Registry.register(
            BuiltInRegistries.FEATURE, CarMain.id("oil_deposit"), new OilDepositFeature(NoneFeatureConfiguration.CODEC));

    public static final ResourceKey<PlacedFeature> OIL_DEPOSIT_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, CarMain.id("oil_deposit"));

    private ModWorldGen() {
    }

    public static void register() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, OIL_DEPOSIT_PLACED);
    }
}
