package me.trossty.car.item;

import me.trossty.car.block.AsphaltBlock;
import me.trossty.car.block.AsphaltBlock.Marking;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModComponents;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

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

    public RoadPaintItem(AsphaltBlock.Paint paint, Settings settings) {
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
        return tin.getDamage() >= tin.getMaxDamage();
    }

    /**
     * Opens the marking menu; set by the client, a no-op on a server. The
     * menu lives client side, so the common item only asks for it.
     */
    public static java.util.function.Consumer<Hand> openMenu = hand -> {
    };

    /** Used in the air, or sneaking anywhere: the marking menu. */
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            openMenu.accept(hand);
        }
        return ActionResult.SUCCESS;
    }

    /** The choice made in the menu, checked against what is really in the hand. */
    public static void choose(PlayerEntity player, Hand hand, int index) {
        ItemStack tin = player.getStackInHand(hand);
        if (!(tin.getItem() instanceof RoadPaintItem) || index < 0 || index >= Marking.values().length) {
            return;
        }
        Marking marking = Marking.values()[index];
        select(tin, marking);
        player.sendMessage(Text.translatable("message.car.marking_selected",
                Text.translatable(marking.translationKey()).formatted(Formatting.WHITE),
                marking.ordinal() + 1, Marking.values().length).formatted(Formatting.GRAY), true);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        ItemStack tin = context.getStack();
        if (player != null && player.isSneaking()) {
            if (context.getWorld().isClient()) {
                openMenu.accept(context.getHand());
            }
            return ActionResult.SUCCESS;
        }
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(ModBlocks.ASPHALT)) {
            // Not road: falls through to use(), which opens the menu.
            return ActionResult.PASS;
        }
        BlockState painted = paint(state, selected(tin), this.paint, context.getHorizontalPlayerFacing());
        if (painted == state) {
            return ActionResult.PASS;
        }
        boolean erasing = painted.get(AsphaltBlock.MARKING) == Marking.NONE;
        if (!erasing && isEmpty(tin)) {
            if (player != null && world.isClient()) {
                player.sendMessage(Text.translatable("message.car.paint_empty").formatted(Formatting.GRAY), true);
            }
            return ActionResult.FAIL;
        }
        if (!world.isClient()) {
            world.setBlockState(pos, painted, net.minecraft.block.Block.NOTIFY_ALL);
            boolean fresh = state.get(AsphaltBlock.MARKING) != painted.get(AsphaltBlock.MARKING)
                    || state.get(AsphaltBlock.PAINT) != painted.get(AsphaltBlock.PAINT);
            if (!erasing && fresh && (player == null || !player.isInCreativeMode())) {
                tin.setDamage(Math.min(tin.getMaxDamage(), tin.getDamage() + 1));
            }
            double top = pos.getY() + AsphaltBlock.height(painted);
            world.playSound(null, pos.getX() + 0.5, top, pos.getZ() + 0.5,
                    erasing ? SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC : SoundEvents.ITEM_DYE_USE,
                    SoundCategory.BLOCKS, 0.8F, erasing ? 1.0F : 0.8F + world.getRandom().nextFloat() * 0.2F);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * What a road looks like after this marking in this paint is put on it by
     * someone facing this way. The same marking in the same paint again turns
     * it a quarter clockwise instead; erase takes any marking off.
     */
    public static BlockState paint(BlockState road, Marking marking, AsphaltBlock.Paint paint, Direction facing) {
        if (marking == Marking.NONE) {
            return road.get(AsphaltBlock.MARKING) == Marking.NONE ? road
                    : road.with(AsphaltBlock.MARKING, Marking.NONE);
        }
        if (road.get(AsphaltBlock.MARKING) == marking && road.get(AsphaltBlock.PAINT) == paint) {
            return road.with(AsphaltBlock.FACING, road.get(AsphaltBlock.FACING).rotateYClockwise());
        }
        return road.with(AsphaltBlock.MARKING, marking)
                .with(AsphaltBlock.PAINT, paint)
                .with(AsphaltBlock.FACING, facing);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, display, tooltip, type);
        Marking marking = selected(stack);
        tooltip.accept(Text.translatable("tooltip.car.marking",
                Text.translatable(marking.translationKey()).formatted(Formatting.WHITE)).formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("tooltip.car.spray_charges",
                stack.getMaxDamage() - stack.getDamage(), stack.getMaxDamage()).formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("tooltip.car.marking_hint").formatted(Formatting.DARK_GRAY));
    }
}
