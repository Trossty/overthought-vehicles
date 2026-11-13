package me.trossty.car.client.screen;

import me.trossty.car.block.entity.FuelRefineryBlockEntity;
import me.trossty.car.refinery.FuelRefineryScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The refinery window. Like the workbench's, it is drawn from plain rectangles
 * rather than a texture, so the mod ships no GUI image.
 *
 * <p>The two tanks are the point of the screen: biomass on the left, petrol on
 * the right, both reading top-down like a gauge. Everything else — the burner,
 * the two progress arrows — only exists to explain why those two levels move.
 */
public class FuelRefineryScreen extends AbstractContainerScreen<FuelRefineryScreenHandler> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF555555;
    private static final int SLOT_BODY = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int TEXT = 0xFF404040;

    private static final int TANK_WALL = 0xFF3A3A3A;
    private static final int TANK_EMPTY = 0xFF2A2A2A;
    private static final int BIOMASS = 0xFF5FA03A;
    private static final int BIOMASS_LIGHT = 0xFF86C75C;
    private static final int CRUDE = 0xFF1E1A16;
    private static final int CRUDE_LIGHT = 0xFF3A332B;
    private static final int RAW_PETROL = 0xFFB0702A;
    private static final int RAW_PETROL_LIGHT = 0xFFD08F45;
    private static final int RAW_DIESEL = 0xFF4E4224;
    private static final int RAW_DIESEL_LIGHT = 0xFF6B5C36;
    private static final int BIOFUEL = 0xFF9CC23A;
    private static final int BIOFUEL_LIGHT = 0xFFC2E266;
    private static final int ARROW = 0xFF6E6E6E;
    private static final int ARROW_LIT = 0xFF4A90D9;

    private static final int TANK_TOP = 15;
    private static final int TANK_HEIGHT = 54;
    private static final int TANK_WIDTH = 12;
    private static final int BIOMASS_TANK_X = 44;
    private static final int PETROL_TANK_X = 100;

    private static final int POWER = 0xFFD43C2E;
    private static final int POWER_LIGHT = 0xFFF2705F;
    private static final int COLD = 0xFF4A90D9;
    private static final int WARM = 0xFFE8A01B;
    private static final int HOT = 0xFFE8461B;
    private static final int MARK = 0xFFE6E6E6;
    private static final int TANK_MARK = 0x99E6E6E6;
    private static final int LAMP_ON = 0xFF3FBF4F;
    private static final int LAMP_OFF = 0xFF8B2A22;

    /** The energy buffer, drawn as a third tank at the right-hand edge. */
    private static final int ENERGY_X = 158;

    /** The column thermometer, under the catalyst it works on. */
    private static final int THERMO_X = 76;
    private static final int THERMO_Y = 39;
    private static final int THERMO_WIDTH = 5;
    private static final int THERMO_HEIGHT = 30;

    public FuelRefineryScreen(FuelRefineryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
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

        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            drawSlot(context, left + slot.x - 1, top + slot.y - 1);
        }

        this.drawThermometer(context);
        this.drawTank(context, left + ENERGY_X, top + TANK_TOP,
                (float) this.menu.getEnergy() / FuelRefineryBlockEntity.ENERGY_CAPACITY,
                POWER, POWER_LIGHT);
        drawArrow(context, left + 38, top + 21, this.menu.getDistillFraction());
        this.drawTank(context, left + BIOMASS_TANK_X, top + TANK_TOP,
                (float) this.menu.getBiomass() / FuelRefineryBlockEntity.BIOMASS_CAPACITY,
                this.menu.getFeed() == FuelRefineryBlockEntity.FEED_CRUDE ? CRUDE : BIOMASS,
                this.menu.getFeed() == FuelRefineryBlockEntity.FEED_CRUDE ? CRUDE_LIGHT : BIOMASS_LIGHT);
        drawArrow(context, left + 90, top + 21, this.menu.getRefineFraction());
        this.drawTank(context, left + PETROL_TANK_X, top + TANK_TOP,
                (float) this.menu.getPetrol() / FuelRefineryBlockEntity.PETROL_CAPACITY,
                switch (this.menu.getOutput()) {
                    case FuelRefineryBlockEntity.PRODUCT_RAW_DIESEL -> RAW_DIESEL;
                    case FuelRefineryBlockEntity.PRODUCT_BIOFUEL -> BIOFUEL;
                    default -> RAW_PETROL;
                },
                switch (this.menu.getOutput()) {
                    case FuelRefineryBlockEntity.PRODUCT_RAW_DIESEL -> RAW_DIESEL_LIGHT;
                    case FuelRefineryBlockEntity.PRODUCT_BIOFUEL -> BIOFUEL_LIGHT;
                    default -> RAW_PETROL_LIGHT;
                });
        drawArrow(context, left + 118, top + 39, this.menu.getPetrol() > 0 ? 1.0F : 0.0F);
    }

    /** A tank drains from the top, which is how a gauge is read. */
    private void drawTank(GuiGraphics context, int x, int y, float fill, int body, int highlight) {
        context.fill(x - 1, y - 1, x + TANK_WIDTH + 1, y + TANK_HEIGHT + 1, TANK_WALL);
        context.fill(x, y, x + TANK_WIDTH, y + TANK_HEIGHT, TANK_EMPTY);

        int filled = Math.round(Math.min(1.0F, Math.max(0.0F, fill)) * TANK_HEIGHT);
        if (filled > 0) {
            int surface = y + TANK_HEIGHT - filled;
            context.fill(x, surface, x + TANK_WIDTH, y + TANK_HEIGHT, body);
            context.fill(x, surface, x + TANK_WIDTH, surface + 1, highlight);
            context.fill(x + 1, surface, x + 3, y + TANK_HEIGHT, highlight);
        }
        // a gauge's marks, every quarter, over whatever is in it
        for (int quarter = 1; quarter < 4; quarter++) {
            int at = y + TANK_HEIGHT * quarter / 4;
            context.fill(x + TANK_WIDTH - 4, at, x + TANK_WIDTH, at + 1, TANK_MARK);
        }
    }

    /**
     * The column's temperature, with a tick where it starts to work and
     * another where it starts to work fast.
     */
    private void drawThermometer(GuiGraphics context) {
        int x = this.leftPos + THERMO_X;
        int y = this.topPos + THERMO_Y;
        context.fill(x - 1, y - 1, x + THERMO_WIDTH + 1, y + THERMO_HEIGHT + 1, TANK_WALL);
        context.fill(x, y, x + THERMO_WIDTH, y + THERMO_HEIGHT, TANK_EMPTY);

        int temperature = this.menu.getTemperature();
        int filled = Math.round((float) temperature
                / FuelRefineryBlockEntity.MAX_TEMPERATURE * THERMO_HEIGHT);
        if (filled > 0) {
            int colour = temperature >= FuelRefineryBlockEntity.HOT_TEMPERATURE ? HOT
                    : temperature >= FuelRefineryBlockEntity.WORKING_TEMPERATURE ? WARM : COLD;
            context.fill(x, y + THERMO_HEIGHT - filled, x + THERMO_WIDTH, y + THERMO_HEIGHT, colour);
        }
        for (int mark : new int[] {FuelRefineryBlockEntity.WORKING_TEMPERATURE,
                FuelRefineryBlockEntity.HOT_TEMPERATURE}) {
            int at = y + THERMO_HEIGHT
                    - Math.round((float) mark / FuelRefineryBlockEntity.MAX_TEMPERATURE * THERMO_HEIGHT);
            context.fill(x + THERMO_WIDTH + 1, at, x + THERMO_WIDTH + 3, at + 1, MARK);
        }
    }

    /** A chevron that fills left to right as its stage runs. */
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

        // Whether it is working, as the other machines say it: a lamp and a word.
        boolean on = this.menu.getDistillFraction() > 0.0F || this.menu.getRefineFraction() > 0.0F;
        Component state = Component.translatable(on ? "gui.car.machine.running" : "gui.car.machine.idle");
        int stateX = this.imageWidth - 8 - this.font.width(state);
        context.fill(stateX - 7, this.titleLabelY + 1, stateX - 2, this.titleLabelY + 6, on ? LAMP_ON : LAMP_OFF);
        context.drawString(this.font, state, stateX, this.titleLabelY, TEXT, false);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        this.renderBackground(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);
        this.drawTankTooltips(context, mouseX, mouseY);
        this.renderTooltip(context, mouseX, mouseY);
    }

    /** Hovering a tank says how much is in it, since a bar alone does not. */
    private void drawTankTooltips(GuiGraphics context, int mouseX, int mouseY) {
        if (overTank(mouseX, mouseY, this.leftPos + BIOMASS_TANK_X)) {
            context.renderTooltip(this.font,
                    Component.translatable(this.menu.getFeed() == FuelRefineryBlockEntity.FEED_CRUDE
                                    && this.menu.getBiomass() > 0 ? "tooltip.car.crude" : "tooltip.car.biomass",
                            this.menu.getBiomass(), FuelRefineryBlockEntity.BIOMASS_CAPACITY),
                    mouseX, mouseY);
        } else if (overTank(mouseX, mouseY, this.leftPos + ENERGY_X)) {
            context.renderTooltip(this.font,
                    Component.translatable("tooltip.car.energy",
                            this.menu.getEnergy(), FuelRefineryBlockEntity.ENERGY_CAPACITY),
                    mouseX, mouseY);
        } else if (mouseX >= this.leftPos + THERMO_X - 1 && mouseX < this.leftPos + THERMO_X + THERMO_WIDTH + 3
                && mouseY >= this.topPos + THERMO_Y && mouseY < this.topPos + THERMO_Y + THERMO_HEIGHT) {
            int temperature = this.menu.getTemperature();
            String state = temperature >= FuelRefineryBlockEntity.HOT_TEMPERATURE ? "hot"
                    : temperature >= FuelRefineryBlockEntity.WORKING_TEMPERATURE ? "working" : "cold";
            context.renderTooltip(this.font,
                    Component.translatable("tooltip.car.column_" + state, 20 + temperature * 2 / 5),
                    mouseX, mouseY);
        } else if (overTank(mouseX, mouseY, this.leftPos + PETROL_TANK_X)) {
            context.renderTooltip(this.font,
                    Component.translatable(this.menu.getPetrol() == 0 ? "tooltip.car.product_empty"
                                    : switch (this.menu.getOutput()) {
                                        case FuelRefineryBlockEntity.PRODUCT_RAW_DIESEL -> "tooltip.car.raw_diesel";
                                        case FuelRefineryBlockEntity.PRODUCT_BIOFUEL -> "tooltip.car.biofuel";
                                        default -> "tooltip.car.raw_petrol";
                                    },
                            this.menu.getPetrol(), FuelRefineryBlockEntity.PETROL_CAPACITY),
                    mouseX, mouseY);
        }
    }

    private boolean overTank(int mouseX, int mouseY, int tankX) {
        return mouseX >= tankX && mouseX < tankX + TANK_WIDTH
                && mouseY >= this.topPos + TANK_TOP && mouseY < this.topPos + TANK_TOP + TANK_HEIGHT;
    }
}
