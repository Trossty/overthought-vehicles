package me.trossty.car.machine;

import java.util.List;
import java.util.function.Predicate;
import me.trossty.car.fluid.UnitTank;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModScreenHandlers;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * One window for every machine that is mostly gauges: the generators, the
 * hydrotreater, the pumpjack, the battery and the charging post.
 *
 * <p>Two screen types share this class: {@link ModScreenHandlers#MACHINE} has
 * no slots of the machine's own, {@link ModScreenHandlers#FUELLED_MACHINE}
 * has the one firebox slot the coal generator burns from. Everything else is
 * properties, and properties travel as shorts, so the big numbers (a
 * battery's charge, the crude left in the ground) are sent in two halves.
 */
public class MachineScreenHandler extends AbstractContainerMenu {

    public static final int KIND = 0;
    public static final int ENERGY_LO = 1;
    public static final int ENERGY_HI = 2;
    public static final int CAPACITY_LO = 3;
    public static final int CAPACITY_HI = 4;
    public static final int A_UNITS = 5;
    public static final int A_CAPACITY = 6;
    public static final int A_FLUID = 7;
    public static final int B_UNITS = 8;
    public static final int B_CAPACITY = 9;
    public static final int B_FLUID = 10;
    public static final int PROGRESS = 11;
    public static final int EXTRA_LO = 12;
    public static final int EXTRA_HI = 13;
    public static final int WORKING = 14;
    public static final int EXTRA2 = 15;
    public static final int COUNT = 16;

    /** The fluids a gauge can show, by the number sent for them; -1 is "no tank". */
    public static final List<Fluid> FLUIDS = List.of(Fluids.EMPTY, ModFluids.CRUDE_OIL, ModFluids.RAW_PETROL,
            ModFluids.RAW_DIESEL, ModFluids.PETROL, ModFluids.DIESEL, ModFluids.BIOFUEL);

    /** The firebox slot, under the fire window on the window's faceplate. */
    public static final int FUEL_SLOT_X = 22;
    public static final int FUEL_SLOT_Y = 47;

    private static final int PLAYER_INVENTORY_TOP = 103;
    private static final int HOTBAR_TOP = 161;
    private static final int HALF = 1 << 15;

    private final ContainerData properties;
    private final Predicate<Player> canUse;
    private final boolean fuelled;

    /** Client side, no firebox. */
    public static MachineScreenHandler plain(int syncId, Inventory playerInventory) {
        return new MachineScreenHandler(ModScreenHandlers.MACHINE, syncId, playerInventory, null,
                new SimpleContainerData(COUNT), player -> true);
    }

    /** Client side, with the firebox slot. */
    public static MachineScreenHandler fuelled(int syncId, Inventory playerInventory) {
        return new MachineScreenHandler(ModScreenHandlers.FUELLED_MACHINE, syncId, playerInventory,
                new SimpleContainer(1), new SimpleContainerData(COUNT), player -> true);
    }

    /** Server side: live gauges off the machine itself. */
    public static MachineScreenHandler of(int syncId, Inventory playerInventory, BlockEntity machine,
                                          @Nullable Container firebox) {
        GaugeSource source = (GaugeSource) machine;
        return new MachineScreenHandler(firebox == null ? ModScreenHandlers.MACHINE : ModScreenHandlers.FUELLED_MACHINE,
                syncId, playerInventory, firebox, delegate(source),
                player -> !machine.isRemoved() && player.distanceToSqr(
                        machine.getBlockPos().getCenter()) <= 64.0);
    }

    private MachineScreenHandler(MenuType<?> type, int syncId, Inventory playerInventory,
                                 @Nullable Container firebox, ContainerData properties,
                                 Predicate<Player> canUse) {
        super(type, syncId);
        checkContainerDataCount(properties, COUNT);
        this.properties = properties;
        this.canUse = canUse;
        this.fuelled = firebox != null;
        if (firebox != null) {
            this.addSlot(new Slot(firebox, 0, FUEL_SLOT_X, FUEL_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return me.trossty.car.block.entity.GeneratorBlockEntity.burnTicks(stack,
                            playerInventory.player.level()) > 0;
                }
            });
        }
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

    /** The properties a machine's gauges are read through, live, every sync. */
    private static ContainerData delegate(GaugeSource source) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case KIND -> source.gaugeKind();
                    case ENERGY_LO -> lo(source.gaugeEnergy());
                    case ENERGY_HI -> hi(source.gaugeEnergy());
                    case CAPACITY_LO -> lo(source.gaugeEnergyCapacity());
                    case CAPACITY_HI -> hi(source.gaugeEnergyCapacity());
                    case A_UNITS -> units(source.gaugeTankA());
                    case A_CAPACITY -> capacity(source.gaugeTankA());
                    case A_FLUID -> fluid(source.gaugeTankA());
                    case B_UNITS -> units(source.gaugeTankB());
                    case B_CAPACITY -> capacity(source.gaugeTankB());
                    case B_FLUID -> fluid(source.gaugeTankB());
                    case PROGRESS -> Math.max(0, Math.min(1000, source.gaugeProgress()));
                    case EXTRA_LO -> lo(source.gaugeExtra());
                    case EXTRA_HI -> hi(source.gaugeExtra());
                    case WORKING -> source.gaugeWorking() ? 1 : 0;
                    case EXTRA2 -> Math.max(0, Math.min(Short.MAX_VALUE, source.gaugeExtra2()));
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return COUNT;
            }
        };
    }

    private static int lo(long value) {
        return (int) (Math.max(0, value) % HALF);
    }

    private static int hi(long value) {
        return (int) Math.min(Short.MAX_VALUE, Math.max(0, value) / HALF);
    }

    private static int units(@Nullable UnitTank tank) {
        return tank == null ? 0 : tank.getUnits();
    }

    private static int capacity(@Nullable UnitTank tank) {
        return tank == null ? 0 : tank.getCapacityUnits();
    }

    private static int fluid(@Nullable UnitTank tank) {
        if (tank == null || tank.getCapacityUnits() == 0) {
            return -1;
        }
        int index = FLUIDS.indexOf(tank.isEmpty() ? Fluids.EMPTY : tank.getFluid());
        return Math.max(0, index);
    }

    // ---- what the client reads ----

    public int get(int index) {
        return this.properties.get(index);
    }

    private long joined(int lo, int hi) {
        return (long) (this.properties.get(hi) & 0x7FFF) * HALF + (this.properties.get(lo) & 0x7FFF);
    }

    public int kind() {
        return this.properties.get(KIND);
    }

    public long energy() {
        return this.joined(ENERGY_LO, ENERGY_HI);
    }

    public long energyCapacity() {
        return this.joined(CAPACITY_LO, CAPACITY_HI);
    }

    public long extra() {
        return this.joined(EXTRA_LO, EXTRA_HI);
    }

    public boolean working() {
        return this.properties.get(WORKING) != 0;
    }

    public float progress() {
        return this.properties.get(PROGRESS) / 1000.0F;
    }

    public boolean isFuelled() {
        return this.fuelled;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.canUse.test(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineEnd = this.fuelled ? 1 : 0;
        int inventoryEnd = machineEnd + 27;
        if (slotIndex < machineEnd) {
            if (!this.moveItemStackTo(stack, machineEnd, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.fuelled && this.slots.get(0).mayPlace(stack)) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < inventoryEnd) {
            if (!this.moveItemStackTo(stack, inventoryEnd, this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, machineEnd, inventoryEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
