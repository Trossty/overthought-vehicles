package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.DrainPanBlockEntity;
import me.trossty.car.entity.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A wide, shallow pan slid under a car up on the lift. A click opens the
 * sump plug of the car above it and catches the old oil, which goes out of the
 * pan down a pipe to the refinery like any other crude. Then the car takes
 * fresh oil from the top, under the bonnet.
 */
public class DrainPanBlock extends ModelEntityBlock {

    public static final MapCodec<DrainPanBlock> CODEC = simpleCodec(DrainPanBlock::new);

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0);

    public DrainPanBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrainPanBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level world, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return world.isClientSide() ? null : createTickerHelper(type,
                me.trossty.car.registry.ModBlockEntities.DRAIN_PAN, DrainPanBlockEntity::tick);
    }

    /** The car up on a lift over this pan, if there is one. */
    public static @Nullable CarEntity carAbove(Level world, BlockPos pos) {
        AABB above = new AABB(pos).inflate(1.5, 0.0, 1.5).expandTowards(0.0, 4.0, 0.0);
        for (CarEntity car : world.getEntitiesOfClass(CarEntity.class, above, car -> car.upkeep().hasOil())) {
            if (VehicleLiftBlock.isServicing(world, car)) {
                return car;
            }
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof DrainPanBlockEntity pan)) {
            return InteractionResult.CONSUME;
        }
        CarEntity car = carAbove(world, pos);
        if (car == null) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.pan_no_car",
                    pan.millibuckets()), true);
            return InteractionResult.CONSUME;
        }
        if (car.upkeep().getOilLevel() <= 0.0F) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.pan_dry"), true);
            return InteractionResult.CONSUME;
        }
        if (!pan.hasRoomFor(car.upkeep().getOilLevel())) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.pan_full"), true);
            return InteractionResult.CONSUME;
        }
        pan.catchOil(car.upkeep().drainOil());
        world.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.8F, 0.8F);
        player.displayClientMessage(Component.translatable("message.overthought_vehicles.pan_drained", pan.millibuckets()), true);
        return InteractionResult.CONSUME;
    }
}
