package me.trossty.car.item;

import me.trossty.car.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.function.Consumer;

/**
 * A car key. Blank from the crafting table; held against a car that has no
 * key it becomes that car's, and held against a car whose key you already
 * carry it becomes a copy. Crouch and click to lock or unlock; a car with a key
 * will not start unless the driver has it on them.
 */
public class CarKeyItem extends Item {

    public CarKeyItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                java.util.List<Component> lines, TooltipFlag type) {
        super.appendHoverText(stack, context, lines, type);
        Consumer<Component> tooltip = lines::add;
        if (!stack.has(ModComponents.VEHICLE_KEY)) {
            tooltip.accept(Component.translatable("tooltip.car.key_blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        ResourceLocation vehicle = stack.get(ModComponents.CAR_VEHICLE);
        Component name = vehicle == null
                ? Component.translatable("item.car.car")
                : me.trossty.car.vehicle.VehicleDefinition.nameOf(vehicle);
        tooltip.accept(Component.translatable("tooltip.car.key_for", name).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.car.key_use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
