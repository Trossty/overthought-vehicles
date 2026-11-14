package me.trossty.car.block.entity;

import me.trossty.car.block.GeneratorBlock;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.neoforged.neoforge.energy.IEnergyStorage;
import me.trossty.car.energy.EnergyBuffer;

/**
 * Where the electricity comes from, if nothing better is about.
 *
 * <p>Three of them, all the size of a house's standby set and all wired the
 * same way — whatever is in the buffer is offered every tick to whatever
 * stands against the block, through the same energy API the tech mods speak:
 *
 * <ul>
 *   <li>the <b>coal</b> one burns what a furnace burns, from its firebox;</li>
 *   <li>the <b>biofuel</b> one runs on biofuel out of its own tank: more power
 *       than coal, but it drinks;</li>
 *   <li>the <b>diesel</b> one runs on clean diesel: the most power, and the
 *       most out of every unit.</li>
 * </ul>
 *
 * <p>An empty hand opens the gauge window (the coal one's has its firebox
 * slot). Coal also goes in by hand or by hopper; liquid by a jerry can held
 * against it, a tanker's hose or another mod's pipe.
 */
public class GeneratorBlockEntity extends BlockEntity implements Container, me.trossty.car.machine.GaugeSource {

    public static final int CAPACITY = 20_000;

    /** What the coal one makes for every tick something is burning. */
    public static final int OUTPUT = 20;

    /** The liquid ones' tank, in units. */
    public static final int TANK_CAPACITY = 2_000;

    /** Which generator this is. */
    public enum Kind {
        /** Furnace fuel in a firebox. */
        COAL(20, 0, null),
        /** Biofuel: 30 a tick, a unit every three ticks — 90 a unit. */
        BIOFUEL(30, 3, me.trossty.car.entity.CarSetup.Fuel.BIOFUEL),
        /** Diesel: 40 a tick, a unit every four ticks — 160 a unit. */
        DIESEL(40, 4, me.trossty.car.entity.CarSetup.Fuel.DIESEL);

        /** Energy per tick while running. */
        public final int output;
        /** Ticks one unit of liquid lasts; 0 for the coal one. */
        public final int ticksPerUnit;
        /** What liquid it runs on; null for the coal one. */
        public final me.trossty.car.entity.CarSetup.@org.jetbrains.annotations.Nullable Fuel fuel;

        Kind(int output, int ticksPerUnit, me.trossty.car.entity.CarSetup.@org.jetbrains.annotations.Nullable Fuel fuel) {
            this.output = output;
            this.ticksPerUnit = ticksPerUnit;
            this.fuel = fuel;
        }

        public boolean burnsLiquid() {
            return this.fuel != null;
        }

        public net.minecraft.world.level.material.Fluid fluid() {
            return this.fuel == null ? net.minecraft.world.level.material.Fluids.EMPTY : me.trossty.car.registry.ModFluids.fluidOf(this.fuel);
        }
    }

    /** The most it will push into one neighbour, or let a cable pull, a tick. */
    public static final int MAX_TRANSFER = 200;

    private final Kind kind;
    private final me.trossty.car.fluid.UnitTank tank;
    private ItemStack fuel = ItemStack.EMPTY;
    private int burnTime;
    private int burnTimeTotal;
    /** Lit but waiting for room in the buffer; worked out afresh every tick. */
    private boolean banked;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 0, MAX_TRANSFER) {
        @Override
        protected void onChanged() {
            GeneratorBlockEntity.this.setChanged();
        }
    };

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR, pos, state);
        this.kind = state.getBlock() instanceof GeneratorBlock block ? block.getKind() : Kind.COAL;
        net.minecraft.world.level.material.Fluid liquid = this.kind.fluid();
        this.tank = new me.trossty.car.fluid.UnitTank(this.kind.burnsLiquid() ? TANK_CAPACITY : 0,
                fluid -> fluid == liquid, true, false, this::setChanged);
    }

    public Kind getKind() {
        return this.kind;
    }

    /** The liquid ones' tank; the coal one has an empty, closed one. */
    public me.trossty.car.fluid.UnitTank getTank() {
        return this.tank;
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energy;
    }

    public int getEnergy() {
        return (int) this.energy.amount;
    }

    public void setEnergy(int amount) {
        this.energy.amount = Math.max(0, Math.min(CAPACITY, amount));
    }

    /** Burning, as opposed to idle or banked because the buffer is full. */
    public boolean isBurning() {
        return this.burnTime > 0 && !this.banked;
    }

    public int getBurnTime() {
        return this.burnTime;
    }

    /** How long this would burn in the coal one's firebox; nothing if it would not. */
    public static int burnTicks(ItemStack stack, Level world) {
        if (stack.isEmpty() || stack.is(ModItems.JERRY_CAN)) {
            return 0;
        }
        return world == null ? 0 : stack.getBurnTime(null);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, GeneratorBlockEntity generator) {
        boolean wasBurning = generator.isBurning();
        boolean dirty = false;

        int output = (int) Math.round(generator.kind.output * me.trossty.car.CarConfig.get().generatorOutput);
        // A full generator idles: a lit piece waits, banked, until something draws
        // power off, and the next piece is lit only once there is room for it.
        boolean room = generator.energy.amount + output <= CAPACITY;
        generator.banked = !room;
        if (generator.burnTime > 0 && room) {
            generator.burnTime--;
            generator.energy.amount += output;
            dirty = true;
        }

        if (generator.kind.burnsLiquid()) {
            if (generator.burnTime == 0 && room && generator.tank.drain(1) > 0) {
                generator.burnTime = generator.kind.ticksPerUnit;
                generator.burnTimeTotal = generator.kind.ticksPerUnit;
                dirty = true;
            }
        } else if (generator.burnTime == 0 && room) {
            int ticks = burnTicks(generator.fuel, world);
            if (ticks > 0) {
                generator.burnTime = ticks;
                generator.burnTimeTotal = ticks;
                ItemStack left = generator.fuel.getCraftingRemainingItem();
                generator.fuel.shrink(1);
                if (generator.fuel.isEmpty()) {
                    generator.fuel = left;
                }
                dirty = true;
            }
        }

        // Offer what is in the buffer to everything standing against the block.
        if (generator.energy.amount > 0) {
            for (Direction side : Direction.values()) {
                IEnergyStorage target = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos.relative(side), side.getOpposite());
                if (target != null && target.canReceive()) {
                    if (EnergyBuffer.move(generator.energy, target, MAX_TRANSFER) > 0) {
                        dirty = true;
                    }
                }
            }
        }

        if (wasBurning != generator.isBurning()) {
            world.setBlock(pos, state.setValue(GeneratorBlock.LIT, generator.isBurning()), 3);
            dirty = true;
        }
        if (dirty) {
            generator.setChanged();
        }
    }

    // ---- the gauge window ----

    @Override
    public int gaugeKind() {
        return switch (this.kind) {
            case COAL -> KIND_COAL_GENERATOR;
            case BIOFUEL -> KIND_BIOFUEL_GENERATOR;
            case DIESEL -> KIND_DIESEL_GENERATOR;
        };
    }

    @Override
    public long gaugeEnergy() {
        return this.energy.amount;
    }

    @Override
    public long gaugeEnergyCapacity() {
        return CAPACITY;
    }

    @Override
    public me.trossty.car.fluid.@org.jetbrains.annotations.Nullable UnitTank gaugeTankA() {
        return this.kind.burnsLiquid() ? this.tank : null;
    }

    /** What is left of the piece or unit burning now. */
    @Override
    public int gaugeProgress() {
        return this.burnTimeTotal <= 0 ? 0 : this.burnTime * 1000 / this.burnTimeTotal;
    }

    /** Seconds it can keep running on what it holds. */
    @Override
    public int gaugeExtra() {
        if (this.kind.burnsLiquid()) {
            return (this.tank.getUnits() * this.kind.ticksPerUnit + this.burnTime) / 20;
        }
        int queued = this.fuel.isEmpty() || this.level == null ? 0
                : burnTicks(this.fuel, this.level) * this.fuel.getCount();
        return (queued + this.burnTime) / 20;
    }

    /** Energy made per tick while running. */
    @Override
    public int gaugeExtra2() {
        return this.kind.output;
    }

    @Override
    public boolean gaugeWorking() {
        return this.isBurning();
    }

    // ---- the one slot, so a hopper can keep it fed ----

    @Override
    public int getContainerSize() {
        return this.kind.burnsLiquid() ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
        return this.fuel.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? this.fuel : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || this.fuel.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = this.fuel.split(amount);
        this.setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = this.fuel;
        this.fuel = ItemStack.EMPTY;
        this.setChanged();
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            this.fuel = stack;
            this.setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && !this.kind.burnsLiquid() && burnTicks(stack, this.getLevel()) > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.fuel = ItemStack.EMPTY;
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.fuel = view.read("Fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.burnTime = view.getIntOr("BurnTime", 0);
        this.burnTimeTotal = view.getIntOr("BurnTimeTotal", 0);
        this.energy.amount = Math.max(0, Math.min(CAPACITY, view.getIntOr("Energy", 0)));
        this.tank.read(view, "Tank");
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        if (!this.fuel.isEmpty()) {
            view.store("Fuel", ItemStack.CODEC, this.fuel);
        }
        view.putInt("BurnTime", this.burnTime);
        view.putInt("BurnTimeTotal", this.burnTimeTotal);
        view.putInt("Energy", (int) this.energy.amount);
        this.tank.write(view, "Tank");
    }
}
