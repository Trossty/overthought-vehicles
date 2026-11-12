package me.trossty.car.block.entity;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * The forecourt tank.
 *
 * <p>A pump holds petrol and nothing else — no slots, no window. It fills itself
 * from any refinery it is touching, so a refinery with a pump beside it becomes
 * a filling station on its own, and it empties straight into a car parked at it.
 * The jerry can stays useful for carrying fuel away from the station; it is no
 * longer the only way to move it.
 *
 * <p>The tank lives in the lower half of the block. The upper half is scenery
 * and forwards everything down, which is why nothing here knows about halves.
 */
public class FuelPumpBlockEntity extends BlockEntity {

    public static final int CAPACITY = 2000;

    /**
     * Petrol one unit of a car's tank costs. The refinery's pour is 250 petrol
     * and a pour is {@link CarEntity#JERRY_CAN_UNITS} of tank, so this keeps one
     * exchange rate across the whole mod rather than inventing a second.
     */
    public static final int PETROL_PER_FUEL_UNIT =
            FuelRefineryBlockEntity.PETROL_PER_POUR / (int) CarEntity.JERRY_CAN_UNITS;

    /** Petrol drawn from a touching refinery each tick, when there is room. */
    private static final int DRAW_RATE = 4;

    private int petrol;

    /** What the clients last heard. */
    private int sentPetrol = -1;

    @Override
    public net.minecraft.network.packet.@org.jspecify.annotations.Nullable Packet<
            net.minecraft.network.listener.ClientPlayPacketListener> toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.NbtCompound toInitialChunkDataNbt(net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        return this.createNbt(registries);
    }

    /** This holder as other mods' pipes see it. */
    private final me.trossty.car.fluid.PetrolSlot fluidStorage = new me.trossty.car.fluid.PetrolSlot(
            () -> this.petrol, value -> this.petrol = value, CAPACITY, true, false, this::markDirty);

    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> getFluidStorage() {
        return this.fluidStorage;
    }

    public FuelPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_PUMP, pos, state);
    }

    public int getPetrol() {
        return this.petrol;
    }

    public int getCapacity() {
        return CAPACITY;
    }

    public void setPetrol(int petrol) {
        this.petrol = Math.max(0, Math.min(CAPACITY, petrol));
        this.markDirty();
    }

    /** Takes what it can, up to {@code amount}, and reports how much went in. */
    public int fill(int amount) {
        int accepted = Math.min(amount, CAPACITY - this.petrol);
        if (accepted > 0) {
            this.petrol += accepted;
            this.markDirty();
        }
        return accepted;
    }

    /** Gives out what it can, up to {@code amount}, and reports how much came out. */
    public int drain(int amount) {
        int given = Math.min(amount, this.petrol);
        if (given > 0) {
            this.petrol -= given;
            this.markDirty();
        }
        return given;
    }

    /**
     * Pours into a car's tank and reports the petrol spent. Works in whole fuel
     * units so a car never ends up holding a fraction the gauge cannot show.
     */
    public int refuel(CarEntity car) {
        return this.refuel(car, Integer.MAX_VALUE);
    }

    /** The same, but no more than {@code most} units: one tick of the nozzle's flow. */
    public int refuel(CarEntity car, int most) {
        float room = car.getMaxFuel() - car.getFuel();
        int affordable = Math.min(Math.min((int) room, most), this.petrol / PETROL_PER_FUEL_UNIT);
        if (affordable <= 0) {
            return 0;
        }
        float accepted = car.addFuel(affordable);
        int spent = Math.round(accepted) * PETROL_PER_FUEL_UNIT;
        this.drain(spent);
        return spent;
    }

    public static void tick(World world, BlockPos pos, BlockState state, FuelPumpBlockEntity pump) {
        pump.watchNozzle(world, pos, state);
        // The sight window and the display show what is in it, so the
        // clients are told — a few times a second, not every tick.
        if (pump.petrol != pump.sentPetrol && world.getTime() % 5 == 0) {
            pump.sentPetrol = pump.petrol;
            world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_LISTENERS);
        }
        if (pump.petrol >= CAPACITY) {
            return;
        }
        // Siphons from whatever refineries it is up against, in whatever
        // arrangement: the point is that a station is something you build out of
        // the blocks rather than something you carry fuel to.
        for (Direction side : Direction.values()) {
            if (me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos.offset(side)) instanceof me.trossty.car.fluid.PetrolSource refinery) {
                int taken = refinery.drainPetrol(Math.min(DRAW_RATE, CAPACITY - pump.petrol));
                if (taken > 0) {
                    pump.fill(taken);
                    if (pump.petrol >= CAPACITY) {
                        return;
                    }
                }
            }
        }
    }

    // ---- the nozzle ----

    /** How far the hose stretches from the pump to the hand, in blocks. */
    public static final double HOSE_LENGTH = 6.0;

    /**
     * Who has the nozzle off its hook. Not saved: after a reload nobody is
     * holding anything, the stray nozzle item cleans itself up (see
     * FuelNozzleItem#inventoryTick) and the nozzle is back on the pump.
     */
    private java.util.@org.jspecify.annotations.Nullable UUID nozzleHolder;

    public boolean isHeldBy(net.minecraft.entity.Entity entity) {
        return entity.getUuid().equals(this.nozzleHolder);
    }

    /** Puts the nozzle in the player's empty hand. False if someone else has it. */
    public boolean takeNozzle(net.minecraft.entity.player.PlayerEntity player) {
        if (this.nozzleHolder != null && !this.isHeldBy(player)) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.car.nozzle_in_use"), true);
            return false;
        }
        this.nozzleHolder = player.getUuid();
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,
                me.trossty.car.item.FuelNozzleItem.forPump(this.pos));
        me.trossty.car.block.FuelPumpBlock.setNozzleHung(this.world, this.pos, false);
        this.world.playSound(null, this.pos.up(), net.minecraft.sound.SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN,
                net.minecraft.sound.SoundCategory.BLOCKS, 0.5F, 1.4F);
        return true;
    }

    /**
     * Hangs the nozzle back on its hook, out of whoever had it: their hands,
     * their pockets, and the ground at their feet if they dropped it.
     *
     * @param snapped the hose ran out rather than being hung up properly
     */
    public void hangUpNozzle(boolean snapped) {
        if (this.world == null) {
            return;
        }
        net.minecraft.entity.player.PlayerEntity holder = this.nozzleHolder == null
                ? null : this.world.getPlayerByUuid(this.nozzleHolder);
        this.nozzleHolder = null;
        if (holder != null) {
            net.minecraft.entity.player.PlayerInventory inventory = holder.getInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                if (me.trossty.car.item.FuelNozzleItem.isFrom(inventory.getStack(slot), this.pos)) {
                    inventory.setStack(slot, net.minecraft.item.ItemStack.EMPTY);
                }
            }
            if (holder.isUsingItem() && holder.getActiveItem().isOf(me.trossty.car.registry.ModItems.FUEL_NOZZLE)) {
                holder.stopUsingItem();
            }
            for (net.minecraft.entity.ItemEntity dropped : this.world.getEntitiesByClass(
                    net.minecraft.entity.ItemEntity.class, holder.getBoundingBox().expand(HOSE_LENGTH + 4.0),
                    item -> me.trossty.car.item.FuelNozzleItem.isFrom(item.getStack(), this.pos))) {
                dropped.discard();
            }
        }
        me.trossty.car.block.FuelPumpBlock.setNozzleHung(this.world, this.pos, true);
        this.world.playSound(null, this.pos.up(),
                snapped ? net.minecraft.sound.SoundEvents.ITEM_LEAD_BREAK
                        : net.minecraft.sound.SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,
                net.minecraft.sound.SoundCategory.BLOCKS, 0.6F, snapped ? 0.9F : 1.4F);
    }

    /**
     * Keeps the nozzle honest. It stays out only while the one who took it
     * has it in a hand and is within the hose's length; anything else, and
     * it is back on the hook.
     */
    private void watchNozzle(World world, BlockPos pos, BlockState state) {
        if (this.nozzleHolder == null) {
            if (!state.get(me.trossty.car.block.FuelPumpBlock.NOZZLE)) {
                me.trossty.car.block.FuelPumpBlock.setNozzleHung(world, pos, true);
            }
            return;
        }
        net.minecraft.entity.player.PlayerEntity holder = world.getPlayerByUuid(this.nozzleHolder);
        if (holder == null || !holder.isAlive() || holder.isSpectator()
                || !(me.trossty.car.item.FuelNozzleItem.isFrom(holder.getMainHandStack(), pos)
                        || me.trossty.car.item.FuelNozzleItem.isFrom(holder.getOffHandStack(), pos))) {
            this.hangUpNozzle(false);
            return;
        }
        net.minecraft.util.math.Vec3d outlet = me.trossty.car.block.FuelPumpBlock.hoseOutlet(
                pos, state.get(me.trossty.car.block.FuelPumpBlock.FACING));
        if (holder.getEntityPos().add(0.0, 0.9, 0.0).squaredDistanceTo(outlet) > HOSE_LENGTH * HOSE_LENGTH) {
            this.hangUpNozzle(true);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.petrol = view.getInt("Petrol", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("Petrol", this.petrol);
    }
}
