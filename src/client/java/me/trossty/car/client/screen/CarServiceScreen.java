package me.trossty.car.client.screen;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.service.CarServiceScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * The service window: the whole car on one page.
 *
 * <p>The list is the car as it comes apart. Each part is indented under the one
 * that is bolted on top of it, with a line back up to it, so the order you have
 * to work in is the order you read in — the clutch sits two steps in from the
 * driveshaft because that is how many things have to come off before you can
 * touch it. Each part has a lamp and a row of LEDs for what is left of it.
 *
 * <p>Beside the list is a diagnostic screen for the chosen part, and under it a
 * strip of dashboard: the temperature gauge, the three warning lamps a driver
 * would see, and a line saying what is stopping the car, if anything.
 *
 * <p>Every number here is read straight off the car entity, which the client
 * already has: the window only ever had to be told which car it is about.
 */
public class CarServiceScreen extends AbstractContainerScreen<CarServiceScreenHandler> {

    private static final int TEXT_ON_WELL = 0xFFE0E0E0;
    private static final int TEXT_DIM = 0xFF9AA2AC;
    private static final int HEADING = 0xFF353B43;
    private static final int TREE = 0xFF6E7884;
    private static final int BAR_BACK = 0xFF1C2026;
    private static final int SCREEN = 0xFF141A20;

    private static final int GOOD = GuiStyle.LED_GREEN;
    private static final int FAIR = GuiStyle.LED_AMBER;
    private static final int BAD = GuiStyle.LED_RED;
    private static final int ABSENT = 0xFF6E6E6E;
    private static final int LAMP_DARK = 0xFF3A424C;
    private static final int COLD = 0xFF3F7FD0;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 154;
    private static final int ROW_HEIGHT = 13;
    private static final int VISIBLE_ROWS = 11;
    private static final int LIST_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_X = LIST_X + LIST_WIDTH - SCROLLBAR_WIDTH - 1;
    private static final int SCROLLBAR_Y = LIST_Y + 1;
    private static final int SCROLLBAR_HEIGHT = LIST_HEIGHT - 2;
    /** Where a row's reading ends, short of the scrollbar. */
    private static final int ROW_RIGHT = SCROLLBAR_X - 2;

    private static final int DETAIL_X = 166;
    private static final int DETAIL_WIDTH = 82;
    private static final int DETAIL_HEIGHT = LIST_HEIGHT;

    /** The dashboard strip under the list. */
    private static final int DASH_Y = LIST_Y + LIST_HEIGHT + 5;
    private static final int DASH_HEIGHT = 35;
    private static final int GAUGE_X = LIST_X + 16;
    private static final int GAUGE_Y = DASH_Y + 5;
    private static final int GAUGE_WIDTH = 92;
    private static final int LAMPS_X = LIST_X + 117;
    private static final int READOUT_Y = DASH_Y + 19;

    private static final int BUTTON_X = DETAIL_X - 1;
    private static final int BUTTON_WIDTH = DETAIL_WIDTH + 2;

    private static final String[] THERMOMETER = {
            "...#.##",
            "...#...",
            "...#.##",
            "..###..",
            "..###..",
            "#.#.#.#",
            ".#.#.#.",
    };
    private static final String[] BATTERY = {
            ".##...##.",
            "#########",
            "#.#.....#",
            "####.####",
            "#.#.....#",
            "#########",
    };
    private static final String[] ENGINE = {
            "..###....",
            "...#.....",
            ".######.#",
            "##.....##",
            ".#.....#.",
            ".#######.",
    };

    /** One line of the list: a system heading, or a part. */
    private record Line(CarPart.Group group, CarPart part) {

        boolean isHeading() {
            return this.part == null;
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private int scroll;
    private boolean draggingKnob;
    private PanelButton removeButton;
    private PanelButton fitButton;

    public CarServiceScreen(CarServiceScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = DASH_Y + DASH_HEIGHT + 7;
        // Only what this vehicle has: a trailer lists its running gear and no engine.
        CarEntity car = handler.car();
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
        this.titleLabelY = GuiStyle.TITLE_Y;
        this.inventoryLabelY = -1000;
        this.removeButton = PanelButton.steel(this.leftPos + BUTTON_X, this.topPos + DASH_Y,
                BUTTON_WIDTH, 16, Component.translatable("gui.car.remove"),
                button -> this.send(CarServiceScreenHandler.REMOVE_BUTTON));
        this.fitButton = PanelButton.action(this.leftPos + BUTTON_X, this.topPos + DASH_Y + DASH_HEIGHT - 16,
                BUTTON_WIDTH, 16, Component.translatable("gui.car.fit"),
                button -> this.send(CarServiceScreenHandler.FIT_BUTTON), VehicleWorkbenchScreen.BUILD_FACE);
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

    private int knobHeight() {
        return GuiStyle.knob(SCROLLBAR_HEIGHT, VISIBLE_ROWS, this.lines.size());
    }

    /** Where line {@code index} is drawn, or Integer.MIN_VALUE when hidden. */
    private int rowY(int index) {
        int row = index - this.scroll;
        if (row < 0 || row >= VISIBLE_ROWS) {
            return Integer.MIN_VALUE;
        }
        return this.topPos + LIST_Y + row * ROW_HEIGHT;
    }

    /** The part row under the cursor, as a line index, or -1. */
    private int partUnder(double mouseX, double mouseY) {
        if (mouseX < this.leftPos + LIST_X || mouseX >= this.leftPos + SCROLLBAR_X) {
            return -1;
        }
        for (int index = 0; index < this.lines.size(); index++) {
            int rowY = this.rowY(index);
            if (rowY != Integer.MIN_VALUE && !this.lines.get(index).isHeading()
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                return index;
            }
        }
        return -1;
    }

    private boolean overTrack(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos - SCROLLBAR_X;
        double y = mouseY - this.topPos - SCROLLBAR_Y;
        return x >= 0 && x < SCROLLBAR_WIDTH && y >= 0 && y < SCROLLBAR_HEIGHT;
    }

    private void scrollTo(double mouseY) {
        this.scroll = GuiStyle.scrollFor(mouseY - this.topPos - SCROLLBAR_Y,
                SCROLLBAR_HEIGHT, this.knobHeight(), this.maxScroll());
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
        int index = this.partUnder(mouseX, mouseY);
        if (index >= 0) {
            this.send(this.lines.get(index).part().ordinal());
            return true;
        }
        if (button == 0 && this.maxScroll() > 0 && this.overTrack(mouseX, mouseY)) {
            this.draggingKnob = true;
            this.scrollTo(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingKnob) {
            this.scrollTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingKnob = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        CarEntity car = this.menu.car();
        CarPart selected = this.menu.selectedPart();
        this.removeButton.active = car != null && car.isFitted(selected) && car.canWorkOn(selected);
        this.fitButton.active = car != null && car.canWorkOn(selected)
                && !this.menu.spareFor(selected).isEmpty()
                && car.getCondition(selected) < CarPart.MAX_CONDITION;
        super.render(context, mouseX, mouseY, deltaTicks);
        this.drawTooltips(context, car, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics context, float deltaTicks, int mouseX, int mouseY) {
        GuiStyle.frame(context, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        CarEntity car = this.menu.car();
        this.drawList(context, car, mouseX, mouseY);
        this.drawDetail(context, car);
        this.drawDash(context, car);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        GuiStyle.title(context, this.font, this.title, this.imageWidth - GuiStyle.TITLE_X - 10);
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

    /** A row of LEDs three pixels long with a gap between, lit as far as {@code fraction}. */
    private static void ledBar(GuiGraphics context, int left, int top, int right, int height,
                               float fraction, int colour) {
        context.fill(left, top, right, top + height, BAR_BACK);
        int segments = (right - left + 1) / 4;
        int lit = fraction <= 0.0F ? 0 : Math.max(1, Math.round(segments * GuiStyle.clamp(fraction)));
        for (int segment = 0; segment < segments; segment++) {
            int x = left + segment * 4;
            context.fill(x, top, x + 3, top + height, segment < lit ? colour : GuiStyle.LED_OFF);
        }
    }

    private void drawList(GuiGraphics context, CarEntity car, int mouseX, int mouseY) {
        int listLeft = this.leftPos + LIST_X;
        int rowRight = this.leftPos + ROW_RIGHT;
        GuiStyle.well(context, listLeft, this.topPos + LIST_Y, LIST_WIDTH, LIST_HEIGHT);

        this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
        CarPart selected = this.menu.selectedPart();
        int hovered = this.partUnder(mouseX, mouseY);

        for (int index = 0; index < this.lines.size(); index++) {
            int rowY = this.rowY(index);
            if (rowY == Integer.MIN_VALUE) {
                continue;
            }
            Line line = this.lines.get(index);
            if (line.isHeading()) {
                context.fill(listLeft, rowY, rowRight + 2, rowY + ROW_HEIGHT - 1, HEADING);
                context.fill(listLeft, rowY + ROW_HEIGHT - 1, rowRight + 2, rowY + ROW_HEIGHT, 0xFF5B636C);
                context.drawString(this.font,
                        GuiStyle.fit(this.font, Component.translatable("group.car." + line.group().key()),
                                rowRight - listLeft - 6),
                        listLeft + 4, rowY + 3, GuiStyle.TITLE_SOFT, false);
                continue;
            }

            CarPart part = line.part();
            boolean fitted = car != null && car.isFitted(part);
            boolean seen = car == null || car.canInspect(part);
            float fraction = car == null ? 1.0F : car.getConditionFraction(part);
            int colour = seen ? conditionColour(fraction, fitted) : ABSENT;
            // Indented under whatever is bolted on top of it, so the list reads
            // as the order the car comes apart in.
            int indent = 5 + part.depth() * 7;

            if (part == selected) {
                context.fill(listLeft, rowY, rowRight + 2, rowY + ROW_HEIGHT, 0x70E8781E);
                context.fill(listLeft, rowY, listLeft + 2, rowY + ROW_HEIGHT, GuiStyle.ACCENT);
            } else if (index == hovered) {
                context.fill(listLeft, rowY, rowRight + 2, rowY + ROW_HEIGHT, 0x24FFFFFF);
            }
            if (part.depth() > 0) {
                // A line back up to the part this one sits under.
                int stem = listLeft + indent - 6;
                context.fill(stem, rowY - 6, stem + 1, rowY + 5, TREE);
                context.fill(stem + 1, rowY + 4, stem + 4, rowY + 5, TREE);
            }
            GuiStyle.disc(context, listLeft + indent + 1, rowY + 4, 2, GuiStyle.OUTLINE);
            GuiStyle.disc(context, listLeft + indent + 1, rowY + 4, 1, colour);
            GuiStyle.dot(context, listLeft + indent, rowY + 3, GuiStyle.lighter(colour));

            String reading = !fitted ? "—" : seen ? Math.round(fraction * 100.0F) + "%" : "?";
            int nameLeft = listLeft + indent + 6;
            // The name stops short of the reading rather than running under it.
            int nameRoom = rowRight - this.font.width(reading) - 4 - nameLeft;
            context.drawString(this.font,
                    GuiStyle.fit(this.font, Component.translatable("part.car." + part.key()), nameRoom),
                    nameLeft, rowY + 1, fitted ? TEXT_ON_WELL : ABSENT, false);
            context.drawString(this.font, reading, rowRight - this.font.width(reading), rowY + 1, colour, false);
            // The LEDs are the row's own underline: they start where the name
            // does, so an indented part visibly belongs to the one above it.
            ledBar(context, nameLeft, rowY + 10, rowRight, 2, fitted && seen ? fraction : 0.0F, colour);
        }

        GuiStyle.scrollbar(context, this.leftPos + SCROLLBAR_X, this.topPos + SCROLLBAR_Y,
                SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, this.knobHeight(), this.scroll, this.maxScroll());
    }

    /**
     * The chosen part on a diagnostic screen: the part itself, what is left of
     * it in large figures over a row of LEDs, and under a rule its name, what
     * it does, how it stands, what it was built to and whether there is a
     * spare to hand.
     */
    private void drawDetail(GuiGraphics context, CarEntity car) {
        int left = this.leftPos + DETAIL_X;
        int top = this.topPos + LIST_Y;
        GuiStyle.inset(context, left, top, DETAIL_WIDTH, DETAIL_HEIGHT, SCREEN);
        context.fillGradient(left, top, left + DETAIL_WIDTH, top + 30, 0x14FFFFFF, 0x00FFFFFF);

        CarPart part = this.menu.selectedPart();
        int count = car == null ? part.count() : car.countOf(part);
        GuiStyle.slot(context, left + 3, top + 3);
        context.renderItem(new ItemStack(part.item(), count), left + 4, top + 4);

        boolean fitted = car != null && car.isFitted(part);
        CarPart hidden = car == null || !fitted ? null : car.hiddenBy(part);
        float fraction = car == null ? 1.0F : car.getConditionFraction(part);
        int colour = hidden == null ? conditionColour(fraction, fitted) : ABSENT;
        String big = !fitted ? "—" : hidden == null ? Math.round(fraction * 100.0F) + "%" : "?";
        context.pose().pushPose();
        context.pose().translate(left + 25, top + 4, 0.0F);
        context.pose().scale(2.0F, 2.0F, 1.0F);
        context.drawString(this.font, big, 0, 0, colour, true);
        context.pose().popPose();
        ledBar(context, left + 25, top + 20, left + DETAIL_WIDTH - 3, 2,
                fitted && hidden == null ? fraction : 0.0F, colour);
        context.fill(left + 3, top + 25, left + DETAIL_WIDTH - 3, top + 26, 0x30FFFFFF);

        int line = top + 29;
        int room = DETAIL_WIDTH - 8;
        for (FormattedCharSequence piece : this.font.split(Component.translatable("part.car." + part.key()), room)) {
            context.drawString(this.font, piece, left + 4, line, TEXT_ON_WELL, false);
            line += 10;
        }
        // What it is actually for, which is the only reason to care about it.
        for (FormattedCharSequence piece : this.font.split(
                Component.translatable("part.car." + part.key() + ".effect"), room)) {
            context.drawString(this.font, piece, left + 4, line, TEXT_DIM, false);
            line += 9;
        }
        line += 4;
        if (car == null) {
            return;
        }

        CarPart inTheWay = car.blockedBy(part);
        Component status;
        int statusColour;
        if (inTheWay != null) {
            status = Component.translatable("gui.car.status_blocked",
                    Component.translatable("part.car." + inTheWay.key()));
            statusColour = BAD;
        } else if (!fitted) {
            status = Component.translatable("gui.car.status_absent");
            statusColour = ABSENT;
        } else if (hidden != null) {
            status = Component.translatable("gui.car.status_hidden",
                    Component.translatable("part.car." + hidden.key()));
            statusColour = TEXT_DIM;
        } else if (fraction >= 1.0F) {
            status = Component.translatable("gui.car.status_new");
            statusColour = GOOD;
        } else {
            status = Component.translatable(fraction > 0.5F ? "gui.car.status_ok" : "gui.car.status_worn");
            statusColour = colour;
        }
        for (FormattedCharSequence piece : this.font.split(status, room)) {
            context.drawString(this.font, piece, left + 4, line, statusColour, false);
            line += 9;
        }

        // What is in the slot was built to something, and that is half the
        // answer to "why is this worn out again already".
        if (fitted) {
            CarPart.Quality quality = car.getQuality(part);
            context.drawString(this.font, Component.translatable("quality.car." + quality.key()),
                    left + 4, line, quality.argb(), false);
            line += 9;
        }

        ItemStack spare = this.menu.spareFor(part);
        Component have = spare.isEmpty()
                ? Component.translatable("gui.car.no_spare")
                : Component.translatable("gui.car.spare", Math.round(CarEntity.conditionOf(spare)) + "%");
        context.drawString(this.font, GuiStyle.fit(this.font, have, room), left + 4, line + 2,
                spare.isEmpty() ? ABSENT : TEXT_ON_WELL, false);
        if (count > 1) {
            context.drawString(this.font, Component.translatable("gui.car.set_of", count),
                    left + 4, line + 12, TEXT_DIM, false);
        }
    }

    /**
     * A strip of dashboard: the temperature gauge with its cold and hot ends
     * and a needle, the battery, engine and temperature warning lamps, and an
     * LCD line saying what is stopping the car — or that nothing is.
     */
    private void drawDash(GuiGraphics context, CarEntity car) {
        int left = this.leftPos + LIST_X;
        int top = this.topPos + DASH_Y;
        GuiStyle.well(context, left, top, LIST_WIDTH, DASH_HEIGHT);

        float temp = car == null ? 0.0F : Mth.clamp(car.getEngineTemp(), 0.0F, 1.0F);
        boolean hot = car != null && car.isOverheating();
        GuiStyle.pixels(context, THERMOMETER, left + 5, this.topPos + GAUGE_Y - 1, GuiStyle.TITLE_SOFT);

        int gx = this.leftPos + GAUGE_X;
        int gy = this.topPos + GAUGE_Y;
        int fifth = GAUGE_WIDTH / 5;
        context.fill(gx - 1, gy - 1, gx + GAUGE_WIDTH + 1, gy + 5, GuiStyle.OUTLINE);
        context.fill(gx, gy, gx + GAUGE_WIDTH, gy + 4, 0xFF1A1F25);
        context.fill(gx, gy, gx + fifth, gy + 4, COLD);
        context.fill(gx + GAUGE_WIDTH - fifth, gy, gx + GAUGE_WIDTH, gy + 4, GuiStyle.LED_RED);
        context.fill(gx, gy, gx + GAUGE_WIDTH, gy + 1, 0x30FFFFFF);
        for (int tick = 0; tick <= 4; tick++) {
            int tx = gx + Math.min(GAUGE_WIDTH - 1, tick * GAUGE_WIDTH / 4);
            context.fill(tx, gy + 6, tx + 1, gy + (tick % 2 == 0 ? 9 : 8), GuiStyle.TITLE_SOFT);
        }
        int needle = gx + Math.round((GAUGE_WIDTH - 1) * temp);
        context.fill(needle - 1, gy - 3, needle + 2, gy - 2, 0xFFF4F4F4);
        context.fill(needle, gy - 2, needle + 1, gy + 5, 0xFFF4F4F4);

        CarPart missing = car == null ? null : car.missingEssential();
        boolean flat = car != null && car.isBatteryFlat();
        int lampsX = this.leftPos + LAMPS_X;
        int lampsY = this.topPos + GAUGE_Y - 1;
        GuiStyle.pixels(context, BATTERY, lampsX, lampsY, flat ? BAD : LAMP_DARK);
        GuiStyle.pixels(context, ENGINE, lampsX + 12, lampsY, missing != null ? FAIR : LAMP_DARK);
        GuiStyle.pixels(context, THERMOMETER, lampsX + 24, lampsY - 1, hot ? BAD : LAMP_DARK);

        Component warning = this.warning(car);
        GuiStyle.readout(context, this.font,
                warning != null ? warning : Component.translatable("gui.car.ready"),
                left + 4, this.topPos + READOUT_Y, LIST_WIDTH - 8,
                warning != null ? 0xFFFF7A64 : GuiStyle.READOUT_TEXT);
    }

    /** Whatever is stopping the car going anywhere, worst first, or null. */
    private Component warning(CarEntity car) {
        if (car == null) {
            return null;
        }
        CarPart missing = car.missingEssential();
        if (missing != null) {
            return Component.translatable("gui.car.needs_part", Component.translatable("part.car." + missing.key()));
        }
        if (car.isBatteryFlat()) {
            return Component.translatable("gui.car.flat_battery");
        }
        if (car.isOverheating()) {
            return Component.translatable("gui.car.overheating");
        }
        return null;
    }

    /** The gauge gives its figure under the cursor, and a name cut short in the list is given in full. */
    private void drawTooltips(GuiGraphics context, CarEntity car, int mouseX, int mouseY) {
        int gx = this.leftPos + GAUGE_X;
        int gy = this.topPos + GAUGE_Y;
        if (car != null && mouseX >= gx - 12 && mouseX < gx + GAUGE_WIDTH + 1 && mouseY >= gy - 4 && mouseY < gy + 10) {
            int percent = Math.round(Mth.clamp(car.getEngineTemp(), 0.0F, 1.0F) * 100.0F);
            context.renderTooltip(this.font,
                    Component.translatable("gui.car.temperature").append(" " + percent + "%"), mouseX, mouseY);
            return;
        }
        int index = this.partUnder(mouseX, mouseY);
        if (index >= 0) {
            Component name = Component.translatable("part.car." + this.lines.get(index).part().key());
            int indent = 5 + this.lines.get(index).part().depth() * 7;
            int room = ROW_RIGHT - LIST_X - indent - 6 - this.font.width("100%") - 4;
            if (this.font.width(name) > room) {
                context.renderTooltip(this.font, name, mouseX, mouseY);
            }
        }
    }
}
