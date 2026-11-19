package me.trossty.car;

import me.trossty.car.network.CarNetworking;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModEntities;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModItems;
import me.trossty.car.registry.ModScreenHandlers;
import me.trossty.car.registry.ModWorldGen;
import me.trossty.car.sound.ModSounds;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(CarMain.MOD_ID)
public class CarMain {

    public static final String MOD_ID = "car";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public CarMain(IEventBus modBus) {
        CarConfig.load();
        // The registries are open to new entries only while RegisterEvent runs,
        // so that is when the static fields of the Mod* classes get made. The
        // block event comes first; loading everything there keeps the fields in
        // the order they refer to one another.
        modBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                ModBlocks.register();
                ModFluids.register();
                ModBlockEntities.register();
                ModWorldGen.register();
                ModComponents.register();
                ModEntities.register();
                ModItems.register();
                ModSounds.register();
                ModScreenHandlers.register();
            }
        });
        // Vehicles described by data packs: loaded with the world, sent to
        // every client that joins.
        modBus.addListener((DataPackRegistryEvent.NewRegistry event) ->
                event.dataPackRegistry(VehicleDefinition.KEY, VehicleDefinition.CODEC, VehicleDefinition.CODEC));
        modBus.addListener(ModFluids::registerCapabilities);
        modBus.addListener(CarNetworking::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                me.trossty.car.entity.TrailerEntity::keepSleeping);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(me.trossty.car.entity.TrailerEntity::wakeUp);
    }
}
