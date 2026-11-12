package me.trossty.car.client.sound;

import me.trossty.car.entity.CarEntity;
import net.minecraft.block.BlockState;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

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
        BlockPos pos = car.getBlockPos();
        BlockState here = car.getEntityWorld().getBlockState(pos);
        if (!here.isAir() && !here.getCollisionShape(car.getEntityWorld(), pos).isEmpty()) {
            return here;
        }
        return car.getSteppingBlockState();
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
        BlockSoundGroup group = state.getSoundGroup();
        if (group == BlockSoundGroup.GRAVEL || group == BlockSoundGroup.SUSPICIOUS_GRAVEL) {
            return 1.0F;
        }
        if (group == BlockSoundGroup.SAND || group == BlockSoundGroup.SOUL_SAND
                || group == BlockSoundGroup.ROOTED_DIRT || group == BlockSoundGroup.MUD) {
            return 0.8F;
        }
        if (group == BlockSoundGroup.GRASS || group == BlockSoundGroup.SNOW
                || group == BlockSoundGroup.POWDER_SNOW) {
            return 0.65F;
        }
        if (group == BlockSoundGroup.WOOD || group == BlockSoundGroup.NETHERRACK) {
            return 0.5F;
        }
        if (group == BlockSoundGroup.STONE) {
            return 0.12F;
        }
        return 0.35F;
    }

    /** How much the car is shaking right now: surface roughness times how fast it is going over it. */
    public static float shake(CarEntity car) {
        if (!car.isOnGround()) {
            return 0.0F;
        }
        float speed = MathHelper.clamp(Math.abs(car.getEngineSpeed()) / CarEntity.MAX_SPEED, 0.0F, 1.0F);
        return roughness(under(car)) * speed;
    }
}
