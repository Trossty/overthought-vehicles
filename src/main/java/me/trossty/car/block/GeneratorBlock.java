package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.GeneratorBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * The block a {@link GeneratorBlockEntity} lives in: a standby set the size of
 * a house's, on a skid, with the exhaust out of the back. One class for all
 * three; which fuel it takes is the kind it was registered with.
 */
public class GeneratorBlock extends BlockWithEntity {

    public static final MapCodec<GeneratorBlock> CODEC =
            createCodec(settings -> new GeneratorBlock(GeneratorBlockEntity.Kind.COAL, settings));
    public static final MapCodec<GeneratorBlock> BIOFUEL_CODEC =
            createCodec(settings -> new GeneratorBlock(GeneratorBlockEntity.Kind.BIOFUEL, settings));
    public static final MapCodec<GeneratorBlock> DIESEL_CODEC =
            createCodec(settings -> new GeneratorBlock(GeneratorBlockEntity.Kind.DIESEL, settings));

    private static final net.minecraft.util.shape.VoxelShape SHAPE =
            BlockWithEntity.createCuboidShape(0.5, 0, 0.5, 15.5, 15.5, 15.5);

    private final GeneratorBlockEntity.Kind kind;

    public static final BooleanProperty LIT = Properties.LIT;

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public GeneratorBlock(GeneratorBlockEntity.Kind kind, Settings settings) {
        super(settings);
        this.kind = kind;
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(LIT, false).with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
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
    protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world,
                                                                  BlockPos pos, net.minecraft.block.ShapeContext context) {
        return SHAPE;
    }

    /** Exhaust out of the stack at the back while it runs. */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (!state.get(LIT)) {
            return;
        }
        Direction back = state.get(FACING).getOpposite();
        Direction right = state.get(FACING).rotateYClockwise();
        // out of the muffler's outlet, up the back corner of the engine end
        double x = pos.getX() + 0.5 + back.getOffsetX() * 0.125 + right.getOffsetX() * 0.34;
        double z = pos.getZ() + 0.5 + back.getOffsetZ() * 0.125 + right.getOffsetZ() * 0.34;
        double y = pos.getY() + 0.78;
        // Small puffs, a muffler's worth; the diesel puts out two at a time.
        var smoke = this.kind == GeneratorBlockEntity.Kind.BIOFUEL
                ? net.minecraft.particle.ParticleTypes.WHITE_SMOKE
                : net.minecraft.particle.ParticleTypes.SMOKE;
        world.addParticleClient(smoke, x, y, z, 0.0, 0.03, 0.0);
        if (this.kind == GeneratorBlockEntity.Kind.DIESEL && random.nextBoolean()) {
            world.addParticleClient(smoke, x, y + 0.08, z, 0.0, 0.04, 0.0);
        }
        if (this.kind == GeneratorBlockEntity.Kind.COAL && random.nextInt(4) == 0) {
            world.addParticleClient(net.minecraft.particle.ParticleTypes.FLAME,
                    pos.getX() + 0.5 + state.get(FACING).getOffsetX() * 0.33 + right.getOffsetX() * 0.17,
                    pos.getY() + 0.38,
                    pos.getZ() + 0.5 + state.get(FACING).getOffsetZ() * 0.33 + right.getOffsetZ() * 0.17, 0.0, 0.0, 0.0);
        }
        if (this.kind == GeneratorBlockEntity.Kind.COAL && random.nextInt(20) == 0) {
            world.playSoundClient(x, y, z, net.minecraft.sound.SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE,
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.4F, 0.6F, false);
        }
        // The set's own drone: the coal one rougher, the diesel deeper.
        float pitch = switch (this.kind) {
            case COAL -> 0.9F;
            case BIOFUEL -> 1.0F;
            case DIESEL -> 0.8F;
        };
        world.playSoundClient(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                me.trossty.car.sound.ModSounds.GENERATOR_HUM, net.minecraft.sound.SoundCategory.BLOCKS,
                0.45F, pitch + (random.nextFloat() - 0.5F) * 0.04F, false);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.GENERATOR, GeneratorBlockEntity::tick);
    }

    /** Something that burns goes in the firebox; anything else reads the gauge. */
    @Override
    protected ActionResult onUseWithItem(ItemStack held, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (this.kind.burnsLiquid()) {
            if (!held.isOf(me.trossty.car.registry.ModItems.JERRY_CAN)) {
                return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
            }
            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }
            if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
                pour(generator, held, player);
            }
            return ActionResult.CONSUME;
        }
        if (GeneratorBlockEntity.burnTicks(held, world) <= 0) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            feed(generator, held, player, hand);
        }
        return ActionResult.CONSUME;
    }

    /** Empties a can of the right fuel into a liquid generator's tank. */
    public static boolean pour(GeneratorBlockEntity generator, ItemStack can, PlayerEntity player) {
        var kind = generator.getKind();
        if (me.trossty.car.item.JerryCans.isEmpty(can)) {
            player.sendMessage(Text.translatable("message.car.can_empty"), true);
            return false;
        }
        if (me.trossty.car.entity.CarEntity.fuelIn(can) != kind.fuel) {
            player.sendMessage(Text.translatable("message.car.generator_wrong_fuel",
                    Text.translatable("fuel.car." + kind.fuel.key())), true);
            return false;
        }
        var tank = generator.getTank();
        int poured = me.trossty.car.item.JerryCans.emptyInto(can, tank.room(),
                units -> tank.fill(kind.fluid(), units));
        if (poured <= 0) {
            player.sendMessage(Text.translatable("message.car.generator_full"), true);
            return false;
        }
        generator.markDirty();
        player.getEntityWorld().playSound(null, generator.getPos(),
                net.minecraft.sound.SoundEvents.ITEM_BUCKET_EMPTY, net.minecraft.sound.SoundCategory.BLOCKS, 0.6F, 1.0F);
        return true;
    }

    /** Puts as much of what is held into the firebox as will go. */
    public static boolean feed(GeneratorBlockEntity generator, ItemStack held,
                               PlayerEntity player, Hand hand) {
        ItemStack inside = generator.getStack(0);
        if (inside.isEmpty()) {
            generator.setStack(0, held.copy());
            player.setStackInHand(hand, ItemStack.EMPTY);
            return true;
        }
        if (ItemStack.areItemsAndComponentsEqual(inside, held)
                && inside.getCount() < inside.getMaxCount()) {
            int moved = Math.min(held.getCount(), inside.getMaxCount() - inside.getCount());
            inside.increment(moved);
            held.decrement(moved);
            generator.markDirty();
            return true;
        }
        player.sendMessage(Text.translatable("message.car.generator_full"), true);
        return false;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            // A sneaking empty hand takes back what has not been burnt yet.
            if (player.isSneaking() && !generator.getStack(0).isEmpty()) {
                player.getInventory().offerOrDrop(generator.removeStack(0));
                return ActionResult.CONSUME;
            }
            player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, who) -> me.trossty.car.machine.MachineScreenHandler.of(syncId, inventory,
                            generator, this.kind.burnsLiquid() ? null : generator),
                    this.getName()));
        }
        return ActionResult.CONSUME;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            ItemScatterer.spawn(world, pos, generator);
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
