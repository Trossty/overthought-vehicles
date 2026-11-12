package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.workbench.VehicleWorkstationScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * The vehicle workstation: a drafting table with every vehicle in the game
 * pinned to it. See {@link VehicleWorkstationScreenHandler}.
 */
public class VehicleWorkstationBlock extends HorizontalFacingBlock {

    public static final MapCodec<VehicleWorkstationBlock> CODEC = createCodec(VehicleWorkstationBlock::new);

    private static final Text TITLE = Text.translatable("container.car.vehicle_workstation");

    public VehicleWorkstationBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        player.openHandledScreen(this.createScreenHandlerFactory(state, world, pos));
        return ActionResult.CONSUME;
    }

    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new VehicleWorkstationScreenHandler(
                        syncId, inventory, ScreenHandlerContext.create(world, pos)),
                TITLE);
    }
}
