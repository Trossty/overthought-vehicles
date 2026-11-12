package me.trossty.car.block.entity;

import me.trossty.car.block.BatteryBankBlock;
import me.trossty.car.energy.OneWayEnergy;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModComponents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * The store between the generators and whatever needs power after they stop:
 * a night's worth of a coal generator, and the buffer a charging post draws a
 * car's whole battery from in one go.
 *
 * <p>Five faces take energy in and never give it back; the front gives it out
 * and never takes. So a cable feeding the back and another leading off the
 * front make a line with the bank in it, not a loop the charge sloshes round.
 * Broken, it keeps its charge in the item.
 */
public class BatteryBankBlockEntity extends BlockEntity implements me.trossty.car.machine.GaugeSource {

    public static final long CAPACITY = 200_000;
    /** The most it takes in, and the most it gives out, a tick. */
    public static final long RATE = 500;

    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, RATE, RATE) {
        @Override
        protected void onFinalCommit() {
            BatteryBankBlockEntity.this.markDirty();
        }
    };
    private final EnergyStorage input = OneWayEnergy.in(this.energy);
    private final EnergyStorage output = OneWayEnergy.out(this.energy);

    /** Where the amount stood at the end of the last tick, to tell what came in since. */
    private long lastAmount = -1;
    private int flowIn;
    private int flowOut;

    public BatteryBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_BANK, pos, state);
    }

    /** What a neighbour on this face sees: the front gives, the rest take. */
    public EnergyStorage sideStorage(Direction side) {
        BlockState state = this.getCachedState();
        Direction front = state.contains(BatteryBankBlock.FACING) ? state.get(BatteryBankBlock.FACING) : Direction.NORTH;
        return side == front ? this.output : this.input;
    }

    public long getEnergy() {
        return this.energy.amount;
    }

    public void setEnergy(long amount) {
        this.energy.amount = Math.max(0, Math.min(CAPACITY, amount));
        this.markDirty();
    }

    public int getFlowIn() {
        return this.flowIn;
    }

    public int getFlowOut() {
        return this.flowOut;
    }

    public static void tick(World world, BlockPos pos, BlockState state, BatteryBankBlockEntity bank) {
        long before = bank.energy.amount;
        bank.flowIn = bank.lastAmount < 0 ? 0 : (int) Math.max(0, before - bank.lastAmount);

        long moved = 0;
        Direction front = state.get(BatteryBankBlock.FACING);
        if (bank.energy.amount > 0) {
            EnergyStorage target = EnergyStorage.SIDED.find(world, pos.offset(front), front.getOpposite());
            if (target != null && target.supportsInsertion()) {
                moved = EnergyStorageUtil.move(bank.output, target, RATE, null);
            }
        }
        bank.flowOut = (int) moved;
        bank.lastAmount = bank.energy.amount;

        int level = levelOf(bank.energy.amount);
        if (state.get(BatteryBankBlock.LEVEL) != level) {
            world.setBlockState(pos, state.with(BatteryBankBlock.LEVEL, level), Block.NOTIFY_LISTENERS);
        }
        if (moved > 0 || bank.flowIn > 0) {
            bank.markDirty();
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
    protected void readData(ReadView view) {
        super.readData(view);
        this.energy.amount = Math.max(0, Math.min(CAPACITY, view.getLong("Energy", 0L)));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putLong("Energy", this.energy.amount);
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        Long stored = components.get(ModComponents.STORED_ENERGY);
        if (stored != null) {
            this.energy.amount = Math.max(0, Math.min(CAPACITY, stored));
        }
    }

    @Override
    protected void addComponents(ComponentMap.Builder builder) {
        super.addComponents(builder);
        if (this.energy.amount > 0) {
            builder.add(ModComponents.STORED_ENERGY, this.energy.amount);
        }
    }

    @Override
    public void removeFromCopiedStackData(WriteView view) {
        super.removeFromCopiedStackData(view);
        view.remove("Energy");
    }
}
