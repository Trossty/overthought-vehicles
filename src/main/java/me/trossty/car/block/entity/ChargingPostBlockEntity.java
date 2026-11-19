package me.trossty.car.block.entity;

import me.trossty.car.block.ChargingPostBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.machine.GaugeSource;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.energy.IEnergyStorage;
import me.trossty.car.energy.EnergyBuffer;

/**
 * Takes power off a cable into a small buffer and pours it into the electric
 * vehicle its plug is in. The plug comes off its holster in a bare hand and
 * goes into a car's charge port; the lead is drawn from the post to the hand
 * or the car, and pulled too far it comes back. A full charge of a city car is
 * a few minutes on a coal generator, under a minute off a full battery bank.
 */
public class ChargingPostBlockEntity extends BlockEntity implements GaugeSource {

    public static final long CAPACITY = 20_000;
    /** Most a cable can push in a tick. */
    public static final long INPUT = 400;
    /** Most it puts into a car a tick. */
    public static final long OUTPUT = 200;
    /** How far its lead reaches, from the holster. */
    public static final double REACH = 5.0;

    /** Posts the client has loaded, so their leads can be drawn. */
    public static final java.util.Set<ChargingPostBlockEntity> CLIENT_LOADED =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, INPUT, 0) {
        @Override
        protected void onChanged() {
            ChargingPostBlockEntity.this.setChanged();
        }
    };

    /** Who has the plug in hand. Not saved: after a reload the plug is back on its holster. */
    private java.util.@Nullable UUID plugHolder;
    /** The car the plug is in. */
    private java.util.@Nullable UUID pluggedCar;
    /** The same car's network id, for the client to draw the lead to; -1 for none. */
    private int pluggedCarId = -1;

    private boolean carPresent;
    private int carChargePermille;
    private int lastRate;

    public ChargingPostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_POST, pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energy;
    }

    public long getEnergy() {
        return this.energy.amount;
    }

    public void setEnergy(long amount) {
        this.energy.amount = Math.max(0, Math.min(CAPACITY, amount));
        this.setChanged();
    }

    /** Where the lead leaves the post: the holster on its side, in the world. */
    public static net.minecraft.world.phys.Vec3 holster(BlockPos pos, net.minecraft.core.Direction facing) {
        // In the model, which faces north: the holster at x 12.3, z 8.3, the plug's top 15 up.
        double x = 12.3 / 16.0;
        double z = 8.3 / 16.0;
        double rx;
        double rz;
        switch (facing) {
            case EAST -> { rx = 1.0 - z; rz = x; }
            case SOUTH -> { rx = 1.0 - x; rz = 1.0 - z; }
            case WEST -> { rx = z; rz = 1.0 - x; }
            default -> { rx = x; rz = z; }
        }
        return new net.minecraft.world.phys.Vec3(pos.getX() + rx, pos.getY() + 15.0 / 16.0, pos.getZ() + rz);
    }

    public net.minecraft.world.phys.Vec3 holster() {
        return holster(this.worldPosition, this.getBlockState().getValue(ChargingPostBlock.FACING));
    }

    // ---- the plug ----

    public boolean isPlugOut() {
        return this.plugHolder != null || this.pluggedCar != null;
    }

    public boolean isHeldBy(net.minecraft.world.entity.Entity entity) {
        return entity.getUUID().equals(this.plugHolder);
    }

    /** The car the plug is in, on the side this runs on; null when there is none. */
    public @Nullable CarEntity pluggedCar() {
        if (this.level == null) {
            return null;
        }
        if (this.level instanceof net.minecraft.server.level.ServerLevel server) {
            return this.pluggedCar != null && server.getEntity(this.pluggedCar) instanceof CarEntity car ? car : null;
        }
        return this.pluggedCarId >= 0 && this.level.getEntity(this.pluggedCarId) instanceof CarEntity car ? car : null;
    }

    /** Puts the plug in the player's empty hand: off the holster, or out of the car it was in. */
    public boolean takePlug(net.minecraft.world.entity.player.Player player) {
        if (this.plugHolder != null && !this.isHeldBy(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.overthought_vehicles.plug_in_use"), true);
            return false;
        }
        this.pluggedCar = null;
        this.pluggedCarId = -1;
        this.plugHolder = player.getUUID();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                me.trossty.car.item.ChargingPlugItem.forPost(this.worldPosition));
        this.level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.6F);
        this.changedForClients();
        return true;
    }

    /** Pushes the plug from the hand into a car's charge port. */
    public boolean plugInto(CarEntity car, net.minecraft.world.entity.player.@Nullable Player from) {
        if (!car.isElectric()) {
            if (from != null) {
                from.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.overthought_vehicles.not_electric"), true);
            }
            return false;
        }
        net.minecraft.world.phys.Vec3 port = car.fillerPos();
        if (port == null || port.distanceToSqr(this.holster()) > REACH * REACH) {
            if (from != null) {
                from.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.overthought_vehicles.lead_too_short"), true);
            }
            return false;
        }
        if (from != null) {
            this.takeOutOf(from);
        }
        this.plugHolder = null;
        this.pluggedCar = car.getUUID();
        this.pluggedCarId = car.getId();
        this.level.playSound(null, port.x, port.y, port.z, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.8F);
        this.changedForClients();
        return true;
    }

    /**
     * Back on the holster, out of whoever had it.
     *
     * @param snapped the lead ran out rather than being put back properly
     */
    public void hangUpPlug(boolean snapped) {
        if (this.level == null) {
            return;
        }
        net.minecraft.world.entity.player.Player holder = this.plugHolder == null
                ? null : this.level.getPlayerByUUID(this.plugHolder);
        if (holder != null) {
            this.takeOutOf(holder);
        }
        this.plugHolder = null;
        this.pluggedCar = null;
        this.pluggedCarId = -1;
        this.level.playSound(null, this.worldPosition,
                snapped ? net.minecraft.sounds.SoundEvents.LEASH_KNOT_BREAK : net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, snapped ? 0.9F : 1.6F);
        this.changedForClients();
    }

    private void takeOutOf(net.minecraft.world.entity.player.Player holder) {
        net.minecraft.world.entity.player.Inventory inventory = holder.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (me.trossty.car.item.ChargingPlugItem.isFrom(inventory.getItem(slot), this.worldPosition)) {
                inventory.setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        for (net.minecraft.world.entity.item.ItemEntity dropped : this.level.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, holder.getBoundingBox().inflate(REACH + 4.0),
                item -> me.trossty.car.item.ChargingPlugItem.isFrom(item.getItem(), this.worldPosition))) {
            dropped.discard();
        }
    }

    /** The plug stays out only in the taker's hand or in a car, and only within the lead's reach. */
    private void watchPlug(Level world) {
        net.minecraft.world.phys.Vec3 holster = this.holster();
        if (this.plugHolder != null) {
            net.minecraft.world.entity.player.Player holder = world.getPlayerByUUID(this.plugHolder);
            if (holder == null || !holder.isAlive() || holder.isSpectator()
                    || !(me.trossty.car.item.ChargingPlugItem.isFrom(holder.getMainHandItem(), this.worldPosition)
                            || me.trossty.car.item.ChargingPlugItem.isFrom(holder.getOffhandItem(), this.worldPosition))) {
                this.hangUpPlug(false);
            } else if (holder.position().add(0.0, 0.9, 0.0).distanceToSqr(holster) > REACH * REACH) {
                this.hangUpPlug(true);
            }
            return;
        }
        if (this.pluggedCar != null) {
            CarEntity car = this.pluggedCar();
            net.minecraft.world.phys.Vec3 port = car == null ? null : car.fillerPos();
            if (car == null || !car.isAlive() || port == null) {
                this.hangUpPlug(false);
            } else if (port.distanceToSqr(holster) > REACH * REACH) {
                this.hangUpPlug(true);
            } else if (car.getId() != this.pluggedCarId) {
                this.pluggedCarId = car.getId();
                this.changedForClients();
            }
        }
    }

    private void changedForClients() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void tick(Level world, BlockPos pos, BlockState state, ChargingPostBlockEntity post) {
        post.watchPlug(world);
        CarEntity car = post.pluggedCar();
        int rate = 0;
        if (car != null) {
            float room = car.getMaxFuel() - car.getFuel();
            long wanted = (long) Math.ceil(room * CarEntity.ENERGY_PER_CHARGE_UNIT);
            long moved = Math.min(OUTPUT, Math.min(post.energy.amount, wanted));
            if (moved > 0) {
                // Only what the battery took leaves the post; a cell already
                // near full takes less than was offered.
                float taken = car.charge(moved / (float) CarEntity.ENERGY_PER_CHARGE_UNIT);
                moved = Math.min(moved, (long) Math.ceil(taken * CarEntity.ENERGY_PER_CHARGE_UNIT));
                post.energy.amount -= moved;
                post.setChanged();
                rate = (int) moved;
            }
            float max = car.getMaxFuel();
            post.carChargePermille = max <= 0 ? 0 : Math.round(car.getFuel() * 1000 / max);
        } else {
            post.carChargePermille = 0;
        }
        post.carPresent = car != null;
        post.lastRate = rate;

        boolean lit = rate > 0;
        if (state.getValue(ChargingPostBlock.LIT) != lit) {
            world.setBlock(pos, state.setValue(ChargingPostBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide()) {
            CLIENT_LOADED.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_LOADED.remove(this);
    }

    @Override
    public net.minecraft.network.protocol.@Nullable Packet<
            net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = this.saveWithoutMetadata(registries);
        tag.putInt("PluggedId", this.pluggedCarId);
        return tag;
    }

    // ---- the gauge window ----

    @Override
    public int gaugeKind() {
        return KIND_CHARGER;
    }

    @Override
    public long gaugeEnergy() {
        return this.energy.amount;
    }

    @Override
    public long gaugeEnergyCapacity() {
        return CAPACITY;
    }

    /** 0 with no car beside it, else 1 plus what it is putting in a tick. */
    @Override
    public int gaugeExtra() {
        return this.carPresent ? 1 + this.lastRate : 0;
    }

    /** The parked car's charge, in tenths of a percent. */
    @Override
    public int gaugeExtra2() {
        return this.carChargePermille;
    }

    @Override
    public boolean gaugeWorking() {
        return this.lastRate > 0;
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.energy.amount = Math.max(0, Math.min(CAPACITY, view.getLongOr("Energy", 0L)));
        this.pluggedCar = tag.hasUUID("Plugged") ? tag.getUUID("Plugged") : null;
        this.pluggedCarId = tag.contains("PluggedId") ? tag.getInt("PluggedId") : -1;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        view.putLong("Energy", this.energy.amount);
        if (this.pluggedCar != null) {
            tag.putUUID("Plugged", this.pluggedCar);
        }
    }
}
