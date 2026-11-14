package me.trossty.car.block.entity;

import me.trossty.car.network.FluidNetwork;
import me.trossty.car.network.Network;
import me.trossty.car.network.NetworkBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A length of pipe. It carries any liquid — crude, raw fuel, petrol, diesel,
 * water, another mod's — and holds none: the whole line it is part of works as
 * one {@link FluidNetwork}, which takes from what gives and hands it straight
 * to what takes.
 *
 * <p>The one thing a length keeps is which of its arms a wrench has cut, which
 * is how two lines running side by side are kept apart.
 */
public class FuelPipeBlockEntity extends NetworkBlockEntity<FluidNetwork> {

    /** Arms a wrench has cut, one bit per {@link Direction#ordinal()}. */
    private int blocked;

    private final IFluidHandler[] doors = new IFluidHandler[7];

    public FuelPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_PIPE, pos, state);
    }

    @Override
    public Network.Link link(Direction side) {
        if (!this.getBlockState().getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
            return Network.Link.NONE;
        }
        return this.level != null && this.level.getBlockEntity(this.worldPosition.relative(side)) instanceof FuelPipeBlockEntity
                ? Network.Link.MEMBER : Network.Link.PORT;
    }

    @Override
    protected Object linkIn(BlockState state, Direction side) {
        return state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
    }

    @Override
    protected FluidNetwork gather(ServerLevel world) {
        return Network.gather(world, this, FluidNetwork::new);
    }

    /** What the line is carrying, or nothing. */
    public Fluid getFluid() {
        FluidNetwork network = this.network();
        return network == null ? Fluids.EMPTY : network.carrying();
    }

    /** Millibuckets the line moved in its last busy tick. */
    public int getFlow() {
        FluidNetwork network = this.network();
        return network == null ? 0 : network.flow();
    }

    /**
     * The pipe as other mods' pipes, valves and machines see it on one face:
     * whatever goes in goes on along the line, and drawing from it draws from
     * what the line reaches.
     */
    public IFluidHandler getFluidStorage(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        if (this.doors[index] == null) {
            BlockPos from = side == null ? this.worldPosition : this.worldPosition.relative(side);
            this.doors[index] = new Door(from);
        }
        return this.doors[index];
    }

    private final class Door implements IFluidHandler {
        private final BlockPos from;

        Door(BlockPos from) {
            this.from = from;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return FluidNetwork.RATE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            FluidNetwork network = FuelPipeBlockEntity.this.network();
            return network == null ? 0 : network.insert(resource, action, this.from);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidNetwork network = FuelPipeBlockEntity.this.network();
            return network == null || resource.isEmpty() ? FluidStack.EMPTY
                    : network.extract(resource.getFluid(), resource.getAmount(), action, this.from);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidNetwork network = FuelPipeBlockEntity.this.network();
            return network == null ? FluidStack.EMPTY : network.extract(null, maxDrain, action, this.from);
        }
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

    // ---- saving ----

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Pipes saved before lines became networks held liquid of their own; it is let go.
        this.blocked = ValueInput.of(tag, registries).getIntOr("Blocked", 0);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput.of(tag, registries).putInt("Blocked", this.blocked);
    }
}
