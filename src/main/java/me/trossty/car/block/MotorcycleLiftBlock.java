package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/**
 * A motorbike lift: a narrow table the bike is wheeled on to, its front wheel
 * in the clamp at the end, that raises it to about waist height. A car lift's
 * arms have nothing to take a bike by, so a bike goes up on this and a car on
 * the other. It is the same lift otherwise: the same three stops, a right
 * click up and a sneaking one down, nothing worked on while it is down.
 */
public class MotorcycleLiftBlock extends VehicleLiftBlock {

    public static final MapCodec<MotorcycleLiftBlock> CODEC = simpleCodec(MotorcycleLiftBlock::new);

    public MotorcycleLiftBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
