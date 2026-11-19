package me.trossty.car.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import me.trossty.car.machine.GaugeSource;
import me.trossty.car.machine.MachineScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The window every plain machine shares, drawn as the machine's own control
 * panel: a steel faceplate under the nameplate with the machine's working
 * parts on it, left to right as the work flows, each with its name stamped
 * under it, and a green LCD above the inventory with the one figure that
 * matters most for that machine.
 *
 * <ul>
 *   <li>A generator: its firebox, or its fuel tank, piping heat or fuel to
 *       the alternator's dial, cabled on to the LEDs of its store.</li>
 *   <li>The hydrotreater: feed tank, the reactor with a window on what is
 *       going on inside, product tank, and its store.</li>
 *   <li>The pumpjack: a cut through the ground with the jack nodding over its
 *       well and the oil pocket below, piped to its tank.</li>
 *   <li>The battery bank: a rack of cells between a cable in and a cable out.</li>
 *   <li>The charging post: its store, cabled to the car parked at it and that
 *       car's battery.</li>
 * </ul>
 *
 * Pipes and cables run their chevrons only while something flows, a tank
 * bubbles while it is worked, and the lamp on the nameplate says the rest.
 */
public class MachineScreen extends AbstractContainerScreen<MachineScreenHandler> {

    private static final int ENERGY_FLOW = 0xFFF2CF4A;
    private static final int FLAME = 0xFFF08A1E;
    private static final int FLAME_CORE = 0xFFFAD84A;
    private static final int FLAME_TIP = 0xFFD8461C;

    /** Body and highlight colours by the fluid numbers {@link MachineScreenHandler#FLUIDS} sends. */
    private static final int[][] FLUID_COLOURS = {
            {0xFF2A2A2A, 0xFF2A2A2A},
            {0xFF1E1A16, 0xFF5A4E3E},
            {0xFFB0702A, 0xFFD08F45},
            {0xFF4E4224, 0xFF7A6A40},
            {0xFFD9B23A, 0xFFF0D267},
            {0xFF8A6A2C, 0xFFAE8B48},
            {0xFF9CC23A, 0xFFC2E266},
    };

    private static final int PLATE_X = 7;
    private static final int PLATE_Y = 17;
    private static final int PLATE_WIDTH = 162;
    private static final int PLATE_HEIGHT = 66;
    private static final int GAUGE_TOP = 22;
    private static final int GAUGE_HEIGHT = 44;
    private static final int CAPTION_Y = 70;
    private static final int PIPE_Y = 40;
    private static final int READOUT_Y = 87;
    private static final int TANK_WIDTH = 18;
    private static final int ENERGY_X = 141;
    private static final int ENERGY_WIDTH = 16;

    /** What the pointer can ask about, gathered as the panel is drawn. */
    private record Hit(int x, int y, int width, int height, Supplier<Component> text) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private float needle;
    private float crank;
    private long lastFrame;

    public MachineScreen(MachineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private int kind() {
        return this.menu.kind();
    }

    private boolean isGenerator() {
        int kind = this.kind();
        return kind == GaugeSource.KIND_COAL_GENERATOR || kind == GaugeSource.KIND_BIOFUEL_GENERATOR
                || kind == GaugeSource.KIND_DIESEL_GENERATOR;
    }

    /** The rule under the nameplate, in the colour the machine is painted. */
    private int accent() {
        return switch (this.kind()) {
            case GaugeSource.KIND_COAL_GENERATOR -> 0xFFD0432F;
            case GaugeSource.KIND_BIOFUEL_GENERATOR -> 0xFF5DB33A;
            case GaugeSource.KIND_DIESEL_GENERATOR -> 0xFFE2B323;
            case GaugeSource.KIND_HYDROTREATER -> 0xFF4A90D9;
            case GaugeSource.KIND_PUMPJACK -> 0xFFE0A42A;
            case GaugeSource.KIND_BATTERY -> 0xFF38B6E8;
            case GaugeSource.KIND_CHARGER -> 0xFF3FC24A;
            default -> GuiStyle.ACCENT;
        };
    }

    // ---- drawing ----------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics context, float deltaTicks, int mouseX, int mouseY) {
        this.animate();
        this.hits.clear();
        int left = this.leftPos;
        int top = this.topPos;
        GuiStyle.frame(context, left, top, this.imageWidth, this.imageHeight, this.accent());
        GuiStyle.plate(context, left + PLATE_X, top + PLATE_Y, PLATE_WIDTH, PLATE_HEIGHT);

        if (this.isGenerator()) {
            this.drawGenerator(context, left, top);
        } else if (this.kind() == GaugeSource.KIND_HYDROTREATER) {
            this.drawHydrotreater(context, left, top);
        } else if (this.kind() == GaugeSource.KIND_PUMPJACK) {
            this.drawPumpjack(context, left, top);
        } else if (this.kind() == GaugeSource.KIND_BATTERY) {
            this.drawBatteryBank(context, left, top);
        } else if (this.kind() == GaugeSource.KIND_CHARGER) {
            this.drawCharger(context, left, top);
        }

        for (Slot slot : this.menu.slots) {
            GuiStyle.slot(context, left + slot.x - 1, top + slot.y - 1);
        }
        if (this.menu.isFuelled() && !this.menu.getSlot(0).hasItem()) {
            GuiStyle.ghost(context, new ItemStack(Items.COAL), left + MachineScreenHandler.FUEL_SLOT_X,
                    top + MachineScreenHandler.FUEL_SLOT_Y);
        }
    }

    /** Eases the alternator's needle and turns the pumpjack's crank, by the time since the last frame. */
    private void animate() {
        long now = net.minecraft.Util.getMillis();
        float seconds = this.lastFrame == 0 ? 0.0F : Math.min(0.25F, (now - this.lastFrame) / 1000.0F);
        this.lastFrame = now;
        boolean running = this.menu.working();
        float target = running ? 0.62F + 0.03F * (float) Math.sin(GuiStyle.ticks() * 0.9F) : 0.0F;
        this.needle += (target - this.needle) * Math.min(1.0F, seconds * 4.0F);
        if (running) {
            this.crank = (this.crank + seconds * 3.2F) % (float) (Math.PI * 2);
        }
    }

    private void drawGenerator(GuiGraphics context, int left, int top) {
        boolean running = this.menu.working();
        int feed;
        if (this.kind() == GaugeSource.KIND_COAL_GENERATOR) {
            drawFirebox(context, left + 13, top + GAUGE_TOP, running ? this.menu.progress() : 0.0F);
            this.caption(context, "firebox", left + 30, top, 38);
            feed = FLAME;
        } else {
            this.drawTank(context, left + 21, top, MachineScreenHandler.A_UNITS, running);
            this.caption(context, "fuel", left + 30, top, 38);
            feed = this.fluidColour(MachineScreenHandler.A_UNITS)[1];
        }
        GuiStyle.flow(context, left + 48, top + PIPE_Y, 30, feed, running ? 1.0F : 0.0F, running);
        GuiStyle.dial(context, left + 96, top + 42, 17, this.needle);
        this.caption(context, "alternator", left + 96, top, 58);
        GuiStyle.flow(context, left + 114, top + PIPE_Y, 27, ENERGY_FLOW, running ? 1.0F : 0.0F, running);
        this.drawEnergy(context, left + ENERGY_X, top);
    }

    private void drawHydrotreater(GuiGraphics context, int left, int top) {
        boolean running = this.menu.working();
        this.drawTank(context, left + 13, top, MachineScreenHandler.A_UNITS, running);
        this.caption(context, "feed", left + 22, top, 30);
        GuiStyle.flow(context, left + 31, top + PIPE_Y, 26, this.fluidColour(MachineScreenHandler.A_UNITS)[1],
                this.menu.progress(), running);
        drawReactor(context, left + 57, top + GAUGE_TOP, 24, GAUGE_HEIGHT, running);
        this.caption(context, "reactor", left + 69, top, 40);
        GuiStyle.flow(context, left + 81, top + PIPE_Y, 24, this.fluidColour(MachineScreenHandler.B_UNITS)[1],
                this.menu.progress(), running);
        this.drawTank(context, left + 105, top, MachineScreenHandler.B_UNITS, running);
        this.caption(context, "product", left + 114, top, 42);
        this.drawEnergy(context, left + ENERGY_X, top);
    }

    private void drawPumpjack(GuiGraphics context, int left, int top) {
        boolean running = this.menu.working();
        this.drawWell(context, left + 11, top + GAUGE_TOP, 86, GAUGE_HEIGHT, running, this.menu.progress());
        this.caption(context, "well", left + 54, top, 80);
        GuiStyle.flow(context, left + 97, top + 41, 10, FLUID_COLOURS[1][1], running ? 1.0F : 0.0F, running);
        this.drawTank(context, left + 107, top, MachineScreenHandler.A_UNITS, running);
        this.caption(context, "crude", left + 116, top, 30);
        this.drawEnergy(context, left + ENERGY_X, top);
    }

    private void drawBatteryBank(GuiGraphics context, int left, int top) {
        boolean giving = this.menu.working();
        boolean taking = this.menu.get(MachineScreenHandler.EXTRA2) > 0;
        float charge = fraction(this.menu.energy(), this.menu.energyCapacity());
        GuiStyle.flow(context, left + 11, top + PIPE_Y, 44, ENERGY_FLOW, taking ? 1.0F : 0.0F, taking);
        this.caption(context, "in", left + 33, top, 40);
        drawCells(context, left + 55, top + GAUGE_TOP + 4, 66, GAUGE_HEIGHT - 4, charge);
        this.hits.add(new Hit(55, GAUGE_TOP, 66, GAUGE_HEIGHT, this::energyText));
        GuiStyle.caption(context, this.font, Component.translatable("gui.overthought_vehicles.caption.charge",
                Math.round(charge * 100)), left + 88, top + CAPTION_Y, 62);
        GuiStyle.flow(context, left + 121, top + PIPE_Y, 44, ENERGY_FLOW, giving ? 1.0F : 0.0F, giving);
        this.caption(context, "out", left + 143, top, 40);
    }

    private void drawCharger(GuiGraphics context, int left, int top) {
        boolean charging = this.menu.working();
        boolean parked = this.menu.extra() > 0 || this.menu.get(MachineScreenHandler.EXTRA2) > 0;
        this.drawEnergy(context, left + 17, top);
        GuiStyle.flow(context, left + 34, top + PIPE_Y, 34, 0xFF5BE06A, charging ? 1.0F : 0.0F, charging);
        drawCar(context, left + 70, top + 26, parked, charging);

        float car = this.menu.get(MachineScreenHandler.EXTRA2) / 1000.0F;
        drawLevel(context, left + 86, top + 59, 54, 7, parked ? car : 0.0F);
        if (parked) {
            this.hits.add(new Hit(86, 59, 56, 7, () -> Component.translatable("gui.overthought_vehicles.machine.vehicle_charge",
                    this.menu.get(MachineScreenHandler.EXTRA2) / 10)));
        }
        this.caption(context, "vehicle", left + 113, top, 60);
    }

    private void caption(GuiGraphics context, String key, int centre, int top, int room) {
        GuiStyle.caption(context, this.font, Component.translatable("gui.overthought_vehicles.caption." + key), centre,
                top + CAPTION_Y, room);
    }

    private void drawEnergy(GuiGraphics context, int x, int top) {
        GuiStyle.energy(context, x, top + GAUGE_TOP, ENERGY_WIDTH, GAUGE_HEIGHT,
                fraction(this.menu.energy(), this.menu.energyCapacity()));
        this.hits.add(new Hit(x - this.leftPos, GAUGE_TOP, ENERGY_WIDTH, GAUGE_HEIGHT, this::energyText));
        GuiStyle.bolt(context, x + ENERGY_WIDTH / 2 - 2, top + CAPTION_Y);
    }

    private void drawTank(GuiGraphics context, int x, int top, int unitsIndex, boolean busy) {
        int units = this.menu.get(unitsIndex);
        int capacity = this.menu.get(unitsIndex + 1);
        int[] colour = this.fluidColour(unitsIndex);
        GuiStyle.tank(context, x, top + GAUGE_TOP, TANK_WIDTH, GAUGE_HEIGHT, fraction(units, capacity),
                colour[0], colour[1], busy);
        this.hits.add(new Hit(x - this.leftPos, GAUGE_TOP, TANK_WIDTH, GAUGE_HEIGHT, () -> this.tankText(unitsIndex)));
    }

    private int[] fluidColour(int unitsIndex) {
        int fluid = this.menu.get(unitsIndex + 2);
        return FLUID_COLOURS[Math.max(0, Math.min(FLUID_COLOURS.length - 1, fluid))];
    }

    private static float fraction(long amount, long capacity) {
        return capacity <= 0 ? 0.0F : GuiStyle.clamp((float) amount / capacity);
    }

    /**
     * A cast-iron firebox: a window onto the fire over the grate, the fire
     * standing as tall as what is left of the piece burning, and the fuel
     * slot below it.
     */
    private static void drawFirebox(GuiGraphics context, int x, int y, float burning) {
        int w = 34;
        int h = GAUGE_HEIGHT;
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, GuiStyle.OUTLINE);
        context.fill(x, y, x + w, y + h, 0xFF33363B);
        context.fill(x, y, x + w, y + 1, 0xFF50555B);
        context.fill(x, y, x + 1, y + h, 0xFF50555B);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF222428);
        context.fill(x + w - 1, y, x + w, y + h, 0xFF222428);
        for (int[] rivet : new int[][]{{x + 2, y + 2}, {x + w - 3, y + 2}, {x + 2, y + h - 3}, {x + w - 3, y + h - 3}}) {
            GuiStyle.dot(context, rivet[0], rivet[1], 0xFF767C83);
        }

        int wx = x + 5;
        int wy = y + 3;
        int ww = w - 10;
        int wh = 18;
        context.fill(wx - 1, wy - 1, wx + ww + 1, wy + wh + 1, 0xFF141518);
        float time = GuiStyle.ticks();
        if (burning > 0.0F) {
            context.fillGradient(wx, wy, wx + ww, wy + wh, 0xFF2A0E06, 0xFF8A2A0A);
            int height = Math.max(3, Math.round(burning * (wh - 4)));
            int base = wy + wh - 3;
            flame(context, wx + ww / 2, base, height + Math.round((float) Math.sin(time * 0.8F)), 4);
            flame(context, wx + ww / 2 - 6, base, Math.max(2, height * 2 / 3 + Math.round((float) Math.sin(time * 1.1F + 1))), 3);
            flame(context, wx + ww / 2 + 6, base, Math.max(2, height * 3 / 4 + Math.round((float) Math.sin(time * 0.9F + 2))), 3);
            for (int ember = wx; ember < wx + ww; ember += 2) {
                boolean hot = ((ember + (int) (time * 0.5F)) & 3) == 0;
                GuiStyle.dot(context, ember, wy + wh - 3, hot ? FLAME_CORE : FLAME);
            }
        } else {
            context.fill(wx, wy, wx + ww, wy + wh, 0xFF0F1012);
            for (int lump = wx + 1; lump < wx + ww - 2; lump += 4) {
                context.fill(lump, wy + wh - 4, lump + 3, wy + wh - 2, 0xFF1F2023);
            }
        }
        for (int bar = wx; bar < wx + ww; bar += 3) {
            context.fill(bar, wy + wh - 2, bar + 2, wy + wh, 0xFF5A5E64);
        }
        context.fill(wx + 1, wy + 1, wx + 4, wy + 2, 0x30FFFFFF);
        context.fill(wx + 1, wy + 2, wx + 2, wy + 4, 0x30FFFFFF);
    }

    /** One tongue of fire: orange round a yellow core, red at the very tip. */
    private static void flame(GuiGraphics context, int cx, int base, int height, int width) {
        for (int row = 0; row < height; row++) {
            float up = (float) row / height;
            int half = Math.round((1.0F - up) * width);
            int y = base - row;
            context.fill(cx - half, y, cx + half + 1, y + 1, up > 0.75F ? FLAME_TIP : FLAME);
            if (half >= 2 && up < 0.6F) {
                context.fill(cx - half + 1, y, cx + half, y + 1, FLAME_CORE);
            }
        }
    }

    /**
     * The hydrotreater's reactor: an aluminium vessel with a domed head and
     * banded shell, a nozzle on top and a round window that glows and
     * bubbles with hydrogen while it works.
     */
    private static void drawReactor(GuiGraphics context, int x, int y, int w, int h, boolean running) {
        int[] dome = {7, 4, 2, 1, 1};
        context.fill(x + w / 2 - 2, y - 1, x + w / 2 + 2, y + 2, GuiStyle.OUTLINE);
        context.fill(x + w / 2 - 1, y, x + w / 2 + 1, y + 2, GuiStyle.STEEL_DARK);
        for (int row = 0; row < h; row++) {
            int in = row < dome.length ? dome[row] : 0;
            int ry = y + 2 + row;
            if (ry >= y + h) {
                break;
            }
            context.fill(x + in - 1, ry - 1, x + w - in + 1, ry + 2, GuiStyle.OUTLINE);
        }
        for (int row = 0; row < h - 2; row++) {
            int in = row < dome.length ? dome[row] : 0;
            int ry = y + 2 + row;
            context.fill(x + in, ry, x + w - in, ry + 1, 0xFFB9C1CA);
            context.fill(x + in, ry, x + Math.max(in + 1, 4), ry + 1, 0xFFDDE3E9);
            context.fill(x + w - Math.max(in + 1, 5), ry, x + w - in, ry + 1, 0xFF8A929B);
        }
        for (int band : new int[]{y + 9, y + 31, y + 38}) {
            context.fill(x, band, x + w, band + 2, 0xFF6F7780);
            context.fill(x, band, x + w, band + 1, 0xFF9AA2AB);
        }
        int cx = x + w / 2;
        int cy = y + 21;
        GuiStyle.disc(context, cx, cy, 6, GuiStyle.OUTLINE);
        GuiStyle.disc(context, cx, cy, 5, GuiStyle.STEEL_DARK);
        GuiStyle.disc(context, cx, cy, 4, running ? 0xFFC0501A : 0xFF1A1E22);
        if (running) {
            GuiStyle.disc(context, cx, cy + 1, 2, 0xFFF08A2A);
            float time = GuiStyle.ticks();
            for (int bubble = 0; bubble < 3; bubble++) {
                int rise = (int) ((time * 0.5F + bubble * 3) % 7);
                GuiStyle.dot(context, cx - 2 + bubble * 2, cy + 3 - rise, 0xFFFFE2A8);
            }
        }
        GuiStyle.dot(context, cx - 2, cy - 3, 0x60FFFFFF);
        GuiStyle.dot(context, cx - 3, cy - 2, 0x60FFFFFF);
    }

    /**
     * The pumpjack's well, cut open: sky over grass, the jack on its skid
     * nodding over the wellhead, the casing down through dirt and stone to
     * the pocket of crude it draws on, which drains as the block is worked.
     */
    private void drawWell(GuiGraphics context, int x, int y, int w, int h, boolean running, float drawn) {
        GuiStyle.inset(context, x, y, w, h, 0xFF7FAEDD);
        int ground = y + 26;
        context.fillGradient(x, y, x + w, ground, 0xFF5C8CC4, 0xFFAFCFEA);
        context.fill(x, ground, x + w, ground + 2, 0xFF5E9E3A);
        context.fill(x, ground, x + w, ground + 1, 0xFF7CBC50);
        context.fill(x, ground + 2, x + w, ground + 7, 0xFF7A5A3A);
        context.fill(x, ground + 7, x + w, y + h, 0xFF6E6E6E);
        for (int speck = 0; speck < 26; speck++) {
            int sx = x + (speck * 37 + 11) % w;
            int sy = ground + 3 + (speck * 13 + 5) % (y + h - ground - 4);
            GuiStyle.dot(context, sx, sy, sy < ground + 7 ? 0xFF65482C : 0xFF5A5A5A);
        }

        int wellX = x + 78;
        int pocketTop = ground + 9;
        int pocketBottom = y + h - 2;
        int px0 = x + 64;
        int px1 = x + w - 3;
        context.fill(px0 + 1, pocketTop, px1 - 1, pocketBottom, 0xFF3C3833);
        context.fill(px0, pocketTop + 1, px1, pocketBottom - 1, 0xFF3C3833);
        int depth = pocketBottom - pocketTop - 1;
        int oil = Math.round((1.0F - GuiStyle.clamp(drawn)) * depth);
        if (oil > 0) {
            int surface = pocketBottom - 1 - oil;
            context.fill(px0 + 1, surface, px1 - 1, pocketBottom - 1, 0xFF1E1A16);
            context.fill(px0 + 1, surface, px1 - 1, surface + 1, 0xFF5A4E3E);
        }
        context.fill(wellX, ground, wellX + 2, pocketTop + 2, 0xFF9AA2AB);
        context.fill(wellX + 1, ground, wellX + 2, pocketTop + 2, 0xFF6A727B);
        if (running) {
            int rise = (int) (GuiStyle.ticks() * 0.6F) % 4;
            for (int blob = pocketTop + 1 - rise; blob > ground; blob -= 4) {
                GuiStyle.dot(context, wellX, blob, 0xFF2A241E);
            }
        }
        context.fill(wellX - 2, ground - 4, wellX + 4, ground, GuiStyle.OUTLINE);
        context.fill(wellX - 1, ground - 3, wellX + 3, ground, 0xFF7E868F);
        context.fill(wellX + 3, ground - 4, x + w, ground - 2, GuiStyle.OUTLINE);
        context.fill(wellX + 3, ground - 4, x + w, ground - 3, 0xFF5B636C);

        this.drawJack(context, x, ground);
    }

    /** The jack itself, its walking beam rocked by the crank. */
    private void drawJack(GuiGraphics context, int x, int ground) {
        int yellow = 0xFFE0B23A;
        int steel = 0xFF4A5058;
        context.fill(x + 6, ground - 2, x + 70, ground, 0xFF2F3439);
        context.fill(x + 8, ground - 7, x + 14, ground - 2, 0xFF3E7A46);
        context.fill(x + 8, ground - 7, x + 14, ground - 6, 0xFF5A9C62);
        context.fill(x + 14, ground - 9, x + 26, ground - 2, steel);
        context.fill(x + 14, ground - 9, x + 26, ground - 8, 0xFF6A717A);

        double pivotX = x + 46;
        double pivotY = ground - 17;
        GuiStyle.beam(context, x + 38, ground - 2, pivotX - 0.5, pivotY + 1, 2, 0xFF5B636C);
        GuiStyle.beam(context, x + 54, ground - 2, pivotX + 0.5, pivotY + 1, 2, 0xFF5B636C);

        double crankX = x + 21;
        double crankY = ground - 10;
        double pinX = crankX + Math.cos(this.crank) * 5;
        double pinY = crankY + Math.sin(this.crank) * 5;
        double lift = (pinY - crankY) * 0.7;
        double sin = lift / 22.0;
        double cos = Math.sqrt(1 - sin * sin);
        double rearX = pivotX - 22 * cos;
        double rearY = pivotY + 22 * sin;
        double headX = pivotX + 30 * cos;
        double headY = pivotY - 30 * sin;

        GuiStyle.beam(context, crankX, crankY, pinX, pinY, 2, 0xFF2A2F35);
        context.fill((int) pinX - 3, (int) pinY - 3, (int) pinX + 3, (int) pinY + 3, GuiStyle.OUTLINE);
        context.fill((int) pinX - 2, (int) pinY - 2, (int) pinX + 2, (int) pinY + 2, 0xFF9A3226);
        GuiStyle.line(context, rearX, rearY, pinX, pinY, 0xFF2A2F35);

        GuiStyle.beam(context, rearX, rearY, headX, headY, 4, GuiStyle.OUTLINE);
        GuiStyle.beam(context, rearX, rearY, headX, headY, 2, yellow);
        // the horse head, square to the beam, its face over the well
        double dx = cos;
        double dy = -sin;
        double nx = sin;
        double ny = cos;
        for (int pass = 0; pass < 2; pass++) {
            double grow = pass == 0 ? 1 : 0;
            int colour = pass == 0 ? GuiStyle.OUTLINE : yellow;
            for (double u = -2 - grow; u <= 2 + grow; u += 0.5) {
                for (double v = -3 - grow; v <= 6 + grow; v += 0.5) {
                    GuiStyle.dot(context, (int) Math.floor(headX + u * dx + v * nx),
                            (int) Math.floor(headY + u * dy + v * ny), colour);
                }
            }
        }
        double rodX = headX + 2 * dx + 6 * nx;
        double rodY = headY + 2 * dy + 6 * ny;
        GuiStyle.line(context, rodX, rodY, rodX, ground - 4, 0xFFC3CAD2);
        context.fill((int) pivotX - 1, (int) pivotY - 1, (int) pivotX + 2, (int) pivotY + 2, GuiStyle.OUTLINE);
        GuiStyle.dot(context, (int) pivotX, (int) pivotY, 0xFF9AA2AB);
    }

    /** The bank: a rack of four cells, each lit to the bank's charge. */
    private static void drawCells(GuiGraphics context, int x, int y, int w, int h, float charge) {
        GuiStyle.casing(context, x, y, w, h);
        context.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF2A2F35);
        int level = charge > 0.5F ? GuiStyle.LED_GREEN : charge > 0.2F ? GuiStyle.LED_AMBER : GuiStyle.LED_RED;
        for (int cell = 0; cell < 4; cell++) {
            int cx = x + 4 + cell * 15;
            int cy = y + 6;
            int cw = 13;
            int ch = h - 9;
            context.fill(cx + 4, cy - 3, cx + cw - 4, cy, GuiStyle.OUTLINE);
            context.fill(cx + 5, cy - 2, cx + cw - 5, cy, cell % 2 == 0 ? 0xFFC8302A : 0xFF3A3A3A);
            context.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, GuiStyle.OUTLINE);
            context.fill(cx, cy, cx + cw, cy + ch, 0xFF1B1F24);
            int segments = (ch - 1) / 4;
            int lit = charge <= 0.0F ? 0 : Math.max(1, Math.round(charge * segments));
            for (int segment = 0; segment < segments; segment++) {
                int sy = cy + ch - (segment + 1) * 4 + 1;
                context.fill(cx + 2, sy, cx + cw - 2, sy + 3, segment < lit ? level : GuiStyle.LED_OFF);
                if (segment < lit) {
                    context.fill(cx + 2, sy, cx + cw - 2, sy + 1, GuiStyle.lighter(level));
                }
            }
            context.fill(cx, cy, cx + 1, cy + ch, 0x20FFFFFF);
        }
    }

    /** A battery lying on its side, lit from the left to {@code level}, for the car's charge. */
    private static void drawLevel(GuiGraphics context, int x, int y, int w, int h, float level) {
        GuiStyle.casing(context, x, y, w, h);
        context.fill(x + w + 1, y + 2, x + w + 3, y + h - 2, GuiStyle.OUTLINE);
        context.fill(x + w + 1, y + 3, x + w + 2, y + h - 3, GuiStyle.STEEL);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF101316);
        int segments = (w - 2) / 4;
        int lit = level <= 0.0F ? 0 : Math.max(1, Math.round(GuiStyle.clamp(level) * segments));
        int colour = level > 0.5F ? GuiStyle.LED_GREEN : level > 0.2F ? GuiStyle.LED_AMBER : GuiStyle.LED_RED;
        for (int segment = 0; segment < segments; segment++) {
            int sx = x + 2 + segment * 4;
            context.fill(sx, y + 2, sx + 3, y + h - 2, segment < lit ? colour : GuiStyle.LED_OFF);
        }
    }

    /**
     * A small car side on, facing right, plugged in at its rear wing while it
     * stands at the post; a pale outline of one while the bay is empty.
     */
    private static void drawCar(GuiGraphics context, int x, int y, boolean parked, boolean charging) {
        float alpha = parked ? 1.0F : 0.28F;
        int body = GuiStyle.fade(0xFF3D78C2, alpha);
        int bodyLight = GuiStyle.fade(0xFF6FA0DC, alpha);
        int bodyDark = GuiStyle.fade(0xFF2A5488, alpha);
        int glass = GuiStyle.fade(0xFF9FC6DE, alpha);
        int glassDark = GuiStyle.fade(0xFF6E98B8, alpha);
        int tyre = GuiStyle.fade(0xFF202225, alpha);
        int rim = GuiStyle.fade(0xFFA8B0B8, alpha);
        int arch = GuiStyle.fade(0xFF15181C, alpha);

        if (parked) {
            context.fill(x + 4, y + 30, x + 88, y + 31, 0x50000000);
        }
        for (int row = 0; row <= 10; row++) {
            int rear = x + 20 - row * 8 / 10;
            int front = x + 54 + row * 16 / 10;
            context.fill(rear, y + 3 + row, front, y + 4 + row, row == 0 ? bodyLight : body);
        }
        int[][] sides = {{4, 88}, {1, 91}, {1, 91}, {1, 91}, {1, 91}, {1, 91}, {1, 91}, {1, 91}, {2, 90}, {4, 88}};
        for (int row = 0; row < sides.length; row++) {
            int colour = row == 0 ? bodyLight : row >= 8 ? bodyDark : body;
            context.fill(x + sides[row][0], y + 14 + row, x + sides[row][1], y + 15 + row, colour);
        }
        for (int row = 0; row < 7; row++) {
            int rear = x + 22 - row * 8 / 10;
            int front = x + 52 + row * 16 / 10;
            context.fill(rear, y + 5 + row, x + 36, y + 6 + row, row < 2 ? glassDark : glass);
            context.fill(x + 39, y + 5 + row, front, y + 6 + row, row < 2 ? glassDark : glass);
        }
        context.fill(x + 24, y + 7, x + 26, y + 8, GuiStyle.fade(0x90FFFFFF, alpha));
        context.fill(x + 43, y + 7, x + 46, y + 8, GuiStyle.fade(0x90FFFFFF, alpha));
        context.fill(x + 38, y + 15, x + 39, y + 22, bodyDark);
        context.fill(x + 42, y + 16, x + 45, y + 17, bodyLight);
        context.fill(x + 88, y + 16, x + 91, y + 18, GuiStyle.fade(0xFFF4E7A0, alpha));
        context.fill(x + 1, y + 16, x + 3, y + 18, GuiStyle.fade(0xFFD8402F, alpha));
        for (int wheel : new int[]{x + 18, x + 74}) {
            GuiStyle.disc(context, wheel, y + 24, 7, arch);
            GuiStyle.disc(context, wheel, y + 24, 6, tyre);
            GuiStyle.disc(context, wheel, y + 24, 3, rim);
            GuiStyle.dot(context, wheel, y + 24, tyre);
        }
        if (parked) {
            context.fill(x + 5, y + 15, x + 9, y + 19, GuiStyle.OUTLINE);
            context.fill(x + 6, y + 16, x + 8, y + 18, charging ? 0xFF5BE06A : 0xFF3A3F46);
            context.fill(x - 2, y + 15, x + 5, y + 21, GuiStyle.OUTLINE);
            context.fill(x - 1, y + 16, x + 4, y + 20, 0xFF3A3F46);
            context.fill(x - 1, y + 16, x + 4, y + 17, 0xFF5B636C);
        }
    }

    // ---- words ------------------------------------------------------------------------

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        int lamp = GuiStyle.lamp(context, this.font, this.menu.working(), this.imageWidth - 9);
        GuiStyle.title(context, this.font, this.title, lamp - GuiStyle.TITLE_X - 4);
        Component info = this.infoLine();
        if (info != null) {
            GuiStyle.readout(context, this.font, info, 8, READOUT_Y, this.imageWidth - 16);
        }
    }

    /** The one number that matters most for this machine, on the LCD above the inventory. */
    private Component infoLine() {
        long extra = this.menu.extra();
        return switch (this.kind()) {
            case GaugeSource.KIND_COAL_GENERATOR, GaugeSource.KIND_BIOFUEL_GENERATOR,
                 GaugeSource.KIND_DIESEL_GENERATOR -> Component.translatable("gui.overthought_vehicles.machine.runtime",
                    extra / 60, String.format("%02d", extra % 60), this.menu.get(MachineScreenHandler.EXTRA2));
            case GaugeSource.KIND_PUMPJACK -> Component.translatable("gui.overthought_vehicles.machine.reserve", extra);
            case GaugeSource.KIND_HYDROTREATER -> Component.translatable("gui.overthought_vehicles.machine.rate", extra);
            case GaugeSource.KIND_BATTERY -> Component.translatable("gui.overthought_vehicles.machine.flow",
                    this.menu.get(MachineScreenHandler.EXTRA2), extra);
            case GaugeSource.KIND_CHARGER -> this.menu.get(MachineScreenHandler.EXTRA2) == 0 && extra == 0
                    ? Component.translatable("gui.overthought_vehicles.machine.no_vehicle")
                    : Component.translatable("gui.overthought_vehicles.machine.vehicle_charge",
                            this.menu.get(MachineScreenHandler.EXTRA2) / 10);
            default -> null;
        };
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        this.renderBackground(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        for (Hit hit : this.hits) {
            if (x >= hit.x() && x < hit.x() + hit.width() && y >= hit.y() && y < hit.y() + hit.height()) {
                context.renderTooltip(this.font, hit.text().get(), mouseX, mouseY);
                break;
            }
        }
        this.renderTooltip(context, mouseX, mouseY);
    }

    private Component energyText() {
        return Component.translatable("tooltip.overthought_vehicles.energy", this.menu.energy(), this.menu.energyCapacity());
    }

    private Component tankText(int unitsIndex) {
        int units = this.menu.get(unitsIndex);
        int capacity = this.menu.get(unitsIndex + 1);
        int fluid = this.menu.get(unitsIndex + 2);
        Component name = units <= 0 || fluid <= 0
                ? Component.translatable("fluid.overthought_vehicles.nothing")
                : Component.translatable("fluid.overthought_vehicles." + BuiltInRegistries.FLUID.getKey(MachineScreenHandler.FLUIDS.get(fluid)).getPath());
        return Component.translatable("gui.overthought_vehicles.machine.tank", name, units, capacity);
    }
}
