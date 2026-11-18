package me.trossty.car.test;

import me.trossty.car.CarMain;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The server half of the two-player test ({@code mpServer}): waits for Alice
 * and Bob, sits them in one car (Alice driving, Bob beside her) in front of a
 * wall, and logs what the server sees while Alice's client drives into it.
 * Then night falls, Bob gets out to look at the lamps, and the server quits.
 * Only with {@code -Dcar.mp.server=true}; otherwise it does nothing.
 */
@Mod(value = CarMain.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class MpServerHarness {

    private static final int Y = 100;
    /** The wall stands this far down the road from where the car starts. */
    private static final int WALL_Z = 16;

    private int started = -1;
    private CarEntity car;

    public MpServerHarness() {
        if (!Boolean.getBoolean("car.mp.server")) {
            return;
        }
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> this.tick(event.getServer()));
    }

    private static void log(String line) {
        System.out.println("car mp server " + line);
    }

    private static ServerPlayer player(MinecraftServer server, String name) {
        return server.getPlayerList().getPlayerByName(name);
    }

    private void tick(MinecraftServer server) {
        ServerPlayer alice = player(server, "Alice");
        ServerPlayer bob = player(server, "Bob");
        int now = server.getTickCount();
        if (this.started < 0) {
            if (alice == null || bob == null) {
                if (now == 20 * 180) {
                    log("FAILED: nobody came");
                    server.halt(false);
                }
                return;
            }
            this.started = now;
        }
        int t = now - this.started;
        ServerLevel world = server.overworld();
        if (t == 40) {
            this.setUp(server, world, alice, bob);
        } else if (t == 60) {
            alice.startRiding(this.car, true);
            bob.startRiding(this.car, true);
            log("seated: driver " + name(this.car.getControllingPassenger()) + ", riders "
                    + this.car.getPassengers().stream().map(MpServerHarness::name).toList());
        } else if (t > 60 && t <= 260 && t % 10 == 0) {
            log("t=%d time=%d %s".formatted(t, world.getGameTime(), this.state(alice, bob)));
        } else if (t == 270) {
            world.setDayTime(18000);
            bob.stopRiding();
            // Past the end of the wall, looking back at the car's nose: the lamps
            // should be lit, and the ground on this side of the wall dark.
            bob.teleportTo(world, 6.5, Y, WALL_Z + 4.0, 138.0F, 25.0F);
            log("night, Bob out past the wall: " + this.state(alice, bob));
        } else if (t == 380) {
            log("done: " + this.state(alice, bob));
            server.halt(false);
        }
    }

    private void setUp(MinecraftServer server, ServerLevel world, ServerPlayer alice, ServerPlayer bob) {
        world.setDayTime(6000);
        for (BlockPos at : BlockPos.betweenClosed(-8, Y - 1, -8, 8, Y - 1, 40)) {
            world.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
        }
        for (BlockPos at : BlockPos.betweenClosed(-8, Y, -8, 8, Y + 6, 40)) {
            world.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos at : BlockPos.betweenClosed(-4, Y, WALL_Z, 4, Y + 2, WALL_Z)) {
            world.setBlockAndUpdate(at, Blocks.STONE_BRICKS.defaultBlockState());
        }
        for (ServerPlayer player : new ServerPlayer[] {alice, bob}) {
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.teleportTo(world, 2.5, Y, 0.5, 0.0F, 10.0F);
        }
        this.car = ModEntities.CAR.create(world);
        this.car.moveTo(0.5, Y, 0.5, 0.0F, 0.0F);
        world.addFreshEntity(this.car);
        log("set up: car " + this.car.getId() + " at 0.5 " + Y + " 0.5, wall at z " + WALL_Z);
    }

    private String state(ServerPlayer alice, ServerPlayer bob) {
        return "car %.2f %.2f yaw %.0f speed %.2f hp %.1f lights-driver %s | Alice hp %.1f riding %s | Bob hp %.1f riding %s"
                .formatted(this.car.getX(), this.car.getZ(), this.car.getYRot(), this.car.getEngineSpeed(),
                        this.car.damage().getHealth(), name(this.car.getControllingPassenger()),
                        alice.getHealth(), this.car.hasPassenger(alice), bob.getHealth(), this.car.hasPassenger(bob));
    }

    private static String name(net.minecraft.world.entity.Entity entity) {
        return entity == null ? "nobody" : entity.getName().getString();
    }
}
