package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.registry.ModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/**
 * Rapeseed: a field crop grown for the oil in its seed, the one farmers press
 * for biodiesel. It grows like wheat and flowers yellow before it sets pods;
 * what it gives is the seed itself, which goes back in the ground or into the
 * refinery's boiler, where it is worth three of anything else organic.
 */
public class RapeseedCropBlock extends CropBlock {

    public static final MapCodec<RapeseedCropBlock> CODEC = simpleCodec(RapeseedCropBlock::new);

    public RapeseedCropBlock(Properties settings) {
        super(settings);
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.RAPESEED;
    }
}
