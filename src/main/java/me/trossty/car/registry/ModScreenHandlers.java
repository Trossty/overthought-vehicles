package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.refinery.FuelRefineryScreenHandler;
import me.trossty.car.service.CarServiceScreenHandler;
import me.trossty.car.workbench.VehicleWorkbenchScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {

    public static final ScreenHandlerType<VehicleWorkbenchScreenHandler> VEHICLE_WORKBENCH =
            register("vehicle_workbench", VehicleWorkbenchScreenHandler::new);

    public static final ScreenHandlerType<FuelRefineryScreenHandler> FUEL_REFINERY =
            register("fuel_refinery", FuelRefineryScreenHandler::new);

    /**
     * The service window. Opened against a car rather than a block, which is
     * why it carries an entity id instead of a position.
     */
    public static final ScreenHandlerType<CarServiceScreenHandler> CAR_SERVICE =
            register("car_service", CarServiceScreenHandler::new);

    /** The vehicle workstation: choose a vehicle, build it from your pockets. */
    public static final ScreenHandlerType<me.trossty.car.workbench.VehicleWorkstationScreenHandler> VEHICLE_WORKSTATION =
            register("vehicle_workstation", me.trossty.car.workbench.VehicleWorkstationScreenHandler::new);

    /** The shared gauge window: generators on liquid, the hydrotreater, the pumpjack, the battery, the charger. */
    public static final ScreenHandlerType<me.trossty.car.machine.MachineScreenHandler> MACHINE =
            register("machine", me.trossty.car.machine.MachineScreenHandler::plain);

    /** The same window with a firebox slot, for the coal generator. */
    public static final ScreenHandlerType<me.trossty.car.machine.MachineScreenHandler> FUELLED_MACHINE =
            register("fuelled_machine", me.trossty.car.machine.MachineScreenHandler::fuelled);

    private ModScreenHandlers() {
    }

    private static <T extends ScreenHandler> ScreenHandlerType<T> register(
            String name, ScreenHandlerType.Factory<T> factory) {
        // The constructor is reachable thanks to Fabric's transitive access widener.
        return Registry.register(Registries.SCREEN_HANDLER, CarMain.id(name),
                new ScreenHandlerType<>(factory, FeatureFlags.VANILLA_FEATURES));
    }

    /** Forces class loading so the static fields above are registered. */
    public static void register() {
    }
}
