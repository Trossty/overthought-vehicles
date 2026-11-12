package me.trossty.car.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

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

    public SprayCanItem(Settings settings) {
        super(settings);
    }

    /** The paint in the can as 0xRRGGBB, or null if it has never been filled. */
    public static Integer colourOf(ItemStack can) {
        DyedColorComponent dyed = can.get(DataComponentTypes.DYED_COLOR);
        return dyed == null ? null : dyed.rgb() & 0xFFFFFF;
    }

    public static boolean isEmpty(ItemStack can) {
        return can.getDamage() >= can.getMaxDamage();
    }

    /** Uses one charge. The can is kept when empty: it is refilled, not thrown away. */
    public static void use(ItemStack can) {
        can.setDamage(Math.min(can.getMaxDamage(), can.getDamage() + 1));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, display, tooltip, type);
        Integer colour = colourOf(stack);
        if (colour == null || isEmpty(stack)) {
            tooltip.accept(Text.translatable("tooltip.car.spray_empty").formatted(Formatting.DARK_GRAY));
            return;
        }
        tooltip.accept(Text.translatable("tooltip.car.spray_colour",
                        Text.literal(String.format("#%06X", colour)).withColor(colour))
                .formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("tooltip.car.spray_charges",
                stack.getMaxDamage() - stack.getDamage(), stack.getMaxDamage()).formatted(Formatting.GRAY));
    }
}
