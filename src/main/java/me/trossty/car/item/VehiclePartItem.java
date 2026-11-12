package me.trossty.car.item;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/**
 * A spare: an engine, a gearbox, a set of springs or a wheel.
 *
 * <p>The only thing this adds to a plain item is the line that says how worn it
 * is. Everything the workbench makes comes out new and carries no condition at
 * all; the ones that do carry it came off a car at the lift, and refitting one
 * puts exactly that much life back in.
 */
public class VehiclePartItem extends Item {

    public VehiclePartItem(Properties settings) {
        super(settings);
    }

    /**
     * A part that comes in kinds is called by its kind: nobody asks the
     * parts counter for "an engine".
     */
    @Override
    public Component getName(ItemStack stack) {
        CarPart part = CarPart.forItem(this);
        int variant = CarEntity.variantOf(stack);
        if (part == null || CarSetup.variantsOf(part) == 1) {
            return super.getName(stack);
        }
        return nameOf(part, variant);
    }

    /** What a part of this kind is called, on the item and on the car alike. */
    public static Component nameOf(CarPart part, int variant) {
        return switch (part) {
            case ENGINE -> variant == CarSetup.ELECTRIC_MOTOR ? Component.translatable("item.car.engine.electric")
                    : Component.translatable("item.car.engine.kind",
                    Component.translatable("engine.car." + CarSetup.layoutOf(variant).key()),
                    Component.translatable("fuel.car." + CarSetup.fuelOf(variant).key() + ".short"));
            case WHEELS -> Component.translatable("item.car.wheel.kind",
                    Component.translatable("tyre.car." + CarSetup.Tyre.byOrdinal(variant).key()));
            case AIR_FILTER -> Component.translatable(
                    variant == 1 ? "item.car.air_filter.open" : "item.car.air_filter");
            default -> Component.translatable("part.car." + part.key());
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                              TooltipDisplay display,
                              Consumer<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, display, tooltip, type);
        // The grade is always shown, even on a standard part: three grades you
        // cannot compare at a glance may as well be one.
        CarPart part = CarPart.forItem(this);
        if (part != null && CarSetup.variantsOf(part) > 1) {
            // What this kind does, in a line, where you choose between them.
            int variant = CarEntity.variantOf(stack);
            switch (part) {
                case ENGINE -> {
                    tooltip.accept(Component.translatable("tooltip.car.layout."
                            + CarSetup.layoutOf(variant).key()).withStyle(ChatFormatting.DARK_AQUA));
                    tooltip.accept(Component.translatable("tooltip.car.fuel."
                            + CarSetup.fuelOf(variant).key()).withStyle(ChatFormatting.DARK_AQUA));
                }
                case WHEELS -> tooltip.accept(Component.translatable("tooltip.car.tyre."
                        + CarSetup.Tyre.byOrdinal(variant).key()).withStyle(ChatFormatting.DARK_AQUA));
                default -> tooltip.accept(Component.translatable(variant == 1
                        ? "tooltip.car.filter.open" : "tooltip.car.filter.paper")
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        CarPart.Quality quality = CarEntity.qualityOf(stack);
        tooltip.accept(Component.translatable("tooltip.car.part_quality",
                        Component.translatable("quality.car." + quality.key()))
                .withStyle(colorOf(quality)));
        if (part != null && CarEntity.gradeIsFelt(part) && quality.performance() != 1.0F) {
            // A grade is felt on the road as well as in how long the part lasts.
            int percent = Math.round((quality.performance() - 1.0F) * 100.0F);
            tooltip.accept(Component.translatable("tooltip.car.grade_effect",
                            (percent > 0 ? "+" : "") + percent,
                            Component.translatable("part.car." + part.key() + ".effect"))
                    .withStyle(colorOf(quality)));
        }
        Integer condition = stack.get(ModComponents.PART_CONDITION);
        if (condition != null) {
            tooltip.accept(Component.translatable("tooltip.car.part_condition", condition)
                    .withStyle(condition < 35 ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
    }

    /** Each tier in its own material's colour, the way an item's rarity is. */
    private static ChatFormatting colorOf(CarPart.Quality quality) {
        return quality.formatting();
    }
}
