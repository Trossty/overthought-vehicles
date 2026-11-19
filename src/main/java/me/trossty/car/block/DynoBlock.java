package me.trossty.car.block;

import me.trossty.car.entity.CarEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rollers in the floor a car is driven on to: a click runs it up and prints
 * what it actually does — top speed, pull away, stopping distance, which way it
 * drifts and what its fluids read — so a repair can be measured, not guessed.
 */
public class DynoBlock extends ModelEntityBlock {

    public static final com.mojang.serialization.MapCodec<DynoBlock> CODEC = simpleCodec(DynoBlock::new);

    /**
     * Never placed: the state the renderer draws a roller from, so the
     * rollers can turn while the rest of the block stands still.
     */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ROLLER =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("roller");

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    /** The speed a stop is measured from, blocks a second. */
    private static final float TEST_SPEED = 10.0F;

    public DynoBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(ROLLER, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROLLER);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new me.trossty.car.block.entity.DynoBlockEntity(pos, state);
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level world, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return world.isClientSide() ? createTickerHelper(type, me.trossty.car.registry.ModBlockEntities.DYNO,
                me.trossty.car.block.entity.DynoBlockEntity::clientTick) : null;
    }

    /** The car standing on these rollers, if there is one. */
    public static @org.jetbrains.annotations.Nullable CarEntity carOn(Level world, BlockPos pos) {
        AABB over = new AABB(pos).inflate(0.6, 0.0, 0.6).expandTowards(0.0, 1.5, 0.0);
        return world.getEntitiesOfClass(CarEntity.class, over, c -> true).stream().findFirst().orElse(null);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        CarEntity car = carOn(world, pos);
        if (car == null) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.dyno_empty"), true);
            return InteractionResult.CONSUME;
        }
        world.playSound(null, pos, SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.6F, 0.7F);
        for (Component line : readout(car)) {
            player.sendSystemMessage(line);
        }
        return InteractionResult.CONSUME;
    }

    /** What the rollers measure, a line each. Public so a test can read it too. */
    public static java.util.List<Component> readout(CarEntity car) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        float top = car.getMaxSpeed() * 20.0F;
        float pull = car.throttleStep() * 400.0F;
        float brake = car.brakeStrength() * 400.0F;
        float stop = brake <= 0.0F ? Float.POSITIVE_INFINITY : TEST_SPEED * TEST_SPEED / (2.0F * brake);
        lines.add(Component.translatable("dyno.overthought_vehicles.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.translatable("dyno.overthought_vehicles.top", String.format(java.util.Locale.ROOT, "%.1f", top)));
        lines.add(Component.translatable("dyno.overthought_vehicles.pull", String.format(java.util.Locale.ROOT, "%.1f", pull)));
        lines.add(Component.translatable("dyno.overthought_vehicles.stop", Float.isInfinite(stop) ? "∞"
                : String.format(java.util.Locale.ROOT, "%.1f", stop)));
        float drift = car.pull();
        lines.add(Math.abs(drift) < 0.01F ? Component.translatable("dyno.overthought_vehicles.straight")
                : Component.translatable(drift < 0.0F ? "dyno.overthought_vehicles.drifts_left" : "dyno.overthought_vehicles.drifts_right",
                String.format(java.util.Locale.ROOT, "%.2f", Math.abs(drift))));
        if (car.upkeep().hasOil()) {
            lines.add(Component.translatable("dyno.overthought_vehicles.oil", Math.round(car.upkeep().getOilLevel() * 100.0F),
                    Math.round(car.upkeep().getOilAge() * 100.0F)));
        }
        lines.add(Component.translatable("dyno.overthought_vehicles.fluids", Math.round(car.upkeep().getCoolantLevel() * 100.0F),
                Math.round(car.upkeep().getBrakeFluid() * 100.0F)));
        if (car.hasPart(me.trossty.car.entity.CarPart.WHEELS) && car.parts().isFitted(me.trossty.car.entity.CarPart.WHEELS)) {
            // A new tyre has eight millimetres of tread; the legal limit is 1.6.
            float tread = 1.6F + 6.4F * car.parts().getConditionFraction(me.trossty.car.entity.CarPart.WHEELS);
            lines.add(Component.translatable("dyno.overthought_vehicles.tyres", String.format(java.util.Locale.ROOT, "%.1f", tread),
                    Math.round(car.upkeep().getTyrePressure() * 100.0F)));
        }
        if (car.upkeep().hasOil()) {
            lines.add(Component.translatable("dyno.overthought_vehicles.service", Math.round(car.upkeep().getBeltWear() * 100.0F),
                    Math.round(car.upkeep().getCatClog() * 100.0F)));
        }
        if (car.upkeep().isOnDyno() && Math.abs(car.getEngineSpeed()) > 0.001F) {
            lines.add(Component.translatable("dyno.overthought_vehicles.rolling",
                    String.format(java.util.Locale.ROOT, "%.1f", Math.abs(car.getEngineSpeed()) * 20.0F)));
        }
        return lines;
    }
}
