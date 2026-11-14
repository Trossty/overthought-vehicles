package me.trossty.car.block.entity;

import me.trossty.car.block.FuelValveBlock;
import me.trossty.car.fluid.Liquids;
import me.trossty.car.fluid.UnitTank;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The one-way valve: how you tell a line of pipe where to go first.
 *
 * <p>Pipe on its own has no destinations. It evens itself out, so liquid drifts
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
 *       That is twice what levelling moves across a join, so at a junction the
 *       branch with a valve on it is served first.
 *   <li><b>It only goes one way.</b> Liquid past a valve cannot drift back.
 * </ul>
 *
 * <p>It works on anything with a liquid door — pipe, tank, pump, machine,
 * another mod's block — and any liquid, one at a time. Closing the handle or
 * powering it with redstone stops it; with a comparator on a tank that is a
 * routing rule built out of blocks rather than a settings screen.
 */
public class FuelValveBlockEntity extends BlockEntity {

    /** Just enough to carry a tick's worth; a valve is not storage. */
    public static final int CAPACITY = 200;

    /** Twice the pipe's levelling rate. That ratio is the whole feature. */
    public static final int PUMP_RATE = 100;

    private final UnitTank tank = new UnitTank(CAPACITY, fluid -> true, true, true, this::setChanged);

    public FuelValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_VALVE, pos, state);
    }

    public IFluidHandler getFluidStorage() {
        return this.tank;
    }

    public UnitTank getTank() {
        return this.tank;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, FuelValveBlockEntity valve) {
        boolean shut = world.hasNeighborSignal(pos);
        if (state.getValue(FuelValveBlock.POWERED) != shut) {
            world.setBlock(pos, state.setValue(FuelValveBlock.POWERED, shut), Block.UPDATE_ALL);
        }
        if (shut || !state.getValue(FuelValveBlock.OPEN)) {
            return;
        }

        Direction facing = state.getValue(FuelValveBlock.FACING);
        UnitTank tank = valve.tank;
        // Out before in, so a tick's worth passes straight through rather than
        // sitting in the valve until the next one.
        IFluidHandler ahead = world.getCapability(Capabilities.FluidHandler.BLOCK,
                pos.relative(facing), facing.getOpposite());
        if (ahead != null && !tank.isEmpty()) {
            Liquids.move(tank, ahead, tank.getFluid(), PUMP_RATE);
        }
        IFluidHandler behind = world.getCapability(Capabilities.FluidHandler.BLOCK,
                pos.relative(facing.getOpposite()), facing);
        if (behind != null && tank.room() > 0) {
            Liquids.move(behind, tank, tank.isEmpty() ? null : tank.getFluid(), Math.min(PUMP_RATE, tank.room()));
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        if (tag.contains("Petrol")) {
            this.tank.set(me.trossty.car.registry.ModFluids.PETROL, view.getIntOr("Petrol", 0));
        } else {
            this.tank.read(view, "Tank");
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        this.tank.write(view, "Tank");
    }
}
