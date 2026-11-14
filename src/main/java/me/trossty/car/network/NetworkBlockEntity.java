package me.trossty.car.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One length of a {@link Network}: a pipe or a cable. It knows which way it
 * reaches and which run it belongs to, and nothing else; the run does the work.
 */
public abstract class NetworkBlockEntity<N extends Network> extends BlockEntity {

    @Nullable
    private N network;

    protected NetworkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** What this length's side leads to, read from its block state. */
    public abstract Network.Link link(Direction side);

    /** Walks the run this length is part of. */
    protected abstract N gather(ServerLevel world);

    /** The run, found now if it has to be; null on the client. */
    @Nullable
    public final N network() {
        if (this.network == null && this.level instanceof ServerLevel server && !this.isRemoved()) {
            this.network = this.gather(server);
        }
        return this.network;
    }

    @SuppressWarnings("unchecked")
    final void attach(Network network) {
        if (this.network != null && this.network != network) {
            this.network.invalidate();
        }
        this.network = (N) network;
    }

    final void detach(Network network) {
        if (this.network == network) {
            this.network = null;
        }
    }

    private void forget() {
        if (this.network != null) {
            this.network.invalidate();
        }
    }

    public static void tick(Level world, BlockPos pos, BlockState state, NetworkBlockEntity<?> length) {
        Network network = length.network();
        if (network != null) {
            network.tick();
        }
    }

    /** A length that now reaches a different way changes the run. */
    @Override
    public void setBlockState(BlockState state) {
        BlockState before = this.getBlockState();
        super.setBlockState(state);
        for (Direction side : Direction.values()) {
            if (!java.util.Objects.equals(this.linkIn(before, side), this.linkIn(state, side))) {
                this.forget();
                return;
            }
        }
    }

    /** How this side is joined in a state, which is all {@link #setBlockState} compares. */
    protected abstract Object linkIn(BlockState state, Direction side);

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.forget();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.forget();
    }
}
