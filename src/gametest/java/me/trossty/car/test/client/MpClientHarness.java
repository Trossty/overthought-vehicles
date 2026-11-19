package me.trossty.car.test.client;

import me.trossty.car.entity.CarEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * One player of the two-player test ({@code mpAlice}, {@code mpBob}): joins
 * the {@code mpServer} on this machine and plays its part, going by nothing
 * but what its own client sees. Alice, once she is in the driving seat, holds
 * the throttle into the wall and then the brake. Bob rides along, logs where
 * his client puts the car, and photographs the drive and, once night falls
 * and he is set down in front of it, the lamps. Either quits once the server
 * has gone.
 */
final class MpClientHarness {

    private static final String ADDRESS = "localhost:25599";

    private final String who;
    private boolean connecting;
    private boolean joined;
    private int ticks;
    /** Ticks since the player first sat in the car; -1 before that. */
    private int seated = -1;
    private boolean ridingShot;
    private int nightTicks;
    private String pendingShot;

    private MpClientHarness(String who) {
        this.who = who;
    }

    static void install(String who) {
        MpClientHarness harness = new MpClientHarness(who);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> harness.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post event) -> harness.shoot(Minecraft.getInstance()));
    }

    private void log(String line) {
        System.out.println("car mp " + this.who + " " + line);
    }

    private void tick(Minecraft client) {
        this.ticks++;
        if (!this.connecting) {
            if (client.getOverlay() == null
                    && (client.screen instanceof TitleScreen || client.screen instanceof AccessibilityOnboardingScreen)) {
                this.connecting = true;
                client.options.pauseOnLostFocus = false;
                client.options.onboardAccessibility = false;
                client.options.setCameraType(CameraType.FIRST_PERSON);
                ConnectScreen.startConnecting(new TitleScreen(), client, ServerAddress.parseString(ADDRESS),
                        new ServerData("car mp", ADDRESS, ServerData.Type.OTHER), false, null);
                log("connecting");
            }
            return;
        }
        if (client.level == null || client.player == null) {
            // In, then out again: the server is done with us.
            if (this.joined && !(client.screen instanceof ConnectScreen)) {
                log("server gone, quitting");
                client.stop();
            }
            if (this.ticks > 20 * 300) {
                log("FAILED: gave up");
                client.stop();
            }
            return;
        }
        if (!this.joined) {
            this.joined = true;
            log("joined");
        }
        if (client.player.getVehicle() instanceof CarEntity car) {
            this.seated++;
            if (this.seated == 0) {
                this.seated = 1;
                log("seated, driving: " + (car.getControllingPassenger() == client.player));
            }
            if ("alice".equals(this.who)) {
                this.drive(client);
            } else {
                if (this.seated % 10 == 0) {
                    log("t=%d time=%d sees car %.2f %.2f yaw %.0f speed %.2f hp %.1f, own hp %.1f".formatted(
                            this.seated, client.level.getGameTime(), car.getX(), car.getZ(), car.getYRot(),
                            car.getEngineSpeed(), car.damage().getHealth(), client.player.getHealth()));
                }
                if (!this.ridingShot && car.getEngineSpeed() > 0.3F) {
                    this.ridingShot = true;
                    this.pendingShot = "mp_bob_riding";
                }
            }
        } else if (this.seated > 0 && "bob".equals(this.who)) {
            // Out in the dark, looking at the car with Alice still in it.
            if (client.level.getDayTime() % 24000 > 13000 && ++this.nightTicks == 40) {
                this.pendingShot = "mp_bob_night_front";
                log("night: own hp %.1f".formatted(client.player.getHealth()));
            }
        }
        if ("alice".equals(this.who) && client.level.getDayTime() % 24000 > 13000 && ++this.nightTicks == 60) {
            this.pendingShot = "mp_alice_night";
        }
    }

    /** Throttle for three seconds, into the wall, then the brake for one and a half. */
    private void drive(Minecraft client) {
        if (this.seated == 20) {
            KeyMapping.set(client.options.keyUp.getKey(), true);
        } else if (this.seated == 80) {
            KeyMapping.set(client.options.keyUp.getKey(), false);
            KeyMapping.set(client.options.keyJump.getKey(), true);
        } else if (this.seated == 110) {
            KeyMapping.set(client.options.keyJump.getKey(), false);
            log("drove: own hp %.1f".formatted(client.player.getHealth()));
        }
    }

    private void shoot(Minecraft client) {
        if (this.pendingShot != null) {
            Screenshot.grab(client.gameDirectory, this.pendingShot + ".png", client.getMainRenderTarget(), message -> { });
            log("shot " + this.pendingShot);
            this.pendingShot = null;
        }
    }
}
