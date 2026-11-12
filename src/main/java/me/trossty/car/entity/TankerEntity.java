package me.trossty.car.entity;

import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModItems;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

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
    private static final TrackedData<Integer> CARGO_AMOUNT =
            DataTracker.registerData(TankerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Which fluid it is, by registry number; -1 for none. */
    private static final TrackedData<Integer> CARGO_FLUID =
            DataTracker.registerData(TankerEntity.class, TrackedDataHandlerRegistry.INTEGER);

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

    private final SingleVariantStorage<FluidVariant> barrel = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return CARGO_CAPACITY * ModFluids.UNIT;
        }

        @Override
        protected void onFinalCommit() {
            TankerEntity.this.cargoChanged();
        }
    };

    public TankerEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CARGO_AMOUNT, 0);
        builder.add(CARGO_FLUID, -1);
    }

    // ---- the load ----

    /** The barrel, for anything that moves liquid through the transfer API. */
    public Storage<FluidVariant> getCargoStorage() {
        return this.barrel;
    }

    /** The load in millibuckets, on either side. */
    public int getCargo() {
        return this.getEntityWorld().isClient()
                ? this.dataTracker.get(CARGO_AMOUNT)
                : (int) (this.barrel.amount / ModFluids.UNIT);
    }

    public int getCargoCapacity() {
        return CARGO_CAPACITY;
    }

    /** What the load is, blank when there is none, on either side. */
    public FluidVariant getLiquid() {
        if (this.getEntityWorld().isClient()) {
            int raw = this.dataTracker.get(CARGO_FLUID);
            Fluid fluid = raw < 0 ? null : Registries.FLUID.get(raw);
            return fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid);
        }
        return this.barrel.variant;
    }

    public Hose getHose() {
        return this.hose;
    }

    public void setHose(Hose hose) {
        this.hose = hose;
    }

    /** Sets the load outright: so many millibuckets of one liquid. */
    public void setCargo(FluidVariant liquid, int millibuckets) {
        int amount = Math.max(0, Math.min(CARGO_CAPACITY, millibuckets));
        if (liquid.isBlank() || amount == 0) {
            this.barrel.variant = FluidVariant.blank();
            this.barrel.amount = 0;
        } else {
            this.barrel.variant = liquid;
            this.barrel.amount = amount * ModFluids.UNIT;
        }
        this.cargoChanged();
    }

    /** Petrol, as every load was before the barrel learnt any other liquid. */
    public void setCargo(int petrol) {
        this.setCargo(FluidVariant.of(ModFluids.PETROL), petrol);
    }

    /** Puts up to {@code millibuckets} of a liquid in; reports how much went. */
    public int fill(FluidVariant liquid, int millibuckets) {
        if (millibuckets <= 0 || liquid.isBlank()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = this.barrel.insert(liquid, millibuckets * ModFluids.UNIT, transaction);
            transaction.commit();
            return (int) (moved / ModFluids.UNIT);
        }
    }

    /** Takes up to {@code millibuckets} of a liquid out; reports how much came. */
    public int drain(FluidVariant liquid, int millibuckets) {
        if (millibuckets <= 0 || liquid.isBlank()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = this.barrel.extract(liquid, millibuckets * ModFluids.UNIT, transaction);
            transaction.commit();
            return (int) (moved / ModFluids.UNIT);
        }
    }

    /** Room for a liquid: all the space left if it matches the load or there is none, otherwise nothing. */
    public int roomFor(FluidVariant liquid) {
        boolean fits = this.barrel.isResourceBlank() || this.barrel.variant.equals(liquid);
        return fits ? CARGO_CAPACITY - (int) (this.barrel.amount / ModFluids.UNIT) : 0;
    }

    private void cargoChanged() {
        if (this.barrel.amount <= 0) {
            this.barrel.amount = 0;
            this.barrel.variant = FluidVariant.blank();
        }
        if (!this.getEntityWorld().isClient()) {
            this.dataTracker.set(CARGO_AMOUNT, (int) (this.barrel.amount / ModFluids.UNIT));
            this.dataTracker.set(CARGO_FLUID, this.barrel.isResourceBlank()
                    ? -1 : Registries.FLUID.getRawId(this.barrel.variant.getFluid()));
        }
    }

    /** What the gauge calls the load. */
    public Text liquidName() {
        FluidVariant liquid = this.getLiquid();
        return liquid.isBlank() ? Text.translatable("tanker.car.empty") : FluidVariantAttributes.getName(liquid);
    }

    @Override
    protected Item asItem() {
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
        if (this.getEntityWorld().isClient() || this.isRemoved()) {
            return;
        }
        this.drawFromOwnCargo();
        if (this.age % SCAN_INTERVAL == 0) {
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
        if (this.getFuel() >= SELF_REFUEL_BELOW || this.barrel.isResourceBlank()) {
            return;
        }
        // Only the fuel its engine takes: a diesel lorry hauling petrol, or
        // milk, fills up like anybody else.
        CarSetup.Fuel fuel = ModFluids.fuelOf(this.barrel.variant.getFluid());
        if (fuel == null || !this.getEngineFuel().burns(fuel)) {
            return;
        }
        int perUnit = FuelPumpBlockEntity.PETROL_PER_FUEL_UNIT;
        int units = (int) Math.min(this.getMaxFuel() - this.getFuel(), this.getCargo() / perUnit);
        if (units > 0) {
            int accepted = Math.round(this.addFuel(units, fuel));
            this.drain(this.barrel.variant, accepted * perUnit);
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
        World world = this.getEntityWorld();
        BlockPos at = this.getBlockPos();
        int batch = HOSE_RATE * SCAN_INTERVAL;

        BlockPos nearest = null;
        Storage<FluidVariant> storage = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(at.add(-HOSE_REACH, -2, -HOSE_REACH),
                at.add(HOSE_REACH, 2, HOSE_REACH))) {
            double distance = pos.getSquaredDistance(this.getEntityPos());
            if (distance >= nearestDistance || isPlumbing(world, pos)) {
                continue;
            }
            Direction facing = Direction.getFacing(this.getEntityPos().subtract(Vec3d.ofCenter(pos)));
            Storage<FluidVariant> found = FluidStorage.SIDED.find(world, pos, facing);
            if (found != null) {
                nearestDistance = distance;
                nearest = pos.toImmutable();
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

        FluidVariant petrol = FluidVariant.of(ModFluids.PETROL);
        if (loading && pump != null) {
            // A pump hands petrol over through its own counter, not its pipe
            // connection, which only takes it in.
            int room = this.roomFor(petrol);
            int taken = room <= 0 ? 0 : pump.drain(Math.min(batch, room));
            if (taken > 0) {
                this.fill(petrol, taken);
                this.playHoseSound(FluidVariantAttributes.getFillSound(petrol), 1.1F);
            }
            return;
        }

        long moved = this.pipe(storage, loading, batch * ModFluids.UNIT);
        if (moved == 0) {
            // A cauldron, or anything else that only deals in whole buckets.
            moved = this.pipe(storage, loading, net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);
        }
        if (moved > 0) {
            FluidVariant liquid = loading ? this.barrel.variant : StorageUtil.findStoredResource(storage);
            if (liquid != null && !liquid.isBlank()) {
                this.playHoseSound(loading ? FluidVariantAttributes.getFillSound(liquid)
                        : FluidVariantAttributes.getEmptySound(liquid), loading ? 1.1F : 0.8F);
            }
        }
    }

    private long pipe(Storage<FluidVariant> other, boolean loading, long amount) {
        return loading
                ? StorageUtil.move(other, this.barrel, variant -> true, amount, null)
                : StorageUtil.move(this.barrel, other, variant -> true, amount, null);
    }

    /** The yard's pipes and valves carry liquid past; they are not somewhere to load or unload. */
    private static boolean isPlumbing(World world, BlockPos pos) {
        var block = world.getBlockState(pos).getBlock();
        return block instanceof me.trossty.car.block.FuelPipeBlock
                || block instanceof me.trossty.car.block.FuelValveBlock;
    }

    private void playHoseSound(SoundEvent sound, float pitch) {
        this.getEntityWorld().playSound(null, this.getBlockPos(), sound, SoundCategory.NEUTRAL, 0.5F, pitch);
    }

    // ---- sneaking is the cargo ----

    @Override
    protected ActionResult interactUnlocked(PlayerEntity player, Hand hand) {
        if (!player.shouldCancelInteraction()) {
            return super.interactUnlocked(player, hand);
        }
        if (this.getEntityWorld().isClient()) {
            return ActionResult.SUCCESS;
        }

        ItemStack held = player.getStackInHand(hand);
        if (held.isEmpty()) {
            this.hose = this.hose.next();
        } else if (held.isOf(ModItems.JERRY_CAN)) {
            this.useJerryCan(player, held);
        } else {
            this.useContainer(player, hand);
        }
        this.showGauge(player);
        return ActionResult.CONSUME;
    }

    /** The gauge: what, how much, and what the hose is doing. */
    public void showGauge(PlayerEntity player) {
        player.sendMessage(Text.translatable("message.car.tanker_gauge", this.liquidName(),
                this.getCargo(), CARGO_CAPACITY, Text.translatable("hose.car." + this.hose.key())), true);
    }

    /** A bucket, a bottle, or another mod's tank item: whichever way it can go, it goes. */
    private void useContainer(PlayerEntity player, Hand hand) {
        Storage<FluidVariant> item = ContainerItemContext.forPlayerInteraction(player, hand).find(FluidStorage.ITEM);
        if (item == null) {
            return;
        }
        FluidVariant before = this.barrel.variant;
        long poured = StorageUtil.move(item, this.barrel, variant -> true, Long.MAX_VALUE, null);
        if (poured > 0) {
            this.playHoseSound(FluidVariantAttributes.getEmptySound(this.barrel.variant), 1.0F);
            return;
        }
        long drawn = StorageUtil.move(this.barrel, item, variant -> true, Long.MAX_VALUE, null);
        if (drawn > 0) {
            this.playHoseSound(FluidVariantAttributes.getFillSound(before), 1.0F);
            return;
        }
        FluidVariant offered = StorageUtil.findStoredResource(item);
        if (offered != null && !this.barrel.isResourceBlank() && !offered.equals(this.barrel.variant)) {
            player.sendMessage(Text.translatable("message.car.tanker_holds", this.liquidName()), true);
        }
    }

    /** A jerry can is fuel only: it tops up from a load of its own fuel, or tips into one. */
    private void useJerryCan(PlayerEntity player, ItemStack can) {
        FluidVariant inCan = FluidVariant.of(ModFluids.fluidOf(fuelIn(can)));
        CarSetup.Fuel loaded = this.barrel.isResourceBlank() ? null : ModFluids.fuelOf(this.barrel.variant.getFluid());

        if (!me.trossty.car.item.JerryCans.isFull(can) && loaded != null
                && (me.trossty.car.item.JerryCans.isEmpty(can) || fuelIn(can) == loaded)) {
            FluidVariant load = this.barrel.variant;
            if (me.trossty.car.item.JerryCans.topUp(can, this.getCargo(), amount -> this.drain(load, amount)) > 0) {
                markFuel(can, loaded);
                this.playHoseSound(FluidVariantAttributes.getFillSound(load), 1.1F);
                return;
            }
        }
        if (!me.trossty.car.item.JerryCans.isEmpty(can)) {
            if (me.trossty.car.item.JerryCans.emptyInto(can, this.roomFor(inCan),
                    amount -> this.fill(inCan, amount)) > 0) {
                this.playHoseSound(FluidVariantAttributes.getEmptySound(inCan), 0.8F);
                return;
            }
        }
        if (!this.barrel.isResourceBlank() && loaded == null) {
            player.sendMessage(Text.translatable("message.car.tanker_holds", this.liquidName()), true);
        }
    }

    /** What a tanker item says it is carrying (older worlds pocketed them): the load and the liquid, petrol if it never said. */
    public void loadFrom(ItemStack stack) {
        Integer cargo = stack.get(ModComponents.TANKER_CARGO);
        if (cargo != null) {
            FluidVariant liquid = stack.get(ModComponents.TANKER_LIQUID);
            this.setCargo(liquid == null ? FluidVariant.of(ModFluids.PETROL) : liquid, cargo);
        }
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        int cargo = view.getInt("Cargo", 0);
        FluidVariant liquid = view.read("Liquid", FluidVariant.CODEC)
                .orElseGet(() -> FluidVariant.of(ModFluids.PETROL));
        this.setCargo(liquid, cargo);
        String hose = view.getString("Hose", Hose.AUTO.name());
        try {
            this.hose = Hose.valueOf(hose);
        } catch (IllegalArgumentException unknown) {
            this.hose = Hose.AUTO;
        }
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putInt("Cargo", this.getCargo());
        if (!this.barrel.isResourceBlank()) {
            view.put("Liquid", FluidVariant.CODEC, this.barrel.variant);
        }
        view.putString("Hose", this.hose.name());
    }

    @Override
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.TANKER;
    }

    /** Whether this liquid glows in the glass: lava, or anything else as bright. */
    public static boolean glows(@Nullable FluidVariant liquid) {
        return liquid != null && !liquid.isBlank() && FluidVariantAttributes.getLuminance(liquid) > 0;
    }
}
