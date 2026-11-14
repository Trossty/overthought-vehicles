package me.trossty.car.block.entity;

import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModFluids;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import org.jetbrains.annotations.Nullable;

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
    public static final long CELL_CAPACITY = 16 * 1000L;

    /** The old measure, in this mod's units (millibuckets): one cell. */
    public static final int CAPACITY = (int) (CELL_CAPACITY / ModFluids.UNIT);

    /** No tank is grown past this many cells by looking for its neighbours. */
    private static final int MAX_CELLS = 512;

    /** What a cell gives a touching block, or takes from it, a tick: what a pipe connection moves. */
    private static final int RATE = me.trossty.car.network.FluidNetwork.RATE;

    private Fluid fluid = Fluids.EMPTY;
    private long amount;
    /** The liquid a wrench has locked the tank to; every cell keeps a copy. */
    private Fluid lock = Fluids.EMPTY;

    /** What the clients were last told, so they are not told it every tick. */
    private Fluid sentFluid = Fluids.EMPTY;
    private long sentAmount = -1;

    private @Nullable Group group;

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_TANK, pos, state);
    }

    /**
     * A cell was put down or taken away at {@code pos}: the tanks it touches
     * are a different shape now, and are worked out again when next asked.
     * Nothing further away is touched.
     */
    public static void reshapeAround(Level world, BlockPos pos) {
        forgetGroupAt(world, pos);
        for (Direction side : Direction.values()) {
            forgetGroupAt(world, pos.relative(side));
        }
    }

    private static void forgetGroupAt(Level world, BlockPos pos) {
        // CHECK, not the usual lookup: this runs while a cell is being set up,
        // and the usual one would make it again, and again.
        if (world.isLoaded(pos) && world.getChunkAt(pos).getBlockEntity(pos,
                net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) instanceof FuelTankBlockEntity cell
                && cell.group != null) {
            cell.group.invalidate();
        }
    }

    // ---- this cell ----

    public Fluid getFluid() {
        return this.fluid;
    }

    /** What this cell holds, in millibuckets. */
    public long getAmount() {
        return this.amount;
    }

    /** How full this cell is, nought to one: how high its liquid is drawn. */
    public float getFill() {
        return Math.min(1.0F, (float) this.amount / CELL_CAPACITY);
    }

    private void setContents(Fluid fluid, long amount) {
        Fluid now = amount <= 0 ? Fluids.EMPTY : fluid;
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
        if (this.group == null || !this.group.valid || this.isRemoved()) {
            this.group = Group.build(this);
        }
        return this.group;
    }

    /** Every face of every cell is a door into the whole tank. */
    public IFluidHandler getFluidStorage() {
        return this.getGroup();
    }

    /** The tank's petrol in units, or nought if it holds something else. */
    public int getPetrol() {
        Group tank = this.getGroup();
        return tank.fluid == ModFluids.PETROL ? (int) (tank.amount / ModFluids.UNIT) : 0;
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

    public int fill(Fluid liquid, int units) {
        if (units <= 0) {
            return 0;
        }
        return this.getGroup().fill(new FluidStack(liquid, units), IFluidHandler.FluidAction.EXECUTE);
    }

    public int drain(Fluid liquid, int units) {
        if (units <= 0) {
            return 0;
        }
        return this.getGroup().drain(new FluidStack(liquid, units), IFluidHandler.FluidAction.EXECUTE).getAmount();
    }

    /** The whole tank's fill, nought to fifteen, as a comparator reads it. */
    public int comparatorOutput() {
        Group tank = this.getGroup();
        return me.trossty.car.block.LiquidStores.comparatorOutput(tank.amount, tank.capacity);
    }

    // ---- ticking ----

    public static void tick(Level world, BlockPos pos, BlockState state, FuelTankBlockEntity tank) {
        // What the tank touches directly is served by the same rules as a
        // pipe line: it draws off a machine's output, and feeds anything that
        // only takes (a pump, a generator, the crude side of a refinery).
        // Another store is left alone, and a pipe or valve does its own work.
        Group whole = tank.getGroup();
        for (Direction side : Direction.values()) {
            if (state.getValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
                continue;
            }
            BlockPos next = pos.relative(side);
            Block block = world.getBlockState(next).getBlock();
            if (block instanceof me.trossty.car.block.FuelPipeBlock || block instanceof me.trossty.car.block.FuelValveBlock) {
                continue;
            }
            IFluidHandler other = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    next, side.getOpposite());
            if (other == null) {
                continue;
            }
            boolean maker = me.trossty.car.fluid.FluidSource.at(world, next);
            if (!maker && me.trossty.car.network.FluidNetwork.isStore(world.getBlockEntity(next), other)) {
                continue;
            }
            if (maker) {
                me.trossty.car.fluid.Liquids.moveAny(other, whole, RATE);
            }
            if (!whole.isResourceBlank()) {
                me.trossty.car.fluid.Liquids.move(whole, other, whole.getResource(), RATE);
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
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.fluid = view.read("Fluid", me.trossty.car.fluid.Liquids.CODEC).orElse(Fluids.EMPTY);
        this.amount = view.getLongOr("Amount", 0L);
        // A tank from before it could hold anything but petrol.
        int petrol = view.getIntOr("Petrol", 0);
        if (this.amount <= 0 && petrol > 0) {
            this.fluid = ModFluids.PETROL;
            this.amount = petrol * ModFluids.UNIT;
        }
        if (this.amount <= 0) {
            this.fluid = Fluids.EMPTY;
            this.amount = 0;
        }
        this.lock = view.read("Lock", me.trossty.car.fluid.Liquids.CODEC).orElse(Fluids.EMPTY);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        if (this.fluid != Fluids.EMPTY && this.amount > 0) {
            view.store("Fluid", me.trossty.car.fluid.Liquids.CODEC, this.fluid);
            view.putLong("Amount", this.amount);
        }
        if (this.lock != Fluids.EMPTY) {
            view.store("Lock", me.trossty.car.fluid.Liquids.CODEC, this.lock);
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
        if (this.group != null) {
            this.group.invalidate();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.group != null) {
            this.group.invalidate();
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null) {
            reshapeAround(this.level, this.worldPosition);
        }
    }

    // ---- the lock ----

    public Fluid getLock() {
        return this.getGroup().lock;
    }

    /** A wrench on any cell locks or frees the whole tank. */
    public void toggleLock(net.minecraft.world.entity.player.Player player) {
        Group whole = this.getGroup();
        Fluid now = me.trossty.car.block.LiquidStores.toggleLock(this.level, this.worldPosition, player,
                whole.lock, whole.fluid);
        whole.setLock(now);
    }

    // ---- travelling in the item ----

    /** A cell put down from an item that carries liquid pours it back in. */
    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        me.trossty.car.registry.ModComponents.StoredFluid held = components.get(me.trossty.car.registry.ModComponents.STORED_FLUID);
        if (held != null) {
            this.setContents(held.fluid(), Math.min(held.amount(), CELL_CAPACITY));
            // The tank it joined may already have been worked out without this cell's share.
            if (this.group != null) {
                this.group.invalidate();
            }
        }
    }

    /** A cell broken out of a tank takes the share that was in it, not lose it. */
    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (this.fluid != Fluids.EMPTY && this.amount > 0) {
            builder.set(me.trossty.car.registry.ModComponents.STORED_FLUID,
                    new me.trossty.car.registry.ModComponents.StoredFluid(this.fluid, this.amount));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Fluid");
        tag.remove("Amount");
    }

    // ---- the group ----

    /**
     * All the cells of one tank, and what is in it. It is the one storage
     * every face of every cell hands out, so whichever door a pipe uses, it is
     * the same liquid it is moving.
     */
    public static final class Group implements IFluidHandler {

        private final List<FuelTankBlockEntity> cells;
        private final long capacity;
        private Fluid fluid;
        private long amount;
        private Fluid lock = Fluids.EMPTY;
        private boolean valid = true;

        private Group(List<FuelTankBlockEntity> cells, Fluid fluid, long amount) {
            this.cells = cells;
            this.capacity = cells.size() * CELL_CAPACITY;
            this.fluid = amount > 0 ? fluid : Fluids.EMPTY;
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
            Fluid liquid = Fluids.EMPTY;
            long most = 0;
            for (FuelTankBlockEntity cell : cells) {
                if (cell.fluid != Fluids.EMPTY && cell.amount > most) {
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
            Fluid lock = Fluids.EMPTY;
            for (FuelTankBlockEntity cell : cells) {
                cell.group = group;
                if (lock == Fluids.EMPTY) {
                    lock = cell.lock;
                }
            }
            // A cell added to a locked tank is locked with it.
            group.setLock(lock);
            group.settle();
            return group;
        }

        public int size() {
            return this.cells.size();
        }

        /** The cells' shape may have changed: they work the tank out again when next asked. */
        void invalidate() {
            if (!this.valid) {
                return;
            }
            this.valid = false;
            for (FuelTankBlockEntity cell : this.cells) {
                if (cell.group == this) {
                    cell.group = null;
                }
            }
        }

        public Fluid getLock() {
            return this.lock;
        }

        void setLock(Fluid lock) {
            this.lock = lock;
            for (FuelTankBlockEntity cell : this.cells) {
                if (cell.lock != lock) {
                    cell.lock = lock;
                    cell.setChanged();
                }
            }
        }

        private boolean takes(Fluid liquid) {
            return (this.lock == Fluids.EMPTY || liquid == this.lock)
                    && (this.isResourceBlank() || liquid == this.fluid);
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

        public Fluid getResource() {
            return this.fluid;
        }

        public boolean isResourceBlank() {
            return this.fluid == Fluids.EMPTY;
        }

        public long getAmount() {
            return this.amount;
        }

        public long getCapacity() {
            return this.capacity;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return this.isResourceBlank() || this.amount <= 0 ? FluidStack.EMPTY
                    : new FluidStack(this.fluid, (int) Math.min(Integer.MAX_VALUE, this.amount));
        }

        @Override
        public int getTankCapacity(int tank) {
            return (int) Math.min(Integer.MAX_VALUE, this.capacity);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return this.takes(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.isComponentsPatchEmpty()) {
                return 0;
            }
            if (!this.takes(resource.getFluid())) {
                return 0;
            }
            int taken = (int) Math.min(resource.getAmount(), this.capacity - this.amount);
            if (taken <= 0) {
                return 0;
            }
            if (action.execute()) {
                this.fluid = resource.getFluid();
                this.amount += taken;
                this.settle();
            }
            return taken;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.is(this.fluid)) {
                return FluidStack.EMPTY;
            }
            return this.drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            Fluid held = this.fluid;
            int given = (int) Math.min(maxDrain, this.amount);
            if (given <= 0 || held == Fluids.EMPTY) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                this.amount -= given;
                if (this.amount <= 0) {
                    this.fluid = Fluids.EMPTY;
                }
                this.settle();
            }
            return new FluidStack(held, given);
        }
    }
}
