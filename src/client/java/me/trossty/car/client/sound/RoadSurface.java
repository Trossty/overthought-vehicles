package me.trossty.car.client.sound;

import me.trossty.car.entity.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the wheels are running on, as the ear and the seat feel it. Works from
 * the block's sound group, so any block from any mod that sounds like gravel
 * drives like gravel too.
 */
public final class RoadSurface {

    private RoadSurface() {
    }

    /** The block the wheels are on: a snow layer or carpet on top wins over the block below. */
    public static BlockState under(CarEntity car) {
        BlockPos pos = car.blockPosition();
        BlockState here = car.level().getBlockState(pos);
        if (!here.isAir() && !here.getCollisionShape(car.level(), pos).isEmpty()) {
            return here;
        }
        return car.getBlockStateOn();
    }

    /**
     * How rough a surface is, 0 for glass-smooth to 1 for loose gravel. Stone,
     * concrete and the like — the closest Minecraft has to tarmac — are nearly
     * silent; loose and soft ground is loud.
     */
    public static float roughness(BlockState state) {
        if (state.isAir()) {
            return 0.0F;
        }
        SoundType group = state.getSoundType();
        if (group == SoundType.GRAVEL || group == SoundType.SUSPICIOUS_GRAVEL) {
            return 1.0F;
        }
        if (group == SoundType.SAND || group == SoundType.SOUL_SAND
                || group == SoundType.ROOTED_DIRT || group == SoundType.MUD) {
            return 0.8F;
        }
        if (group == SoundType.GRASS || group == SoundType.SNOW
                || group == SoundType.POWDER_SNOW) {
            return 0.65F;
        }
        if (group == SoundType.WOOD || group == SoundType.NETHERRACK) {
            return 0.5F;
        }
        if (group == SoundType.STONE) {
            return 0.12F;
        }
        return 0.35F;
    }

    /** How much the car is shaking right now: surface roughness times how fast it is going over it. */
    public static float shake(CarEntity car) {
        if (!car.onGround()) {
            return 0.0F;
        }
        float speed = Mth.clamp(Math.abs(car.getEngineSpeed()) / CarEntity.MAX_SPEED, 0.0F, 1.0F);
        return roughness(under(car)) * speed;
    }
}
