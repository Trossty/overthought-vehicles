package me.trossty.car.machine;

import me.trossty.car.fluid.UnitTank;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that shows its gauges in the shared machine window: an energy
 * bar, up to two tanks, a progress mark and one extra number whose meaning
 * depends on the machine (seconds of fuel left, crude left in the ground,
 * a vehicle's charge).
 *
 * <p>Generators, the hydrotreater, the pumpjack, the battery and the charging
 * post all speak this; {@link MachineScreenHandler} turns it into synced
 * properties and the client draws them without knowing which machine it is
 * beyond {@link #gaugeKind()}.
 */
public interface GaugeSource {

    int KIND_COAL_GENERATOR = 0;
    int KIND_BIOFUEL_GENERATOR = 1;
    int KIND_DIESEL_GENERATOR = 2;
    int KIND_HYDROTREATER = 3;
    int KIND_PUMPJACK = 4;
    int KIND_BATTERY = 5;
    int KIND_CHARGER = 6;

    int gaugeKind();

    long gaugeEnergy();

    long gaugeEnergyCapacity();

    /** The first tank (what goes in, or the only one); null when there is none. */
    default @Nullable UnitTank gaugeTankA() {
        return null;
    }

    /** The second tank (what comes out); null when there is none. */
    default @Nullable UnitTank gaugeTankB() {
        return null;
    }

    /** How far along the current job is, 0 to 1000. */
    default int gaugeProgress() {
        return 0;
    }

    /** The one machine-specific number. */
    default int gaugeExtra() {
        return 0;
    }

    /** A second machine-specific number, for the few that need one. */
    default int gaugeExtra2() {
        return 0;
    }

    boolean gaugeWorking();
}
