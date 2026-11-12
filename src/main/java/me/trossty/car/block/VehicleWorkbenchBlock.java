package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.workbench.VehicleWorkbenchScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The bench you build vehicles at. It holds nothing of its own: the window's
 * grid is scratch space handed back when you close it, exactly like a vanilla
 * crafting table, which is why this needs no block entity.
 */
public class VehicleWorkbenchBlock extends Block {

    public static final MapCodec<VehicleWorkbenchBlock> CODEC = createCodec(VehicleWorkbenchBlock::new);

    private static final Text TITLE = Text.translatable("container.car.vehicle_workbench");

    public VehicleWorkbenchBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        player.openHandledScreen(this.createFactory(world, pos));
        return ActionResult.CONSUME;
    }

    /**
     * Declared as well as used in {@link #onUse}, so anything that asks the
     * block for its screen — a test, a mod, a command — gets the same window a
     * right click would open instead of nothing.
     */
    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return this.createFactory(world, pos);
    }

    private NamedScreenHandlerFactory createFactory(World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new VehicleWorkbenchScreenHandler(
                        syncId, inventory, ScreenHandlerContext.create(world, pos)),
                TITLE);
    }
}
