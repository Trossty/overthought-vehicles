package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.workbench.VehicleWorkstationScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The vehicle workstation: a drafting table with every vehicle in the game
 * pinned to it. See {@link VehicleWorkstationScreenHandler}.
 */
public class VehicleWorkstationBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<VehicleWorkstationBlock> CODEC = simpleCodec(VehicleWorkstationBlock::new);

    private static final Component TITLE = Component.translatable("container.car.vehicle_workstation");

    public VehicleWorkstationBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(this.getMenuProvider(state, world, pos));
        return InteractionResult.CONSUME;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return new SimpleMenuProvider(
                (syncId, inventory, player) -> new VehicleWorkstationScreenHandler(
                        syncId, inventory, ContainerLevelAccess.create(world, pos)),
                TITLE);
    }
}
