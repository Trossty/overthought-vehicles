package me.trossty.car.block.entity;

import me.trossty.car.block.MultiblockBlock;
import me.trossty.car.fluid.FluidSource;
import me.trossty.car.fluid.PetrolSource;
import me.trossty.car.fluid.UnitTank;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import me.trossty.car.energy.EnergyBuffer;

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
            true, false, this::setChanged);
    private final UnitTank output = new UnitTank(TANK_CAPACITY,
            fluid -> fluid == ModFluids.PETROL || fluid == ModFluids.DIESEL,
            false, true, this::setChanged);

    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_CAPACITY, 200, 0) {
        @Override
        protected void onChanged() {
            HydrotreaterBlockEntity.this.setChanged();
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

    public static void tick(Level world, BlockPos pos, BlockState state, HydrotreaterBlockEntity plant) {
        if (!MultiblockBlock.checkIntact(world, pos, state)) {
            return;
        }
        boolean dirty = false;

        // Raw fuel off a refinery standing against either cell.
        if (plant.input.room() > 0 && state.getBlock() instanceof MultiblockBlock machine) {
            Direction facing = state.getValue(MultiblockBlock.FACING);
            outer:
            for (int[] cell : machine.cells()) {
                BlockPos at = MultiblockBlock.cellPos(pos, facing, cell);
                for (Direction side : Direction.values()) {
                    if (MultiblockBlock.controllerEntity(world, at.relative(side))
                            instanceof FuelRefineryBlockEntity refinery) {
                        UnitTank product = refinery.getProductTank();
                        if (!product.isEmpty() && plant.input.canTake(product.getFluid())) {
                            Fluid raw = product.getFluid();
                            int moved = plant.input.fill(raw, product.drain(Math.min(DRAW_RATE, plant.input.room())));
                            if (moved > 0) {
                                refinery.setChanged();
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
            plant.setChanged();
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

    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidStorage() {
        return new me.trossty.car.fluid.CombinedTanks(this.input, this.output);
    }

    public EnergyBuffer getEnergyStorage() {
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
        this.setChanged();
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
            this.setChanged();
        }
        return given;
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        this.input.read(view, "In");
        this.output.read(view, "Out");
        this.energy.amount = Math.max(0, Math.min(ENERGY_CAPACITY, view.getIntOr("Energy", 0)));
        this.working = view.getBooleanOr("Working", false);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        this.input.write(view, "In");
        this.output.write(view, "Out");
        view.putInt("Energy", (int) this.energy.amount);
        view.putBoolean("Working", this.working);
    }
}
