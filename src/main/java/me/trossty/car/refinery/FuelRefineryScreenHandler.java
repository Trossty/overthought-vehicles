package me.trossty.car.refinery;

import me.trossty.car.block.entity.FuelRefineryBlockEntity;
import me.trossty.car.registry.ModScreenHandlers;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The refinery's window. Four slots, two tanks and two timers; the tanks and
 * timers come across as properties, because they are the only part of this
 * machine the client has to draw and none of them lives in an item stack.
 */
public class FuelRefineryScreenHandler extends AbstractContainerMenu {

    public static final int ORGANIC_X = 18;
    public static final int ORGANIC_Y = 17;
    public static final int HEAT_X = 18;
    public static final int HEAT_Y = 52;
    public static final int CATALYST_X = 70;
    public static final int CATALYST_Y = 17;
    /**
     * Everything sits a few pixels higher than feels natural: the burner slot
     * reaches y 70 as it is, and the "Inventory" label starts at 72.
     */
    public static final int CAN_X = 134;
    public static final int CAN_Y = 35;
    /** Tar comes out under the can. */
    public static final int TAR_X = 134;
    public static final int TAR_Y = 56;

    private static final int PLAYER_INVENTORY_TOP = 84;
    private static final int HOTBAR_TOP = 142;

    private final Container inventory;
    private final ContainerData properties;
    private final Level world;

    /** Client-side constructor: scratch inventory, zeroed properties, filled by sync. */
    public FuelRefineryScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory,
                new SimpleContainer(FuelRefineryBlockEntity.SIZE),
                new SimpleContainerData(FuelRefineryBlockEntity.PROPERTY_COUNT));
    }

    public FuelRefineryScreenHandler(int syncId, Inventory playerInventory,
                                     Container inventory, ContainerData properties) {
        super(ModScreenHandlers.FUEL_REFINERY, syncId);
        checkContainerSize(inventory, FuelRefineryBlockEntity.SIZE);
        checkContainerDataCount(properties, FuelRefineryBlockEntity.PROPERTY_COUNT);
        this.inventory = inventory;
        this.properties = properties;
        this.world = playerInventory.player.level();
        inventory.startOpen(playerInventory.player);

        this.addSlot(refinerySlot(FuelRefineryBlockEntity.ORGANIC_SLOT, ORGANIC_X, ORGANIC_Y));
        this.addSlot(refinerySlot(FuelRefineryBlockEntity.OIL_SLOT, HEAT_X, HEAT_Y));
        this.addSlot(refinerySlot(FuelRefineryBlockEntity.CATALYST_SLOT, CATALYST_X, CATALYST_Y));
        this.addSlot(refinerySlot(FuelRefineryBlockEntity.CAN_SLOT, CAN_X, CAN_Y));
        this.addSlot(refinerySlot(FuelRefineryBlockEntity.TAR_SLOT, TAR_X, TAR_Y));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, PLAYER_INVENTORY_TOP + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_TOP));
        }

        this.addDataSlots(properties);
    }

    /**
     * The world is captured because deciding what burns needs the fuel registry,
     * and the client has one too — it just has to be reached through the player
     * rather than through the block entity, which only the server holds.
     */
    private Slot refinerySlot(int index, int x, int y) {
        return new Slot(this.inventory, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FuelRefineryBlockEntity.isValidFor(index, stack,
                        FuelRefineryScreenHandler.this.world);
            }
        };
    }

    public int getBiomass() {
        return this.properties.get(FuelRefineryBlockEntity.BIOMASS_INDEX);
    }

    /** What the second tank holds: one of the refinery's {@code PRODUCT_} numbers. */
    public int getOutput() {
        return this.properties.get(FuelRefineryBlockEntity.OUTPUT_INDEX);
    }

    /** What the feed tank holds: biomass or crude. */
    public int getFeed() {
        return this.properties.get(FuelRefineryBlockEntity.FEED_INDEX);
    }

    public int getPetrol() {
        return this.properties.get(FuelRefineryBlockEntity.PETROL_INDEX);
    }

    public int getEnergy() {
        return this.properties.get(FuelRefineryBlockEntity.ENERGY_INDEX);
    }

    public int getTemperature() {
        return this.properties.get(FuelRefineryBlockEntity.TEMPERATURE_INDEX);
    }

    public float getDistillFraction() {
        return (float) this.properties.get(FuelRefineryBlockEntity.DISTILL_INDEX)
                / FuelRefineryBlockEntity.DISTILL_TIME;
    }

    public float getRefineFraction() {
        return (float) this.properties.get(FuelRefineryBlockEntity.REFINE_INDEX)
                / FuelRefineryBlockEntity.REFINE_TIME;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineEnd = FuelRefineryBlockEntity.SIZE;

        if (slotIndex < machineEnd) {
            if (!this.moveItemStackTo(stack, machineEnd, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.insertIntoMachine(stack)) {
            int inventoryEnd = machineEnd + 27;
            if (slotIndex < inventoryEnd) {
                if (!this.moveItemStackTo(stack, inventoryEnd, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, machineEnd, inventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** Shift-click puts a stack wherever it actually belongs, one slot at a time. */
    private boolean insertIntoMachine(ItemStack stack) {
        for (int index = 0; index < FuelRefineryBlockEntity.SIZE; index++) {
            if (this.slots.get(index).mayPlace(stack)
                    && this.moveItemStackTo(stack, index, index + 1, false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }
}
