package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.BatteryBankBlockEntity;
import me.trossty.car.machine.MachineScreenHandler;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A grey cabinet of cells with four charge bars on its door. It takes power
 * in on every side but the front and gives it out of the socket on the front,
 * so it sits in a cable run like a reservoir in a pipe.
 */
public class BatteryBankBlock extends BlockWithEntity {

    public static final MapCodec<BatteryBankBlock> CODEC = createCodec(BatteryBankBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    /** How many of the four bars on the door are lit. */
    public static final IntProperty LEVEL = IntProperty.of("level", 0, 4);

    private static final VoxelShape SHAPE = createCuboidShape(1, 0, 1, 15, 15, 15);

    public BatteryBankBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEVEL);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ModBlockEntities.BATTERY_BANK, BatteryBankBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof BatteryBankBlockEntity bank) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, who) -> MachineScreenHandler.of(syncId, inventory, bank, null),
                    this.getName()));
        }
        return ActionResult.CONSUME;
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof BatteryBankBlockEntity bank) {
            return (int) Math.round(bank.getEnergy() * 15.0 / BatteryBankBlockEntity.CAPACITY);
        }
        return 0;
    }
}
