package me.trossty.car.block.entity;

import me.trossty.car.registry.ModBlockEntities;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.neoforged.neoforge.energy.IEnergyStorage;
import me.trossty.car.energy.EnergyBuffer;

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

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, RATE, RATE) {
        @Override
        protected void onChanged() {
            EnergyCableBlockEntity.this.setChanged();
        }
    };

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE, pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energy;
    }

    public int getEnergy() {
        return (int) this.energy.amount;
    }

    public void setEnergy(int amount) {
        this.energy.amount = Math.max(0, Math.min(CAPACITY, amount));
        this.setChanged();
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
    private final BlockCapabilityCache<IEnergyStorage, Direction>[] neighbours = new BlockCapabilityCache[6];

    private BlockCapabilityCache<IEnergyStorage, Direction> neighbour(ServerLevel world, Direction side) {
        BlockCapabilityCache<IEnergyStorage, Direction> cache = this.neighbours[side.ordinal()];
        if (cache == null) {
            cache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, world,
                    this.worldPosition.relative(side), side.getOpposite());
            this.neighbours[side.ordinal()] = cache;
        }
        return cache;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, EnergyCableBlockEntity cable) {
        // Spread over the ten ticks by position, so a long line does not all update at once.
        if (Math.floorMod(world.getGameTime() + pos.hashCode(), GLOW_EVERY) == 0) {
            cable.updateGlow(world, pos, state);
        }
        if (cable.energy.amount <= 0 || !(world instanceof ServerLevel server)) {
            return;
        }
        // Machines first: whatever stands against the cable and takes power.
        EnergyCableBlockEntity[] cables = new EnergyCableBlockEntity[6];
        for (Direction side : Direction.values()) {
            BlockCapabilityCache<IEnergyStorage, Direction> cache = cable.neighbour(server, side);
            if (server.getBlockEntity(cache.pos()) instanceof EnergyCableBlockEntity other) {
                cables[side.ordinal()] = other;
                continue;
            }
            if (cable.energy.amount <= 0) {
                continue;
            }
            IEnergyStorage target = cache.getCapability();
            if (target != null && target.canReceive()) {
                EnergyBuffer.move(cable.energy, target, rate());
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
                cable.setChanged();
                other.setChanged();
            }
        }
    }

    private void updateGlow(Level world, BlockPos pos, BlockState state) {
        if (!state.hasProperty(me.trossty.car.block.EnergyCableBlock.POWERED)) {
            return;
        }
        boolean lit = state.getValue(me.trossty.car.block.EnergyCableBlock.POWERED);
        boolean wanted = lit ? this.energy.amount > 0 : this.energy.amount >= GLOW_ON;
        if (wanted != lit) {
            world.setBlock(pos, state.setValue(me.trossty.car.block.EnergyCableBlock.POWERED, wanted),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.energy.amount = Math.max(0, Math.min(CAPACITY, view.getIntOr("Energy", 0)));
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        view.putInt("Energy", (int) this.energy.amount);
    }
}
