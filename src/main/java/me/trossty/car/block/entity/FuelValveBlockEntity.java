package me.trossty.car.block.entity;

import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.block.FuelValveBlock;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;

/**
 * The one-way valve: how you tell a line of pipe where to go first.
 *
 * <p>Pipe on its own has no destinations. It evens itself out, so fuel drifts
 * towards whatever is being emptied and a junction feeding two pumps feeds them
 * about equally. That is the right default and the wrong answer when one of the
 * two matters more.
 *
 * <p>A valve answers it without adding a network. It does two things pipe
 * cannot:
 *
 * <ul>
 *   <li><b>It pumps.</b> It drags {@link #PUMP_RATE} out of whatever is behind
 *       it and shoves it into whatever is in front, with no regard for levels.
 *       That is four times what levelling moves across a join, so at a junction
 *       the branch with a valve on it is served first and the other branch gets
 *       what is left.
 *   <li><b>It only goes one way.</b> Fuel past a valve cannot drift back, so a
 *       pump that has been filled stays filled even when the rest of the yard
 *       runs dry.
 * </ul>
 *
 * <p>Redstone shuts it: powered is closed. With a comparator on a tank that is
 * a full routing rule — fill this one until it reads nine, then send the rest
 * somewhere else — built out of blocks rather than a settings screen.
 */
public class FuelValveBlockEntity extends BlockEntity {

    /** Just enough to carry a tick's worth; a valve is not storage. */
    public static final int CAPACITY = 100;

    /** Four times the pipe's levelling rate. That ratio is the whole feature. */
    public static final int PUMP_RATE = 32;

    private int petrol;

    /** This holder as other mods' pipes see it. */
    private final me.trossty.car.fluid.PetrolSlot fluidStorage = new me.trossty.car.fluid.PetrolSlot(
            () -> this.petrol, value -> this.petrol = value, CAPACITY, true, true, this::setChanged);

    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidStorage() {
        return this.fluidStorage;
    }

    public FuelValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_VALVE, pos, state);
    }

    public int getPetrol() {
        return this.petrol;
    }

    public int getCapacity() {
        return CAPACITY;
    }

    public void setPetrol(int petrol) {
        this.petrol = Math.max(0, Math.min(CAPACITY, petrol));
        this.setChanged();
    }

    public int fill(int amount) {
        int accepted = Math.min(amount, CAPACITY - this.petrol);
        if (accepted > 0) {
            this.petrol += accepted;
            this.setChanged();
        }
        return accepted;
    }

    public int drain(int amount) {
        int given = Math.min(amount, this.petrol);
        if (given > 0) {
            this.petrol -= given;
            this.setChanged();
        }
        return given;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, FuelValveBlockEntity valve) {
        boolean shut = world.hasNeighborSignal(pos);
        if (state.getValue(FuelValveBlock.POWERED) != shut) {
            world.setBlock(pos, state.setValue(FuelValveBlock.POWERED, shut), Block.UPDATE_ALL);
        }
        if (shut) {
            return;
        }

        Direction facing = state.getValue(FuelValveBlock.FACING);
        // Out before in, so a tick's worth passes straight through rather than
        // sitting in the valve until the next one.
        push(world, pos.relative(facing), valve);
        pull(world, pos.relative(facing.getOpposite()), valve);
    }

    /** Shoves what it is holding into whatever is in front of it. */
    private static void push(Level world, BlockPos to, FuelValveBlockEntity valve) {
        if (valve.petrol <= 0) {
            return;
        }
        int offer = Math.min(PUMP_RATE, valve.petrol);

        FuelPumpBlockEntity pump = FuelPumpBlock.tankAt(world, to);
        if (pump != null) {
            valve.drain(pump.fill(offer));
            return;
        }
        FuelTankBlockEntity tank = FuelTankBlock.tankAt(world, to);
        if (tank != null) {
            valve.drain(tank.fill(offer));
            return;
        }
        BlockEntity ahead = world.getBlockEntity(to);
        if (ahead instanceof FuelPipeBlockEntity pipe) {
            valve.drain(pipe.fill(offer));
        } else if (ahead instanceof FuelValveBlockEntity next) {
            valve.drain(next.fill(offer));
        } else {
            var foreign = me.trossty.car.fluid.ForeignFluids.at(world, valve.getBlockPos(),
                    net.minecraft.core.Direction.getNearest(to.subtract(valve.getBlockPos()).getX(), to.subtract(valve.getBlockPos()).getY(), to.subtract(valve.getBlockPos()).getZ()));
            if (foreign != null) {
                me.trossty.car.fluid.ForeignFluids.move(valve.fluidStorage, foreign, offer);
            }
        }
    }

    /** Drags fuel out of whatever is behind it, whatever its level. */
    private static void pull(Level world, BlockPos from, FuelValveBlockEntity valve) {
        int room = Math.min(PUMP_RATE, CAPACITY - valve.petrol);
        if (room <= 0) {
            return;
        }

        BlockEntity behind = me.trossty.car.block.MultiblockBlock.controllerEntity(world, from);
        if (behind instanceof FuelPipeBlockEntity pipe) {
            valve.fill(pipe.drain(room));
            return;
        }
        if (behind instanceof me.trossty.car.fluid.PetrolSource refinery) {
            valve.fill(refinery.drainPetrol(room));
            return;
        }
        FuelTankBlockEntity tank = FuelTankBlock.tankAt(world, from);
        if (tank != null) {
            valve.fill(tank.drain(room));
            return;
        }
        FuelPumpBlockEntity pump = FuelPumpBlock.tankAt(world, from);
        if (pump != null) {
            valve.fill(pump.drain(room));
            return;
        }
        var foreign = me.trossty.car.fluid.ForeignFluids.at(world, valve.getBlockPos(),
                net.minecraft.core.Direction.getNearest(from.subtract(valve.getBlockPos()).getX(), from.subtract(valve.getBlockPos()).getY(), from.subtract(valve.getBlockPos()).getZ()));
        if (foreign != null) {
            me.trossty.car.fluid.ForeignFluids.move(foreign, valve.fluidStorage, room);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.petrol = view.getIntOr("Petrol", 0);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        view.putInt("Petrol", this.petrol);
    }
}
