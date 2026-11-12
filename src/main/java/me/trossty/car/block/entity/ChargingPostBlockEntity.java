package me.trossty.car.block.entity;

import java.util.Comparator;
import me.trossty.car.block.ChargingPostBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.machine.GaugeSource;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

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

    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, INPUT, 0) {
        @Override
        protected void onFinalCommit() {
            ChargingPostBlockEntity.this.markDirty();
        }
    };

    private boolean carPresent;
    private int carChargePermille;
    private int lastRate;

    public ChargingPostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_POST, pos, state);
    }

    public EnergyStorage getEnergyStorage() {
        return this.energy;
    }

    public long getEnergy() {
        return this.energy.amount;
    }

    public void setEnergy(long amount) {
        this.energy.amount = Math.max(0, Math.min(CAPACITY, amount));
        this.markDirty();
    }

    /** The electric car nearest the post, within reach; null when none is parked there. */
    public static @Nullable CarEntity nearestElectric(World world, BlockPos pos) {
        Box reach = new Box(pos).expand(REACH);
        return world.getEntitiesByClass(CarEntity.class, reach, car -> car.isAlive() && car.isElectric())
                .stream()
                .min(Comparator.comparingDouble(car -> car.squaredDistanceTo(pos.toCenterPos())))
                .orElse(null);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ChargingPostBlockEntity post) {
        CarEntity car = world.getTime() % 5 == 0 || post.carPresent ? nearestElectric(world, pos) : null;
        int rate = 0;
        if (car != null) {
            float room = car.getMaxFuel() - car.getFuel();
            long wanted = (long) Math.ceil(room * CarEntity.ENERGY_PER_CHARGE_UNIT);
            long moved = Math.min(OUTPUT, Math.min(post.energy.amount, wanted));
            if (moved > 0) {
                car.charge(moved / (float) CarEntity.ENERGY_PER_CHARGE_UNIT);
                post.energy.amount -= moved;
                post.markDirty();
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
        if (state.get(ChargingPostBlock.LIT) != lit) {
            world.setBlockState(pos, state.with(ChargingPostBlock.LIT, lit), Block.NOTIFY_LISTENERS);
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
    protected void readData(ReadView view) {
        super.readData(view);
        this.energy.amount = Math.max(0, Math.min(CAPACITY, view.getLong("Energy", 0L)));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putLong("Energy", this.energy.amount);
    }
}
