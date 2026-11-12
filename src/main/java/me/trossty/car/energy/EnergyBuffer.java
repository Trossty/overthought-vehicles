package me.trossty.car.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * A plain store of energy with its own in and out limits, open to every other
 * mod's cables through NeoForge's energy capability (one unit is one FE).
 *
 * <p>The block entities read and set {@link #amount} directly for their own
 * bookkeeping; {@link #onChanged()} runs whenever another block moves energy
 * in or out for real, which is where a block entity marks itself dirty.
 */
public class EnergyBuffer implements IEnergyStorage {

    private final long capacity;
    private final long maxInsert;
    private final long maxExtract;

    public long amount;

    public EnergyBuffer(long capacity, long maxInsert, long maxExtract) {
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
    }

    /** Runs after energy was really moved in or out by someone else. */
    protected void onChanged() {
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        long accepted = Math.min(Math.min(this.maxInsert, toReceive), this.capacity - this.amount);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            this.amount += accepted;
            this.onChanged();
        }
        return (int) accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        long given = Math.min(Math.min(this.maxExtract, toExtract), this.amount);
        if (given <= 0) {
            return 0;
        }
        if (!simulate) {
            this.amount -= given;
            this.onChanged();
        }
        return (int) given;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, this.amount);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, this.capacity);
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxInsert > 0;
    }

    /** Moves up to {@code max} from one store to another, as much as both allow; how much went. */
    public static int move(IEnergyStorage from, IEnergyStorage to, long max) {
        int limit = (int) Math.min(Integer.MAX_VALUE, max);
        int offered = from.extractEnergy(limit, true);
        int accepted = offered <= 0 ? 0 : to.receiveEnergy(offered, true);
        if (accepted <= 0) {
            return 0;
        }
        int taken = from.extractEnergy(accepted, false);
        return to.receiveEnergy(taken, false);
    }
}
