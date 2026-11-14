package me.trossty.car.network;

import me.trossty.car.block.entity.FuelDrumBlockEntity;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.fluid.FluidSource;
import me.trossty.car.fluid.Liquids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A line of fuel pipe. What it touches falls into three kinds:
 *
 * <ul>
 *   <li><b>Machines that make something</b> — pumpjack, refinery,
 *       hydrotreater. The line draws off what they make on its own.
 *   <li><b>Stores</b> — a yard tank, a drum, another mod's tank: anything that
 *       holds liquid and will give it back. A store takes what the machines
 *       make once every machine on the line has had what it wants, and gives
 *       only to machines, never to another store, so two tanks on one line sit
 *       still until a valve pulls from one of them.
 *   <li><b>Everything else</b> — a fuel pump, a generator, the crude side of a
 *       refinery: they only take, and are served before any store.
 * </ul>
 *
 * <p>A line carries one liquid in a tick. When two things on it offer
 * different liquids they take it in turns, so a refinery's petrol and the
 * crude feeding it can share one line without ever mixing in a tank.
 */
public final class FluidNetwork extends Network {

    /** The most one connection gives or takes a tick, in millibuckets. */
    public static final int RATE = 100;

    /** How long after the last drop a line still says what it was carrying. */
    private static final int SHOW_FOR = 20;

    private Fluid carried = Fluids.EMPTY;
    private long carriedAt = Long.MIN_VALUE;
    private int flow;
    private int flowing;
    /** Set while the line is moving liquid, so a block that hands it back in at once is turned away. */
    private boolean busy;

    public FluidNetwork(ServerLevel world, List<NetworkBlockEntity<?>> members, List<Port> ports) {
        super(world, members, ports);
    }

    /** The liquid the line moved last, while it is still moving. */
    public Fluid carrying() {
        return this.world.getGameTime() - this.carriedAt <= SHOW_FOR ? this.carried : Fluids.EMPTY;
    }

    /** Millibuckets moved in the last tick that moved any. */
    public int flow() {
        return this.carrying() == Fluids.EMPTY ? 0 : this.flow;
    }

    // ---- what is on the line ----

    private record End(BlockPos pos, IFluidHandler handler, boolean maker, boolean store) {
        /** A machine or a store; anything else only takes. */
        boolean gives() {
            return this.maker || this.store;
        }
    }

    private List<End> ends() {
        List<End> ends = new ArrayList<>(this.ports.size());
        for (Port port : this.ports) {
            if (!this.world.isLoaded(port.pos())) {
                continue;
            }
            IFluidHandler handler = this.world.getCapability(Capabilities.FluidHandler.BLOCK, port.pos(), port.face());
            if (handler == null) {
                continue;
            }
            boolean maker = FluidSource.at(this.world, port.pos());
            boolean store = !maker && (isStore(this.world.getBlockEntity(port.pos()))
                    || !handler.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty());
            ends.add(new End(port.pos(), handler, maker, store));
        }
        return ends;
    }

    /** This mod's stores count as stores even while empty. */
    private static boolean isStore(@Nullable BlockEntity entity) {
        return entity instanceof FuelTankBlockEntity || entity instanceof FuelDrumBlockEntity;
    }

    // ---- each tick ----

    @Override
    protected void work() {
        List<End> ends = this.ends();
        if (ends.size() < 2) {
            return;
        }
        this.busy = true;
        try {
            Fluid fluid = this.choose(ends);
            if (fluid == null) {
                return;
            }
            int count = ends.size();
            int[] given = new int[count];
            int[] taken = new int[count];
            int moved = 0;
            // Machines' output to everyone, machines first; then stores to machines.
            for (int k = 0; k < count; k++) {
                int from = this.rotated(k, count);
                if (ends.get(from).maker()) {
                    moved += this.pass(ends, from, fluid, given, taken, true);
                }
            }
            for (int k = 0; k < count; k++) {
                int from = this.rotated(k, count);
                if (ends.get(from).store()) {
                    moved += this.pass(ends, from, fluid, given, taken, false);
                }
            }
            if (moved > 0) {
                this.carried(fluid, moved);
            }
        } finally {
            this.busy = false;
        }
    }

    private void carried(Fluid fluid, int amount) {
        long now = this.world.getGameTime();
        this.flowing = this.carriedAt == now ? this.flowing + amount : amount;
        this.flow = this.flowing;
        this.carried = fluid;
        this.carriedAt = now;
    }

    /**
     * This tick's liquid: the first, counting round from a different end each
     * tick, that something on the line gives and something else will take.
     */
    @Nullable
    private Fluid choose(List<End> ends) {
        int count = ends.size();
        for (int k = 0; k < count; k++) {
            End giver = ends.get(this.rotated(k, count));
            if (!giver.gives()) {
                continue;
            }
            int tanks = giver.handler().getTanks();
            for (int t = 0; t < tanks; t++) {
                Fluid fluid = giver.handler().getFluidInTank(this.rotated(t, tanks)).getFluid();
                if (Liquids.isBlank(fluid) || giver.handler().drain(new FluidStack(fluid, RATE),
                        IFluidHandler.FluidAction.SIMULATE).isEmpty()) {
                    continue;
                }
                if (this.anyTakes(ends, giver.pos(), fluid, giver.maker())) {
                    return fluid;
                }
            }
        }
        return null;
    }

    private boolean anyTakes(List<End> ends, BlockPos from, Fluid fluid, boolean storesToo) {
        FluidStack probe = new FluidStack(fluid, RATE);
        for (End end : ends) {
            if (!end.pos().equals(from) && (storesToo || !end.store())
                    && end.handler().fill(probe, IFluidHandler.FluidAction.SIMULATE) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Who is served first: what only takes, then a store already holding this
     * liquid, then any other store, so one tank fills up with petrol and the
     * next is left for diesel rather than both getting a little of the first.
     */
    private static int rank(End end, Fluid fluid) {
        if (!end.store()) {
            return 0;
        }
        IFluidHandler handler = end.handler();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (handler.getFluidInTank(tank).getFluid() == fluid) {
                return 1;
            }
        }
        return 2;
    }

    /** Hands one end's liquid round the others in {@link #rank} order, stores only if allowed. */
    private int pass(List<End> ends, int from, Fluid fluid, int[] given, int[] taken, boolean storesToo) {
        End giver = ends.get(from);
        int count = ends.size();
        int moved = 0;
        for (int pass = 0; pass < (storesToo ? 3 : 1); pass++) {
            for (int k = 0; k < count && given[from] < RATE; k++) {
                int to = this.rotated(k, count);
                End taker = ends.get(to);
                if (taker.pos().equals(giver.pos()) || rank(taker, fluid) != pass || taken[to] >= RATE) {
                    continue;
                }
                int amount = Liquids.move(giver.handler(), taker.handler(), fluid,
                        Math.min(RATE - given[from], RATE - taken[to]));
                given[from] += amount;
                taken[to] += amount;
                moved += amount;
            }
        }
        return moved;
    }

    // ---- other blocks working the line ----

    /** Whether the line can still take this liquid this tick. */
    private boolean free(Fluid fluid) {
        return !this.busy && (this.carriedAt != this.world.getGameTime() || this.carried == fluid);
    }

    /**
     * Something pouring into the line at {@code from} — another mod's pipe, a
     * valve, a machine pushing out — goes straight on to whatever takes it,
     * in {@link #rank} order, never back where it came from.
     */
    public int insert(FluidStack stack, IFluidHandler.FluidAction action, BlockPos from) {
        if (stack.isEmpty() || !this.free(stack.getFluid())) {
            return 0;
        }
        List<End> ends = this.ends();
        int left = Math.min(stack.getAmount(), RATE);
        this.busy = true;
        try {
            for (int pass = 0; pass < 3 && left > 0; pass++) {
                for (int k = 0; k < ends.size() && left > 0; k++) {
                    End taker = ends.get(this.rotated(k, ends.size()));
                    if (taker.pos().equals(from) || rank(taker, stack.getFluid()) != pass) {
                        continue;
                    }
                    left -= taker.handler().fill(stack.copyWithAmount(left), action);
                }
            }
        } finally {
            this.busy = false;
        }
        int filled = Math.min(stack.getAmount(), RATE) - left;
        if (filled > 0 && action.execute()) {
            this.carried(stack.getFluid(), filled);
        }
        return filled;
    }

    /**
     * Something drawing off the line at {@code from} — a valve, another mod's
     * pipe — gets what the machines make first, then what the stores hold.
     * {@code only} narrows it to one liquid.
     */
    public FluidStack extract(@Nullable Fluid only, int max, IFluidHandler.FluidAction action, BlockPos from) {
        if (max <= 0 || this.busy) {
            return FluidStack.EMPTY;
        }
        long now = this.world.getGameTime();
        Fluid fluid = only != null ? only : this.carriedAt == now ? this.carried : null;
        if (fluid != null && !this.free(fluid)) {
            return FluidStack.EMPTY;
        }
        List<End> ends = this.ends();
        int left = Math.min(max, RATE);
        this.busy = true;
        try {
            for (int pass = 0; pass < 2 && left > 0; pass++) {
                for (int k = 0; k < ends.size() && left > 0; k++) {
                    End giver = ends.get(this.rotated(k, ends.size()));
                    if (giver.pos().equals(from) || !(pass == 0 ? giver.maker() : giver.store())) {
                        continue;
                    }
                    FluidStack got = fluid == null
                            ? giver.handler().drain(left, action)
                            : giver.handler().drain(new FluidStack(fluid, left), action);
                    if (!got.isEmpty()) {
                        fluid = got.getFluid();
                        left -= got.getAmount();
                    }
                }
            }
        } finally {
            this.busy = false;
        }
        int drained = Math.min(max, RATE) - left;
        if (drained <= 0 || fluid == null) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            this.carried(fluid, drained);
        }
        return new FluidStack(fluid, drained);
    }
}
