package me.trossty.car.test.client;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.test.client.PhotoHarness.ClientGameTestContext;
import me.trossty.car.test.client.PhotoHarness.TestServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A survival player's afternoon with a car, played with the keys: put it
 * down, get in, drive it into a wall, into a pond and off a ledge, run it
 * dry, knock a cow over and punch the car. Everything that happens is
 * logged as "car playtest ..." so a run can be read back as a report.
 */
final class Playtest {

    private static final int Y = 150;

    private Playtest() {
    }

    static void run(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("kill @e[type=!player]");
        server.runCommand("time set noon");
        server.runCommand("fill -40 %d -20 40 %d 120 minecraft:air".formatted(Y, Y + 20));
        server.runCommand("fill -40 %d -20 40 %d 120 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("clear @a");
        server.runCommand("gamemode survival @a");
        server.runCommand("effect give @a minecraft:resistance 1 0");
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

        place(context, server);
        crash(context, server);
        hill(context, server);
        pond(context, server);
        ledge(context, server);
        runDry(context, server);
        worn(context, server);
        cow(context, server);
        punch(context, server);
        night(context, server);

        server.runCommand("gamemode creative @a");
        server.runCommand("kill @e[type=!player]");
    }

    private static void log(String line) {
        System.out.println("car playtest " + line);
    }

    private static CarEntity serverCar(net.minecraft.server.MinecraftServer minecraft) {
        List<CarEntity> cars = minecraft.overworld().getEntitiesOfClass(CarEntity.class,
                new AABB(-60, Y - 30, -40, 60, Y + 30, 200), e -> true);
        return cars.isEmpty() ? null : cars.get(0);
    }

    private static String aim(Minecraft client) {
        var hit = client.hitResult;
        return hit == null ? "nothing" : hit.getType() + (hit instanceof net.minecraft.world.phys.EntityHitResult e
                ? " " + e.getEntity().getType() : "") + " from " + "%.2f".formatted(client.player.getEyePosition()
                .distanceTo(hit.getLocation())) + " pitch " + client.player.getXRot() + " yaw " + client.player.getYRot();
    }

    private static String state(TestServerContext server) {
        String[] out = new String[1];
        server.runOnServer(minecraft -> {
            CarEntity car = serverCar(minecraft);
            var player = minecraft.getPlayerList().getPlayers().get(0);
            out[0] = car == null ? "no car, player hp %.1f".formatted(player.getHealth())
                    : "car at %.1f %.1f %.1f hp %.1f/%.0f%% fuel %.1f water %s riding %s player hp %.1f air %d"
                    .formatted(car.getX(), car.getY() - Y, car.getZ(), car.getHealth(),
                            car.getHealthFraction() * 100, car.getFuel(), car.isInWater(),
                            car.hasPassenger(player), player.getHealth(), player.getAirSupply());
        });
        return out[0];
    }

    /** Puts a car in the player's hand and has them set it down and climb in, all by key. */
    private static void place(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("give @a car:car");
        server.runCommand("tp @a 0.5 %d 0.5 0 60".formatted(Y));
        context.waitTicks(10);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(10);
        log("place: " + state(server) + ", items left "
                + context.computeOnClient(client -> client.player.getMainHandItem().getCount()));
        // Look at it and get in.
        server.runCommand("tp @a 0.5 %d -3.5 0 25".formatted(Y));
        // Standing behind the boot, as a player would walk up to it.
        context.waitTicks(10);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(10);
        log("enter from behind: " + state(server) + ", looking at " + context.computeOnClient(Playtest::aim));
        if (!context.computeOnClient(client -> client.player.getVehicle() instanceof CarEntity)) {
            // Then from beside the driver's door.
            server.runCommand("tp @a -2.0 %d 1.5 -90 30".formatted(Y));
            context.waitTicks(10);
            context.getInput().pressKey(options -> options.keyUse);
            context.waitTicks(10);
        }
        log("enter: " + state(server) + ", looking at " + context.computeOnClient(Playtest::aim) + ", cannot run "
                + context.computeOnClient(client -> client.player.getVehicle() instanceof CarEntity c
                && !c.canRun()));
        context.takeScreenshot("play_inside");
    }

    private static void drive(ClientGameTestContext context, TestServerContext server, String what, int ticks) {
        context.getInput().holdKey(options -> options.keyUp);
        StringBuilder trace = new StringBuilder(what + " trace:");
        for (int t = 0; t < ticks; t++) {
            context.waitTicks(1);
            if (t % 5 == 4) {
                trace.append(context.<String>computeOnClient(client -> client.player.getVehicle() instanceof CarEntity car
                        ? " %.1f/%.2f/%.2f".formatted(car.getZ(), car.getY() - Y, car.getEngineSpeed()) : " out"));
            }
        }
        context.getInput().releaseKey(options -> options.keyUp);
        log(trace.toString());
    }

    /** Moves the car (and whoever is in it) to the start of the next run, facing south. */
    private static void startAt(ClientGameTestContext context, TestServerContext server, double x, double y, double z) {
        server.runOnServer(minecraft -> {
            CarEntity car = serverCar(minecraft);
            var player = minecraft.getPlayerList().getPlayers().get(0);
            if (car == null) {
                // The last run wrecked it: a fresh one, as a player would build.
                log("no car left, summoning a new one");
                car = me.trossty.car.registry.ModEntities.CAR.create(minecraft.overworld());
                car.moveTo(x, y, z, 0.0F, 0.0F);
                minecraft.overworld().addFreshEntity(car);
            }
            car.setHealth(CarEntity.MAX_HEALTH);
            player.stopRiding();
        });
        // Out first: while somebody drives, the driver's client owns the car's position.
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            CarEntity car = serverCar(minecraft);
            var player = minecraft.getPlayerList().getPlayers().get(0);
            car.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            car.teleportTo(x, y, z);
            car.setYRot(0.0F);
            player.teleportTo(x, y, z);
        });
        context.waitTicks(5);
        server.runOnServer(minecraft -> minecraft.getPlayerList().getPlayers().get(0)
                .startRiding(serverCar(minecraft), true));
        context.waitTicks(10);
        context.runOnClient(client -> {
            client.player.setYRot(0.0F);
            client.player.setXRot(10.0F);
        });
    }

    private static void crash(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("fill -4 %d 40 4 %d 40 minecraft:stone".formatted(Y, Y + 2));
        startAt(context, server, 0.5, Y, 5.5);
        log("crash before: " + state(server));
        drive(context, server, "crash", 90);
        log("crash after: " + state(server));
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.waitTicks(5);
        context.takeScreenshot("play_crash");
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
    }

    /** A hillside a block per block, then a kerb: the nose check must not stop either. */
    private static void hill(ClientGameTestContext context, TestServerContext server) {
        for (int i = 0; i < 6; i++) {
            server.runCommand("fill 7 %d %d 14 %d 100 minecraft:grass_block".formatted(Y, 66 + i, Y + i));
        }
        server.runCommand("fill -3 %d 66 3 %d 66 minecraft:stone_slab".formatted(Y, Y));
        server.runCommand("fill -3 %d 72 3 %d 100 minecraft:stone".formatted(Y, Y));
        startAt(context, server, 10.5, Y, 58.5);
        drive(context, server, "hill", 70);
        log("hill after: " + state(server));
        startAt(context, server, 0.5, Y, 58.5);
        drive(context, server, "kerb", 60);
        log("kerb after: " + state(server));
    }

    private static void pond(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("fill 15 %d 29 25 %d 51 minecraft:dirt".formatted(Y - 5, Y - 1));
        server.runCommand("fill 16 %d 30 24 %d 50 minecraft:water".formatted(Y - 3, Y - 1));
        // A ford a block deep on the way in: wade it, slowly, engine running.
        server.runCommand("fill 15 %d 17 25 %d 28 minecraft:dirt".formatted(Y - 2, Y - 2));
        server.runCommand("fill 16 %d 18 24 %d 28 minecraft:water".formatted(Y - 1, Y - 1));
        startAt(context, server, 20.5, Y, 10.5);
        log("pond before: " + state(server));
        drive(context, server, "pond", 80);
        log("pond in: " + state(server));
        for (int i = 0; i < 5; i++) {
            context.waitTicks(20);
            log("pond +" + (i + 1) + "s: " + state(server));
        }
        log("pond after 5s: " + state(server));
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.waitTicks(5);
        context.takeScreenshot("play_pond");
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        // Can it drive back out?
        context.getInput().holdKey(options -> options.keyDown);
        context.waitTicks(80);
        context.getInput().releaseKey(options -> options.keyDown);
        log("pond reversing out: " + state(server));
        // Drowned: the next run gets a dry car.
        server.runOnServer(minecraft -> serverCar(minecraft).discard());
    }

    private static void ledge(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("fill -24 %d 0 -16 %d 30 minecraft:stone".formatted(Y, Y + 7));
        startAt(context, server, -19.5, Y + 8, 5.5);
        server.runCommand("effect clear @a");
        log("ledge before: " + state(server));
        drive(context, server, "ledge", 70);
        context.waitTicks(20);
        log("ledge after: " + state(server));
    }

    private static void runDry(ClientGameTestContext context, TestServerContext server) {
        startAt(context, server, 32.5, Y, 0.5);
        server.runOnServer(minecraft -> serverCar(minecraft).setFuel(0.3F));
        log("dry before: " + state(server));
        drive(context, server, "dry", 120);
        log("dry after: " + state(server));
        context.takeScreenshot("play_dry");
        server.runOnServer(minecraft -> serverCar(minecraft).setFuel(60.0F));
    }

    /** A tired car: a worn rack pulls it off a straight line, and low pads squeal under braking. */
    private static void worn(ClientGameTestContext context, TestServerContext server) {
        startAt(context, server, -8.5, Y, 0.5);
        server.runOnServer(minecraft -> {
            CarEntity car = serverCar(minecraft);
            car.setCondition(me.trossty.car.entity.CarPart.STEERING_RACK, 10.0F);
            car.setCondition(me.trossty.car.entity.CarPart.BRAKE_PADS, 20.0F);
            log("worn pull " + car.pull());
        });
        drive(context, server, "worn", 60);
        log("worn after hands-off: yaw %.1f, x %.1f".formatted(
                context.<Float>computeOnClient(client -> client.player.getVehicle().getYRot()),
                context.<Double>computeOnClient(client -> client.player.getVehicle().getX())));
        context.takeScreenshot("play_worn_pull");
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(30);
        context.getInput().releaseKey(options -> options.keyJump);
        log("worn braked: " + state(server));
        server.runOnServer(minecraft -> {
            CarEntity car = serverCar(minecraft);
            car.setCondition(me.trossty.car.entity.CarPart.STEERING_RACK, 100.0F);
            car.setCondition(me.trossty.car.entity.CarPart.BRAKE_PADS, 100.0F);
        });
    }

    private static void cow(ClientGameTestContext context, TestServerContext server) {
        startAt(context, server, -32.5, Y, 0.5);
        server.runCommand("summon minecraft:cow -32.5 %d 18.5 {NoAI:1b,Tags:[\"cow\"]}".formatted(Y));
        context.waitTicks(5);
        drive(context, server, "cow", 60);
        String[] cow = new String[1];
        server.runOnServer(minecraft -> {
            var cows = minecraft.overworld().getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class,
                    new AABB(-60, Y - 30, -40, 60, Y + 30, 200), e -> true);
            cow[0] = cows.isEmpty() ? "gone" : "hp %.1f at %.1f %.1f %.1f".formatted(cows.get(0).getHealth(),
                    cows.get(0).getX(), cows.get(0).getY() - Y, cows.get(0).getZ());
        });
        log("cow: " + cow[0] + "; " + state(server));
    }

    private static void punch(ClientGameTestContext context, TestServerContext server) {
        context.getInput().pressKey(options -> options.keyShift);
        context.waitTicks(10);
        server.runOnServer(minecraft -> {
            CarEntity car = serverCar(minecraft);
            car.teleportTo(0.5, Y, 70.5);
            car.setYRot(90.0F);
        });
        server.runCommand("tp @a 0.5 %d 67.5 0 20".formatted(Y));
        server.runOnServer(minecraft -> serverCar(minecraft).setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO));
        // Let it roll to a stop first: the question is what the fists do.
        context.waitTicks(60);
        server.runOnServer(minecraft -> serverCar(minecraft).teleportTo(0.5, Y, 70.5));
        context.waitTicks(5);
        log("punch before: " + state(server));
        for (int i = 0; i < 12; i++) {
            context.getInput().pressKey(options -> options.keyAttack);
            context.waitTicks(12);
        }
        log("punch after 12: " + state(server) + ", items "
                + context.computeOnClient(client -> client.player.getInventory().items.stream()
                .filter(s -> !s.isEmpty()).map(s -> s.getCount() + " " + s.getItem()).toList()));
        context.takeScreenshot("play_punched");
    }

    private static void night(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("time set midnight");
        server.runOnServer(minecraft -> {
            CarEntity car = serverCar(minecraft);
            if (car != null) {
                car.discard();
            }
        });
        server.runCommand("summon car:car 30.5 %d 110.5 {Rotation:[180f,0f],Color:14,Tags:[\"night\"]}".formatted(Y));
        server.runCommand("tp @a 30.5 %d 110.5 180 10".formatted(Y));
        context.waitTicks(5);
        server.runCommand("ride @p mount @e[tag=night,limit=1]");
        context.waitTicks(20);
        context.takeScreenshot("play_night_inside");
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.waitTicks(5);
        context.takeScreenshot("play_night_front");
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.waitTicks(5);
        context.runOnClient(client -> client.player.setXRot(35.0F));
        context.waitTicks(3);
        context.takeScreenshot("play_night_back");
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        server.runCommand("time set noon");
    }
}
