package me.trossty.car.client.screen;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.item.CarItem;
import me.trossty.car.vehicle.VehicleDefinition;
import me.trossty.car.workbench.VehicleRecipe;
import me.trossty.car.workbench.VehicleWorkstationScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
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
public class VehicleWorkstationScreen extends HandledScreen<VehicleWorkstationScreenHandler> {

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

    private ButtonWidget buildButton;
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

    public VehicleWorkstationScreen(VehicleWorkstationScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 256;
        this.backgroundHeight = 228;
    }

    @Override
    protected void init() {
        super.init();
        this.playerInventoryTitleX = VehicleWorkstationScreenHandler.PLAYER_INVENTORY_LEFT;
        this.playerInventoryTitleY = VehicleWorkstationScreenHandler.PLAYER_INVENTORY_TOP - 11;
        this.buildButton = ButtonWidget.builder(Text.translatable("gui.car.build"),
                        button -> {
                            if (this.handler.canBuild()) {
                                this.builtAt = Util.getMeasuringTimeMs();
                            }
                            this.click(VehicleWorkstationScreenHandler.BUILD_ID);
                        })
                .dimensions(this.x + LIST_X, this.y + LIST_Y + ROWS * ROW + 4, LIST_WIDTH, 16)
                .build();
        this.addDrawableChild(this.buildButton);
    }

    private void click(int id) {
        if (this.client != null && this.client.interactionManager != null) {
            this.client.interactionManager.clickButton(this.handler.syncId, id);
        }
    }

    private boolean overStand(double mouseX, double mouseY) {
        int left = this.x + PREVIEW_X;
        int top = this.y + PREVIEW_Y;
        return mouseX >= left && mouseX < left + PREVIEW_WIDTH && mouseY >= top && mouseY < top + PREVIEW_HEIGHT;
    }

    /** Dragging across the stand turns the vehicle on it, to look it over. */
    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (this.dragging) {
            this.standTurn += (float) offsetX * 0.03F;
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.dragging = false;
        return super.mouseReleased(click);
    }

    private int maxScroll() {
        return Math.max(0, this.handler.getVehicles().size() - ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0.0 && this.maxScroll() > 0) {
            this.scroll = MathHelper.clamp(this.scroll - (int) Math.signum(vertical), 0, this.maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    /** The list row under the cursor, as a vehicle index, or -1. */
    private int rowUnder(double mouseX, double mouseY) {
        int left = this.x + LIST_X;
        int top = this.y + LIST_Y;
        if (mouseX < left || mouseX >= left + LIST_WIDTH || mouseY < top || mouseY >= top + ROWS * ROW) {
            return -1;
        }
        int index = this.scroll + (int) (mouseY - top) / ROW;
        return index < this.handler.getVehicles().size() ? index : -1;
    }

    /** The paint swatch under the cursor: -1 factory, 0-15 a dye, or -2 for none. */
    private int swatchUnder(double mouseX, double mouseY) {
        int top = this.y + SWATCH_Y;
        if (mouseY < top || mouseY >= top + SWATCH - 1) {
            return -2;
        }
        int column = (int) Math.floor((mouseX - this.x - PREVIEW_X) / SWATCH);
        if (column < 0 || column > 16 || mouseX - this.x - PREVIEW_X - column * SWATCH >= SWATCH - 1) {
            return -2;
        }
        return column - 1;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
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
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.buildButton.active = this.handler.canBuild();
        super.render(context, mouseX, mouseY, deltaTicks);
        this.drawTooltips(context, mouseX, mouseY);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, TEXT, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle,
                this.playerInventoryTitleX, this.playerInventoryTitleY, TEXT, false);
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
        for (net.minecraft.screen.slot.Slot slot : this.handler.slots) {
            drawSlot(context, left + slot.x - 1, top + slot.y - 1);
        }
        this.drawList(context, mouseX, mouseY);
        this.drawStand(context);
        this.drawSwatches(context);
        this.drawMaterials(context);
    }

    private static void drawSlot(DrawContext context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, SLOT_BODY);
        context.fill(x, y, x + 18, y + 1, SLOT_DARK);
        context.fill(x, y, x + 1, y + 18, SLOT_DARK);
        context.fill(x + 17, y + 1, x + 18, y + 18, PANEL_LIGHT);
        context.fill(x + 1, y + 17, x + 18, y + 18, PANEL_LIGHT);
    }

    private static void inset(DrawContext context, int x, int y, int width, int height, int fill) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, SLOT_DARK);
        context.fill(x, y, x + width + 1, y + height + 1, PANEL_LIGHT);
        context.fill(x, y, x + width, y + height, fill);
    }

    private void drawList(DrawContext context, int mouseX, int mouseY) {
        this.scroll = MathHelper.clamp(this.scroll, 0, this.maxScroll());
        int left = this.x + LIST_X;
        int top = this.y + LIST_Y;
        inset(context, left, top, LIST_WIDTH, ROWS * ROW, SLOT_BODY);
        List<Identifier> vehicles = this.handler.getVehicles();
        int hovered = this.rowUnder(mouseX, mouseY);
        for (int row = 0; row < ROWS && this.scroll + row < vehicles.size(); row++) {
            int index = this.scroll + row;
            int rowTop = top + row * ROW;
            if (index == this.handler.getSelectedIndex()) {
                context.fill(left, rowTop, left + LIST_WIDTH, rowTop + ROW, SELECTED);
            } else if (index == hovered) {
                context.fill(left, rowTop, left + LIST_WIDTH, rowTop + ROW, 0xFF9C9C9C);
            }
            String name = VehicleDefinition.nameOf(vehicles.get(index)).getString();
            // A long name is drawn smaller to fit rather than cut off; only
            // one too long even then loses its end.
            int room = LIST_WIDTH - 8;
            float scale = Math.max(0.7F, Math.min(1.0F, (float) room / this.textRenderer.getWidth(name)));
            name = this.textRenderer.trimToWidth(name, (int) (room / scale));
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(left + 3.0F, rowTop + 2.0F + (1.0F - scale) * 4.0F);
            context.getMatrices().scale(scale, scale);
            context.drawText(this.textRenderer, name, 0, 0, 0xFFFFFFFF, true);
            context.getMatrices().popMatrix();
        }
        if (this.maxScroll() > 0) {
            int trackHeight = ROWS * ROW;
            int knob = Math.max(8, trackHeight * ROWS / vehicles.size());
            int knobY = top + (trackHeight - knob) * this.scroll / this.maxScroll();
            context.fill(left + LIST_WIDTH - 3, knobY, left + LIST_WIDTH - 1, knobY + knob, PANEL_LIGHT);
        }
    }

    /** A dark stand with the chosen vehicle turning on it. */
    private void drawStand(DrawContext context) {
        int left = this.x + PREVIEW_X;
        int top = this.y + PREVIEW_Y;
        inset(context, left, top, PREVIEW_WIDTH, PREVIEW_HEIGHT, STAND_BOTTOM);
        context.fillGradient(left, top, left + PREVIEW_WIDTH, top + PREVIEW_HEIGHT, STAND_TOP, STAND_BOTTOM);

        Identifier id = this.handler.getSelectedId();
        VehicleDefinition definition = this.handler.getSelected();
        if (id == null || definition == null) {
            return;
        }
        context.drawText(this.textRenderer, VehicleDefinition.nameOf(id), left + 4, top + 4, 0xFFFFFFFF, true);
        CarEntity car = this.standFor(id, definition);
        if (car == null) {
            return;
        }
        int paint = this.handler.getPaint();
        car.setColor(paint >= 0 ? DyeColor.byIndex(paint) : definition.color().orElse(CarEntity.DEFAULT_COLOR));

        MinecraftClient client = MinecraftClient.getInstance();
        EntityRenderState state = client.getEntityRenderDispatcher().getRenderer(car)
                .getAndUpdateRenderState(car, 1.0F);
        state.light = 0xF000F0;
        state.outlineColor = 0;
        long now = Util.getMeasuringTimeMs();
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
            this.standTurn += since / 12000.0F * MathHelper.TAU * pace;
        }
        float turn = this.standTurn;
        // A new vehicle drops onto the stand and settles with a small bounce.
        float arrived = this.shownAt < 0L ? 1.0F : Math.min(1.0F, (now - this.shownAt) / 350.0F);
        float settle = 1.0F - (1.0F - arrived) * (1.0F - arrived) * MathHelper.cos(arrived * 7.0F);
        Quaternionf tilt = new Quaternionf().rotateX(MathHelper.RADIANS_PER_DEGREE * -22.0F);
        Quaternionf rotation = new Quaternionf().rotateZ(MathHelper.PI).mul(tilt).rotateY(turn);
        float length = Math.max(2.0F, definition.width() * 2.6F);
        float scale = Math.min(PREVIEW_WIDTH * 0.62F / length, (PREVIEW_HEIGHT - 14) / (definition.height() + 0.4F));
        scale *= 0.8F + 0.2F * settle;
        context.addEntity(state, scale, new Vector3f(0.0F, car.getHeight() / 2.0F, 0.0F), rotation, tilt,
                left, top + 10, left + PREVIEW_WIDTH, top + PREVIEW_HEIGHT);
        if (built < 1.0F) {
            // The finished vehicle comes off the stand in a flash of light
            // that fades, with a burst of sparks thrown up and out.
            int alpha = (int) (200 * (1.0F - built) * (1.0F - built));
            context.fill(left, top, left + PREVIEW_WIDTH, top + PREVIEW_HEIGHT, (alpha << 24) | 0xFFF4D8);
            int middleX = left + PREVIEW_WIDTH / 2;
            int floorY = top + PREVIEW_HEIGHT - 8;
            for (int spark = 0; spark < 14; spark++) {
                float angle = (spark * 137.5F) * MathHelper.RADIANS_PER_DEGREE;
                float reach = built * (24.0F + (spark * 7 % 13) * 2.0F);
                int sparkX = middleX + (int) (MathHelper.cos(angle) * reach * 1.6F);
                int sparkY = floorY - (int) (Math.abs(MathHelper.sin(angle)) * reach) + (int) (built * built * 20.0F);
                if (sparkX > left && sparkX < left + PREVIEW_WIDTH - 2 && sparkY > top && sparkY < top + PREVIEW_HEIGHT - 2) {
                    int glow = (int) (255 * (1.0F - built));
                    context.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, (glow << 24) | (spark % 3 == 0 ? 0xFFFFFF : 0xFFC040));
                }
            }
        }
    }

    private CarEntity standFor(Identifier id, VehicleDefinition definition) {
        return this.stands.computeIfAbsent(id, key -> {
            if (this.client == null || this.client.world == null
                    || !(CarItem.itemFor(definition.chassis()) instanceof CarItem item)) {
                return null;
            }
            CarEntity car = item.entityType().create(this.client.world, SpawnReason.LOAD);
            if (car != null) {
                car.setModel(key);
                car.calculateDimensions();
            }
            return car;
        });
    }

    private void drawSwatches(DrawContext context) {
        int top = this.y + SWATCH_Y;
        int chosen = this.handler.getPaint();
        VehicleDefinition definition = this.handler.getSelected();
        int factory = (definition == null ? CarEntity.DEFAULT_COLOR
                : definition.color().orElse(CarEntity.DEFAULT_COLOR)).getEntityColor();
        for (int column = 0; column <= 16; column++) {
            int left = this.x + PREVIEW_X + column * SWATCH;
            int colour = column == 0 ? factory : DyeColor.byIndex(column - 1).getEntityColor();
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
        return new int[] {this.x + PREVIEW_X + (index % MATERIALS_COLUMNS) * 18 + 1,
                this.y + MATERIALS_Y + (index / MATERIALS_COLUMNS) * 18 + 1};
    }

    private void drawMaterials(DrawContext context) {
        List<VehicleRecipe.Material> materials = this.handler.getMaterials();
        for (int index = 0; index < materials.size() && index < MATERIALS_COLUMNS * 2; index++) {
            VehicleRecipe.Material material = materials.get(index);
            int[] at = this.materialAt(index);
            drawSlot(context, at[0] - 1, at[1] - 1);
            context.drawItem(new ItemStack(material.item(), material.count()), at[0], at[1]);
            boolean enough = this.handler.have(material.item()) >= material.count();
            boolean optional = VehicleWorkstationScreenHandler.isOptional(material.item());
            String count = String.valueOf(material.count());
            context.drawTextWithShadow(this.textRenderer, count,
                    at[0] + 17 - this.textRenderer.getWidth(count), at[1] + 9,
                    enough ? 0xFFFFFFFF : optional ? 0xFFFFD34A : 0xFFFF5555);
        }
    }

    private void drawTooltips(DrawContext context, int mouseX, int mouseY) {
        List<VehicleRecipe.Material> materials = this.handler.getMaterials();
        for (int index = 0; index < materials.size() && index < MATERIALS_COLUMNS * 2; index++) {
            int[] at = this.materialAt(index);
            if (mouseX >= at[0] && mouseX < at[0] + 16 && mouseY >= at[1] && mouseY < at[1] + 16) {
                VehicleRecipe.Material material = materials.get(index);
                ItemStack stack = new ItemStack(material.item(), material.count());
                List<Text> lines = new java.util.ArrayList<>(getTooltipFromItem(this.client, stack));
                lines.add(Text.translatable("gui.car.workstation.have",
                        this.handler.have(material.item()), material.count()).formatted(Formatting.GRAY));
                if (VehicleWorkstationScreenHandler.isOptional(material.item())) {
                    lines.add(Text.translatable("gui.car.workstation.optional").formatted(Formatting.YELLOW));
                }
                context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
                return;
            }
        }
        int swatch = this.swatchUnder(mouseX, mouseY);
        if (swatch >= -1) {
            context.drawTooltip(this.textRenderer, swatch < 0
                    ? Text.translatable("gui.car.workstation.factory_paint")
                    : Text.translatable("color.minecraft." + DyeColor.byIndex(swatch).getId()), mouseX, mouseY);
        }
    }
}
