package me.trossty.car.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * Several of a machine's tanks behind one door: a pipe filling it reaches the
 * first one that takes the liquid, and one draining it the first that gives.
 */
public final class CombinedTanks implements IFluidHandler {

    private final List<IFluidHandler> parts;

    public CombinedTanks(IFluidHandler... parts) {
        this.parts = List.of(parts);
    }

    @Override
    public int getTanks() {
        int tanks = 0;
        for (IFluidHandler part : this.parts) {
            tanks += part.getTanks();
        }
        return tanks;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        for (IFluidHandler part : this.parts) {
            if (tank < part.getTanks()) {
                return part.getFluidInTank(tank);
            }
            tank -= part.getTanks();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        for (IFluidHandler part : this.parts) {
            if (tank < part.getTanks()) {
                return part.getTankCapacity(tank);
            }
            tank -= part.getTanks();
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        for (IFluidHandler part : this.parts) {
            if (tank < part.getTanks()) {
                return part.isFluidValid(tank, stack);
            }
            tank -= part.getTanks();
        }
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        for (IFluidHandler part : this.parts) {
            int filled = part.fill(resource, action);
            if (filled > 0) {
                return filled;
            }
        }
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        for (IFluidHandler part : this.parts) {
            FluidStack drained = part.drain(resource, action);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (IFluidHandler part : this.parts) {
            FluidStack drained = part.drain(maxDrain, action);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }
}
