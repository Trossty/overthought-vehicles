package me.trossty.car.client.screen;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.item.CarItem;
import me.trossty.car.vehicle.VehicleDefinition;
import me.trossty.car.workbench.VehicleRecipe;
import me.trossty.car.workbench.VehicleWorkstationScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The vehicle workstation's window: the list of vehicles in a steel well on
 * the left, the chosen one turning on a showroom turntable on the right under
 * a spotlight, its paint chips underneath and what it costs pinned up below
 * that on a blueprint. Drawn with plain rectangles, like the bench.
 */
public class VehicleWorkstationScreen extends AbstractContainerScreen<VehicleWorkstationScreenHandler> {

    private static final int STUDIO_TOP = 0xFF34475E;
    private static final int STUDIO_WALL = 0xFF1C2631;
    private static final int STUDIO_FLOOR = 0xFF2A3038;
    private static final int STUDIO_FLOOR_FRONT = 0xFF14181D;
    private static final int TURNTABLE = 0xFF3D454F;
    private static final int TURNTABLE_EDGE = 0xFF0D1014;
    private static final int OPTIONAL = 0xFFFFD34A;
    /** How far the stand looks down on the vehicle, and so how flat the turntable is drawn. */
    private static final float TILT_DEGREES = 22.0F;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 100;
    private static final int ROW = 12;
    private static final int ROWS = 8;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_X = LIST_X + LIST_WIDTH - SCROLLBAR_WIDTH - 1;
    private static final int SCROLLBAR_Y = LIST_Y + 1;
    private static final int SCROLLBAR_HEIGHT = ROWS * ROW - 2;
    /** Where a name has to stop: short of the scrollbar, with a pixel to spare. */
    private static final int NAME_ROOM = SCROLLBAR_X - LIST_X - 6;

    private static final int PREVIEW_X = 112;
    private static final int PREVIEW_Y = 18;
    private static final int PREVIEW_WIDTH = 136;
    private static final int PREVIEW_HEIGHT = 60;

    /** Seventeen chips, factory paint first, the width of the stand. */
    private static final int SWATCH_X = PREVIEW_X;
    private static final int SWATCH_Y = 81;
    private static final int SWATCH = 8;

    private static final int SHEET_X = PREVIEW_X;
    private static final int SHEET_Y = 92;
    private static final int SHEET_WIDTH = PREVIEW_WIDTH;
    private static final int SHEET_HEIGHT = 42;
    private static final int MATERIALS_X = SHEET_X + 5;
    private static final int MATERIALS_Y = SHEET_Y + 3;
    private static final int MATERIALS_COLUMNS = 7;

    private PanelButton buildButton;
    private static final float BUILD_FLASH_MS = 900.0F;
    private int scroll;
    /** The stand's turn, in radians; it creeps round on its own and can be dragged. */
    private float standTurn;
    private long lastFrame = -1L;
    /** When the last build went through, for the flash and the fast spin; -1 for never. */
    private long builtAt = -1L;
    /** When the vehicle on the stand last changed, so the new one can settle on. */
    private long shownAt = -1L;
    private ResourceLocation shown;
    private boolean dragging;
    private boolean draggingKnob;
    private final Map<ResourceLocation, CarEntity> stands = new HashMap<>();

    public VehicleWorkstationScreen(VehicleWorkstationScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = VehicleWorkstationScreenHandler.HOTBAR_TOP + 24;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = VehicleWorkstationScreenHandler.PLAYER_INVENTORY_LEFT;
        this.inventoryLabelY = VehicleWorkstationScreenHandler.PLAYER_INVENTORY_TOP - 11;
        this.buildButton = PanelButton.action(this.leftPos + LIST_X - 1, this.topPos + SHEET_Y + SHEET_HEIGHT - 16,
                LIST_WIDTH + 2, 16, Component.translatable("gui.overthought_vehicles.build"),
                button -> {
                    if (this.menu.canBuild()) {
                        this.builtAt = Util.getMillis();
                    }
                    this.click(VehicleWorkstationScreenHandler.BUILD_ID);
                }, VehicleWorkbenchScreen.BUILD_FACE);
        this.addRenderableWidget(this.buildButton);
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private boolean overStand(double mouseX, double mouseY) {
        int left = this.leftPos + PREVIEW_X;
        int top = this.topPos + PREVIEW_Y;
        return mouseX >= left && mouseX < left + PREVIEW_WIDTH && mouseY >= top && mouseY < top + PREVIEW_HEIGHT;
    }

    /** Dragging across the stand turns the vehicle on it, to look it over. */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double offsetX, double offsetY) {
        if (this.dragging) {
            this.standTurn += (float) offsetX * 0.03F;
            return true;
        }
        if (this.draggingKnob) {
            this.scrollTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
        this.draggingKnob = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int maxScroll() {
        return Math.max(0, this.menu.getVehicles().size() - ROWS);
    }

    private int knobHeight() {
        return GuiStyle.knob(SCROLLBAR_HEIGHT, ROWS, this.menu.getVehicles().size());
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
            this.scroll = Mth.clamp(this.scroll - (int) Math.signum(vertical), 0, this.maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    /** The list row under the cursor, as a vehicle index, or -1. */
    private int rowUnder(double mouseX, double mouseY) {
        int left = this.leftPos + LIST_X;
        int top = this.topPos + LIST_Y;
        if (mouseX < left || mouseX >= this.leftPos + SCROLLBAR_X || mouseY < top || mouseY >= top + ROWS * ROW) {
            return -1;
        }
        int index = this.scroll + (int) (mouseY - top) / ROW;
        return index < this.menu.getVehicles().size() ? index : -1;
    }

    /** The paint swatch under the cursor: -1 factory, 0-15 a dye, or -2 for none. */
    private int swatchUnder(double mouseX, double mouseY) {
        int top = this.topPos + SWATCH_Y;
        if (mouseY < top || mouseY >= top + SWATCH - 1) {
            return -2;
        }
        int column = (int) Math.floor((mouseX - this.leftPos - SWATCH_X) / SWATCH);
        if (column < 0 || column > 16 || mouseX - this.leftPos - SWATCH_X - column * SWATCH >= SWATCH - 1) {
            return -2;
        }
        return column - 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.overStand(mouseX, mouseY)) {
            this.dragging = true;
            return true;
        }
        if (button == 0 && this.maxScroll() > 0 && this.overTrack(mouseX, mouseY)) {
            this.draggingKnob = true;
            this.scrollTo(mouseY);
            return true;
        }
        int row = this.rowUnder(mouseX, mouseY);
        if (row >= 0) {
            this.click(row);
            return true;
        }
        int swatch = this.swatchUnder(mouseX, mouseY);
        if (swatch >= -1) {
            this.click(swatch < 0 ? VehicleWorkstationScreenHandler.FACTORY_PAINT_ID
                    : VehicleWorkstationScreenHandler.DYE_ID + swatch);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        this.buildButton.active = this.menu.canBuild();
        super.render(context, mouseX, mouseY, deltaTicks);
        this.drawTooltips(context, mouseX, mouseY);
        this.renderTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        GuiStyle.title(context, this.font, this.title, this.imageWidth - GuiStyle.TITLE_X - 10);
        context.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, GuiStyle.TEXT, false);
    }

    @Override
    protected void renderBg(GuiGraphics context, float deltaTicks, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        GuiStyle.frame(context, left, top, this.imageWidth, this.imageHeight);
        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            GuiStyle.slot(context, left + slot.x - 1, top + slot.y - 1);
        }
        this.drawList(context, mouseX, mouseY);
        this.drawStand(context);
        this.drawSwatches(context, mouseX, mouseY);
        GuiStyle.blueprint(context, left + SHEET_X, top + SHEET_Y, SHEET_WIDTH, SHEET_HEIGHT);
        this.drawMaterials(context);
    }

    /**
     * The vehicles, one to a row in a steel well: the chosen one washed orange
     * with a bar down its edge, the one under the cursor lifted a shade. A long
     * name is cut with an ellipsis rather than squeezed; the stand names it in full.
     */
    private void drawList(GuiGraphics context, int mouseX, int mouseY) {
        this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
        int left = this.leftPos + LIST_X;
        int top = this.topPos + LIST_Y;
        int rowRight = this.leftPos + SCROLLBAR_X - 1;
        GuiStyle.well(context, left, top, LIST_WIDTH, ROWS * ROW);
        List<ResourceLocation> vehicles = this.menu.getVehicles();
        int hovered = this.rowUnder(mouseX, mouseY);
        for (int row = 0; row < ROWS && this.scroll + row < vehicles.size(); row++) {
            int index = this.scroll + row;
            int rowTop = top + row * ROW;
            boolean chosen = index == this.menu.getSelectedIndex();
            if (index % 2 == 1) {
                context.fill(left, rowTop, rowRight, rowTop + ROW, 0x0CFFFFFF);
            }
            if (chosen) {
                context.fill(left, rowTop, rowRight, rowTop + ROW, 0x70E8781E);
                context.fill(left, rowTop, left + 2, rowTop + ROW, GuiStyle.ACCENT);
            } else if (index == hovered) {
                context.fill(left, rowTop, rowRight, rowTop + ROW, 0x24FFFFFF);
            }
            Component name = VehicleDefinition.nameOf(vehicles.get(index));
            context.drawString(this.font, GuiStyle.fit(this.font, name, NAME_ROOM),
                    left + 4, rowTop + 2, chosen ? 0xFFFFFFFF : 0xFFD5DAE0, true);
        }
        GuiStyle.scrollbar(context, this.leftPos + SCROLLBAR_X, this.topPos + SCROLLBAR_Y,
                SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, this.knobHeight(), this.scroll, this.maxScroll());
    }

    /**
     * The showroom: a studio wall shading down to a floor, a lamp at the top
     * throwing a soft cone of light, and a turntable the chosen vehicle stands
     * on, its marks going round with it. The vehicle's name is on a plaque in
     * the corner.
     */
    private void drawStand(GuiGraphics context) {
        int left = this.leftPos + PREVIEW_X;
        int top = this.topPos + PREVIEW_Y;
        int right = left + PREVIEW_WIDTH;
        int bottom = top + PREVIEW_HEIGHT;
        int middle = left + PREVIEW_WIDTH / 2;
        GuiStyle.inset(context, left, top, PREVIEW_WIDTH, PREVIEW_HEIGHT, STUDIO_FLOOR_FRONT);
        int horizon = top + PREVIEW_HEIGHT * 11 / 20;
        context.fillGradient(left, top, right, horizon, STUDIO_TOP, STUDIO_WALL);
        context.fillGradient(left, horizon, right, bottom, STUDIO_FLOOR, STUDIO_FLOOR_FRONT);
        context.fill(left, horizon, right, horizon + 1, 0x24FFFFFF);
        for (int y = top + 2; y < bottom; y++) {
            int half = 5 + (y - top) * 3 / 4;
            context.fill(Math.max(left, middle - half), y, Math.min(right, middle + half + 1), y + 1, 0x0AFFF4DC);
        }
        context.fill(middle - 6, top, middle + 7, top + 2, 0xFF0B0E12);
        context.fill(middle - 4, top + 2, middle + 5, top + 3, 0xFFFFF0C4);

        ResourceLocation id = this.menu.getSelectedId();
        VehicleDefinition definition = this.menu.getSelected();
        if (id == null || definition == null) {
            return;
        }
        CarEntity car = this.standFor(id, definition);
        if (car != null) {
            this.drawVehicle(context, car, id, definition, left, top, right, bottom);
        }

        // The plaque is hung in front of the vehicle, which may reach up behind it.
        Component name = VehicleDefinition.nameOf(id);
        FormattedCharSequence line = GuiStyle.fit(this.font, name, PREVIEW_WIDTH - 14);
        int plate = this.font.width(line) + 7;
        context.pose().pushPose();
        context.pose().translate(0.0F, 0.0F, 250.0F);
        context.fill(left + 2, top + 2, left + 2 + plate, top + 13, 0xB0101419);
        context.fill(left + 2, top + 2, left + 3, top + 13, GuiStyle.ACCENT);
        context.drawString(this.font, line, left + 6, top + 4, 0xFFFFFFFF, true);
        context.pose().popPose();
    }

    private void drawVehicle(GuiGraphics context, CarEntity car, ResourceLocation id, VehicleDefinition definition,
                             int left, int top, int right, int bottom) {
        int paint = this.menu.getPaint();
        car.setColor(paint >= 0 ? DyeColor.byId(paint) : definition.color().orElse(CarEntity.DEFAULT_COLOR));

        long now = Util.getMillis();
        long since = this.lastFrame < 0L ? 0L : Math.min(100L, now - this.lastFrame);
        this.lastFrame = now;
        if (!id.equals(this.shown)) {
            this.shown = id;
            this.shownAt = now;
        }
        // A full turn every twelve seconds, whirled round for a moment
        // after a build; held still while it is being dragged.
        float built = this.builtAt < 0L ? 1.0F : Math.min(1.0F, (now - this.builtAt) / BUILD_FLASH_MS);
        float pace = 1.0F + 9.0F * (1.0F - built) * (1.0F - built);
        if (!this.dragging) {
            this.standTurn += since / 12000.0F * Mth.TWO_PI * pace;
        }
        float turn = this.standTurn;

        // The turntable is a disc a little longer than the vehicle, seen from
        // the same height as the vehicle is. The vehicle is fitted to the stand
        // end on, its worst case: then its far end rises above its roof by the
        // disc's depth and its near end drops below its wheels by as much.
        float lean = Mth.sin(TILT_DEGREES * Mth.DEG_TO_RAD);
        float length = Math.max(2.0F, definition.width() * 2.6F);
        float radius = length * 0.54F;
        float fit = Math.min(PREVIEW_WIDTH * 0.62F / length,
                (PREVIEW_HEIGHT - 7) / (definition.height() * 0.93F + 2.0F * radius * lean));
        float rx = radius * fit;
        float ry = Math.max(3.0F, rx * lean);
        int middle = (left + right) / 2;
        int feet = bottom - Math.round(ry) - 4;
        this.drawTurntable(context, middle, feet, rx, ry, turn, left, top, right, bottom);

        // A new vehicle drops onto the turntable and settles with a small bounce.
        float arrived = this.shownAt < 0L ? 1.0F : Math.min(1.0F, (now - this.shownAt) / 350.0F);
        float settle = 1.0F - (1.0F - arrived) * (1.0F - arrived) * Mth.cos(arrived * 7.0F);
        Quaternionf tilt = new Quaternionf().rotateX(Mth.DEG_TO_RAD * -TILT_DEGREES);
        Quaternionf rotation = new Quaternionf().rotateZ(Mth.PI).mul(tilt).rotateY(turn);
        drawOnStand(context, car, fit * (0.8F + 0.2F * settle), rotation, tilt, middle, feet,
                left, top, right, bottom);

        if (built < 1.0F) {
            // The finished vehicle comes off the stand in a flash of light
            // that fades, with a burst of sparks thrown up and out.
            int alpha = (int) (200 * (1.0F - built) * (1.0F - built));
            context.fill(left, top, right, bottom, (alpha << 24) | 0xFFF4D8);
            for (int spark = 0; spark < 14; spark++) {
                float angle = (spark * 137.5F) * Mth.DEG_TO_RAD;
                float reach = built * (24.0F + (spark * 7 % 13) * 2.0F);
                int sparkX = middle + (int) (Mth.cos(angle) * reach * 1.6F);
                int sparkY = feet - (int) (Math.abs(Mth.sin(angle)) * reach) + (int) (built * built * 20.0F);
                if (sparkX > left && sparkX < right - 2 && sparkY > top && sparkY < bottom - 2) {
                    int glow = (int) (255 * (1.0F - built));
                    context.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, (glow << 24) | (spark % 3 == 0 ? 0xFFFFFF : 0xFFC040));
                }
            }
        }
    }

    /**
     * A round turntable seen from above and in front: its dark edge, the
     * steel top with a lighter ring, and twelve marks round the rim that
     * turn with the vehicle, so a slow turn still reads as a turn.
     */
    private void drawTurntable(GuiGraphics context, int cx, int cy, float rx, float ry, float turn,
                               int left, int top, int right, int bottom) {
        context.enableScissor(left, top, right, bottom);
        ellipse(context, cx, cy + 2, rx, ry, TURNTABLE_EDGE);
        ellipse(context, cx, cy, rx, ry, GuiStyle.lighter(TURNTABLE));
        ellipse(context, cx, cy, rx - 1, ry - 1, TURNTABLE);
        ellipse(context, cx, cy, rx * 0.55F, ry * 0.55F, GuiStyle.scale(TURNTABLE, 0.85F));
        for (int mark = 0; mark < 12; mark++) {
            // The rim's angle on screen runs the other way to the vehicle's
            // turn, because the stand looks at it upside down and mirrored.
            float angle = Mth.PI - turn + mark * Mth.TWO_PI / 12.0F;
            GuiStyle.dot(context, Math.round(cx + Mth.cos(angle) * (rx - 2.5F)),
                    Math.round(cy + Mth.sin(angle) * (ry - 1.5F)), 0xFF8C96A2);
        }
        context.disableScissor();
    }

    /** A filled ellipse, a row of pixels at a time. */
    private static void ellipse(GuiGraphics context, int cx, int cy, float rx, float ry, int colour) {
        if (rx < 1.0F || ry < 1.0F) {
            return;
        }
        int rows = (int) Math.ceil(ry);
        for (int dy = -rows; dy <= rows; dy++) {
            float across = 1.0F - (dy * dy) / (ry * ry);
            if (across <= 0.0F) {
                continue;
            }
            int half = Math.round(rx * Mth.sqrt(across));
            context.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, colour);
        }
    }

    private CarEntity standFor(ResourceLocation id, VehicleDefinition definition) {
        return this.stands.computeIfAbsent(id, key -> {
            if (this.minecraft == null || this.minecraft.level == null
                    || !(CarItem.itemFor(definition.chassis()) instanceof CarItem item)) {
                return null;
            }
            CarEntity car = item.entityType().create(this.minecraft.level);
            if (car != null) {
                car.setModel(key);
                car.refreshDimensions();
            }
            return car;
        });
    }

    /**
     * Seventeen paint chips, each with a gloss along its top. The factory
     * paint comes first, its corner marked so it does not read as just another
     * dye; the chosen chip is ringed in orange with a caret over it.
     */
    private void drawSwatches(GuiGraphics context, int mouseX, int mouseY) {
        int top = this.topPos + SWATCH_Y;
        int chosen = this.menu.getPaint();
        int hovered = this.swatchUnder(mouseX, mouseY);
        VehicleDefinition definition = this.menu.getSelected();
        int factory = (definition == null ? CarEntity.DEFAULT_COLOR
                : definition.color().orElse(CarEntity.DEFAULT_COLOR)).getTextureDiffuseColor();
        for (int column = 0; column <= 16; column++) {
            int x = this.leftPos + SWATCH_X + column * SWATCH;
            int colour = 0xFF000000 | (column == 0 ? factory : DyeColor.byId(column - 1).getTextureDiffuseColor());
            boolean picked = column - 1 == chosen;
            int size = SWATCH - 1;
            context.fill(x, top, x + size, top + size,
                    picked ? GuiStyle.ACCENT : column - 1 == hovered ? 0xFFE6E6E6 : GuiStyle.OUTLINE);
            context.fill(x + 1, top + 1, x + size - 1, top + size - 1, colour);
            context.fill(x + 1, top + 1, x + size - 1, top + 2, GuiStyle.lighter(colour));
            context.fill(x + 1, top + size - 2, x + size - 1, top + size - 1, GuiStyle.darker(colour));
            if (column == 0) {
                context.fill(x + 1, top + 1, x + 3, top + 2, 0xFFFFFFFF);
                context.fill(x + 1, top + 2, x + 2, top + 3, 0xFFFFFFFF);
            }
            if (picked) {
                context.fill(x + 1, top - 2, x + size - 1, top - 1, GuiStyle.ACCENT);
                context.fill(x + 2, top - 1, x + size - 2, top, GuiStyle.ACCENT);
            }
        }
    }

    /** The item position of material {@code index}'s cell on the blueprint. */
    private int[] materialAt(int index) {
        return new int[] {this.leftPos + MATERIALS_X + (index % MATERIALS_COLUMNS) * 18 + 1,
                this.topPos + MATERIALS_Y + (index / MATERIALS_COLUMNS) * 18 + 1};
    }

    /**
     * What the vehicle costs, each on the blueprint with its count: white when
     * the player has enough, red when a part is short, amber for the extras a
     * build goes ahead without.
     */
    private void drawMaterials(GuiGraphics context) {
        List<VehicleRecipe.Material> materials = this.menu.getMaterials();
        for (int index = 0; index < materials.size() && index < MATERIALS_COLUMNS * 2; index++) {
            VehicleRecipe.Material material = materials.get(index);
            int[] at = this.materialAt(index);
            boolean enough = this.menu.have(material.item()) >= material.count();
            boolean optional = VehicleWorkstationScreenHandler.isOptional(material.item());
            GuiStyle.material(context, this.font, new ItemStack(material.item(), material.count()),
                    at[0] - 1, at[1] - 1,
                    enough ? GuiStyle.COUNT_OK : optional ? OPTIONAL : GuiStyle.COUNT_MISSING,
                    !enough && !optional);
        }
    }

    private void drawTooltips(GuiGraphics context, int mouseX, int mouseY) {
        List<VehicleRecipe.Material> materials = this.menu.getMaterials();
        for (int index = 0; index < materials.size() && index < MATERIALS_COLUMNS * 2; index++) {
            int[] at = this.materialAt(index);
            if (mouseX >= at[0] && mouseX < at[0] + 16 && mouseY >= at[1] && mouseY < at[1] + 16) {
                VehicleRecipe.Material material = materials.get(index);
                ItemStack stack = new ItemStack(material.item(), material.count());
                List<Component> lines = new java.util.ArrayList<>(getTooltipFromItem(this.minecraft, stack));
                lines.add(Component.translatable("gui.overthought_vehicles.workstation.have",
                        this.menu.have(material.item()), material.count()).withStyle(ChatFormatting.GRAY));
                if (VehicleWorkstationScreenHandler.isOptional(material.item())) {
                    lines.add(Component.translatable("gui.overthought_vehicles.workstation.optional").withStyle(ChatFormatting.YELLOW));
                }
                context.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
        int swatch = this.swatchUnder(mouseX, mouseY);
        if (swatch >= -1) {
            context.renderTooltip(this.font, swatch < 0
                    ? Component.translatable("gui.overthought_vehicles.workstation.factory_paint")
                    : Component.translatable("color.minecraft." + DyeColor.byId(swatch).getName()), mouseX, mouseY);
            return;
        }
        // A name the list had to cut short is given in full under the cursor.
        int row = this.rowUnder(mouseX, mouseY);
        if (row >= 0) {
            Component name = VehicleDefinition.nameOf(this.menu.getVehicles().get(row));
            if (this.font.width(name) > NAME_ROOM) {
                context.renderTooltip(this.font, name, mouseX, mouseY);
            }
        }
    }

    /**
     * Draws a vehicle standing at {@code feetX, feetY}, the way the inventory
     * draws the player: lit as an item is, at the given scale and turn, and
     * clipped to the box it is shown in. It is drawn well in front of the
     * panel so no part of it, turned end on, dips behind the turntable.
     */
    private static void drawOnStand(GuiGraphics context, CarEntity car, float scale,
                                    Quaternionf rotation, Quaternionf tilt, int feetX, int feetY,
                                    int x1, int y1, int x2, int y2) {
        context.enableScissor(x1, y1, x2, y2);
        context.pose().pushPose();
        context.pose().translate(feetX, feetY, 100.0F);
        context.pose().scale(scale, scale, -scale);
        context.pose().mulPose(rotation);
        com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();
        net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher =
                Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.overrideCameraOrientation(new Quaternionf(tilt).conjugate());
        dispatcher.setRenderShadow(false);
        dispatcher.render(car, 0.0, 0.0, 0.0, 0.0F, 1.0F, context.pose(), context.bufferSource(), 0xF000F0);
        context.flush();
        dispatcher.setRenderShadow(true);
        context.pose().popPose();
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        context.disableScissor();
    }
}
