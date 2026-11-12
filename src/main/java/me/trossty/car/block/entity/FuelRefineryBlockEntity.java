package me.trossty.car.block.entity;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.refinery.FuelRefineryScreenHandler;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModItems;
import me.trossty.car.registry.ModTags;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import me.trossty.car.registry.ModFluids;
import net.minecraft.block.BlockState;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The middle of the production line.
 *
 * <p>Two stages in one plant, both on electricity. The boiler cooks organic
 * matter down to <em>biomass</em>, counted in units like the fluid it is meant
 * to be; crude oil — out of a pumpjack, or another mod's — goes into the same
 * feed tank instead, though never both at once. The column then cracks the
 * feed with a catalyst:
 *
 * <ul>
 *   <li>biomass becomes <b>biofuel</b>, ready to burn, whatever the catalyst;</li>
 *   <li>crude becomes <b>raw petrol</b> (redstone, blaze or glowstone) or
 *       <b>raw diesel</b> (coal), which no engine takes until a hydrotreater
 *       has cleaned it.</li>
 * </ul>
 *
 * <p>The column has to be hot: it warms while the plant works and cools when it
 * does not, cracks nothing below {@link #WORKING_TEMPERATURE} and works twice as
 * fast past {@link #HOT_TEMPERATURE}. A jerry can in the last slot drinks
 * biofuel as it is made; the raw fuels leave through the product door, for a
 * hydrotreater standing against the plant, a tanker's hose or another mod's pipe.
 */
public class FuelRefineryBlockEntity extends LockableContainerBlockEntity implements me.trossty.car.fluid.FluidSource {

    public static final int ORGANIC_SLOT = 0;
    /** A bucket of somebody else's crude oil, emptied into the feedstock tank. */
    public static final int OIL_SLOT = 1;
    public static final int CATALYST_SLOT = 2;
    public static final int CAN_SLOT = 3;
    /** Out only: tar, the heavy end left in the column when crude is cracked. */
    public static final int TAR_SLOT = 4;
    public static final int SIZE = 5;

    public static final int BIOMASS_CAPACITY = 2000;
    public static final int PETROL_CAPACITY = 1000;

    /** Heat ticks one piece of organic matter needs, and what it yields. */
    public static final int DISTILL_TIME = 120;
    public static final int BIOMASS_PER_ITEM = 100;

    /** One catalyst turns this much feed into this much product. */
    public static final int REFINE_TIME = 100;
    public static final int BIOMASS_PER_REFINE = 200;
    public static final int PETROL_PER_REFINE = 200;

    /**
     * Petrol one pour into a jerry can costs. Four of these fill a can, and a
     * full can is exactly one full tank, so the whole chain is measured in tanks.
     */
    public static final int PETROL_PER_POUR = PETROL_CAPACITY / 4;

    /** What the buffer holds, and how fast a cable may fill it. */
    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_INPUT = 200;

    /** What each stage draws for every tick it is working. */
    public static final int DISTILL_POWER = 8;
    public static final int REFINE_POWER = 16;

    /** What the column heater draws while it is bringing a cold column up. */
    public static final int PREHEAT_POWER = 12;

    /** The column's temperature runs from stone cold to this. */
    public static final int MAX_TEMPERATURE = 1000;

    /** Below this the column cracks nothing at all. */
    public static final int WORKING_TEMPERATURE = 300;

    /** At or above this it cracks twice as fast for the same power. */
    public static final int HOT_TEMPERATURE = 800;

    /** How fast it warms while working, and cools while idle, per tick. */
    public static final int WARM_RATE = 2;
    public static final int COOL_RATE = 1;

    /** Feedstock a full bucket of crude oil is worth: one unit a millibucket. */
    public static final int OIL_PER_BUCKET = 1000;

    public static final int ENERGY_INDEX = 0;
    public static final int TEMPERATURE_INDEX = 1;
    public static final int DISTILL_INDEX = 2;
    public static final int REFINE_INDEX = 3;
    public static final int BIOMASS_INDEX = 4;
    public static final int PETROL_INDEX = 5;
    /** What the second tank holds: one of the {@code PRODUCT_} numbers. */
    public static final int OUTPUT_INDEX = 6;
    /** What the feed tank holds: {@link #FEED_BIOMASS} or {@link #FEED_CRUDE}. */
    public static final int FEED_INDEX = 7;
    public static final int PROPERTY_COUNT = 8;

    /** What a product turns into once hydrotreated; biofuel needs nothing doing. */
    private static net.minecraft.fluid.Fluid cleanOf(net.minecraft.fluid.Fluid product) {
        return product == ModFluids.BIOFUEL ? product : HydrotreaterBlockEntity.cleanOf(product);
    }

    public static final int PRODUCT_RAW_PETROL = 0;
    public static final int PRODUCT_RAW_DIESEL = 1;
    public static final int PRODUCT_BIOFUEL = 2;

    public static final int FEED_BIOMASS = 0;
    public static final int FEED_CRUDE = 1;

    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

    private int temperature;
    private boolean working;

    /**
     * The buffer. It only takes: nothing is allowed to pull power back out of
     * a refinery, so a cable network cannot use one as a battery.
     */
    private final SimpleEnergyStorage energy =
            new SimpleEnergyStorage(ENERGY_CAPACITY, ENERGY_MAX_INPUT, 0) {
                @Override
                protected void onFinalCommit() {
                    FuelRefineryBlockEntity.this.markDirty();
                }
            };

    /**
     * The feedstock tank as another mod's pipe sees it: somewhere crude oil
     * can be pushed in, a millibucket for a unit, and nothing taken out.
     */
    private final Storage<FluidVariant> oilInlet = new OilInlet();

    private final class OilInlet extends SnapshotParticipant<Integer>
            implements InsertionOnlyStorage<FluidVariant> {

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || !isCrudeOil(resource)
                    || (FuelRefineryBlockEntity.this.biomass > 0
                            && FuelRefineryBlockEntity.this.feed != FEED_CRUDE)) {
                return 0;
            }
            long units = Math.min(maxAmount / ModFluids.UNIT,
                    BIOMASS_CAPACITY - FuelRefineryBlockEntity.this.biomass);
            if (units <= 0) {
                return 0;
            }
            this.updateSnapshots(transaction);
            FuelRefineryBlockEntity.this.biomass += (int) units;
            FuelRefineryBlockEntity.this.feed = FEED_CRUDE;
            return units * ModFluids.UNIT;
        }

        @Override
        protected Integer createSnapshot() {
            return FuelRefineryBlockEntity.this.biomass * 2 + FuelRefineryBlockEntity.this.feed;
        }

        @Override
        protected void readSnapshot(Integer snapshot) {
            FuelRefineryBlockEntity.this.biomass = snapshot / 2;
            FuelRefineryBlockEntity.this.feed = snapshot % 2;
        }

        @Override
        protected void onFinalCommit() {
            FuelRefineryBlockEntity.this.markDirty();
        }
    }
    private int distillProgress;
    private int refineProgress;
    /** Units in the feed tank, and what they are. */
    private int biomass;
    private int feed = FEED_BIOMASS;

    /**
     * The second tank. One product at a time — a different one waits until
     * it is empty — and open to pipes and hoses for taking out only.
     */
    private final me.trossty.car.fluid.UnitTank product = new me.trossty.car.fluid.UnitTank(
            PETROL_CAPACITY,
            fluid -> fluid == ModFluids.RAW_PETROL || fluid == ModFluids.RAW_DIESEL || fluid == ModFluids.BIOFUEL,
            false, true, this::markDirty);

    private final PropertyDelegate properties = new ArrayPropertyDelegate(PROPERTY_COUNT) {
        @Override
        public int get(int index) {
            return switch (index) {
                case ENERGY_INDEX -> (int) FuelRefineryBlockEntity.this.energy.amount;
                case TEMPERATURE_INDEX -> FuelRefineryBlockEntity.this.temperature;
                case DISTILL_INDEX -> FuelRefineryBlockEntity.this.distillProgress;
                case REFINE_INDEX -> FuelRefineryBlockEntity.this.refineProgress;
                case BIOMASS_INDEX -> FuelRefineryBlockEntity.this.biomass;
                case PETROL_INDEX -> FuelRefineryBlockEntity.this.product.getUnits();
                case OUTPUT_INDEX -> FuelRefineryBlockEntity.this.getProduct();
                case FEED_INDEX -> FuelRefineryBlockEntity.this.feed;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case ENERGY_INDEX -> FuelRefineryBlockEntity.this.energy.amount = value;
                case TEMPERATURE_INDEX -> FuelRefineryBlockEntity.this.temperature = value;
                case DISTILL_INDEX -> FuelRefineryBlockEntity.this.distillProgress = value;
                case REFINE_INDEX -> FuelRefineryBlockEntity.this.refineProgress = value;
                case BIOMASS_INDEX -> FuelRefineryBlockEntity.this.biomass = value;
                case FEED_INDEX -> FuelRefineryBlockEntity.this.feed = value;
                default -> { }
            }
        }
    };

    public FuelRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_REFINERY, pos, state);
    }

    /** Whether the machine drew power last tick, which is what lights it up. */
    public boolean isWorking() {
        return this.working;
    }

    /** What other mods' cables talk to. */
    public EnergyStorage getEnergyStorage() {
        return this.energy;
    }

    /** What other mods' pipes push crude oil into. */
    public Storage<FluidVariant> getOilInlet() {
        return this.oilInlet;
    }

    /** Both doors at once: crude in, product out. */
    public Storage<FluidVariant> getFluidStorage() {
        return new net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage<>(
                java.util.List.of(this.oilInlet, this.product));
    }

    /** The product tank itself, for the hydrotreater drawing from it and for tests. */
    public me.trossty.car.fluid.UnitTank getProductTank() {
        return this.product;
    }

    public int getEnergy() {
        return (int) this.energy.amount;
    }

    /** Fills or empties the buffer outright: commands and tests. */
    public void setEnergy(int amount) {
        this.energy.amount = Math.max(0, Math.min(ENERGY_CAPACITY, amount));
    }

    public int getTemperature() {
        return this.temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = Math.max(0, Math.min(MAX_TEMPERATURE, temperature));
    }

    /** Whether a fluid is crude oil as far as this machine is concerned. */
    public static boolean isCrudeOil(FluidVariant fluid) {
        return fluid.getRegistryEntry().isIn(ModTags.CRUDE_OIL);
    }

    /**
     * How much crude oil an item is carrying, in feedstock units; nothing if
     * it is not a container of the stuff. Asks the item the way a pipe would,
     * so any mod's bucket, cell or tank answers without being listed anywhere.
     */
    public static int oilIn(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Storage<FluidVariant> held = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
        if (held == null) {
            return 0;
        }
        long found = 0;
        for (StorageView<FluidVariant> view : held) {
            if (!view.isResourceBlank() && isCrudeOil(view.getResource())) {
                found += view.getAmount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, found / ModFluids.UNIT);
    }

    /** Draws power for one tick of work, or says there is not enough. */
    private boolean draw(int amount) {
        if (this.energy.amount < amount) {
            return false;
        }
        this.energy.amount -= amount;
        return true;
    }

    public int getBiomass() {
        return this.biomass;
    }

    /** What is in the feed tank: {@link #FEED_BIOMASS} or {@link #FEED_CRUDE}. */
    public int getFeed() {
        return this.feed;
    }

    /** Units in the product tank. */
    public int getPetrol() {
        return this.product.getUnits();
    }

    /** What the product tank holds, as one of the {@code PRODUCT_} numbers. */
    public int getProduct() {
        net.minecraft.fluid.Fluid fluid = this.product.getFluid();
        return fluid == ModFluids.RAW_DIESEL ? PRODUCT_RAW_DIESEL
                : fluid == ModFluids.BIOFUEL ? PRODUCT_BIOFUEL : PRODUCT_RAW_PETROL;
    }

    /** Puts feed straight into the tank; for tests and commands. */
    public void setFeed(int kind, int units) {
        this.feed = kind;
        this.biomass = Math.max(0, Math.min(BIOMASS_CAPACITY, units));
    }

    /** What this catalyst makes of what is in the feed tank. */
    public static net.minecraft.fluid.Fluid productOf(int feed, ItemStack catalyst) {
        if (feed == FEED_BIOMASS) {
            return ModFluids.BIOFUEL;
        }
        return catalyst.isIn(ModTags.REFINERY_DIESEL_CATALYST) ? ModFluids.RAW_DIESEL : ModFluids.RAW_PETROL;
    }

    /** One tar into the out slot, if it has room; a full slot just loses it, as residue does. */
    private void leaveTar() {
        ItemStack tar = this.inventory.get(TAR_SLOT);
        if (tar.isEmpty()) {
            this.inventory.set(TAR_SLOT, new ItemStack(ModItems.TAR));
        } else if (tar.isOf(ModItems.TAR) && tar.getCount() < tar.getMaxCount()) {
            tar.increment(1);
        }
    }

    /** What a slot will accept, which the screen handler and hoppers both honour. */
    public static boolean isValidFor(int slot, ItemStack stack, World world) {
        return switch (slot) {
            case ORGANIC_SLOT -> stack.isIn(ModTags.REFINERY_ORGANIC);
            case OIL_SLOT -> oilIn(stack) > 0;
            case CATALYST_SLOT -> stack.isIn(ModTags.REFINERY_CATALYST)
                    || stack.isIn(ModTags.REFINERY_DIESEL_CATALYST);
            case CAN_SLOT -> stack.isOf(ModItems.JERRY_CAN);
            default -> false;
        };
    }

    public static void tick(World world, BlockPos pos, BlockState state, FuelRefineryBlockEntity refinery) {
        if (!me.trossty.car.block.MultiblockBlock.checkIntact(world, pos, state)) {
            return;
        }
        boolean wasWorking = refinery.working;
        boolean dirty = false;
        boolean worked = false;

        // --- somebody else's crude oil, straight into the feedstock tank ---
        ItemStack oil = refinery.inventory.get(OIL_SLOT);
        int carried = oilIn(oil);
        if (carried > 0 && refinery.biomass + carried <= BIOMASS_CAPACITY
                && (refinery.biomass == 0 || refinery.feed == FEED_CRUDE)) {
            ItemStack emptied = oil.getItem().getRecipeRemainder();
            if (oil.getCount() == 1 || emptied.isEmpty()) {
                refinery.biomass += carried;
                refinery.feed = FEED_CRUDE;
                oil.decrement(1);
                if (oil.isEmpty()) {
                    // The bucket is still a bucket; only what was in it is gone.
                    refinery.inventory.set(OIL_SLOT, emptied);
                }
                dirty = true;
            }
        }

        // --- stage one: the boiler cooks organic matter down to feedstock ---
        ItemStack organic = refinery.inventory.get(ORGANIC_SLOT);
        boolean canDistill = !organic.isEmpty()
                && (refinery.biomass == 0 || refinery.feed == FEED_BIOMASS)
                && refinery.biomass + BIOMASS_PER_ITEM <= BIOMASS_CAPACITY;

        if (canDistill && refinery.draw(DISTILL_POWER)) {
            worked = true;
            refinery.distillProgress++;
            if (refinery.distillProgress >= DISTILL_TIME) {
                refinery.distillProgress = 0;
                organic.decrement(1);
                refinery.biomass += BIOMASS_PER_ITEM;
                refinery.feed = FEED_BIOMASS;
                dirty = true;
            }
        } else if (refinery.distillProgress > 0) {
            // A boiler with the power cut does not hold its place; it cools.
            refinery.distillProgress = Math.max(0, refinery.distillProgress - 2);
        }

        // --- stage two: a catalyst cracks feedstock in the column, hot ---
        ItemStack catalyst = refinery.inventory.get(CATALYST_SLOT);
        net.minecraft.fluid.Fluid making = productOf(refinery.feed, catalyst);
        boolean canRefine = !catalyst.isEmpty()
                && refinery.product.canTake(making)
                && refinery.biomass >= BIOMASS_PER_REFINE
                && refinery.product.room() >= PETROL_PER_REFINE;
        if (canRefine && refinery.temperature < WORKING_TEMPERATURE) {
            // Nothing cracks in a cold column: all the heater can do is warm it.
            if (refinery.draw(PREHEAT_POWER)) {
                worked = true;
            }
            refinery.refineProgress = 0;
        } else if (canRefine && refinery.draw(REFINE_POWER)) {
            worked = true;
            refinery.refineProgress += refinery.temperature >= HOT_TEMPERATURE ? 2 : 1;
            if (refinery.refineProgress >= REFINE_TIME) {
                refinery.refineProgress = 0;
                refinery.biomass -= BIOMASS_PER_REFINE;
                refinery.product.fill(making, PETROL_PER_REFINE);
                catalyst.decrement(1);
                if (refinery.feed == FEED_CRUDE) {
                    refinery.leaveTar();
                }
                dirty = true;
            }
        } else if (!canRefine) {
            refinery.refineProgress = 0;
        }

        // --- the column keeps what heat it is given, and loses the rest ---
        int before = refinery.temperature;
        refinery.temperature = worked
                ? Math.min(MAX_TEMPERATURE, refinery.temperature + WARM_RATE)
                : Math.max(0, refinery.temperature - COOL_RATE);
        if (worked || before != refinery.temperature) {
            dirty = true;
        }
        refinery.working = worked;

        // --- product out into a can. Biofuel is ready to burn as it comes; raw
        // petrol and diesel go in a can marked raw, which a car will run on but
        // its engine will not thank anyone for: the hydrotreater is the proper road.
        ItemStack can = refinery.inventory.get(CAN_SLOT);
        CarSetup.Fuel canFuel = ModFluids.fuelOf(cleanOf(refinery.product.getFluid()));
        boolean raw = refinery.product.getFluid() != ModFluids.BIOFUEL;
        if (canFuel != null && !refinery.product.isEmpty()
                && can.isOf(ModItems.JERRY_CAN) && !me.trossty.car.item.JerryCans.isFull(can)
                && (me.trossty.car.item.JerryCans.isEmpty(can)
                        || CarEntity.fuelIn(can) == canFuel && CarEntity.isRaw(can) == raw)) {
            int perUnit = PETROL_PER_POUR / me.trossty.car.item.JerryCans.POUR;
            int units = Math.min(Math.min(me.trossty.car.item.JerryCans.POUR,
                    me.trossty.car.item.JerryCans.room(can)), refinery.product.getUnits() / perUnit);
            if (units > 0 && (units == me.trossty.car.item.JerryCans.room(can)
                    || units == me.trossty.car.item.JerryCans.POUR)) {
                refinery.product.drain(units * perUnit);
                me.trossty.car.item.JerryCans.fill(can, units);
                CarEntity.markFuel(can, canFuel);
                CarEntity.markRaw(can, raw);
                dirty = true;
            }
        }

        if (wasWorking != refinery.working
                && state.getBlock() instanceof me.trossty.car.block.MultiblockBlock plant) {
            dirty = true;
            plant.setLit(world, pos, state, refinery.working);
        }
        if (dirty) {
            refinery.markDirty();
        }
    }

    // ---- plumbing ----

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.car.fuel_refinery");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FuelRefineryScreenHandler(syncId, playerInventory, this, this.properties);
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.inventory;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> stacks) {
        this.inventory = stacks;
    }

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return isValidFor(slot, stack, this.getWorld());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        Inventories.readData(view, this.inventory);
        this.energy.amount = Math.max(0, Math.min(ENERGY_CAPACITY, view.getInt("Energy", 0)));
        this.temperature = view.getInt("Temperature", 0);
        this.distillProgress = view.getInt("DistillProgress", 0);
        this.refineProgress = view.getInt("RefineProgress", 0);
        this.biomass = view.getInt("Biomass", 0);
        this.feed = view.getInt("Feed", FEED_BIOMASS);
        this.product.read(view, "Product");
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        view.putInt("Energy", (int) this.energy.amount);
        view.putInt("Temperature", this.temperature);
        view.putInt("DistillProgress", this.distillProgress);
        view.putInt("RefineProgress", this.refineProgress);
        view.putInt("Biomass", this.biomass);
        view.putInt("Feed", this.feed);
        this.product.write(view, "Product");
    }
}
