package me.trossty.car.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.function.Predicate;

/**
 * A machine's tank: one liquid at a time, counted in the mod's units (a
 * millibucket each), and open to NeoForge's fluid handlers in whichever directions
 * the machine allows.
 *
 * <p>The production line's machines all hold their liquids in one of these —
 * the pumpjack's crude, the hydrotreater's two sides, a generator's fuel — so
 * a tanker's hose, a bucket or another mod's pipe all reach them the same way.
 * Like the petrol holders, it only lets whole units across.
 */
public final class UnitTank implements IFluidHandler {

    private final int capacity;
    private final Predicate<Fluid> accepts;
    private final boolean insertable;
    private final boolean extractable;
    private final Runnable changed;

    private Fluid fluid = Fluids.EMPTY;
    private int amount;

    public UnitTank(int capacity, Predicate<Fluid> accepts, boolean insertable, boolean extractable, Runnable changed) {
        this.capacity = capacity;
        this.accepts = accepts;
        this.insertable = insertable;
        this.extractable = extractable;
        this.changed = changed;
    }

    // ---- what the machine itself uses ----

    public Fluid getFluid() {
        return this.fluid;
    }

    /** Units in the tank. */
    public int getUnits() {
        return this.amount;
    }

    public int getCapacityUnits() {
        return this.capacity;
    }

    public boolean isEmpty() {
        return this.amount <= 0;
    }

    public int room() {
        return this.capacity - this.amount;
    }

    /** Whether this liquid could go in now: it is wanted, and the tank is empty or already holds it. */
    public boolean canTake(Fluid liquid) {
        return liquid != Fluids.EMPTY && this.accepts.test(liquid) && (this.isEmpty() || this.fluid == liquid);
    }

    /** Puts up to {@code units} in, and says how many went. */
    public int fill(Fluid liquid, int units) {
        if (units <= 0 || !this.canTake(liquid)) {
            return 0;
        }
        int accepted = Math.min(units, this.room());
        if (accepted > 0) {
            this.fluid = liquid;
            this.amount += accepted;
        }
        return accepted;
    }

    /** Takes up to {@code units} out, and says how many came. */
    public int drain(int units) {
        int given = Math.min(Math.max(0, units), this.amount);
        this.amount -= given;
        if (this.amount <= 0) {
            this.amount = 0;
            this.fluid = Fluids.EMPTY;
        }
        return given;
    }

    /** For commands and tests: puts the tank straight into a state. */
    public void set(Fluid liquid, int units) {
        this.fluid = units > 0 ? liquid : Fluids.EMPTY;
        this.amount = Math.max(0, Math.min(this.capacity, units));
    }

    public void write(ValueOutput view, String key) {
        view.putString(key + "Fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());
        view.putInt(key + "Amount", this.amount);
    }

    public void read(ValueInput view, String key) {
        ResourceLocation id = ResourceLocation.tryParse(view.getStringOr(key + "Fluid", "minecraft:empty"));
        Fluid stored = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
        this.set(stored == null ? Fluids.EMPTY : stored, view.getIntOr(key + "Amount", 0));
    }

    // ---- what a pipe sees ----

    public boolean supportsInsertion() {
        return this.insertable;
    }

    public boolean supportsExtraction() {
        return this.extractable;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.isEmpty() ? FluidStack.EMPTY : new FluidStack(this.fluid, this.amount);
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return this.insertable && this.accepts.test(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!this.insertable || resource.isEmpty() || !resource.isComponentsPatchEmpty()
                || !this.canTake(resource.getFluid())) {
            return 0;
        }
        int units = Math.min(resource.getAmount(), this.room());
        if (units > 0 && action.execute()) {
            this.fluid = resource.getFluid();
            this.amount += units;
            this.changed.run();
        }
        return Math.max(0, units);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || resource.getFluid() != this.fluid) {
            return FluidStack.EMPTY;
        }
        return this.drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (!this.extractable || this.isEmpty()) {
            return FluidStack.EMPTY;
        }
        Fluid held = this.fluid;
        int units = Math.min(maxDrain, this.amount);
        if (units <= 0) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            this.drain(units);
            this.changed.run();
        }
        return new FluidStack(held, units);
    }
}
