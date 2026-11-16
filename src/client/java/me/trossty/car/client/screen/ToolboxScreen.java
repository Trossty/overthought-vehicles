package me.trossty.car.client.screen;

import me.trossty.car.block.entity.ToolboxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The toolbox, drawn as the front of the roller cabinet it is: red steel, three
 * drawers with a full-width aluminium pull along the top of each, the row of
 * slots under it, a dark seam between drawers and a kick plate at the foot.
 */
public class ToolboxScreen extends AbstractContainerScreen<ToolboxMenu> {

    /** The cabinet's red, taken from the block's own paint, with its light and shade. */
    private static final int RED = 0xFFAB251D;
    private static final int RED_LIGHT = 0xFFCB3C2F;
    private static final int RED_DARK = 0xFF7A1812;
    private static final int SEAM = 0xFF2A0907;
    private static final int RUBBER = 0xFF1C1D1F;
    private static final int PULL_TOP = 0xFFEEF1F4;
    private static final int PULL = 0xFFB7BEC6;
    private static final int PULL_UNDER = 0xFF7B838C;
    /** The drawers' grey foam liner: the pocket floor, its shaded walls and its cut edge. */
    private static final int FOAM = 0xFF3B3F45;
    private static final int FOAM_DEEP = 0xFF1A1C1F;
    private static final int FOAM_EDGE = 0xFF62676E;

    /** The cabinet's sides, inside the window: three pixels of red either side of the slots. */
    private static final int CABINET_LEFT = 4;
    private static final int CABINET_RIGHT = 172;

    public ToolboxScreen(ToolboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = ToolboxMenu.HOTBAR_TOP + 24;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ToolboxMenu.PLAYER_INVENTORY_TOP - 12;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        this.renderTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics context, float deltaTicks, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        GuiStyle.frame(context, left, top, this.imageWidth, this.imageHeight, RED);
        this.drawCabinet(context, left, top);
        for (int index = 0; index < this.menu.slots.size(); index++) {
            Slot slot = this.menu.slots.get(index);
            if (index < ToolboxMenu.SIZE) {
                this.pocket(context, left + slot.x - 1, top + slot.y - 1);
            } else {
                GuiStyle.slot(context, left + slot.x - 1, top + slot.y - 1);
            }
        }
    }

    /** A pocket cut in the drawer's dark foam liner, the way tools are kept in a real one. */
    private void pocket(GuiGraphics context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, FOAM);
        context.fill(x, y, x + 18, y + 1, FOAM_DEEP);
        context.fill(x, y, x + 1, y + 18, FOAM_DEEP);
        context.fill(x + 1, y + 1, x + 17, y + 2, 0x30000000);
        context.fill(x + 17, y + 1, x + 18, y + 18, FOAM_EDGE);
        context.fill(x + 1, y + 17, x + 18, y + 18, FOAM_EDGE);
    }

    private void drawCabinet(GuiGraphics context, int left, int top) {
        int x0 = left + CABINET_LEFT;
        int x1 = left + CABINET_RIGHT;
        int y0 = top + ToolboxMenu.DRAWER_TOP;
        int y1 = top + ToolboxMenu.drawerTop(ToolboxMenu.DRAWERS);
        // The steel shell, and the rubber strip the top chest sits on.
        context.fill(x0 - 1, y0 - 2, x1 + 1, y1 + 3, GuiStyle.OUTLINE);
        context.fill(x0, y0 - 1, x1, y0, RUBBER);
        for (int drawer = 0; drawer < ToolboxMenu.DRAWERS; drawer++) {
            this.drawDrawer(context, x0, top + ToolboxMenu.drawerTop(drawer), x1 - x0);
        }
        // The kick plate under the bottom drawer.
        context.fill(x0, y1, x1, y1 + 2, 0xFF2E3238);
        context.fill(x0, y1, x1, y1 + 1, 0xFF4A5058);
    }

    /** One drawer's face: the pull along its top, the red face round its slots, the seam under it. */
    private void drawDrawer(GuiGraphics context, int x, int y, int width) {
        int bottom = y + ToolboxMenu.DRAWER_PITCH - 1;
        context.fill(x, y, x + width, bottom, RED);
        context.fillGradient(x, y + 3, x + width, bottom, 0x1CFFFFFF, 0x00FFFFFF);
        context.fill(x, y + 3, x + 1, bottom, RED_LIGHT);
        context.fill(x + width - 1, y + 3, x + width, bottom, RED_DARK);
        context.fill(x, bottom - 1, x + width, bottom, RED_DARK);
        context.fill(x, bottom, x + width, bottom + 1, SEAM);

        // The extruded aluminium pull, lit on top, with a black end cap each side.
        context.fill(x + 2, y, x + width - 2, y + 1, PULL_TOP);
        context.fill(x + 2, y + 1, x + width - 2, y + 2, PULL);
        context.fill(x + 2, y + 2, x + width - 2, y + 3, PULL_UNDER);
        context.fill(x + 2, y + 3, x + width - 2, y + 4, 0x50000000);
        context.fill(x, y, x + 2, y + 3, RUBBER);
        context.fill(x + width - 2, y, x + width, y + 3, RUBBER);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        GuiStyle.title(context, this.font, this.title, this.imageWidth - GuiStyle.TITLE_X - 10);
        context.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, GuiStyle.TEXT, false);
    }
}
