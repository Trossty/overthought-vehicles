package me.trossty.car.client.screen;

import me.trossty.car.CarMain;
import me.trossty.car.block.AsphaltBlock.Marking;
import me.trossty.car.item.RoadPaintItem;
import me.trossty.car.network.RoadMarkingC2SPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The road paint tin's menu: every marking it can paint, each shown as a
 * square of road with that marking on it in the tin's own colour, the way it
 * will look from above with the painter facing up the screen. The first square
 * is "erase". A click picks one and closes; so do the arrows and Enter, or a
 * number key for the first nine.
 */
public class RoadMarkingScreen extends Screen {

    private static final ResourceLocation ASPHALT = CarMain.id("textures/block/asphalt.png");
    private static final int COLUMNS = 5;
    private static final int TILE = 32;
    private static final int CELL = 38;
    private static final int PAD = 10;
    /** The nameplate and the gap under it, down to the first row of tiles. */
    private static final int HEADER = 22;
    /** The LCD with the marking's name, the paint gauge, the hint and the margin under them. */
    private static final int FOOTER = 54;
    private static final int GAUGE_WIDTH = 49;

    private static final int HOVER = 0xFFB8C0C8;
    private static final int ERASE = 0xFFE0473B;
    private static final int ERASE_TEXT = 0xFFFF7A64;
    private static final int HINT = 0xFF6A6F75;

    private final InteractionHand hand;
    private int cursor;
    private int left;
    private int top;

    public RoadMarkingScreen(InteractionHand hand) {
        super(Component.translatable("screen.car.road_paint"));
        this.hand = hand;
    }

    private ItemStack tin() {
        return this.minecraft == null || this.minecraft.player == null ? ItemStack.EMPTY
                : this.minecraft.player.getItemInHand(this.hand);
    }

    private static int rows() {
        return (Marking.values().length + COLUMNS - 1) / COLUMNS;
    }

    private static int gridWidth() {
        return COLUMNS * CELL - (CELL - TILE);
    }

    private static int gridHeight() {
        return rows() * CELL - (CELL - TILE);
    }

    /** Wide enough for the grid, or for the hint line under it if that is wider. */
    private int panelWidth() {
        return Math.max(gridWidth(), this.font.width(Component.translatable("screen.car.road_paint.hint"))) + PAD * 2;
    }

    private int panelHeight() {
        return HEADER + gridHeight() + FOOTER;
    }

    @Override
    protected void init() {
        this.left = (this.width - this.panelWidth()) / 2;
        this.top = (this.height - this.panelHeight()) / 2;
        this.cursor = RoadPaintItem.selected(this.tin()).ordinal();
    }

    @Override
    public void tick() {
        // Put away, dropped or swapped: there is nothing left to choose for.
        if (!(this.tin().getItem() instanceof RoadPaintItem)) {
            this.onClose();
        }
    }

    /** Where cell i sits: rows of five, the last row centred. */
    private int cellX(int i) {
        int row = i / COLUMNS;
        int inRow = Math.min(COLUMNS, Marking.values().length - row * COLUMNS);
        int grid = inRow * CELL - (CELL - TILE);
        return this.left + (this.panelWidth() - grid) / 2 + (i % COLUMNS) * CELL;
    }

    private int cellY(int i) {
        return this.top + HEADER + (i / COLUMNS) * CELL;
    }

    private int cellAt(double x, double y) {
        for (int i = 0; i < Marking.values().length; i++) {
            int cx = this.cellX(i);
            int cy = this.cellY(i);
            if (x >= cx - 1 && x < cx + TILE + 1 && y >= cy - 1 && y < cy + TILE + 1) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        ItemStack tin = this.tin();
        if (!(tin.getItem() instanceof RoadPaintItem item)) {
            return;
        }
        int paint = 0xFF000000 | item.paint.rgb;
        int w = this.panelWidth();
        int h = this.panelHeight();
        // The workshop's own window, its nameplate underlined in the tin's paint
        // the way the label goes round a tin.
        GuiStyle.frame(context, this.left, this.top, w, h, paint);
        context.pose().pushPose();
        context.pose().translate(this.left, this.top, 0.0F);
        GuiStyle.title(context, this.font, tin.getHoverName(), w - GuiStyle.TITLE_X - 10);
        context.pose().popPose();

        // The squares of road lie in a dark tray.
        int gridLeft = this.left + (w - gridWidth()) / 2;
        int gridTop = this.top + HEADER;
        GuiStyle.well(context, gridLeft - 4, gridTop - 4, gridWidth() + 8, gridHeight() + 8);

        int hovered = this.cellAt(mouseX, mouseY);
        int current = RoadPaintItem.selected(tin).ordinal();
        boolean empty = RoadPaintItem.isEmpty(tin);
        for (int i = 0; i < Marking.values().length; i++) {
            this.tile(context, Marking.values()[i], i, paint, item.paint.rgb, empty,
                    i == current, i == hovered || i == this.cursor);
        }

        // The marking under the mouse (or the arrow keys' cursor) by name, on the LCD.
        int shown = hovered >= 0 ? hovered : this.cursor;
        Marking name = Marking.values()[shown];
        Component label = Component.translatable(name.translationKey());
        if (name == Marking.NONE) {
            label = label.copy().append(Component.literal(" · "))
                    .append(Component.translatable("screen.car.road_paint.erase_note"));
        }
        int footer = gridTop + gridHeight() + 10;
        GuiStyle.readout(context, this.font, label, gridLeft - 4, footer, gridWidth() + 8,
                name == Marking.NONE ? ERASE_TEXT : GuiStyle.READOUT_TEXT);

        int lineY = footer + GuiStyle.READOUT_HEIGHT + 6;
        this.paintLeft(context, tin, paint, empty, lineY);
        context.drawCenteredString(this.font, Component.translatable("screen.car.road_paint.hint"),
                this.left + w / 2, lineY + 14, HINT);
    }

    /** One square of road with its marking painted on, in its ring. */
    private void tile(GuiGraphics context, Marking marking, int i, int paint, int rgb, boolean empty,
                      boolean current, boolean lit) {
        int x = this.cellX(i);
        int y = this.cellY(i);
        int ring = current ? paint : lit ? HOVER : GuiStyle.OUTLINE;
        context.fill(x - 2, y - 2, x + TILE + 2, y + TILE + 2, ring);
        if (current) {
            context.fill(x - 1, y - 1, x + TILE + 1, y + TILE + 1, GuiStyle.darker(paint));
        }
        context.blit(ASPHALT, x, y, TILE, TILE, 0, 0, 16, 16, 16, 16);
        if (marking == Marking.NONE) {
            this.cross(context, x, y);
        } else {
            // An empty tin shows what it would paint, faded.
            int colour = empty ? 0x60000000 | rgb : paint;
            context.setColor(((colour >> 16) & 0xFF) / 255.0F, ((colour >> 8) & 0xFF) / 255.0F,
                    (colour & 0xFF) / 255.0F, (colour >>> 24) / 255.0F);
            context.blit(CarMain.id("textures/block/marking/" + marking.getSerializedName() + ".png"),
                    x, y, TILE, TILE, 0, 0, 16, 16, 16, 16);
            context.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        // Daylight falling across the road from above.
        context.fillGradient(x, y, x + TILE, y + TILE / 2, 0x18FFFFFF, 0x00FFFFFF);
        if (i < 9) {
            // The number key that picks it, on a little keycap in the corner.
            context.fill(x + 1, y + 1, x + 9, y + 11, 0xC0101419);
            context.fill(x + 1, y + 1, x + 9, y + 2, 0x40FFFFFF);
            context.drawString(this.font, String.valueOf(i + 1), x + 3, y + 2, 0xFFFFFFFF, false);
        }
    }

    /** How much paint is left, in words and as a row of LEDs in the paint's colour. */
    private void paintLeft(GuiGraphics context, ItemStack tin, int paint, boolean empty, int y) {
        int left = tin.getMaxDamage() - tin.getDamageValue();
        Component words = Component.translatable("screen.car.road_paint.left", left, tin.getMaxDamage());
        int textWidth = this.font.width(words);
        int x = this.left + (this.panelWidth() - textWidth - 5 - GAUGE_WIDTH) / 2;
        context.drawString(this.font, words, x, y, empty ? ERASE : GuiStyle.TEXT, false);

        int gauge = x + textWidth + 5;
        context.fill(gauge - 1, y - 1, gauge + GAUGE_WIDTH + 1, y + 8, GuiStyle.OUTLINE);
        context.fill(gauge, y, gauge + GAUGE_WIDTH, y + 7, 0xFF101316);
        int segments = (GAUGE_WIDTH - 1) / 4;
        int lit = left <= 0 ? 0 : Math.max(1, Math.round(segments * (float) left / tin.getMaxDamage()));
        for (int segment = 0; segment < segments; segment++) {
            int sx = gauge + 1 + segment * 4;
            context.fill(sx, y + 1, sx + 3, y + 6, segment < lit ? paint : GuiStyle.LED_OFF);
        }
    }

    /** "Erase": a red cross over bare road. */
    private void cross(GuiGraphics context, int x, int y) {
        for (int k = 6; k < TILE - 6; k++) {
            context.fill(x + k, y + k, x + k + 3, y + k + 2, ERASE);
            context.fill(x + TILE - k - 3, y + k, x + TILE - k, y + k + 2, ERASE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cell = this.cellAt(mouseX, mouseY);
        if (cell >= 0) {
            this.pick(cell);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode;
        int count = Marking.values().length;
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            this.pick(key - GLFW.GLFW_KEY_1);
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> this.cursor = Math.floorMod(this.cursor - 1, count);
            case GLFW.GLFW_KEY_RIGHT -> this.cursor = Math.floorMod(this.cursor + 1, count);
            case GLFW.GLFW_KEY_UP -> this.cursor = Math.max(0, this.cursor - COLUMNS);
            case GLFW.GLFW_KEY_DOWN -> this.cursor = Math.min(count - 1, this.cursor + COLUMNS);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> this.pick(this.cursor);
            default -> {
                // The inventory key closes it too, the way it closes a container.
                if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                    this.onClose();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    private void pick(int index) {
        if (index < 0 || index >= Marking.values().length) {
            return;
        }
        PacketDistributor.sendToServer(new RoadMarkingC2SPayload(this.hand == InteractionHand.OFF_HAND, index));
        if (this.minecraft != null && this.minecraft.player != null) {
            // Show the choice at once, before the server's copy comes back.
            RoadPaintItem.select(this.tin(), Marking.values()[index]);
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, 1.4F);
        }
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
