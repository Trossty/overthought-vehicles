package me.trossty.car.client.screen;

import me.trossty.car.entity.CarPart;
import me.trossty.car.workbench.VehicleRecipe;
import me.trossty.car.workbench.VehicleWorkbenchScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The workbench window. The panel is drawn with plain rectangles rather than a
 * texture: it is the same flat palette vanilla's widget sheet uses, and this way
 * the mod ships no GUI image and nothing can go missing at load time.
 */
public class VehicleWorkbenchScreen extends AbstractContainerScreen<VehicleWorkbenchScreenHandler> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF555555;
    private static final int SLOT_BODY = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SELECTED = 0xFF4A90D9;
    private static final int TEXT = 0xFF404040;
    private static final int COUNT_OK = 0xFFFFFFFF;
    private static final int COUNT_MISSING = 0xFFFF5555;

    private static final int RECIPE_LIST_X = 8;
    private static final int RECIPE_LIST_Y = 18;
    /** One slot plus a hairline, so neighbouring entries do not merge. */
    private static final int RECIPE_ENTRY = 19;
    /**
     * The catalogue outgrew a single column the moment the bench started making
     * service parts, so it is a grid that scrolls. Four across and three down
     * leaves the materials panel exactly where it was.
     */
    private static final int RECIPE_COLUMNS = 4;
    private static final int RECIPE_ROWS = 3;
    private static final int SCROLLBAR_X = RECIPE_LIST_X + RECIPE_COLUMNS * RECIPE_ENTRY + 2;
    private static final int SCROLLBAR_WIDTH = 6;

    private static final int MATERIALS_X = 104;
    private static final int MATERIALS_Y = 18;
    /** Three across, so even the six-part tanker stays clear of the button. */
    private static final int MATERIALS_COLUMNS = 3;

    /**
     * The four tier buttons — iron, gold, diamond, netherite — on the line
     * between the materials and the build button, each showing its material.
     */
    private static final int QUALITY_X = 104;
    private static final int QUALITY_Y = 56;
    private static final int QUALITY_WIDTH = 15;
    private static final int QUALITY_HEIGHT = 14;
    private static final int QUALITY_GAP = 1;

    private static final int BUILD_X = 104;
    private static final int BUILD_Y = 72;
    private static final int BUILD_WIDTH = 64;
    private static final int BUILD_HEIGHT = 16;

    private Button buildButton;
    private final java.util.List<Button> qualityButtons = new java.util.ArrayList<>();
    /** First visible row of the catalogue. */
    private int scrollRow;

    public VehicleWorkbenchScreen(VehicleWorkbenchScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        // Taller than vanilla's by the row of material slots the catalogue used
        // to be sitting on top of, plus the line its heading needs.
        this.imageHeight = 204;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        this.buildButton = Button.builder(
                        Component.translatable(this.menu.getActionKey()), button -> this.sendBuild())
                .bounds(this.leftPos + BUILD_X, this.topPos + BUILD_Y, BUILD_WIDTH, BUILD_HEIGHT)
                .build();
        this.addRenderableWidget(this.buildButton);

        this.qualityButtons.clear();
        for (CarPart.Quality quality : CarPart.Quality.values()) {
            int index = quality.ordinal();
            Button button = Button.builder(
                            Component.empty(),
                            whatever -> this.sendSelect(
                                    VehicleWorkbenchScreenHandler.QUALITY_BUTTON_ID + index))
                    .bounds(this.leftPos + QUALITY_X + index * (QUALITY_WIDTH + QUALITY_GAP),
                            this.topPos + QUALITY_Y, QUALITY_WIDTH, QUALITY_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("quality.car." + quality.key())
                            .withStyle(quality.formatting())
                            .append(Component.literal("\n"))
                            .append(Component.translatable("gui.car.quality_tip." + quality.key())
                                    .withStyle(net.minecraft.ChatFormatting.GRAY))))
                    .build();
            this.qualityButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    private void sendBuild() {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(
                this.menu.containerId, VehicleWorkbenchScreenHandler.BUILD_BUTTON_ID);
    }

    private void sendSelect(int index) {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
    }

    /** How many rows the catalogue has in total. */
    private int totalRows() {
        int count = this.menu.getRecipes().size();
        return (count + RECIPE_COLUMNS - 1) / RECIPE_COLUMNS;
    }

    /** The furthest down the list can be pushed. Zero when it all fits. */
    private int maxScroll() {
        return Math.max(0, this.totalRows() - RECIPE_ROWS);
    }

    /** Where entry {@code index} is drawn, or null when it is scrolled away. */
    private int[] entryAt(int index) {
        int row = index / RECIPE_COLUMNS - this.scrollRow;
        if (row < 0 || row >= RECIPE_ROWS) {
            return null;
        }
        return new int[] {
                this.leftPos + RECIPE_LIST_X + (index % RECIPE_COLUMNS) * RECIPE_ENTRY,
                this.topPos + RECIPE_LIST_Y + row * RECIPE_ENTRY};
    }

    /** The entry under the cursor, or -1. */
    private int entryUnder(double mouseX, double mouseY) {
        for (int index = 0; index < this.menu.getRecipes().size(); index++) {
            int[] at = this.entryAt(index);
            if (at != null && mouseX >= at[0] && mouseX < at[0] + 18
                    && mouseY >= at[1] && mouseY < at[1] + 18) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.maxScroll() > 0 && vertical != 0.0) {
            this.scrollRow = Mth.clamp(
                    this.scrollRow - (int) Math.signum(vertical), 0, this.maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int index = this.entryUnder(click.x(), click.y());
        if (index >= 0) {
            if (index != this.menu.getSelectedRecipeIndex()) {
                this.sendSelect(index);
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        this.buildButton.active = this.menu.canBuild();
        // The label belongs to the selected recipe, not to the window: the ramp
        // assembles cars and rebuilds parts, and those are different jobs.
        this.buildButton.setMessage(Component.translatable(this.menu.getActionKey()));
        // A body shell is a body shell: the grades only mean something for the
        // things that wear out.
        boolean grading = this.menu.choosesQuality();
        for (Button button : this.qualityButtons) {
            button.visible = grading;
        }
        super.render(context, mouseX, mouseY, deltaTicks);
        if (grading) {
            this.outlineChosenQuality(context);
            this.drawTierIcons(context);
        }
        this.drawCustomTooltips(context, mouseX, mouseY);
        this.renderTooltip(context, mouseX, mouseY);
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

        // Every slot the handler declared, drawn one pixel out from the 16x16
        // item area so the recess frames it the way vanilla's does.
        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            drawSlot(context, left + slot.x - 1, top + slot.y - 1);
        }

        this.drawRecipeList(context);
        this.drawMaterials(context);
    }

    private static void drawSlot(GuiGraphics context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, SLOT_BODY);
        context.fill(x, y, x + 18, y + 1, SLOT_DARK);
        context.fill(x, y, x + 1, y + 18, SLOT_DARK);
        context.fill(x + 17, y + 1, x + 18, y + 18, PANEL_LIGHT);
        context.fill(x + 1, y + 17, x + 18, y + 18, PANEL_LIGHT);
    }

    private void drawRecipeList(GuiGraphics context) {
        // Keep the selection in view even when the server picks it for us.
        this.scrollRow = Mth.clamp(this.scrollRow, 0, this.maxScroll());
        int selected = this.menu.getSelectedRecipeIndex();
        for (int index = 0; index < this.menu.getRecipes().size(); index++) {
            int[] at = this.entryAt(index);
            if (at == null) {
                continue;
            }
            int entryX = at[0];
            int entryY = at[1];

            drawSlot(context, entryX, entryY);
            if (index == selected) {
                // A ring rather than a fill, so the item underneath stays readable.
                context.fill(entryX, entryY, entryX + 18, entryY + 2, SELECTED);
                context.fill(entryX, entryY + 16, entryX + 18, entryY + 18, SELECTED);
                context.fill(entryX, entryY, entryX + 2, entryY + 18, SELECTED);
                context.fill(entryX + 16, entryY, entryX + 18, entryY + 18, SELECTED);
            }
            context.renderItem(this.menu.getRecipes().get(index).createResult(), entryX + 1, entryY + 1);
        }
        this.drawScrollbar(context);
    }

    /** Only drawn when there is something to scroll. */
    private void drawScrollbar(GuiGraphics context) {
        int max = this.maxScroll();
        if (max == 0) {
            return;
        }
        int trackX = this.leftPos + SCROLLBAR_X;
        int trackY = this.topPos + RECIPE_LIST_Y;
        int trackHeight = RECIPE_ROWS * RECIPE_ENTRY - 1;
        context.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, SLOT_DARK);

        int knobHeight = Math.max(8, trackHeight * RECIPE_ROWS / this.totalRows());
        int knobY = trackY + (trackHeight - knobHeight) * this.scrollRow / max;
        context.fill(trackX + 1, knobY, trackX + SCROLLBAR_WIDTH, knobY + knobHeight, SLOT_BODY);
        context.fill(trackX + 1, knobY, trackX + SCROLLBAR_WIDTH - 1, knobY + 1, PANEL_LIGHT);
    }

    /**
     * Which grade is chosen, drawn on top of the buttons. A ring rather than a
     * greyed-out button: a button you cannot press reads as "not available"
     * rather than "this is the one".
     */
    private void outlineChosenQuality(GuiGraphics context) {
        Button chosen = this.qualityButtons.get(this.menu.getQuality().ordinal());
        int left = chosen.getX() - 1;
        int top = chosen.getY() - 1;
        int right = left + chosen.getWidth() + 2;
        int bottom = top + chosen.getHeight() + 2;
        context.fill(left, top, right, top + 1, SELECTED);
        context.fill(left, bottom - 1, right, bottom, SELECTED);
        context.fill(left, top, left + 1, bottom, SELECTED);
        context.fill(right - 1, top, right, bottom, SELECTED);
    }

    /** Each tier's button shows what it is made of, small enough to sit inside it. */
    private void drawTierIcons(GuiGraphics context) {
        for (CarPart.Quality quality : CarPart.Quality.values()) {
            Button button = this.qualityButtons.get(quality.ordinal());
            context.pose().pushMatrix();
            context.pose().translate(button.getX() + 1.5F, button.getY() + 1.0F);
            context.pose().scale(0.75F, 0.75F);
            context.renderItem(new ItemStack(quality.material()), 0, 0);
            context.pose().popMatrix();
        }
    }

    private void drawMaterials(GuiGraphics context) {
        // Right-aligned: the window title sits on this same line and in some
        // languages it runs most of the way across.
        Component heading = Component.translatable("gui.car.materials");
        context.drawString(this.font, heading,
                this.leftPos + this.imageWidth - 8 - this.font.width(heading),
                this.topPos + 8, TEXT, false);

        java.util.List<VehicleRecipe.Material> materials = this.menu.getMaterials();
        for (int index = 0; index < materials.size(); index++) {
            VehicleRecipe.Material material = materials.get(index);
            int slotX = this.leftPos + MATERIALS_X + (index % MATERIALS_COLUMNS) * 18;
            int slotY = this.topPos + MATERIALS_Y + (index / MATERIALS_COLUMNS) * 18;

            int cost = material.count();
            context.renderItem(new ItemStack(material.item(), cost), slotX, slotY);

            boolean enough = this.menu.countInput(material.item()) >= cost;
            String count = String.valueOf(cost);
            context.drawString(this.font, count,
                    slotX + 17 - this.font.width(count), slotY + 9,
                    enough ? COUNT_OK : COUNT_MISSING);
        }
    }

    private void drawCustomTooltips(GuiGraphics context, int mouseX, int mouseY) {
        java.util.List<VehicleRecipe.Material> materials = this.menu.getMaterials();
        for (int index = 0; index < materials.size(); index++) {
            int slotX = this.leftPos + MATERIALS_X + (index % MATERIALS_COLUMNS) * 18;
            int slotY = this.topPos + MATERIALS_Y + (index / MATERIALS_COLUMNS) * 18;
            if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                VehicleRecipe.Material material = materials.get(index);
                context.setTooltipForNextFrame(this.font,
                        new ItemStack(material.item(), material.count()),
                        mouseX, mouseY);
                return;
            }
        }
        int hovered = this.entryUnder(mouseX, mouseY);
        if (hovered >= 0) {
            context.setTooltipForNextFrame(this.font,
                    this.menu.getRecipes().get(hovered).createResult(), mouseX, mouseY);
        }
    }
}
