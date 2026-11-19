package me.trossty.car.block.entity;

import me.trossty.car.registry.ModScreenHandlers;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The toolbox opened: a chest's worth of room, laid out as the cabinet's three
 * drawers of nine, each row sitting under its drawer's pull with a gap to the
 * next, and the player's pockets below.
 */
public class ToolboxMenu extends AbstractContainerMenu {

    public static final int DRAWERS = 3;
    public static final int PER_DRAWER = 9;
    public static final int SIZE = DRAWERS * PER_DRAWER;

    /** Where the first drawer's face starts; each face is one pitch tall, seam included. */
    public static final int DRAWER_TOP = 17;
    public static final int DRAWER_PITCH = 24;
    /** How far below the top of its face a drawer's row of slots sits, under the pull. */
    public static final int SLOT_DROP = 5;
    public static final int PLAYER_INVENTORY_TOP = 107;
    public static final int HOTBAR_TOP = 165;
    private static final int LEFT = 8;

    private final Container drawers;

    /** The client's side, before the server's copy of the contents arrives. */
    public ToolboxMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SIZE));
    }

    public ToolboxMenu(int id, Inventory inventory, Container drawers) {
        super(ModScreenHandlers.TOOLBOX, id);
        checkContainerSize(drawers, SIZE);
        this.drawers = drawers;
        drawers.startOpen(inventory.player);
        for (int drawer = 0; drawer < DRAWERS; drawer++) {
            for (int column = 0; column < PER_DRAWER; column++) {
                this.addSlot(new Slot(drawers, drawer * PER_DRAWER + column,
                        LEFT + column * 18, drawerTop(drawer) + SLOT_DROP));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, 9 + row * 9 + column,
                        LEFT + column * 18, PLAYER_INVENTORY_TOP + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, LEFT + column * 18, HOTBAR_TOP));
        }
    }

    /** The top of a drawer's face, inside the window. */
    public static int drawerTop(int drawer) {
        return DRAWER_TOP + drawer * DRAWER_PITCH;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SIZE) {
            // Out of the drawers, into the pockets, hotbar first.
            if (!this.moveItemStackTo(stack, SIZE, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.drawers.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.drawers.stopOpen(player);
    }
}
