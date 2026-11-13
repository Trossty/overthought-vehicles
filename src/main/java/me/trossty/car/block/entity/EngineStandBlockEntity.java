package me.trossty.car.block.entity;

import me.trossty.car.registry.ModBlockEntities;
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
     * With the engine opened up: its pistons, head gasket and crankshaft, in
     * that order, each with its own wear; an empty slot is one taken out.
     * Empty all through while the engine is whole.
     */
    private final net.minecraft.core.NonNullList<ItemStack> internals =
            net.minecraft.core.NonNullList.withSize(3, ItemStack.EMPTY);
    private boolean stripped;

    public boolean isStripped() {
        return this.stripped;
    }

    public net.minecraft.core.NonNullList<ItemStack> getInternals() {
        return this.internals;
    }

    /**
     * Opens the engine up. Its wear is shared out among the three insides,
     * not evenly — one of them is always the worst — but so they average to
     * what the engine was, and putting them back as they were gives the same
     * engine back.
     */
    public void strip(net.minecraft.util.RandomSource random) {
        float condition = me.trossty.car.entity.CarEntity.conditionOf(this.engine);
        net.minecraft.world.item.Item[] kinds = me.trossty.car.registry.ModItems.engineInternals();
        float[] share = new float[3];
        float spread = Math.min(condition - 1.0F, me.trossty.car.entity.CarPart.MAX_CONDITION - condition) * 0.6F;
        share[0] = condition + (random.nextFloat() * 2.0F - 1.0F) * spread;
        share[1] = condition + (random.nextFloat() * 2.0F - 1.0F) * spread;
        share[2] = 3.0F * condition - share[0] - share[1];
        for (int i = 0; i < 3; i++) {
            ItemStack inside = new ItemStack(kinds[i]);
            inside.set(me.trossty.car.registry.ModComponents.PART_CONDITION,
                    Math.round(net.minecraft.util.Mth.clamp(share[i], 1.0F, me.trossty.car.entity.CarPart.MAX_CONDITION)));
            this.internals.set(i, inside);
        }
        this.stripped = true;
        this.update();
    }

    /** Whether all three insides are in, so the engine can be closed up. */
    public boolean isComplete() {
        return this.internals.stream().noneMatch(ItemStack::isEmpty);
    }

    /** Closes the engine up again, as good as the average of what went into it. */
    public void rebuild() {
        float sum = 0.0F;
        for (ItemStack inside : this.internals) {
            sum += me.trossty.car.entity.CarEntity.conditionOf(inside);
        }
        this.engine.set(me.trossty.car.registry.ModComponents.PART_CONDITION, Math.max(1, Math.round(sum / 3.0F)));
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
        net.minecraft.world.ContainerHelper.loadAllItems(tag, this.internals, registries);
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
