package me.trossty.car.block.entity;

import me.trossty.car.block.LiquidStores;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModComponents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A drum: one liquid, eight buckets of it, and nothing else going on. It does
 * not join up with its neighbours the way a yard tank does, and it does not
 * reach out to anything: a bucket, a jerry can or a pipe does the moving.
 *
 * <p>Its paint is the colour of what is in it. The client is only ever told
 * which liquid that is, and only when it changes, which is all the colour
 * needs; the chunk is drawn again then.
 *
 * <p>Broken, it keeps what was in it. The item carries the liquid in a
 * {@link ModComponents#STORED_FLUID} component, and putting it down again
 * pours it back in.
 */
public class FuelDrumBlockEntity extends BlockEntity
        implements net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity {

    public static final long CAPACITY = 8 * FluidConstants.BUCKET;

    private final SingleFluidStorage storage = SingleFluidStorage.withFixedCapacity(CAPACITY, this::changed);

    /** The liquid the clients were last told about. */
    private FluidVariant shown = FluidVariant.blank();

    public FuelDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_DRUM, pos, state);
    }

    public SingleSlotStorage<FluidVariant> getFluidStorage() {
        return this.storage;
    }

    public FluidVariant getFluid() {
        return this.storage.variant;
    }

    /** In droplets. */
    public long getAmount() {
        return this.storage.amount;
    }

    public int comparatorOutput() {
        return LiquidStores.comparatorOutput(this.storage.amount, CAPACITY);
    }

    /** The liquid, for the block's colour. Read on the render thread, so a snapshot. */
    @Override
    public Object getRenderData() {
        return this.storage.amount > 0 ? this.storage.variant : FluidVariant.blank();
    }

    private void changed() {
        this.setChanged();
        FluidVariant now = (FluidVariant) this.getRenderData();
        if (!now.equals(this.shown) && this.level != null && !this.level.isClientSide()) {
            this.shown = now;
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    private void setContents(FluidVariant fluid, long amount) {
        if (fluid.isBlank() || amount <= 0) {
            this.storage.variant = FluidVariant.blank();
            this.storage.amount = 0;
        } else {
            this.storage.variant = fluid;
            this.storage.amount = Math.min(amount, CAPACITY);
        }
    }

    // ---- saving, and travelling in the item ----

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.setContents(view.read("Fluid", FluidVariant.CODEC).orElse(FluidVariant.blank()),
                view.getLongOr("Amount", 0L));
        FluidVariant now = (FluidVariant) this.getRenderData();
        if (!now.equals(this.shown)) {
            this.shown = now;
            // The client has just been told of a new liquid: repaint.
            if (this.level != null && this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (!this.storage.isResourceBlank() && this.storage.amount > 0) {
            view.store("Fluid", FluidVariant.CODEC, this.storage.variant);
            view.putLong("Amount", this.storage.amount);
        }
    }

    @Override
    public net.minecraft.network.protocol.@org.jspecify.annotations.Nullable Packet<
            net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(
            net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        ModComponents.StoredFluid held = components.get(ModComponents.STORED_FLUID);
        if (held != null) {
            this.setContents(held.fluid(), held.amount());
            // Put down full: the clients need to know what colour it is.
            this.changed();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!this.storage.isResourceBlank() && this.storage.amount > 0) {
            builder.set(ModComponents.STORED_FLUID,
                    new ModComponents.StoredFluid(this.storage.variant, this.storage.amount));
        }
    }

    /** The liquid travels in the component; the item's copy of the block data leaves it out. */
    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard("Fluid");
        view.discard("Amount");
    }
}
