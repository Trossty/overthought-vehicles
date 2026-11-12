package me.trossty.car.block.entity;

import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A length of fuel line.
 *
 * <p>There is no network here, no graph to rebuild when somebody breaks a pipe
 * in the middle: every pipe is a tiny tank that does three things each tick and
 * knows nothing but its six neighbours.
 *
 * <ol>
 *   <li>drinks from any refinery it touches,
 *   <li>empties into any pump it touches,
 *   <li>settles half full against any storage tank it touches,
 *   <li>evens itself out with the pipes next door.
 * </ol>
 *
 * <p>That third rule is what makes fuel travel. A pump that is being emptied
 * pulls the pipe beside it down, that pipe pulls its own neighbour down, and the
 * dip runs back along the line to the refinery — the fuel flows downhill towards
 * whatever is using it. Nothing has to be told where the ends of the line are,
 * loops and branches cost nothing, and a pipe pulled out of the middle just
 * stops the slope there.
 *
 * <p>The cost of the simplicity is that a line takes a moment to prime, since
 * each pipe only moves {@link #TRANSFER_RATE} per tick and only half of whatever
 * gap it sees. In practice a station is a handful of blocks long and the
 * refinery is far slower than the line, so you never see it.
 *
 * <p>It also means every pipe keeps a last drop: half of a gap of one is
 * nothing, and moving the whole drop would only send it sloshing back on the
 * next tick. One petrol is a tenth of one unit of a car's tank, so a line holds
 * a film of fuel rather than running truly dry — which is what a real one does.
 */
public class FuelPipeBlockEntity extends BlockEntity {

    /** Deliberately small: a pipe carries fuel, it does not store it. */
    public static final int CAPACITY = 200;

    /** Petrol moved across one join per tick — twice what a pump siphons alone. */
    public static final int TRANSFER_RATE = 8;

    private int petrol;

    /** This holder as other mods' pipes see it. */
    private final me.trossty.car.fluid.PetrolSlot fluidStorage = new me.trossty.car.fluid.PetrolSlot(
            () -> this.petrol, value -> this.petrol = value, CAPACITY, true, true, this::setChanged);

    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> getFluidStorage() {
        return this.fluidStorage;
    }

    public FuelPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_PIPE, pos, state);
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

    /** Takes what it can, up to {@code amount}, and reports how much went in. */
    public int fill(int amount) {
        int accepted = Math.min(amount, CAPACITY - this.petrol);
        if (accepted > 0) {
            this.petrol += accepted;
            this.setChanged();
        }
        return accepted;
    }

    /** Gives out what it can, up to {@code amount}, and reports how much came out. */
    public int drain(int amount) {
        int given = Math.min(amount, this.petrol);
        if (given > 0) {
            this.petrol -= given;
            this.setChanged();
        }
        return given;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, FuelPipeBlockEntity pipe) {
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);

            // 1. the source
            if (pipe.petrol < CAPACITY
                    && me.trossty.car.block.MultiblockBlock.controllerEntity(world, next) instanceof me.trossty.car.fluid.PetrolSource refinery) {
                pipe.fill(refinery.drainPetrol(Math.min(TRANSFER_RATE, CAPACITY - pipe.petrol)));
                continue;
            }

            // 2. the drain
            FuelPumpBlockEntity pump = FuelPumpBlock.tankAt(world, next);
            if (pump != null) {
                if (pipe.petrol > 0) {
                    pipe.drain(pump.fill(Math.min(TRANSFER_RATE, pipe.petrol)));
                }
                continue;
            }

            // 2b. the yard tank, which is both at once. A pipe settles half
            //     full against one: below that it drinks, above it tips its
            //     surplus back. So the same line fills the tank from a refinery
            //     and empties it towards a pump without being told which job it
            //     is doing today.
            FuelTankBlockEntity tank = FuelTankBlock.tankAt(world, next);
            if (tank != null) {
                int room = CAPACITY / 2 - pipe.petrol;
                if (room > 0) {
                    pipe.fill(tank.drain(Math.min(TRANSFER_RATE, room)));
                } else if (room < 0) {
                    pipe.drain(tank.fill(Math.min(TRANSFER_RATE, -room)));
                }
                continue;
            }

            // 3. the slope. Half the gap, so two pipes settle instead of
            //    swapping their contents back and forth for ever.
            if (world.getBlockEntity(next) instanceof FuelPipeBlockEntity neighbour) {
                int gap = pipe.petrol - neighbour.petrol;
                if (gap > 1) {
                    int moved = neighbour.fill(Math.min(TRANSFER_RATE, gap / 2));
                    pipe.drain(moved);
                }
                continue;
            }

            // 4. anything another mod built: a tank, a machine, a pipe. Treated
            //    like the yard tank — the pipe settles half full against it —
            //    so the same join feeds a foreign line or drinks from one.
            var foreign = me.trossty.car.fluid.ForeignFluids.at(world, pos, side);
            if (foreign != null) {
                int room = CAPACITY / 2 - pipe.petrol;
                if (room > 0) {
                    me.trossty.car.fluid.ForeignFluids.move(foreign, pipe.fluidStorage, Math.min(TRANSFER_RATE, room));
                } else if (room < 0) {
                    me.trossty.car.fluid.ForeignFluids.move(pipe.fluidStorage, foreign, Math.min(TRANSFER_RATE, -room));
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.petrol = view.getIntOr("Petrol", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("Petrol", this.petrol);
    }
}
