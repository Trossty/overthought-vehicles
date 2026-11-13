package me.trossty.car.item;

import me.trossty.car.block.AsphaltBlock;
import me.trossty.car.block.AsphaltBlock.Marking;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Consumer;

/**
 * A tin of road paint, one per colour. Used on asphalt it paints the chosen
 * marking on the top, running the way the painter faces; used again on the
 * same marking it turns it a quarter, which is how a bend or an arrow is set
 * the right way round. Used in the air, or sneaking, it opens a menu of
 * markings to pick from; the first one, "erase", scrubs a marking off and
 * costs no paint.
 */
public class RoadPaintItem extends Item {

    /** Markings one tin paints. */
    public static final int CHARGES = 128;

    public final AsphaltBlock.Paint paint;

    public RoadPaintItem(AsphaltBlock.Paint paint, Properties settings) {
        super(settings);
        this.paint = paint;
    }

    /** The marking this tin is set to; a tin fresh off the bench paints a dashed centre line. */
    public static Marking selected(ItemStack tin) {
        return Marking.byIndex(tin.getOrDefault(ModComponents.ROAD_MARKING, Marking.DASHED.ordinal()));
    }

    public static void select(ItemStack tin, Marking marking) {
        tin.set(ModComponents.ROAD_MARKING, marking.ordinal());
    }

    public static boolean isEmpty(ItemStack tin) {
        return tin.getDamageValue() >= tin.getMaxDamage();
    }

    /**
     * Opens the marking menu; set by the client, a no-op on a server. The
     * menu lives client side, so the common item only asks for it.
     */
    public static java.util.function.Consumer<InteractionHand> openMenu = hand -> {
    };

    /** Used in the air, or sneaking anywhere: the marking menu. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            openMenu.accept(hand);
        }
        return net.minecraft.world.InteractionResultHolder.success(user.getItemInHand(hand));
    }

    /** The choice made in the menu, checked against what is really in the hand. */
    public static void choose(Player player, InteractionHand hand, int index) {
        ItemStack tin = player.getItemInHand(hand);
        if (!(tin.getItem() instanceof RoadPaintItem) || index < 0 || index >= Marking.values().length) {
            return;
        }
        Marking marking = Marking.values()[index];
        select(tin, marking);
        player.displayClientMessage(Component.translatable("message.car.marking_selected",
                Component.translatable(marking.translationKey()).withStyle(ChatFormatting.WHITE),
                marking.ordinal() + 1, Marking.values().length).withStyle(ChatFormatting.GRAY), true);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack tin = context.getItemInHand();
        if (player != null && player.isShiftKeyDown()) {
            if (context.getLevel().isClientSide()) {
                openMenu.accept(context.getHand());
            }
            return InteractionResult.SUCCESS;
        }
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        if (!state.is(ModBlocks.ASPHALT)) {
            // Not road: falls through to use(), which opens the menu.
            return InteractionResult.PASS;
        }
        BlockState painted = paint(state, selected(tin), this.paint, context.getHorizontalDirection());
        if (painted == state) {
            return InteractionResult.PASS;
        }
        boolean erasing = painted.getValue(AsphaltBlock.MARKING) == Marking.NONE;
        if (!erasing && isEmpty(tin)) {
            if (player != null && world.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.car.paint_empty").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide()) {
            world.setBlock(pos, painted, net.minecraft.world.level.block.Block.UPDATE_ALL);
            boolean fresh = state.getValue(AsphaltBlock.MARKING) != painted.getValue(AsphaltBlock.MARKING)
                    || state.getValue(AsphaltBlock.PAINT) != painted.getValue(AsphaltBlock.PAINT);
            if (!erasing && fresh && (player == null || !player.hasInfiniteMaterials())) {
                tin.setDamageValue(Math.min(tin.getMaxDamage(), tin.getDamageValue() + 1));
            }
            double top = pos.getY() + AsphaltBlock.height(painted);
            world.playSound(null, pos.getX() + 0.5, top, pos.getZ() + 0.5,
                    erasing ? SoundEvents.BRUSH_GENERIC : SoundEvents.DYE_USE,
                    SoundSource.BLOCKS, 0.8F, erasing ? 1.0F : 0.8F + world.getRandom().nextFloat() * 0.2F);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * What a road looks like after this marking in this paint is put on it by
     * someone facing this way. The same marking in the same paint again turns
     * it a quarter clockwise instead; erase takes any marking off.
     */
    public static BlockState paint(BlockState road, Marking marking, AsphaltBlock.Paint paint, Direction facing) {
        if (marking == Marking.NONE) {
            return road.getValue(AsphaltBlock.MARKING) == Marking.NONE ? road
                    : road.setValue(AsphaltBlock.MARKING, Marking.NONE);
        }
        if (road.getValue(AsphaltBlock.MARKING) == marking && road.getValue(AsphaltBlock.PAINT) == paint) {
            return road.setValue(AsphaltBlock.FACING, road.getValue(AsphaltBlock.FACING).getClockWise());
        }
        return road.setValue(AsphaltBlock.MARKING, marking)
                .setValue(AsphaltBlock.PAINT, paint)
                .setValue(AsphaltBlock.FACING, facing);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                java.util.List<Component> lines, TooltipFlag type) {
        super.appendHoverText(stack, context, lines, type);
        Consumer<Component> tooltip = lines::add;
        Marking marking = selected(stack);
        tooltip.accept(Component.translatable("tooltip.car.marking",
                Component.translatable(marking.translationKey()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.car.spray_charges",
                stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.car.marking_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
