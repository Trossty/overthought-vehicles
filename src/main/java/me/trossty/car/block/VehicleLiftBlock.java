package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.VehicleLiftBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The ramp a car is assembled on, the only place one can be worked on
 * afterwards, and — since it is a lift — the thing that puts the car in the air
 * to do it.
 *
 * <p>Assembly happens in its window, which is the workbench's window fed a
 * different recipe list, and that works with the platform down: you build a car
 * standing on the floor. Repair is the other way about. Beating dents out is
 * still a matter of walking up with an iron ingot, but
 * {@link #isServicing(World, Entity)} wants a lift that is actually
 * <em>up</em>, so the job happens where a garage would do it, on a car you can
 * get under.
 *
 * <p>A plain right click opens the window. A sneaking one works the lift, which
 * is the one control it has besides redstone;
 * {@link VehicleLiftBlockEntity} explains how the two get along.
 */
public class VehicleLiftBlock extends BlockWithEntity {

    public static final MapCodec<VehicleLiftBlock> CODEC = createCodec(VehicleLiftBlock::new);

    /** Quarter-blocks of travel, 0 being flat on the floor. */
    public static final IntProperty LEVEL =
            IntProperty.of("level", 0, VehicleLiftBlockEntity.MAX_LEVEL);

    /**
     * Which way the platform runs. It is longer than the block — a car is —
     * so it is laid along the way the player was looking when it went down,
     * which is the way a car will be driven on to it.
     */
    public static final net.minecraft.state.property.EnumProperty<net.minecraft.util.math.Direction> FACING =
            net.minecraft.state.property.Properties.HORIZONTAL_FACING;

    /**
     * How far from a lift a car still counts as being on it. The car is 2.5
     * blocks long and nobody parks to the block, so this is deliberately
     * forgiving: anywhere over the ramp or a block either side of it.
     */
    private static final int REACH = 2;

    /** The floor plate, which is the whole block when the lift is down. */
    private static final VoxelShape PLATE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    private final VoxelShape[] shapes = buildShapes();

    public VehicleLiftBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(LEVEL, 0)
                .with(FACING, net.minecraft.util.math.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, FACING);
    }

    /**
     * One shape per level: the plate, the ram that comes out of it and the deck
     * on top. The deck is what a car — or a player who climbed up — stands on,
     * so it has to be in the collision shape and not just in the model.
     */
    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[VehicleLiftBlockEntity.MAX_LEVEL + 1];
        shapes[0] = PLATE;
        for (int level = 1; level < shapes.length; level++) {
            double deck = 12.0 + 4.0 * level;
            shapes[level] = VoxelShapes.union(
                    PLATE,
                    Block.createCuboidShape(6.0, 16.0, 6.0, 10.0, deck, 10.0),
                    Block.createCuboidShape(1.0, deck, 1.0, 15.0, deck + 4.0, 15.0));
        }
        return shapes;
    }

    @Override
    public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.shapes[state.get(LEVEL)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.shapes[state.get(LEVEL)];
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new VehicleLiftBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.VEHICLE_LIFT, VehicleLiftBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        return working(world, pos, player);
    }

    /** A click is the lift's lever: latch it up, latch it down. It has no window. */
    private static ActionResult working(World world, BlockPos pos, PlayerEntity player) {
        if (!(world.getBlockEntity(pos) instanceof VehicleLiftBlockEntity lift)) {
            return ActionResult.CONSUME;
        }
        boolean up = lift.toggle();
        world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.4F, up ? 1.1F : 0.8F);
        player.sendMessage(Text.translatable(up ? "message.car.lift_up" : "message.car.lift_down"), true);
        return ActionResult.CONSUME;
    }

    /**
     * True when there is a lift close enough under a vehicle to work on it, and
     * that lift has the car in the air. A car on its wheels is a car you cannot
     * get under, which is the point of owning the lift in the first place.
     */
    public static boolean isServicing(World world, Entity vehicle) {
        BlockPos at = vehicle.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(
                at.add(-REACH, -3, -REACH), at.add(REACH, 0, REACH))) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(ModBlocks.VEHICLE_LIFT) && state.get(LEVEL) > 0) {
                return true;
            }
        }
        return false;
    }
}
