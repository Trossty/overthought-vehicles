package me.trossty.car.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * One face of a store that only lets energy one way: an input face that takes
 * and never gives, or an output face that gives and never takes. A battery
 * bank shows the first on five sides and the second on its front, so a cable
 * on the front is fed and never pushes back into it.
 */
public final class OneWayEnergy implements EnergyStorage {

    private final EnergyStorage store;
    private final boolean in;

    private OneWayEnergy(EnergyStorage store, boolean in) {
        this.store = store;
        this.in = in;
    }

    public static EnergyStorage in(EnergyStorage store) {
        return new OneWayEnergy(store, true);
    }

    public static EnergyStorage out(EnergyStorage store) {
        return new OneWayEnergy(store, false);
    }

    @Override
    public boolean supportsInsertion() {
        return this.in;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        return this.in ? this.store.insert(maxAmount, transaction) : 0;
    }

    @Override
    public boolean supportsExtraction() {
        return !this.in;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return this.in ? 0 : this.store.extract(maxAmount, transaction);
    }

    @Override
    public long getAmount() {
        return this.store.getAmount();
    }

    @Override
    public long getCapacity() {
        return this.store.getCapacity();
    }
}
