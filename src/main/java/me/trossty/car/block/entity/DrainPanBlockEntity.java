package me.trossty.car.block.entity;

import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The old oil in a drain pan, in millibuckets. It only ever goes out: a pipe
 * draws it off as crude oil for the refinery to make something of again.
 */
public class DrainPanBlockEntity extends BlockEntity {

    /** What a whole sump comes to, drained. */
    public static final int PER_SUMP = 1000;
    public static final int CAPACITY = 8 * PER_SUMP;

    private int oil;

    public DrainPanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAIN_PAN, pos, state);
    }

    public int millibuckets() {
        return this.oil;
    }

    public boolean hasRoomFor(float sumps) {
        return this.oil + Math.round(sumps * PER_SUMP) <= CAPACITY;
    }

    public void catchOil(float sumps) {
        this.oil = Math.min(CAPACITY, this.oil + Math.round(sumps * PER_SUMP));
        this.setChanged();
    }

    private final IFluidHandler outlet = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return oil <= 0 ? FluidStack.EMPTY : new FluidStack(ModFluids.CRUDE_OIL, oil);
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != ModFluids.CRUDE_OIL) {
                return FluidStack.EMPTY;
            }
            return this.drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int out = Math.min(maxDrain, oil);
            if (out <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                oil -= out;
                setChanged();
            }
            return new FluidStack(ModFluids.CRUDE_OIL, out);
        }
    };

    /** How much a pan passes on to its neighbour every five ticks. */
    private static final int RATE = 250;

    /**
     * Pours the old oil into whatever tank, drum or refinery stands beside it,
     * as crude: nothing in the garage pulls from a block, so the pan pushes.
     */
    public static void tick(net.minecraft.world.level.Level world, BlockPos pos, BlockState state, DrainPanBlockEntity pan) {
        if (pan.oil <= 0 || world.getGameTime() % 5 != 0) {
            return;
        }
        for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            IFluidHandler next = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    pos.relative(side), side.getOpposite());
            if (next != null && me.trossty.car.fluid.Liquids.move(pan.outlet, next, ModFluids.CRUDE_OIL, RATE) > 0
                    && pan.oil <= 0) {
                return;
            }
        }
    }

    public IFluidHandler getOutlet() {
        return this.outlet;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.oil = tag.getInt("Oil");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Oil", this.oil);
    }
}
