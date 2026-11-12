package me.trossty.car.network;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.workbench.BenchWork;
import me.trossty.car.workbench.ModVehicleRecipes;
import me.trossty.car.workbench.VehicleRecipe;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class CarNetworking {

    private CarNetworking() {
    }

    /** Payload types must be known to both sides, so this runs in the common initializer. */
    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(HornC2SPayload.ID, HornC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CrashC2SPayload.ID, CrashC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(
                ServicePartC2SPayload.ID, ServicePartC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(
                BenchBuildC2SPayload.ID, BenchBuildC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(
                RoadMarkingC2SPayload.ID, RoadMarkingC2SPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(HornC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                // Only the driver may sound the horn, and only of the car they are in.
                if (player.getVehicle() instanceof CarEntity car
                        && car.getControllingPassenger() == player) {
                    car.honk();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ServicePartC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            int car = payload.car();
            String part = payload.part();
            context.server().execute(() -> serviceByHand(player, car, part));
        });

        ServerPlayNetworking.registerGlobalReceiver(BenchBuildC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> buildAtBench(
                    player, payload.bench(), payload.recipe(), payload.quality()));
        });

        ServerPlayNetworking.registerGlobalReceiver(RoadMarkingC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> me.trossty.car.item.RoadPaintItem.choose(player,
                    payload.offHand() ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND,
                    payload.marking()));
        });

        ServerPlayNetworking.registerGlobalReceiver(CrashC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            float severity = payload.severity();
            context.server().execute(() -> {
                // Only the driver can crash the car they are driving.
                if (player.getVehicle() instanceof CarEntity car
                        && car.getControllingPassenger() == player
                        && car.level() instanceof ServerLevel world) {
                    car.applyReportedCrash(world, severity);
                }
            });
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
        car.serviceByHand(player, part);
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
        void report(float severity);
    }

    private static CrashReporter crashReporter = severity -> {
    };

    public static void setCrashReporter(CrashReporter reporter) {
        crashReporter = reporter;
    }

    public static void reportCrash(float severity) {
        crashReporter.report(severity);
    }
}
