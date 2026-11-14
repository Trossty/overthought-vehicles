package me.trossty.car.block.entity;

import me.trossty.car.block.MultiblockBlock;
import me.trossty.car.fluid.FluidSource;
import me.trossty.car.fluid.UnitTank;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import org.jetbrains.annotations.Nullable;
import me.trossty.car.energy.EnergyBuffer;

/**
 * What the pumpjack keeps: the crude it has brought up, the electricity it
 * runs on, and the deposit block it is drawing from.
 *
 * <p>It looks straight down from the wellhead, and a couple of blocks to each
 * side, for {@linkplain ModBlocks#OIL_DEPOSIT oil-bearing rock}. Each block of
 * it gives {@link #CRUDE_PER_DEPOSIT} units and is then spent: it turns back
 * into the stone or deepslate around it, so a deposit runs dry in the end and
 * the jack has to move on. Crude goes out by itself into anything that will
 * take it standing against any part of the jack — a refinery bolted on, or
 * another mod's pipe — and into a tanker's hose.
 */
public class PumpjackBlockEntity extends BlockEntity implements FluidSource, me.trossty.car.machine.GaugeSource {

    public static final int TANK_CAPACITY = 8_000;
    public static final int ENERGY_CAPACITY = 4_000;
    /** Electricity per tick while it pumps. */
    public static final int ENERGY_PER_TICK = 8;
    /** Crude per tick while it pumps. */
    public static final int RATE = 10;
    /** What one block of deposit is worth. */
    public static final int CRUDE_PER_DEPOSIT = 1_000;
    /** How far out to each side of the wellhead it looks. */
    public static final int REACH = 2;
    /** How long to wait before looking again after finding nothing. */
    private static final int IDLE_SCAN = 200;

    private final UnitTank crude = new UnitTank(TANK_CAPACITY,
            fluid -> fluid == ModFluids.CRUDE_OIL, false, true, this::setChanged);

    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_CAPACITY, 200, 0) {
        @Override
        protected void onChanged() {
            PumpjackBlockEntity.this.setChanged();
        }
    };

    private @Nullable BlockPos target;
    /** What is left in the target block. */
    private int left;
    private int scanCooldown;
    private boolean working;

    public PumpjackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMPJACK, pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, PumpjackBlockEntity jack) {
        if (!MultiblockBlock.checkIntact(world, pos, state)) {
            return;
        }
        boolean dirty = false;

        if (jack.target != null && !world.getBlockState(jack.target).is(ModBlocks.OIL_DEPOSIT)) {
            jack.target = null;
        }
        if (jack.target == null && --jack.scanCooldown <= 0) {
            jack.target = findDeposit(world, pos);
            jack.left = CRUDE_PER_DEPOSIT;
            jack.scanCooldown = jack.target == null ? IDLE_SCAN : 0;
            dirty = true;
        }

        boolean wasWorking = jack.working;
        int batch = Math.min(RATE, Math.min(jack.left, jack.crude.room()));
        jack.working = jack.target != null && batch > 0 && jack.energy.amount >= ENERGY_PER_TICK;
        if (jack.working) {
            jack.energy.amount -= ENERGY_PER_TICK;
            jack.crude.fill(ModFluids.CRUDE_OIL, batch);
            jack.left -= batch;
            if (jack.left <= 0) {
                spend(world, jack.target);
                jack.target = null;
                jack.scanCooldown = 0;
            }
            dirty = true;
        }

        if (!jack.crude.isEmpty() && pushCrude(world, pos, state, jack)) {
            dirty = true;
        }

        if (wasWorking != jack.working && state.getBlock() instanceof MultiblockBlock machine) {
            machine.setLit(world, pos, state, jack.working);
            dirty = true;
        }
        if (dirty) {
            jack.setChanged();
        }
    }

    /** The nearest deposit block under the wellhead, the shallowest first. */
    public static @Nullable BlockPos findDeposit(Level world, BlockPos wellhead) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int y = wellhead.getY() - 1; y >= world.getMinBuildHeight(); y--) {
            for (int r = 0; r <= REACH; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        at.set(wellhead.getX() + dx, y, wellhead.getZ() + dz);
                        if (world.getBlockState(at).is(ModBlocks.OIL_DEPOSIT)) {
                            return at.immutable();
                        }
                    }
                }
            }
        }
        return null;
    }

    /** A drained block goes back to being the rock it was found in. */
    private static void spend(Level world, BlockPos pos) {
        Block rock = pos.getY() < 0 ? Blocks.DEEPSLATE : Blocks.STONE;
        world.setBlock(pos, rock.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Offers crude to everything touching the jack that is not the jack. */
    private static boolean pushCrude(Level world, BlockPos pos, BlockState state, PumpjackBlockEntity jack) {
        if (!(state.getBlock() instanceof MultiblockBlock machine) || world.getGameTime() % 5 != 0) {
            return false;
        }
        Direction facing = state.getValue(MultiblockBlock.FACING);
        boolean moved = false;
        for (int[] cell : machine.cells()) {
            BlockPos at = MultiblockBlock.cellPos(pos, facing, cell);
            for (Direction side : Direction.values()) {
                BlockPos next = at.relative(side);
                if (world.getBlockState(next).is(state.getBlock())) {
                    continue;
                }
                var other = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                        next, side.getOpposite());
                if (other != null
                        && me.trossty.car.fluid.Liquids.move(jack.crude, other, null, RATE * 5 * ModFluids.UNIT) > 0) {
                    moved = true;
                }
                if (jack.crude.isEmpty()) {
                    return moved;
                }
            }
        }
        return moved;
    }

    // ---- the gauge window ----

    private int reserveSeen;
    private long reserveCheckedAt = Long.MIN_VALUE;

    @Override
    public int gaugeKind() {
        return KIND_PUMPJACK;
    }

    @Override
    public long gaugeEnergy() {
        return this.energy.amount;
    }

    @Override
    public long gaugeEnergyCapacity() {
        return ENERGY_CAPACITY;
    }

    @Override
    public UnitTank gaugeTankA() {
        return this.crude;
    }

    /** How much of the block being drawn on is gone. */
    @Override
    public int gaugeProgress() {
        return this.target == null ? 0 : (CRUDE_PER_DEPOSIT - this.left) * 1000 / CRUDE_PER_DEPOSIT;
    }

    /** Crude left within reach; looked up once a second, not every sync. */
    @Override
    public int gaugeExtra() {
        long now = this.level == null ? 0 : this.level.getGameTime();
        if (now - this.reserveCheckedAt >= 20) {
            this.reserveSeen = this.estimateReserve();
            this.reserveCheckedAt = now;
        }
        return this.reserveSeen;
    }

    @Override
    public boolean gaugeWorking() {
        return this.working;
    }

    /** Roughly how much crude is left within reach, for the gauge. */
    public int estimateReserve() {
        if (this.level == null) {
            return 0;
        }
        int blocks = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int y = this.worldPosition.getY() - 1; y >= this.level.getMinBuildHeight(); y--) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    at.set(this.worldPosition.getX() + dx, y, this.worldPosition.getZ() + dz);
                    if (this.level.getBlockState(at).is(ModBlocks.OIL_DEPOSIT)) {
                        blocks++;
                    }
                }
            }
        }
        if (blocks == 0) {
            return 0;
        }
        return (blocks - (this.target != null ? 1 : 0)) * CRUDE_PER_DEPOSIT + (this.target != null ? this.left : 0);
    }

    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidStorage() {
        return this.crude;
    }

    public EnergyBuffer getEnergyStorage() {
        return this.energy;
    }

    public UnitTank getCrude() {
        return this.crude;
    }

    public int getEnergy() {
        return (int) this.energy.amount;
    }

    public void setEnergy(int amount) {
        this.energy.amount = Math.max(0, Math.min(ENERGY_CAPACITY, amount));
        this.setChanged();
    }

    public boolean isWorking() {
        return this.working;
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.crude.read(view, "Crude");
        this.energy.amount = Math.max(0, Math.min(ENERGY_CAPACITY, view.getIntOr("Energy", 0)));
        this.left = view.getIntOr("Left", 0);
        this.working = view.getBooleanOr("Working", false);
        this.target = view.read("Target", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        this.crude.write(view, "Crude");
        view.putInt("Energy", (int) this.energy.amount);
        view.putInt("Left", this.left);
        view.putBoolean("Working", this.working);
        if (this.target != null) {
            view.store("Target", BlockPos.CODEC, this.target);
        }
    }
}
