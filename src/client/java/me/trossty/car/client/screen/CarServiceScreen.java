package me.trossty.car.client.screen;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.service.CarServiceScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * The service window: the whole car on one page.
 *
 * <p>The list is the car as it comes apart. Each part is indented under the one
 * that is bolted on top of it, so the order you have to work in is the order
 * you read in — the clutch sits two steps in from the driveshaft because that
 * is how many things have to come off before you can touch it.
 *
 * <p>Every number here is read straight off the car entity, which the client
 * already has: the window only ever had to be told which car it is about.
 */
public class CarServiceScreen extends AbstractContainerScreen<CarServiceScreenHandler> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF555555;
    private static final int WELL = 0xFF373737;
    private static final int WELL_LIGHT = 0xFF8B8B8B;
    private static final int TEXT = 0xFF404040;
    private static final int TEXT_ON_WELL = 0xFFE0E0E0;
    private static final int TEXT_DIM = 0xFF9A9A9A;
    private static final int SELECTED = 0xFF4A90D9;

    private static final int GOOD = 0xFF5BA85B;
    private static final int FAIR = 0xFFD9A441;
    private static final int BAD = 0xFFC0503F;
    private static final int ABSENT = 0xFF6E6E6E;
    private static final int HOT = 0xFFD94F3D;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 20;
    private static final int LIST_WIDTH = 150;
    private static final int ROW_HEIGHT = 13;
    private static final int VISIBLE_ROWS = 11;
    private static final int LIST_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 5;

    private static final int DETAIL_X = 166;
    private static final int DETAIL_WIDTH = 82;

    /** One line of the list: a system heading, or a part. */
    private record Line(CarPart.Group group, CarPart part) {

        boolean isHeading() {
            return this.part == null;
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private int scroll;
    private Button removeButton;
    private Button fitButton;

    public CarServiceScreen(CarServiceScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 204;
        // Only what this vehicle has: a trailer lists its running gear and no engine.
        me.trossty.car.entity.CarEntity car = handler.car();
        for (CarPart.Group group : CarPart.Group.values()) {
            List<Line> parts = new ArrayList<>();
            for (CarPart part : CarPart.of(group)) {
                if (car == null || car.hasPart(part)) {
                    parts.add(new Line(group, part));
                }
            }
            if (!parts.isEmpty()) {
                this.lines.add(new Line(group, null));
                this.lines.addAll(parts);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        // There is no player inventory in this window, so neither title belongs
        // where HandledScreen would put them.
        this.titleLabelY = 7;
        this.inventoryLabelY = -1000;
        this.removeButton = Button.builder(
                        Component.translatable("gui.car.remove"), button -> this.send(CarServiceScreenHandler.REMOVE_BUTTON))
                .bounds(this.leftPos + DETAIL_X, this.topPos + 146, DETAIL_WIDTH, 16)
                .build();
        this.fitButton = Button.builder(
                        Component.translatable("gui.car.fit"), button -> this.send(CarServiceScreenHandler.FIT_BUTTON))
                .bounds(this.leftPos + DETAIL_X, this.topPos + 164, DETAIL_WIDTH, 16)
                .build();
        this.addRenderableWidget(this.removeButton);
        this.addRenderableWidget(this.fitButton);
    }

    private void send(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    // ---- the list ----

    private int maxScroll() {
        return Math.max(0, this.lines.size() - VISIBLE_ROWS);
    }

    /** Where line {@code index} is drawn, or Integer.MIN_VALUE when hidden. */
    private int rowY(int index) {
        int row = index - this.scroll;
        if (row < 0 || row >= VISIBLE_ROWS) {
            return Integer.MIN_VALUE;
        }
        return this.topPos + LIST_Y + row * ROW_HEIGHT;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0.0 && this.maxScroll() > 0) {
            this.scroll = Mth.clamp(
                    this.scroll - (int) Math.signum(vertical), 0, this.maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int index = 0; index < this.lines.size(); index++) {
            Line line = this.lines.get(index);
            if (line.isHeading()) {
                continue;
            }
            int rowY = this.rowY(index);
            if (rowY == Integer.MIN_VALUE) {
                continue;
            }
            if (mouseX >= this.leftPos + LIST_X && mouseX < this.leftPos + LIST_X + LIST_WIDTH
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                this.send(line.part().ordinal());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        CarEntity car = this.menu.car();
        CarPart selected = this.menu.selectedPart();
        this.removeButton.active = car != null && car.isFitted(selected) && car.canReach(selected);
        this.fitButton.active = car != null && car.canReach(selected)
                && !this.menu.spareFor(selected).isEmpty()
                && car.getCondition(selected) < CarPart.MAX_CONDITION;
        super.render(context, mouseX, mouseY, deltaTicks);
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

        CarEntity car = this.menu.car();
        this.drawList(context, car);
        this.drawDetail(context, car);
        this.drawFooter(context, car);
    }

    /** A recessed well, the way vanilla frames anything you read out of. */
    private static void drawWell(GuiGraphics context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, WELL);
        context.fill(x, y, x + width, y + 1, PANEL_SHADOW);
        context.fill(x, y, x + 1, y + height, PANEL_SHADOW);
        context.fill(x + width - 1, y + 1, x + width, y + height, PANEL_LIGHT);
        context.fill(x + 1, y + height - 1, x + width, y + height, PANEL_LIGHT);
    }

    /** Green while there is plenty left, amber when it is going, red when it is gone. */
    private static int conditionColour(float fraction, boolean fitted) {
        if (!fitted) {
            return ABSENT;
        }
        if (fraction > 0.60F) {
            return GOOD;
        }
        return fraction > 0.25F ? FAIR : BAD;
    }

    private void drawList(GuiGraphics context, CarEntity car) {
        int listLeft = this.leftPos + LIST_X;
        int listTop = this.topPos + LIST_Y;
        drawWell(context, listLeft - 1, listTop - 1, LIST_WIDTH + 2, LIST_HEIGHT + 2);

        this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
        CarPart selected = this.menu.selectedPart();

        for (int index = 0; index < this.lines.size(); index++) {
            int rowY = this.rowY(index);
            if (rowY == Integer.MIN_VALUE) {
                continue;
            }
            Line line = this.lines.get(index);
            if (line.isHeading()) {
                context.drawString(this.font,
                        Component.translatable("group.car." + line.group().key()),
                        listLeft + 3, rowY + 3, TEXT_DIM, false);
                context.fill(listLeft + 2, rowY + ROW_HEIGHT - 1,
                        listLeft + LIST_WIDTH - 2, rowY + ROW_HEIGHT, 0xFF4A4A4A);
                continue;
            }

            CarPart part = line.part();
            boolean fitted = car != null && car.isFitted(part);
            float fraction = car == null ? 1.0F : car.getConditionFraction(part);
            // Indented under whatever is bolted on top of it, so the list reads
            // as the order the car comes apart in.
            int indent = 6 + part.depth() * 7;

            if (part == selected) {
                context.fill(listLeft + 1, rowY, listLeft + LIST_WIDTH - 1, rowY + ROW_HEIGHT - 1,
                        0x33000000 | (SELECTED & 0x00FFFFFF));
                context.fill(listLeft + 1, rowY, listLeft + 2, rowY + ROW_HEIGHT - 1, SELECTED);
            }

            context.drawString(this.font, Component.translatable("part.car." + part.key()),
                    listLeft + indent, rowY + 1, fitted ? TEXT_ON_WELL : ABSENT, false);

            String reading = fitted ? Math.round(fraction * 100.0F) + "%" : "—";
            context.drawString(this.font, reading,
                    listLeft + LIST_WIDTH - 5 - this.font.width(reading), rowY + 1,
                    conditionColour(fraction, fitted), false);

            // The bar is the row's own underline: it starts where the name does,
            // so an indented part visibly belongs to the one above it.
            int barLeft = listLeft + indent;
            int barRight = listLeft + LIST_WIDTH - 5;
            context.fill(barLeft, rowY + 10, barRight, rowY + 12, 0xFF202020);
            if (fitted) {
                int filled = Math.max(1, Math.round((barRight - barLeft) * fraction));
                context.fill(barLeft, rowY + 10, barLeft + filled, rowY + 12,
                        conditionColour(fraction, true));
            }
        }

        this.drawScrollbar(context, listLeft + LIST_WIDTH + 3, listTop);
    }

    private void drawScrollbar(GuiGraphics context, int trackX, int trackY) {
        int max = this.maxScroll();
        if (max == 0) {
            return;
        }
        context.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + LIST_HEIGHT, WELL);
        int knob = Math.max(10, LIST_HEIGHT * VISIBLE_ROWS / this.lines.size());
        int knobY = trackY + (LIST_HEIGHT - knob) * this.scroll / max;
        context.fill(trackX, knobY, trackX + SCROLLBAR_WIDTH, knobY + knob, WELL_LIGHT);
        context.fill(trackX, knobY, trackX + SCROLLBAR_WIDTH - 1, knobY + 1, PANEL_LIGHT);
    }

    private void drawDetail(GuiGraphics context, CarEntity car) {
        int left = this.leftPos + DETAIL_X;
        int top = this.topPos + LIST_Y;
        drawWell(context, left - 1, top - 1, DETAIL_WIDTH + 2, 122);

        CarPart part = this.menu.selectedPart();
        int line = top + 3;

        context.renderItem(new ItemStack(part.item(), part.count()), left + 3, line);
        line += 20;

        for (net.minecraft.util.FormattedCharSequence piece : this.font.split(
                Component.translatable("part.car." + part.key()), DETAIL_WIDTH - 8)) {
            context.drawString(this.font, piece, left + 4, line, TEXT_ON_WELL, false);
            line += 10;
        }

        // What it is actually for, which is the only reason to care about it.
        for (net.minecraft.util.FormattedCharSequence piece : this.font.split(
                Component.translatable("part.car." + part.key() + ".effect"), DETAIL_WIDTH - 8)) {
            context.drawString(this.font, piece, left + 4, line, TEXT_DIM, false);
            line += 9;
        }
        line += 3;

        if (car != null) {
            boolean fitted = car.isFitted(part);
            float fraction = car.getConditionFraction(part);
            CarPart inTheWay = car.blockedBy(part);

            Component status;
            int colour;
            if (inTheWay != null) {
                status = Component.translatable("gui.car.status_blocked",
                        Component.translatable("part.car." + inTheWay.key()));
                colour = BAD;
            } else if (!fitted) {
                status = Component.translatable("gui.car.status_absent");
                colour = ABSENT;
            } else if (fraction >= 1.0F) {
                status = Component.translatable("gui.car.status_new");
                colour = GOOD;
            } else {
                status = Component.translatable(
                        fraction > 0.5F ? "gui.car.status_ok" : "gui.car.status_worn");
                colour = conditionColour(fraction, true);
            }
            for (net.minecraft.util.FormattedCharSequence piece : this.font.split(status, DETAIL_WIDTH - 8)) {
                context.drawString(this.font, piece, left + 4, line, colour, false);
                line += 9;
            }

            // What is in the slot was built to something, and that is half the
            // answer to "why is this worn out again already".
            if (fitted) {
                CarPart.Quality quality = car.getQuality(part);
                context.drawString(this.font,
                        Component.translatable("quality.car." + quality.key()),
                        left + 4, line, qualityColour(quality), false);
                line += 9;
            }

            ItemStack spare = this.menu.spareFor(part);
            Component have = spare.isEmpty()
                    ? Component.translatable("gui.car.no_spare")
                    : Component.translatable("gui.car.spare",
                            Math.round(CarEntity.conditionOf(spare)) + "%");
            context.drawString(this.font, have, left + 4, line + 2,
                    spare.isEmpty() ? ABSENT : TEXT_ON_WELL, false);
            if (part.count() > 1) {
                context.drawString(this.font,
                        Component.translatable("gui.car.set_of", part.count()),
                        left + 4, line + 12, TEXT_DIM, false);
            }
        }
    }

    /** The same reading the tooltip gives: dull is cheap, bright is good. */
    private static int qualityColour(CarPart.Quality quality) {
        return quality.argb();
    }

    /** Temperature, and whatever is stopping the car going anywhere. */
    private void drawFooter(GuiGraphics context, CarEntity car) {
        int left = this.leftPos + LIST_X;
        int top = this.topPos + LIST_Y + LIST_HEIGHT + 6;

        context.drawString(this.font, Component.translatable("gui.car.temperature"),
                left, top, TEXT, false);

        // Right-aligned and a fixed width, so a longer word in another language
        // runs out of room before it runs under the bar.
        int barRight = this.leftPos + LIST_X + LIST_WIDTH;
        int barLeft = barRight - 60;
        drawWell(context, barLeft - 1, top - 2, barRight - barLeft + 2, 11);
        if (car != null) {
            float temp = Mth.clamp(car.getEngineTemp(), 0.0F, 1.0F);
            int filled = Math.round((barRight - barLeft) * temp);
            context.fill(barLeft, top - 1, barLeft + filled, top + 8,
                    car.isOverheating() ? HOT : FAIR);
            String reading = Math.round(temp * 100.0F) + "%";
            context.drawString(this.font, reading,
                    barRight - 3 - this.font.width(reading), top, TEXT_ON_WELL, false);
        }

        if (car == null) {
            return;
        }
        Component warning = null;
        CarPart missing = car.missingEssential();
        if (missing != null) {
            warning = Component.translatable("gui.car.needs_part",
                    Component.translatable("part.car." + missing.key()));
        } else if (car.isBatteryFlat()) {
            warning = Component.translatable("gui.car.flat_battery");
        } else if (car.isOverheating()) {
            warning = Component.translatable("gui.car.overheating");
        }
        if (warning != null) {
            context.drawString(this.font, warning, left, top + 14, BAD, false);
        }
    }
}
