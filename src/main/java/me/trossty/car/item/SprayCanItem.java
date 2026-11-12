package me.trossty.car.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A can of car paint. Filled at a crafting table with dyes, which mix the way
 * they do on leather armour, so a car can be any colour at all rather than one
 * of sixteen. Each car it paints uses one charge; durability is the gauge.
 *
 * <p>The colour lives in the vanilla {@code dyed_color} component, so the
 * item model tints itself with the stock dye tint source.
 */
public class SprayCanItem extends Item {

    /** Whole cars one can paints before it needs refilling. */
    public static final int CHARGES = 10;

    public SprayCanItem(Properties settings) {
        super(settings);
    }

    /** The paint in the can as 0xRRGGBB, or null if it has never been filled. */
    public static Integer colourOf(ItemStack can) {
        DyedItemColor dyed = can.get(DataComponents.DYED_COLOR);
        return dyed == null ? null : dyed.rgb() & 0xFFFFFF;
    }

    public static boolean isEmpty(ItemStack can) {
        return can.getDamageValue() >= can.getMaxDamage();
    }

    /** Uses one charge. The can is kept when empty: it is refilled, not thrown away. */
    public static void use(ItemStack can) {
        can.setDamageValue(Math.min(can.getMaxDamage(), can.getDamageValue() + 1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                              TooltipDisplay display,
                              Consumer<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, display, tooltip, type);
        Integer colour = colourOf(stack);
        if (colour == null || isEmpty(stack)) {
            tooltip.accept(Component.translatable("tooltip.car.spray_empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.accept(Component.translatable("tooltip.car.spray_colour",
                        Component.literal(String.format("#%06X", colour)).withColor(colour))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.car.spray_charges",
                stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
    }
}
