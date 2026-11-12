package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.world.OilDepositFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

/** Oil deposits in the overworld; the feature's placement is in the data pack. */
public final class ModWorldGen {

    public static final Feature<DefaultFeatureConfig> OIL_DEPOSIT = Registry.register(
            Registries.FEATURE, CarMain.id("oil_deposit"), new OilDepositFeature(DefaultFeatureConfig.CODEC));

    public static final RegistryKey<PlacedFeature> OIL_DEPOSIT_PLACED =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, CarMain.id("oil_deposit"));

    private ModWorldGen() {
    }

    public static void register() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES, OIL_DEPOSIT_PLACED);
    }
}
