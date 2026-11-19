package me.trossty.car.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A turbocharger, which says on the box what it does and where it goes. */
public class TurboItem extends Item {

    public TurboItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                java.util.List<Component> lines, TooltipFlag type) {
        super.appendHoverText(stack, context, lines, type);
        Consumer<Component> tooltip = lines::add;
        tooltip.accept(Component.translatable("tooltip.overthought_vehicles.turbo").withStyle(ChatFormatting.DARK_AQUA));
    }
}
