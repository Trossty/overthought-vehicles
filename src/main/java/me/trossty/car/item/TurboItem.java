package me.trossty.car.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/** A turbocharger, which says on the box what it does and where it goes. */
public class TurboItem extends Item {

    public TurboItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, display, tooltip, type);
        tooltip.accept(Text.translatable("tooltip.car.turbo").formatted(Formatting.DARK_AQUA));
    }
}
