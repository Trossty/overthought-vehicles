package me.trossty.car.client.screen;

import me.trossty.car.block.entity.FuelRefineryBlockEntity;
import me.trossty.car.refinery.FuelRefineryScreenHandler;
import me.trossty.car.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The refinery's control panel, read left to right as the plant works:
 * organic matter (or a bucket of crude) piped into the feed tank, the feed
 * piped into the column under its catalyst hopper, the column glowing as hot
 * as it is with its working and hot marks beside it, the fuel it makes in the
 * product tank, and that piped on into a jerry can, with tar dropping out
 * under the can. The energy store stands at the right edge, and the LCD says
 * how hot the column is and what that means.
 *
 * <p>Like every window of the mod it is drawn from rectangles, so the mod
 * ships no GUI texture. An empty slot shows a faded picture of what goes in it.
 */
public class FuelRefineryScreen extends AbstractContainerScreen<FuelRefineryScreenHandler> {

    private static final int BIOMASS = 0xFF5FA03A;
    private static final int BIOMASS_LIGHT = 0xFF86C75C;
    private static final int CRUDE = 0xFF1E1A16;
    private static final int CRUDE_LIGHT = 0xFF5A4E3E;
    private static final int RAW_PETROL = 0xFFB0702A;
    private static final int RAW_PETROL_LIGHT = 0xFFD08F45;
    private static final int RAW_DIESEL = 0xFF4E4224;
    private static final int RAW_DIESEL_LIGHT = 0xFF7A6A40;
    private static final int BIOFUEL = 0xFF9CC23A;
    private static final int BIOFUEL_LIGHT = 0xFFC2E266;

    private static final int COLD = 0xFF3F6E9E;
    private static final int WARM = 0xFFE8A01B;
    private static final int HOT = 0xFFE8461B;

    private static final int PLATE_X = 7;
    private static final int PLATE_Y = 17;
    private static final int PLATE_HEIGHT = 66;
    private static final int GAUGE_TOP = 22;
    private static final int GAUGE_HEIGHT = 44;
    private static final int CAPTION_Y = 70;
    private static final int READOUT_Y = 87;
    private static final int TANK_WIDTH = 18;
    private static final int FEED_X = 43;
    private static final int PRODUCT_X = 109;
    /** The column under the catalyst hopper, which is also the plant's thermometer. */
    private static final int COLUMN_X = 79;
    private static final int COLUMN_Y = 42;
    private static final int COLUMN_WIDTH = 14;
    private static final int COLUMN_HEIGHT = 24;
    private static final int ENERGY_X = 168;
    private static final int ENERGY_Y = 23;
    private static final int ENERGY_WIDTH = 14;

    public FuelRefineryScreen(FuelRefineryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 196;
        this.imageHeight = 184;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private boolean crude() {
        return this.menu.getFeed() == FuelRefineryBlockEntity.FEED_CRUDE && this.menu.getBiomass() > 0;
    }

    private int[] productColour() {
        return switch (this.menu.getOutput()) {
            case FuelRefineryBlockEntity.PRODUCT_RAW_DIESEL -> new int[]{RAW_DIESEL, RAW_DIESEL_LIGHT};
            case FuelRefineryBlockEntity.PRODUCT_BIOFUEL -> new int[]{BIOFUEL, BIOFUEL_LIGHT};
            default -> new int[]{RAW_PETROL, RAW_PETROL_LIGHT};
        };
    }

    @Override
    protected void renderBg(GuiGraphics context, float deltaTicks, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        GuiStyle.frame(context, left, top, this.imageWidth, this.imageHeight);
        GuiStyle.plate(context, left + PLATE_X, top + PLATE_Y, this.imageWidth - 2 * PLATE_X, PLATE_HEIGHT);

        float distill = this.menu.getDistillFraction();
        float refine = this.menu.getRefineFraction();
        int feedBody = this.crude() ? CRUDE : BIOMASS;
        int feedLight = this.crude() ? CRUDE_LIGHT : BIOMASS_LIGHT;
        int[] product = this.productColour();

        // what goes in, and the feed tank it goes into
        GuiStyle.flow(context, left + 30, top + slotY(FuelRefineryScreenHandler.ORGANIC_Y), 13, BIOMASS_LIGHT,
                distill, distill > 0.0F);
        GuiStyle.flow(context, left + 30, top + slotY(FuelRefineryScreenHandler.HEAT_Y), 13, CRUDE_LIGHT,
                0.0F, false);
        GuiStyle.tank(context, left + FEED_X, top + GAUGE_TOP, TANK_WIDTH, GAUGE_HEIGHT,
                (float) this.menu.getBiomass() / FuelRefineryBlockEntity.BIOMASS_CAPACITY,
                feedBody, feedLight, distill > 0.0F);
        this.caption(context, "feed", left + FEED_X + TANK_WIDTH / 2, top, 32);

        // the column under its catalyst hopper
        GuiStyle.flow(context, left + FEED_X + TANK_WIDTH, top + 44, COLUMN_X - FEED_X - TANK_WIDTH,
                feedLight, refine, refine > 0.0F);
        GuiStyle.flow(context, left + COLUMN_X + COLUMN_WIDTH, top + 44, PRODUCT_X - COLUMN_X - COLUMN_WIDTH,
                product[1], refine, refine > 0.0F);
        this.drawColumn(context, left, top, refine > 0.0F);
        this.caption(context, "column", left + COLUMN_X + COLUMN_WIDTH / 2, top, 36);

        // what it makes, and the can it goes into
        GuiStyle.tank(context, left + PRODUCT_X, top + GAUGE_TOP, TANK_WIDTH, GAUGE_HEIGHT,
                (float) this.menu.getPetrol() / FuelRefineryBlockEntity.PETROL_CAPACITY,
                product[0], product[1], refine > 0.0F);
        this.caption(context, "fuel", left + PRODUCT_X + TANK_WIDTH / 2, top, 32);
        boolean pouring = this.menu.getPetrol() > 0 && this.menu.getSlot(FuelRefineryBlockEntity.CAN_SLOT).hasItem();
        GuiStyle.flow(context, left + PRODUCT_X + TANK_WIDTH, top + slotY(FuelRefineryScreenHandler.CAN_Y),
                FuelRefineryScreenHandler.CAN_X - 1 - PRODUCT_X - TANK_WIDTH, product[1],
                pouring ? 1.0F : 0.0F, pouring);

        GuiStyle.energy(context, left + ENERGY_X, top + ENERGY_Y, ENERGY_WIDTH, GAUGE_TOP + GAUGE_HEIGHT - ENERGY_Y,
                (float) this.menu.getEnergy() / FuelRefineryBlockEntity.ENERGY_CAPACITY);
        GuiStyle.bolt(context, left + ENERGY_X + ENERGY_WIDTH / 2 - 2, top + CAPTION_Y);

        for (Slot slot : this.menu.slots) {
            GuiStyle.slot(context, left + slot.x - 1, top + slot.y - 1);
        }
        this.ghost(context, FuelRefineryBlockEntity.ORGANIC_SLOT, new ItemStack(ModItems.RAPESEED));
        this.ghost(context, FuelRefineryBlockEntity.OIL_SLOT, new ItemStack(Items.BUCKET));
        this.ghost(context, FuelRefineryBlockEntity.CATALYST_SLOT, new ItemStack(Items.REDSTONE));
        this.ghost(context, FuelRefineryBlockEntity.CAN_SLOT, new ItemStack(ModItems.JERRY_CAN));
        this.ghost(context, FuelRefineryBlockEntity.TAR_SLOT, new ItemStack(ModItems.TAR));
    }

    /** The top of an eight-high pipe level with the middle of a slot at {@code slotY}. */
    private static int slotY(int slotY) {
        return slotY + 4;
    }

    private void ghost(GuiGraphics context, int index, ItemStack stack) {
        Slot slot = this.menu.getSlot(index);
        if (!slot.hasItem()) {
            GuiStyle.ghost(context, stack, this.leftPos + slot.x, this.topPos + slot.y);
        }
    }

    private void caption(GuiGraphics context, String key, int centre, int top, int room) {
        GuiStyle.caption(context, this.font, Component.translatable("gui.car.caption." + key), centre,
                top + CAPTION_Y, room);
    }

    /**
     * The catalyst hopper over the column, and the column itself: its trays
     * behind a glass that glows from the bottom up as far as it is hot, blue
     * when cold, amber once it works, red once it works fast, with a mark at
     * each of those two heats and vapour rising while it cracks.
     */
    private void drawColumn(GuiGraphics context, int left, int top, boolean cracking) {
        int hx = left + FuelRefineryScreenHandler.CATALYST_X - 3;
        int hy = top + FuelRefineryScreenHandler.CATALYST_Y - 3;
        GuiStyle.casing(context, hx, hy, 22, 22);
        context.fill(hx + 2, hy + 20, hx + 20, hy + 22, GuiStyle.STEEL_DARK);

        int x = left + COLUMN_X;
        int y = top + COLUMN_Y;
        GuiStyle.casing(context, x, y, COLUMN_WIDTH, COLUMN_HEIGHT);
        int gx = x + 2;
        int gy = y + 1;
        int gw = COLUMN_WIDTH - 4;
        int gh = COLUMN_HEIGHT - 2;
        context.fill(gx, gy, gx + gw, gy + gh, GuiStyle.GLASS_BACK);
        int temperature = this.menu.getTemperature();
        int heat = Math.round((float) temperature / FuelRefineryBlockEntity.MAX_TEMPERATURE * gh);
        if (heat > 0) {
            int colour = temperature >= FuelRefineryBlockEntity.HOT_TEMPERATURE ? HOT
                    : temperature >= FuelRefineryBlockEntity.WORKING_TEMPERATURE ? WARM : COLD;
            context.fillGradient(gx, gy + gh - heat, gx + gw, gy + gh, GuiStyle.darker(colour), colour);
        }
        for (int tray = gy + 3; tray < gy + gh; tray += 4) {
            context.fill(gx, tray, gx + gw, tray + 1, 0x60000000);
            context.fill(gx + (tray / 4 % 2 == 0 ? 0 : 3), tray, gx + gw - (tray / 4 % 2 == 0 ? 3 : 0), tray + 1,
                    0xA0656C74);
        }
        if (cracking) {
            float time = GuiStyle.ticks();
            for (int bubble = 0; bubble < 3; bubble++) {
                int rise = (int) ((time * 0.5F + bubble * gh / 3.0F) % gh);
                GuiStyle.dot(context, gx + 2 + bubble * 3, gy + gh - 1 - rise, 0x90FFFFFF);
            }
        }
        for (int mark : new int[]{FuelRefineryBlockEntity.WORKING_TEMPERATURE, FuelRefineryBlockEntity.HOT_TEMPERATURE}) {
            int at = gy + gh - Math.round((float) mark / FuelRefineryBlockEntity.MAX_TEMPERATURE * gh);
            context.fill(gx + gw - 3, at, gx + gw, at + 1, mark == FuelRefineryBlockEntity.HOT_TEMPERATURE
                    ? 0xFFFF8A6A : 0xFFFFE08A);
        }
        context.fill(gx + 1, gy + 1, gx + 2, gy + gh - 1, 0x30FFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        boolean on = this.menu.getDistillFraction() > 0.0F || this.menu.getRefineFraction() > 0.0F;
        int lamp = GuiStyle.lamp(context, this.font, on, this.imageWidth - 9);
        GuiStyle.title(context, this.font, this.title, lamp - GuiStyle.TITLE_X - 4);
        GuiStyle.readout(context, this.font, Component.translatable("gui.car.refinery.column",
                this.celsius(), Component.translatable("gui.car.refinery." + this.columnState())),
                8, READOUT_Y, this.imageWidth - 16);
    }

    private int celsius() {
        return 20 + this.menu.getTemperature() * 2 / 5;
    }

    private String columnState() {
        int temperature = this.menu.getTemperature();
        return temperature >= FuelRefineryBlockEntity.HOT_TEMPERATURE ? "hot"
                : temperature >= FuelRefineryBlockEntity.WORKING_TEMPERATURE ? "working" : "cold";
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        this.renderBackground(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);
        this.drawGaugeTooltips(context, mouseX, mouseY);
        this.renderTooltip(context, mouseX, mouseY);
    }

    /** Hovering a gauge says what is in it, since a level alone does not. */
    private void drawGaugeTooltips(GuiGraphics context, int mouseX, int mouseY) {
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        Component text = null;
        if (over(x, y, FEED_X, GAUGE_TOP, TANK_WIDTH, GAUGE_HEIGHT)) {
            text = Component.translatable(this.crude() ? "tooltip.car.crude" : "tooltip.car.biomass",
                    this.menu.getBiomass(), FuelRefineryBlockEntity.BIOMASS_CAPACITY);
        } else if (over(x, y, COLUMN_X, COLUMN_Y, COLUMN_WIDTH, COLUMN_HEIGHT)) {
            text = Component.translatable("tooltip.car.column_" + this.columnState(), this.celsius());
        } else if (over(x, y, PRODUCT_X, GAUGE_TOP, TANK_WIDTH, GAUGE_HEIGHT)) {
            text = Component.translatable(this.menu.getPetrol() == 0 ? "tooltip.car.product_empty"
                            : switch (this.menu.getOutput()) {
                                case FuelRefineryBlockEntity.PRODUCT_RAW_DIESEL -> "tooltip.car.raw_diesel";
                                case FuelRefineryBlockEntity.PRODUCT_BIOFUEL -> "tooltip.car.biofuel";
                                default -> "tooltip.car.raw_petrol";
                            },
                    this.menu.getPetrol(), FuelRefineryBlockEntity.PETROL_CAPACITY);
        } else if (over(x, y, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, GAUGE_TOP + GAUGE_HEIGHT - ENERGY_Y)) {
            text = Component.translatable("tooltip.car.energy",
                    this.menu.getEnergy(), FuelRefineryBlockEntity.ENERGY_CAPACITY);
        }
        if (text != null) {
            context.renderTooltip(this.font, text, mouseX, mouseY);
        }
    }

    private static boolean over(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
