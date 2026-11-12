package me.trossty.car.client.screen;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.service.CarServiceScreenHandler;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

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
public class CarServiceScreen extends HandledScreen<CarServiceScreenHandler> {

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
    private ButtonWidget removeButton;
    private ButtonWidget fitButton;

    public CarServiceScreen(CarServiceScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 256;
        this.backgroundHeight = 204;
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
        this.titleY = 7;
        this.playerInventoryTitleY = -1000;
        this.removeButton = ButtonWidget.builder(
                        Text.translatable("gui.car.remove"), button -> this.send(CarServiceScreenHandler.REMOVE_BUTTON))
                .dimensions(this.x + DETAIL_X, this.y + 146, DETAIL_WIDTH, 16)
                .build();
        this.fitButton = ButtonWidget.builder(
                        Text.translatable("gui.car.fit"), button -> this.send(CarServiceScreenHandler.FIT_BUTTON))
                .dimensions(this.x + DETAIL_X, this.y + 164, DETAIL_WIDTH, 16)
                .build();
        this.addDrawableChild(this.removeButton);
        this.addDrawableChild(this.fitButton);
    }

    private void send(int id) {
        if (this.client != null && this.client.interactionManager != null) {
            this.client.interactionManager.clickButton(this.handler.syncId, id);
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
        return this.y + LIST_Y + row * ROW_HEIGHT;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0.0 && this.maxScroll() > 0) {
            this.scroll = MathHelper.clamp(
                    this.scroll - (int) Math.signum(vertical), 0, this.maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        for (int index = 0; index < this.lines.size(); index++) {
            Line line = this.lines.get(index);
            if (line.isHeading()) {
                continue;
            }
            int rowY = this.rowY(index);
            if (rowY == Integer.MIN_VALUE) {
                continue;
            }
            if (click.x() >= this.x + LIST_X && click.x() < this.x + LIST_X + LIST_WIDTH
                    && click.y() >= rowY && click.y() < rowY + ROW_HEIGHT) {
                this.send(line.part().ordinal());
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    // ---- drawing ----

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        CarEntity car = this.handler.car();
        CarPart selected = this.handler.selectedPart();
        this.removeButton.active = car != null && car.isFitted(selected) && car.canReach(selected);
        this.fitButton.active = car != null && car.canReach(selected)
                && !this.handler.spareFor(selected).isEmpty()
                && car.getCondition(selected) < CarPart.MAX_CONDITION;
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
        int left = this.x;
        int top = this.y;
        int right = left + this.backgroundWidth;
        int bottom = top + this.backgroundHeight;

        context.fill(left, top, right, bottom, PANEL);
        context.fill(left, top, right, top + 1, PANEL_LIGHT);
        context.fill(left, top, left + 1, bottom, PANEL_LIGHT);
        context.fill(left, bottom - 1, right, bottom, PANEL_SHADOW);
        context.fill(right - 1, top, right, bottom, PANEL_SHADOW);

        CarEntity car = this.handler.car();
        this.drawList(context, car);
        this.drawDetail(context, car);
        this.drawFooter(context, car);
    }

    /** A recessed well, the way vanilla frames anything you read out of. */
    private static void drawWell(DrawContext context, int x, int y, int width, int height) {
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

    private void drawList(DrawContext context, CarEntity car) {
        int listLeft = this.x + LIST_X;
        int listTop = this.y + LIST_Y;
        drawWell(context, listLeft - 1, listTop - 1, LIST_WIDTH + 2, LIST_HEIGHT + 2);

        this.scroll = MathHelper.clamp(this.scroll, 0, this.maxScroll());
        CarPart selected = this.handler.selectedPart();

        for (int index = 0; index < this.lines.size(); index++) {
            int rowY = this.rowY(index);
            if (rowY == Integer.MIN_VALUE) {
                continue;
            }
            Line line = this.lines.get(index);
            if (line.isHeading()) {
                context.drawText(this.textRenderer,
                        Text.translatable("group.car." + line.group().key()),
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

            context.drawText(this.textRenderer, Text.translatable("part.car." + part.key()),
                    listLeft + indent, rowY + 1, fitted ? TEXT_ON_WELL : ABSENT, false);

            String reading = fitted ? Math.round(fraction * 100.0F) + "%" : "—";
            context.drawText(this.textRenderer, reading,
                    listLeft + LIST_WIDTH - 5 - this.textRenderer.getWidth(reading), rowY + 1,
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

    private void drawScrollbar(DrawContext context, int trackX, int trackY) {
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

    private void drawDetail(DrawContext context, CarEntity car) {
        int left = this.x + DETAIL_X;
        int top = this.y + LIST_Y;
        drawWell(context, left - 1, top - 1, DETAIL_WIDTH + 2, 122);

        CarPart part = this.handler.selectedPart();
        int line = top + 3;

        context.drawItem(new ItemStack(part.item(), part.count()), left + 3, line);
        line += 20;

        for (net.minecraft.text.OrderedText piece : this.textRenderer.wrapLines(
                Text.translatable("part.car." + part.key()), DETAIL_WIDTH - 8)) {
            context.drawText(this.textRenderer, piece, left + 4, line, TEXT_ON_WELL, false);
            line += 10;
        }

        // What it is actually for, which is the only reason to care about it.
        for (net.minecraft.text.OrderedText piece : this.textRenderer.wrapLines(
                Text.translatable("part.car." + part.key() + ".effect"), DETAIL_WIDTH - 8)) {
            context.drawText(this.textRenderer, piece, left + 4, line, TEXT_DIM, false);
            line += 9;
        }
        line += 3;

        if (car != null) {
            boolean fitted = car.isFitted(part);
            float fraction = car.getConditionFraction(part);
            CarPart inTheWay = car.blockedBy(part);

            Text status;
            int colour;
            if (inTheWay != null) {
                status = Text.translatable("gui.car.status_blocked",
                        Text.translatable("part.car." + inTheWay.key()));
                colour = BAD;
            } else if (!fitted) {
                status = Text.translatable("gui.car.status_absent");
                colour = ABSENT;
            } else if (fraction >= 1.0F) {
                status = Text.translatable("gui.car.status_new");
                colour = GOOD;
            } else {
                status = Text.translatable(
                        fraction > 0.5F ? "gui.car.status_ok" : "gui.car.status_worn");
                colour = conditionColour(fraction, true);
            }
            for (net.minecraft.text.OrderedText piece : this.textRenderer.wrapLines(status, DETAIL_WIDTH - 8)) {
                context.drawText(this.textRenderer, piece, left + 4, line, colour, false);
                line += 9;
            }

            // What is in the slot was built to something, and that is half the
            // answer to "why is this worn out again already".
            if (fitted) {
                CarPart.Quality quality = car.getQuality(part);
                context.drawText(this.textRenderer,
                        Text.translatable("quality.car." + quality.key()),
                        left + 4, line, qualityColour(quality), false);
                line += 9;
            }

            ItemStack spare = this.handler.spareFor(part);
            Text have = spare.isEmpty()
                    ? Text.translatable("gui.car.no_spare")
                    : Text.translatable("gui.car.spare",
                            Math.round(CarEntity.conditionOf(spare)) + "%");
            context.drawText(this.textRenderer, have, left + 4, line + 2,
                    spare.isEmpty() ? ABSENT : TEXT_ON_WELL, false);
            if (part.count() > 1) {
                context.drawText(this.textRenderer,
                        Text.translatable("gui.car.set_of", part.count()),
                        left + 4, line + 12, TEXT_DIM, false);
            }
        }
    }

    /** The same reading the tooltip gives: dull is cheap, bright is good. */
    private static int qualityColour(CarPart.Quality quality) {
        return quality.argb();
    }

    /** Temperature, and whatever is stopping the car going anywhere. */
    private void drawFooter(DrawContext context, CarEntity car) {
        int left = this.x + LIST_X;
        int top = this.y + LIST_Y + LIST_HEIGHT + 6;

        context.drawText(this.textRenderer, Text.translatable("gui.car.temperature"),
                left, top, TEXT, false);

        // Right-aligned and a fixed width, so a longer word in another language
        // runs out of room before it runs under the bar.
        int barRight = this.x + LIST_X + LIST_WIDTH;
        int barLeft = barRight - 60;
        drawWell(context, barLeft - 1, top - 2, barRight - barLeft + 2, 11);
        if (car != null) {
            float temp = MathHelper.clamp(car.getEngineTemp(), 0.0F, 1.0F);
            int filled = Math.round((barRight - barLeft) * temp);
            context.fill(barLeft, top - 1, barLeft + filled, top + 8,
                    car.isOverheating() ? HOT : FAIR);
            String reading = Math.round(temp * 100.0F) + "%";
            context.drawText(this.textRenderer, reading,
                    barRight - 3 - this.textRenderer.getWidth(reading), top, TEXT_ON_WELL, false);
        }

        if (car == null) {
            return;
        }
        Text warning = null;
        CarPart missing = car.missingEssential();
        if (missing != null) {
            warning = Text.translatable("gui.car.needs_part",
                    Text.translatable("part.car." + missing.key()));
        } else if (car.isBatteryFlat()) {
            warning = Text.translatable("gui.car.flat_battery");
        } else if (car.isOverheating()) {
            warning = Text.translatable("gui.car.overheating");
        }
        if (warning != null) {
            context.drawText(this.textRenderer, warning, left, top + 14, BAD, false);
        }
    }
}
