package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.refinery.FuelRefineryScreenHandler;
import me.trossty.car.service.CarServiceScreenHandler;
import me.trossty.car.workbench.VehicleWorkbenchScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {

    public static final MenuType<VehicleWorkbenchScreenHandler> VEHICLE_WORKBENCH =
            register("vehicle_workbench", VehicleWorkbenchScreenHandler::new);

    public static final MenuType<FuelRefineryScreenHandler> FUEL_REFINERY =
            register("fuel_refinery", FuelRefineryScreenHandler::new);

    /**
     * The service window. Opened against a car rather than a block, which is
     * why it carries an entity id instead of a position.
     */
    public static final MenuType<CarServiceScreenHandler> CAR_SERVICE =
            register("car_service", CarServiceScreenHandler::new);

    /** The vehicle workstation: choose a vehicle, build it from your pockets. */
    public static final MenuType<me.trossty.car.workbench.VehicleWorkstationScreenHandler> VEHICLE_WORKSTATION =
            register("vehicle_workstation", me.trossty.car.workbench.VehicleWorkstationScreenHandler::new);

    /** The shared gauge window: generators on liquid, the hydrotreater, the pumpjack, the battery, the charger. */
    public static final MenuType<me.trossty.car.machine.MachineScreenHandler> MACHINE =
            register("machine", me.trossty.car.machine.MachineScreenHandler::plain);

    /** The same window with a firebox slot, for the coal generator. */
    public static final MenuType<me.trossty.car.machine.MachineScreenHandler> FUELLED_MACHINE =
            register("fuelled_machine", me.trossty.car.machine.MachineScreenHandler::fuelled);

    private ModScreenHandlers() {
    }

    private static <T extends AbstractContainerMenu> MenuType<T> register(
            String name, MenuType.MenuSupplier<T> factory) {
        // The constructor is reachable thanks to Fabric's transitive access widener.
        return Registry.register(BuiltInRegistries.MENU, CarMain.id(name),
                new MenuType<>(factory, FeatureFlags.VANILLA_SET));
    }

    /** Forces class loading so the static fields above are registered. */
    public static void register() {
    }
}
