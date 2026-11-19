package me.trossty.car.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A run of pipe or cable joined end to end, worked as one thing.
 *
 * <p>The run holds nothing itself. Each tick it looks at the blocks its ends
 * touch (its {@link Port ports}), takes from those that give and hands it
 * straight to those that take, so a hundred lengths cost what one does and a
 * machine at the far end is served as quickly as one next door.
 *
 * <p>The run is found once, by walking from length to length, and kept until
 * one of its lengths changes which way it reaches (it is laid, broken, cut
 * with a wrench, or joins a new neighbour) or its chunk unloads. Then every
 * length forgets it, and the next one to tick walks the run again.
 */
public abstract class Network {

    /** Where a run touches something that is not more of itself: that block, and the face it shows the run. */
    public record Port(BlockPos pos, Direction face) {
    }

    /** What one side of a length leads to. */
    public enum Link {
        NONE,
        /** More of the same run. */
        MEMBER,
        /** Something to give to or take from. */
        PORT
    }

    protected final ServerLevel world;
    private final List<NetworkBlockEntity<?>> members;
    protected final List<Port> ports;
    private boolean valid = true;
    private long lastTick = Long.MIN_VALUE;
    /** Moves on every tick, so who goes first changes and a short supply is shared round. */
    protected int turn;

    protected Network(ServerLevel world, List<NetworkBlockEntity<?>> members, List<Port> ports) {
        this.world = world;
        this.members = members;
        this.ports = ports;
        for (NetworkBlockEntity<?> member : members) {
            member.attach(this);
        }
    }

    /** Runs the tick's work once, however many lengths ask. */
    public final void tick() {
        long now = this.world.getGameTime();
        if (!this.valid || now == this.lastTick) {
            return;
        }
        this.lastTick = now;
        this.turn++;
        this.work();
    }

    protected abstract void work();

    public final boolean isValid() {
        return this.valid;
    }

    public final int size() {
        return this.members.size();
    }

    protected final List<NetworkBlockEntity<?>> members() {
        return this.members;
    }

    /** Every length forgets this run; the next to tick finds it again. */
    public final void invalidate() {
        if (!this.valid) {
            return;
        }
        this.valid = false;
        for (NetworkBlockEntity<?> member : this.members) {
            member.detach(this);
        }
    }

    /** {@code index}, counted from this tick's starting point. */
    protected final int rotated(int index, int count) {
        return Math.floorMod(this.turn + index, count);
    }

    @FunctionalInterface
    public interface Factory<N extends Network> {
        N make(ServerLevel world, List<NetworkBlockEntity<?>> members, List<Port> ports);
    }

    /**
     * Walks out from {@code start} along every {@link Link#MEMBER} side to the
     * lengths of the same kind, collecting the ports they touch. Unloaded
     * chunks end the walk rather than being loaded.
     */
    public static <N extends Network> N gather(ServerLevel world, NetworkBlockEntity<N> start, Factory<N> factory) {
        Class<?> kind = start.getClass();
        List<NetworkBlockEntity<?>> members = new ArrayList<>();
        Set<Port> ports = new LinkedHashSet<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.getBlockPos());
        seen.add(start.getBlockPos());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!(world.getBlockEntity(pos) instanceof NetworkBlockEntity<?> member)
                    || member.getClass() != kind || member.isRemoved()) {
                continue;
            }
            members.add(member);
            for (Direction side : Direction.values()) {
                Link link = member.link(side);
                BlockPos next = pos.relative(side);
                if (link == Link.PORT) {
                    ports.add(new Port(next, side.getOpposite()));
                } else if (link == Link.MEMBER && world.isLoaded(next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return factory.make(world, members, new ArrayList<>(ports));
    }
}
