package me.trossty.car.block.entity;

import me.trossty.car.entity.EngineInternals.Snapshot;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** The engine on a stand, kept whole with its wear and quality, and shown to every client. */
public class EngineStandBlockEntity extends BlockEntity {

    private ItemStack engine = ItemStack.EMPTY;

    /**
     * With the engine opened up: its pistons, valves, head gasket and
     * crankshaft, in the order of {@link ModItems#engineInternals}, each with
     * its own wear; an empty slot is one taken out. Empty all through while
     * the engine is whole.
     */
    private final net.minecraft.core.NonNullList<ItemStack> internals =
            net.minecraft.core.NonNullList.withSize(ModItems.engineInternals().length, ItemStack.EMPTY);
    private boolean stripped;

    public boolean isStripped() {
        return this.stripped;
    }

    public net.minecraft.core.NonNullList<ItemStack> getInternals() {
        return this.internals;
    }

    /**
     * Opens the engine up: out come its insides as they are, the pistons and
     * valves with each cylinder's own wear on them. Putting them back as they
     * were gives the same engine back.
     */
    public void strip() {
        Snapshot inside = Snapshot.of(this.engine);
        this.internals.set(PISTONS, set(ModItems.PISTONS, inside.rings()));
        this.internals.set(VALVES, set(ModItems.VALVES, inside.valves()));
        this.internals.set(GASKET, worn(new ItemStack(ModItems.HEAD_GASKET), inside.gasket()));
        this.internals.set(CRANKSHAFT, worn(new ItemStack(ModItems.CRANKSHAFT), inside.bearings()));
        this.stripped = true;
        this.update();
    }

    private static final int PISTONS = 0;
    private static final int VALVES = 1;
    private static final int GASKET = 2;
    private static final int CRANKSHAFT = 3;

    /** A set of pistons or valves, carrying each cylinder's wear unless they are all alike. */
    private static ItemStack set(net.minecraft.world.item.Item kind, java.util.List<Integer> cylinders) {
        ItemStack set = worn(new ItemStack(kind), Math.round(Snapshot.mean(cylinders)));
        if (cylinders.stream().distinct().count() > 1) {
            set.set(ModComponents.PART_CYLINDERS, cylinders);
        }
        return set;
    }

    private static ItemStack worn(ItemStack part, int condition) {
        if (condition < me.trossty.car.entity.CarPart.MAX_CONDITION) {
            part.set(ModComponents.PART_CONDITION, condition);
        }
        return part;
    }

    /** One set's wear for this engine, cylinder by cylinder: what it carries, or its condition all through. */
    private static java.util.List<Integer> cylindersOf(ItemStack set, int cylinders) {
        java.util.List<Integer> kept = set.get(ModComponents.PART_CYLINDERS);
        return kept != null ? Snapshot.resize(kept, cylinders)
                : java.util.Collections.nCopies(cylinders, Math.round(me.trossty.car.entity.Workshop.conditionOf(set)));
    }

    /** Whether all four insides are in, so the engine can be closed up. */
    public boolean isComplete() {
        return this.internals.stream().noneMatch(ItemStack::isEmpty);
    }

    /** Closes the engine up again with what went into it: it is as good as they are, cylinder by cylinder. */
    public void rebuild() {
        int cylinders = me.trossty.car.entity.CarSetup.layoutOf(me.trossty.car.entity.CarEntity.variantOf(this.engine)).cylinders();
        Snapshot inside = new Snapshot(
                cylindersOf(this.internals.get(PISTONS), cylinders),
                cylindersOf(this.internals.get(VALVES), cylinders),
                Math.round(me.trossty.car.entity.Workshop.conditionOf(this.internals.get(GASKET))),
                Math.round(me.trossty.car.entity.Workshop.conditionOf(this.internals.get(CRANKSHAFT))));
        int condition = Math.max(1, Math.round(inside.condition()));
        if (condition < me.trossty.car.entity.CarPart.MAX_CONDITION) {
            this.engine.set(ModComponents.PART_CONDITION, condition);
        } else {
            this.engine.remove(ModComponents.PART_CONDITION);
        }
        inside.stamp(this.engine);
        java.util.Collections.fill(this.internals, ItemStack.EMPTY);
        this.stripped = false;
        this.update();
    }

    private void update() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    public EngineStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENGINE_STAND, pos, state);
    }

    public ItemStack getEngine() {
        return this.engine;
    }

    public void setEngine(ItemStack engine) {
        this.engine = engine;
        this.update();
    }

    /** Puts one of the insides back, in its own slot. */
    public void setInternal(int slot, ItemStack inside) {
        this.internals.set(slot, inside);
        this.update();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.engine = tag.contains("Engine") ? ItemStack.parseOptional(registries, tag.getCompound("Engine"))
                : ItemStack.EMPTY;
        this.stripped = tag.getBoolean("Stripped");
        java.util.Collections.fill(this.internals, ItemStack.EMPTY);
        // Each back in its own kind's slot, whatever slot it was saved in.
        net.minecraft.core.NonNullList<ItemStack> saved =
                net.minecraft.core.NonNullList.withSize(this.internals.size(), ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, saved, registries);
        for (ItemStack inside : saved) {
            int slot = java.util.Arrays.asList(ModItems.engineInternals()).indexOf(inside.getItem());
            if (slot >= 0) {
                this.internals.set(slot, inside);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.engine.isEmpty()) {
            tag.put("Engine", this.engine.save(registries));
        }
        if (this.stripped) {
            tag.putBoolean("Stripped", true);
        }
        net.minecraft.world.ContainerHelper.saveAllItems(tag, this.internals, true, registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
