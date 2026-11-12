package me.trossty.car.fluid;

import me.trossty.car.registry.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Any of the mod's petrol holders — pipe, valve, pump, refinery outlet — as the
 * rest of Fabric sees it: one slot of petrol in the transfer API.
 *
 * <p>This is the door another mod's pipe uses. It opens on the same number the
 * mod's own blocks use, so there is still only one store of fuel.
 *
 * <p>Only whole units cross. The API counts in eighty-oneths of a unit, and a
 * holder that took the odd fraction and could not give it back would be
 * destroying fuel, so less than one unit is declined.
 */
public final class PetrolSlot extends SnapshotParticipant<Integer> implements SingleSlotStorage<FluidVariant> {

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

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!this.insertable || !resource.isOf(ModFluids.PETROL)) {
            return 0;
        }
        int units = (int) Math.min(maxAmount / ModFluids.UNIT, this.capacity - this.amount.getAsInt());
        if (units <= 0) {
            return 0;
        }
        this.updateSnapshots(transaction);
        this.set.accept(this.amount.getAsInt() + units);
        return units * ModFluids.UNIT;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!this.extractable || !resource.isOf(ModFluids.PETROL)) {
            return 0;
        }
        int units = (int) Math.min(maxAmount / ModFluids.UNIT, this.amount.getAsInt());
        if (units <= 0) {
            return 0;
        }
        this.updateSnapshots(transaction);
        this.set.accept(this.amount.getAsInt() - units);
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
        return this.amount.getAsInt() <= 0;
    }

    @Override
    public FluidVariant getResource() {
        return this.isResourceBlank() ? FluidVariant.blank() : FluidVariant.of(ModFluids.PETROL);
    }

    @Override
    public long getAmount() {
        return this.amount.getAsInt() * ModFluids.UNIT;
    }

    @Override
    public long getCapacity() {
        return this.capacity * ModFluids.UNIT;
    }

    @Override
    protected Integer createSnapshot() {
        return this.amount.getAsInt();
    }

    @Override
    protected void readSnapshot(Integer snapshot) {
        this.set.accept(snapshot);
    }

    @Override
    protected void onFinalCommit() {
        this.changed.run();
    }
}
