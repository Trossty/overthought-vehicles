package me.trossty.car.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * One face of a store that only lets energy one way: an input face that takes
 * and never gives, or an output face that gives and never takes. A battery
 * bank shows the second on its front, so a cable on the front is fed and never
 * pushes back into it.
 */
public final class OneWayEnergy implements IEnergyStorage {

    private final IEnergyStorage store;
    private final boolean in;

    private OneWayEnergy(IEnergyStorage store, boolean in) {
        this.store = store;
        this.in = in;
    }

    public static IEnergyStorage in(IEnergyStorage store) {
        return new OneWayEnergy(store, true);
    }

    public static IEnergyStorage out(IEnergyStorage store) {
        return new OneWayEnergy(store, false);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return this.in ? this.store.receiveEnergy(toReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return this.in ? 0 : this.store.extractEnergy(toExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return this.store.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return this.store.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return !this.in && this.store.canExtract();
    }

    @Override
    public boolean canReceive() {
        return this.in && this.store.canReceive();
    }
}
