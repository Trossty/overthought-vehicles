package me.trossty.car.block.entity;

import me.trossty.car.block.EnergyCableBlock;
import me.trossty.car.network.EnergyNetwork;
import me.trossty.car.network.Network;
import me.trossty.car.network.NetworkBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * One length of cable. It holds no charge: the run it is part of works as one
 * {@link EnergyNetwork}, taking from what gives and handing it straight to what
 * takes, so a long line delivers as fast as a short one and loses nothing.
 */
public class EnergyCableBlockEntity extends NetworkBlockEntity<EnergyNetwork> {

    private final IEnergyStorage[] doors = new IEnergyStorage[7];

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE, pos, state);
    }

    @Override
    public Network.Link link(Direction side) {
        return switch (this.getBlockState().getValue(EnergyCableBlock.SIDES.get(side))) {
            case NONE -> Network.Link.NONE;
            case CABLE -> Network.Link.MEMBER;
            case DEVICE -> Network.Link.PORT;
        };
    }

    @Override
    protected Object linkIn(BlockState state, Direction side) {
        return state.getValue(EnergyCableBlock.SIDES.get(side));
    }

    @Override
    protected EnergyNetwork gather(ServerLevel world) {
        return Network.gather(world, this, EnergyNetwork::new);
    }

    /** Energy the run moved in its last tick. */
    public int getFlow() {
        EnergyNetwork network = this.network();
        return network == null ? 0 : network.flow();
    }

    /** Lights or darkens the copper stripe. */
    public void showPowered(boolean lit) {
        BlockState state = this.getBlockState();
        if (this.level != null && state.hasProperty(EnergyCableBlock.POWERED)
                && state.getValue(EnergyCableBlock.POWERED) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(EnergyCableBlock.POWERED, lit), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * The cable as another mod's cable or machine sees it on one face: power
     * pushed in goes on along the run, and drawing from it draws from what the
     * run reaches.
     */
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        if (this.doors[index] == null) {
            BlockPos from = side == null ? this.worldPosition : this.worldPosition.relative(side);
            this.doors[index] = new Door(from);
        }
        return this.doors[index];
    }

    private final class Door implements IEnergyStorage {
        private final BlockPos from;

        Door(BlockPos from) {
            this.from = from;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            EnergyNetwork network = EnergyCableBlockEntity.this.network();
            return network == null ? 0 : network.insert(toReceive, simulate, this.from);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            EnergyNetwork network = EnergyCableBlockEntity.this.network();
            return network == null ? 0 : network.extract(toExtract, simulate, this.from);
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return EnergyNetwork.rate();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
