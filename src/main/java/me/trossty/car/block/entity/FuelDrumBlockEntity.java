package me.trossty.car.block.entity;

import me.trossty.car.block.LiquidStores;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import me.trossty.car.fluid.UnitTank;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;

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
public class FuelDrumBlockEntity extends BlockEntity {

    public static final long CAPACITY = 8 * 1000L;

    private final UnitTank storage = new UnitTank((int) CAPACITY, fluid -> true, true, true, this::changed);

    /** The liquid the clients were last told about. */
    private Fluid shown = Fluids.EMPTY;

    public FuelDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_DRUM, pos, state);
    }

    public UnitTank getFluidStorage() {
        return this.storage;
    }

    public Fluid getFluid() {
        return this.storage.getFluid();
    }

    /** In millibuckets. */
    public long getAmount() {
        return this.storage.getUnits();
    }

    public int comparatorOutput() {
        return LiquidStores.comparatorOutput(this.storage.getUnits(), CAPACITY);
    }

    /** The liquid, for the block's colour. Read on the render thread, so a snapshot. */
    public Object getRenderData() {
        return this.storage.getUnits() > 0 ? this.storage.getFluid() : Fluids.EMPTY;
    }

    private void changed() {
        this.setChanged();
        Fluid now = (Fluid) this.getRenderData();
        if (!now.equals(this.shown) && this.level != null && !this.level.isClientSide()) {
            this.shown = now;
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    private void setContents(Fluid fluid, long amount) {
        this.storage.set(fluid, (int) Math.min(amount, CAPACITY));
    }

    // ---- saving, and travelling in the item ----

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.setContents(view.read("Fluid", me.trossty.car.fluid.Liquids.CODEC).orElse(Fluids.EMPTY),
                view.getLongOr("Amount", 0L));
        Fluid now = (Fluid) this.getRenderData();
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
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        if (!this.storage.isEmpty() && this.storage.getUnits() > 0) {
            view.store("Fluid", me.trossty.car.fluid.Liquids.CODEC, this.storage.getFluid());
            view.putLong("Amount", this.storage.getUnits());
        }
    }

    @Override
    public net.minecraft.network.protocol.@org.jetbrains.annotations.Nullable Packet<
            net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(
            net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
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
        if (!this.storage.isEmpty() && this.storage.getUnits() > 0) {
            builder.set(ModComponents.STORED_FLUID,
                    new ModComponents.StoredFluid(this.storage.getFluid(), this.storage.getUnits()));
        }
    }

    /** The liquid travels in the component; the item's copy of the block data leaves it out. */
    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag tag) {
        ValueOutput view = ValueOutput.of(tag, null);
        super.removeComponentsFromTag(tag);
        view.discard("Fluid");
        view.discard("Amount");
    }
}
