package me.trossty.car.client.screen;

import me.trossty.car.CarMain;
import me.trossty.car.block.AsphaltBlock.Marking;
import me.trossty.car.item.RoadPaintItem;
import me.trossty.car.network.RoadMarkingC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

    private static final Identifier ASPHALT = CarMain.id("textures/block/asphalt.png");
    private static final int COLUMNS = 5;
    private static final int TILE = 32;
    private static final int CELL = 38;
    private static final int PAD = 10;
    private static final int HEADER = 24;
    private static final int FOOTER = 34;

    private static final int PANEL = 0xE0181C20;
    private static final int EDGE = 0xFF3A4048;
    private static final int HOVER = 0xFFB8C0C8;
    private static final int ERASE = 0xFFE0473B;

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

    /** Wide enough for the grid, or for the hint line under it if that is wider. */
    private int panelWidth() {
        int grid = COLUMNS * CELL + PAD * 2 - (CELL - TILE);
        return Math.max(grid, this.font.width(Component.translatable("screen.car.road_paint.hint")) + PAD * 2);
    }

    private int panelHeight() {
        return HEADER + rows() * CELL - (CELL - TILE) + FOOTER + PAD;
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
        context.fill(this.left - 1, this.top - 1, this.left + w + 1, this.top + h + 1, EDGE);
        context.fill(this.left, this.top, this.left + w, this.top + h, PANEL);
        // A stripe of the tin's paint along the top, like the label on the tin.
        context.fill(this.left, this.top, this.left + w, this.top + 2, paint);

        context.drawCenteredString(this.font, tin.getHoverName(), this.left + w / 2, this.top + 9, paint);

        int hovered = this.cellAt(mouseX, mouseY);
        int current = RoadPaintItem.selected(tin).ordinal();
        boolean empty = RoadPaintItem.isEmpty(tin);
        for (int i = 0; i < Marking.values().length; i++) {
            Marking marking = Marking.values()[i];
            int x = this.cellX(i);
            int y = this.cellY(i);
            int ring = i == current ? paint : i == hovered || i == this.cursor ? HOVER : 0;
            if (ring != 0) {
                context.fill(x - 2, y - 2, x + TILE + 2, y + TILE + 2, ring);
            }
            context.blit(RenderPipelines.GUI_TEXTURED, ASPHALT, x, y, 0, 0, TILE, TILE, 16, 16, 16, 16);
            if (marking == Marking.NONE) {
                this.cross(context, x, y);
            } else {
                // An empty tin shows what it would paint, faded.
                int colour = empty ? 0x60000000 | item.paint.rgb : paint;
                context.blit(RenderPipelines.GUI_TEXTURED,
                        CarMain.id("textures/block/marking/" + marking.getSerializedName() + ".png"),
                        x, y, 0, 0, TILE, TILE, 16, 16, 16, 16, colour);
            }
            if (i < 9) {
                context.drawString(this.font, Component.literal(String.valueOf(i + 1)), x + 2, y + 2, 0xC0FFFFFF, true);
            }
        }

        int shown = hovered >= 0 ? hovered : this.cursor;
        Marking name = Marking.values()[shown];
        Component label = Component.translatable(name.translationKey());
        if (name == Marking.NONE) {
            label = label.copy().append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable("screen.car.road_paint.erase_note").withStyle(ChatFormatting.GRAY));
        }
        int footer = this.top + h - FOOTER - PAD + 6;
        context.drawCenteredString(this.font, label, this.left + w / 2, footer, 0xFFFFFFFF);
        int left = tin.getMaxDamage() - tin.getDamageValue();
        context.drawCenteredString(this.font,
                Component.translatable("screen.car.road_paint.left", left, tin.getMaxDamage()),
                this.left + w / 2, footer + 12, empty ? ERASE : 0xFFA0A8B0);
        context.drawCenteredString(this.font, Component.translatable("screen.car.road_paint.hint"),
                this.left + w / 2, footer + 24, 0xFF707880);
    }

    /** "Erase": a red cross over bare road. */
    private void cross(GuiGraphics context, int x, int y) {
        for (int k = 6; k < TILE - 6; k++) {
            context.fill(x + k, y + k, x + k + 3, y + k + 2, ERASE);
            context.fill(x + TILE - k - 3, y + k, x + TILE - k, y + k + 2, ERASE);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int cell = this.cellAt(click.x(), click.y());
        if (cell >= 0) {
            this.pick(cell);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
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
                if (this.minecraft != null && this.minecraft.options.keyInventory.matches(input)) {
                    this.onClose();
                    return true;
                }
                return super.keyPressed(input);
            }
        }
        return true;
    }

    private void pick(int index) {
        if (index < 0 || index >= Marking.values().length) {
            return;
        }
        ClientPlayNetworking.send(new RoadMarkingC2SPayload(this.hand == InteractionHand.OFF_HAND, index));
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
