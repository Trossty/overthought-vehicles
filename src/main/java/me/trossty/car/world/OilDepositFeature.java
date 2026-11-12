package me.trossty.car.world;

import com.mojang.serialization.Codec;
import me.trossty.car.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * An oil deposit: a lens of oil-bearing rock somewhere under the surface, a
 * few blocks across and a couple deep, with a seep at the surface over it to
 * give it away. Placed rarely, across the whole overworld.
 */
public class OilDepositFeature extends Feature<DefaultFeatureConfig> {

    public OilDepositFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();

        int surface = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        int floor = world.getBottomY() + 12;
        int ceiling = Math.min(surface - 12, 48);
        if (ceiling <= floor) {
            return false;
        }
        int depth = floor + random.nextInt(ceiling - floor);
        BlockPos centre = new BlockPos(origin.getX(), depth, origin.getZ());

        float rx = 2.5F + random.nextFloat() * 2.5F;
        float rz = 2.5F + random.nextFloat() * 2.5F;
        float ry = 1.5F + random.nextFloat() * 1.5F;
        int placed = 0;
        BlockPos.Mutable at = new BlockPos.Mutable();
        BlockState rock = ModBlocks.OIL_DEPOSIT.getDefaultState();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    float d = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
                    if (d > 1.0F || (d > 0.7F && random.nextInt(3) == 0)) {
                        continue;
                    }
                    at.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (world.getBlockState(at).isIn(BlockTags.BASE_STONE_OVERWORLD)) {
                        world.setBlockState(at, rock, 2);
                        placed++;
                    }
                }
            }
        }
        if (placed == 0) {
            return false;
        }

        // The seep: on dry land, straight over the middle, if the ground takes it.
        int top = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, centre.getX(), centre.getZ());
        BlockPos seep = new BlockPos(centre.getX(), top, centre.getZ());
        BlockState seepState = ModBlocks.OIL_SEEP.getDefaultState();
        if (world.getBlockState(seep).isAir() && world.getFluidState(seep).isEmpty()
                && seepState.canPlaceAt(world, seep)) {
            world.setBlockState(seep, seepState, 2);
        }
        return true;
    }
}
