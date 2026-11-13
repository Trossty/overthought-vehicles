package me.trossty.car.fluid;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * An item that is either full of one measure of one liquid or empty, and turns
 * into the other item when poured or filled: a milk bucket and its bucket, a
 * honey bottle and its glass bottle. All or nothing, like the items themselves.
 */
public final class FixedContainer implements IFluidHandlerItem {

    private ItemStack container;
    private final Item other;
    private final Fluid fluid;
    private final int amount;
    private final boolean full;

    public FixedContainer(ItemStack container, Item emptied, Fluid fluid, int amount) {
        this(container, emptied, fluid, amount, true);
    }

    private FixedContainer(ItemStack container, Item other, Fluid fluid, int amount, boolean full) {
        this.container = container;
        this.other = other;
        this.fluid = fluid;
        this.amount = amount;
        this.full = full;
    }

    /** An empty container that becomes {@code filled} once a whole measure goes in. */
    public static FixedContainer empty(ItemStack container, Item filled, Fluid fluid, int amount) {
        return new FixedContainer(container, filled, fluid, amount, false);
    }

    private boolean holds() {
        return this.full && !this.container.isEmpty() && this.container.getItem() != this.other;
    }

    @Override
    public ItemStack getContainer() {
        return this.container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.holds() ? new FluidStack(this.fluid, this.amount) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.amount;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.is(this.fluid);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (this.full || this.container.getCount() != 1 || !resource.is(this.fluid)
                || resource.getAmount() < this.amount) {
            return 0;
        }
        if (action.execute()) {
            this.container = new ItemStack(this.other);
        }
        return this.amount;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return resource.is(this.fluid) ? this.drain(resource.getAmount(), action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (!this.holds() || this.container.getCount() != 1 || maxDrain < this.amount) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            this.container = new ItemStack(this.other);
        }
        return new FluidStack(this.fluid, this.amount);
    }
}
