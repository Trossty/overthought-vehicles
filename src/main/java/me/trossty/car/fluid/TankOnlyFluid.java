package me.trossty.car.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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

    private final net.neoforged.neoforge.fluids.FluidType type;

    /** {@code name} is the fluid's id path; its type carries the translated name every other mod shows. */
    private final Item bucket;

    public TankOnlyFluid(String name) {
        this(name, Items.AIR);
    }

    /** A liquid that does have a vanilla bucket, like milk, so an empty bucket can be filled with it. */
    public TankOnlyFluid(String name, Item bucket) {
        this.bucket = bucket;
        this.type = new net.neoforged.neoforge.fluids.FluidType(net.neoforged.neoforge.fluids.FluidType.Properties.create()
                .descriptionId("fluid.car." + name)
                .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL)
                .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY));
    }

    @Override
    public net.neoforged.neoforge.fluids.FluidType getFluidType() {
        return this.type;
    }

    @Override
    public Item getBucket() {
        // The jerry can is this mod's bucket, and it holds a measured amount
        // rather than "one bucket", so fuel is not offered in one. Milk keeps
        // the milk bucket.
        return this.bucket;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter world, BlockPos pos,
                                        Fluid fluid, Direction direction) {
        return true;
    }

    @Override
    protected Vec3 getFlow(BlockGetter world, BlockPos pos, FluidState state) {
        return Vec3.ZERO;
    }

    @Override
    public int getTickDelay(LevelReader world) {
        return 0;
    }

    @Override
    protected float getExplosionResistance() {
        return 0.0F;
    }

    @Override
    public float getHeight(FluidState state, BlockGetter world, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public float getOwnHeight(FluidState state) {
        return 0.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState state) {
        return true;
    }

    @Override
    public int getAmount(FluidState state) {
        return 0;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter world, BlockPos pos) {
        return Shapes.empty();
    }
}
