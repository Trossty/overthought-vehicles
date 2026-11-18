package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.EngineStandBlockEntity;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A wheeled stand an engine out of a car is bolted to by its back, so it can
 * be looked over off the floor. Right click with an engine to hang it on;
 * with an empty hand to take it off again. Broken, the stand lets the
 * engine fall.
 *
 * <p>It faces the way it was put down so the engine hangs out towards you.
 */
public class EngineStandBlock extends ModelEntityBlock {

    public static final MapCodec<EngineStandBlock> CODEC = simpleCodec(EngineStandBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public EngineStandBlock(Properties settings) {
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

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return new EngineStandBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof EngineStandBlockEntity stand)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (held.is(ModItems.ENGINE) && stand.getEngine().isEmpty()) {
            if (!world.isClientSide()) {
                stand.setEngine(held.copyWithCount(1));
                held.consume(1, player);
            }
            return ItemInteractionResult.sidedSuccess(world.isClientSide());
        }
        if (held.is(ModItems.WRENCH) && !stand.getEngine().isEmpty()) {
            if (!world.isClientSide()) {
                if (!stand.isStripped()) {
                    stand.strip(world.getRandom());
                    world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
                    player.displayClientMessage(Component.translatable("message.car.engine_stripped"), true);
                } else if (stand.isComplete()) {
                    stand.rebuild();
                    world.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                    player.displayClientMessage(Component.translatable("message.car.engine_rebuilt",
                            Math.round(me.trossty.car.entity.Workshop.conditionOf(stand.getEngine()))), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.car.engine_incomplete"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(world.isClientSide());
        }
        int slot = java.util.Arrays.asList(ModItems.engineInternals()).indexOf(held.getItem());
        if (slot >= 0 && stand.isStripped() && stand.getInternals().get(slot).isEmpty()) {
            if (!world.isClientSide()) {
                stand.setInternal(slot, held.copyWithCount(1));
                held.consume(1, player);
                world.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(world.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof EngineStandBlockEntity stand) || stand.getEngine().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide()) {
            if (stand.isStripped()) {
                // Opened up: an empty hand takes the insides out one at a
                // time, and says how worn each one is.
                for (int slot = 0; slot < 3; slot++) {
                    ItemStack inside = stand.getInternals().get(slot);
                    if (!inside.isEmpty()) {
                        stand.setInternal(slot, ItemStack.EMPTY);
                        player.displayClientMessage(Component.translatable("message.car.internal_out",
                                inside.getHoverName(), Math.round(me.trossty.car.entity.Workshop.conditionOf(inside))), true);
                        if (!player.addItem(inside)) {
                            player.drop(inside, false);
                        }
                        return InteractionResult.CONSUME;
                    }
                }
                player.displayClientMessage(Component.translatable("message.car.engine_incomplete"), true);
                return InteractionResult.CONSUME;
            }
            ItemStack engine = stand.getEngine();
            stand.setEngine(ItemStack.EMPTY);
            if (!player.addItem(engine)) {
                player.drop(engine, false);
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof EngineStandBlockEntity stand) {
            if (stand.isStripped()) {
                // An engine block with its insides out is a shell, not an engine.
                stand.getEngine().set(me.trossty.car.registry.ModComponents.PART_CONDITION, 1);
            }
            Containers.dropItemStack(world, pos.getX(), pos.getY() + 0.5, pos.getZ(), stand.getEngine());
            for (ItemStack inside : stand.getInternals()) {
                Containers.dropItemStack(world, pos.getX(), pos.getY() + 0.5, pos.getZ(), inside);
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }
}
