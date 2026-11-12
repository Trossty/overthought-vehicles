package me.trossty.car.item;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.registry.ModComponents;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

    public VehiclePartItem(Settings settings) {
        super(settings);
    }

    /**
     * A part that comes in kinds is called by its kind: nobody asks the
     * parts counter for "an engine".
     */
    @Override
    public Text getName(ItemStack stack) {
        CarPart part = CarPart.forItem(this);
        int variant = CarEntity.variantOf(stack);
        if (part == null || CarSetup.variantsOf(part) == 1) {
            return super.getName(stack);
        }
        return nameOf(part, variant);
    }

    /** What a part of this kind is called, on the item and on the car alike. */
    public static Text nameOf(CarPart part, int variant) {
        return switch (part) {
            case ENGINE -> variant == CarSetup.ELECTRIC_MOTOR ? Text.translatable("item.car.engine.electric")
                    : Text.translatable("item.car.engine.kind",
                    Text.translatable("engine.car." + CarSetup.layoutOf(variant).key()),
                    Text.translatable("fuel.car." + CarSetup.fuelOf(variant).key() + ".short"));
            case WHEELS -> Text.translatable("item.car.wheel.kind",
                    Text.translatable("tyre.car." + CarSetup.Tyre.byOrdinal(variant).key()));
            case AIR_FILTER -> Text.translatable(
                    variant == 1 ? "item.car.air_filter.open" : "item.car.air_filter");
            default -> Text.translatable("part.car." + part.key());
        };
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, display, tooltip, type);
        // The grade is always shown, even on a standard part: three grades you
        // cannot compare at a glance may as well be one.
        CarPart part = CarPart.forItem(this);
        if (part != null && CarSetup.variantsOf(part) > 1) {
            // What this kind does, in a line, where you choose between them.
            int variant = CarEntity.variantOf(stack);
            switch (part) {
                case ENGINE -> {
                    tooltip.accept(Text.translatable("tooltip.car.layout."
                            + CarSetup.layoutOf(variant).key()).formatted(Formatting.DARK_AQUA));
                    tooltip.accept(Text.translatable("tooltip.car.fuel."
                            + CarSetup.fuelOf(variant).key()).formatted(Formatting.DARK_AQUA));
                }
                case WHEELS -> tooltip.accept(Text.translatable("tooltip.car.tyre."
                        + CarSetup.Tyre.byOrdinal(variant).key()).formatted(Formatting.DARK_AQUA));
                default -> tooltip.accept(Text.translatable(variant == 1
                        ? "tooltip.car.filter.open" : "tooltip.car.filter.paper")
                        .formatted(Formatting.DARK_AQUA));
            }
        }
        CarPart.Quality quality = CarEntity.qualityOf(stack);
        tooltip.accept(Text.translatable("tooltip.car.part_quality",
                        Text.translatable("quality.car." + quality.key()))
                .formatted(colorOf(quality)));
        if (part != null && CarEntity.gradeIsFelt(part) && quality.performance() != 1.0F) {
            // A grade is felt on the road as well as in how long the part lasts.
            int percent = Math.round((quality.performance() - 1.0F) * 100.0F);
            tooltip.accept(Text.translatable("tooltip.car.grade_effect",
                            (percent > 0 ? "+" : "") + percent,
                            Text.translatable("part.car." + part.key() + ".effect"))
                    .formatted(colorOf(quality)));
        }
        Integer condition = stack.get(ModComponents.PART_CONDITION);
        if (condition != null) {
            tooltip.accept(Text.translatable("tooltip.car.part_condition", condition)
                    .formatted(condition < 35 ? Formatting.RED : Formatting.GRAY));
        }
    }

    /** Each tier in its own material's colour, the way an item's rarity is. */
    private static Formatting colorOf(CarPart.Quality quality) {
        return quality.formatting();
    }
}
