package me.trossty.car.client.screen;

import java.util.ArrayList;
import java.util.List;

import me.trossty.car.entity.CarPart;
import me.trossty.car.workbench.VehicleRecipe;
import me.trossty.car.workbench.VehicleWorkbenchScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The workbench window, drawn like the rest of the mod's windows from plain
 * rectangles: the catalogue of what the bench can make sits in a dark steel
 * well on the left, the selected job's materials are pinned up beside it on a
 * blueprint, and under the blueprint are the grade keys and the one key the
 * bench is for. The tray the materials go in runs the width of the window
 * above the player's own inventory.
 */
public class VehicleWorkbenchScreen extends AbstractContainerScreen<VehicleWorkbenchScreenHandler> {

    static final int BUILD_FACE = 0xFFD8691A;

    /** The steel well the catalogue sits in, lined up with the inventory's left edge. */
    private static final int CATALOGUE_X = 8;
    private static final int CATALOGUE_Y = 18;
    private static final int CATALOGUE_WIDTH = 87;
    private static final int CATALOGUE_HEIGHT = 80;
    private static final int RECIPE_LIST_X = CATALOGUE_X + 1;
    private static final int RECIPE_LIST_Y = CATALOGUE_Y + 1;
    /** One slot and a two-pixel gap, room enough for the ring round the chosen one. */
    private static final int RECIPE_ENTRY = 20;
    /**
     * The catalogue outgrew a single column the moment the bench started making
     * service parts, so it is a grid that scrolls: four across and four down.
     */
    private static final int RECIPE_COLUMNS = 4;
    private static final int RECIPE_ROWS = 4;
    private static final int SCROLLBAR_X = RECIPE_LIST_X + RECIPE_COLUMNS * RECIPE_ENTRY;
    private static final int SCROLLBAR_Y = RECIPE_LIST_Y;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_HEIGHT = RECIPE_ROWS * RECIPE_ENTRY - 2;

    /** The blueprint, its right edge on the inventory's. */
    private static final int SHEET_X = 101;
    private static final int SHEET_Y = 18;
    private static final int SHEET_WIDTH = 67;
    private static final int SHEET_HEIGHT = 47;
    /** Three across, so even the six-part tanker fits on two lines. */
    private static final int MATERIALS_COLUMNS = 3;
    private static final int MATERIALS_X = SHEET_X + 6;
    private static final int MATERIALS_Y = SHEET_Y + 10;

    /** The four grade keys — iron, gold, diamond, netherite — each showing its material. */
    private static final int QUALITY_X = SHEET_X - 1;
    private static final int QUALITY_Y = 68;
    private static final int QUALITY_WIDTH = 15;
    private static final int QUALITY_HEIGHT = 14;
    private static final int QUALITY_GAP = 3;
    private static final int QUALITY_STRIP = 4 * QUALITY_WIDTH + 3 * QUALITY_GAP;

    private static final int BUILD_X = SHEET_X - 1;
    private static final int BUILD_Y = 84;
    private static final int BUILD_WIDTH = SHEET_WIDTH + 2;
    private static final int BUILD_HEIGHT = 15;

    private PanelButton buildButton;
    private final List<PanelButton> qualityButtons = new ArrayList<>();
    /** First visible row of the catalogue. */
    private int scrollRow;
    private boolean draggingKnob;

    public VehicleWorkbenchScreen(VehicleWorkbenchScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 215;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = VehicleWorkbenchScreenHandler.PLAYER_INVENTORY_TOP - 12;
        this.buildButton = PanelButton.action(this.leftPos + BUILD_X, this.topPos + BUILD_Y,
                BUILD_WIDTH, BUILD_HEIGHT, Component.translatable(this.menu.getActionKey()),
                button -> this.sendBuild(), BUILD_FACE);
        this.addRenderableWidget(this.buildButton);

        this.qualityButtons.clear();
        for (CarPart.Quality quality : CarPart.Quality.values()) {
            int index = quality.ordinal();
            PanelButton button = PanelButton.steel(
                    this.leftPos + QUALITY_X + index * (QUALITY_WIDTH + QUALITY_GAP), this.topPos + QUALITY_Y,
                    QUALITY_WIDTH, QUALITY_HEIGHT, Component.empty(),
                    whatever -> this.sendSelect(VehicleWorkbenchScreenHandler.QUALITY_BUTTON_ID + index));
            button.setTooltip(Tooltip.create(Component.translatable("quality.car." + quality.key())
                    .withStyle(quality.formatting())
                    .append(Component.literal("\n"))
                    .append(Component.translatable("gui.car.quality_tip." + quality.key())
                            .withStyle(ChatFormatting.GRAY))));
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

    /** Where entry {@code index}'s slot frame is drawn, or null when it is scrolled away. */
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

    /** Where material {@code index}'s cell on the blueprint starts, in the window's own coordinates. */
    private static int materialX(int index) {
        return MATERIALS_X + (index % MATERIALS_COLUMNS) * 18;
    }

    private static int materialY(int index) {
        return MATERIALS_Y + (index / MATERIALS_COLUMNS) * 18;
    }

    private int knobHeight() {
        return GuiStyle.knob(SCROLLBAR_HEIGHT, RECIPE_ROWS, this.totalRows());
    }

    private boolean overTrack(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos - SCROLLBAR_X;
        double y = mouseY - this.topPos - SCROLLBAR_Y;
        return x >= 0 && x < SCROLLBAR_WIDTH && y >= 0 && y < SCROLLBAR_HEIGHT;
    }

    /** Puts the knob's middle under the cursor, as far as it will go. */
    private void scrollTo(double mouseY) {
        this.scrollRow = GuiStyle.scrollFor(mouseY - this.topPos - SCROLLBAR_Y,
                SCROLLBAR_HEIGHT, this.knobHeight(), this.maxScroll());
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = this.entryUnder(mouseX, mouseY);
        if (index >= 0) {
            if (index != this.menu.getSelectedRecipeIndex()) {
                this.sendSelect(index);
            }
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

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        this.buildButton.active = this.menu.canBuild();
        // The label belongs to the selected recipe, not to the window: the ramp
        // assembles cars and rebuilds parts, and those are different jobs.
        this.buildButton.setMessage(Component.translatable(this.menu.getActionKey()));
        // A body shell is a body shell: the grades only mean something for the
        // things that wear out.
        boolean grading = this.menu.choosesQuality();
        int chosen = this.menu.getQuality().ordinal();
        for (int index = 0; index < this.qualityButtons.size(); index++) {
            PanelButton button = this.qualityButtons.get(index);
            button.visible = grading;
            button.setSelected(index == chosen);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
        if (grading) {
            this.drawTierIcons(context, chosen);
        }
        this.drawCustomTooltips(context, mouseX, mouseY);
        this.renderTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics context, float deltaTicks, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        GuiStyle.frame(context, left, top, this.imageWidth, this.imageHeight);

        // Every slot the handler declared, drawn one pixel out from the 16x16
        // item area so the recess frames it the way vanilla's does.
        for (Slot slot : this.menu.slots) {
            GuiStyle.slot(context, left + slot.x - 1, top + slot.y - 1);
        }

        this.drawCatalogue(context, mouseX, mouseY);
        GuiStyle.blueprint(context, left + SHEET_X, top + SHEET_Y, SHEET_WIDTH, SHEET_HEIGHT);
        this.drawMaterials(context);
        if (!this.menu.choosesQuality()) {
            this.drawEmptySockets(context);
        }
    }

    private void drawCatalogue(GuiGraphics context, int mouseX, int mouseY) {
        GuiStyle.well(context, this.leftPos + CATALOGUE_X, this.topPos + CATALOGUE_Y,
                CATALOGUE_WIDTH, CATALOGUE_HEIGHT);

        // Keep the selection in view even when the server picks it for us.
        this.scrollRow = Mth.clamp(this.scrollRow, 0, this.maxScroll());
        int selected = this.menu.getSelectedRecipeIndex();
        int hovered = this.entryUnder(mouseX, mouseY);
        for (int index = 0; index < this.menu.getRecipes().size(); index++) {
            int[] at = this.entryAt(index);
            if (at == null) {
                continue;
            }
            int x = at[0];
            int y = at[1];
            GuiStyle.slot(context, x, y);
            if (index == selected) {
                // A ring round the slot and a warm wash inside it, so the item
                // itself stays as readable as its neighbours.
                GuiStyle.chosen(context, x, y, 18, 18);
            }
            context.renderItem(this.menu.getRecipes().get(index).createResult(), x + 1, y + 1);
            if (index == hovered) {
                AbstractContainerScreen.renderSlotHighlight(context, x + 1, y + 1, 0);
            }
        }
        this.drawScrollbar(context);
    }

    private void drawScrollbar(GuiGraphics context) {
        GuiStyle.scrollbar(context, this.leftPos + SCROLLBAR_X, this.topPos + SCROLLBAR_Y,
                SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, this.knobHeight(), this.scrollRow, this.maxScroll());
    }

    /** Each material in its cell on the blueprint, red where the tray is short. */
    private void drawMaterials(GuiGraphics context) {
        List<VehicleRecipe.Material> materials = this.menu.getMaterials();
        for (int index = 0; index < materials.size(); index++) {
            VehicleRecipe.Material material = materials.get(index);
            boolean enough = this.menu.countInput(material.item()) >= material.count();
            GuiStyle.material(context, this.font, new ItemStack(material.item(), material.count()),
                    this.leftPos + materialX(index), this.topPos + materialY(index),
                    enough ? GuiStyle.COUNT_OK : GuiStyle.COUNT_MISSING, !enough);
        }
    }

    /**
     * With nothing to grade, the keys drop away and leave a dark strip that
     * says so, rather than four empty boxes that would read as slots.
     */
    private void drawEmptySockets(GuiGraphics context) {
        GuiStyle.well(context, this.leftPos + QUALITY_X + 1, this.topPos + QUALITY_Y + 1,
                QUALITY_STRIP - 2, QUALITY_HEIGHT - 2);
    }

    /** Each grade's key shows what it is made of, small enough to sit inside it. */
    private void drawTierIcons(GuiGraphics context, int chosen) {
        for (CarPart.Quality quality : CarPart.Quality.values()) {
            PanelButton button = this.qualityButtons.get(quality.ordinal());
            float sink = quality.ordinal() == chosen ? 1.0F : 0.0F;
            context.pose().pushPose();
            context.pose().translate(button.getX() + 1.5F, button.getY() + 1.0F + sink, 0.0F);
            context.pose().scale(0.75F, 0.75F, 1.0F);
            context.renderItem(new ItemStack(quality.material()), 0, 0);
            context.pose().popPose();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        GuiStyle.title(context, this.font, this.title, this.imageWidth - GuiStyle.TITLE_X - 10);
        GuiStyle.caption(context, this.font, Component.translatable("gui.car.materials"),
                SHEET_X + SHEET_WIDTH / 2, SHEET_Y + 2, SHEET_WIDTH - 10);
        if (!this.menu.choosesQuality()) {
            Component single = Component.translatable("gui.car.quality_single");
            FormattedCharSequence line = GuiStyle.fit(this.font, single, QUALITY_STRIP - 8);
            context.drawString(this.font, line, QUALITY_X + (QUALITY_STRIP - this.font.width(line)) / 2,
                    QUALITY_Y + 3, GuiStyle.TITLE_SOFT, false);
        }
        context.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, GuiStyle.TEXT, false);
    }

    private void drawCustomTooltips(GuiGraphics context, int mouseX, int mouseY) {
        List<VehicleRecipe.Material> materials = this.menu.getMaterials();
        for (int index = 0; index < materials.size(); index++) {
            int x = this.leftPos + materialX(index) + 1;
            int y = this.topPos + materialY(index) + 1;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                VehicleRecipe.Material material = materials.get(index);
                ItemStack stack = new ItemStack(material.item(), material.count());
                int have = this.menu.countInput(material.item());
                List<Component> lines = new ArrayList<>(getTooltipFromItem(this.minecraft, stack));
                lines.add(Component.translatable("gui.car.materials.have", have, material.count())
                        .withStyle(have >= material.count() ? ChatFormatting.GREEN : ChatFormatting.RED));
                context.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
        int hovered = this.entryUnder(mouseX, mouseY);
        if (hovered >= 0) {
            context.renderTooltip(this.font,
                    this.menu.getRecipes().get(hovered).createResult(), mouseX, mouseY);
        }
    }
}
