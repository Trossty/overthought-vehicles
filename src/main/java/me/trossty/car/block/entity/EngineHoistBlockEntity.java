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

/**
 * The engine hanging on a hoist's hook, kept whole with its wear, grade and
 * kind, and shown to every client. Empty while the hook is.
 */
public class EngineHoistBlockEntity extends BlockEntity {

    private ItemStack engine = ItemStack.EMPTY;

    public EngineHoistBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENGINE_HOIST, pos, state);
    }

    public ItemStack getEngine() {
        return this.engine;
    }

    public boolean isHolding() {
        return !this.engine.isEmpty();
    }

    public void setEngine(ItemStack engine) {
        this.engine = engine;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    /** Takes the engine off the hook and hands it back. */
    public ItemStack takeEngine() {
        ItemStack engine = this.engine;
        this.setEngine(ItemStack.EMPTY);
        return engine;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.engine = tag.contains("Engine") ? ItemStack.parseOptional(registries, tag.getCompound("Engine"))
                : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.engine.isEmpty()) {
            tag.put("Engine", this.engine.save(registries));
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
}
