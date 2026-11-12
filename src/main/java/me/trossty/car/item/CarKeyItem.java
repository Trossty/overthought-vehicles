package me.trossty.car.item;

import me.trossty.car.registry.ModComponents;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * A car key. Blank from the crafting table; held against a car that has no
 * key it becomes that car's, and held against a car whose key you already
 * carry it becomes a copy. Crouch and click to lock or unlock; a car with a key
 * will not start unless the driver has it on them.
 */
public class CarKeyItem extends Item {

    public CarKeyItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, display, tooltip, type);
        if (!stack.contains(ModComponents.VEHICLE_KEY)) {
            tooltip.accept(Text.translatable("tooltip.car.key_blank").formatted(Formatting.DARK_GRAY));
            return;
        }
        Identifier vehicle = stack.get(ModComponents.CAR_VEHICLE);
        Text name = vehicle == null
                ? Text.translatable("item.car.car")
                : me.trossty.car.vehicle.VehicleDefinition.nameOf(vehicle);
        tooltip.accept(Text.translatable("tooltip.car.key_for", name).formatted(Formatting.GOLD));
        tooltip.accept(Text.translatable("tooltip.car.key_use").formatted(Formatting.DARK_GRAY));
    }
}
