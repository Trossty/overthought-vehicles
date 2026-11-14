package me.trossty.car.block.entity;

import me.trossty.car.fluid.FluidSource;
import me.trossty.car.fluid.Liquids;
import me.trossty.car.fluid.UnitTank;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A length of pipe. It carries any liquid — crude, raw fuel, petrol, diesel,
 * water, another mod's — one at a time, like a real line.
 *
 * <p>There is no network here, no graph to rebuild when somebody breaks a pipe
 * in the middle: every pipe is a small tank that, each tick, looks along each
 * of its connected arms and does one thing there.
 *
 * <ol>
 *   <li><b>Another pipe:</b> the two even out, if they hold the same liquid or
 *       one is empty. Liquid flows downhill towards whatever is drawing it off.
 *   <li><b>A valve:</b> nothing; the valve does its own pumping.
 *   <li><b>A machine that makes something</b> (pumpjack, refinery,
 *       hydrotreater): the pipe hands over what it carries if the machine
 *       takes it — crude into a refinery — and draws off what the machine
 *       makes. An empty pipe only starts drawing when no pipe beside it holds
 *       something else, so a refinery's product does not back up into the
 *       crude line feeding it.
 *   <li><b>Anything that only takes</b> (a fuel pump, a generator, an empty
 *       tank): the pipe empties into it.
 *   <li><b>Anything that holds</b> (a yard tank, a drum, another mod's tank):
 *       the pipe settles at {@link #SETTLE} against it, so the same line fills it from
 *       one end and empties it towards the other.
 * </ol>
 *
 * <p>Two liquids never mix: a pipe holding one will not take another, which is
 * how a crude line and a petrol line can run side by side as long as a wrench
 * has cut the arm between them.
 */
public class FuelPipeBlockEntity extends BlockEntity {

    /** A bucket a block: enough for a line to carry a pumpjack's flow without backing up. */
    public static final int CAPACITY = 1_000;

    /** Liquid moved across one join per tick. */
    public static final int TRANSFER_RATE = 50;

    /**
     * What a pipe keeps against a tank it touches: below this it drinks, above
     * it tips the surplus in. Low, so a tank on a line ends up holding the
     * liquid rather than the pipes.
     */
    public static final int SETTLE = 100;

    private final UnitTank tank = new UnitTank(CAPACITY, fluid -> true, true, true, this::setChanged);

    /** Arms a wrench has cut, one bit per {@link Direction#ordinal()}. */
    private int blocked;

    public FuelPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_PIPE, pos, state);
    }

    /** This pipe as other mods' pipes, valves and hoses see it. */
    public IFluidHandler getFluidStorage() {
        return this.tank;
    }

    public UnitTank getTank() {
        return this.tank;
    }

    public Fluid getFluid() {
        return this.tank.getFluid();
    }

    public int getAmount() {
        return this.tank.getUnits();
    }

    /** Petrol standing in the pipe; nought if it holds something else. */
    public int getPetrol() {
        return this.tank.getFluid() == ModFluids.PETROL ? this.tank.getUnits() : 0;
    }

    public int getCapacity() {
        return CAPACITY;
    }

    public void setPetrol(int petrol) {
        this.tank.set(ModFluids.PETROL, petrol);
        this.setChanged();
    }

    // ---- the wrench ----

    public boolean isBlocked(Direction side) {
        return (this.blocked & (1 << side.ordinal())) != 0;
    }

    public void setBlocked(Direction side, boolean cut) {
        int bit = 1 << side.ordinal();
        this.blocked = cut ? this.blocked | bit : this.blocked & ~bit;
        this.setChanged();
    }

    // ---- each tick ----

    public static void tick(Level world, BlockPos pos, BlockState state, FuelPipeBlockEntity pipe) {
        UnitTank tank = pipe.tank;
        for (Direction side : Direction.values()) {
            if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
                continue;
            }
            BlockPos next = pos.relative(side);
            BlockEntity there = world.getBlockEntity(next);

            if (there instanceof FuelPipeBlockEntity neighbour) {
                level(tank, neighbour.tank);
                continue;
            }
            if (there instanceof FuelValveBlockEntity) {
                continue;
            }
            IFluidHandler other = world.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite());
            if (other == null) {
                continue;
            }

            if (FluidSource.at(world, next)) {
                if (!tank.isEmpty()) {
                    Liquids.move(tank, other, tank.getFluid(), TRANSFER_RATE);
                }
                if (tank.room() > 0) {
                    draw(world, pos, state, tank, other);
                }
                continue;
            }

            boolean gives = !other.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty();
            if (!gives) {
                if (!tank.isEmpty()) {
                    Liquids.move(tank, other, tank.getFluid(), TRANSFER_RATE);
                }
                continue;
            }
            int room = SETTLE - tank.getUnits();
            if (room > 0) {
                Liquids.move(other, tank, tank.isEmpty() ? null : tank.getFluid(), Math.min(TRANSFER_RATE, room));
            } else if (room < 0) {
                Liquids.move(tank, other, tank.getFluid(), Math.min(TRANSFER_RATE, -room));
            }
        }
    }

    /**
     * Half the gap, so two pipes settle instead of swapping their contents back
     * and forth for ever. Different liquids stay where they are.
     */
    private static void level(UnitTank from, UnitTank to) {
        int gap = from.getUnits() - to.getUnits();
        if (gap > 1 && to.canTake(from.getFluid())) {
            Liquids.move(from, to, from.getFluid(), Math.min(TRANSFER_RATE, gap / 2));
        }
    }

    /** Draws off what a machine makes, without starting a second liquid in a line already carrying one. */
    private static void draw(Level world, BlockPos pos, BlockState state, UnitTank tank, IFluidHandler machine) {
        int want = Math.min(TRANSFER_RATE, tank.room());
        if (!tank.isEmpty()) {
            Liquids.move(machine, tank, tank.getFluid(), want);
            return;
        }
        FluidStack offered = machine.drain(want, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))
                    && world.getBlockEntity(pos.relative(side)) instanceof FuelPipeBlockEntity neighbour
                    && !neighbour.tank.isEmpty() && neighbour.tank.getFluid() != offered.getFluid()) {
                return;
            }
        }
        Liquids.move(machine, tank, offered.getFluid(), want);
    }

    // ---- saving and syncing ----

    @Override
    public net.minecraft.network.protocol.@org.jetbrains.annotations.Nullable Packet<
            net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        if (tag.contains("Petrol")) {
            // Saved before pipes carried anything but petrol.
            this.tank.set(ModFluids.PETROL, view.getIntOr("Petrol", 0));
        } else {
            this.tank.read(view, "Tank");
        }
        this.blocked = view.getIntOr("Blocked", 0);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        this.tank.write(view, "Tank");
        view.putInt("Blocked", this.blocked);
    }
}
