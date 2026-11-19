package me.trossty.car.block.entity;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;

/**
 * The forecourt tank.
 *
 * <p>A pump holds petrol and nothing else — no slots, no window. It fills itself
 * from any hydrotreater it is touching — the stage that turns the refinery's
 * crude into petrol — so a hydrotreater with a pump beside it is a filling
 * station on its own, and it empties straight into a car parked at it.
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
    public net.minecraft.network.protocol.@org.jetbrains.annotations.Nullable Packet<
            net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    /**
     * What the pump holds: petrol, diesel or biofuel, one at a time. Meaningless
     * while it is empty, when the next thing in decides it.
     */
    private CarSetup.Fuel fuel = CarSetup.Fuel.PETROL;

    /**
     * This holder as pipes see it: it takes any clean fuel a car runs on, one
     * at a time, and gives nothing back through the pipe — fuel leaves by the
     * nozzle, a can or a tanker's hose.
     */
    private final net.neoforged.neoforge.fluids.capability.IFluidHandler fluidStorage =
            new net.neoforged.neoforge.fluids.capability.IFluidHandler() {
                @Override
                public int getTanks() {
                    return 1;
                }

                @Override
                public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
                    return FuelPumpBlockEntity.this.petrol <= 0 ? net.neoforged.neoforge.fluids.FluidStack.EMPTY
                            : new net.neoforged.neoforge.fluids.FluidStack(ModFluids.fluidOf(FuelPumpBlockEntity.this.fuel),
                                    FuelPumpBlockEntity.this.petrol);
                }

                @Override
                public int getTankCapacity(int tank) {
                    return CAPACITY;
                }

                @Override
                public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
                    return FuelPumpBlockEntity.this.takes(ModFluids.fuelOf(stack.getFluid()));
                }

                @Override
                public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
                    CarSetup.Fuel kind = ModFluids.fuelOf(resource.getFluid());
                    if (!FuelPumpBlockEntity.this.takes(kind)) {
                        return 0;
                    }
                    int units = Math.min(resource.getAmount(), CAPACITY - FuelPumpBlockEntity.this.petrol);
                    if (units > 0 && action.execute()) {
                        FuelPumpBlockEntity.this.fill(kind, units);
                    }
                    return Math.max(0, units);
                }

                @Override
                public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
                    return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
                }

                @Override
                public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
                    return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
                }
            };

    /** Whether this fuel may go in now: a liquid one, and the same as what is there. */
    public boolean takes(CarSetup.@org.jetbrains.annotations.Nullable Fuel kind) {
        return kind != null && !kind.isElectric() && (this.petrol <= 0 || kind == this.fuel);
    }

    /** What the pump holds, or petrol if it is empty. */
    public CarSetup.Fuel getFuel() {
        return this.petrol <= 0 ? CarSetup.Fuel.PETROL : this.fuel;
    }

    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidStorage() {
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
        this.setChanged();
    }

    /** Takes what petrol it can, up to {@code amount}, and reports how much went in. */
    public int fill(int amount) {
        return this.fill(CarSetup.Fuel.PETROL, amount);
    }

    /** The same for any fuel; nothing goes into a pump holding something else. */
    public int fill(CarSetup.Fuel kind, int amount) {
        if (!this.takes(kind)) {
            return 0;
        }
        int accepted = Math.min(amount, CAPACITY - this.petrol);
        if (accepted > 0) {
            this.fuel = kind;
            this.petrol += accepted;
            this.setChanged();
        }
        return accepted;
    }

    /** Gives out what it can, up to {@code amount}, and reports how much came out. */
    public int drain(int amount) {
        int given = Math.min(amount, this.petrol);
        if (given > 0) {
            this.petrol -= given;
            this.setChanged();
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
        float accepted = car.addFuel(affordable, this.getFuel());
        int spent = Math.round(accepted) * PETROL_PER_FUEL_UNIT;
        this.drain(spent);
        return spent;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, FuelPumpBlockEntity pump) {
        pump.watchNozzle(world, pos, state);
        // The sight window and the display show what is in it, so the
        // clients are told — a few times a second, not every tick.
        if (pump.petrol != pump.sentPetrol && world.getGameTime() % 5 == 0) {
            pump.sentPetrol = pump.petrol;
            world.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        if (pump.petrol >= CAPACITY) {
            return;
        }
        // Siphons from whatever refineries it is up against, in whatever
        // arrangement: the point is that a station is something you build out of
        // the blocks rather than something you carry fuel to.
        for (Direction side : Direction.values()) {
            if (me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos.relative(side)) instanceof me.trossty.car.fluid.PetrolSource refinery) {
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
    private java.util.@org.jetbrains.annotations.Nullable UUID nozzleHolder;

    public boolean isHeldBy(net.minecraft.world.entity.Entity entity) {
        return entity.getUUID().equals(this.nozzleHolder);
    }

    /** Puts the nozzle in the player's empty hand. False if someone else has it. */
    public boolean takeNozzle(net.minecraft.world.entity.player.Player player) {
        if (this.nozzleHolder != null && !this.isHeldBy(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.overthought_vehicles.nozzle_in_use"), true);
            return false;
        }
        this.nozzleHolder = player.getUUID();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                me.trossty.car.item.FuelNozzleItem.forPump(this.worldPosition));
        me.trossty.car.block.FuelPumpBlock.setNozzleHung(this.level, this.worldPosition, false);
        this.level.playSound(null, this.worldPosition.above(), net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.4F);
        return true;
    }

    /**
     * Hangs the nozzle back on its hook, out of whoever had it: their hands,
     * their pockets, and the ground at their feet if they dropped it.
     *
     * @param snapped the hose ran out rather than being hung up properly
     */
    public void hangUpNozzle(boolean snapped) {
        if (this.level == null) {
            return;
        }
        net.minecraft.world.entity.player.Player holder = this.nozzleHolder == null
                ? null : this.level.getPlayerByUUID(this.nozzleHolder);
        this.nozzleHolder = null;
        if (holder != null) {
            net.minecraft.world.entity.player.Inventory inventory = holder.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (me.trossty.car.item.FuelNozzleItem.isFrom(inventory.getItem(slot), this.worldPosition)) {
                    inventory.setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
            if (holder.isUsingItem() && holder.getUseItem().is(me.trossty.car.registry.ModItems.FUEL_NOZZLE)) {
                holder.releaseUsingItem();
            }
            for (net.minecraft.world.entity.item.ItemEntity dropped : this.level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, holder.getBoundingBox().inflate(HOSE_LENGTH + 4.0),
                    item -> me.trossty.car.item.FuelNozzleItem.isFrom(item.getItem(), this.worldPosition))) {
                dropped.discard();
            }
        }
        me.trossty.car.block.FuelPumpBlock.setNozzleHung(this.level, this.worldPosition, true);
        this.level.playSound(null, this.worldPosition.above(),
                snapped ? net.minecraft.sounds.SoundEvents.LEASH_KNOT_BREAK
                        : net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, snapped ? 0.9F : 1.4F);
    }

    /**
     * Keeps the nozzle honest. It stays out only while the one who took it
     * has it in a hand and is within the hose's length; anything else, and
     * it is back on the hook.
     */
    private void watchNozzle(Level world, BlockPos pos, BlockState state) {
        if (this.nozzleHolder == null) {
            if (!state.getValue(me.trossty.car.block.FuelPumpBlock.NOZZLE)) {
                me.trossty.car.block.FuelPumpBlock.setNozzleHung(world, pos, true);
            }
            return;
        }
        net.minecraft.world.entity.player.Player holder = world.getPlayerByUUID(this.nozzleHolder);
        if (holder == null || !holder.isAlive() || holder.isSpectator()
                || !(me.trossty.car.item.FuelNozzleItem.isFrom(holder.getMainHandItem(), pos)
                        || me.trossty.car.item.FuelNozzleItem.isFrom(holder.getOffhandItem(), pos))) {
            this.hangUpNozzle(false);
            return;
        }
        net.minecraft.world.phys.Vec3 outlet = me.trossty.car.block.FuelPumpBlock.hoseOutlet(
                pos, state.getValue(me.trossty.car.block.FuelPumpBlock.FACING));
        if (holder.position().add(0.0, 0.9, 0.0).distanceToSqr(outlet) > HOSE_LENGTH * HOSE_LENGTH) {
            this.hangUpNozzle(true);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.petrol = view.getIntOr("Petrol", 0);
        this.fuel = CarSetup.Fuel.byOrdinal(view.getIntOr("Fuel", CarSetup.Fuel.PETROL.ordinal()));
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        view.putInt("Petrol", this.petrol);
        view.putInt("Fuel", this.fuel.ordinal());
    }
}
