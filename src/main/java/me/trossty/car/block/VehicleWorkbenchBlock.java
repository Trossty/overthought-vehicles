package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.workbench.VehicleWorkbenchScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The bench you build vehicles at. It holds nothing of its own: the window's
 * grid is scratch space handed back when you close it, exactly like a vanilla
 * crafting table, which is why this needs no block entity.
 */
public class VehicleWorkbenchBlock extends Block {

    public static final MapCodec<VehicleWorkbenchBlock> CODEC = simpleCodec(VehicleWorkbenchBlock::new);

    private static final Component TITLE = Component.translatable("container.overthought_vehicles.vehicle_workbench");

    public VehicleWorkbenchBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(this.createFactory(world, pos));
        return InteractionResult.CONSUME;
    }

    /**
     * Declared as well as used in {@link #useWithoutItem}, so anything that asks the
     * block for its screen — a test, a mod, a command — gets the same window a
     * right click would open instead of nothing.
     */
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return this.createFactory(world, pos);
    }

    private MenuProvider createFactory(Level world, BlockPos pos) {
        return new SimpleMenuProvider(
                (syncId, inventory, player) -> new VehicleWorkbenchScreenHandler(
                        syncId, inventory, ContainerLevelAccess.create(world, pos)),
                TITLE);
    }
}
