package me.trossty.car.entity;

import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DeadBushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What a farm vehicle does to the ground it rolls over, once for every new
 * block it gets onto: a seed drill sows the seeds in its load into tilled
 * ground, a spreader puts bone meal from its load on the crops, a mower cuts
 * grass and flowers and drops what they leave into the trailer it tows.
 *
 * <p>Drills and spreaders only work hitched up, as they are worked from the
 * tractor; a mower works whenever its engine runs. Each covers a row across
 * as many blocks as the vehicle is wide, a spreader a block further each side.
 */
final class FarmWork {

    private @Nullable BlockPos workedAt;

    void tick(CarEntity vehicle) {
        VehicleDefinition defined = vehicle.getDefinition();
        VehicleDefinition.Implement implement = defined == null ? VehicleDefinition.Implement.NONE
                : defined.implement();
        if (implement == VehicleDefinition.Implement.NONE || !(vehicle.level() instanceof ServerLevel world)) {
            return;
        }
        boolean working = switch (implement) {
            case SEEDER, FERTILIZER -> vehicle instanceof TrailerEntity trailer && trailer.isHitched();
            case MOWER -> vehicle.isEngineRunning();
            case NONE -> false;
        };
        BlockPos at = BlockPos.containing(vehicle.getX(), vehicle.getY() + 0.5, vehicle.getZ());
        if (!working || at.equals(this.workedAt)) {
            return;
        }
        this.workedAt = at;

        int across = Math.max(1, Math.round(defined.width()))
                + (implement == VehicleDefinition.Implement.FERTILIZER ? 2 : 0);
        float yaw = vehicle.getYRot() * Mth.DEG_TO_RAD;
        double leftX = Mth.cos(yaw);
        double leftZ = Mth.sin(yaw);
        BlockPos last = null;
        for (int i = 0; i < across; i++) {
            double offset = i - (across - 1) / 2.0;
            BlockPos cell = BlockPos.containing(vehicle.getX() + leftX * offset, vehicle.getY() + 0.5,
                    vehicle.getZ() + leftZ * offset);
            if (cell.equals(last)) {
                continue;
            }
            last = cell;
            switch (implement) {
                case SEEDER -> sow(world, cell, ((TrailerEntity) vehicle).getLoad());
                case FERTILIZER -> feed(world, cell, ((TrailerEntity) vehicle).getLoad());
                case MOWER -> mow(world, cell, vehicle);
                case NONE -> { }
            }
        }
    }

    /** Puts the first seed in the load that grows there into an empty tilled block. */
    private static void sow(ServerLevel world, BlockPos cell, Container load) {
        if (!world.isEmptyBlock(cell)) {
            return;
        }
        for (int slot = 0; slot < load.getContainerSize(); slot++) {
            ItemStack stack = load.getItem(slot);
            if (stack.getItem() instanceof BlockItem seed
                    && (seed.getBlock() instanceof CropBlock || seed.getBlock() instanceof StemBlock)
                    && seed.getBlock().defaultBlockState().canSurvive(world, cell)) {
                world.setBlockAndUpdate(cell, seed.getBlock().defaultBlockState());
                world.playSound(null, cell, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.shrink(1);
                load.setChanged();
                return;
            }
        }
    }

    /** Spends a bone meal from the load on a crop that is still growing. */
    private static void feed(ServerLevel world, BlockPos cell, Container load) {
        BlockState state = world.getBlockState(cell);
        if (!(state.getBlock() instanceof CropBlock || state.getBlock() instanceof StemBlock)) {
            return;
        }
        for (int slot = 0; slot < load.getContainerSize(); slot++) {
            ItemStack stack = load.getItem(slot);
            if (stack.is(Items.BONE_MEAL)) {
                if (BoneMealItem.applyBonemeal(stack, world, cell, null)) {
                    world.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, cell, 15);
                    load.setChanged();
                }
                return;
            }
        }
    }

    /** Cuts grass and flowers, and loads what they drop into the towed trailer. */
    private static void mow(ServerLevel world, BlockPos cell, CarEntity mower) {
        BlockState state = world.getBlockState(cell);
        Block block = state.getBlock();
        boolean cuttable = block instanceof TallGrassBlock || block instanceof FlowerBlock
                || block instanceof DeadBushBlock
                || block instanceof DoublePlantBlock && !(block instanceof PitcherCropBlock);
        if (!cuttable) {
            return;
        }
        TrailerEntity trailer = mower.towed();
        for (ItemStack drop : Block.getDrops(state, world, cell, null, mower, ItemStack.EMPTY)) {
            ItemStack left = trailer != null && trailer.getLoad() instanceof SimpleContainer load
                    ? load.addItem(drop) : drop;
            if (!left.isEmpty()) {
                Block.popResource(world, cell, left);
            }
        }
        world.destroyBlock(cell, false, mower);
    }
}
