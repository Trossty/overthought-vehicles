package me.trossty.car.block.entity;

import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A tank: a steel-and-glass cell that holds one liquid, any liquid — petrol,
 * diesel, crude, milk, water, lava, another mod's oil.
 *
 * <p>Tanks standing against each other are one tank. However a block of them
 * was built, three tall or a four-by-four floor, it fills from the bottom up
 * like a single vessel, every face of it is a door into the whole of it, and
 * the walls between the cells disappear so it looks like one too. Each cell
 * keeps the part of the liquid that sits inside it, which is what it saves and
 * what it draws, and the whole is worked out again when a cell is added or
 * taken away.
 *
 * <p>It still does the fuel yard's jobs on its own when it holds petrol: it
 * drinks from a refinery it touches and feeds a pump it touches, and the mod's
 * pipes and valves move petrol in and out of it.
 */
public class FuelTankBlockEntity extends BlockEntity {

    /** Sixteen buckets a cell. */
    public static final long CELL_CAPACITY = 16 * FluidConstants.BUCKET;

    /** The old measure, in this mod's units (millibuckets): one cell. */
    public static final int CAPACITY = (int) (CELL_CAPACITY / ModFluids.UNIT);

    /** No tank is grown past this many cells by looking for its neighbours. */
    private static final int MAX_CELLS = 512;

    /** Drawn from a touching refinery each tick: twice what a pump manages. */
    private static final int DRAW_RATE = 8;

    /** Pushed into a touching pump each tick. */
    private static final int FEED_RATE = 16;

    /**
     * Goes up whenever a cell is placed, broken, loaded or unloaded anywhere:
     * a group built before that might be the wrong shape now, and is built
     * again the next time it is asked for.
     */
    private static final java.util.concurrent.atomic.AtomicLong SHAPE_VERSION =
            new java.util.concurrent.atomic.AtomicLong();

    private FluidVariant fluid = FluidVariant.blank();
    private long amount;

    /** What the clients were last told, so they are not told it every tick. */
    private FluidVariant sentFluid = FluidVariant.blank();
    private long sentAmount = -1;

    private @Nullable Group group;

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_TANK, pos, state);
    }

    /** Something about the shape of the tanks has changed somewhere. */
    public static void reshape() {
        SHAPE_VERSION.incrementAndGet();
    }

    // ---- this cell ----

    public FluidVariant getFluid() {
        return this.fluid;
    }

    /** What this cell holds, in droplets. */
    public long getAmount() {
        return this.amount;
    }

    /** How full this cell is, nought to one: how high its liquid is drawn. */
    public float getFill() {
        return Math.min(1.0F, (float) this.amount / CELL_CAPACITY);
    }

    private void setContents(FluidVariant fluid, long amount) {
        FluidVariant now = amount <= 0 ? FluidVariant.blank() : fluid;
        long held = Math.max(0, amount);
        if (!now.equals(this.fluid) || held != this.amount) {
            this.fluid = now;
            this.amount = held;
            this.setChanged();
        }
    }

    // ---- the whole tank ----

    /** The tank this cell is part of, worked out again if the shape may have changed. */
    public Group getGroup() {
        if (this.group == null || this.group.version != SHAPE_VERSION.get() || this.isRemoved()) {
            this.group = Group.build(this);
        }
        return this.group;
    }

    /** Every face of every cell is a door into the whole tank. */
    public SingleSlotStorage<FluidVariant> getFluidStorage() {
        return this.getGroup();
    }

    /** The tank's petrol in units, or nought if it holds something else. */
    public int getPetrol() {
        Group tank = this.getGroup();
        return tank.fluid.isOf(ModFluids.PETROL) ? (int) (tank.amount / ModFluids.UNIT) : 0;
    }

    /** The whole tank's room, in units. */
    public int getCapacity() {
        return (int) (this.getGroup().capacity / ModFluids.UNIT);
    }

    /** Petrol in, up to {@code units}; how much went. For the station's own pipes and valves. */
    public int fill(int units) {
        return this.fill(ModFluids.PETROL, units);
    }

    /** Petrol out, up to {@code units}; how much came. */
    public int drain(int units) {
        return this.drain(ModFluids.PETROL, units);
    }

    public int fill(net.minecraft.world.level.material.Fluid liquid, int units) {
        if (units <= 0) {
            return 0;
        }
        return (int) (this.move(FluidVariant.of(liquid), units * ModFluids.UNIT, true) / ModFluids.UNIT);
    }

    public int drain(net.minecraft.world.level.material.Fluid liquid, int units) {
        if (units <= 0) {
            return 0;
        }
        return (int) (this.move(FluidVariant.of(liquid), units * ModFluids.UNIT, false) / ModFluids.UNIT);
    }

    private long move(FluidVariant liquid, long droplets, boolean in) {
        if (Transaction.isOpen()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            Group tank = this.getGroup();
            long moved = in ? tank.insert(liquid, droplets, transaction) : tank.extract(liquid, droplets, transaction);
            transaction.commit();
            return moved;
        }
    }

    /** The whole tank's fill, nought to fifteen, as a comparator reads it. */
    public int comparatorOutput() {
        Group tank = this.getGroup();
        return me.trossty.car.block.LiquidStores.comparatorOutput(tank.amount, tank.capacity);
    }

    // ---- ticking ----

    public static void tick(Level world, BlockPos pos, BlockState state, FuelTankBlockEntity tank) {
        Group whole = tank.getGroup();
        boolean petrol = whole.fluid.isBlank() || whole.fluid.isOf(ModFluids.PETROL);
        if (petrol) {
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                BlockEntity neighbour = me.trossty.car.block.MultiblockBlock.controllerEntity(world, next);
                if (neighbour instanceof me.trossty.car.fluid.PetrolSource refinery) {
                    int room = (int) Math.min(DRAW_RATE, (whole.capacity - whole.amount) / ModFluids.UNIT);
                    if (room > 0) {
                        tank.fill(refinery.drainPetrol(room));
                    }
                    continue;
                }
                if (neighbour instanceof FuelTankBlockEntity) {
                    continue;
                }
                FuelPumpBlockEntity pump = FuelPumpBlock.tankAt(world, next);
                if (pump != null && whole.fluid.isOf(ModFluids.PETROL)) {
                    int offer = Math.min(FEED_RATE, tank.getPetrol());
                    if (offer > 0) {
                        tank.drain(pump.fill(offer));
                    }
                }
            }
        }

        // Tell the clients, but not every tick: a few times a second is plenty
        // for a level going up the glass.
        boolean changedLiquid = !tank.fluid.equals(tank.sentFluid);
        if (tank.amount != tank.sentAmount && (changedLiquid || world.getGameTime() % 4 == 0)) {
            tank.sentFluid = tank.fluid;
            tank.sentAmount = tank.amount;
            world.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            world.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    // ---- saving and syncing ----

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.fluid = view.read("Fluid", FluidVariant.CODEC).orElse(FluidVariant.blank());
        this.amount = view.getLongOr("Amount", 0L);
        // A tank from before it could hold anything but petrol.
        int petrol = view.getIntOr("Petrol", 0);
        if (this.amount <= 0 && petrol > 0) {
            this.fluid = FluidVariant.of(ModFluids.PETROL);
            this.amount = petrol * ModFluids.UNIT;
        }
        if (this.amount <= 0) {
            this.fluid = FluidVariant.blank();
            this.amount = 0;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (!this.fluid.isBlank() && this.amount > 0) {
            view.store("Fluid", FluidVariant.CODEC, this.fluid);
            view.putLong("Amount", this.amount);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        reshape();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        reshape();
    }

    // ---- the group ----

    /**
     * All the cells of one tank, and what is in it. It is the one storage
     * every face of every cell hands out, so whichever door a pipe uses, it is
     * the same liquid it is moving.
     */
    public static final class Group extends SnapshotParticipant<ResourceAmount<FluidVariant>>
            implements SingleSlotStorage<FluidVariant> {

        private final List<FuelTankBlockEntity> cells;
        private final long version;
        private final long capacity;
        private FluidVariant fluid;
        private long amount;

        private Group(List<FuelTankBlockEntity> cells, FluidVariant fluid, long amount) {
            this.cells = cells;
            this.version = SHAPE_VERSION.get();
            this.capacity = cells.size() * CELL_CAPACITY;
            this.fluid = amount > 0 ? fluid : FluidVariant.blank();
            this.amount = Math.min(amount, this.capacity);
        }

        /** Every tank cell joined to {@code start}, face to face. */
        static Group build(FuelTankBlockEntity start) {
            Level world = start.getLevel();
            List<FuelTankBlockEntity> cells = new ArrayList<>();
            cells.add(start);
            if (world != null && !start.isRemoved()) {
                Set<BlockPos> seen = new HashSet<>();
                seen.add(start.getBlockPos());
                ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                queue.add(start.getBlockPos());
                while (!queue.isEmpty() && cells.size() < MAX_CELLS) {
                    BlockPos at = queue.poll();
                    for (Direction side : Direction.values()) {
                        BlockPos next = at.relative(side);
                        if (!seen.add(next) || !world.hasChunkAt(next)) {
                            continue;
                        }
                        if (world.getBlockEntity(next) instanceof FuelTankBlockEntity cell && !cell.isRemoved()) {
                            cells.add(cell);
                            queue.add(next);
                            if (cells.size() >= MAX_CELLS) {
                                break;
                            }
                        }
                    }
                }
            }

            // One liquid in a tank. Cells are never put up between two
            // different ones, so this only sorts out an odd old world.
            FluidVariant liquid = FluidVariant.blank();
            long most = 0;
            for (FuelTankBlockEntity cell : cells) {
                if (!cell.fluid.isBlank() && cell.amount > most) {
                    liquid = cell.fluid;
                    most = cell.amount;
                }
            }
            long total = 0;
            for (FuelTankBlockEntity cell : cells) {
                if (cell.fluid.equals(liquid)) {
                    total += cell.amount;
                }
            }
            cells.sort(Comparator.comparingInt((FuelTankBlockEntity cell) -> cell.getBlockPos().getY()));
            Group group = new Group(cells, liquid, total);
            for (FuelTankBlockEntity cell : cells) {
                cell.group = group;
            }
            group.settle();
            return group;
        }

        public int size() {
            return this.cells.size();
        }

        /**
         * Shares the liquid out from the bottom up, a layer at a time and evenly
         * across each layer, so every cell holds exactly what would be in it if
         * the walls between them were not there.
         */
        private void settle() {
            long left = this.amount;
            int i = 0;
            while (i < this.cells.size()) {
                int y = this.cells.get(i).getBlockPos().getY();
                int end = i;
                while (end < this.cells.size() && this.cells.get(end).getBlockPos().getY() == y) {
                    end++;
                }
                int count = end - i;
                long layer = Math.min(left, count * CELL_CAPACITY);
                left -= layer;
                long each = layer / count;
                long odd = layer % count;
                for (int k = i; k < end; k++) {
                    long share = each + (k - i < odd ? 1 : 0);
                    this.cells.get(k).setContents(this.fluid, share);
                }
                i = end;
            }
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) {
                return 0;
            }
            if (!this.fluid.isBlank() && !this.fluid.equals(resource)) {
                return 0;
            }
            long taken = Math.min(maxAmount, this.capacity - this.amount);
            if (taken <= 0) {
                return 0;
            }
            this.updateSnapshots(transaction);
            this.fluid = resource;
            this.amount += taken;
            return taken;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0 || !resource.equals(this.fluid)) {
                return 0;
            }
            long given = Math.min(maxAmount, this.amount);
            if (given <= 0) {
                return 0;
            }
            this.updateSnapshots(transaction);
            this.amount -= given;
            if (this.amount <= 0) {
                this.fluid = FluidVariant.blank();
            }
            return given;
        }

        @Override
        public boolean isResourceBlank() {
            return this.fluid.isBlank();
        }

        @Override
        public FluidVariant getResource() {
            return this.fluid;
        }

        @Override
        public long getAmount() {
            return this.amount;
        }

        @Override
        public long getCapacity() {
            return this.capacity;
        }

        @Override
        protected ResourceAmount<FluidVariant> createSnapshot() {
            return new ResourceAmount<>(this.fluid, this.amount);
        }

        @Override
        protected void readSnapshot(ResourceAmount<FluidVariant> snapshot) {
            this.fluid = snapshot.resource();
            this.amount = snapshot.amount();
        }

        @Override
        protected void onFinalCommit() {
            this.settle();
        }
    }
}
