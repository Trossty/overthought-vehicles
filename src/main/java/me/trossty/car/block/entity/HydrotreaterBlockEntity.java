package me.trossty.car.block.entity;

import me.trossty.car.block.MultiblockBlock;
import me.trossty.car.fluid.FluidSource;
import me.trossty.car.fluid.PetrolSource;
import me.trossty.car.fluid.UnitTank;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

/**
 * The last step for fossil fuel: raw petrol and raw diesel straight off the
 * refinery's column still carry the sulphur and the heavy ends that foul an
 * engine. The hydrotreater cooks them under hydrogen on electricity and hands
 * out the clean fuel a car will take.
 *
 * <p>It draws raw fuel by itself out of a refinery it stands against, or takes
 * it from a pipe or a tanker's hose; the clean fuel leaves by a jerry can
 * held against it, by the station's own petrol pipes, by a tanker or by
 * another mod's pipe. Biofuel needs none of this and never comes here.
 */
public class HydrotreaterBlockEntity extends BlockEntity implements FluidSource, PetrolSource, me.trossty.car.machine.GaugeSource {

    public static final int TANK_CAPACITY = 4_000;
    public static final int ENERGY_CAPACITY = 4_000;
    /** Electricity per tick while it works. */
    public static final int ENERGY_PER_TICK = 12;
    /** Units cleaned per tick. */
    public static final int RATE = 4;
    /** Units drawn from a neighbouring refinery per tick. */
    public static final int DRAW_RATE = 10;

    private final UnitTank input = new UnitTank(TANK_CAPACITY,
            fluid -> fluid == ModFluids.RAW_PETROL || fluid == ModFluids.RAW_DIESEL,
            true, false, this::markDirty);
    private final UnitTank output = new UnitTank(TANK_CAPACITY,
            fluid -> fluid == ModFluids.PETROL || fluid == ModFluids.DIESEL,
            false, true, this::markDirty);

    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(ENERGY_CAPACITY, 200, 0) {
        @Override
        protected void onFinalCommit() {
            HydrotreaterBlockEntity.this.markDirty();
        }
    };

    private boolean working;

    public HydrotreaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDROTREATER, pos, state);
    }

    /** What the clean side of this raw fuel is. */
    public static Fluid cleanOf(Fluid raw) {
        return raw == ModFluids.RAW_DIESEL ? ModFluids.DIESEL
                : raw == ModFluids.RAW_PETROL ? ModFluids.PETROL : Fluids.EMPTY;
    }

    public static void tick(World world, BlockPos pos, BlockState state, HydrotreaterBlockEntity plant) {
        if (!MultiblockBlock.checkIntact(world, pos, state)) {
            return;
        }
        boolean dirty = false;

        // Raw fuel off a refinery standing against either cell.
        if (plant.input.room() > 0 && state.getBlock() instanceof MultiblockBlock machine) {
            Direction facing = state.get(MultiblockBlock.FACING);
            outer:
            for (int[] cell : machine.cells()) {
                BlockPos at = MultiblockBlock.cellPos(pos, facing, cell);
                for (Direction side : Direction.values()) {
                    if (MultiblockBlock.controllerEntity(world, at.offset(side))
                            instanceof FuelRefineryBlockEntity refinery) {
                        UnitTank product = refinery.getProductTank();
                        if (!product.isEmpty() && plant.input.canTake(product.getFluid())) {
                            Fluid raw = product.getFluid();
                            int moved = plant.input.fill(raw, product.drain(Math.min(DRAW_RATE, plant.input.room())));
                            if (moved > 0) {
                                refinery.markDirty();
                                dirty = true;
                            }
                        }
                        break outer;
                    }
                }
            }
        }

        boolean wasWorking = plant.working;
        Fluid clean = cleanOf(plant.input.getFluid());
        int batch = Math.min(RATE, Math.min(plant.input.getUnits(), plant.output.room()));
        plant.working = batch > 0 && plant.output.canTake(clean)
                && plant.energy.amount >= ENERGY_PER_TICK;
        if (plant.working) {
            plant.energy.amount -= ENERGY_PER_TICK;
            plant.input.drain(batch);
            plant.output.fill(clean, batch);
            dirty = true;
        }

        if (wasWorking != plant.working && state.getBlock() instanceof MultiblockBlock machine) {
            machine.setLit(world, pos, state, plant.working);
            dirty = true;
        }
        if (dirty) {
            plant.markDirty();
        }
    }

    // ---- the gauge window ----

    @Override
    public int gaugeKind() {
        return KIND_HYDROTREATER;
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
        return this.input;
    }

    @Override
    public UnitTank gaugeTankB() {
        return this.output;
    }

    @Override
    public int gaugeProgress() {
        return this.working ? 1000 : 0;
    }

    /** Units cleaned a tick. */
    @Override
    public int gaugeExtra() {
        return RATE;
    }

    @Override
    public boolean gaugeWorking() {
        return this.working;
    }

    // ---- what the world sees ----

    public Storage<FluidVariant> getFluidStorage() {
        return new CombinedStorage<>(List.of(this.input, this.output));
    }

    public SimpleEnergyStorage getEnergyStorage() {
        return this.energy;
    }

    public UnitTank getInput() {
        return this.input;
    }

    public UnitTank getOutput() {
        return this.output;
    }

    public int getEnergy() {
        return (int) this.energy.amount;
    }

    public void setEnergy(int amount) {
        this.energy.amount = Math.max(0, Math.min(ENERGY_CAPACITY, amount));
        this.markDirty();
    }

    public boolean isWorking() {
        return this.working;
    }

    /** The station's petrol pipes draw from here, but only clean petrol. */
    @Override
    public int drainPetrol(int amount) {
        if (this.output.getFluid() != ModFluids.PETROL) {
            return 0;
        }
        int given = this.output.drain(amount);
        if (given > 0) {
            this.markDirty();
        }
        return given;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.input.read(view, "In");
        this.output.read(view, "Out");
        this.energy.amount = Math.max(0, Math.min(ENERGY_CAPACITY, view.getInt("Energy", 0)));
        this.working = view.getBoolean("Working", false);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        this.input.write(view, "In");
        this.output.write(view, "Out");
        view.putInt("Energy", (int) this.energy.amount);
        view.putBoolean("Working", this.working);
    }
}
