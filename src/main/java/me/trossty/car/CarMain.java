package me.trossty.car;

import me.trossty.car.network.CarNetworking;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModEntities;
import me.trossty.car.registry.ModItems;
import me.trossty.car.registry.ModScreenHandlers;
import me.trossty.car.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class CarMain implements ModInitializer {

    public static final String MOD_ID = "car";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CarConfig.load();
        // Vehicles described by data packs: loaded with the world, sent to
        // every client that joins.
        net.fabricmc.fabric.api.event.registry.DynamicRegistries.registerSynced(
                me.trossty.car.vehicle.VehicleDefinition.KEY,
                me.trossty.car.vehicle.VehicleDefinition.CODEC);
        ModBlocks.register();
        ModBlockEntities.register();
        me.trossty.car.registry.ModWorldGen.register();
        ModFluids.register();
        ModComponents.register();
        ModEntities.register();
        ModItems.register();
        ModSounds.register();
        ModScreenHandlers.register();
        me.trossty.car.world.AbandonedVehicles.register();
        CarNetworking.registerPayloads();
        CarNetworking.registerServerReceivers();
    }
}
