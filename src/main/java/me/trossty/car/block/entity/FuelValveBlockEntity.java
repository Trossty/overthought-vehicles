package me.trossty.car.block.entity;

import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.block.FuelValveBlock;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

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
            () -> this.petrol, value -> this.petrol = value, CAPACITY, true, true, this::markDirty);

    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> getFluidStorage() {
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
        this.markDirty();
    }

    public int fill(int amount) {
        int accepted = Math.min(amount, CAPACITY - this.petrol);
        if (accepted > 0) {
            this.petrol += accepted;
            this.markDirty();
        }
        return accepted;
    }

    public int drain(int amount) {
        int given = Math.min(amount, this.petrol);
        if (given > 0) {
            this.petrol -= given;
            this.markDirty();
        }
        return given;
    }

    public static void tick(World world, BlockPos pos, BlockState state, FuelValveBlockEntity valve) {
        boolean shut = world.isReceivingRedstonePower(pos);
        if (state.get(FuelValveBlock.POWERED) != shut) {
            world.setBlockState(pos, state.with(FuelValveBlock.POWERED, shut), Block.NOTIFY_ALL);
        }
        if (shut) {
            return;
        }

        Direction facing = state.get(FuelValveBlock.FACING);
        // Out before in, so a tick's worth passes straight through rather than
        // sitting in the valve until the next one.
        push(world, pos.offset(facing), valve);
        pull(world, pos.offset(facing.getOpposite()), valve);
    }

    /** Shoves what it is holding into whatever is in front of it. */
    private static void push(World world, BlockPos to, FuelValveBlockEntity valve) {
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
            var foreign = me.trossty.car.fluid.ForeignFluids.at(world, valve.getPos(),
                    Direction.fromVector(to.subtract(valve.getPos()), null));
            if (foreign != null) {
                me.trossty.car.fluid.ForeignFluids.move(valve.fluidStorage, foreign, offer);
            }
        }
    }

    /** Drags fuel out of whatever is behind it, whatever its level. */
    private static void pull(World world, BlockPos from, FuelValveBlockEntity valve) {
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
        var foreign = me.trossty.car.fluid.ForeignFluids.at(world, valve.getPos(),
                Direction.fromVector(from.subtract(valve.getPos()), null));
        if (foreign != null) {
            me.trossty.car.fluid.ForeignFluids.move(foreign, valve.fluidStorage, room);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.petrol = view.getInt("Petrol", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("Petrol", this.petrol);
    }
}
