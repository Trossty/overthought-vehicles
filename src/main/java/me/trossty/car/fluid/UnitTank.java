package me.trossty.car.fluid;

import me.trossty.car.registry.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;

import java.util.function.Predicate;

/**
 * A machine's tank: one liquid at a time, counted in the mod's units (a
 * millibucket each), and open to the transfer API in whichever directions
 * the machine allows.
 *
 * <p>The production line's machines all hold their liquids in one of these —
 * the pumpjack's crude, the hydrotreater's two sides, a generator's fuel — so
 * a tanker's hose, a bucket or another mod's pipe all reach them the same way.
 * Like the petrol holders, it only lets whole units across.
 */
public final class UnitTank extends SnapshotParticipant<UnitTank.State> implements SingleSlotStorage<FluidVariant> {

    record State(Fluid fluid, int amount) {
    }

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

    public void write(WriteView view, String key) {
        view.putString(key + "Fluid", Registries.FLUID.getId(this.fluid).toString());
        view.putInt(key + "Amount", this.amount);
    }

    public void read(ReadView view, String key) {
        Identifier id = Identifier.tryParse(view.getString(key + "Fluid", "minecraft:empty"));
        Fluid stored = id == null ? Fluids.EMPTY : Registries.FLUID.get(id);
        this.set(stored == null ? Fluids.EMPTY : stored, view.getInt(key + "Amount", 0));
    }

    // ---- what a pipe sees ----

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!this.insertable || resource.isBlank() || resource.hasComponents() || !this.canTake(resource.getFluid())) {
            return 0;
        }
        int units = (int) Math.min(maxAmount / ModFluids.UNIT, this.room());
        if (units <= 0) {
            return 0;
        }
        this.updateSnapshots(transaction);
        this.fluid = resource.getFluid();
        this.amount += units;
        return units * ModFluids.UNIT;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!this.extractable || this.isEmpty() || !resource.isOf(this.fluid)) {
            return 0;
        }
        int units = (int) Math.min(maxAmount / ModFluids.UNIT, this.amount);
        if (units <= 0) {
            return 0;
        }
        this.updateSnapshots(transaction);
        this.drain(units);
        return units * ModFluids.UNIT;
    }

    @Override
    public boolean supportsInsertion() {
        return this.insertable;
    }

    @Override
    public boolean supportsExtraction() {
        return this.extractable;
    }

    @Override
    public boolean isResourceBlank() {
        return this.isEmpty();
    }

    @Override
    public FluidVariant getResource() {
        return this.isEmpty() ? FluidVariant.blank() : FluidVariant.of(this.fluid);
    }

    @Override
    public long getAmount() {
        return this.amount * ModFluids.UNIT;
    }

    @Override
    public long getCapacity() {
        return this.capacity * ModFluids.UNIT;
    }

    @Override
    protected State createSnapshot() {
        return new State(this.fluid, this.amount);
    }

    @Override
    protected void readSnapshot(State snapshot) {
        this.fluid = snapshot.fluid();
        this.amount = snapshot.amount();
    }

    @Override
    protected void onFinalCommit() {
        this.changed.run();
    }
}
