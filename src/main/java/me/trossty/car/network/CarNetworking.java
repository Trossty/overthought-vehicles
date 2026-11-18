package me.trossty.car.network;

import me.trossty.car.entity.BodyZone;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.workbench.BenchWork;
import me.trossty.car.workbench.ModVehicleRecipes;
import me.trossty.car.workbench.VehicleRecipe;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class CarNetworking {

    private CarNetworking() {
    }

    /**
     * Every packet the client sends. The handlers run on the server thread
     * (NeoForge's default), so they act on the world directly.
     */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(HornC2SPayload.ID, HornC2SPayload.CODEC, (payload, context) -> {
            // Only the driver may sound the horn, and only of the car they are in.
            if (context.player().getVehicle() instanceof CarEntity car
                    && car.getControllingPassenger() == context.player()) {
                car.honk();
            }
        });

        registrar.playToServer(BrakeC2SPayload.ID, BrakeC2SPayload.CODEC, (payload, context) -> {
            if (context.player().getVehicle() instanceof CarEntity car
                    && car.getControllingPassenger() == context.player()) {
                car.setBrakeLamps(payload.pressed());
            }
        });

        registrar.playToServer(BankC2SPayload.ID, BankC2SPayload.CODEC, (payload, context) -> {
            if (context.player().getVehicle() instanceof me.trossty.car.entity.AirplaneEntity plane
                    && plane.getControllingPassenger() == context.player()) {
                plane.setReportedBank(payload.bank());
            }
        });

        registrar.playToServer(ServicePartC2SPayload.ID, ServicePartC2SPayload.CODEC, (payload, context) ->
                serviceByHand(context.player(), payload.car(), payload.part()));

        registrar.playToServer(BenchBuildC2SPayload.ID, BenchBuildC2SPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                buildAtBench(player, payload.bench(), payload.recipe(), payload.quality());
            }
        });

        registrar.playToServer(RoadMarkingC2SPayload.ID, RoadMarkingC2SPayload.CODEC, (payload, context) ->
                me.trossty.car.item.RoadPaintItem.choose(context.player(),
                        payload.offHand() ? net.minecraft.world.InteractionHand.OFF_HAND
                                : net.minecraft.world.InteractionHand.MAIN_HAND,
                        payload.marking()));

        registrar.playToServer(CrashC2SPayload.ID, CrashC2SPayload.CODEC, (payload, context) -> {
            // Only the driver can crash the car they are driving.
            if (context.player().getVehicle() instanceof CarEntity car
                    && car.getControllingPassenger() == context.player()
                    && car.level() instanceof ServerLevel world) {
                car.damage().applyReportedCrash(world, payload.severity(), payload.side());
            }
        });
    }

    /**
     * How far from a car a player may be and still work on it by hand. The same
     * eight blocks the service window allows, squared.
     */
    private static final double SERVICE_RANGE_SQUARED = 64.0;

    /**
     * Acts on a hands-on service click. Separated from the receiver so a test
     * can run exactly what the packet runs, without a network, which is also
     * why it takes any player rather than only a connected one.
     */
    public static void serviceByHand(Player player, int carId, String partKey) {
        CarPart part = CarPart.byKey(partKey);
        if (part == null) {
            return;
        }
        if (!(player.level().getEntity(carId) instanceof CarEntity car)
                || car.isRemoved()
                || car.distanceToSqr(player) > SERVICE_RANGE_SQUARED) {
            return;
        }
        car.workshop().serviceByHand(player, part);
    }

    /**
     * The catalogue a block builds from, or null if it is not a bench at all.
     * The same question the client asks to decide what to draw on a block, so
     * the two cannot disagree about what a ramp makes.
     */
    public static List<VehicleRecipe> catalogueOf(BlockState state) {
        if (state.is(ModBlocks.VEHICLE_WORKBENCH)) {
            return ModVehicleRecipes.WORKBENCH;
        }
        return null;
    }

    /**
     * Acts on a press of the build button on a bench top. Separated from the
     * receiver for the same reason as {@link #serviceByHand}: a test can run
     * exactly what the packet runs.
     *
     * @return whether anything was built
     */
    public static boolean buildAtBench(Player player, BlockPos bench, int recipe,
                                       int quality) {
        if (player.distanceToSqr(Vec3.atCenterOf(bench)) > SERVICE_RANGE_SQUARED) {
            return false;
        }
        List<VehicleRecipe> catalogue = catalogueOf(player.level().getBlockState(bench));
        if (catalogue == null) {
            return false;
        }
        return BenchWork.buildFromPockets(player, bench, catalogue, recipe,
                CarPart.Quality.byOrdinal(quality));
    }

    /**
     * How a driving client tells the server it hit something. The common code
     * that spots the collision cannot reach the client networking classes, so
     * the client initializer fills this in; on a dedicated server, where those
     * classes do not exist, it stays a no-op and the server detects its own
     * collisions directly.
     */
    public interface CrashReporter {
        void report(float severity, @Nullable BodyZone zone);
    }

    private static CrashReporter crashReporter = (severity, zone) -> {
    };

    public static void setCrashReporter(CrashReporter reporter) {
        crashReporter = reporter;
    }

    public static void reportCrash(float severity, @Nullable BodyZone zone) {
        crashReporter.report(severity, zone);
    }
}
