package me.trossty.car.block.entity;

import me.trossty.car.block.BatteryBankBlock;
import me.trossty.car.energy.OneWayEnergy;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.neoforged.neoforge.energy.IEnergyStorage;
import me.trossty.car.energy.EnergyBuffer;

/**
 * The store between the generators and whatever needs power after they stop:
 * a night's worth of a coal generator, and the buffer a charging post draws a
 * car's whole battery from in one go.
 *
 * <p>The front only gives, and pushes out of its own accord, so a cable feeding
 * the back and another leading off the front make a line with the bank in it.
 * The other five faces are an ordinary store: on a run it fills once the
 * machines have had theirs and gives back to them when the generators stop.
 * Broken, it keeps its charge in the item.
 */
public class BatteryBankBlockEntity extends BlockEntity implements me.trossty.car.machine.GaugeSource {

    public static final long CAPACITY = 200_000;
    /** The most it takes in, and the most it gives out, a tick. */
    public static final long RATE = 500;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, RATE, RATE) {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int accepted = super.receiveEnergy(toReceive, simulate);
            if (!simulate) {
                BatteryBankBlockEntity.this.cameIn += accepted;
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int given = super.extractEnergy(toExtract, simulate);
            if (!simulate) {
                BatteryBankBlockEntity.this.wentOut += given;
            }
            return given;
        }

        @Override
        protected void onChanged() {
            BatteryBankBlockEntity.this.setChanged();
        }
    };
    private final IEnergyStorage output = OneWayEnergy.out(this.energy);

    /** Moved in and out since the last tick, by anyone. */
    private int cameIn;
    private int wentOut;
    private int flowIn;
    private int flowOut;

    public BatteryBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_BANK, pos, state);
    }

    /** What a neighbour on this face sees: the front only gives, the rest both take and give. */
    public IEnergyStorage sideStorage(Direction side) {
        BlockState state = this.getBlockState();
        Direction front = state.hasProperty(BatteryBankBlock.FACING) ? state.getValue(BatteryBankBlock.FACING) : Direction.NORTH;
        return side == front ? this.output : this.energy;
    }

    public long getEnergy() {
        return this.energy.amount;
    }

    public void setEnergy(long amount) {
        this.energy.amount = Math.max(0, Math.min(CAPACITY, amount));
        this.setChanged();
    }

    public int getFlowIn() {
        return this.flowIn;
    }

    public int getFlowOut() {
        return this.flowOut;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, BatteryBankBlockEntity bank) {
        long moved = 0;
        Direction front = state.getValue(BatteryBankBlock.FACING);
        if (bank.energy.amount > 0) {
            IEnergyStorage target = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos.relative(front), front.getOpposite());
            if (target != null && target.canReceive()) {
                moved = EnergyBuffer.move(bank.output, target, RATE);
            }
        }
        bank.flowIn = bank.cameIn;
        bank.flowOut = bank.wentOut;
        bank.cameIn = 0;
        bank.wentOut = 0;

        int level = levelOf(bank.energy.amount);
        if (state.getValue(BatteryBankBlock.LEVEL) != level) {
            world.setBlock(pos, state.setValue(BatteryBankBlock.LEVEL, level), Block.UPDATE_CLIENTS);
        }
        if (moved > 0) {
            bank.setChanged();
        }
    }

    /** How many of the four bars are lit: the first as soon as there is anything in it. */
    public static int levelOf(long amount) {
        if (amount <= 0) {
            return 0;
        }
        return (int) Math.max(1, Math.min(4, Math.round(amount * 4.0 / CAPACITY)));
    }

    // ---- the gauge window ----

    @Override
    public int gaugeKind() {
        return KIND_BATTERY;
    }

    @Override
    public long gaugeEnergy() {
        return this.energy.amount;
    }

    @Override
    public long gaugeEnergyCapacity() {
        return CAPACITY;
    }

    /** Out a tick. */
    @Override
    public int gaugeExtra() {
        return this.flowOut;
    }

    /** In a tick. */
    @Override
    public int gaugeExtra2() {
        return this.flowIn;
    }

    @Override
    public boolean gaugeWorking() {
        return this.flowOut > 0;
    }

    // ---- saving, and the charge riding along in the item ----

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.energy.amount = Math.max(0, Math.min(CAPACITY, view.getLongOr("Energy", 0L)));
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        view.putLong("Energy", this.energy.amount);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        super.applyImplicitComponents(components);
        Long stored = components.get(ModComponents.STORED_ENERGY);
        if (stored != null) {
            this.energy.amount = Math.max(0, Math.min(CAPACITY, stored));
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (this.energy.amount > 0) {
            builder.set(ModComponents.STORED_ENERGY, this.energy.amount);
        }
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag tag) {
        ValueOutput view = ValueOutput.of(tag, null);
        super.removeComponentsFromTag(tag);
        view.discard("Energy");
    }
}
