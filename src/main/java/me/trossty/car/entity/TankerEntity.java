package me.trossty.car.entity;

import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.world.phys.Vec3;
import me.trossty.car.fluid.Liquids;
import me.trossty.car.fluid.UnitTank;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The tanker: a lorry with a barrel on the back, and the barrel will carry
 * any one liquid at a time — petrol or diesel, water, lava, milk, honey, or
 * whatever another mod pipes into it.
 *
 * <p>Pipe is how liquid crosses a yard. This is how it crosses a world. The
 * barrel holds {@link #CARGO_CAPACITY} millibuckets, eight buckets: eight full
 * car tanks of fuel, or a lake's worth of nothing in particular. Between its
 * middle bands it is glass, and the load shows through it at its own level.
 *
 * <p>Underneath it is a fluid storage in Fabric's transfer API, which is what
 * makes "any liquid" true rather than a list: a bucket, a bottle, a cauldron,
 * or another mod's tank, if the transfer API can move it, the tanker takes it.
 *
 * <h2>The hose</h2>
 * Parked with nobody at the wheel, it looks for something holding liquid
 * within {@link #HOSE_REACH} and works the nearest one, in one of four ways
 * the driver sets by sneaking at it with an empty hand:
 * <ul>
 *   <li><b>auto</b> — the fuel yard's own way: loads from a pump or refinery,
 *       unloads into a storage tank, and delivers into anything else;</li>
 *   <li><b>load</b> — draws from whatever is nearest;</li>
 *   <li><b>unload</b> — empties into whatever is nearest;</li>
 *   <li><b>off</b>.</li>
 * </ul>
 * Nearest wins, so a tanker parked between two tanks does one thing rather
 * than pumping round in a circle; and auto never draws from a stranger, so it
 * can never fill from the thing it has just emptied into.
 *
 * <h2>Sneaking is the cargo</h2>
 * A plain right click is the vehicle. Sneaking is the load: a bucket or bottle
 * of something pours in, an empty one is filled, a jerry can tops up or tips
 * in, an empty hand sets the hose. Every one of them reads the gauge.
 *
 * <p>And a tanker never strands itself: when its own tank runs low and the
 * barrel holds the fuel its engine takes, it helps itself.
 */
public class TankerEntity extends CarEntity {

    /** Eight buckets: eight full car tanks. Half a yard tank, which is the point of the trip. */
    public static final int CARGO_CAPACITY = 8000;

    /** How far from the hose a tank, pump or anything else holding liquid can stand. */
    public static final int HOSE_REACH = 3;

    /** Millibuckets moved per tick while parked, averaged over the scan interval. */
    private static final int HOSE_RATE = 16;

    /** Ticks between hose scans. Looking at 245 blocks every tick would be rude. */
    private static final int SCAN_INTERVAL = 10;

    /** Laden and geared for pulling, not for going. */
    private static final float TOP_SPEED_FACTOR = 0.78F;

    /** Below this the tanker helps itself to its own cargo. */
    private static final float SELF_REFUEL_BELOW = 20.0F;

    /** The load, in millibuckets, for the sight glass. */
    private static final EntityDataAccessor<Integer> CARGO_AMOUNT =
            SynchedEntityData.defineId(TankerEntity.class, EntityDataSerializers.INT);
    /** Which fluid it is, by registry number; -1 for none. */
    private static final EntityDataAccessor<Integer> CARGO_FLUID =
            SynchedEntityData.defineId(TankerEntity.class, EntityDataSerializers.INT);

    /** What the hose does with the nearest thing holding liquid. */
    public enum Hose {
        AUTO, LOAD, UNLOAD, OFF;

        public String key() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }

        Hose next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    private Hose hose = Hose.AUTO;

    private final UnitTank barrel = new UnitTank(CARGO_CAPACITY, liquid -> true, true, true, this::cargoChanged);

    public TankerEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CARGO_AMOUNT, 0);
        builder.define(CARGO_FLUID, -1);
    }

    // ---- the load ----

    /** The barrel, for anything that moves liquid through NeoForge's fluid handlers. */
    public IFluidHandler getCargoStorage() {
        return this.barrel;
    }

    /** The load in millibuckets, on either side. */
    public int getCargo() {
        return this.level().isClientSide()
                ? this.entityData.get(CARGO_AMOUNT)
                : this.barrel.getUnits();
    }

    public int getCargoCapacity() {
        return CARGO_CAPACITY;
    }

    /** What the load is, blank when there is none, on either side. */
    public Fluid getLiquid() {
        if (this.level().isClientSide()) {
            int raw = this.entityData.get(CARGO_FLUID);
            Fluid fluid = raw < 0 ? null : BuiltInRegistries.FLUID.byId(raw);
            return fluid == null ? Fluids.EMPTY : fluid;
        }
        return this.barrel.getFluid();
    }

    public Hose getHose() {
        return this.hose;
    }

    public void setHose(Hose hose) {
        this.hose = hose;
    }

    /** Sets the load outright: so many millibuckets of one liquid. */
    public void setCargo(Fluid liquid, int millibuckets) {
        int amount = Math.max(0, Math.min(CARGO_CAPACITY, millibuckets));
        this.barrel.set(liquid, amount);
        this.cargoChanged();
    }

    /** Petrol, as every load was before the barrel learnt any other liquid. */
    public void setCargo(int petrol) {
        this.setCargo(ModFluids.PETROL, petrol);
    }

    /** Puts up to {@code millibuckets} of a liquid in; reports how much went. */
    public int fill(Fluid liquid, int millibuckets) {
        if (millibuckets <= 0 || Liquids.isBlank(liquid)) {
            return 0;
        }
        int moved = this.barrel.fill(liquid, millibuckets);
        this.cargoChanged();
        return moved;
    }

    /** Takes up to {@code millibuckets} of a liquid out; reports how much came. */
    public int drain(Fluid liquid, int millibuckets) {
        if (millibuckets <= 0 || Liquids.isBlank(liquid)) {
            return 0;
        }
        if (this.barrel.getFluid() != liquid) {
            return 0;
        }
        int moved = this.barrel.drain(millibuckets);
        this.cargoChanged();
        return moved;
    }

    /** Room for a liquid: all the space left if it matches the load or there is none, otherwise nothing. */
    public int roomFor(Fluid liquid) {
        return this.barrel.canTake(liquid) ? this.barrel.room() : 0;
    }

    private void cargoChanged() {
        if (!this.level().isClientSide()) {
            this.entityData.set(CARGO_AMOUNT, this.barrel.getUnits());
            this.entityData.set(CARGO_FLUID, this.barrel.isEmpty()
                    ? -1 : BuiltInRegistries.FLUID.getId(this.barrel.getFluid()));
        }
    }

    /** What the gauge calls the load. */
    public Component liquidName() {
        Fluid liquid = this.getLiquid();
        return Liquids.isBlank(liquid) ? Component.translatable("tanker.car.empty") : Liquids.name(liquid);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.FUEL_TANKER;
    }

    /** A loaded tanker is a lorry, not a sports car. */
    @Override
    public float getMaxSpeed() {
        return super.getMaxSpeed() * TOP_SPEED_FACTOR;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || this.isRemoved()) {
            return;
        }
        this.drawFromOwnCargo();
        if (this.tickCount % SCAN_INTERVAL == 0) {
            this.workTheHose();
        }
    }

    /** Eight buckets of anything on the back is a third again at the pump. */
    @Override
    public float loadThirst() {
        return super.loadThirst() * (1.0F + 0.3F * this.getCargo() / CARGO_CAPACITY);
    }

    /** A tanker that runs dry while carrying eight tanks of its own fuel is a joke. */
    private void drawFromOwnCargo() {
        if (this.getFuel() >= SELF_REFUEL_BELOW || this.barrel.isEmpty()) {
            return;
        }
        // Only the fuel its engine takes: a diesel lorry hauling petrol, or
        // milk, fills up like anybody else.
        CarSetup.Fuel fuel = ModFluids.fuelOf(this.barrel.getFluid());
        if (fuel == null || !this.getEngineFuel().burns(fuel)) {
            return;
        }
        int perUnit = FuelPumpBlockEntity.PETROL_PER_FUEL_UNIT;
        int units = (int) Math.min(this.getMaxFuel() - this.getFuel(), this.getCargo() / perUnit);
        if (units > 0) {
            int accepted = Math.round(this.addFuel(units, fuel));
            this.drain(this.barrel.getFluid(), accepted * perUnit);
        }
    }

    // ---- the hose ----

    /**
     * Finds the nearest thing holding liquid and moves the load the way the
     * hose is set. One winner, so nothing can set up a loop.
     */
    private void workTheHose() {
        if (this.getControllingPassenger() != null || this.hose == Hose.OFF) {
            return;
        }
        Level world = this.level();
        BlockPos at = this.blockPosition();
        int batch = HOSE_RATE * SCAN_INTERVAL;

        BlockPos nearest = null;
        IFluidHandler storage = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-HOSE_REACH, -2, -HOSE_REACH),
                at.offset(HOSE_REACH, 2, HOSE_REACH))) {
            double distance = pos.distToCenterSqr(this.position());
            if (distance >= nearestDistance || isPlumbing(world, pos)) {
                continue;
            }
            Direction facing = Direction.getNearest(this.position().subtract(Vec3.atCenterOf(pos)));
            IFluidHandler found = world.getCapability(Capabilities.FluidHandler.BLOCK, pos, facing);
            if (found != null) {
                nearestDistance = distance;
                nearest = pos.immutable();
                storage = found;
            }
        }
        if (nearest == null) {
            return;
        }

        FuelPumpBlockEntity pump = FuelPumpBlock.tankAt(world, nearest);
        boolean loading = switch (this.hose) {
            // The yard's own way: pumps, pumpjacks and the plants are where
            // liquid comes from; a tank, or anything of another mod's, is where it goes.
            case AUTO -> pump != null || me.trossty.car.fluid.FluidSource.at(world, nearest);
            case LOAD -> true;
            default -> false;
        };

        if (loading && pump != null) {
            Fluid petrol = ModFluids.fluidOf(pump.getFuel());
            // A pump hands petrol over through its own counter, not its pipe
            // connection, which only takes it in.
            int room = this.roomFor(petrol);
            int taken = room <= 0 ? 0 : pump.drain(Math.min(batch, room));
            if (taken > 0) {
                this.fill(petrol, taken);
                this.playHoseSound(Liquids.fillSound(petrol), 1.1F);
            }
            return;
        }

        long moved = this.pipe(storage, loading, batch * ModFluids.UNIT);
        if (moved == 0) {
            // A cauldron, or anything else that only deals in whole buckets.
            moved = this.pipe(storage, loading, net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME);
        }
        if (moved > 0) {
            Fluid liquid = loading ? this.barrel.getFluid() : Liquids.storedIn(storage);
            if (!Liquids.isBlank(liquid)) {
                this.playHoseSound(loading ? Liquids.fillSound(liquid)
                        : Liquids.emptySound(liquid), loading ? 1.1F : 0.8F);
            }
        }
    }

    private long pipe(IFluidHandler other, boolean loading, int amount) {
        return loading
                ? Liquids.move(other, this.barrel, null, amount)
                : Liquids.move(this.barrel, other, null, amount);
    }

    /** The yard's pipes and valves carry liquid past; they are not somewhere to load or unload. */
    private static boolean isPlumbing(Level world, BlockPos pos) {
        var block = world.getBlockState(pos).getBlock();
        return block instanceof me.trossty.car.block.FuelPipeBlock
                || block instanceof me.trossty.car.block.FuelValveBlock;
    }

    private void playHoseSound(SoundEvent sound, float pitch) {
        this.level().playSound(null, this.blockPosition(), sound, SoundSource.NEUTRAL, 0.5F, pitch);
    }

    // ---- sneaking is the cargo ----

    @Override
    protected InteractionResult interactUnlocked(Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive()) {
            return super.interactUnlocked(player, hand);
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            this.hose = this.hose.next();
        } else if (held.is(ModItems.JERRY_CAN)) {
            this.useJerryCan(player, held);
        } else {
            this.useContainer(player, hand);
        }
        this.showGauge(player);
        return InteractionResult.CONSUME;
    }

    /** The gauge: what, how much, and what the hose is doing. */
    public void showGauge(Player player) {
        player.displayClientMessage(Component.translatable("message.car.tanker_gauge", this.liquidName(),
                this.getCargo(), CARGO_CAPACITY, Component.translatable("hose.car." + this.hose.key())), true);
    }

    /** A bucket, a bottle, or another mod's tank item: whichever way it can go, it goes. */
    private void useContainer(Player player, InteractionHand hand) {
        Fluid before = this.barrel.getFluid();
        if (FluidUtil.interactWithFluidHandler(player, hand, this.barrel)) {
            return;
        }
        // Nothing moved: say why, if it is because the load is something else.
        var item = FluidUtil.getFluidHandler(player.getItemInHand(hand).copyWithCount(1)).orElse(null);
        Fluid offered = item == null ? Fluids.EMPTY : Liquids.storedIn(item);
        if (!Liquids.isBlank(offered) && !this.barrel.isEmpty() && offered != before) {
            player.displayClientMessage(Component.translatable("message.car.tanker_holds", this.liquidName()), true);
        }
    }

    /** A jerry can is fuel only: it tops up from a load of its own fuel, or tips into one. */
    private void useJerryCan(Player player, ItemStack can) {
        Fluid inCan = ModFluids.fluidOf(fuelIn(can));
        CarSetup.Fuel loaded = this.barrel.isEmpty() ? null : ModFluids.fuelOf(this.barrel.getFluid());

        if (!me.trossty.car.item.JerryCans.isFull(can) && loaded != null
                && (me.trossty.car.item.JerryCans.isEmpty(can) || fuelIn(can) == loaded)) {
            Fluid load = this.barrel.getFluid();
            if (me.trossty.car.item.JerryCans.topUp(can, this.getCargo(), amount -> this.drain(load, amount)) > 0) {
                markFuel(can, loaded);
                this.playHoseSound(Liquids.fillSound(load), 1.1F);
                return;
            }
        }
        if (!me.trossty.car.item.JerryCans.isEmpty(can)) {
            if (me.trossty.car.item.JerryCans.emptyInto(can, this.roomFor(inCan),
                    amount -> this.fill(inCan, amount)) > 0) {
                this.playHoseSound(Liquids.emptySound(inCan), 0.8F);
                return;
            }
        }
        if (!this.barrel.isEmpty() && loaded == null) {
            player.displayClientMessage(Component.translatable("message.car.tanker_holds", this.liquidName()), true);
        }
    }

    /** What a tanker item says it is carrying (older worlds pocketed them): the load and the liquid, petrol if it never said. */
    public void loadFrom(ItemStack stack) {
        Integer cargo = stack.get(ModComponents.TANKER_CARGO);
        if (cargo != null) {
            Fluid liquid = stack.get(ModComponents.TANKER_LIQUID);
            this.setCargo(liquid == null ? ModFluids.PETROL : liquid, cargo);
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        ValueInput view = ValueInput.of(tag, this.registryAccess());
        super.readAdditionalSaveData(tag);
        int cargo = view.getIntOr("Cargo", 0);
        Fluid liquid = view.read("Liquid", Liquids.CODEC)
                .orElseGet(() -> ModFluids.PETROL);
        this.setCargo(liquid, cargo);
        String hose = view.getStringOr("Hose", Hose.AUTO.name());
        try {
            this.hose = Hose.valueOf(hose);
        } catch (IllegalArgumentException unknown) {
            this.hose = Hose.AUTO;
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        ValueOutput view = ValueOutput.of(tag, this.registryAccess());
        super.addAdditionalSaveData(tag);
        view.putInt("Cargo", this.getCargo());
        if (!this.barrel.isEmpty()) {
            view.store("Liquid", Liquids.CODEC, this.barrel.getFluid());
        }
        view.putString("Hose", this.hose.name());
    }

    @Override
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.TANKER;
    }

    /** Whether this liquid glows in the glass: lava, or anything else as bright. */
    public static boolean glows(@Nullable Fluid liquid) {
        return Liquids.luminance(liquid) > 0;
    }
}
