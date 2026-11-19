package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.GeneratorBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The block a {@link GeneratorBlockEntity} lives in: a standby set the size of
 * a house's, on a skid, with the exhaust out of the back. One class for all
 * three; which fuel it takes is the kind it was registered with.
 */
public class GeneratorBlock extends ModelEntityBlock {

    public static final MapCodec<GeneratorBlock> CODEC =
            simpleCodec(settings -> new GeneratorBlock(GeneratorBlockEntity.Kind.COAL, settings));
    public static final MapCodec<GeneratorBlock> BIOFUEL_CODEC =
            simpleCodec(settings -> new GeneratorBlock(GeneratorBlockEntity.Kind.BIOFUEL, settings));
    public static final MapCodec<GeneratorBlock> DIESEL_CODEC =
            simpleCodec(settings -> new GeneratorBlock(GeneratorBlockEntity.Kind.DIESEL, settings));

    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE =
            BaseEntityBlock.box(0.5, 0, 0.5, 15.5, 15.5, 15.5);

    private final GeneratorBlockEntity.Kind kind;

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public GeneratorBlock(GeneratorBlockEntity.Kind kind, Properties settings) {
        super(settings);
        this.kind = kind;
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(LIT, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return switch (this.kind) {
            case COAL -> CODEC;
            case BIOFUEL -> BIOFUEL_CODEC;
            case DIESEL -> DIESEL_CODEC;
        };
    }

    public GeneratorBlockEntity.Kind getKind() {
        return this.kind;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter world,
                                                                  BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    /** Exhaust out of the stack at the back while it runs. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        Direction back = state.getValue(FACING).getOpposite();
        Direction right = state.getValue(FACING).getClockWise();
        // out of the muffler's outlet, up the back corner of the engine end
        double x = pos.getX() + 0.5 + back.getStepX() * 0.125 + right.getStepX() * 0.34;
        double z = pos.getZ() + 0.5 + back.getStepZ() * 0.125 + right.getStepZ() * 0.34;
        double y = pos.getY() + 0.78;
        // Small puffs, a muffler's worth; the diesel puts out two at a time.
        var smoke = this.kind == GeneratorBlockEntity.Kind.BIOFUEL
                ? net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE
                : net.minecraft.core.particles.ParticleTypes.SMOKE;
        world.addParticle(smoke, x, y, z, 0.0, 0.03, 0.0);
        if (this.kind == GeneratorBlockEntity.Kind.DIESEL && random.nextBoolean()) {
            world.addParticle(smoke, x, y + 0.08, z, 0.0, 0.04, 0.0);
        }
        if (this.kind == GeneratorBlockEntity.Kind.COAL && random.nextInt(4) == 0) {
            world.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                    pos.getX() + 0.5 + state.getValue(FACING).getStepX() * 0.33 + right.getStepX() * 0.17,
                    pos.getY() + 0.38,
                    pos.getZ() + 0.5 + state.getValue(FACING).getStepZ() * 0.33 + right.getStepZ() * 0.17, 0.0, 0.0, 0.0);
        }
        if (this.kind == GeneratorBlockEntity.Kind.COAL && random.nextInt(20) == 0) {
            world.playLocalSound(x, y, z, net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 0.6F, false);
        }
        // The set's own drone: the coal one rougher, the diesel deeper.
        float pitch = switch (this.kind) {
            case COAL -> 0.9F;
            case BIOFUEL -> 1.0F;
            case DIESEL -> 0.8F;
        };
        world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                me.trossty.car.sound.ModSounds.GENERATOR_HUM, net.minecraft.sounds.SoundSource.BLOCKS,
                0.45F, pitch + (random.nextFloat() - 0.5F) * 0.04F, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.GENERATOR, GeneratorBlockEntity::tick);
    }

    /** Something that burns goes in the firebox; anything else reads the gauge. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (this.kind.burnsLiquid()) {
            if (!held.is(me.trossty.car.registry.ModItems.JERRY_CAN)) {
                return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (world.isClientSide()) {
                return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
            if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
                pour(generator, held, player);
            }
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }
        if (GeneratorBlockEntity.burnTicks(held, world) <= 0) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (world.isClientSide()) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            feed(generator, held, player, hand);
        }
        return net.minecraft.world.ItemInteractionResult.CONSUME;
    }

    /** Empties a can of the right fuel into a liquid generator's tank. */
    public static boolean pour(GeneratorBlockEntity generator, ItemStack can, Player player) {
        var kind = generator.getKind();
        if (me.trossty.car.item.JerryCans.isEmpty(can)) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.can_empty"), true);
            return false;
        }
        if (me.trossty.car.entity.CarEntity.fuelIn(can) != kind.fuel) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.generator_wrong_fuel",
                    Component.translatable("fuel.overthought_vehicles." + kind.fuel.key())), true);
            return false;
        }
        var tank = generator.getTank();
        int poured = me.trossty.car.item.JerryCans.emptyInto(can, tank.room(),
                units -> tank.fill(kind.fluid(), units));
        if (poured <= 0) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.generator_full"), true);
            return false;
        }
        generator.setChanged();
        player.level().playSound(null, generator.getBlockPos(),
                net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.0F);
        return true;
    }

    /** Puts as much of what is held into the firebox as will go. */
    public static boolean feed(GeneratorBlockEntity generator, ItemStack held,
                               Player player, InteractionHand hand) {
        ItemStack inside = generator.getItem(0);
        if (inside.isEmpty()) {
            generator.setItem(0, held.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
            return true;
        }
        if (ItemStack.isSameItemSameComponents(inside, held)
                && inside.getCount() < inside.getMaxStackSize()) {
            int moved = Math.min(held.getCount(), inside.getMaxStackSize() - inside.getCount());
            inside.grow(moved);
            held.shrink(moved);
            generator.setChanged();
            return true;
        }
        player.displayClientMessage(Component.translatable("message.overthought_vehicles.generator_full"), true);
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            // A sneaking empty hand takes back what has not been burnt yet.
            if (player.isShiftKeyDown() && !generator.getItem(0).isEmpty()) {
                player.getInventory().placeItemBackInInventory(generator.removeItemNoUpdate(0));
                return InteractionResult.CONSUME;
            }
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (syncId, inventory, who) -> me.trossty.car.machine.MachineScreenHandler.of(syncId, inventory,
                            generator, this.kind.burnsLiquid() ? null : generator),
                    this.getName()));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level world, BlockPos pos,
                            BlockState newState, boolean moved) {
        // Only when the block really goes, not when its state changes.
        if (!world.isClientSide() && !state.is(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
                Containers.dropContents(world, pos, generator);
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }
}
