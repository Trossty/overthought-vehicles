package me.trossty.car.fluid;

import me.trossty.car.registry.ModFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Any of the mod's petrol holders — pipe, valve, pump, refinery outlet — as the
 * rest of NeoForge sees it: one tank of petrol behind the fluid capability.
 *
 * <p>This is the door another mod's pipe uses. It opens on the same number the
 * mod's own blocks use, so there is still only one store of fuel.
 */
public final class PetrolSlot implements IFluidHandler {

    private final IntSupplier amount;
    private final IntConsumer set;
    private final int capacity;
    private final boolean insertable;
    private final boolean extractable;
    private final Runnable changed;

    public PetrolSlot(IntSupplier amount, IntConsumer set, int capacity,
                      boolean insertable, boolean extractable, Runnable changed) {
        this.amount = amount;
        this.set = set;
        this.capacity = capacity;
        this.insertable = insertable;
        this.extractable = extractable;
        this.changed = changed;
    }

    public boolean supportsInsertion() {
        return this.insertable;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        int held = this.amount.getAsInt();
        return held <= 0 ? FluidStack.EMPTY : new FluidStack(ModFluids.PETROL, held);
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.is(ModFluids.PETROL);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!this.insertable || !resource.is(ModFluids.PETROL)) {
            return 0;
        }
        int units = Math.min(resource.getAmount(), this.capacity - this.amount.getAsInt());
        if (units <= 0) {
            return 0;
        }
        if (action.execute()) {
            this.set.accept(this.amount.getAsInt() + units);
            this.changed.run();
        }
        return units;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return resource.is(ModFluids.PETROL) ? this.drain(resource.getAmount(), action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (!this.extractable) {
            return FluidStack.EMPTY;
        }
        int units = Math.min(maxDrain, this.amount.getAsInt());
        if (units <= 0) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            this.set.accept(this.amount.getAsInt() - units);
            this.changed.run();
        }
        return new FluidStack(ModFluids.PETROL, units);
    }
}
