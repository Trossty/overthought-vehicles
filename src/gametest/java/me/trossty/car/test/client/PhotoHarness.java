package me.trossty.car.test.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.trossty.car.CarMain;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Drives the real client through {@link CarPhotoTest}'s scenes and quits: a
 * fresh flat creative world, the scenes' commands run on its server, and a
 * screenshot wherever a scene asks for one. Only with {@code -Dcar.photo=true}
 * (the {@code photoClient} run); otherwise it does nothing.
 *
 * <p>The scenes run on a thread of their own and wait on the client's ticks,
 * the same way Fabric's client gametests did, so they read top to bottom.
 */
@Mod(value = CarMain.MOD_ID, dist = Dist.CLIENT)
public final class PhotoHarness {

    private static final String WORLD = "car_photo";

    private static final AtomicInteger TICKS = new AtomicInteger();
    private static volatile boolean started;
    private static volatile String pendingShot;
    private static volatile CompletableFuture<Void> shotTaken;

    public PhotoHarness() {
        String player = System.getProperty("car.mp");
        if (player != null) {
            MpClientHarness.install(player);
            return;
        }
        if (!Boolean.getBoolean("car.photo")) {
            return;
        }
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            TICKS.incrementAndGet();
            Minecraft client = Minecraft.getInstance();
            if (!started && client.getOverlay() == null
                    && (client.screen instanceof TitleScreen || client.screen instanceof AccessibilityOnboardingScreen)) {
                started = true;
                Thread runner = new Thread(PhotoHarness::run, "car photo");
                runner.setDaemon(true);
                runner.start();
            }
        });
        // A screenshot is taken once a whole frame, the HUD with it, is drawn.
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post event) -> {
            String name = pendingShot;
            if (name != null) {
                pendingShot = null;
                Minecraft client = Minecraft.getInstance();
                Screenshot.grab(client.gameDirectory, "photo_" + name + ".png", client.getMainRenderTarget(),
                        message -> { });
                System.out.println("car photo shot " + name);
                shotTaken.complete(null);
            }
        });
    }

    private static void run() {
        Minecraft client = Minecraft.getInstance();
        ClientGameTestContext context = new ClientGameTestContext();
        try {
            context.runOnClient(PhotoHarness::openWorld);
            // In the world, the loading screens gone and the ground drawn.
            context.waitFor(c -> c.level != null && c.player != null && c.screen == null
                    && c.getSingleplayerServer() != null);
            context.waitTicks(20);
            context.waitFor(c -> c.levelRenderer.hasRenderedAllSections());
            new CarPhotoTest().runTest(context, new TestServerContext(client.getSingleplayerServer()));
            System.out.println("car photo done");
        } catch (Throwable failure) {
            System.out.println("car photo FAILED");
            failure.printStackTrace(System.out);
        } finally {
            client.execute(client::stop);
        }
    }

    private static void openWorld(Minecraft client) {
        Options options = client.options;
        options.pauseOnLostFocus = false;
        options.onboardAccessibility = false;
        options.skipMultiplayerWarning = true;
        client.getTutorial().setStep(TutorialSteps.NONE);
        Path old = client.getLevelSource().getBaseDir().resolve(WORLD);
        if (Files.exists(old)) {
            try (Stream<Path> files = Files.walk(old)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                rules, WorldDataConfiguration.DEFAULT);
        client.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                client.screen);
    }

    /** What a scene has of the client: time, screenshots, and its hands on the game. */
    public static final class ClientGameTestContext {

        private final TestInput input = new TestInput(this);

        public void waitTicks(int ticks) {
            int until = TICKS.get() + ticks;
            while (TICKS.get() < until) {
                sleep();
            }
        }

        void waitFor(java.util.function.Predicate<Minecraft> ready) {
            long deadline = System.currentTimeMillis() + 120_000;
            while (!this.computeOnClient(ready::test)) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("timed out waiting on the client");
                }
                this.waitTicks(1);
            }
        }

        public void takeScreenshot(String name) {
            shotTaken = new CompletableFuture<>();
            pendingShot = name;
            shotTaken.join();
            // The file is written off the render thread; let it land.
            this.waitTicks(2);
        }

        public void runOnClient(Consumer<Minecraft> action) {
            Minecraft client = Minecraft.getInstance();
            client.submit(() -> action.accept(client)).join();
        }

        public <T> T computeOnClient(Function<Minecraft, T> action) {
            Minecraft client = Minecraft.getInstance();
            return client.submit(() -> action.apply(client)).join();
        }

        public TestInput getInput() {
            return this.input;
        }

        private static void sleep() {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    /** The integrated server, for commands and for reaching into the world. */
    public static final class TestServerContext {

        private final MinecraftServer server;

        TestServerContext(MinecraftServer server) {
            this.server = server;
        }

        public void runCommand(String command) {
            this.server.submit(() -> this.server.getCommands().performPrefixedCommand(
                    this.server.createCommandSourceStack().withSuppressedOutput(), command)).join();
        }

        public void runOnServer(Consumer<MinecraftServer> action) {
            this.server.submit(() -> action.accept(this.server)).join();
        }
    }

    /** Keys pressed as a player would: a key binding held or tapped, or a raw key sent to the screen. */
    public static final class TestInput {

        private final ClientGameTestContext context;

        TestInput(ClientGameTestContext context) {
            this.context = context;
        }

        public void holdKey(Function<Options, KeyMapping> binding) {
            this.context.runOnClient(client -> {
                InputConstants.Key key = binding.apply(client.options).getKey();
                KeyMapping.set(key, true);
                KeyMapping.click(key);
            });
        }

        public void releaseKey(Function<Options, KeyMapping> binding) {
            this.context.runOnClient(client -> KeyMapping.set(binding.apply(client.options).getKey(), false));
        }

        public void pressKey(Function<Options, KeyMapping> binding) {
            this.holdKey(binding);
            this.context.waitTicks(1);
            this.releaseKey(binding);
        }

        public void pressKey(int glfwKey) {
            this.context.runOnClient(client -> {
                long window = client.getWindow().getWindow();
                int scan = GLFW.glfwGetKeyScancode(glfwKey);
                client.keyboardHandler.keyPress(window, glfwKey, scan, GLFW.GLFW_PRESS, 0);
                client.keyboardHandler.keyPress(window, glfwKey, scan, GLFW.GLFW_RELEASE, 0);
            });
            this.context.waitTicks(1);
        }
    }
}
