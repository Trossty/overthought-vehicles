package me.trossty.car.item;

import me.trossty.car.block.entity.PumpjackBlockEntity;
import me.trossty.car.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * The seismic survey kit: a sledgehammer and a plate. Struck on the ground,
 * the echo says whether there is oil-bearing rock under the hammer, how deep
 * and roughly how much; failing that, which way the nearest is, as far as
 * the echo carries.
 *
 * <p>It is the honest way to put a pumpjack down. A seep on the surface still
 * gives a deposit away, but only one sitting right under it.
 */
public class SeismicSurveyItem extends Item {

    /** How far to each side the echo carries. */
    public static final int RANGE = 16;
    /** Deposits are laid no higher than this, so nothing above is looked at. */
    private static final int TOP = 51;
    /** Between strikes: the plate rings for a while. */
    private static final int COOLDOWN = 40;
    /** Past this many blocks a deposit's size is just "more than". */
    private static final int MOST = 400;

    public SeismicSurveyItem(Properties settings) {
        super(settings);
    }

    /** What a strike at {@code pos} finds. */
    public record Survey(@Nullable BlockPos nearest, int belowHere, int depositBlocks) {
        public boolean underfoot() {
            return this.belowHere > 0;
        }
    }

    public static Survey survey(Level world, BlockPos pos) {
        // What a pumpjack standing here would reach.
        int below = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int top = Math.min(pos.getY() - 1, TOP);
        int bottom = world.getMinBuildHeight();
        for (int y = top; y >= bottom; y--) {
            for (int dx = -PumpjackBlockEntity.REACH; dx <= PumpjackBlockEntity.REACH; dx++) {
                for (int dz = -PumpjackBlockEntity.REACH; dz <= PumpjackBlockEntity.REACH; dz++) {
                    at.set(pos.getX() + dx, y, pos.getZ() + dz);
                    if (world.getBlockState(at).is(ModBlocks.OIL_DEPOSIT)) {
                        below++;
                    }
                }
            }
        }
        // The nearest deposit block in reach of the echo, nearest across first, then shallowest.
        BlockPos nearest = null;
        long best = Long.MAX_VALUE;
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dz = -RANGE; dz <= RANGE; dz++) {
                int x = pos.getX() + dx;
                int z = pos.getZ() + dz;
                if (!world.hasChunkAt(at.set(x, pos.getY(), z))) {
                    continue;
                }
                long across = (long) dx * dx + (long) dz * dz;
                if (across * 1024 >= best) {
                    continue;
                }
                for (int y = top; y >= bottom; y--) {
                    if (world.getBlockState(at.set(x, y, z)).is(ModBlocks.OIL_DEPOSIT)) {
                        long score = across * 1024 + (pos.getY() - y);
                        if (score < best) {
                            best = score;
                            nearest = at.immutable();
                        }
                        break;
                    }
                }
            }
        }
        return new Survey(nearest, below, nearest == null ? 0 : size(world, nearest));
    }

    /** How many blocks the deposit holding {@code start} runs to, up to {@link #MOST}. */
    private static int size(Level world, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && seen.size() < MOST) {
            BlockPos here = queue.poll();
            for (Direction side : Direction.values()) {
                BlockPos next = here.relative(side);
                if (!seen.contains(next) && world.hasChunkAt(next)
                        && world.getBlockState(next).is(ModBlocks.OIL_DEPOSIT)) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return seen.size();
    }

    /** Buckets of crude in this many deposit blocks. */
    public static int buckets(int blocks) {
        return blocks * PumpjackBlockEntity.CRUDE_PER_DEPOSIT / 1000;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }
        world.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35F, 0.5F);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        context.getItemInHand().hurtAndBreak(1, player,
                context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        Survey found = survey(world, pos.above());
        player.displayClientMessage(report(found, pos.above()), false);
        return InteractionResult.CONSUME;
    }

    public static Component report(Survey found, BlockPos from) {
        if (found.nearest() == null) {
            return Component.translatable("message.overthought_vehicles.survey_none", RANGE).withStyle(ChatFormatting.GRAY);
        }
        int depth = from.getY() - found.nearest().getY();
        if (found.underfoot()) {
            return Component.translatable("message.overthought_vehicles.survey_here", buckets(found.belowHere()), depth)
                    .withStyle(ChatFormatting.GREEN);
        }
        int dx = found.nearest().getX() - from.getX();
        int dz = found.nearest().getZ() - from.getZ();
        int distance = (int) Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
        return Component.translatable("message.overthought_vehicles.survey_away", distance,
                Component.translatable("direction.overthought_vehicles." + compass(dx, dz)), depth,
                found.depositBlocks() >= MOST ? Component.translatable("message.overthought_vehicles.survey_huge")
                        : Component.literal(String.valueOf(buckets(found.depositBlocks())))).withStyle(ChatFormatting.YELLOW);
    }

    /** One of eight points, north being towards negative z. */
    static String compass(int dx, int dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        String[] points = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
        return points[(int) Math.floorMod(Math.round(angle / 45.0), 8L)];
    }
}
