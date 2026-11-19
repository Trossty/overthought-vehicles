package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.EngineHoistBlockEntity;
import me.trossty.car.block.entity.EngineStandBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.Workshop;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A folding engine crane: legs on castors, a mast and a boom with a chain
 * and hook. The engine of a car with a bonnet comes out of the top on one of
 * these and no other way, and goes back in the same way.
 *
 * <p>Stand it at the car's nose with the boom over the open bay and click it
 * with an empty hand: the engine comes up onto the hook. Sneak and click to
 * swing the boom a quarter turn, until the hook hangs over an engine stand,
 * and click again to set the engine down on it. From the stand back onto the
 * hook and down into an empty bay is the same the other way round, and an
 * engine in the hand hangs straight on an empty hook.
 */
public class EngineHoistBlock extends ModelEntityBlock {

    public static final MapCodec<EngineHoistBlock> CODEC = simpleCodec(EngineHoistBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Where the hook hangs: out from the middle of the block along the boom, and up off the floor, in blocks. */
    public static final double HOOK_OUT = 18.5 / 16.0;
    public static final double HOOK_HEIGHT = 11.6 / 16.0;

    /** How far the hook may hang from the middle of a bonnet and still have that engine on it, in blocks. */
    private static final double REACH = 1.5;

    /** The legs and the mast; the boom hangs over the car and stops nothing. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
            Block.box(6.0, 3.0, 6.0, 10.0, 16.0, 10.0));

    public EngineHoistBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The boom points the way the player looks: at the car they are standing in front of. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EngineHoistBlockEntity(pos, state);
    }

    /** Where the hook of a hoist standing here with its boom this way hangs. */
    public static Vec3 hook(BlockPos pos, Direction facing) {
        return Vec3.atBottomCenterOf(pos).add(facing.getStepX() * HOOK_OUT, HOOK_HEIGHT, facing.getStepZ() * HOOK_OUT);
    }

    /** Whether a hook hanging here is over the car's engine bay. */
    private static boolean reaches(Vec3 hook, CarEntity car) {
        Vec3 bay = car.bonnetCentre();
        if (bay == null) {
            return false;
        }
        double dx = bay.x - hook.x;
        double dz = bay.z - hook.z;
        return dx * dx + dz * dz <= REACH * REACH && Math.abs(bay.y - hook.y) <= REACH;
    }

    /** Whether a hoist stands with its hook over the car's engine bay. */
    public static boolean isNear(Level world, CarEntity car) {
        return hoistOver(world, car, false) != null;
    }

    /**
     * The hoist whose hook hangs nearest the middle of the car's engine bay,
     * or null when none reaches it; with {@code emptyHook}, only one with
     * nothing on its hook yet.
     */
    public static @Nullable EngineHoistBlockEntity hoistOver(Level world, CarEntity car, boolean emptyHook) {
        Vec3 bay = car.bonnetCentre();
        if (bay == null) {
            return null;
        }
        BlockPos at = BlockPos.containing(bay);
        EngineHoistBlockEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-3, -3, -3), at.offset(3, 1, 3))) {
            BlockState state = world.getBlockState(pos);
            if (!state.is(ModBlocks.ENGINE_HOIST) || !(world.getBlockEntity(pos) instanceof EngineHoistBlockEntity hoist)
                    || emptyHook && hoist.isHolding()) {
                continue;
            }
            Vec3 hook = hook(pos, state.getValue(FACING));
            double distance = hook.distanceToSqr(bay);
            if (reaches(hook, car) && distance < nearest) {
                best = hoist;
                nearest = distance;
            }
        }
        return best;
    }

    /** The car whose engine bay this hoist's hook hangs over, or null. */
    public static @Nullable CarEntity carUnder(Level world, BlockPos pos, BlockState state) {
        Vec3 hook = hook(pos, state.getValue(FACING));
        CarEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (CarEntity car : world.getEntitiesOfClass(CarEntity.class, AABB.ofSize(hook, 8.0, 8.0, 8.0))) {
            if (reaches(hook, car)) {
                double distance = car.bonnetCentre().distanceToSqr(hook);
                if (distance < nearest) {
                    best = car;
                    nearest = distance;
                }
            }
        }
        return best;
    }

    /** An engine in the hand hangs on an empty hook. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!held.is(ModItems.ENGINE) || !(world.getBlockEntity(pos) instanceof EngineHoistBlockEntity hoist)
                || hoist.isHolding()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!world.isClientSide()) {
            hoist.setEngine(held.copyWithCount(1));
            held.consume(1, player);
            world.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.car.engine_hooked",
                    Math.round(Workshop.conditionOf(hoist.getEngine()))), true);
        }
        return ItemInteractionResult.sidedSuccess(world.isClientSide());
    }

    /**
     * Sneaking swings the boom; otherwise the hook works on whatever hangs
     * under it, a stand first, then a car's engine bay: an empty hook picks
     * the engine up, a full one sets it down.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof EngineHoistBlockEntity hoist)) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Direction facing = state.getValue(FACING);
        if (player.isSecondaryUseActive()) {
            // A quarter turn on the mast, clockwise, with whatever hangs on it.
            world.setBlock(pos, state.setValue(FACING, facing.getClockWise()), Block.UPDATE_ALL);
            world.playSound(null, pos, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 1.0F, 0.7F);
            return InteractionResult.CONSUME;
        }
        EngineStandBlockEntity stand = world.getBlockEntity(pos.relative(facing)) instanceof EngineStandBlockEntity under
                ? under : null;
        CarEntity car = carUnder(world, pos, state);
        if (hoist.isHolding()) {
            if (stand != null && stand.getEngine().isEmpty()) {
                stand.setEngine(hoist.takeEngine());
                world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
                player.displayClientMessage(Component.translatable("message.car.engine_on_stand"), true);
                return InteractionResult.CONSUME;
            }
            if (car != null) {
                car.workshop().fitPart(player, hoist.getEngine(), CarPart.ENGINE, hoist);
                return InteractionResult.CONSUME;
            }
        } else {
            if (stand != null && !stand.getEngine().isEmpty()) {
                if (stand.isStripped()) {
                    // The block and its insides are not one thing to lift yet.
                    player.displayClientMessage(Component.translatable("message.car.engine_open_on_stand"), true);
                    return InteractionResult.CONSUME;
                }
                hoist.setEngine(stand.getEngine());
                stand.setEngine(ItemStack.EMPTY);
                world.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                player.displayClientMessage(Component.translatable("message.car.engine_hooked",
                        Math.round(Workshop.conditionOf(hoist.getEngine()))), true);
                return InteractionResult.CONSUME;
            }
            if (car != null) {
                car.workshop().removePart(player, CarPart.ENGINE, hoist);
                return InteractionResult.CONSUME;
            }
        }
        player.displayClientMessage(Component.translatable("message.car.hoist_nothing"), true);
        return InteractionResult.CONSUME;
    }

    /** Knocked down, the hoist lets go of what it holds. */
    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof EngineHoistBlockEntity hoist) {
            Containers.dropItemStack(world, pos.getX(), pos.getY() + 0.5, pos.getZ(), hoist.getEngine());
        }
        super.onRemove(state, world, pos, newState, moved);
    }
}
