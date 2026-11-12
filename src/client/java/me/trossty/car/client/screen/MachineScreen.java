package me.trossty.car.client.screen;

import me.trossty.car.machine.GaugeSource;
import me.trossty.car.machine.MachineScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The gauge window every plain machine shares. Drawn from rectangles like the
 * refinery's, so the mod still ships no GUI texture.
 *
 * <p>Left to right it reads as the machine works: what goes in (a firebox or
 * a tank), an arrow that fills while it runs, what comes out (a second tank),
 * and the energy bar on the right edge. A lamp by the title says whether it is
 * running, and one line above the inventory gives the number that matters most
 * for that machine — time left on the fuel, crude left in the ground, charge.
 */
public class MachineScreen extends AbstractContainerScreen<MachineScreenHandler> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF555555;
    private static final int SLOT_BODY = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int TEXT = 0xFF404040;
    private static final int TANK_WALL = 0xFF3A3A3A;
    private static final int TANK_EMPTY = 0xFF2A2A2A;
    private static final int ARROW = 0xFF6E6E6E;
    private static final int ARROW_LIT = 0xFF4A90D9;
    private static final int POWER = 0xFFD43C2E;
    private static final int POWER_LIGHT = 0xFFF2705F;
    private static final int LAMP_ON = 0xFF3FC24A;
    private static final int LAMP_OFF = 0xFF6E1F1F;
    private static final int FLAME = 0xFFE8861B;
    private static final int FLAME_TIP = 0xFFF8D040;

    /** Body and highlight colours by the fluid numbers {@link MachineScreenHandler#FLUIDS} sends. */
    private static final int[][] FLUID_COLOURS = {
            {0xFF2A2A2A, 0xFF2A2A2A},
            {0xFF1E1A16, 0xFF3A332B},
            {0xFFB0702A, 0xFFD08F45},
            {0xFF4E4224, 0xFF6B5C36},
            {0xFFD9B23A, 0xFFF0D267},
            {0xFF8A6A2C, 0xFFAE8B48},
            {0xFF9CC23A, 0xFFC2E266},
    };

    private static final int TANK_TOP = 17;
    private static final int TANK_HEIGHT = 50;
    private static final int TANK_WIDTH = 14;
    private static final int TANK_A_X = 52;
    private static final int TANK_B_X = 106;
    private static final int ENERGY_X = 152;
    private static final int BATTERY_X = 70;
    private static final int BATTERY_WIDTH = 36;

    public MachineScreen(MachineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private boolean isGenerator() {
        int kind = this.menu.kind();
        return kind == GaugeSource.KIND_COAL_GENERATOR || kind == GaugeSource.KIND_BIOFUEL_GENERATOR
                || kind == GaugeSource.KIND_DIESEL_GENERATOR;
    }

    @Override
    protected void renderBg(GuiGraphics context, float deltaTicks, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        int right = left + this.imageWidth;
        int bottom = top + this.imageHeight;
        context.fill(left, top, right, bottom, PANEL);
        context.fill(left, top, right, top + 1, PANEL_LIGHT);
        context.fill(left, top, left + 1, bottom, PANEL_LIGHT);
        context.fill(left, bottom - 1, right, bottom, PANEL_SHADOW);
        context.fill(right - 1, top, right, bottom, PANEL_SHADOW);
        for (Slot slot : this.menu.slots) {
            drawSlot(context, left + slot.x - 1, top + slot.y - 1);
        }

        int kind = this.menu.kind();
        float energy = fraction(this.menu.energy(), this.menu.energyCapacity());
        if (kind == GaugeSource.KIND_BATTERY) {
            drawBar(context, left + BATTERY_X, top + TANK_TOP, BATTERY_WIDTH, energy, POWER, POWER_LIGHT);
            drawArrow(context, left + 40, top + 36, this.menu.get(MachineScreenHandler.EXTRA2) > 0 ? 1.0F : 0.0F);
            drawArrow(context, left + 120, top + 36, this.menu.working() ? 1.0F : 0.0F);
            return;
        }
        drawBar(context, left + ENERGY_X, top + TANK_TOP, TANK_WIDTH, energy, POWER, POWER_LIGHT);

        if (this.isGenerator()) {
            drawFlame(context, left + 27, top + 17, this.menu.working() ? this.menu.progress() : 0.0F);
            if (this.menu.get(MachineScreenHandler.A_FLUID) >= 0) {
                this.drawTank(context, left + TANK_A_X, MachineScreenHandler.A_UNITS);
            }
            drawArrow(context, left + 124, top + 36, this.menu.working() ? 1.0F : 0.0F);
            return;
        }
        if (kind == GaugeSource.KIND_CHARGER) {
            // the car's battery, as a second bar beside the post's own
            float car = this.menu.get(MachineScreenHandler.EXTRA2) / 1000.0F;
            drawBar(context, left + TANK_B_X, top + TANK_TOP, TANK_WIDTH, car, 0xFF3FC24A, 0xFF7BE083);
            drawArrow(context, left + 128, top + 36, this.menu.working() ? 1.0F : 0.0F);
            return;
        }
        if (this.menu.get(MachineScreenHandler.A_FLUID) >= 0) {
            this.drawTank(context, left + TANK_A_X, MachineScreenHandler.A_UNITS);
        }
        if (this.menu.get(MachineScreenHandler.B_FLUID) >= 0) {
            drawArrow(context, left + 80, top + 36, this.menu.progress());
            this.drawTank(context, left + TANK_B_X, MachineScreenHandler.B_UNITS);
        } else {
            // the pumpjack: the ground it draws from, then its own tank
            drawArrow(context, left + 28, top + 36, this.menu.progress());
            drawGround(context, left + 10, top + TANK_TOP);
        }
    }

    private void drawTank(GuiGraphics context, int x, int unitsIndex) {
        int units = this.menu.get(unitsIndex);
        int capacity = this.menu.get(unitsIndex + 1);
        int fluid = Math.max(0, Math.min(FLUID_COLOURS.length - 1, this.menu.get(unitsIndex + 2)));
        drawBar(context, x, this.topPos + TANK_TOP, TANK_WIDTH, fraction(units, capacity),
                FLUID_COLOURS[fluid][0], FLUID_COLOURS[fluid][1]);
    }

    private static float fraction(long amount, long capacity) {
        return capacity <= 0 ? 0.0F : Math.min(1.0F, Math.max(0.0F, (float) amount / capacity));
    }

    /** A bar fills from the bottom, which is how a gauge is read. */
    private static void drawBar(GuiGraphics context, int x, int y, int width, float fill, int body, int highlight) {
        context.fill(x - 1, y - 1, x + width + 1, y + TANK_HEIGHT + 1, TANK_WALL);
        context.fill(x, y, x + width, y + TANK_HEIGHT, TANK_EMPTY);
        int filled = Math.round(fill * TANK_HEIGHT);
        if (filled <= 0) {
            return;
        }
        int surface = y + TANK_HEIGHT - filled;
        context.fill(x, surface, x + width, y + TANK_HEIGHT, body);
        context.fill(x, surface, x + width, surface + 1, highlight);
        context.fill(x + 1, surface, x + 3, y + TANK_HEIGHT, highlight);
    }

    /** A stack of strata with an oil pocket, for the pumpjack's "where it comes from". */
    private static void drawGround(GuiGraphics context, int x, int y) {
        context.fill(x - 1, y - 1, x + 15, y + TANK_HEIGHT + 1, TANK_WALL);
        context.fill(x, y, x + 14, y + 8, 0xFF5E9E3A);
        context.fill(x, y + 8, x + 14, y + 24, 0xFF7A5A3A);
        context.fill(x, y + 24, x + 14, y + TANK_HEIGHT, 0xFF6B6B6B);
        context.fill(x + 3, y + 34, x + 11, y + 44, 0xFF1E1A16);
        context.fill(x + 6, y, x + 8, y + 36, 0xFF3A3A3A);
    }

    /** A flame that shrinks as the piece burning runs out. */
    private static void drawFlame(GuiGraphics context, int x, int y, float left) {
        int height = 14;
        context.fill(x, y, x + 14, y + height, TANK_EMPTY);
        int lit = Math.round(left * height);
        if (lit <= 0) {
            return;
        }
        int topY = y + height - lit;
        context.fill(x + 2, topY + lit / 3, x + 12, y + height, FLAME);
        context.fill(x + 5, topY, x + 9, y + height, FLAME_TIP);
    }

    private static void drawArrow(GuiGraphics context, int x, int y, float progress) {
        int width = 16;
        context.fill(x, y + 3, x + width, y + 7, ARROW);
        context.fill(x + width - 4, y + 1, x + width - 3, y + 9, ARROW);
        context.fill(x + width - 3, y + 2, x + width - 2, y + 8, ARROW);
        context.fill(x + width - 2, y + 3, x + width - 1, y + 7, ARROW);
        int lit = Math.round(Math.min(1.0F, Math.max(0.0F, progress)) * width);
        if (lit > 0) {
            context.fill(x, y + 4, x + lit, y + 6, ARROW_LIT);
        }
    }

    private static void drawSlot(GuiGraphics context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, SLOT_BODY);
        context.fill(x, y, x + 18, y + 1, SLOT_DARK);
        context.fill(x, y, x + 1, y + 18, SLOT_DARK);
        context.fill(x + 17, y + 1, x + 18, y + 18, PANEL_LIGHT);
        context.fill(x + 1, y + 17, x + 18, y + 18, PANEL_LIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        context.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        context.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, TEXT, false);

        boolean on = this.menu.working();
        Component state = Component.translatable(on ? "gui.car.machine.running" : "gui.car.machine.idle");
        int stateWidth = this.font.width(state);
        int stateX = this.imageWidth - 8 - stateWidth;
        context.fill(stateX - 7, this.titleLabelY + 1, stateX - 2, this.titleLabelY + 6, on ? LAMP_ON : LAMP_OFF);
        context.drawString(this.font, state, stateX, this.titleLabelY, TEXT, false);

        Component info = this.infoLine();
        if (info != null) {
            context.drawString(this.font, info,
                    this.imageWidth - 8 - this.font.width(info), this.inventoryLabelY, TEXT, false);
        }
    }

    /** The one number that matters most for this machine, above the inventory on the right. */
    private Component infoLine() {
        long extra = this.menu.extra();
        return switch (this.menu.kind()) {
            case GaugeSource.KIND_COAL_GENERATOR, GaugeSource.KIND_BIOFUEL_GENERATOR,
                 GaugeSource.KIND_DIESEL_GENERATOR -> Component.translatable("gui.car.machine.runtime",
                    extra / 60, String.format("%02d", extra % 60), this.menu.get(MachineScreenHandler.EXTRA2));
            case GaugeSource.KIND_PUMPJACK -> Component.translatable("gui.car.machine.reserve", extra);
            case GaugeSource.KIND_HYDROTREATER -> Component.translatable("gui.car.machine.rate", extra);
            case GaugeSource.KIND_BATTERY -> Component.translatable("gui.car.machine.flow",
                    this.menu.get(MachineScreenHandler.EXTRA2), extra);
            case GaugeSource.KIND_CHARGER -> this.menu.get(MachineScreenHandler.EXTRA2) == 0 && extra == 0
                    ? Component.translatable("gui.car.machine.no_vehicle")
                    : Component.translatable("gui.car.machine.vehicle_charge",
                            this.menu.get(MachineScreenHandler.EXTRA2) / 10);
            default -> null;
        };
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        this.renderBackground(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);
        this.drawGaugeTooltips(context, mouseX, mouseY);
        this.renderTooltip(context, mouseX, mouseY);
    }

    private void drawGaugeTooltips(GuiGraphics context, int mouseX, int mouseY) {
        int kind = this.menu.kind();
        int energyX = kind == GaugeSource.KIND_BATTERY ? BATTERY_X : ENERGY_X;
        int energyWidth = kind == GaugeSource.KIND_BATTERY ? BATTERY_WIDTH : TANK_WIDTH;
        if (this.over(mouseX, mouseY, energyX, energyWidth)) {
            context.renderTooltip(this.font, Component.translatable("tooltip.car.energy",
                    this.menu.energy(), this.menu.energyCapacity()), mouseX, mouseY);
            return;
        }
        if (kind == GaugeSource.KIND_BATTERY || kind == GaugeSource.KIND_CHARGER) {
            return;
        }
        if (this.menu.get(MachineScreenHandler.A_FLUID) >= 0 && this.over(mouseX, mouseY, TANK_A_X, TANK_WIDTH)) {
            context.renderTooltip(this.font, this.tankText(MachineScreenHandler.A_UNITS), mouseX, mouseY);
        } else if (this.menu.get(MachineScreenHandler.B_FLUID) >= 0
                && this.over(mouseX, mouseY, TANK_B_X, TANK_WIDTH)) {
            context.renderTooltip(this.font, this.tankText(MachineScreenHandler.B_UNITS), mouseX, mouseY);
        }
    }

    private Component tankText(int unitsIndex) {
        int units = this.menu.get(unitsIndex);
        int capacity = this.menu.get(unitsIndex + 1);
        int fluid = this.menu.get(unitsIndex + 2);
        Component name = units <= 0 || fluid <= 0
                ? Component.translatable("fluid.car.nothing")
                : Component.translatable("fluid.car." + BuiltInRegistries.FLUID.getKey(MachineScreenHandler.FLUIDS.get(fluid)).getPath());
        return Component.translatable("gui.car.machine.tank", name, units, capacity);
    }

    private boolean over(int mouseX, int mouseY, int barX, int width) {
        return mouseX >= this.leftPos + barX && mouseX < this.leftPos + barX + width
                && mouseY >= this.topPos + TANK_TOP && mouseY < this.topPos + TANK_TOP + TANK_HEIGHT;
    }
}
