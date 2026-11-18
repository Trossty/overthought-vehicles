package me.trossty.car.test.client;

import me.trossty.car.test.client.PhotoHarness.ClientGameTestContext;
import me.trossty.car.test.client.PhotoHarness.TestServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Pictures of the things only a picture can vouch for: how the vehicles, the
 * wheels, the station blocks, the pipes and the dashboard actually look in the
 * game. Nothing here asserts; the screenshots are the result.
 */
public class CarPhotoTest {

    private static final int Y = 150;

    public void runTest(ClientGameTestContext context, TestServerContext server) {
        {
            // Two layers: the top one is swapped for grass or gravel per scene,
            // and gravel needs stone under it or it falls.
            server.runCommand("fill -20 %d -10 20 %d 40 minecraft:stone".formatted(Y - 2, Y - 1));
            server.runCommand("fill -12 %d -10 12 %d 90 minecraft:air".formatted(Y, Y + 8));
            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("gamerule doDaylightCycle false");
            context.waitTicks(5);

            // The scenes already looked at are skipped unless asked for:
            // -Dcar.photo.all=true runs every one of them again, and
            // -Dcar.photo.scenes=a,b runs just those.
            Map<String, BiConsumer<ClientGameTestContext, TestServerContext>> scenes = new LinkedHashMap<>();
            scenes.put("showroom", this::showroom);
            scenes.put("portraits", this::portraits);
            scenes.put("icons", this::icons);
            scenes.put("tyres", this::tyres);
            scenes.put("station", this::station);
            scenes.put("driving", this::driving);
            scenes.put("batteryGauge", this::batteryGauge);
            scenes.put("rideAndLeave", this::rideAndLeave);
            scenes.put("twoWheelsAndTrailer", this::twoWheelsAndTrailer);
            scenes.put("workstation", this::workstation);
            scenes.put("towing", this::towing);
            scenes.put("flatbed", this::flatbed);
            scenes.put("planeTow", this::planeTow);
            scenes.put("stands", this::stands);
            scenes.put("bedLoad", this::bedLoad);
            scenes.put("towTruck", this::towTruck);
            scenes.put("caravan", this::caravan);
            scenes.put("tyreLooks", this::tyreLooks);
            scenes.put("lids", this::lids);
            scenes.put("hoist", this::hoist);
            scenes.put("garageTools", this::garageTools);
            scenes.put("garageParts", this::garageParts);
            scenes.put("engineInternals", this::engineInternals);
            scenes.put("rapeseed", this::rapeseed);
            scenes.put("itemGrid", this::itemGrid);
            scenes.put("pumpjackLook", this::pumpjackLook);
            scenes.put("hydroCables", this::hydroCables);
            scenes.put("overlayLook", this::overlayLook);
            scenes.put("liftHitboxes", this::liftHitboxes);
            scenes.put("glassWater", this::glassWater);
            scenes.put("roadLife", this::roadLife);
            scenes.put("leaning", this::leaning);
            scenes.put("items3d", this::items3d);
            scenes.put("heldParts", this::heldParts);
            scenes.put("kerbClimb", this::kerbClimb);
            scenes.put("engineStand", this::engineStand);
            scenes.put("carGaps", this::carGaps);
            scenes.put("parked", this::parked);
            scenes.put("riderLean", this::riderLean);
            scenes.put("steeringWheel", this::steeringWheel);
            scenes.put("fuelSystem", this::fuelSystem);
            scenes.put("refuelling", this::refuelling);
            scenes.put("longCorolla", this::longCorolla);
            scenes.put("longFleet", this::longFleet);
            scenes.put("anyLiquid", this::anyLiquid);
            scenes.put("portableGenerators", this::portableGenerators);
            scenes.put("productionLine", this::productionLine);
            scenes.put("energyCables", this::energyCables);
            scenes.put("modTabs", this::modTabs);
            scenes.put("roadsAndCharging", this::roadsAndCharging);
            scenes.put("paintedRoads", this::paintedRoads);
            scenes.put("markingMenuAndTablet", this::markingMenuAndTablet);
            scenes.put("menus", this::menus);
            scenes.put("servicingOnLift", this::servicingOnLift);
            scenes.put("blockGallery", this::blockGallery);
            scenes.put("pumpAndLift", this::pumpAndLift);
            scenes.put("gasPump", this::gasPump);
            scenes.put("drums", this::drums);
            scenes.put("cabins", this::cabins);
            scenes.put("lamps", this::lamps);
            scenes.put("nightRoad", this::nightRoad);
            scenes.put("playtest", Playtest::run);
            scenes.put("tired", Playtest::tired);
            scenes.put("boat", Playtest::boat);
            scenes.put("plane", this::plane);
            scenes.put("flight", Playtest::flight);
            scenes.put("stall", Playtest::stall);
            scenes.put("airturn", Playtest::airturn);
            scenes.put("planecrash", Playtest::planeCrash);
            String only = System.getProperty("car.photo.scenes", "");
            List<String> wanted = Boolean.getBoolean("car.photo.all") ? List.copyOf(scenes.keySet())
                    : only.isBlank() ? List.of() : List.of(only.split(","));
            for (String name : wanted) {
                BiConsumer<ClientGameTestContext, TestServerContext> scene = scenes.get(name.strip());
                if (scene == null) {
                    throw new IllegalArgumentException("no photo scene " + name + "; there are " + scenes.keySet());
                }
                scene.accept(context, server);
            }
        }
    }

    /**
     * Spanner work on a raised lift, on three very different vehicles: the
     * marks have to sit on each one's own wheels and engine bay, the plates
     * have to hang still over them, and a tyre that has come off has to be
     * gone from the car.
     */
    private void servicingOnLift(ClientGameTestContext context, TestServerContext server) {
        String[][] vehicles = {
                {"car:car", "car:toyota_corolla"},
                {"car:fuel_tanker", "car:mercedes_atego"},
                {"car:motorcycle", "car:honda_cbr600rr"},
        };
        for (String[] vehicle : vehicles) {
            String name = vehicle[1].substring(4);
            clear(server);
            server.runCommand("clear @a");
            // The road scenes leave asphalt a layer up, which would bury the lift.
            server.runCommand("fill -6 %d -6 6 %d 6 minecraft:air".formatted(Y, Y + 8));
            server.runCommand("fill -6 %d -6 6 %d 6 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
            server.runCommand("setblock 0 %d 0 car:%s[level=4]{Stop:2}".formatted(Y,
                    vehicle[0].equals("car:motorcycle") ? "motorcycle_lift" : "vehicle_lift"));
            server.runCommand("summon %s 0.5 %d 0.5 {Rotation:[0f,0f],Model:\"%s\",Color:14,Tags:[\"lift\"]}"
                    .formatted(vehicle[0], Y, vehicle[1]));
            context.waitTicks(5);
            // Off with the tyres and the air filter, and a worn battery, so
            // there is something missing, something tired and something fine.
            server.runOnServer(minecraft -> {
                var player = minecraft.getPlayerList().getPlayers().get(0);
                var box = new net.minecraft.world.phys.AABB(-10, Y - 5, -10, 10, Y + 5, 10);
                var car = minecraft.overworld().getEntitiesOfClass(
                        me.trossty.car.entity.CarEntity.class, box, e -> e.getTags().contains("lift")).get(0);
                car.workshop().removePart(player, me.trossty.car.entity.CarPart.WHEELS);
                if (car.hasLid(me.trossty.car.entity.CarEntity.BONNET_BIT)) {
                    car.setLidOpen(me.trossty.car.entity.CarEntity.BONNET_BIT, true);
                }
                car.workshop().removePart(player, me.trossty.car.entity.CarPart.AIR_FILTER);
                car.parts().setCondition(me.trossty.car.entity.CarPart.BATTERY,
                        me.trossty.car.entity.CarPart.MAX_CONDITION * 0.5F);
                System.out.println("car photo lift " + name + ": servicing="
                        + me.trossty.car.block.VehicleLiftBlock.isServicing(minecraft.overworld(), car)
                        + " wheels=" + car.parts().isFitted(me.trossty.car.entity.CarPart.WHEELS)
                        + " filter=" + car.parts().isFitted(me.trossty.car.entity.CarPart.AIR_FILTER)
                        + " tyre at " + car.anchorOf(me.trossty.car.entity.CarPart.WHEELS)
                        + " engine at " + car.anchorOf(me.trossty.car.entity.CarPart.ENGINE));
            });
            server.runCommand("clear @a");
            // From the front corner, and then side on from the near side.
            shot(context, server, "lift_" + name + "_corner", -3.0, Y + 0.4, 5.5, -145, 22);
            shot(context, server, "lift_" + name + "_side", -4.5, Y + 0.2, 0.5, -90, 15);
            shot(context, server, "lift_" + name + "_under", -2.6, Y - 0.2, 0.5, -90, -22);
            // Lying under it looking up, to see the drivetrain joined up; and
            // down into the open bay, to see the boxes sit on what they mark.
            shot(context, server, "lift_" + name + "_belly", -1.2, Y, 0.5, -90, -38);
            shot(context, server, "lift_" + name + "_belly_rear", 0.5, Y, -1.6, 0, -40);
            // Over the battery, looking down into the bay at it: its box has
            // to sit on the battery drawn there.
            net.minecraft.world.phys.Vec3 battery = context.computeOnClient(client -> {
                var car = client.level.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class,
                        new net.minecraft.world.phys.AABB(-10, Y - 5, -10, 10, Y + 5, 10), e -> true).get(0);
                return me.trossty.car.client.render.CarServiceOverlay.boxOf(
                        car, me.trossty.car.entity.CarPart.BATTERY, 1.0F).getCenter();
            });
            System.out.println("car photo lift " + name + " battery box at " + battery);
            // Up in the air over the bay: flying, or the camera drops to the floor.
            server.runCommand("gamemode creative @a");
            server.runOnServer(minecraft -> {
                var player = minecraft.getPlayerList().getPlayers().get(0);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
            server.runCommand("tp @a %.2f %.2f %.2f -90 40".formatted(battery.x - 1.6, battery.y + 1.3 - 1.62, battery.z));
            context.waitTicks(12);
            context.takeScreenshot("lift_" + name + "_bay");
            // Aimed at the bare hub: its box lights and the box by the crosshair speaks.
            net.minecraft.world.phys.Vec3 wheel = context.computeOnClient(client -> {
                var car = client.level.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class,
                        new net.minecraft.world.phys.AABB(-10, Y - 5, -10, 10, Y + 5, 10), e -> true).get(0);
                return me.trossty.car.client.render.CarServiceOverlay.boxOf(
                        car, me.trossty.car.entity.CarPart.WHEELS, 1.0F).getCenter();
            });
            double eyeY = Y + 0.2 + 1.62;
            float pitch = (float) Math.toDegrees(Math.atan2(eyeY - wheel.y, 2.2));
            server.runCommand("tp @a %.2f %.2f %.2f -90 %.1f".formatted(wheel.x - 2.2, Y + 0.2, wheel.z, pitch));
            context.waitTicks(12);
            context.takeScreenshot("lift_" + name + "_aimed");
            // And the back wheel on the far side: every tyre can be got at, not only the first.
            net.minecraft.world.phys.Vec3 far = context.computeOnClient(client -> {
                var car = client.level.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class,
                        new net.minecraft.world.phys.AABB(-10, Y - 5, -10, 10, Y + 5, 10), e -> true).get(0);
                return me.trossty.car.client.render.CarServiceOverlay.boxesOf(
                                car, me.trossty.car.entity.CarPart.WHEELS, 1.0F).stream()
                        .map(net.minecraft.world.phys.AABB::getCenter)
                        .max(java.util.Comparator.comparingDouble(c -> c.x - c.z)).orElseThrow();
            });
            float farPitch = (float) Math.toDegrees(Math.atan2(eyeY - far.y, 2.2));
            server.runCommand("tp @a %.2f %.2f %.2f 90 %.1f".formatted(far.x + 2.2, Y + 0.2, far.z, farPitch));
            context.waitTicks(12);
            context.runOnClient(client -> System.out.println("car photo lift " + name + " far wheel aimed: "
                    + me.trossty.car.client.render.CarServiceOverlay.aimedPart()));
            context.takeScreenshot("lift_" + name + "_far_wheel");
        }
        clear(server);
        server.runCommand("fill -6 %d -6 6 %d 6 minecraft:stone".formatted(Y - 1, Y - 1));
    }

    /**
     * The road paint tin's menu, opened and used with real key presses, then
     * the OBD tablet as a solid thing: on display, dropped, held at an angle
     * and in third person.
     */
    /** Every machine's window, one after another, opened as a click on the block would. */
    private void menus(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -44 %d -2 64 %d 8 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("clear @a");
        server.runCommand("give @a minecraft:coal 12");
        server.runCommand("give @a car:engine 1");
        String[][] machines = {
                {"car:vehicle_workbench", "workbench"}, {"car:vehicle_workstation", "workstation"},
                {"car:fuel_refinery", "refinery"}, {"car:hydrotreater", "hydrotreater"},
                {"car:pumpjack", "pumpjack"}, {"car:generator", "generator"},
                {"car:diesel_generator", "diesel_generator"}, {"car:battery_bank", "battery_bank"},
                {"car:charging_post", "charging_post"}, {"car:toolbox", "toolbox"}};
        for (int i = 0; i < machines.length; i++) {
            int x = i * 8 - 40;
            // Not every one of them turns: a plain setblock stands if the facing one is refused.
            server.runCommand("setblock %d %d 4 %s".formatted(x, Y, machines[i][0]));
            place(server, machines[i][0], x, 4);
            server.runCommand("tp @a %d.5 %d 1.5 0 20".formatted(x, Y));
            context.waitTicks(10);
            int at = x;
            server.runOnServer(minecraft -> {
                var player = minecraft.getPlayerList().getPlayers().get(0);
                var world = minecraft.overworld();
                var pos = at(at, 4);
                var state = world.getBlockState(pos);
                // The charging post opens its window to a crouching click.
                player.setShiftKeyDown(true);
                state.useWithoutItem(world, player, new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.NORTH, pos, false));
                player.setShiftKeyDown(false);
            });
            context.waitTicks(10);
            context.takeScreenshot("menu_" + machines[i][1]);
            context.runOnClient(client -> client.setScreen(null));
        }
        server.runCommand("summon car:car 60.5 %d 4.5 {Rotation:[0f,0f],Tags:[\"menus\"]}".formatted(Y));
        server.runCommand("tp @a 60.5 %d 1.5 0 20".formatted(Y));
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var car = minecraft.overworld().getEntitiesOfClass(me.trossty.car.entity.CarEntity.class,
                    player.getBoundingBox().inflate(8)).get(0);
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (syncId, inventory, opener) -> new me.trossty.car.service.CarServiceScreenHandler(syncId, inventory, car.getId()),
                    car.getDisplayName()));
        });
        context.waitTicks(10);
        context.takeScreenshot("menu_service");
        context.runOnClient(client -> client.setScreen(null));
    }

    private void markingMenuAndTablet(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -4 %d 0 4 %d 8 car:asphalt".formatted(Y, Y));
        server.runCommand("clear @a");
        server.runCommand("item replace entity @a weapon.mainhand with car:yellow_road_paint");
        server.runCommand("tp @a 0.5 %d 4.5 180 -60".formatted(Y + 1));
        context.waitTicks(10);
        // Used in the air: the menu.
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(5);
        context.runOnClient(client -> System.out.println("car photo marking menu open: "
                + (client.screen instanceof me.trossty.car.client.screen.RoadMarkingScreen)));
        context.takeScreenshot("menu_marking");
        context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT);
        context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN);
        context.waitTicks(2);
        context.takeScreenshot("menu_marking_cursor");
        // 8 is the crosswalk.
        context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_8);
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var player = minecraft.getPlayerList().getPlayers().get(0);
            System.out.println("car photo marking chosen: "
                    + me.trossty.car.item.RoadPaintItem.selected(player.getMainHandItem()));
        });
        context.runOnClient(client -> System.out.println("car photo marking menu closed: "
                + (client.screen == null)));
        // And painted where the player looks.
        server.runCommand("tp @a 0.5 %d 6.5 180 60".formatted(Y + 1));
        context.waitTicks(5);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(5);
        server.runOnServer(minecraft -> System.out.println("car photo marking painted: "
                + minecraft.overworld().getBlockState(at(0, 5))));
        shot(context, server, "menu_marking_painted", 0.5, Y + 1, 7.5, 180, 55);

        // The tablet, solid: as an icon, on the ground, in a frame.
        server.runCommand("clear @a");
        String[] modes = {"gui", "ground", "fixed", "thirdperson_righthand"};
        for (int i = 0; i < modes.length; i++) {
            server.runCommand(("summon minecraft:item_display %.1f %.1f 12.5 {item_display:\"%s\","
                    + "transformation:{left_rotation:[0f,0.2f,0f,0.98f],right_rotation:[0f,0f,0f,1f],"
                    + "translation:[0f,0f,0f],scale:[1.4f,1.4f,1.4f]},item:{id:\"car:obd_scanner\",count:1}}")
                    .formatted(-2.4 + i * 1.6, Y + 1.5, modes[i]));
        }
        server.runCommand(("summon minecraft:item_display 0.5 %.1f 12.5 {item_display:\"fixed\","
                + "transformation:{left_rotation:[0f,1f,0f,0f],right_rotation:[0f,0f,0f,1f],"
                + "translation:[0f,0f,0f],scale:[1.4f,1.4f,1.4f]},item:{id:\"car:obd_scanner\",count:1}}")
                .formatted(Y + 3.1));
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "tablet3d_display", 0.0, Y + 0.8, 17.0, 180, -5);
        shot(context, server, "tablet3d_display_side", 3.5, Y + 0.8, 16.0, 150, -5);
        clear(server);
        server.runCommand("summon minecraft:item {Item:{id:\"car:obd_scanner\",count:1},Motion:[0d,0d,0d]}".replace("{Item", "0.5 %d 3.5 {Item".formatted(Y)));
        shot(context, server, "tablet3d_dropped", 0.5, Y + 0.6, 5.5, 180, 30);

        // Held: both hands from a low angle shows its edge and back; one hand shows the side.
        context.runOnClient(client -> client.options.hideGui = false);
        server.runCommand("item replace entity @a weapon.mainhand with car:obd_scanner");
        shot(context, server, "tablet3d_held_low", 0.5, Y, -3.5, 0, -10);
        shot(context, server, "tablet3d_held", 0.5, Y, -3.5, 0, 35);
        server.runCommand("item replace entity @a weapon.offhand with car:white_road_paint");
        shot(context, server, "tablet3d_one_hand", 0.5, Y, -3.5, 0, 10);
        server.runCommand("item replace entity @a weapon.offhand with air");
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "tablet3d_third_person", 0.5, Y, -3.5, 180, 10);
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        shot(context, server, "tablet3d_third_back", 0.5, Y, -3.5, 150, 25);
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        context.runOnClient(client -> client.options.hideGui = false);
        String[] hotbar = {"car:obd_scanner", "car:yellow_road_paint", "car:asphalt"};
        server.runCommand("clear @a");
        for (int i = 0; i < hotbar.length; i++) {
            server.runCommand("item replace entity @a hotbar.%d with %s".formatted(i + 1, hotbar[i]));
        }
        shot(context, server, "tablet3d_hotbar", 0.5, Y, 0.5, 0, -30);
        server.runCommand("clear @a");
    }

    /** One block of road at ground level, painted. */
    private void road(TestServerContext server, int x, int z, String marking, String paint, String facing) {
        server.runCommand("setblock %d %d %d car:asphalt[marking=%s,paint=%s,facing=%s]"
                .formatted(x, Y - 1, z, marking, paint, facing));
    }

    /**
     * Asphalt laid in layers and painted: a two-lane road with edge lines, a
     * double yellow centre line turning dashed, a zebra crossing and stop
     * line, turn arrows, a ramp climbing a layer at a time with a car on it,
     * a little roundabout made of four bends, a car park; and then the OBD
     * tablet held up like a map, read off a wreck.
     */
    private void paintedRoads(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -10 20 %d 45 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("fill -20 %d -10 20 %d 45 minecraft:air".formatted(Y, Y + 8));
        server.runCommand("fill -3 %d 0 3 %d 30 car:asphalt".formatted(Y - 1, Y - 1));
        for (int z = 0; z <= 30; z++) {
            road(server, -3, z, "edge", "white", "north");
            road(server, 3, z, "edge", "white", "south");
            road(server, 0, z, z < 14 ? "double" : "dashed", z < 14 ? "yellow" : "white", "north");
        }
        for (int x = -3; x <= 3; x++) {
            road(server, x, 9, "crosswalk", "white", "north");
            road(server, x, 10, "crosswalk", "white", "north");
        }
        for (int x = -3; x <= -1; x++) {
            road(server, x, 12, "stop", "white", "south");
        }
        road(server, -2, 16, "arrow_left", "white", "north");
        road(server, -2, 20, "arrow", "white", "north");
        road(server, 2, 18, "arrow_right", "white", "south");
        road(server, 2, 23, "arrow", "white", "south");

        // A ramp at the end, a layer higher every block, the centre line carried up it.
        for (int i = 0; i < 8; i++) {
            int z = 31 + i;
            server.runCommand("fill -3 %d %d 3 %d %d car:asphalt[layers=%d]".formatted(Y, z, Y, z, i + 1));
            server.runCommand("setblock 0 %d %d car:asphalt[layers=%d,marking=dashed,facing=north]"
                    .formatted(Y, z, i + 1));
        }
        server.runCommand("summon car:car -1.8 %d 33.5 {Rotation:[0f,0f],Color:14,Tags:[\"ramp\"]}".formatted(Y + 1));

        // A roundabout: four bends make a ring.
        server.runCommand("fill 7 %d 3 10 %d 6 car:asphalt".formatted(Y - 1, Y - 1));
        road(server, 8, 4, "corner", "white", "north");
        road(server, 9, 4, "corner", "white", "east");
        road(server, 9, 5, "corner", "white", "south");
        road(server, 8, 5, "corner", "white", "west");

        // A car park: bays, a disabled bay in blue, a hatched no-parking strip.
        server.runCommand("fill 6 %d 11 12 %d 20 car:asphalt".formatted(Y - 1, Y - 1));
        for (int z = 11; z <= 16; z++) {
            for (int x = 6; x <= 12; x += 2) {
                road(server, x, z, "edge", "white", "north");
            }
        }
        road(server, 9, 13, "parking", "blue", "north");
        road(server, 11, 13, "parking", "white", "north");
        for (int x = 6; x <= 12; x++) {
            road(server, x, 18, "hatch", "yellow", "north");
            road(server, x, 19, "hatch", "yellow", "north");
        }
        road(server, 8, 20, "solid", "red", "east");

        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var wreck = wreck(world);
            wreck.moveTo(-10.0, Y, 20.5, 100.0F, 0.0F);
            wreck.addTag("wreck");
            world.addFreshEntity(wreck);
        });
        context.waitTicks(40);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            StringBuilder line = new StringBuilder("car drive test paint:");
            line.append(" centre=").append(world.getBlockState(at(0, 5).below()));
            line.append(" ramp4=").append(world.getBlockState(at(0, 34)));
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            for (var car : world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box,
                    e -> e.getTags().contains("ramp"))) {
                line.append(String.format(java.util.Locale.ROOT, " | ramp car y=%.3f (ground %d) asphalt=%s",
                        car.getY(), Y, car.onAsphalt()));
            }
            System.out.println(line);
        });

        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "paint_overview", 10.0, Y + 9, -2.0, 30, 25);
        shot(context, server, "paint_topdown", 0.5, Y + 14, 16.5, 180, 90);
        shot(context, server, "paint_ramp", 8.5, Y + 2.5, 34.5, 90, 12);
        shot(context, server, "paint_park_roundabout", 9.5, Y + 9, 11.5, 180, 90);
        shot(context, server, "paint_close", 1.5, Y + 1.2, 6.0, 30, 30);

        // The OBD tablet: unpaired, then paired with the wreck, held up in both hands.
        context.runOnClient(client -> client.options.hideGui = false);
        server.runCommand("clear @a");
        server.runCommand("item replace entity @a weapon.mainhand with car:obd_scanner");
        shot(context, server, "tablet_unpaired", -6.5, Y, 20.5, 90, 45);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            var wreck = world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box,
                    e -> e.getTags().contains("wreck")).get(0);
            me.trossty.car.item.ObdScannerItem.pair(player.getMainHandItem(), wreck, player);
        });
        shot(context, server, "tablet_wreck", -6.5, Y, 20.5, 90, 50);
        shot(context, server, "tablet_wreck_level", -6.5, Y, 20.5, 90, 5);
        server.runCommand("time set midnight");
        shot(context, server, "tablet_night", -6.5, Y, 20.5, 90, 50);
        server.runCommand("time set noon");

        server.runCommand("item replace entity @a weapon.offhand with car:yellow_road_paint");
        shot(context, server, "tablet_one_hand", -6.5, Y, 20.5, 90, 40);
        server.runCommand("clear @a");
        String[] hotbar = {"car:asphalt", "car:white_road_paint", "car:yellow_road_paint", "car:red_road_paint",
                "car:blue_road_paint", "car:tar", "car:obd_scanner"};
        for (int i = 0; i < hotbar.length; i++) {
            server.runCommand("item replace entity @a hotbar.%d with %s".formatted(i, hotbar[i]));
        }
        shot(context, server, "paint_hotbar", 1.5, Y, 6.0, 0, 30);
    }

    /**
     * The new end of the chain, in one street: asphalt laid with a centre line
     * and a slab kerb, a coal generator feeding a battery bank feeding a
     * charging post, an electric car on charge, and an abandoned wreck further
     * down the road for the code reader to read. Then the machine windows.
     */
    private void roadsAndCharging(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("fill -2 %d 0 2 %d 40 car:asphalt".formatted(Y - 1, Y - 1));
        server.runCommand("fill 0 %d 0 0 %d 40 car:asphalt[marking=dashed,facing=north]".formatted(Y - 1, Y - 1));
        server.runCommand("fill 3 %d 0 3 %d 40 car:asphalt[layers=4]".formatted(Y, Y));
        server.runCommand("setblock -9 %d 20 car:generator[facing=south]".formatted(Y));
        server.runCommand("setblock -6 %d 20 car:battery_bank[facing=east]".formatted(Y));
        server.runCommand("setblock -3 %d 20 car:charging_post[facing=east]".formatted(Y));
        int[][] run = {{-8, 20}, {-7, 20}, {-5, 20}, {-4, 20}};
        for (int[] c : run) {
            server.runCommand("setblock %d %d %d car:energy_cable".formatted(c[0], Y, c[1]));
        }
        server.runCommand("summon car:car -0.8 %d 20.5 {Rotation:[0f,0f],Color:3,Tags:[\"ev\"]}".formatted(Y));
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            if (world.getBlockEntity(at(-9, 20)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                g.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 16));
            }
            if (world.getBlockEntity(at(-6, 20)) instanceof me.trossty.car.block.entity.BatteryBankBlockEntity bank) {
                bank.setEnergy(120_000);
            }
            for (int[] c : run) {
                var pos = at(c[0], c[1]);
                var state = world.getBlockState(pos);
                for (var side : net.minecraft.core.Direction.values()) {
                    state = state.updateShape(side, world.getBlockState(pos.relative(side)), world, pos,
                            pos.relative(side));
                }
                world.setBlock(pos, state, 2);
            }
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            var ev = world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box,
                    e -> e.getTags().contains("ev")).get(0);
            ev.setSetup(me.trossty.car.entity.CarSetup.withVariant(me.trossty.car.entity.CarSetup.DEFAULT,
                    me.trossty.car.entity.CarPart.ENGINE, me.trossty.car.entity.CarSetup.ELECTRIC_MOTOR));
            ev.setFuel(0.0F);
            var wreck = wreck(world);
            wreck.moveTo(1.2, Y, 31.5, 200.0F, 0.0F);
            wreck.addTag("wreck");
            world.addFreshEntity(wreck);
        });
        context.waitTicks(120);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            StringBuilder line = new StringBuilder("car drive test roads:");
            if (world.getBlockEntity(at(-6, 20)) instanceof me.trossty.car.block.entity.BatteryBankBlockEntity bank) {
                line.append(" bank=").append(bank.getEnergy()).append(" out=").append(bank.getFlowOut());
            }
            line.append(" post=").append(world.getBlockState(at(-3, 20)));
            line.append(" cable=").append(world.getBlockState(at(-4, 20)).getValue(
                    me.trossty.car.block.EnergyCableBlock.POWERED));
            for (var car : world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box, e -> true)) {
                line.append(" | ").append(car.getModelId()).append(" electric=").append(car.isElectric())
                        .append(String.format(java.util.Locale.ROOT, " fuel=%.1f/%.0f dirty=%.2f odo=%.0f health=%.1f asphalt=%s",
                                car.getFuel(), car.getMaxFuel(), car.getDirtyFuel(), car.parts().getOdometer(),
                                car.damage().getHealth(), car.onAsphalt()));
            }
            System.out.println(line);
        });
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "road_overview", 6.5, Y + 4.5, 9.0, 40, 25);
        shot(context, server, "road_charging", 1.5, Y + 1.6, 16.5, 30, 12);
        shot(context, server, "road_wreck", 4.5, Y + 1.6, 26.5, 35, 12);
        shot(context, server, "road_machines", -5.5, Y + 1.8, 24.0, 180, 20);
        server.runCommand("time set midnight");
        shot(context, server, "road_night", 3.5, Y + 2.5, 14.0, 50, 16);
        shot(context, server, "road_machines_night", -5.5, Y + 1.8, 24.0, 180, 20);
        server.runCommand("time set noon");
        context.runOnClient(client -> client.options.hideGui = false);


        // The windows: the bank, the post with a car on it, the refinery with tar.
        // Stood close, or the window shuts itself as out of reach.
        server.runCommand("tp @a -5.5 %d 22.5 180 20".formatted(Y));
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var world = minecraft.overworld();
            if (world.getBlockEntity(at(-6, 20)) instanceof me.trossty.car.block.entity.BatteryBankBlockEntity bank) {
                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (syncId, inventory, who) -> me.trossty.car.machine.MachineScreenHandler.of(
                                syncId, inventory, bank, null),
                        net.minecraft.network.chat.Component.translatable("block.car.battery_bank")));
            }
        });
        context.waitTicks(10);
        context.takeScreenshot("battery_screen");
        context.runOnClient(client -> client.setScreen(null));
        server.runOnServer(minecraft -> {
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var world = minecraft.overworld();
            if (world.getBlockEntity(at(-3, 20)) instanceof me.trossty.car.block.entity.ChargingPostBlockEntity post) {
                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (syncId, inventory, who) -> me.trossty.car.machine.MachineScreenHandler.of(
                                syncId, inventory, post, null),
                        net.minecraft.network.chat.Component.translatable("block.car.charging_post")));
            }
        });
        context.waitTicks(10);
        context.takeScreenshot("charger_screen");
        context.runOnClient(client -> client.setScreen(null));
        place(server, "car:fuel_refinery", -12, 5);
        server.runCommand("tp @a -11.5 %d 9.5 180 20".formatted(Y));
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var world = minecraft.overworld();
            if (world.getBlockEntity(at(-12, 5)) instanceof me.trossty.car.block.entity.FuelRefineryBlockEntity r) {
                r.setItem(me.trossty.car.block.entity.FuelRefineryBlockEntity.TAR_SLOT,
                        new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.TAR, 7));
                var can = new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.JERRY_CAN);
                me.trossty.car.entity.CarEntity.markRaw(can, true);
                r.setItem(me.trossty.car.block.entity.FuelRefineryBlockEntity.CAN_SLOT, can);
                player.openMenu(r);
            }
        });
        context.waitTicks(10);
        context.takeScreenshot("refinery_tar_screen");
        context.runOnClient(client -> client.setScreen(null));

        server.runCommand("clear @a");
        String[] hotbar = {"car:tar", "car:asphalt", "car:white_road_paint", "car:yellow_road_paint", "car:obd_scanner",
                "car:engine[car:part_variant=8]", "car:battery_bank", "car:charging_post",
                "car:jerry_can[car:fuel_raw=true]"};
        for (int i = 0; i < hotbar.length; i++) {
            server.runCommand("item replace entity @a hotbar.%d with %s".formatted(i, hotbar[i]));
        }
        shot(context, server, "road_hotbar", 0.5, Y, 10.0, 0, 30);
        server.runCommand("clear @a");
    }

    /** Every vehicle's own tank, the jerry can in units, the trip computer, coasting, the holed tank. */
    private void fuelSystem(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:gravel".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 0.5 %d -6.5 {Rotation:[0f,0f],Color:8,Tags:[\"photo\"]}".formatted(Y));
        server.runCommand("summon car:motorcycle 6.5 %d -6.5 {Rotation:[0f,0f],Model:\"car:vespa_primavera\",Tags:[\"vespa\"]}".formatted(Y));
        server.runCommand("summon car:fuel_tanker -7.5 %d -2.5 {Rotation:[0f,0f],Tags:[\"tanker\"]}".formatted(Y));
        context.waitTicks(10);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            for (var car : world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box, e -> true)) {
                System.out.printf("car drive test fuel tank: %s max=%.0f fuel=%.0f range=%s showroom=%s%n",
                        car.getModelId(), car.getMaxFuel(), car.getFuel(),
                        me.trossty.car.entity.CarEntity.formatDistance(car.rangeBlocks(0)).getString(),
                        me.trossty.car.entity.CarEntity.formatDistance(
                                me.trossty.car.entity.CarEntity.estimateRange(car.getDefinition())).getString());
            }
            // The can, in units: a full can into a dry scooter keeps what did not fit.
            var player = minecraft.getPlayerList().getPlayers().get(0);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var vespa = world.getEntitiesOfClass(me.trossty.car.entity.MotorcycleEntity.class, box, e -> true).get(0);
            vespa.setFuel(0);
            var can = new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.JERRY_CAN);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, can);
            while (vespa.pourFrom(player, can, false) > 0) { }
            System.out.printf("car drive test fuel pour: vespa=%.0f/%.0f can=%d%n",
                    vespa.getFuel(), vespa.getMaxFuel(), me.trossty.car.item.JerryCans.units(can));
            // An empty can draws a tank off.
            me.trossty.car.item.JerryCans.setUnits(can, 0);
            while (vespa.pourFrom(player, can, true) > 0) { }
            System.out.printf("car drive test fuel siphon: vespa=%.0f can=%d%n",
                    vespa.getFuel(), me.trossty.car.item.JerryCans.units(can));
            // A holed tank, patched with a slime ball.
            vespa.setLeaking(true);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL, 2));
            vespa.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            System.out.printf("car drive test fuel patch: leaking=%s slime=%d%n",
                    vespa.isLeaking(), player.getMainHandItem().getCount());
            // A crash bad enough holes the tank, sooner or later.
            var tanker = world.getEntitiesOfClass(me.trossty.car.entity.TankerEntity.class, box, e -> true).get(0);
            int crashes = 0;
            try {
                var cooldown = me.trossty.car.entity.CarEntity.class.getDeclaredField("crashCooldown");
                cooldown.setAccessible(true);
                while (!tanker.isLeaking() && crashes < 40) {
                    tanker.damage().setHealth(me.trossty.car.entity.CarEntity.MAX_HEALTH);
                    cooldown.setInt(tanker, 0);
                    tanker.damage().applyReportedCrash(world, 9.0F);
                    crashes++;
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            System.out.printf("car drive test fuel puncture: leaking=%s after %d crashes%n", tanker.isLeaking(), crashes);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        });

        // The leak, from low down at the back of the tanker.
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "fuel_leak", -4.2, Y + 1.0, -3.4, 90, 22);
        context.waitTicks(20);
        context.takeScreenshot("fuel_leak_later");
        context.runOnClient(client -> client.options.hideGui = false);

        // The trip computer, driving and then coasting.
        server.runCommand("ride @p mount @e[tag=photo,limit=1]");
        context.waitTicks(5);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            client.player.setXRot(20);
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(30);
        float[] fuel = new float[3];
        server.runOnServer(minecraft -> fuel[0] = carFuel(minecraft));
        context.waitTicks(10);
        server.runOnServer(minecraft -> fuel[1] = carFuel(minecraft));
        context.takeScreenshot("fuel_hud_driving");
        context.getInput().releaseKey(options -> options.keyUp);
        context.waitTicks(2);
        server.runOnServer(minecraft -> fuel[1] = carFuel(minecraft));
        context.waitTicks(10);
        server.runOnServer(minecraft -> fuel[2] = carFuel(minecraft));
        System.out.printf("car drive test fuel burn: driving=%.4f/tick coasting=%.4f/tick%n",
                (fuel[0] - fuel[1]) / 12.0F, (fuel[1] - fuel[2]) / 10.0F);
        // Down into the last tenth: the chime, the message, the amber range.
        server.runOnServer(minecraft -> {
            var box = new net.minecraft.world.phys.AABB(-60, Y - 20, -60, 60, Y + 20, 80);
            var car = minecraft.overworld().getEntitiesOfClass(
                    me.trossty.car.entity.CarEntity.class, box, e -> e.getTags().contains("photo")).get(0);
            car.setFuel(car.getMaxFuel() * 0.1F + 0.05F);
        });
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(30);
        context.takeScreenshot("fuel_hud_low");
        context.getInput().releaseKey(options -> options.keyUp);
        server.runOnServer(minecraft -> {
            var box = new net.minecraft.world.phys.AABB(-60, Y - 20, -60, 60, Y + 20, 80);
            var car = minecraft.overworld().getEntitiesOfClass(
                    me.trossty.car.entity.CarEntity.class, box, e -> e.getTags().contains("photo")).get(0);
            car.setLeaking(true);
        });
        context.waitTicks(10);
        context.takeScreenshot("fuel_hud_leak");
        server.runCommand("ride @p dismount");
        context.waitTicks(5);
    }

    /**
     * Filling up the MrCrayfish way, with the real buttons: a can held to the
     * filler cap with use held down, the same cap drawn off with an empty can,
     * then the pump's nozzle taken off with a bare hand, its hose followed to
     * the car and held down there, and walked off with until it snaps back.
     */
    private void refuelling(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:gravel".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 0.5 %d -2.5 {Rotation:[0f,0f],Color:14,Tags:[\"photo\"]}".formatted(Y));
        server.runCommand("setblock 4 %d -6 car:fuel_pump[facing=west,half=lower]".formatted(Y));
        server.runCommand("setblock 4 %d -6 car:fuel_pump[facing=west,half=upper]".formatted(Y + 1));
        context.waitTicks(10);
        double[] cap = new double[3];
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var car = world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class,
                    new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50), e -> true).get(0);
            car.setFuel(0);
            var at = car.fillerPos();
            cap[0] = at.x;
            cap[1] = at.y;
            cap[2] = at.z;
            System.out.printf("car drive test refuel filler: offset=%s world=%s%n", car.fillerOffset(), at);
            var player = minecraft.getPlayerList().getPlayers().get(0);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.JERRY_CAN));
            var pump = me.trossty.car.block.FuelPumpBlock.tankAt(world, new net.minecraft.core.BlockPos(4, Y, -6));
            pump.setPetrol(pump.getCapacity());
        });
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

        // The can: hold use at the cap for two seconds.
        aim(context, server, cap[0] + 1.7, cap[2] + 0.4, cap[0], cap[1], cap[2]);
        context.getInput().holdKey(options -> options.keyUse);
        context.waitTicks(30);
        context.takeScreenshot("refuel_can_pouring");
        context.waitTicks(10);
        context.getInput().releaseKey(options -> options.keyUse);
        context.waitTicks(5);
        logFill(server, "can held 40 ticks");
        // Looking away mid-pour stops it.
        context.getInput().holdKey(options -> options.keyUse);
        context.waitTicks(10);
        context.runOnClient(client -> client.player.setXRot(-40));
        context.waitTicks(15);
        context.getInput().releaseKey(options -> options.keyUse);
        logFill(server, "can 10 ticks then looked away for 15");

        // An empty can draws it back off.
        server.runOnServer(minecraft -> me.trossty.car.item.JerryCans.setUnits(
                minecraft.getPlayerList().getPlayers().get(0).getMainHandItem(), 0));
        aim(context, server, cap[0] + 1.7, cap[2] + 0.4, cap[0], cap[1], cap[2]);
        context.getInput().holdKey(options -> options.keyUse);
        context.waitTicks(10);
        context.getInput().releaseKey(options -> options.keyUse);
        context.waitTicks(3);
        logFill(server, "empty can held 10 ticks");

        // The pump: a bare hand on it takes the nozzle.
        server.runOnServer(minecraft -> minecraft.getPlayerList().getPlayers().get(0)
                .setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY));
        aim(context, server, 2.2, -4.6, 4.05, Y + 1.0, -5.5);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(5);
        logFill(server, "took the nozzle");
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "refuel_pump_without_nozzle", 1.0, Y + 1.0, -8.5, -60, 10);
        context.runOnClient(client -> client.options.hideGui = false);

        aim(context, server, cap[0] + 1.7, cap[2] + 0.4, cap[0], cap[1], cap[2]);
        context.getInput().holdKey(options -> options.keyUse);
        context.waitTicks(20);
        context.takeScreenshot("refuel_nozzle_first_person");
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.waitTicks(4);
        context.takeScreenshot("refuel_nozzle_third_person");
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        context.waitTicks(30);
        context.getInput().releaseKey(options -> options.keyUse);
        context.waitTicks(3);
        logFill(server, "nozzle held 54 ticks");

        // Walk off with it and the hose lets go.
        server.runCommand("tp @a 14 %d -3".formatted(Y));
        context.waitTicks(5);
        logFill(server, "walked 10 blocks off");
        server.runOnServer(minecraft -> minecraft.getPlayerList().getPlayers().get(0)
                .setGameMode(net.minecraft.world.level.GameType.CREATIVE));
    }

    /** The Corolla at its full length, from the side and from the front three-quarter, someone beside it for scale. */
    private void longCorolla(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 0.5 %d 10.5 {Rotation:[0f,0f],Color:14}".formatted(Y));
        server.runCommand("summon car:car 0.5 %d 2.5 {Rotation:[0f,0f],Model:\"car:tofas_sahin\",Color:0}".formatted(Y));
        server.runCommand("summon minecraft:villager 2.8 %d 10.5 {NoAI:1b,Rotation:[90f,0f]}".formatted(Y));
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "corolla_side", 9.0, Y + 1.2, 10.5, 90, 5);
        shot(context, server, "corolla_three_quarter", 5.5, Y + 2.0, 16.5, 140, 14);
        shot(context, server, "corolla_beside_sahin", 8.5, Y + 2.0, 6.5, 90, 12);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** Every car and the lorry drawn out like the Corolla: noses in a line, then each from the side. */
    private void longFleet(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:grass_block".formatted(Y - 1, Y - 1));
        String[][] vehicles = {
                {"car:car", "car:tofas_sahin", "0"},
                {"car:car", "car:toyota_corolla", "14"},
                {"car:car", "car:ford_mustang", "11"},
                {"car:car", "car:jeep_wrangler", "5"},
                {"car:pickup", "car:toyota_hilux", "9"},
                {"car:fuel_tanker", "car:mercedes_atego", "0"},
        };
        double x = -12.5;
        for (String[] vehicle : vehicles) {
            server.runCommand("summon %s %.1f %d 14.5 {Rotation:[0f,0f],Model:\"%s\",Color:%s}"
                    .formatted(vehicle[0], x, Y, vehicle[1], vehicle[2]));
            x += vehicle[1].contains("atego") ? 4.0 : 3.5;
        }
        server.runCommand("summon minecraft:villager -15.0 %d 14.5 {NoAI:1b,Rotation:[90f,0f]}".formatted(Y));
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "fleet_front", -3.0, Y + 6.0, 28.0, 180, 28);
        shot(context, server, "fleet_rear", 2.0, Y + 6.0, 2.0, 0, 28);
        shot(context, server, "fleet_above", -2.0, Y + 18.0, 14.5, 0, 90);
        for (String[] vehicle : vehicles) {
            // Out of sight first, so the last one's going leaves no puff of smoke in the shot.
            server.runCommand("tp @e[type=!player] 0.5 %d 14.5".formatted(Y - 60));
            clear(server);
            server.runCommand("kill @e[type=item]");
            server.runCommand("summon %s 0.5 %d 14.5 {Rotation:[0f,0f],Model:\"%s\",Color:%s}"
                    .formatted(vehicle[0], Y, vehicle[1], vehicle[2]));
            server.runCommand("summon minecraft:villager -1.5 %d 9.5 {NoAI:1b,Rotation:[90f,0f]}".formatted(Y));
            shot(context, server, "side_" + vehicle[1].substring(4), 10.0, Y + 1.4, 14.5, 90, 4);
        }
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /**
     * The tanker carrying anything: a lava cauldron loaded and unloaded by the
     * hose, a bucket, a milk bucket refused, a bucket drawn, honey by the
     * bottle, a jerry can; then five loads side by side in the sight glass.
     */
    private void anyLiquid(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:fuel_tanker 0.5 %d -6.5 {Rotation:[0f,0f],Tags:[\"liq\"]}".formatted(Y));
        server.runCommand("setblock 3 %d -7 minecraft:lava_cauldron".formatted(Y));
        context.waitTicks(3);
        server.runOnServer(minecraft -> tanker(minecraft).setHose(me.trossty.car.entity.TankerEntity.Hose.LOAD));
        context.waitTicks(25);
        logLiquid(server, "hose load from lava cauldron");
        server.runCommand("setblock 3 %d -7 minecraft:cauldron".formatted(Y));
        server.runOnServer(minecraft -> tanker(minecraft).setHose(me.trossty.car.entity.TankerEntity.Hose.UNLOAD));
        context.waitTicks(25);
        logLiquid(server, "hose unload into cauldron");

        server.runOnServer(minecraft -> {
            var tanker = tanker(minecraft);
            tanker.setHose(me.trossty.car.entity.TankerEntity.Hose.OFF);
            var player = minecraft.getPlayerList().getPlayers().get(0);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
            Object[][] steps = {
                    {"water bucket in", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET)},
                    {"milk bucket refused", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MILK_BUCKET)},
                    {"empty bucket drawn", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET)},
                    {"honey bottle in", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.HONEY_BOTTLE, 3)},
                    {"second honey bottle", null},
                    {"milk into honey refused", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MILK_BUCKET)},
            };
            for (Object[] step : steps) {
                if (step[1] != null) {
                    player.setItemInHand(hand, (net.minecraft.world.item.ItemStack) step[1]);
                }
                tanker.interact(player, hand);
                System.out.printf("car drive test liquid %s: cargo=%d liquid=%s hand=%s x%d%n", step[0],
                        tanker.getCargo(), tanker.liquidName().getString(),
                        player.getMainHandItem().getItem(), player.getMainHandItem().getCount());
            }
            // A jerry can of petrol will not go into honey, and will into an empty barrel.
            tanker.setCargo(net.minecraft.world.level.material.Fluids.EMPTY, 0);
            var can = new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.JERRY_CAN);
            me.trossty.car.item.JerryCans.setUnits(can, me.trossty.car.item.JerryCans.CAPACITY);
            player.setItemInHand(hand, can);
            tanker.interact(player, hand);
            System.out.printf("car drive test liquid jerry can in: cargo=%d liquid=%s canUnits=%d%n",
                    tanker.getCargo(), tanker.liquidName().getString(), me.trossty.car.item.JerryCans.units(can));
            tanker.interact(player, hand);
            System.out.printf("car drive test liquid jerry can back: cargo=%d canUnits=%d%n",
                    tanker.getCargo(), me.trossty.car.item.JerryCans.units(can));
            // An empty hand turns the hose: off to auto.
            player.setItemInHand(hand, net.minecraft.world.item.ItemStack.EMPTY);
            tanker.interact(player, hand);
            System.out.printf("car drive test liquid hose: %s%n", tanker.getHose());
            // Pocketed and put down again, a load keeps what it is.
            tanker.setCargo((net.minecraft.world.level.material.Fluids.LAVA), 3000);
            var view = new net.minecraft.nbt.CompoundTag();
            tanker.saveWithoutId(view);
            var copy = new me.trossty.car.entity.TankerEntity(me.trossty.car.registry.ModEntities.FUEL_TANKER,
                    minecraft.overworld());
            copy.load(view);
            System.out.printf("car drive test liquid saved: cargo=%d liquid=%s%n",
                    copy.getCargo(), copy.liquidName().getString());
            player.setShiftKeyDown(false);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        });

        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:grass_block".formatted(Y - 1, Y - 1));
        Object[][] loads = {
                {net.minecraft.world.level.material.Fluids.WATER, 0.5F},
                {net.minecraft.world.level.material.Fluids.LAVA, 1.0F},
                {me.trossty.car.registry.ModFluids.MILK, 0.3F},
                {me.trossty.car.registry.ModFluids.HONEY, 0.75F},
                {me.trossty.car.registry.ModFluids.PETROL, 0.6F},
        };
        for (int i = 0; i < loads.length; i++) {
            server.runCommand("summon car:fuel_tanker %.1f %d 14.5 {Rotation:[0f,0f],Color:0,Tags:[\"show%d\"]}"
                    .formatted(-16.0 + i * 8.0, Y, i));
        }
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-60, Y - 20, -60, 60, Y + 20, 80);
            for (int i = 0; i < loads.length; i++) {
                String tag = "show" + i;
                var tanker = world.getEntitiesOfClass(me.trossty.car.entity.TankerEntity.class, box,
                        e -> e.getTags().contains(tag)).get(0);
                tanker.setCargo(((net.minecraft.world.level.material.Fluid) loads[i][0]),
                        Math.round((Float) loads[i][1] * me.trossty.car.entity.TankerEntity.CARGO_CAPACITY));
            }
        });
        context.runOnClient(client -> {
            client.options.hideGui = true;
            for (Object[] load : loads) {
                var variant = ((net.minecraft.world.level.material.Fluid) load[0]);
                var sprite = me.trossty.car.client.render.LiquidColours.sprite(variant);
                System.out.printf("car drive test liquid look %s: colour=%08X sprite=%s%n", load[0],
                        me.trossty.car.client.render.LiquidColours.tint(variant),
                        sprite == null ? null : sprite.contents().name());
            }
        });
        shot(context, server, "liquid_row", 14.0, Y + 6.0, 21.0, 115, 22);
        String[] names = {"water", "lava", "milk", "honey", "petrol"};
        for (int i = 0; i < names.length; i++) {
            shot(context, server, "liquid_" + names[i], -16.0 + i * 8.0 + 5.0, Y + 2.2, 14.7, 90, 12);
        }
        context.runOnClient(client -> client.options.hideGui = false);
    }

    private static me.trossty.car.entity.TankerEntity tanker(net.minecraft.server.MinecraftServer minecraft) {
        var box = new net.minecraft.world.phys.AABB(-60, Y - 20, -60, 60, Y + 20, 80);
        return minecraft.overworld().getEntitiesOfClass(me.trossty.car.entity.TankerEntity.class, box,
                e -> e.getTags().contains("liq")).get(0);
    }

    /**
     * The whole production line, crude and bio: a deposit under a pumpjack,
     * crude pushed into the refinery beside it, raw petrol drawn off by the
     * hydrotreater, and a second refinery cooking sugar cane into biofuel and
     * filling a can. All three generators power it.
     */
    private void productionLine(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:air".formatted(Y, Y + 8));
        server.runCommand("fill -20 %d 0 20 %d 40 minecraft:stone".formatted(Y - 12, Y - 2));
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("fill -10 %d 17 -6 %d 22 car:oil_deposit".formatted(Y - 9, Y - 7));
        server.runCommand("setblock -11 %d 22 car:oil_seep".formatted(Y));
        server.runCommand("setblock -12 %d 23 car:oil_seep".formatted(Y));
        place(server, "car:pumpjack", -9, 20);
        place(server, "car:fuel_refinery", -5, 20);
        place(server, "car:hydrotreater", -4, 20);
        place(server, "car:fuel_refinery", 3, 20);
        server.runCommand("setblock -10 %d 20 car:diesel_generator[facing=south]".formatted(Y));
        server.runCommand("setblock -7 %d 21 car:generator[facing=south]".formatted(Y));
        server.runCommand("setblock -3 %d 20 car:biofuel_generator[facing=south]".formatted(Y));
        server.runCommand("setblock 4 %d 20 car:generator[facing=south]".formatted(Y));
        server.runCommand("setblock 9 %d 25 car:generator[facing=south]".formatted(Y));
        server.runCommand("setblock 10 %d 25 car:biofuel_generator[facing=south]".formatted(Y));
        server.runCommand("setblock 12 %d 25 car:diesel_generator[facing=south]".formatted(Y));
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            if (world.getBlockEntity(at(-10, 20)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                g.getTank().set(me.trossty.car.registry.ModFluids.DIESEL, 1000);
            }
            if (world.getBlockEntity(at(-3, 20)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                g.getTank().set(me.trossty.car.registry.ModFluids.BIOFUEL, 1000);
            }
            if (world.getBlockEntity(at(10, 25)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                g.getTank().set(me.trossty.car.registry.ModFluids.BIOFUEL, 1000);
            }
            if (world.getBlockEntity(at(12, 25)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                g.getTank().set(me.trossty.car.registry.ModFluids.DIESEL, 1000);
            }
            for (int[] c : new int[][] {{-7, 21}, {4, 20}, {9, 25}}) {
                if (world.getBlockEntity(at(c[0], c[1])) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                    g.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 16));
                }
            }
            if (world.getBlockEntity(at(-5, 20)) instanceof me.trossty.car.block.entity.FuelRefineryBlockEntity r) {
                r.setItem(me.trossty.car.block.entity.FuelRefineryBlockEntity.CATALYST_SLOT,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE, 32));
                r.setTemperature(me.trossty.car.block.entity.FuelRefineryBlockEntity.HOT_TEMPERATURE);
                r.setEnergy(me.trossty.car.block.entity.FuelRefineryBlockEntity.ENERGY_CAPACITY);
            }
            if (world.getBlockEntity(at(3, 20)) instanceof me.trossty.car.block.entity.FuelRefineryBlockEntity r) {
                r.setItem(me.trossty.car.block.entity.FuelRefineryBlockEntity.ORGANIC_SLOT,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SUGAR_CANE, 64));
                r.setItem(me.trossty.car.block.entity.FuelRefineryBlockEntity.CATALYST_SLOT,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE, 32));
                var can = new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.JERRY_CAN);
                me.trossty.car.item.JerryCans.setUnits(can, 0);
                r.setItem(me.trossty.car.block.entity.FuelRefineryBlockEntity.CAN_SLOT, can);
                r.setTemperature(me.trossty.car.block.entity.FuelRefineryBlockEntity.HOT_TEMPERATURE);
                r.setEnergy(me.trossty.car.block.entity.FuelRefineryBlockEntity.ENERGY_CAPACITY);
            }
            if (world.getBlockEntity(at(-9, 20)) instanceof me.trossty.car.block.entity.PumpjackBlockEntity j) {
                j.setEnergy(me.trossty.car.block.entity.PumpjackBlockEntity.ENERGY_CAPACITY);
            }
            if (world.getBlockEntity(at(-4, 20)) instanceof me.trossty.car.block.entity.HydrotreaterBlockEntity h) {
                h.setEnergy(me.trossty.car.block.entity.HydrotreaterBlockEntity.ENERGY_CAPACITY);
            }
        });
        context.waitTicks(60);
        logLine(server, "after 3s");
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "line_overview", -3.0, Y + 6.0, 31.0, 180, 22);
        shot(context, server, "line_refinery_front", -6.5, Y + 2.5, 27.0, 180, 8);
        shot(context, server, "line_refinery_side", -1.0, Y + 4.0, 10.0, 33, 12);
        shot(context, server, "line_refinery_back", -14.0, Y + 5.0, 11.0, -45, 12);
        shot(context, server, "line_refinery_top", -1.0, Y + 11.0, 24.0, 129, 42);
        server.runCommand("time set midnight");
        context.waitTicks(10);
        shot(context, server, "line_night", -3.0, Y + 5.0, 30.0, 180, 16);
        server.runCommand("time set noon");
        context.runOnClient(client -> client.options.hideGui = false);
        String[] items = {"car:pumpjack", "car:fuel_refinery", "car:hydrotreater", "car:generator",
                "car:biofuel_generator", "car:diesel_generator", "car:oil_deposit",
                "car:jerry_can[car:fuel_kind=2]", "car:oil_seep"};
        for (int i = 0; i < items.length; i++) {
            server.runCommand("item replace entity @a hotbar.%d with %s".formatted(i, items[i]));
        }
        shot(context, server, "line_hotbar", -3.0, Y, 26.0, 180, 30);
        context.waitTicks(140);
        logLine(server, "after 10s");
    }

    /** The three generators redrawn as framed site sets: running, from every side, and as items. */
    private void portableGenerators(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -4 %d 18 10 %d 32 minecraft:air".formatted(Y, Y + 6));
        server.runCommand("fill -4 %d 18 10 %d 32 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("setblock 1 %d 25 car:generator[facing=south]".formatted(Y));
        server.runCommand("setblock 3 %d 25 car:biofuel_generator[facing=south]".formatted(Y));
        server.runCommand("setblock 5 %d 25 car:diesel_generator[facing=south]".formatted(Y));
        server.runCommand("setblock 7 %d 25 car:diesel_generator[facing=east]".formatted(Y));
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            if (world.getBlockEntity(at(1, 25)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                g.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 16));
            }
            if (world.getBlockEntity(at(3, 25)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                g.getTank().set(me.trossty.car.registry.ModFluids.BIOFUEL, 1000);
            }
            if (world.getBlockEntity(at(5, 25)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                g.getTank().set(me.trossty.car.registry.ModFluids.DIESEL, 1000);
            }
        });
        context.waitTicks(40);
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "gen_front", 3.0, Y + 1.2, 27.6, 180, 22);
        shot(context, server, "gen_angle", 6.0, Y + 1.4, 27.2, 127, 22);
        shot(context, server, "gen_close", 4.6, Y - 0.9, 27.2, 165, 8);
        shot(context, server, "gen_back", 1.8, Y + 1.0, 23.0, -25, 22);
        shot(context, server, "gen_side", 9.0, Y + 1.0, 24.0, 63, 22);
        server.runCommand("time set midnight");
        context.waitTicks(10);
        shot(context, server, "gen_night", 3.0, Y + 1.2, 27.6, 180, 22);
        server.runCommand("time set noon");
        context.runOnClient(client -> client.options.hideGui = false);
        String[] items = {"car:generator", "car:biofuel_generator", "car:diesel_generator"};
        for (int i = 0; i < items.length; i++) {
            server.runCommand("item replace entity @a hotbar.%d with %s".formatted(i, items[i]));
        }
        shot(context, server, "gen_hotbar", 3.0, Y, 28.0, 180, 30);
    }

    /** A coal generator feeding a refinery and, down a branch, a hydrotreater, over a run of cable that climbs a step. */
    private void energyCables(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -4 %d 18 14 %d 34 minecraft:air".formatted(Y, Y + 6));
        server.runCommand("fill -4 %d 18 14 %d 34 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("setblock 0 %d 25 car:generator[facing=south]".formatted(Y));
        place(server, "car:fuel_refinery", 10, 25);
        place(server, "car:hydrotreater", 5, 29);
        int[][] run = {{1, 0, 25}, {2, 0, 25}, {2, 1, 25}, {3, 1, 25}, {4, 1, 25}, {4, 0, 25},
                {5, 0, 25}, {6, 0, 25}, {5, 0, 26}, {5, 0, 27}, {5, 0, 28}};
        for (int[] c : run) {
            server.runCommand("setblock %d %d %d car:energy_cable".formatted(c[0], Y + c[1], c[2]));
        }
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            if (world.getBlockEntity(at(0, 25)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                g.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 16));
            }
            // re-place each cable through its own state so it joins up as a hand-laid one would
            for (int[] c : run) {
                var pos = new net.minecraft.core.BlockPos(c[0], Y + c[1], c[2]);
                var state = world.getBlockState(pos);
                for (var side : net.minecraft.core.Direction.values()) {
                    state = state.updateShape(side, world.getBlockState(pos.relative(side)), world, pos,
                            pos.relative(side));
                }
                world.setBlock(pos, state, 2);
            }
        });
        context.waitTicks(200);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            StringBuilder line = new StringBuilder("car drive test cables:");
            if (world.getBlockEntity(at(0, 25)) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                line.append(" gen=").append(g.getEnergy()).append(" burning=").append(g.isBurning());
            }
            for (int[] c : run) {
                if (world.getBlockEntity(new net.minecraft.core.BlockPos(c[0], Y + c[1], c[2]))
                        instanceof me.trossty.car.block.entity.EnergyCableBlockEntity cable) {
                    line.append(" c").append(cable.getFlow());
                }
            }
            if (world.getBlockEntity(at(10, 25)) instanceof me.trossty.car.block.entity.FuelRefineryBlockEntity r) {
                line.append(" refinery=").append(r.getEnergy());
            }
            if (world.getBlockEntity(at(5, 29)) instanceof me.trossty.car.block.entity.HydrotreaterBlockEntity h) {
                line.append(" hydrotreater=").append(h.getEnergy());
            }
            line.append(" state=").append(world.getBlockState(new net.minecraft.core.BlockPos(2, Y + 1, 25)));
            System.out.println(line);
        });
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "cable_overview", 3.5, Y + 2.5, 33.5, 175, 30);
        shot(context, server, "cable_step", 3.0, Y + 0.6, 22.0, 0, 15);
        shot(context, server, "cable_branch", 9.0, Y + 1.5, 29.5, 125, 30);
        context.runOnClient(client -> client.options.hideGui = false);
        server.runCommand("item replace entity @a hotbar.0 with car:energy_cable");
        shot(context, server, "cable_hotbar", 3.0, Y, 33.0, 180, 30);
    }

    /** Every tab of ours, open, page by page: what a player browsing them sees. */
    private void modTabs(ClientGameTestContext context, TestServerContext server) {
        java.util.List<net.minecraft.world.item.CreativeModeTab> tabs = new java.util.ArrayList<>();
        context.runOnClient(client -> {
            net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(client.player.connection.enabledFeatures(),
                    true, client.level.registryAccess());
            for (var entry : net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
                if (entry.getKey().location().getNamespace().equals(me.trossty.car.CarMain.MOD_ID)) {
                    tabs.add(entry.getValue());
                    System.out.println("car photo tab " + entry.getKey().location() + ": "
                            + entry.getValue().getDisplayItems().size() + " stacks: "
                            + entry.getValue().getDisplayItems().stream()
                                    .map(stack -> net.minecraft.core.registries.BuiltInRegistries.ITEM
                                            .getKey(stack.getItem()).getPath())
                                    .collect(java.util.stream.Collectors.joining(" ")));
                }
            }
            // Nothing of ours left out of every tab.
            java.util.Set<net.minecraft.world.item.Item> shown = new java.util.HashSet<>();
            tabs.forEach(tab -> tab.getDisplayItems().forEach(stack -> shown.add(stack.getItem())));
            String missing = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                    .filter(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)
                            .getNamespace().equals(me.trossty.car.CarMain.MOD_ID) && !shown.contains(item))
                    .map(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath())
                    .collect(java.util.stream.Collectors.joining(" "));
            System.out.println("car photo tabs missing: [" + missing + "]");
            client.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(client.player,
                    client.player.connection.enabledFeatures(), true));
        });
        context.waitTicks(3);
        for (var tab : tabs) {
            String name = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getPath();
            int[] pages = new int[1];
            context.runOnClient(client -> {
                try {
                    net.neoforged.fml.util.ObfuscationReflectionHelper.findMethod(
                            net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class, "selectTab",
                            net.minecraft.world.item.CreativeModeTab.class).invoke(client.screen, tab);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
                int rows = (tab.getDisplayItems().size() + 8) / 9;
                pages[0] = Math.max(1, (rows + 4) / 5);
            });
            for (int page = 0; page < pages[0]; page++) {
                float at = pages[0] == 1 ? 0.0F : page / (float) (pages[0] - 1);
                context.runOnClient(client -> {
                    var screen = (net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) client.screen;
                    screen.getMenu().scrollTo(at);
                    try {
                        // The pointer off in the corner, so no tooltip covers the slots.
                        for (String axis : new String[]{"xpos", "ypos"}) {
                            var pointer = net.neoforged.fml.util.ObfuscationReflectionHelper.findField(
                                    net.minecraft.client.MouseHandler.class, axis);
                            pointer.setDouble(client.mouseHandler, 0.0);
                        }
                        var field = net.neoforged.fml.util.ObfuscationReflectionHelper.findField(
                                net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class, "scrollOffs");
                        field.setFloat(screen, at);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                });
                context.waitTicks(4);
                context.takeScreenshot("tab_" + name + "_" + page);
            }
        }
        context.runOnClient(client -> client.setScreen(null));
    }

    private static net.minecraft.core.BlockPos at(int x, int z) {
        return new net.minecraft.core.BlockPos(x, Y, z);
    }

    /** Sets a machine down facing south, every part of it, as placing it by hand would. */
    /** A tank of water beside a car, seen through both its side windows. */
    private void glassWater(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill 92 %d 94 110 %d 116 minecraft:air".formatted(Y, Y + 6));
        server.runCommand("fill 92 %d 94 110 %d 116 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        // a glass tank of water standing at window height
        server.runCommand("fill 103 %d 94 106 %d 105 minecraft:glass".formatted(Y, Y + 3));
        server.runCommand("fill 104 %d 95 105 %d 104 minecraft:water".formatted(Y, Y + 2));
        server.runCommand("summon car:car 100.5 %d 99.5 {Rotation:[0f,0f],Color:3}".formatted(Y));
        context.waitTicks(20);
        // through both side windows to the tank on the far side
        shot(context, server, "glass_water_side", 97.0, Y + 1.5, 99.4, -90, 4);
        shot(context, server, "glass_water_quarter", 97.5, Y + 2.2, 96.0, -60, 20);
    }

    /** The pumpjack alone, from the side, the front and a three-quarter angle, mid-stroke. */
    private void pumpjackLook(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        place(server, "car:pumpjack", 0, 0);
        context.waitTicks(30);
        shot(context, server, "pumpjack_side", 6.5, Y + 1.6, -1.0, 90, 8);
        shot(context, server, "pumpjack_front", 0.5, Y + 1.6, 6.0, 180, 8);
        shot(context, server, "pumpjack_quarter", 5.0, Y + 2.6, 4.5, 135, 16);
    }

    /** A hydrotreater with a cable on every side it takes power from, to see each plate meet its body. */
    private void hydroCables(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -4 %d -4 4 %d 4 minecraft:air".formatted(Y, Y + 4));
        place(server, "car:hydrotreater", 0, 0);
        // none of them touch each other, so every one ends in a plate on the machine
        int[][] cables = {{0, 0, -1}, {1, 0, 0}, {0, 1, 1}, {-1, 1, 0}, {0, 2, 0}};
        for (int[] c : cables) {
            server.runCommand("setblock %d %d %d car:energy_cable".formatted(c[0], Y + c[1], c[2]));
        }
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            for (int[] c : cables) {
                var pos = new net.minecraft.core.BlockPos(c[0], Y + c[1], c[2]);
                var state = world.getBlockState(pos);
                for (var side : net.minecraft.core.Direction.values()) {
                    state = state.updateShape(side, world.getBlockState(pos.relative(side)), world, pos,
                            pos.relative(side));
                }
                world.setBlock(pos, state, 2);
            }
        });
        context.waitTicks(20);
        shot(context, server, "hydro_cables_ne", 3.2, Y + 0.6, -2.2, 45, 18);
        shot(context, server, "hydro_cables_sw", -2.2, Y + 1.4, 3.2, 225, 14);
        shot(context, server, "hydro_cables_north", 0.5, Y, -1.9, 0, 30);
        shot(context, server, "hydro_cables_east", 2.9, Y, 0.5, 90, 30);
        shot(context, server, "hydro_cables_south", 0.5, Y + 0.8, 2.9, 180, 26);
        shot(context, server, "hydro_cables_west", -1.9, Y + 0.8, 0.5, 270, 26);
        shot(context, server, "hydro_cables_top", 0.5, Y + 3.0, -1.6, 0, 50);
    }

    /**
     * What Jade or WTHIT shows over a worn car and a working machine; run with
     * -Pcar.jade=true or -Pcar.wthit=true to have one of them there.
     */
    private void overlayLook(ClientGameTestContext context, TestServerContext server) {
        String overlay = net.neoforged.fml.ModList.get().isLoaded("wthit") ? "wthit" : "jade";
        clear(server);
        server.runCommand("fill -6 %d -6 6 %d 6 minecraft:air".formatted(Y, Y + 4));
        place(server, "car:hydrotreater", 3, 0);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var wreck = wreck(world);
            wreck.moveTo(-2.0, Y, 0.5, 90.0F, 0.0F);
            world.addFreshEntity(wreck);
            if (world.getBlockEntity(at(3, 0)) instanceof me.trossty.car.block.entity.HydrotreaterBlockEntity h) {
                h.setEnergy(me.trossty.car.block.entity.HydrotreaterBlockEntity.ENERGY_CAPACITY / 2);
            }
        });
        context.waitTicks(20);
        shot(context, server, overlay + "_car", -2.0, Y, 5.0, 180, 12);
        // the overlay asks the server for a machine's tanks and charge and
        // draws them when the answer comes, so give it a moment on each half
        server.runCommand("tp @a 3.5 %d 3.0 180 22".formatted(Y));
        context.waitTicks(40);
        shot(context, server, overlay + "_machine_low", 3.5, Y, 3.0, 180, 22);
        server.runCommand("tp @a 3.5 %d 3.0 180 -4".formatted(Y));
        context.waitTicks(40);
        shot(context, server, overlay + "_machine_high", 3.5, Y, 3.0, 180, -4);
    }

    /** The outline the cursor draws on each part of the two lifts: a column, the power unit, the bike deck and vise. */
    private void liftHitboxes(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -4 %d -4 14 %d 4 minecraft:air".formatted(Y, Y + 4));
        // x, z and how many stops up: the top for one bike lift, the first for the other
        int[][] lifts = {{0, 0, 0}, {6, 0, 2}, {10, 0, 1}};
        server.runCommand("setblock 0 %d 0 car:vehicle_lift[facing=north]".formatted(Y));
        server.runCommand("setblock 6 %d 0 car:motorcycle_lift[facing=north]".formatted(Y));
        server.runCommand("setblock 10 %d 0 car:motorcycle_lift[facing=north]".formatted(Y));
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            for (int[] l : lifts) {
                var pos = at(l[0], l[1]);
                var state = world.getBlockState(pos);
                var lift = (me.trossty.car.block.VehicleLiftBlock) state.getBlock();
                lift.placeParts(world, pos, state);
                if (world.getBlockEntity(pos) instanceof me.trossty.car.block.entity.VehicleLiftBlockEntity be) {
                    for (int i = 0; i < l[2]; i++) {
                        be.step(true);
                    }
                }
            }
        });
        context.waitTicks(60);
        aimedShot(context, server, "lift_hit_column", -3.5, 3.0, -0.8, Y + 1.5, 0.5);
        aimedShot(context, server, "lift_hit_power", 3.5, 3.2, 1.82, Y + 1.3, 0.8);
        aimedShot(context, server, "lift_hit_plate", 0.5, 3.5, 0.5, Y + 0.1, 0.5);
        aimedShot(context, server, "bike_hit_front", 6.5, -3.0, 6.5, Y + 1.06, -0.3);
        aimedShot(context, server, "bike_hit_vise", 8.5, 3.5, 6.5, Y + 1.3, 1.35);
        aimedShot(context, server, "bike_hit_legs", 8.0, 3.0, 10.5, Y + 0.3, 0.6);
    }

    /** A shot from where a player stands at x, z, looking at a point. */
    private void aimedShot(ClientGameTestContext context, TestServerContext server, String name,
                           double x, double z, double tx, double ty, double tz) {
        double dx = tx - x;
        double dy = ty - (Y + 1.62);
        double dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
        shot(context, server, name, x, Y, z, yaw, pitch);
    }

    private void place(TestServerContext server, String block, int x, int z) {
        server.runCommand("setblock %d %d %d %s[facing=south]".formatted(x, Y, z, block));
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var pos = at(x, z);
            var state = world.getBlockState(pos);
            if (state.getBlock() instanceof me.trossty.car.block.MultiblockBlock machine) {
                machine.placeParts(world, pos, state);
            }
        });
    }

    private void logLine(TestServerContext server, String when) {
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            StringBuilder line = new StringBuilder("car drive test line " + when + ":");
            if (world.getBlockEntity(at(-9, 20)) instanceof me.trossty.car.block.entity.PumpjackBlockEntity j) {
                line.append(" jack[working=%s crude=%d energy=%d reserve=%d]".formatted(
                        j.isWorking(), j.getCrude().getUnits(), j.getEnergy(), j.estimateReserve()));
            }
            for (int x : new int[] {-5, 3}) {
                if (world.getBlockEntity(at(x, 20)) instanceof me.trossty.car.block.entity.FuelRefineryBlockEntity r) {
                    line.append(" refinery%d[working=%s feed=%d:%d product=%d:%d energy=%d can=%s]".formatted(x,
                            r.isWorking(), r.getFeed(), r.getBiomass(), r.getProduct(), r.getPetrol(), r.getEnergy(),
                            r.getItem(me.trossty.car.block.entity.FuelRefineryBlockEntity.CAN_SLOT).isEmpty() ? "-"
                                    : me.trossty.car.item.JerryCans.units(r.getItem(
                                            me.trossty.car.block.entity.FuelRefineryBlockEntity.CAN_SLOT)) + "/"
                                    + me.trossty.car.entity.CarEntity.fuelIn(r.getItem(
                                            me.trossty.car.block.entity.FuelRefineryBlockEntity.CAN_SLOT))));
                }
            }
            if (world.getBlockEntity(at(-4, 20)) instanceof me.trossty.car.block.entity.HydrotreaterBlockEntity h) {
                line.append(" hydro[working=%s in=%d %s out=%d %s energy=%d]".formatted(h.isWorking(),
                        h.getInput().getUnits(), h.getInput().getFluid(), h.getOutput().getUnits(),
                        h.getOutput().getFluid(), h.getEnergy()));
            }
            for (int[] c : new int[][] {{-10, 20}, {-7, 21}, {-3, 20}, {4, 20}}) {
                if (world.getBlockEntity(at(c[0], c[1])) instanceof me.trossty.car.block.entity.GeneratorBlockEntity g) {
                    line.append(" gen%s[lit=%s energy=%d tank=%d coal=%d]".formatted(g.getKind(),
                            g.isBurning(), g.getEnergy(), g.getTank().getUnits(), g.getItem(0).getCount()));
                }
            }
            int deposits = 0;
            for (var pos : net.minecraft.core.BlockPos.betweenClosed(-10, Y - 9, 17, -6, Y - 7, 22)) {
                if (world.getBlockState(pos).is(me.trossty.car.registry.ModBlocks.OIL_DEPOSIT)) {
                    deposits++;
                }
            }
            line.append(" deposits=").append(deposits);
            System.out.println(line);
        });
    }

    private void logLiquid(TestServerContext server, String when) {
        server.runOnServer(minecraft -> {
            var tanker = tanker(minecraft);
            var cauldron = minecraft.overworld().getBlockState(new net.minecraft.core.BlockPos(3, Y, -7));
            System.out.printf("car drive test liquid %s: cargo=%d liquid=%s cauldron=%s%n", when,
                    tanker.getCargo(), tanker.liquidName().getString(), cauldron.getBlock());
        });
    }

    /** Stands at (x, z) on the floor and looks at the point given. */
    private void aim(ClientGameTestContext context, TestServerContext server,
                     double x, double z, double atX, double atY, double atZ) {
        double dx = atX - x;
        double dy = atY - (Y + 1.62);
        double dz = atZ - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        server.runCommand("tp @a %.3f %d %.3f %.2f %.2f".formatted(x, Y, z, yaw, pitch));
        context.waitTicks(6);
    }

    private void logFill(TestServerContext server, String when) {
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var held = player.getMainHandItem();
            var pump = me.trossty.car.block.FuelPumpBlock.tankAt(world, new net.minecraft.core.BlockPos(4, Y, -6));
            var state = world.getBlockState(new net.minecraft.core.BlockPos(4, Y, -6));
            System.out.printf("car drive test refuel %s: car=%.1f held=%s canUnits=%s pump=%d nozzleHung=%s%n",
                    when, carFuel(minecraft), held.getItem(),
                    held.is(me.trossty.car.registry.ModItems.JERRY_CAN)
                            ? String.valueOf(me.trossty.car.item.JerryCans.units(held)) : "-",
                    pump == null ? -1 : pump.getPetrol(),
                    state.hasProperty(me.trossty.car.block.FuelPumpBlock.NOZZLE)
                            ? state.getValue(me.trossty.car.block.FuelPumpBlock.NOZZLE) : null);
        });
    }

    private static float carFuel(net.minecraft.server.MinecraftServer minecraft) {
        var box = new net.minecraft.world.phys.AABB(-60, Y - 20, -60, 60, Y + 20, 80);
        return minecraft.overworld().getEntitiesOfClass(
                me.trossty.car.entity.CarEntity.class, box, e -> e.getTags().contains("photo")).get(0).getFuel();
    }

    /** An old Jeep for the OBD to read: dented, worn through, a part or two gone, stale fuel. */
    private static me.trossty.car.entity.CarEntity wreck(net.minecraft.server.level.ServerLevel world) {
        var car = me.trossty.car.registry.ModEntities.CAR.create(world);
        car.setModel(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("car", "jeep_wrangler"));
        var defined = car.getDefinition();
        car.setSetup(defined.factory().setup());
        car.refreshDimensions();
        car.setColor(net.minecraft.world.item.DyeColor.LIGHT_GRAY);
        car.damage().setHealth(me.trossty.car.entity.CarEntity.MAX_HEALTH * 0.34F);
        car.parts().setOdometer(me.trossty.car.entity.CarParts.HIGH_MILEAGE * 2.5F);
        var random = net.minecraft.util.RandomSource.create(5);
        for (var part : me.trossty.car.entity.CarPart.values()) {
            car.parts().setCondition(part, me.trossty.car.entity.CarPart.MAX_CONDITION * (0.12F + 0.5F * random.nextFloat()));
        }
        car.parts().setCondition(me.trossty.car.entity.CarPart.BATTERY, me.trossty.car.entity.CarPart.MAX_CONDITION * 0.05F);
        car.parts().setCondition(me.trossty.car.entity.CarPart.SPARK_PLUGS, 0.0F);
        car.setFuel(0.0F);
        car.addFuel(1.0F, car.getEngineFuel(), false);
        car.setDirtyFuel(0.8F);
        return car;
    }

    private void clear(TestServerContext server) {
        server.runCommand("kill @e[type=!player]");
    }



    /** The fuel pump, with and without its nozzle, and the lift at every height, one with a car on it. */
    /** The pump close up: three of them part full, one with its nozzle out, at noon and at night. */
    private void gasPump(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:air".formatted(Y, Y + 8));
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        int[] fills = {1800, 900, 150};
        for (int i = 0; i < 3; i++) {
            boolean nozzle = i != 1;
            int x = -2 + i * 2;
            server.runCommand("setblock %d %d 4 car:fuel_pump[half=lower,facing=south,nozzle=%s]".formatted(x, Y, nozzle));
            server.runCommand("setblock %d %d 4 car:fuel_pump[half=upper,facing=south,nozzle=%s]".formatted(x, Y + 1, nozzle));
        }
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            for (int i = 0; i < 3; i++) {
                if (world.getBlockEntity(new BlockPos(-2 + i * 2, Y, 4))
                        instanceof me.trossty.car.block.entity.FuelPumpBlockEntity pump) {
                    pump.setPetrol(fills[i]);
                }
            }
        });
        context.waitTicks(20);
        shot(context, server, "gaspump_row", 0.5, Y + 1.4, 9.0, 180, 8);
        shot(context, server, "gaspump_close", -1.5, Y + 1.2, 6.6, 180, 5);
        shot(context, server, "gaspump_side", 2.8, Y + 1.3, 6.8, 140, 6);
        shot(context, server, "gaspump_glass", -1.5, Y - 0.6, 6.0, 180, 35);
        shot(context, server, "gaspump_glass2", 0.5, Y - 0.6, 6.0, 180, 35);
        shot(context, server, "gaspump_back", -0.5, Y + 1.5, 1.5, 20, 8);
        server.runCommand("time set midnight");
        context.waitTicks(5);
        shot(context, server, "gaspump_night", 0.5, Y + 1.4, 8.0, 180, 8);
        server.runCommand("time set noon");
    }

    /** A row of drums, each painted the colour of what is in it, by a pump for scale. */
    private void drums(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:air".formatted(Y, Y + 8));
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("setblock -4 %d 4 car:fuel_pump[half=lower,facing=south,nozzle=true]".formatted(Y));
        server.runCommand("setblock -4 %d 4 car:fuel_pump[half=upper,facing=south,nozzle=true]".formatted(Y + 1));
        net.minecraft.world.level.material.Fluid[] liquids = {null, me.trossty.car.registry.ModFluids.PETROL,
                me.trossty.car.registry.ModFluids.DIESEL, net.minecraft.world.level.material.Fluids.WATER,
                net.minecraft.world.level.material.Fluids.LAVA, me.trossty.car.registry.ModFluids.MILK,
                me.trossty.car.registry.ModFluids.CRUDE_OIL};
        for (int i = 0; i < liquids.length; i++) {
            server.runCommand("setblock %d %d 4 car:fuel_drum[facing=south]".formatted(-2 + i, Y));
        }
        server.runCommand("setblock 0 %d 5 car:fuel_drum[facing=east]".formatted(Y));
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            for (int i = 0; i < liquids.length; i++) {
                if (liquids[i] != null && world.getBlockEntity(new BlockPos(-2 + i, Y, 4))
                        instanceof me.trossty.car.block.entity.FuelDrumBlockEntity drum) {
                    me.trossty.car.block.LiquidStores.move(drum.getFluidStorage(), liquids[i], 4000, true);
                }
            }
            // One carried full, in the hand, and one empty in the hotbar beside it.
            var stack = new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModBlocks.FUEL_DRUM);
            stack.set(me.trossty.car.registry.ModComponents.STORED_FLUID,
                    new me.trossty.car.registry.ModComponents.StoredFluid(
                            (net.minecraft.world.level.material.Fluids.LAVA),
                            net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME));
            for (var player : minecraft.getPlayerList().getPlayers()) {
                player.getInventory().setItem(0, stack.copy());
                player.getInventory().setItem(1,
                        new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModBlocks.FUEL_DRUM));
                player.getInventory().selected = 0;
            }
        });
        context.waitTicks(20);
        shot(context, server, "drum_row", 1.0, Y + 1.3, 8.5, 180, 14);
        shot(context, server, "drum_close", 0.0, Y + 1.0, 6.4, 180, 22);
        shot(context, server, "drum_top", 1.0, Y + 2.6, 6.4, 180, 62);
        shot(context, server, "drum_corner", 5.5, Y + 1.4, 7.5, 135, 18);
    }

    private void pumpAndLift(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:air".formatted(Y, Y + 8));
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("setblock -3 %d 4 car:fuel_pump[half=lower,facing=south,nozzle=true]".formatted(Y));
        server.runCommand("setblock -3 %d 4 car:fuel_pump[half=upper,facing=south,nozzle=true]".formatted(Y + 1));
        server.runCommand("setblock -1 %d 4 car:fuel_pump[half=lower,facing=east,nozzle=false]".formatted(Y));
        server.runCommand("setblock -1 %d 4 car:fuel_pump[half=upper,facing=east,nozzle=false]".formatted(Y + 1));
        // The three stops: on the floor, a little way up, right up.
        int[] stops = {0, 1, 4};
        for (int stop = 0; stop < stops.length; stop++) {
            server.runCommand("setblock %d %d 4 car:vehicle_lift[level=%d,facing=north]{Stop:%d}"
                    .formatted(2 + stop * 4, Y, stops[stop], stop));
        }
        server.runCommand("setblock 4 %d 13 car:vehicle_lift[level=4,facing=east]{Stop:2}".formatted(Y));
        server.runCommand("summon car:car 4.5 %d 13.5 {Rotation:[90f,0f],Model:\"car:toyota_corolla\",Color:14}"
                .formatted(Y + 1));
        context.waitTicks(20);
        shot(context, server, "pump_front", -2.5, Y + 1.2, 7.5, 165, 12);
        shot(context, server, "pump_close", -3.0, Y + 0.8, 6.0, 180, 10);
        shot(context, server, "lift_levels", 8.0, Y + 2.6, 10.5, 180, 22);
        shot(context, server, "lift_levels_side", 17.5, Y + 1.2, 5.0, 100, 12);
        shot(context, server, "lift_car", -0.5, Y + 1.2, 17.5, -135, 10);
    }

    /**
     * Every block of the mod side by side on a clean floor, and tanks of
     * several shapes holding several liquids: how they look next to each
     * other, and that a block of tank cells reads as one vessel.
     */
    private void blockGallery(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:air".formatted(Y, Y + 8));
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        String[] row = {"car:vehicle_workbench", "car:vehicle_workstation", "car:vehicle_lift", "car:fuel_pump",
                "car:fuel_valve", "car:fuel_pipe", "car:generator", "car:diesel_generator",
                "car:biofuel_generator", "car:battery_bank", "car:charging_post", "car:energy_cable"};
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            for (int i = 0; i < row.length; i++) {
                var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(row[i]));
                var pos = new BlockPos(-11 + i * 2, Y, 4);
                var state = block.defaultBlockState();
                var facing = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
                if (state.hasProperty(facing)) {
                    state = state.setValue(facing, Direction.SOUTH);
                }
                world.setBlockAndUpdate(pos, state);
                if (block instanceof me.trossty.car.block.FuelPumpBlock) {
                    world.setBlockAndUpdate(pos.above(), state.setValue(me.trossty.car.block.FuelPumpBlock.HALF,
                            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
                }
            }
            // Tanks: a lone cell of petrol, a tower of lava, a two-by-two
            // block of water half full, and a row of milk.
            placeTank(world, new BlockPos(-8, Y, 10), 1, 1, 1);
            placeTank(world, new BlockPos(-5, Y, 10), 1, 3, 1);
            placeTank(world, new BlockPos(-2, Y, 10), 2, 2, 2);
            placeTank(world, new BlockPos(2, Y, 10), 3, 1, 1);
        });
        context.waitTicks(3);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            fillTank(world, new BlockPos(-8, Y, 10), me.trossty.car.registry.ModFluids.PETROL, 11000);
            fillTank(world, new BlockPos(-5, Y, 10), net.minecraft.world.level.material.Fluids.LAVA, 36000);
            fillTank(world, new BlockPos(-2, Y, 10), net.minecraft.world.level.material.Fluids.WATER, 64000);
            fillTank(world, new BlockPos(2, Y, 10), me.trossty.car.registry.ModFluids.MILK, 30000);
        });
        server.runCommand("setblock 14 %d 4 car:vehicle_lift[level=3]{Stop:2}".formatted(Y));
        context.waitTicks(10);
        shot(context, server, "blocks_row", -0.5, Y + 2.2, 10.0, 180, 22);
        shot(context, server, "blocks_row_left", -7.5, Y + 1.0, 7.5, 180, 15);
        shot(context, server, "blocks_row_right", 4.5, Y + 1.0, 7.5, 180, 15);
        shot(context, server, "blocks_row_far_right", 11.5, Y + 1.6, 9.0, 180, 18);
        shot(context, server, "blocks_tanks", -2.0, Y + 2.5, 17.5, 180, 15);
        shot(context, server, "blocks_tanks_corner", 6.0, Y + 3.5, 16.0, 135, 25);
    }

    private static void placeTank(net.minecraft.server.level.ServerLevel world, BlockPos from, int w, int h, int d) {
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    BlockPos pos = from.offset(x, y, z);
                    world.setBlockAndUpdate(pos, me.trossty.car.registry.ModBlocks.FUEL_TANK.defaultBlockState());
                }
            }
        }
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    BlockPos pos = from.offset(x, y, z);
                    var block = (me.trossty.car.block.FuelTankBlock) me.trossty.car.registry.ModBlocks.FUEL_TANK;
                    var state = world.getBlockState(pos);
                    for (Direction side : Direction.values()) {
                        state = state.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(side),
                                world.getBlockState(pos.relative(side)).is(block));
                    }
                    world.setBlockAndUpdate(pos, state);
                }
            }
        }
    }

    private static void fillTank(net.minecraft.server.level.ServerLevel world, BlockPos pos,
                                 net.minecraft.world.level.material.Fluid fluid, int units) {
        if (world.getBlockEntity(pos) instanceof me.trossty.car.block.entity.FuelTankBlockEntity tank) {
            System.out.println("car photo tank " + pos + ": took " + tank.fill(fluid, units)
                    + " cells=" + tank.getGroup().size());
        }
    }

    private void shot(ClientGameTestContext context, TestServerContext server, String name,
                      double x, double y, double z, float yaw, float pitch) {
        server.runCommand("tp @a %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
        context.waitTicks(12);
        context.takeScreenshot(name);
    }

    private void showroom(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("summon car:car -10.0 %d 10.5 {Rotation:[0f,0f],Color:9}".formatted(Y));
        server.runCommand("summon car:car -5.0 %d 10.5 {Rotation:[0f,0f],Model:\"car:tofas_sahin\",Color:0}".formatted(Y));
        server.runCommand("summon car:car 0.0 %d 10.5 {Rotation:[0f,0f],Model:\"car:jeep_wrangler\",Color:13}".formatted(Y));
        server.runCommand("summon car:pickup 5.0 %d 10.5 {Rotation:[0f,0f],Color:14}".formatted(Y));
        server.runCommand("summon car:fuel_tanker 10.5 %d 10.5 {Rotation:[0f,0f],Color:0}".formatted(Y));
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "showroom_front", 13.0, Y + 3, 21.0, 140, 16);
        shot(context, server, "showroom_back", -13.0, Y + 3, 0.0, -40, 16);
        // Looking down into the cabins from above and behind: the steering
        // wheels should all be on the left-hand side.
        shot(context, server, "cabins_from_above", 0.0, Y + 6, 4.0, 0, 50);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** Each vehicle alone on grass, from the front three-quarter: how it is meant to be seen. */
    private void portraits(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:grass_block".formatted(Y - 1, Y - 1));
        String[][] vehicles = {
                {"car:car", "car:toyota_corolla", "9"},
                {"car:car", "car:tofas_sahin", "0"},
                {"car:car", "car:jeep_wrangler", "5"},
                {"car:car", "car:ford_mustang", "11"},
                {"car:pickup", "car:toyota_hilux", "14"},
                {"car:fuel_tanker", "car:mercedes_atego", "0"},
        };
        context.runOnClient(client -> client.options.hideGui = true);
        for (String[] vehicle : vehicles) {
            clear(server);
            server.runCommand("summon %s 0.5 %d 10.5 {Rotation:[0f,0f],Model:\"%s\",Color:%s}"
                    .formatted(vehicle[0], Y, vehicle[1], vehicle[2]));
            boolean big = vehicle[0].equals("car:fuel_tanker");
            double back = big ? 7.0 : 4.0;
            shot(context, server, "portrait_" + vehicle[1].substring(4),
                    0.5 + back * 0.55, Y + (big ? 1.8 : 1.0), 10.5 + back, 151, 10);
            if (vehicle[1].startsWith("car:ford_")) {
                // the newer ones from behind too: the Mustang's roof line, the dolmuş's doors
                shot(context, server, "rear_" + vehicle[1].substring(4),
                        0.5 - back * 0.55, Y + 1.0, 10.5 - back, -29, 10);
            }
        }
        context.runOnClient(client -> client.options.hideGui = false);
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:stone".formatted(Y - 1, Y - 1));
    }

    /**
     * The vehicle items as the inventory draws them: each one plain (the
     * factory paint) beside the same item dyed, so the icon is seen to wear
     * the car's colour.
     */
    private void icons(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("time set noon");
        String[][] items = {
                {"car:car", "car:toyota_corolla"},
                {"car:car", "car:tofas_sahin"},
                {"car:car", "car:jeep_wrangler"},
                {"car:car", "car:ford_mustang"},
                {"car:pickup", "car:toyota_hilux"},
                {"car:pickup", "car:hilux_tow"},
                {"car:fuel_tanker", "car:mercedes_atego"},
                {"car:motorcycle", "car:vespa_primavera"},
                {"car:motorcycle", "car:honda_cbr600rr"},
                {"car:motorboat", "car:bayliner_bowrider"},
                {"car:airplane", "car:cessna_172"},
                {"car:trailer", "car:box_trailer"},
                {"car:trailer", "car:flatbed_trailer"},
                {"car:trailer", "car:caravan"},
                {"car:trailer", "car:tow_dolly"},
                {"car:trailer", "car:plane_tow_dolly"},
                {"car:trailer", "car:wheel_lift"},
        };
        // Nine to a line, and under each line the same nine dyed.
        int perLine = 9;
        for (int i = 0; i < items.length; i++) {
            double x = -8.0 + (i % perLine) * 2.0;
            for (int row = 0; row < 2; row++) {
                String color = row == 0 ? "" : ",\"car:color\":14";
                server.runCommand(("summon minecraft:item_display %.1f %.1f 12.5 {item_display:\"gui\","
                        + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                        + "translation:[0f,0f,0f],scale:[1.6f,1.6f,1.6f]},"
                        + "item:{id:\"%s\",count:1,components:{\"car:vehicle\":\"%s\"%s}}}")
                        .formatted(x, Y + 6.6 - (i / perLine * 2 + row) * 1.9, items[i][0], items[i][1], color));
            }
        }
        context.runOnClient(client -> client.options.hideGui = true);
        // An item display shows its face to the north.
        shot(context, server, "icons", 0.0, Y + 2.0, 2.0, 0, 0);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** The aircraft parked on grass from all round, and the propeller turning with somebody in it. */
    private void plane(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -16 %d -10 16 %d 30 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("fill -16 %d -10 16 %d 30 minecraft:air".formatted(Y, Y + 8));
        server.runCommand("summon car:airplane 0.5 %d 10.5 {Rotation:[180f,0f]}".formatted(Y));
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "plane_front34", -7.0, Y + 2.2, 2.5, -45, 12);
        shot(context, server, "plane_side", -10.5, Y + 1.0, 10.5, -90, 3);
        shot(context, server, "plane_front", 0.5, Y + 1.2, -1.0, 0, 5);
        shot(context, server, "plane_back34", 6.5, Y + 2.6, 18.5, 140, 14);
        shot(context, server, "plane_top", 0.5, Y + 12.0, 10.5, 0, 90);
        shot(context, server, "plane_tail", -4.5, Y + 2.4, 14.6, -90, 4);
        shot(context, server, "plane_cabin", -3.6, Y + 1.6, 8.6, -90, 6);
        shot(context, server, "plane_step", -2.6, Y + 0.6, 7.2, -60, 12);
        // People in the seats, seen from outside: two husks up front, then
        // the pilot's own view over the panel.
        server.runCommand("difficulty easy");
        for (int i = 0; i < 2; i++) {
            server.runCommand("summon husk 0.5 %d 14.5 {NoAI:1b,Silent:1b,Tags:[\"crew\"]}".formatted(Y));
            server.runCommand("ride @e[tag=crew,limit=1,sort=nearest] mount @e[type=car:airplane,limit=1]");
            server.runCommand("tag @e[tag=crew] remove crew");
        }
        shot(context, server, "plane_crew_side", -6.0, Y + 1.6, 9.2, -90, 8);
        shot(context, server, "plane_crew_front", 0.5, Y + 2.4, 2.5, 0, 14);
        shot(context, server, "plane_crew_back34", -4.0, Y + 2.6, 13.5, -130, 16);
        server.runCommand("kill @e[type=husk]");
        server.runCommand("difficulty peaceful");
        server.runCommand("tp @a 3.5 %d 10.5".formatted(Y));
        context.waitTicks(5);
        server.runCommand("ride @p mount @e[type=car:airplane,limit=1]");
        context.waitTicks(10);
        context.runOnClient(client -> {
            client.player.setYRot(180);
            client.player.setXRot(10);
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });
        context.waitTicks(5);
        context.takeScreenshot("plane_cockpit");
        server.runCommand("ride @p dismount");
        context.waitTicks(5);
        context.runOnClient(client -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.options.hideGui = false;
        });
    }

    private void tyres(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        // road, dirt, snow, sport
        for (int i = 0; i < 4; i++) {
            server.runCommand("summon car:car %d.0 %d 10.5 {Rotation:[0f,0f],Color:8,Setup:%d}"
                    .formatted(-9 + i * 6, Y, i << 3));
        }
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "tyres_side", 0.0, Y + 1.0, 18.5, 180, 8);
        for (int i = 0; i < 4; i++) {
            shot(context, server, "tyre_" + i, -9 + i * 6 + 2.6, Y + 0.6, 12.2, 90, 12);
        }
        context.runOnClient(client -> client.options.hideGui = false);
    }

    private void station(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        int y = Y;
        // refinery -> straight run -> bend -> tank -> run -> pump; generator beside
        server.runCommand("setblock -6 %d 20 car:fuel_refinery[facing=south]".formatted(y));
        server.runCommand("setblock -8 %d 20 car:generator[facing=south]".formatted(y));
        server.runCommand("fill -5 %d 20 -2 %d 20 car:fuel_pipe".formatted(y, y));
        server.runCommand("fill -2 %d 21 -2 %d 22 car:fuel_pipe".formatted(y, y));
        server.runCommand("setblock -2 %d 23 car:fuel_tank".formatted(y));
        server.runCommand("fill -2 %d 24 -2 %d 25 car:fuel_pipe".formatted(y, y));
        server.runCommand("fill -1 %d 25 1 %d 25 car:fuel_pipe".formatted(y, y));
        server.runCommand("setblock 2 %d 25 car:fuel_pump[facing=south,half=lower]".formatted(y));
        server.runCommand("setblock 2 %d 25 car:fuel_pump[facing=south,half=upper]".formatted(y + 1));
        // a riser, and a pipe beside a cauldron (another fluid holder)
        server.runCommand("fill 4 %d 20 4 %d 20 car:fuel_pipe".formatted(y, y + 2));
        server.runCommand("setblock 5 %d 20 minecraft:cauldron".formatted(y));
        server.runCommand("setblock 4 %d 21 car:fuel_pipe".formatted(y));
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "station_overview", -1.0, y + 4.5, 31.0, 180, 30);
        shot(context, server, "station_pump", 4.5, y + 1.6, 29.0, 150, 10);
        shot(context, server, "station_refinery", -6.5, y + 1.8, 25.0, 180, 15);
        shot(context, server, "station_pipes", 6.5, y + 2.5, 25.5, 145, 25);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    private void driving(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:gravel".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 0.5 %d -6.5 {Rotation:[0f,0f],Color:8,Tags:[\"photo\"]}".formatted(Y));
        context.waitTicks(5);
        server.runCommand("ride @p mount @e[tag=photo,limit=1]");
        context.waitTicks(5);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            client.player.setXRot(20);
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });
        context.waitTicks(5);
        context.takeScreenshot("driver_seat");
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(25);
        context.takeScreenshot("dash_accelerating");
        context.waitTicks(25);
        context.takeScreenshot("dash_faster");
        context.getInput().releaseKey(options -> options.keyUp);
    }

    /**
     * An electric car's battery gauge in the fuel dial's place: full, over
     * half, low (amber), nearly flat (red), and on charge, with the bolt lit.
     * The last one again in Turkish, where the per cent sign comes first.
     */
    private void batteryGauge(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:gravel".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 0.5 %d -6.5 {Rotation:[0f,0f],Color:3,Tags:[\"photo\"]}".formatted(Y));
        context.waitTicks(5);
        server.runOnServer(minecraft -> photoCar(minecraft).setSetup(me.trossty.car.entity.CarSetup.withVariant(
                me.trossty.car.entity.CarSetup.DEFAULT, me.trossty.car.entity.CarPart.ENGINE,
                me.trossty.car.entity.CarSetup.ELECTRIC_MOTOR)));
        server.runCommand("ride @p mount @e[tag=photo,limit=1]");
        context.waitTicks(5);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            client.player.setXRot(20);
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });
        float[] levels = {1.0F, 0.64F, 0.18F, 0.07F};
        String[] names = {"full", "64", "low", "flat"};
        for (int i = 0; i < levels.length; i++) {
            float level = levels[i];
            server.runOnServer(minecraft -> {
                var car = photoCar(minecraft);
                car.setFuel(car.getMaxFuel() * level);
            });
            context.waitTicks(30);
            context.takeScreenshot("battery_" + names[i]);
        }
        // A post's worth of charge each tick, with the car standing still.
        server.runOnServer(minecraft -> {
            var car = photoCar(minecraft);
            car.setFuel(car.getMaxFuel() * 0.45F);
        });
        for (int i = 0; i < 30; i++) {
            server.runOnServer(minecraft -> photoCar(minecraft).charge(0.02F));
            context.waitTicks(1);
        }
        context.takeScreenshot("battery_charging");
        context.runOnClient(client -> {
            client.getLanguageManager().setSelected("tr_tr");
            client.reloadResourcePacks();
        });
        for (int i = 0; i < 80; i++) {
            server.runOnServer(minecraft -> photoCar(minecraft).charge(0.02F));
            context.waitTicks(1);
        }
        context.takeScreenshot("battery_charging_tr");
        context.runOnClient(client -> {
            client.getLanguageManager().setSelected("en_us");
            client.reloadResourcePacks();
        });
        context.waitTicks(60);
    }

    private static me.trossty.car.entity.CarEntity photoCar(net.minecraft.server.MinecraftServer minecraft) {
        var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
        return minecraft.overworld().getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box,
                e -> e.getTags().contains("photo")).get(0);
    }
    /**
     * Driving across a flat field and getting out: the car has to stay on the
     * ground, with no hop, and the driver has to end up beside it, on their feet.
     */
    private void rideAndLeave(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:air".formatted(Y, Y + 8));
        // A dirt path and a row of slabs on the way.
        server.runCommand("fill -3 %d 2 3 %d 5 minecraft:dirt_path".formatted(Y - 1, Y - 1));
        server.runCommand("fill -3 %d 10 3 %d 10 minecraft:oak_slab".formatted(Y, Y));
        // Then a hill the way the land is: a block up, a block up again, and
        // down the far side a block at a time.
        server.runCommand("fill -4 %d 16 4 %d 34 minecraft:grass_block".formatted(Y, Y));
        server.runCommand("fill -4 %d 21 4 %d 28 minecraft:grass_block".formatted(Y + 1, Y + 1));
        server.runCommand("fill -4 %d 34 4 %d 40 minecraft:air".formatted(Y, Y));
        server.runCommand("summon car:car 0.5 %d -6.5 {Rotation:[0f,0f],Color:14,Tags:[\"photo\"]}".formatted(Y));
        context.waitTicks(5);
        server.runCommand("ride @p mount @e[tag=photo,limit=1]");
        context.waitTicks(5);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            client.player.setXRot(15);
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        context.getInput().holdKey(options -> options.keyUp);
        StringBuilder heights = new StringBuilder("car photo ride heights:");
        for (int tick = 0; tick < 90; tick++) {
            context.waitTicks(1);
            heights.append(context.<String>computeOnClient(client -> {
                var car = client.player.getVehicle();
                return !(car instanceof me.trossty.car.entity.CarEntity driven) ? " -" : " %.1f:%.2f>%.2f%s".formatted(car.getZ(), car.getY() - Y,
                        car.getY() - Y + driven.getStepOffset(1.0F), car.onGround() ? "" : "*");
            }));
            if (tick == 35) {
                context.takeScreenshot("ride_driving");
            }
        }
        context.getInput().releaseKey(options -> options.keyUp);
        System.out.println(heights);
        context.waitTicks(40);
        // The odometer counts the blocks driven, and the client sees it too.
        context.runOnClient(client -> {
            var car = (me.trossty.car.entity.CarEntity) client.player.getVehicle();
            System.out.println("car photo ride odometer: %.1f blocks, z %.1f".formatted(car.parts().getOdometer(), car.getZ() + 6.5));
        });
        context.getInput().pressKey(options -> options.keyShift);
        // Where the car is drawn after getting out: it has to stay put, not
        // jump back to where it was boarded.
        StringBuilder after = new StringBuilder("car photo ride car after leaving:");
        for (int tick = 0; tick < 30; tick++) {
            context.waitTicks(1);
            after.append(context.<String>computeOnClient(client -> client.level.getEntitiesOfClass(
                            me.trossty.car.entity.CarEntity.class,
                            new net.minecraft.world.phys.AABB(-40, Y - 10, -40, 40, Y + 10, 200), e -> true)
                    .stream().map(car -> " %.2f".formatted(car.getZ())).findFirst().orElse(" -")));
        }
        System.out.println(after);
        context.runOnClient(client -> {
            var player = client.player;
            System.out.println("car photo ride left: riding=" + player.isPassenger()
                    + " at " + player.position() + " onGround=" + player.onGround()
                    + " pose=" + player.getPose());
        });
        server.runOnServer(minecraft -> {
            var player = minecraft.getPlayerList().getPlayers().get(0);
            System.out.println("car photo ride left (server): riding=" + player.isPassenger()
                    + " at " + player.position());
        });
        context.takeScreenshot("ride_left");
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
    }

    /** The scooter, the sports bike and the trailer, alone on grass. */
    private void twoWheelsAndTrailer(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("fill -12 %d -10 12 %d 90 minecraft:grass_block".formatted(Y - 1, Y - 1));
        String[][] vehicles = {
                {"car:motorcycle", "car:vespa_primavera"},
                {"car:motorcycle", "car:honda_cbr600rr"},
                {"car:trailer", "car:box_trailer"},
        };
        context.runOnClient(client -> client.options.hideGui = true);
        for (String[] vehicle : vehicles) {
            clear(server);
            server.runCommand("summon %s 0.5 %d 10.5 {Rotation:[0f,0f],Model:\"%s\"}"
                    .formatted(vehicle[0], Y, vehicle[1]));
            shot(context, server, "portrait_" + vehicle[1].substring(4), 2.3, Y + 0.8, 13.6, 151, 12);
            shot(context, server, "side_" + vehicle[1].substring(4), 4.2, Y + 0.6, 10.5, 90, 6);
        }
        context.runOnClient(client -> client.options.hideGui = false);
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:stone".formatted(Y - 1, Y - 1));
    }

    /** The workstation's window with a Mustang picked, painted blue, and only some of what it takes. */
    private void workstation(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("setblock 0 %d 14 car:vehicle_workstation[facing=north]".formatted(Y));
        // Within reach of it, or the menu closes as soon as it opens.
        server.runCommand("tp @a 0.5 %d 11.5 0 30".formatted(Y));
        server.runCommand("clear @a");
        server.runCommand("give @a minecraft:iron_block 2");
        server.runCommand("give @a minecraft:iron_ingot 5");
        server.runCommand("give @a car:engine 1");
        server.runCommand("give @a car:car_body 1");
        server.runCommand("gamemode survival @a");
        server.runOnServer(minecraft -> {
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var world = player.level();
            var pos = new net.minecraft.core.BlockPos(0, Y, 14);
            player.openMenu(world.getBlockState(pos).getMenuProvider(world, pos));
            player.containerMenu.clickMenuButton(player, 1);
            player.containerMenu.clickMenuButton(player, 901 + 11);
            player.containerMenu.broadcastChanges();
        });
        context.waitTicks(30);
        context.takeScreenshot("workstation_screen");
        context.runOnClient(client -> client.setScreen(null));
        server.runCommand("gamemode creative @a");
        server.runCommand("clear @a");
    }

    /**
     * Flat bed trailers: one on a pickup's hitch with a Mustang winched on,
     * one with a boat, one with an engine; then one walked by its drawbar and
     * dropped on a car's tow ball. Logs how much each load takes off the pull.
     */
    private void flatbed(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("clear @a");
        server.runCommand("fill -25 %d -20 30 %d 40 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 18.5 %d 14.5 {Rotation:[0f,0f],Model:\"car:jeep_wrangler\",Color:11,Tags:[\"tractor2\"]}".formatted(Y));
        server.runCommand("summon car:trailer 18.5 %d 9.1 {Rotation:[0f,0f],Model:\"car:tow_dolly\",Tags:[\"bed_dolly\"]}".formatted(Y));
        server.runCommand("summon car:car 18.5 %d 3.0 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:3,Tags:[\"load\"]}".formatted(Y));
        server.runCommand("summon car:pickup 0.5 %d 7.5 {Rotation:[0f,0f],Color:14,Tags:[\"tractor\"]}".formatted(Y));
        String bed = "summon car:trailer %.1f %d %.1f {Rotation:[0f,0f],Model:\"car:flatbed_trailer\",Tags:[\"%s\"]}";
        server.runCommand(bed.formatted(0.5, Y, -1.5, "bed_car"));
        server.runCommand(bed.formatted(8.5, Y, -1.5, "bed_boat"));
        server.runCommand(bed.formatted(-7.5, Y, -1.5, "bed_engine"));
        server.runCommand(bed.formatted(-7.5, Y, 14.5, "bed_walked"));
        server.runCommand(bed.formatted(-15.5, Y, -1.5, "bed_bike"));
        server.runCommand("summon car:motorcycle -15.5 %d -7.5 {Rotation:[0f,0f],Model:\"car:honda_cbr600rr\",Tags:[\"load\"]}".formatted(Y));
        server.runCommand("summon car:car 0.5 %d -9.5 {Rotation:[0f,0f],Model:\"car:ford_mustang\",Tags:[\"load\"]}".formatted(Y));
        server.runCommand("summon car:motorboat 8.5 %d -11.5 {Rotation:[0f,0f],Tags:[\"load\"]}".formatted(Y));
        server.runCommand("summon car:car -1.5 %d 19.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Tags:[\"walk_to\"]}".formatted(Y));
        context.waitTicks(10);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            java.util.function.Function<String, me.trossty.car.entity.TrailerEntity> bedOf = tag -> world
                    .getEntitiesOfClass(me.trossty.car.entity.TrailerEntity.class, box, e -> e.getTags().contains(tag)).get(0);
            var tractor = world.getEntitiesOfClass(me.trossty.car.entity.PickupEntity.class, box, e -> true).get(0);
            var carBed = bedOf.apply("bed_car");
            carBed.hitch(tractor);
            bedOf.apply("bed_dolly").hitch(world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box,
                    e -> e.getTags().contains("tractor2")).get(0));
            System.out.println("car photo flatbed: alone pull=" + tractor.throttleStep() + " top=" + tractor.getMaxSpeed());
            player.setShiftKeyDown(true);
            for (String tag : new String[] {"bed_car", "bed_boat", "bed_bike", "bed_dolly"}) {
                var trailer = bedOf.apply(tag);
                player.moveTo(trailer.getX() + 2.0, trailer.getY(), trailer.getZ());
                System.out.println("car photo flatbed: winch " + tag + " -> "
                        + trailer.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND)
                        + " cargo=" + trailer.hasCargo() + " towed=" + trailer.towedMass());
            }
            player.setShiftKeyDown(false);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.ENGINE));
            System.out.println("car photo flatbed: engine -> " + bedOf.apply("bed_engine")
                    .interact(player, net.minecraft.world.InteractionHand.MAIN_HAND));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            var walked = bedOf.apply("bed_walked");
            player.moveTo(-7.5, Y, 19.0, 180.0F, 0.0F);
            System.out.println("car photo flatbed: grab -> "
                    + walked.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND)
                    + " held=" + (me.trossty.car.entity.TrailerEntity.heldBy(player) == walked));
        });
        // Walk the drawbar across behind the Corolla, then click the car.
        server.runCommand("tp @p -1.5 %d 15.6 0 0".formatted(Y));
        context.waitTicks(10);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            var corolla = world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box,
                    e -> e.getTags().contains("walk_to")).get(0);
            System.out.println("car photo flatbed: hitch by hand -> "
                    + corolla.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND)
                    + " held=" + me.trossty.car.entity.TrailerEntity.heldBy(player));
        });
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            for (var car : world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box,
                    e -> e.getTags().contains("tractor") || e.getTags().contains("walk_to")
                            || e.getTags().contains("tractor2"))) {
                System.out.println("car photo flatbed: " + car.getTags() + " towing=" + car.isTowing(null)
                        + " ratio=" + car.towRatio() + " pull=" + car.throttleStep() + " top=" + car.getMaxSpeed());
            }
        });
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "flatbed_rear", 13.0, Y + 4.0, -19.0, 34, 14);
        shot(context, server, "flatbed_front", -9.0, Y + 3.0, 9.0, 218, 14);
        shot(context, server, "flatbed_walked", -10.5, Y + 2.5, 22.5, 210, 14);
        shot(context, server, "flatbed_car_side", 7.0, Y + 2.2, -1.5, 90, 12);
        shot(context, server, "flatbed_boat_side", 15.5, Y + 2.4, -1.5, 90, 12);
        shot(context, server, "flatbed_bike_side", -19.0, Y + 1.8, -1.5, 270, 12);
        shot(context, server, "dolly_side", 25.5, Y + 1.8, 8.0, 90, 8);
        shot(context, server, "dolly_rear34", 24.0, Y + 3.0, -2.0, 30, 15);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** A pickup pulling a Cessna by its nose wheel on the aircraft tow dolly. */
    private void planeTow(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("clear @a");
        server.runCommand("fill -20 %d -30 20 %d 30 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:pickup 0.5 %d 10.5 {Rotation:[0f,0f],Color:0,Tags:[\"tractor\"]}".formatted(Y));
        server.runCommand("summon car:trailer 0.5 %d 5.0 {Rotation:[0f,0f],Model:\"car:plane_tow_dolly\",Color:4,Tags:[\"tug\"]}".formatted(Y));
        server.runCommand("summon car:airplane 0.5 %d -2.0 {Rotation:[0f,0f],Tags:[\"load\"]}".formatted(Y));
        context.waitTicks(10);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -40, 30, Y + 10, 40);
            var tractor = world.getEntitiesOfClass(me.trossty.car.entity.PickupEntity.class, box, e -> true).get(0);
            var tug = world.getEntitiesOfClass(me.trossty.car.entity.TrailerEntity.class, box,
                    e -> e.getTags().contains("tug")).get(0);
            tug.hitch(tractor);
            player.setShiftKeyDown(true);
            player.moveTo(tug.getX() + 2.0, tug.getY(), tug.getZ());
            System.out.println("car photo plane tow: winch -> "
                    + tug.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND)
                    + " cargo=" + tug.hasCargo() + " towed=" + tug.towedMass());
            player.setShiftKeyDown(false);
        });
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -40, 30, Y + 10, 40);
            var tractor = minecraft.overworld().getEntitiesOfClass(me.trossty.car.entity.PickupEntity.class, box,
                    e -> true).get(0);
            System.out.println("car photo plane tow: ratio=" + tractor.towRatio() + " pull=" + tractor.throttleStep()
                    + " top=" + tractor.getMaxSpeed());
        });
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "plane_tow_side", 15.0, Y + 3.0, 2.0, 90, 12);
        shot(context, server, "plane_tow_rear34", 12.0, Y + 5.0, -14.0, 40, 18);
        shot(context, server, "plane_tow_cradle", 3.5, Y + 1.6, 7.5, 125, 25);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /**
     * A boat let down off a flatbed on to its cradle and a plane jacked up off
     * its wheels: the two vehicles no lift takes, up where they can be worked on.
     */
    private void stands(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("clear @a");
        server.runCommand("fill -24 %d -30 24 %d 30 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:trailer -6.5 %d 8.0 {Rotation:[0f,0f],Model:\"car:flatbed_trailer\",Tags:[\"bed\"]}".formatted(Y));
        server.runCommand("summon car:motorboat -3.5 %d 1.0 {Rotation:[0f,0f],Tags:[\"load\"]}".formatted(Y));
        server.runCommand("setblock -7 %d 0 car:boat_cradle[facing=south]".formatted(Y));
        server.runCommand("setblock -13 %d 6 car:boat_cradle[facing=south]".formatted(Y));
        server.runCommand("setblock 8 %d -1 car:aircraft_jack[raised=true]".formatted(Y));
        server.runCommand("setblock 14 %d 8 car:aircraft_jack".formatted(Y));
        server.runCommand("summon car:airplane 8.5 %d -2.1 {Rotation:[0f,0f],Tags:[\"jacked\"]}".formatted(Y));
        context.waitTicks(10);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -40, 30, Y + 10, 40);
            var bed = world.getEntitiesOfClass(me.trossty.car.entity.TrailerEntity.class, box,
                    e -> e.getTags().contains("bed")).get(0);
            player.setShiftKeyDown(true);
            player.moveTo(bed.getX() + 2.0, bed.getY(), bed.getZ());
            bed.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            System.out.println("car photo stands: boat winched on=" + bed.hasCargo());
            bed.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            System.out.println("car photo stands: boat let down, on bed=" + bed.hasCargo());
            player.setShiftKeyDown(false);
        });
        context.waitTicks(10);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -40, 30, Y + 10, 40);
            var boat = world.getEntitiesOfClass(me.trossty.car.entity.MotorboatEntity.class, box, e -> true).get(0);
            var plane = world.getEntitiesOfClass(me.trossty.car.entity.AirplaneEntity.class, box, e -> true).get(0);
            System.out.println("car photo stands: boat at " + boat.position() + " servicing="
                    + me.trossty.car.block.VehicleLiftBlock.isServicing(world, boat));
            System.out.println("car photo stands: plane y=" + (plane.getY() - Y) + " servicing="
                    + me.trossty.car.block.VehicleLiftBlock.isServicing(world, plane));
        });
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "stands_boat", -1.5, Y + 2.2, -3.5, 48, 15);
        shot(context, server, "stands_plane_side", 17.0, Y + 1.5, 0.0, 90, 5);
        shot(context, server, "stands_overview", 2.0, Y + 7.0, 16.0, 180, 30);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** What is in a pickup's bed and a box trailer, drawn sitting in them. */
    private void bedLoad(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -20 20 %d 20 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:pickup 0.5 %d 0.5 {Rotation:[0f,0f],Color:11}".formatted(Y));
        server.runCommand("summon car:trailer 4.5 %d 0.5 {Rotation:[0f,0f],Model:\"car:box_trailer\"}".formatted(Y));
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 30);
            var pickup = world.getEntitiesOfClass(me.trossty.car.entity.PickupEntity.class, box, e -> true).get(0);
            var trailer = world.getEntitiesOfClass(me.trossty.car.entity.TrailerEntity.class, box, e -> true).get(0);
            net.minecraft.world.item.Item[] things = {
                    net.minecraft.world.item.Items.OAK_LOG, net.minecraft.world.item.Items.HAY_BLOCK,
                    net.minecraft.world.item.Items.IRON_INGOT, net.minecraft.world.item.Items.CHEST,
                    net.minecraft.world.item.Items.APPLE, net.minecraft.world.item.Items.COBBLESTONE,
                    net.minecraft.world.item.Items.DIAMOND_PICKAXE, net.minecraft.world.item.Items.PUMPKIN,
                    net.minecraft.world.item.Items.BREAD};
            for (int i = 0; i < things.length; i++) {
                pickup.getBed().setItem(i, new net.minecraft.world.item.ItemStack(things[i], 16));
            }
            for (int i = 0; i < 5; i++) {
                trailer.getLoad().setItem(i, new net.minecraft.world.item.ItemStack(things[i * 2 % things.length], 8));
            }
            System.out.println("car photo bedLoad: pickup shows " + pickup.shownLoad().size()
                    + ", trailer shows " + trailer.shownLoad().size());
        });
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "bed_load_above", 2.5, Y + 5.0, -6.0, 0, 40);
        shot(context, server, "bed_load_side", 9.0, Y + 2.6, -3.0, 60, 22);
        shot(context, server, "bed_load_trailer", 4.5, Y + 6.0, -2.0, 0, 55);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** A Corolla on a flat front tyre, and one with two tyres taken off. */
    /** Bonnets and boot lids up: a Corolla, a Mustang, a Hilux. */
    private void lids(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -20 20 %d 20 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 0.5 %d 0.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:3,Lids:3,Boot:{Items:[{Slot:0b,id:\"minecraft:chest\",count:1},{Slot:1b,id:\"minecraft:bread\",count:4},{Slot:2b,id:\"minecraft:oak_log\",count:8},{Slot:3b,id:\"car:jerry_can\",count:1},{Slot:4b,id:\"minecraft:apple\",count:3}]}}".formatted(Y));
        server.runCommand("summon car:car 0.5 %d 8.5 {Rotation:[0f,0f],Model:\"car:ford_mustang\",Color:14,Lids:3}".formatted(Y));
        server.runCommand("summon car:pickup 0.5 %d -8.5 {Rotation:[0f,0f],Model:\"car:toyota_hilux\",Color:11,Lids:1}".formatted(Y));
        server.runCommand("summon car:car 6.5 %d 0.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:1,Boot:{Items:[{Slot:0b,id:\"minecraft:chest\",count:1},{Slot:1b,id:\"minecraft:oak_log\",count:8}]}}".formatted(Y));
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(20);
        shot(context, server, "lids_side", -6.5, Y + 1.6, 0.5, -90, 8);
        shot(context, server, "lids_corner", -4.5, Y + 2.2, 4.5, -135, 14);
        shot(context, server, "lids_rear", -3.5, Y + 2.2, -3.5, -40, 14);
        shot(context, server, "lids_bay", 0.5, Y + 3.0, 6.0, 180, 26);
        shot(context, server, "lids_boot", 0.5, Y + 3.0, -5.0, 0, 26);
        shot(context, server, "lids_closed", 10.5, Y + 2.2, 4.5, 130, 20);
        shot(context, server, "lids_top", 0.5, Y + 3.4, 3.3, 180, 70);
        shot(context, server, "lids_boot_top", 0.5, Y + 3.2, -1.7, 0, 72);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** A Corolla on the ground, bonnet up, an engine hoist pushed up to its nose. */
    private void hoist(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -20 20 %d 20 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 0.5 %d 0.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:3,Lids:1}".formatted(Y));
        server.runCommand("setblock 0 %d 3 car:engine_hoist[facing=north]".formatted(Y));
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(20);
        shot(context, server, "hoist_corner", -3.5, Y + 2.0, 5.5, -145, 18);
        shot(context, server, "hoist_side", -4.5, Y + 1.2, 2.5, -90, 8);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** A car on the dyno, a drain pan, a toolbox and an engine opened up on its stand. */
    private void garageTools(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -20 20 %d 20 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("setblock 0 %d 0 car:dyno".formatted(Y - 1));
        server.runCommand("setblock 0 %d 1 car:dyno".formatted(Y - 1));
        server.runCommand("summon car:car 0.5 %d 0.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:3}".formatted(Y));
        server.runCommand("setblock 3 %d 0 car:drain_pan".formatted(Y));
        server.runCommand("setblock 3 %d 2 car:toolbox[facing=east]".formatted(Y));
        server.runCommand("setblock 3 %d -2 car:engine_stand[facing=east]".formatted(Y));
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var pos = new net.minecraft.core.BlockPos(3, Y, -2);
            if (world.getBlockEntity(pos) instanceof me.trossty.car.block.entity.EngineStandBlockEntity stand) {
                stand.setEngine(new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.ENGINE));
                stand.strip();
            }
        });
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(20);
        shot(context, server, "garage_tools", 7.5, Y + 2.2, 0.5, 90, 18);
        shot(context, server, "garage_tools_close", 5.5, Y + 1.4, -1.5, 70, 25);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /**
     * The engine's insides in game: a worn engine opened up on its stand, its
     * four insides out, the valves up close and in the hand, the cylinder by
     * cylinder tooltip, and a compression test on a car whose belt has
     * snapped, read off in chat.
     */
    private void engineInternals(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -20 20 %d 20 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("clear @a");
        server.runCommand("setblock 0 %d 4 car:engine_stand[facing=south]".formatted(Y));
        String[] parts = {"pistons", "valves", "head_gasket", "crankshaft"};
        for (int i = 0; i < parts.length; i++) {
            server.runCommand(("summon item_display %.2f %.2f 4.5 {item:{id:\"car:%s\",count:1},"
                    + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                    + "translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}").formatted(2.6 + i * 0.85, Y + 0.45, parts[i]));
        }
        server.runCommand("summon car:car 0.5 %d -3.0 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:3}".formatted(Y));
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var player = minecraft.getPlayerList().getPlayers().get(0);
            if (world.getBlockEntity(new BlockPos(0, Y, 4)) instanceof me.trossty.car.block.entity.EngineStandBlockEntity stand) {
                // Worn unevenly: cylinder 2's rings tired, 1 and 3's valves bent by a belt.
                var engine = new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.ENGINE);
                var inside = new me.trossty.car.entity.EngineInternals.Snapshot(
                        List.of(80, 35, 75, 70), List.of(0, 90, 0, 85), 60, 45);
                engine.set(me.trossty.car.registry.ModComponents.PART_CONDITION, Math.round(inside.condition()));
                inside.stamp(engine);
                stand.setEngine(engine);
                stand.strip();
                for (int i = 0; i < stand.getInternals().size(); i++) {
                    player.getInventory().setItem(i, stand.getInternals().get(i).copy());
                }
            }
            player.getInventory().setItem(4, new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.COMPRESSION_TESTER));

            var car = world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class,
                    new net.minecraft.world.phys.AABB(-10, Y - 5, -10, 10, Y + 5, 10), e -> true).get(0);
            float speed = car.getMaxSpeed();
            car.setLidOpen(me.trossty.car.entity.CarEntity.BONNET_BIT, true);
            car.upkeep().snapBelt();
            car.parts().setCondition(me.trossty.car.entity.CarPart.SPARK_PLUGS, 0.0F);
            var reading = me.trossty.car.item.CompressionTesterItem.read(car);
            System.out.println("car photo internals: verdict=" + reading.verdict()
                    + " spec=" + reading.spec() + " bar=" + java.util.Arrays.toString(reading.cylinders())
                    + " firing=" + car.parts().firing() + " maxSpeed " + speed + " -> " + car.getMaxSpeed()
                    + " engine=" + car.parts().getCondition(me.trossty.car.entity.CarPart.ENGINE)
                    + " burningOil=" + car.upkeep().isBurningOil() + " gasket=" + car.upkeep().isGasketBlown()
                    + " knocking=" + car.upkeep().isKnocking());
            me.trossty.car.item.CompressionTesterItem.test(car, player);
        });
        context.waitTicks(5);
        // What the player would read on each of the insides; the valves in hand
        // (the slot held is the client's to pick).
        context.runOnClient(client -> {
            client.player.getInventory().selected = 1;
            for (int i = 0; i < 4; i++) {
                var stack = client.player.getInventory().getItem(i);
                var lines = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level),
                        client.player, net.minecraft.world.item.TooltipFlag.NORMAL);
                System.out.println("car photo internals tooltip " + i + ": "
                        + lines.stream().map(net.minecraft.network.chat.Component::getString)
                        .collect(java.util.stream.Collectors.joining(" | ")));
            }
        });
        // The chat first, before it fades: the test was read off just now.
        shot(context, server, "internals_compression", 3.5, Y + 2.0, 0.5, 140, 25);
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "internals_stand", 0.5, Y + 1.5, 6.6, 180, 25);
        shot(context, server, "internals_parts", 3.85, Y + 1.15, 6.0, 180, 30);
        context.runOnClient(client -> client.options.hideGui = false);
        shot(context, server, "internals_hand", 6.0, Y + 1.0, 8.0, 135, 10);
        // Each set's tooltip, the pointer held over it in the inventory: the
        // survival one, as creative swaps in its own.
        server.runCommand("gamemode survival @a");
        context.waitTicks(3);
        context.runOnClient(client -> client.setScreen(
                new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player)));
        context.waitTicks(3);
        for (int i = 0; i < 2; i++) {
            int slot = i;
            context.runOnClient(client -> {
                var screen = (net.minecraft.client.gui.screens.inventory.InventoryScreen) client.screen;
                var window = client.getWindow();
                double x = (screen.getGuiLeft() + 8 + slot * 18 + 8) * (double) window.getScreenWidth() / window.getGuiScaledWidth();
                double y = (screen.getGuiTop() + 142 + 8) * (double) window.getScreenHeight() / window.getGuiScaledHeight();
                try {
                    for (String[] axis : new String[][]{{"xpos", String.valueOf(x)}, {"ypos", String.valueOf(y)}}) {
                        net.neoforged.fml.util.ObfuscationReflectionHelper.findField(
                                net.minecraft.client.MouseHandler.class, axis[0])
                                .setDouble(client.mouseHandler, Double.parseDouble(axis[1]));
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            });
            context.waitTicks(4);
            context.takeScreenshot("internals_tooltip_" + parts[i]);
        }
        context.runOnClient(client -> client.setScreen(null));
        server.runCommand("gamemode creative @a");
    }

    /**
     * The new parts laid out on the floor, a bare dyno, and a scanner plugged
     * into a car with three faults pointing them out.
     */
    private void garageParts(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -20 20 %d 20 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        String[] parts = {"pistons", "crankshaft", "head_gasket", "timing_belt", "catalytic_converter", "tyre_pump", "compression_tester", "seismic_survey"};
        for (int i = 0; i < parts.length; i++) {
            server.runCommand(("summon item_display -6.5 %.2f %.2f {item:{id:\"car:%s\",count:1},"
                    + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                    + "translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}").formatted(Y + 0.45, -3.3 + i * 0.85, parts[i]));
        }
        server.runCommand("setblock 6 %d 0 car:dyno".formatted(Y));
        server.runCommand("summon car:car 0.5 %d 0.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:3}".formatted(Y));
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var car = world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class,
                    new net.minecraft.world.phys.AABB(-10, Y - 5, -10, 10, Y + 5, 10), e -> true).get(0);
            car.setLidOpen(me.trossty.car.entity.CarEntity.BONNET_BIT, true);
            car.upkeep().setBeltWear(0.9F);
            car.upkeep().setCatClog(0.9F);
            car.upkeep().setTyrePressure(0.3F);
            var scanner = new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.OBD_SCANNER);
            scanner.set(me.trossty.car.registry.ModComponents.OBD_LINK, car.getUUID());
            minecraft.getPlayerList().getPlayers().get(0).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, scanner);
        });
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "garage_parts", -9.0, Y + 1.3, -0.25, -90, 18);
        shot(context, server, "garage_dyno", 6.5, Y + 0.2, -2.2, 0, 50);
        context.runOnClient(client -> client.options.hideGui = false);
        shot(context, server, "garage_obd_front", 0.5, Y + 2.2, 5.0, 180, 25);
        shot(context, server, "garage_obd_rear", 3.5, Y + 1.4, -4.0, 40, 15);
    }

    /** Every item of the mod as it shows in an inventory, a page of 40 at a time on a plain wall. */
    private void itemGrid(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (var key : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
            if (key.getNamespace().equals("car")) {
                ids.add(key.getPath());
            }
        }
        java.util.Collections.sort(ids);
        System.out.println("car photo itemGrid: " + ids);
        server.runCommand("time set noon");
        server.runCommand("fill -8 %d 6 8 %d 6 minecraft:light_gray_concrete".formatted(Y - 1, Y + 8));
        context.runOnClient(client -> client.options.hideGui = true);
        int perPage = 40;
        for (int page = 0; page * perPage < ids.size(); page++) {
            server.runCommand("kill @e[type=item_display]");
            for (int i = 0; i < perPage && page * perPage + i < ids.size(); i++) {
                double x = 3.5 - (i % 8);
                double y = Y + 4.6 - (i / 8) * 1.05;
                server.runCommand(("summon item_display %.2f %.2f 5.4 {item:{id:\"car:%s\",count:1},item_display:\"gui\","
                        + "Rotation:[0f,0f],brightness:{sky:15,block:15},"
                        + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                        + "translation:[0f,0f,0f],scale:[0.9f,0.9f,0.9f]}}").formatted(x, y, ids.get(page * perPage + i)));
            }
            context.waitTicks(10);
            shot(context, server, "items_" + page, 0.0, Y + 1.0, -1.4, 0, 0);
        }
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** A rapeseed field at each stage of growth, sprout to ripe pods, with the seed beside it. */
    private void rapeseed(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -12 12 %d 12 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("fill -4 %d -3 3 %d 3 minecraft:farmland[moisture=7]".formatted(Y - 1, Y - 1));
        int[] ages = {0, 2, 4, 5, 6, 7, 7, 7};
        for (int x = -4; x <= 3; x++) {
            server.runCommand("fill %d %d -3 %d %d 3 car:rapeseed_crop[age=%d]".formatted(x, Y, x, Y, ages[x + 4]));
        }
        server.runCommand(("summon item_display -5.5 %.2f 0.5 {item:{id:\"car:rapeseed\",count:1},"
                + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                + "translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}").formatted(Y + 0.45));
        server.runCommand("time set noon");
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(20);
        shot(context, server, "rapeseed_field", 0.0, Y + 2.6, 7.5, 180, 25);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /**
     * What driving leaves behind: ruts and dust over dirt and a muddy car,
     * sparks off a wall, rubber from a slide on stone, and a dirty car beside
     * a clean one.
     */
    private void roadLife(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -6 12 %d 24 minecraft:dirt".formatted(Y - 1, Y - 1));
        server.runCommand("fill -12 %d 25 12 %d 60 minecraft:stone".formatted(Y - 1, Y - 1));
        server.runCommand("fill -6 %d 44 6 %d 45 minecraft:stone_bricks".formatted(Y, Y + 2));
        server.runCommand("summon car:car 0.5 %d -2.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:14,Tags:[\"photo\"]}".formatted(Y));
        server.runCommand("tp @a 0.5 %d -2.5".formatted(Y));
        context.waitTicks(20);
        server.runCommand("ride @p mount @e[tag=photo,limit=1]");
        context.waitTicks(10);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            client.options.hideGui = true;
        });
        lampCamera(context, server, 11.0, Y + 4.0, 14.0, 110, 22);
        context.getInput().holdKey(options -> options.keyUp);
        int shots = 0;
        StringBuilder trace = new StringBuilder("car photo roadLife:");
        for (int t = 0; t < 140 && shots < 8; t++) {
            context.waitTicks(1);
            double z = context.computeOnClient(client -> client.player.getVehicle() == null
                    ? 99.0 : client.player.getVehicle().getZ());
            trace.append(" %.1f".formatted(z));
            if (z > 9.0 && z < 24.0 && t % 3 == 0 && shots < 3) {
                context.takeScreenshot("road_dirt_" + shots++);
            }
            if (z > 40.5 && shots < 8) {
                if (shots < 3) {
                    shots = 3;
                }
                if (shots == 3) {
                    lampCamera(context, server, 8.0, Y + 2.0, 37.5, 62, 12);
                }
                context.takeScreenshot("road_crash_" + (shots++ - 3));
            }
        }
        context.getInput().releaseKey(options -> options.keyUp);
        System.out.println(trace);
        float dirt = context.computeOnClient(client -> client.player.getVehicle()
                instanceof me.trossty.car.entity.CarEntity car ? car.upkeep().getDirt() : -1.0F);
        System.out.println("car photo roadLife dirt after the dirt road: " + dirt);
        lampCamera(context, server, 9.0, Y + 6.0, 6.0, 120, 35);
        context.takeScreenshot("road_ruts");

        // A slide on stone: up to speed, then the wheel hard over.
        server.runCommand("ride @p dismount");
        server.runCommand("kill @e[tag=photo]");
        clear(server);
        server.runCommand("fill -30 %d -30 30 %d 40 minecraft:stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 15.5 %d -24.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:3,Tags:[\"photo\"]}".formatted(Y));
        server.runCommand("tp @a 15.5 %d -24.5".formatted(Y));
        context.waitTicks(20);
        server.runCommand("ride @p mount @e[tag=photo,limit=1]");
        context.waitTicks(10);
        context.runOnClient(client -> client.player.setYRot(0));
        lampCamera(context, server, 15.5, Y + 20.0, -14.0, 0, 55);
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(45);
        // The handbrake with the wheel over: the back steps out.
        context.getInput().holdKey(options -> options.keyLeft);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(25);
        context.getInput().releaseKey(options -> options.keyUp);
        context.getInput().releaseKey(options -> options.keyLeft);
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitTicks(15);
        context.takeScreenshot("road_skid");
        server.runCommand("ride @p dismount");
        server.runCommand("kill @e[tag=camera]");
        context.runOnClient(client -> client.setCameraEntity(client.player));

        // Clean and filthy, side by side.
        clear(server);
        server.runCommand("fill -20 %d -20 20 %d 20 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car -2.0 %d 0.5 {Rotation:[90f,0f],Model:\"car:toyota_corolla\",Color:0}".formatted(Y));
        server.runCommand("summon car:car -2.0 %d 6.5 {Rotation:[90f,0f],Model:\"car:toyota_corolla\",Color:0,Dirt:1.0f}".formatted(Y));
        context.waitTicks(10);
        shot(context, server, "road_clean_and_dirty", 6.0, Y + 1.0, 3.5, 90, 8);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    private void tyreLooks(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -20 20 %d 20 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 0.5 %d 0.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:3,FlatTyre:0}".formatted(Y));
        server.runCommand("summon car:car 0.5 %d 8.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:14,TyresOff:5}".formatted(Y));
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "tyre_flat", -3.5, Y + 0.6, 3.6, -120, 6);
        shot(context, server, "tyre_off", -3.5, Y + 0.6, 11.6, -120, 6);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** A caravan on a Jeep's tow ball, from the door side and from behind. */
    private void caravan(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -25 20 %d 20 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:car 0.5 %d 4.5 {Rotation:[0f,0f],Model:\"car:jeep_wrangler\",Color:13}".formatted(Y));
        server.runCommand("summon car:trailer 0.5 %d -2.0 {Rotation:[0f,0f],Model:\"car:caravan\",Color:11}".formatted(Y));
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 30);
            var jeep = world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box,
                    e -> !(e instanceof me.trossty.car.entity.TrailerEntity)).get(0);
            world.getEntitiesOfClass(me.trossty.car.entity.TrailerEntity.class, box, e -> true).get(0).hitch(jeep);
        });
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "caravan_door", -9.0, Y + 2.5, -3.0, -90, 10);
        shot(context, server, "caravan_rear", 6.0, Y + 3.5, -12.0, 30, 14);
        shot(context, server, "caravan_front", -5.0, Y + 3.0, 10.0, -150, 14);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** The tow truck with its wheel lift on, a Corolla's front wheels up in it. */
    private void towTruck(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -25 20 %d 20 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:pickup 0.5 %d 0.5 {Rotation:[0f,0f],Model:\"car:hilux_tow\",Color:4}".formatted(Y));
        server.runCommand("summon car:trailer 0.5 %d -4.0 {Rotation:[0f,0f],Model:\"car:wheel_lift\",Color:4}".formatted(Y));
        server.runCommand("summon car:car 0.5 %d -8.5 {Rotation:[0f,0f],Model:\"car:toyota_corolla\",Color:3}".formatted(Y));
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var player = minecraft.getPlayerList().getPlayers().get(0);
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 30);
            var truck = world.getEntitiesOfClass(me.trossty.car.entity.PickupEntity.class, box, e -> true).get(0);
            var lift = world.getEntitiesOfClass(me.trossty.car.entity.TrailerEntity.class, box, e -> true).get(0);
            lift.hitch(truck);
            player.setShiftKeyDown(true);
            player.moveTo(lift.getX() + 2.0, lift.getY(), lift.getZ());
            System.out.println("car photo towTruck: winch -> "
                    + lift.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND)
                    + " cargo=" + lift.hasCargo());
            player.setShiftKeyDown(false);
        });
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "tow_truck_side", 6.0, Y + 2.0, -3.5, 90, 12);
        shot(context, server, "tow_truck_rear", -3.5, Y + 3.0, -2.5, -60, 30);
        shot(context, server, "tow_truck_front", 4.5, Y + 2.5, 6.5, 150, 14);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** A car pulling the trailer round a right-hand bend, seen from in front. */
    private void towing(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -20 %d -10 20 %d 40 minecraft:gravel".formatted(Y - 1, Y - 1));
        server.runCommand("summon car:pickup 0.5 %d 1.5 {Rotation:[0f,0f],Color:14,Tags:[\"tractor\"]}".formatted(Y));
        server.runCommand("summon car:trailer 0.5 %d -3.5 {Rotation:[0f,0f],Tags:[\"towed\"]}".formatted(Y));
        context.waitTicks(5);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            var tractor = world.getEntitiesOfClass(me.trossty.car.entity.PickupEntity.class, box, e -> true).get(0);
            var trailer = world.getEntitiesOfClass(me.trossty.car.entity.TrailerEntity.class, box, e -> true).get(0);
            trailer.hitch(tractor);
        });
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "trailer_hitched", 4.0, Y + 2.4, -7.5, 30, 22);
        server.runCommand("ride @p mount @e[tag=tractor,limit=1]");
        context.waitTicks(5);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            // The front camera turns the pitch over: looking up puts it above the car, looking down under the ground.
            client.player.setXRot(-30);
            client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(10);
        towJitter(context, 10, "straight");
        context.getInput().holdKey(options -> options.keyRight);
        towJitter(context, 12, "bend");
        context.takeScreenshot("trailer_turning");
        report(server, "turning");
        context.getInput().releaseKey(options -> options.keyRight);
        context.waitTicks(20);
        context.takeScreenshot("trailer_straightening");
        report(server, "straight");
        context.getInput().releaseKey(options -> options.keyUp);
        context.waitTicks(40);
        server.runCommand("ride @p dismount");
        context.waitTicks(5);
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /**
     * How steadily the client draws the trailer behind the car, tick by tick:
     * the gap between the two at the start, middle and end of each frame's
     * interpolation. Pulled smoothly it barely moves; a trailer that jumps
     * back and forth between frames shows up as a spread in these.
     */
    private void towJitter(ClientGameTestContext context, int ticks, String when) {
        double[] spread = {Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int i = 0; i < ticks; i++) {
            context.waitTicks(1);
            context.runOnClient(client -> {
                var car = client.player.getVehicle();
                var trailer = client.level.getEntitiesOfClass(me.trossty.car.entity.TrailerEntity.class,
                        car.getBoundingBox().inflate(8.0), e -> true).get(0);
                for (float partial : new float[] {0.0F, 0.5F, 1.0F}) {
                    double gap = car.getPosition(partial).distanceTo(trailer.getPosition(partial));
                    spread[0] = Math.min(spread[0], gap);
                    spread[1] = Math.max(spread[1], gap);
                }
            });
        }
        System.out.printf("car drive test tow jitter %s: gap %.3f to %.3f (spread %.3f)%n",
                when, spread[0], spread[1], spread[1] - spread[0]);
    }

    /** Where the trailer is against the tow ball, on the server: printed for the log. */
    private void report(TestServerContext server, String when) {
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-60, Y - 20, -60, 60, Y + 20, 80);
            var trailer = world.getEntitiesOfClass(me.trossty.car.entity.TrailerEntity.class, box, e -> true).get(0);
            var tractor = trailer.getTractor();
            System.out.printf("car drive test trailer %s: hitched=%s gap=%.2f yaw=%.1f tractorYaw=%.1f%n", when,
                    trailer.isHitched(),
                    tractor == null ? -1.0 : Math.sqrt(tractor.hitchPoint().distanceToSqr(
                            trailer.getX() - Math.sin(Math.toRadians(trailer.getYRot())) * 0,
                            trailer.getY(), trailer.getZ())),
                    trailer.getYRot(), tractor == null ? 0.0F : tractor.getYRot());
        });
    }

    /** The spray can and the key as 3D items: on display, and in the hand. */
    private void items3d(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        String[] items = {
                "id:\"car:spray_can\"",
                "id:\"car:spray_can\",components:{\"minecraft:dyed_color\":3949738}",
                "id:\"car:car_key\"",
        };
        for (int i = 0; i < items.length; i++) {
            for (int row = 0; row < 2; row++) {
                server.runCommand(("summon minecraft:item_display %.1f %.1f 12.5 {item_display:\"%s\","
                        + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                        + "translation:[0f,0f,0f],scale:[1.2f,1.2f,1.2f]},"
                        + "item:{%s,count:1}}")
                        .formatted(-1.6 + i * 1.6, Y + 2.2 - row * 1.4, row == 0 ? "gui" : "ground", items[i]));
            }
        }
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "items3d_display", 0.0, Y + 0.6, 17.0, 180, 0);
        clear(server);
        server.runCommand("clear @a");
        server.runCommand("item replace entity @p weapon.mainhand with car:spray_can[minecraft:dyed_color=11546150]");
        server.runCommand("item replace entity @p weapon.offhand with car:car_key");
        context.runOnClient(client -> client.options.hideGui = false);
        shot(context, server, "items3d_in_hand", 0.5, Y, 0.5, 0, 10);
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.runOnClient(client -> client.options.hideGui = true);
        shot(context, server, "items3d_third_person", 0.5, Y, 0.5, 0, 10);
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        context.runOnClient(client -> client.options.hideGui = false);
        server.runCommand("clear @a");
    }

    /** Each car close up from the rear three-quarter and the side, to find where light shows through. */
    private void carGaps(ClientGameTestContext context, TestServerContext server) {
        String only = System.getProperty("car.photo.cars", "ford_mustang,toyota_corolla,tofas_sahin,jeep_wrangler,toyota_hilux,mercedes_atego");
        for (String name : only.split(",")) {
            clear(server);
            String type = name.equals("toyota_hilux") ? "car:pickup" : name.equals("mercedes_atego") ? "car:fuel_tanker" : "car:car";
            server.runCommand("summon %s 0.5 %d 10.5 {Rotation:[0f,0f],Color:11}".formatted(type, Y) .replace("{Rotation", "{Model:\"car:" + name + "\",Rotation"));
            context.runOnClient(client -> client.options.hideGui = true);
            context.waitTicks(10);
            shot(context, server, "gap_" + name + "_rear34", 4.5, Y + 1.4, 5.5, 36, 12);
            shot(context, server, "gap_" + name + "_side", 5.5, Y + 1.0, 10.5, 90, 6);
            shot(context, server, "gap_" + name + "_front34", -4.0, Y + 1.4, 15.5, -141, 12);
        }
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** A car driven up a one-block step, watched from the side a frame at a time. */
    /** Engine stands: two with an engine on, turned two ways, and an empty one. */
    private void engineStand(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var stand = me.trossty.car.registry.ModBlocks.ENGINE_STAND.defaultBlockState();
            var facing = me.trossty.car.block.EngineStandBlock.FACING;
            Direction[] ways = {Direction.SOUTH, Direction.EAST, Direction.SOUTH};
            for (int i = 0; i < 3; i++) {
                BlockPos pos = new BlockPos(-2 + i * 2, Y, 6);
                world.setBlockAndUpdate(pos, stand.setValue(facing, ways[i]));
                if (i < 2 && world.getBlockEntity(pos) instanceof me.trossty.car.block.entity.EngineStandBlockEntity on) {
                    on.setEngine(new net.minecraft.world.item.ItemStack(me.trossty.car.registry.ModItems.ENGINE));
                }
            }
        });
        context.runOnClient(client -> client.options.hideGui = true);
        context.waitTicks(10);
        shot(context, server, "engine_stand_front", -0.5, Y + 1.0, 9.8, 180, 15);
        shot(context, server, "engine_stand_side", 3.6, Y + 1.2, 7.8, 125, 20);
    }

    private void kerbClimb(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -6 %d 8 6 %d 30 minecraft:stone".formatted(Y, Y));
        server.runCommand("summon car:car 0.5 %d -2.5 {Rotation:[0f,0f],Color:14,Tags:[\"photo\"]}".formatted(Y));
        server.runCommand("tp @a 0.5 %d -2.5".formatted(Y));
        context.waitTicks(20);
        server.runCommand("ride @p mount @e[tag=photo,limit=1]");
        context.waitTicks(10);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            client.options.hideGui = true;
        });
        lampCamera(context, server, 9.5, Y + 1.6, 9.0, 90, 8);
        context.getInput().holdKey(options -> options.keyUp);
        StringBuilder trace = new StringBuilder("car photo kerb:");
        int shots = 0;
        for (int t = 0; t < 80 && shots < 6; t++) {
            context.waitTicks(1);
            String line = context.computeOnClient(client -> client.player.getVehicle()
                    instanceof me.trossty.car.entity.CarEntity car
                    ? " %.1f/%.2f/%.1f".formatted(car.getZ(), car.getY() - Y + car.getStepOffset(1.0F),
                    car.getBodyPitch(1.0F)) : " out");
            trace.append(line);
            boolean climbing = context.computeOnClient(client -> client.player.getVehicle()
                    instanceof me.trossty.car.entity.CarEntity car && car.getZ() > 5.0);
            if (climbing && t % 2 == 0) {
                context.takeScreenshot("kerb_" + shots++);
            }
        }
        context.getInput().releaseKey(options -> options.keyUp);
        System.out.println(trace);
        server.runCommand("ride @p dismount");
        context.runOnClient(client -> {
            client.setCameraEntity(client.player);
            client.options.hideGui = false;
        });
        server.runCommand("kill @e[tag=camera]");
    }

    /** The parts people carry, each in the hand: in first person and from in front. */
    private void heldParts(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        String[] items = {"car_key", "jerry_can", "spark_plugs", "wheel", "brake_discs"};
        for (String item : items) {
            server.runCommand("clear @a");
            server.runCommand("item replace entity @p weapon.mainhand with car:" + item);
            context.runOnClient(client -> {
                client.options.hideGui = false;
                client.options.setCameraType(CameraType.FIRST_PERSON);
            });
            shot(context, server, "held_" + item, 0.5, Y, 0.5, 0, 10);
            context.runOnClient(client -> {
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                client.options.hideGui = true;
            });
            shot(context, server, "held_" + item + "_front", 0.5, Y, 0.5, 0, 10);
        }
        context.runOnClient(client -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.options.hideGui = false;
        });
        server.runCommand("clear @a");
    }

    /**
     * Nobody using them: the bike over on its stand, the trailer down on its
     * nose, and a car blinking its indicators as it unlocks.
     */
    private void parked(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("summon car:motorcycle -2.5 %d 10.5 {Rotation:[0f,0f],Model:\"car:vespa_primavera\"}".formatted(Y));
        server.runCommand("summon car:trailer 1.0 %d 10.5 {Rotation:[0f,0f]}".formatted(Y));
        server.runCommand("summon car:car 5.5 %d 10.5 {Rotation:[0f,0f],Color:14,Tags:[\"locked\"]}".formatted(Y));
        context.runOnClient(client -> client.options.hideGui = true);
        // The poses ease in, so give them a second to settle.
        context.waitTicks(30);
        shot(context, server, "parked_side", 1.5, Y + 1.0, 18.5, 180, 8);
        shot(context, server, "parked_front", -3.0, Y + 1.2, 15.0, 160, 10);
        server.runCommand("tp @a 7.5 %.2f 15.5 155 14".formatted(Y + 1.4));
        context.waitTicks(10);
        server.runOnServer(minecraft -> {
            var world = minecraft.overworld();
            var box = new net.minecraft.world.phys.AABB(-30, Y - 5, -30, 30, Y + 10, 50);
            var car = world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class, box,
                    e -> e.getTags().contains("locked")).get(0);
            world.broadcastEntityEvent(car, me.trossty.car.entity.CarEntity.STATUS_UNLOCKED);
        });
        context.waitTicks(2);
        context.takeScreenshot("unlock_flash_1");
        context.waitTicks(5);
        context.takeScreenshot("unlock_flash_2");
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** The rider leant over with the bike, hands on the bars, from behind and in front. */
    private void riderLean(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("summon car:motorcycle 0.5 %d -6.5 {Rotation:[0f,0f],Model:\"car:honda_cbr600rr\",Tags:[\"bike\"]}".formatted(Y));
        context.waitTicks(5);
        server.runCommand("ride @p mount @e[tag=bike,limit=1]");
        context.waitTicks(5);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            client.player.setXRot(15);
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            client.options.hideGui = true;
        });
        context.waitTicks(5);
        context.getInput().holdKey(options -> options.keyRight);
        context.waitTicks(8);
        context.takeScreenshot("bars_turned_still");
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.waitTicks(2);
        context.takeScreenshot("bars_turned_front");
        context.getInput().releaseKey(options -> options.keyRight);
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(30);
        context.getInput().holdKey(options -> options.keyRight);
        context.waitTicks(10);
        context.takeScreenshot("rider_leaning_back");
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.waitTicks(2);
        context.takeScreenshot("rider_leaning_front");
        context.getInput().releaseKey(options -> options.keyRight);
        context.getInput().releaseKey(options -> options.keyUp);
        context.waitTicks(30);
        server.runCommand("ride @p dismount");
        context.waitTicks(5);
        context.runOnClient(client -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.options.hideGui = false;
        });
    }

    /** The driver's hands on a steering wheel that turns, seen through the windscreen. */
    private void steeringWheel(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("summon car:car 0.5 %d -6.5 {Rotation:[0f,0f],Color:0,Tags:[\"photo\"]}".formatted(Y));
        server.runCommand("tp @a 3.5 %d -6.5".formatted(Y));
        context.waitTicks(10);
        server.runCommand("ride @p mount @e[tag=photo,limit=1]");
        context.waitTicks(10);
        context.runOnClient(client -> System.out.println("car drive test wheel: riding="
                + client.player.getVehicle() + " at " + client.player.position()));
        context.runOnClient(client -> {
            // Looking up puts the front camera up over the bonnet, looking
            // back down through the windscreen.
            client.player.setYRot(0);
            client.player.setXRot(-40);
            client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            client.options.hideGui = true;
        });
        context.waitTicks(5);
        context.takeScreenshot("wheel_straight");
        context.getInput().holdKey(options -> options.keyRight);
        context.waitTicks(10);
        context.takeScreenshot("wheel_turned_right");
        context.runOnClient(client -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.player.setXRot(50);
        });
        context.waitTicks(2);
        context.takeScreenshot("wheel_first_person");
        context.getInput().releaseKey(options -> options.keyRight);
        server.runCommand("ride @p dismount");
        context.waitTicks(5);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /** The sports bike leant over in a fast right-hander. */
    private void leaning(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("summon car:motorcycle 0.5 %d -6.5 {Rotation:[0f,0f],Model:\"car:honda_cbr600rr\",Tags:[\"bike\"]}".formatted(Y));
        context.waitTicks(5);
        server.runCommand("ride @p mount @e[tag=bike,limit=1]");
        context.waitTicks(5);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            client.player.setXRot(15);
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            client.options.hideGui = true;
        });
        context.waitTicks(5);
        context.takeScreenshot("bike_riding_still");
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(30);
        context.getInput().holdKey(options -> options.keyRight);
        context.waitTicks(10);
        context.takeScreenshot("bike_leaning");
        context.getInput().releaseKey(options -> options.keyRight);
        context.getInput().releaseKey(options -> options.keyUp);
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /**
     * The Jeep at night facing a rise in the road, a villager and a wall: the
     * pool of light should climb the steps, and the villager and the wall
     * should come out of the dark lit by the lamps.
     */
    private void nightRoad(ClientGameTestContext context, TestServerContext server) {
        clear(server);
        server.runCommand("fill -12 %d -6 12 %d 24 minecraft:grass_block".formatted(Y - 1, Y - 1));
        server.runCommand("fill -12 %d -6 12 %d 24 minecraft:air".formatted(Y, Y + 8));
        server.runCommand("fill -12 %d 7 12 %d 24 minecraft:dirt".formatted(Y, Y));
        server.runCommand("fill -12 %d 10 12 %d 24 minecraft:dirt".formatted(Y + 1, Y + 1));
        server.runCommand("fill -4 %d 18 4 %d 18 minecraft:stone_bricks".formatted(Y + 2, Y + 5));
        server.runCommand("summon villager 2.5 %d 12.5 {NoAI:1b,Rotation:[180f,0f],Tags:[\"photo\"]}".formatted(Y + 2));
        server.runCommand("time set 18000");
        server.runCommand("summon car:car 0.5 %d 0.5 {Rotation:[0f,0f],Model:\"car:jeep_wrangler\",Color:14,Tags:[\"photo\"]}"
                .formatted(Y));
        server.runCommand("tp @a 3.5 %d 0.5".formatted(Y));
        context.waitTicks(10);
        server.runCommand("ride @p mount @e[type=car:car,tag=photo,limit=1]");
        context.waitTicks(10);
        context.runOnClient(client -> {
            client.player.setYRot(0);
            client.player.setXRot(10);
            client.options.hideGui = true;
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });
        context.waitTicks(20);
        context.takeScreenshot("night_road_driver");
        lampCamera(context, server, 9.5, Y + 4.0, 7.0, 90, 22);
        context.takeScreenshot("night_road_side");
        context.runOnClient(client -> client.setCameraEntity(client.player));
        server.runCommand("ride @p dismount");
        context.waitTicks(20);
        // Nobody at the wheel: the lamps are off, and so is their light.
        lampCamera(context, server, 9.5, Y + 4.0, 7.0, 90, 22);
        context.waitTicks(10);
        context.takeScreenshot("night_road_off");
        context.runOnClient(client -> {
            client.setCameraEntity(client.player);
            client.options.hideGui = false;
        });
        server.runCommand("kill @e[tag=camera]");
        server.runCommand("time set noon");
    }

    /** Every vehicle at night with somebody at the wheel: the head lamps and their beam, then the brake lamps. */
    private void lamps(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("fill -20 %d -30 20 %d 30 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        server.runCommand("fill -20 %d -30 20 %d 30 minecraft:air".formatted(Y, Y + 8));
        server.runCommand("time set 18000");
        String[][] vehicles = {
                {"car:car", "car:toyota_corolla"},
                {"car:car", "car:jeep_wrangler"},
                {"car:car", "car:ford_mustang"},
                {"car:car", "car:tofas_sahin"},
                {"car:pickup", "car:toyota_hilux"},
                {"car:fuel_tanker", "car:mercedes_atego"},
                {"car:motorcycle", "car:honda_cbr600rr"},
        };
        for (String[] vehicle : vehicles) {
            clear(server);
            String name = vehicle[1].substring(4);
            server.runCommand("summon %s 0.5 %d 0.5 {Rotation:[0f,0f],Model:\"%s\",Color:14,Tags:[\"photo\"]}"
                    .formatted(vehicle[0], Y, vehicle[1]));
            server.runCommand("tp @a 3.5 %d 0.5".formatted(Y));
            context.waitTicks(10);
            server.runCommand("ride @p mount @e[tag=photo,limit=1]");
            context.waitTicks(10);
            context.runOnClient(client -> {
                client.player.setYRot(0);
                client.options.hideGui = true;
            });
            // A stand-in camera out on the road, so the driver can stay at the wheel.
            lampCamera(context, server, 3.5, Y + 1.4, 7.5, 156, 8);
            context.takeScreenshot("lamps_" + name + "_front");
            context.getInput().holdKey(options -> options.keyJump);
            lampCamera(context, server, 3.0, Y + 1.4, -5.5, 22, 8);
            context.takeScreenshot("lamps_" + name + "_brake");
            context.getInput().releaseKey(options -> options.keyJump);
            context.waitTicks(3);
            context.takeScreenshot("lamps_" + name + "_off");
            // And what the driver sees of it over the bonnet.
            context.runOnClient(client -> {
                client.setCameraEntity(client.player);
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.player.setXRot(15);
            });
            context.waitTicks(5);
            context.takeScreenshot("lamps_" + name + "_driver");
            server.runCommand("ride @p dismount");
            context.waitTicks(5);
        }
        context.runOnClient(client -> {
            client.options.hideGui = false;
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });
        server.runCommand("time set noon");
    }

    /** Looks through an invisible stand placed here, until the camera is handed back to the player. */
    private void lampCamera(ClientGameTestContext context, TestServerContext server,
                            double x, double y, double z, float yaw, float pitch) {
        server.runCommand("kill @e[tag=camera]");
        server.runCommand("summon armor_stand %.2f %.2f %.2f {Rotation:[%.1ff,%.1ff],Invisible:1b,NoGravity:1b,Marker:1b,Tags:[\"camera\"]}"
                .formatted(x, y, z, yaw, pitch));
        context.waitTicks(5);
        context.runOnClient(client -> client.level.entitiesForRendering().forEach(entity -> {
            if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand) {
                client.setCameraEntity(entity);
                client.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }));
        context.waitTicks(5);
    }

    /** The cabins: the round wheel, the seats, the dash and the carpet, from the driver's seat and through a window. */
    private void cabins(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("fill -20 %d -30 20 %d 30 minecraft:smooth_stone".formatted(Y - 1, Y - 1));
        String[][] vehicles = {
                {"car:car", "car:toyota_corolla", "0"},
                {"car:car", "car:ford_mustang", "11"},
                {"car:fuel_tanker", "car:mercedes_atego", "0"},
        };
        for (String[] vehicle : vehicles) {
            clear(server);
            String name = vehicle[1].substring(4);
            server.runCommand("summon %s 0.5 %d -6.5 {Rotation:[0f,0f],Model:\"%s\",Color:%s,Tags:[\"photo\"]}"
                    .formatted(vehicle[0], Y, vehicle[1], vehicle[2]));
            server.runCommand("tp @a 3.5 %d -6.5".formatted(Y));
            context.waitTicks(10);
            server.runCommand("ride @p mount @e[tag=photo,limit=1]");
            context.waitTicks(10);
            context.runOnClient(client -> {
                client.player.setYRot(0);
                client.player.setXRot(22);
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.options.hideGui = true;
            });
            context.waitTicks(5);
            context.takeScreenshot("cab_" + name + "_driver");
            context.runOnClient(client -> client.player.setXRot(55));
            context.waitTicks(3);
            context.takeScreenshot("cab_" + name + "_down");
            server.runCommand("ride @p dismount");
            context.waitTicks(5);
            boolean big = vehicle[0].equals("car:fuel_tanker");
            shot(context, server, "cab_" + name + "_window", big ? 3.0 : 2.7, Y + (big ? 3.9 : 1.7),
                    big ? -3.6 : -6.3, 90, big ? 42 : 35);
        }
        context.runOnClient(client -> client.options.hideGui = false);
    }

}
