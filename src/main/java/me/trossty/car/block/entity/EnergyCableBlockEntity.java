package me.trossty.car.block.entity;

import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * One length of cable, and the charge standing in it.
 *
 * <p>Like the fuel pipe there is no network: each length is a small buffer
 * that, every tick, does two things —
 *
 * <ol>
 *   <li>hands what it holds to every machine against it that will take power
 *       (a generator will not: it only gives), and</li>
 *   <li>levels itself with the cables beside it, half the difference at a
 *       time.</li>
 * </ol>
 *
 * <p>A generator pushes into the first length; a machine drinking at the far
 * end pulls the level down there, and the charge runs downhill towards it.
 * Branches, loops and a length dug out of the middle need no bookkeeping.
 */
public class EnergyCableBlockEntity extends BlockEntity {

    /** What one length holds. */
    public static final int CAPACITY = 1_000;

    /** The most one length passes on, to a machine or along the line, a tick, by default (see {@link #rate()}). */
    public static final int RATE = 250;

    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, RATE, RATE) {
        @Override
        protected void onFinalCommit() {
            EnergyCableBlockEntity.this.markDirty();
        }
    };

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE, pos, state);
    }

    public EnergyStorage getEnergyStorage() {
        return this.energy;
    }

    public int getEnergy() {
        return (int) this.energy.amount;
    }

    public void setEnergy(int amount) {
        this.energy.amount = Math.max(0, Math.min(CAPACITY, amount));
        this.markDirty();
    }

    /** The most one length passes on a tick, as the config has it. */
    public static long rate() {
        return me.trossty.car.CarConfig.get().cableRate;
    }

    /** At or above this the stripe lights; it goes out only when the length is empty. */
    public static final int GLOW_ON = 50;
    /** How often the stripe is looked at, so a flickering line is not a storm of block updates. */
    private static final int GLOW_EVERY = 10;

    /**
     * One lookup cache per side. Finding what is next door is the costly part
     * of a cable's tick; a cache remembers the answer and is told by the world
     * when the neighbour changes, so a long line costs little more than a short one.
     */
    @SuppressWarnings("unchecked")
    private final BlockApiCache<EnergyStorage, Direction>[] neighbours = new BlockApiCache[6];

    private BlockApiCache<EnergyStorage, Direction> neighbour(ServerWorld world, Direction side) {
        BlockApiCache<EnergyStorage, Direction> cache = this.neighbours[side.ordinal()];
        if (cache == null) {
            cache = BlockApiCache.create(EnergyStorage.SIDED, world, this.pos.offset(side));
            this.neighbours[side.ordinal()] = cache;
        }
        return cache;
    }

    public static void tick(World world, BlockPos pos, BlockState state, EnergyCableBlockEntity cable) {
        // Spread over the ten ticks by position, so a long line does not all update at once.
        if (Math.floorMod(world.getTime() + pos.hashCode(), GLOW_EVERY) == 0) {
            cable.updateGlow(world, pos, state);
        }
        if (cable.energy.amount <= 0 || !(world instanceof ServerWorld server)) {
            return;
        }
        // Machines first: whatever stands against the cable and takes power.
        EnergyCableBlockEntity[] cables = new EnergyCableBlockEntity[6];
        for (Direction side : Direction.values()) {
            BlockApiCache<EnergyStorage, Direction> cache = cable.neighbour(server, side);
            if (cache.getBlockEntity() instanceof EnergyCableBlockEntity other) {
                cables[side.ordinal()] = other;
                continue;
            }
            if (cable.energy.amount <= 0) {
                continue;
            }
            EnergyStorage target = cache.find(side.getOpposite());
            if (target != null && target.supportsInsertion()) {
                EnergyStorageUtil.move(cable.energy, target, rate(), null);
            }
        }
        // Then level with the cables either side, downhill only.
        for (EnergyCableBlockEntity other : cables) {
            if (other == null || other.isRemoved()) {
                continue;
            }
            long difference = cable.energy.amount - other.energy.amount;
            if (difference > 1) {
                long moved = Math.min(difference / 2, rate());
                cable.energy.amount -= moved;
                other.energy.amount += moved;
                cable.markDirty();
                other.markDirty();
            }
        }
    }

    private void updateGlow(World world, BlockPos pos, BlockState state) {
        if (!state.contains(me.trossty.car.block.EnergyCableBlock.POWERED)) {
            return;
        }
        boolean lit = state.get(me.trossty.car.block.EnergyCableBlock.POWERED);
        boolean wanted = lit ? this.energy.amount > 0 : this.energy.amount >= GLOW_ON;
        if (wanted != lit) {
            world.setBlockState(pos, state.with(me.trossty.car.block.EnergyCableBlock.POWERED, wanted),
                    net.minecraft.block.Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.energy.amount = Math.max(0, Math.min(CAPACITY, view.getInt("Energy", 0)));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("Energy", (int) this.energy.amount);
    }
}
