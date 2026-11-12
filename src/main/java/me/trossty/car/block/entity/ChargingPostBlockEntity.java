package me.trossty.car.block.entity;

import java.util.Comparator;
import me.trossty.car.block.ChargingPostBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.machine.GaugeSource;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.energy.IEnergyStorage;
import me.trossty.car.energy.EnergyBuffer;

/**
 * Takes power off a cable into a small buffer and pours it into the nearest
 * electric vehicle within reach of its lead. A full charge of a city car is a
 * few minutes on a coal generator, under a minute off a full battery bank.
 */
public class ChargingPostBlockEntity extends BlockEntity implements GaugeSource {

    public static final long CAPACITY = 20_000;
    /** Most a cable can push in a tick. */
    public static final long INPUT = 400;
    /** Most it puts into a car a tick. */
    public static final long OUTPUT = 200;
    /** How far its lead reaches, from the post's centre. */
    public static final double REACH = 3.5;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, INPUT, 0) {
        @Override
        protected void onChanged() {
            ChargingPostBlockEntity.this.setChanged();
        }
    };

    private boolean carPresent;
    private int carChargePermille;
    private int lastRate;

    public ChargingPostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_POST, pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energy;
    }

    public long getEnergy() {
        return this.energy.amount;
    }

    public void setEnergy(long amount) {
        this.energy.amount = Math.max(0, Math.min(CAPACITY, amount));
        this.setChanged();
    }

    /** The electric car nearest the post, within reach; null when none is parked there. */
    public static @Nullable CarEntity nearestElectric(Level world, BlockPos pos) {
        AABB reach = new AABB(pos).inflate(REACH);
        return world.getEntitiesOfClass(CarEntity.class, reach, car -> car.isAlive() && car.isElectric())
                .stream()
                .min(Comparator.comparingDouble(car -> car.distanceToSqr(pos.getCenter())))
                .orElse(null);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, ChargingPostBlockEntity post) {
        CarEntity car = world.getGameTime() % 5 == 0 || post.carPresent ? nearestElectric(world, pos) : null;
        int rate = 0;
        if (car != null) {
            float room = car.getMaxFuel() - car.getFuel();
            long wanted = (long) Math.ceil(room * CarEntity.ENERGY_PER_CHARGE_UNIT);
            long moved = Math.min(OUTPUT, Math.min(post.energy.amount, wanted));
            if (moved > 0) {
                car.charge(moved / (float) CarEntity.ENERGY_PER_CHARGE_UNIT);
                post.energy.amount -= moved;
                post.setChanged();
                rate = (int) moved;
            }
            float max = car.getMaxFuel();
            post.carChargePermille = max <= 0 ? 0 : Math.round(car.getFuel() * 1000 / max);
        } else {
            post.carChargePermille = 0;
        }
        post.carPresent = car != null;
        post.lastRate = rate;

        boolean lit = rate > 0;
        if (state.getValue(ChargingPostBlock.LIT) != lit) {
            world.setBlock(pos, state.setValue(ChargingPostBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    // ---- the gauge window ----

    @Override
    public int gaugeKind() {
        return KIND_CHARGER;
    }

    @Override
    public long gaugeEnergy() {
        return this.energy.amount;
    }

    @Override
    public long gaugeEnergyCapacity() {
        return CAPACITY;
    }

    /** 0 with no car beside it, else 1 plus what it is putting in a tick. */
    @Override
    public int gaugeExtra() {
        return this.carPresent ? 1 + this.lastRate : 0;
    }

    /** The parked car's charge, in tenths of a percent. */
    @Override
    public int gaugeExtra2() {
        return this.carChargePermille;
    }

    @Override
    public boolean gaugeWorking() {
        return this.lastRate > 0;
    }

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
}
