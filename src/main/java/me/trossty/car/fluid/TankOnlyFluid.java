package me.trossty.car.fluid;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/**
 * A liquid that only ever lives in tanks: petrol and diesel, and the milk and
 * honey a tanker can carry, which Minecraft has as items but not as fluids.
 *
 * <p>Inside this mod petrol is a number in a tank and nothing more. That is
 * fine until somebody wants to run a pipe from another mod up to one of these
 * tanks — Create's, most obviously — because every mod that moves liquid on
 * Fabric moves "so much of such a fluid", and a number with no fluid behind it
 * is not something they can be handed.
 *
 * <p>So this is a fluid in name only: it is in the registry, it can be held in
 * a tank and counted, and it can never be poured out into the world. There is
 * no block of it, no bucket of it and it does not flow, which is the same
 * bargain Create strikes for its own tea and potions.
 */
public class TankOnlyFluid extends Fluid {

    @Override
    public Item getBucketItem() {
        // The jerry can is this mod's bucket, and it holds a measured amount
        // rather than "one bucket", so it is not offered as one.
        return Items.AIR;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos,
                                        Fluid fluid, Direction direction) {
        return true;
    }

    @Override
    protected Vec3d getVelocity(BlockView world, BlockPos pos, FluidState state) {
        return Vec3d.ZERO;
    }

    @Override
    public int getTickRate(WorldView world) {
        return 0;
    }

    @Override
    protected float getBlastResistance() {
        return 0.0F;
    }

    @Override
    public float getHeight(FluidState state, BlockView world, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public float getHeight(FluidState state) {
        return 0.0F;
    }

    @Override
    protected BlockState toBlockState(FluidState state) {
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isStill(FluidState state) {
        return true;
    }

    @Override
    public int getLevel(FluidState state) {
        return 0;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }
}
