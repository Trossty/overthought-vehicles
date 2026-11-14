package me.trossty.car.network;

import me.trossty.car.block.entity.EnergyCableBlockEntity;
import me.trossty.car.energy.EnergyBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * A run of power cable. What it touches either only gives (a generator, a
 * battery's front), only takes (a machine, a charging post, a battery's back)
 * or does both (another mod's battery or cable), and power goes from the first
 * to the second, then from the first to the third, then from the third to the
 * second: a store tops up from the generators once the machines have had
 * theirs, and gives back only to machines.
 *
 * <p>Nothing is lost along the way and nothing stands in the cable.
 */
public final class EnergyNetwork extends Network {

    /** How often the copper stripe is brought up to date. */
    private static final int GLOW_EVERY = 10;

    private int flow;
    /** Pushed in from outside since the last tick's work: a generator feeding the run counts as flow. */
    private int pushed;
    private int window;
    /** Set while the run is moving power, so a block that hands it straight back is turned away. */
    private boolean busy;

    public EnergyNetwork(ServerLevel world, List<NetworkBlockEntity<?>> members, List<Port> ports) {
        super(world, members, ports);
    }

    /** The most one connection gives or takes a tick, as the config has it. */
    public static int rate() {
        return me.trossty.car.CarConfig.get().cableRate;
    }

    /** Energy moved in the last tick. */
    public int flow() {
        return this.flow;
    }

    private record End(BlockPos pos, IEnergyStorage storage, boolean gives, boolean takes) {
        boolean store() {
            return this.gives && this.takes;
        }
    }

    private List<End> ends() {
        List<End> ends = new ArrayList<>(this.ports.size());
        for (Port port : this.ports) {
            if (!this.world.isLoaded(port.pos())) {
                continue;
            }
            IEnergyStorage storage = this.world.getCapability(Capabilities.EnergyStorage.BLOCK, port.pos(), port.face());
            if (storage != null && (storage.canExtract() || storage.canReceive())) {
                ends.add(new End(port.pos(), storage, storage.canExtract(), storage.canReceive()));
            }
        }
        return ends;
    }

    @Override
    protected void work() {
        int moved = 0;
        List<End> ends = this.ends();
        if (ends.size() >= 2) {
            this.busy = true;
            try {
                int count = ends.size();
                long[] given = new long[count];
                long[] taken = new long[count];
                for (int k = 0; k < count; k++) {
                    int from = this.rotated(k, count);
                    End giver = ends.get(from);
                    if (giver.gives() && !giver.store()) {
                        moved += this.pass(ends, from, given, taken, true);
                    }
                }
                for (int k = 0; k < count; k++) {
                    int from = this.rotated(k, count);
                    if (ends.get(from).store()) {
                        moved += this.pass(ends, from, given, taken, false);
                    }
                }
            } finally {
                this.busy = false;
            }
        }
        this.flow = moved + this.pushed;
        this.window += this.flow;
        this.pushed = 0;
        if (this.turn % GLOW_EVERY == 0) {
            boolean lit = this.window > 0;
            this.window = 0;
            for (NetworkBlockEntity<?> member : this.members()) {
                if (member instanceof EnergyCableBlockEntity cable) {
                    cable.showPowered(lit);
                }
            }
        }
    }

    /** Hands one end's power round the others: those that only take first, then (if allowed) the stores. */
    private int pass(List<End> ends, int from, long[] given, long[] taken, boolean storesToo) {
        End giver = ends.get(from);
        int count = ends.size();
        long rate = rate();
        int moved = 0;
        for (int pass = 0; pass < (storesToo ? 2 : 1); pass++) {
            for (int k = 0; k < count && given[from] < rate; k++) {
                int to = this.rotated(k, count);
                End taker = ends.get(to);
                if (!taker.takes() || taker.pos().equals(giver.pos()) || taker.store() != (pass == 1)
                        || taken[to] >= rate) {
                    continue;
                }
                int amount = EnergyBuffer.move(giver.storage(), taker.storage(),
                        Math.min(rate - given[from], rate - taken[to]));
                given[from] += amount;
                taken[to] += amount;
                moved += amount;
            }
        }
        return moved;
    }

    // ---- other blocks working the run ----

    /** Power pushed in at {@code from} goes straight on to what takes it, machines before stores. */
    public int insert(int amount, boolean simulate, BlockPos from) {
        if (amount <= 0 || this.busy) {
            return 0;
        }
        List<End> ends = this.ends();
        int left = Math.min(amount, rate());
        int start = left;
        this.busy = true;
        try {
            for (int pass = 0; pass < 2 && left > 0; pass++) {
                for (int k = 0; k < ends.size() && left > 0; k++) {
                    End taker = ends.get(this.rotated(k, ends.size()));
                    if (taker.takes() && !taker.pos().equals(from) && taker.store() == (pass == 1)) {
                        left -= taker.storage().receiveEnergy(left, simulate);
                    }
                }
            }
        } finally {
            this.busy = false;
        }
        if (!simulate) {
            this.pushed += start - left;
        }
        return start - left;
    }

    /** Power drawn off at {@code from} comes from the generators first, then the stores. */
    public int extract(int amount, boolean simulate, BlockPos from) {
        if (amount <= 0 || this.busy) {
            return 0;
        }
        List<End> ends = this.ends();
        int left = Math.min(amount, rate());
        int start = left;
        this.busy = true;
        try {
            for (int pass = 0; pass < 2 && left > 0; pass++) {
                for (int k = 0; k < ends.size() && left > 0; k++) {
                    End giver = ends.get(this.rotated(k, ends.size()));
                    if (giver.gives() && !giver.pos().equals(from) && giver.store() == (pass == 1)) {
                        left -= giver.storage().extractEnergy(left, simulate);
                    }
                }
            }
        } finally {
            this.busy = false;
        }
        if (!simulate) {
            this.pushed += start - left;
        }
        return start - left;
    }
}
