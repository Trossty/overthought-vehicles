package me.trossty.car.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block with a block entity that is drawn from its block model. On 1.21.1 a
 * {@link BaseEntityBlock} is invisible unless it says otherwise, as if its
 * renderer drew all of it; every block of the mod has a model of its own.
 */
public abstract class ModelEntityBlock extends BaseEntityBlock {

    protected ModelEntityBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
