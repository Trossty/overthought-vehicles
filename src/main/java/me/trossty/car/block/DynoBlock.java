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
public class DynoBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    /** The speed a stop is measured from, blocks a second. */
    private static final float TEST_SPEED = 10.0F;

    public DynoBlock(Properties settings) {
        super(settings);
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
        AABB over = new AABB(pos).inflate(0.6, 0.0, 0.6).expandTowards(0.0, 1.5, 0.0);
        CarEntity car = world.getEntitiesOfClass(CarEntity.class, over, c -> true).stream().findFirst().orElse(null);
        if (car == null) {
            player.displayClientMessage(Component.translatable("message.car.dyno_empty"), true);
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
        lines.add(Component.translatable("dyno.car.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.translatable("dyno.car.top", String.format(java.util.Locale.ROOT, "%.1f", top)));
        lines.add(Component.translatable("dyno.car.pull", String.format(java.util.Locale.ROOT, "%.1f", pull)));
        lines.add(Component.translatable("dyno.car.stop", Float.isInfinite(stop) ? "∞"
                : String.format(java.util.Locale.ROOT, "%.1f", stop)));
        float drift = car.pull();
        lines.add(Math.abs(drift) < 0.01F ? Component.translatable("dyno.car.straight")
                : Component.translatable(drift < 0.0F ? "dyno.car.drifts_left" : "dyno.car.drifts_right",
                String.format(java.util.Locale.ROOT, "%.2f", Math.abs(drift))));
        if (car.hasOil()) {
            lines.add(Component.translatable("dyno.car.oil", Math.round(car.getOilLevel() * 100.0F),
                    Math.round(car.getOilAge() * 100.0F)));
        }
        lines.add(Component.translatable("dyno.car.fluids", Math.round(car.getCoolantLevel() * 100.0F),
                Math.round(car.getBrakeFluid() * 100.0F)));
        return lines;
    }
}
