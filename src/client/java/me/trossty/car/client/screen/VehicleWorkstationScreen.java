package me.trossty.car.client.screen;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.item.CarItem;
import me.trossty.car.vehicle.VehicleDefinition;
import me.trossty.car.workbench.VehicleRecipe;
import me.trossty.car.workbench.VehicleWorkstationScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The vehicle workstation's window: the list of vehicles on the left, the
 * chosen one turning slowly on a stand on the right, its paint underneath and
 * what it costs under that. Drawn with plain rectangles, like the bench.
 */
public class VehicleWorkstationScreen extends AbstractContainerScreen<VehicleWorkstationScreenHandler> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF555555;
    private static final int SLOT_BODY = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SELECTED = 0xFF4A90D9;
    private static final int TEXT = 0xFF404040;
    private static final int STAND_TOP = 0xFF2B3A4E;
    private static final int STAND_BOTTOM = 0xFF151C26;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 80;
    private static final int ROW = 12;
    private static final int ROWS = 8;

    private static final int PREVIEW_X = 92;
    private static final int PREVIEW_Y = 18;
    private static final int PREVIEW_WIDTH = 156;
    private static final int PREVIEW_HEIGHT = 64;

    private static final int SWATCH_Y = 86;
    private static final int SWATCH = 9;

    private static final int MATERIALS_Y = 98;
    private static final int MATERIALS_COLUMNS = 8;

    private Button buildButton;
    private static final float BUILD_FLASH_MS = 900.0F;
    private int scroll;
    /** The stand's turn, in radians; it creeps round on its own and can be dragged. */
    private float standTurn;
    private long lastFrame = -1L;
    /** When the last build went through, for the flash and the fast spin; -1 for never. */
    private long builtAt = -1L;
    /** When the vehicle on the stand last changed, so the new one can settle on. */
    private long shownAt = -1L;
    private Identifier shown;
    private boolean dragging;
    private final Map<Identifier, CarEntity> stands = new HashMap<>();

    public VehicleWorkstationScreen(VehicleWorkstationScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 228;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = VehicleWorkstationScreenHandler.PLAYER_INVENTORY_LEFT;
        this.inventoryLabelY = VehicleWorkstationScreenHandler.PLAYER_INVENTORY_TOP - 11;
        this.buildButton = Button.builder(Component.translatable("gui.car.build"),
                        button -> {
                            if (this.menu.canBuild()) {
                                this.builtAt = Util.getMillis();
                            }
                            this.click(VehicleWorkstationScreenHandler.BUILD_ID);
                        })
                .bounds(this.leftPos + LIST_X, this.topPos + LIST_Y + ROWS * ROW + 4, LIST_WIDTH, 16)
                .build();
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
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (this.dragging) {
            this.standTurn += (float) offsetX * 0.03F;
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        this.dragging = false;
        return super.mouseReleased(click);
    }

    private int maxScroll() {
        return Math.max(0, this.menu.getVehicles().size() - ROWS);
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
        if (mouseX < left || mouseX >= left + LIST_WIDTH || mouseY < top || mouseY >= top + ROWS * ROW) {
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
        int column = (int) Math.floor((mouseX - this.leftPos - PREVIEW_X) / SWATCH);
        if (column < 0 || column > 16 || mouseX - this.leftPos - PREVIEW_X - column * SWATCH >= SWATCH - 1) {
            return -2;
        }
        return column - 1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (this.overStand(click.x(), click.y())) {
            this.dragging = true;
            return true;
        }
        int row = this.rowUnder(click.x(), click.y());
        if (row >= 0) {
            this.click(row);
            return true;
        }
        int swatch = this.swatchUnder(click.x(), click.y());
        if (swatch >= -1) {
            this.click(swatch < 0 ? VehicleWorkstationScreenHandler.FACTORY_PAINT_ID
                    : VehicleWorkstationScreenHandler.DYE_ID + swatch);
            return true;
        }
        return super.mouseClicked(click, doubled);
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
        context.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        context.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, TEXT, false);
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
        this.drawList(context, mouseX, mouseY);
        this.drawStand(context);
        this.drawSwatches(context);
        this.drawMaterials(context);
    }

    private static void drawSlot(GuiGraphics context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, SLOT_BODY);
        context.fill(x, y, x + 18, y + 1, SLOT_DARK);
        context.fill(x, y, x + 1, y + 18, SLOT_DARK);
        context.fill(x + 17, y + 1, x + 18, y + 18, PANEL_LIGHT);
        context.fill(x + 1, y + 17, x + 18, y + 18, PANEL_LIGHT);
    }

    private static void inset(GuiGraphics context, int x, int y, int width, int height, int fill) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, SLOT_DARK);
        context.fill(x, y, x + width + 1, y + height + 1, PANEL_LIGHT);
        context.fill(x, y, x + width, y + height, fill);
    }

    private void drawList(GuiGraphics context, int mouseX, int mouseY) {
        this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
        int left = this.leftPos + LIST_X;
        int top = this.topPos + LIST_Y;
        inset(context, left, top, LIST_WIDTH, ROWS * ROW, SLOT_BODY);
        List<Identifier> vehicles = this.menu.getVehicles();
        int hovered = this.rowUnder(mouseX, mouseY);
        for (int row = 0; row < ROWS && this.scroll + row < vehicles.size(); row++) {
            int index = this.scroll + row;
            int rowTop = top + row * ROW;
            if (index == this.menu.getSelectedIndex()) {
                context.fill(left, rowTop, left + LIST_WIDTH, rowTop + ROW, SELECTED);
            } else if (index == hovered) {
                context.fill(left, rowTop, left + LIST_WIDTH, rowTop + ROW, 0xFF9C9C9C);
            }
            String name = VehicleDefinition.nameOf(vehicles.get(index)).getString();
            // A long name is drawn smaller to fit rather than cut off; only
            // one too long even then loses its end.
            int room = LIST_WIDTH - 8;
            float scale = Math.max(0.7F, Math.min(1.0F, (float) room / this.font.width(name)));
            name = this.font.plainSubstrByWidth(name, (int) (room / scale));
            context.pose().pushMatrix();
            context.pose().translate(left + 3.0F, rowTop + 2.0F + (1.0F - scale) * 4.0F);
            context.pose().scale(scale, scale);
            context.drawString(this.font, name, 0, 0, 0xFFFFFFFF, true);
            context.pose().popMatrix();
        }
        if (this.maxScroll() > 0) {
            int trackHeight = ROWS * ROW;
            int knob = Math.max(8, trackHeight * ROWS / vehicles.size());
            int knobY = top + (trackHeight - knob) * this.scroll / this.maxScroll();
            context.fill(left + LIST_WIDTH - 3, knobY, left + LIST_WIDTH - 1, knobY + knob, PANEL_LIGHT);
        }
    }

    /** A dark stand with the chosen vehicle turning on it. */
    private void drawStand(GuiGraphics context) {
        int left = this.leftPos + PREVIEW_X;
        int top = this.topPos + PREVIEW_Y;
        inset(context, left, top, PREVIEW_WIDTH, PREVIEW_HEIGHT, STAND_BOTTOM);
        context.fillGradient(left, top, left + PREVIEW_WIDTH, top + PREVIEW_HEIGHT, STAND_TOP, STAND_BOTTOM);

        Identifier id = this.menu.getSelectedId();
        VehicleDefinition definition = this.menu.getSelected();
        if (id == null || definition == null) {
            return;
        }
        context.drawString(this.font, VehicleDefinition.nameOf(id), left + 4, top + 4, 0xFFFFFFFF, true);
        CarEntity car = this.standFor(id, definition);
        if (car == null) {
            return;
        }
        int paint = this.menu.getPaint();
        car.setColor(paint >= 0 ? DyeColor.byId(paint) : definition.color().orElse(CarEntity.DEFAULT_COLOR));

        Minecraft client = Minecraft.getInstance();
        EntityRenderState state = client.getEntityRenderDispatcher().getRenderer(car)
                .createRenderState(car, 1.0F);
        state.lightCoords = 0xF000F0;
        state.outlineColor = 0;
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
        // A new vehicle drops onto the stand and settles with a small bounce.
        float arrived = this.shownAt < 0L ? 1.0F : Math.min(1.0F, (now - this.shownAt) / 350.0F);
        float settle = 1.0F - (1.0F - arrived) * (1.0F - arrived) * Mth.cos(arrived * 7.0F);
        Quaternionf tilt = new Quaternionf().rotateX(Mth.DEG_TO_RAD * -22.0F);
        Quaternionf rotation = new Quaternionf().rotateZ(Mth.PI).mul(tilt).rotateY(turn);
        float length = Math.max(2.0F, definition.width() * 2.6F);
        float scale = Math.min(PREVIEW_WIDTH * 0.62F / length, (PREVIEW_HEIGHT - 14) / (definition.height() + 0.4F));
        scale *= 0.8F + 0.2F * settle;
        context.submitEntityRenderState(state, scale, new Vector3f(0.0F, car.getBbHeight() / 2.0F, 0.0F), rotation, tilt,
                left, top + 10, left + PREVIEW_WIDTH, top + PREVIEW_HEIGHT);
        if (built < 1.0F) {
            // The finished vehicle comes off the stand in a flash of light
            // that fades, with a burst of sparks thrown up and out.
            int alpha = (int) (200 * (1.0F - built) * (1.0F - built));
            context.fill(left, top, left + PREVIEW_WIDTH, top + PREVIEW_HEIGHT, (alpha << 24) | 0xFFF4D8);
            int middleX = left + PREVIEW_WIDTH / 2;
            int floorY = top + PREVIEW_HEIGHT - 8;
            for (int spark = 0; spark < 14; spark++) {
                float angle = (spark * 137.5F) * Mth.DEG_TO_RAD;
                float reach = built * (24.0F + (spark * 7 % 13) * 2.0F);
                int sparkX = middleX + (int) (Mth.cos(angle) * reach * 1.6F);
                int sparkY = floorY - (int) (Math.abs(Mth.sin(angle)) * reach) + (int) (built * built * 20.0F);
                if (sparkX > left && sparkX < left + PREVIEW_WIDTH - 2 && sparkY > top && sparkY < top + PREVIEW_HEIGHT - 2) {
                    int glow = (int) (255 * (1.0F - built));
                    context.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, (glow << 24) | (spark % 3 == 0 ? 0xFFFFFF : 0xFFC040));
                }
            }
        }
    }

    private CarEntity standFor(Identifier id, VehicleDefinition definition) {
        return this.stands.computeIfAbsent(id, key -> {
            if (this.minecraft == null || this.minecraft.level == null
                    || !(CarItem.itemFor(definition.chassis()) instanceof CarItem item)) {
                return null;
            }
            CarEntity car = item.entityType().create(this.minecraft.level, EntitySpawnReason.LOAD);
            if (car != null) {
                car.setModel(key);
                car.refreshDimensions();
            }
            return car;
        });
    }

    private void drawSwatches(GuiGraphics context) {
        int top = this.topPos + SWATCH_Y;
        int chosen = this.menu.getPaint();
        VehicleDefinition definition = this.menu.getSelected();
        int factory = (definition == null ? CarEntity.DEFAULT_COLOR
                : definition.color().orElse(CarEntity.DEFAULT_COLOR)).getTextureDiffuseColor();
        for (int column = 0; column <= 16; column++) {
            int left = this.leftPos + PREVIEW_X + column * SWATCH;
            int colour = column == 0 ? factory : DyeColor.byId(column - 1).getTextureDiffuseColor();
            boolean picked = column - 1 == chosen;
            context.fill(left, top, left + SWATCH - 1, top + SWATCH - 1, picked ? 0xFFFFFFFF : SLOT_DARK);
            context.fill(left + 1, top + 1, left + SWATCH - 2, top + SWATCH - 2, 0xFF000000 | colour);
            if (column == 0) {
                // The factory paint is marked so it does not read as just another dye.
                context.fill(left + 1, top + 1, left + 3, top + 3, 0xFFFFFFFF);
            }
        }
    }

    private int[] materialAt(int index) {
        return new int[] {this.leftPos + PREVIEW_X + (index % MATERIALS_COLUMNS) * 18 + 1,
                this.topPos + MATERIALS_Y + (index / MATERIALS_COLUMNS) * 18 + 1};
    }

    private void drawMaterials(GuiGraphics context) {
        List<VehicleRecipe.Material> materials = this.menu.getMaterials();
        for (int index = 0; index < materials.size() && index < MATERIALS_COLUMNS * 2; index++) {
            VehicleRecipe.Material material = materials.get(index);
            int[] at = this.materialAt(index);
            drawSlot(context, at[0] - 1, at[1] - 1);
            context.renderItem(new ItemStack(material.item(), material.count()), at[0], at[1]);
            boolean enough = this.menu.have(material.item()) >= material.count();
            boolean optional = VehicleWorkstationScreenHandler.isOptional(material.item());
            String count = String.valueOf(material.count());
            context.drawString(this.font, count,
                    at[0] + 17 - this.font.width(count), at[1] + 9,
                    enough ? 0xFFFFFFFF : optional ? 0xFFFFD34A : 0xFFFF5555);
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
                lines.add(Component.translatable("gui.car.workstation.have",
                        this.menu.have(material.item()), material.count()).withStyle(ChatFormatting.GRAY));
                if (VehicleWorkstationScreenHandler.isOptional(material.item())) {
                    lines.add(Component.translatable("gui.car.workstation.optional").withStyle(ChatFormatting.YELLOW));
                }
                context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
                return;
            }
        }
        int swatch = this.swatchUnder(mouseX, mouseY);
        if (swatch >= -1) {
            context.setTooltipForNextFrame(this.font, swatch < 0
                    ? Component.translatable("gui.car.workstation.factory_paint")
                    : Component.translatable("color.minecraft." + DyeColor.byId(swatch).getName()), mouseX, mouseY);
        }
    }
}
