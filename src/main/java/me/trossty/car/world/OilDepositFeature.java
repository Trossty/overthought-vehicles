package me.trossty.car.world;

import com.mojang.serialization.Codec;
import me.trossty.car.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * An oil deposit: a lens of oil-bearing rock somewhere under the surface, a
 * few blocks across and a couple deep, with a seep at the surface over it to
 * give it away. Placed rarely, across the whole overworld.
 */
public class OilDepositFeature extends Feature<NoneFeatureConfiguration> {

    public OilDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int surface = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        int floor = world.getMinBuildHeight() + 12;
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
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        BlockState rock = ModBlocks.OIL_DEPOSIT.defaultBlockState();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    float d = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
                    if (d > 1.0F || (d > 0.7F && random.nextInt(3) == 0)) {
                        continue;
                    }
                    at.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (world.getBlockState(at).is(BlockTags.BASE_STONE_OVERWORLD)) {
                        world.setBlock(at, rock, 2);
                        placed++;
                    }
                }
            }
        }
        if (placed == 0) {
            return false;
        }

        // The seep: on dry land, straight over the middle, if the ground takes it.
        int top = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centre.getX(), centre.getZ());
        BlockPos seep = new BlockPos(centre.getX(), top, centre.getZ());
        BlockState seepState = ModBlocks.OIL_SEEP.defaultBlockState();
        if (world.getBlockState(seep).isAir() && world.getFluidState(seep).isEmpty()
                && seepState.canSurvive(world, seep)) {
            world.setBlock(seep, seepState, 2);
        }
        return true;
    }
}
