package me.trossty.car.client.compat;

import java.util.ArrayList;
import java.util.List;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * What a look-at overlay says about a car: fuel or charge, the body, the part
 * in the worst shape, and how far it has been. Jade and WTHIT both draw these
 * same lines. Everything here is already tracked on the client, so neither
 * needs anything from the server.
 */
final class CarLookLines {

    private CarLookLines() {
    }

    static List<Component> of(CarEntity car) {
        List<Component> lines = new ArrayList<>();
        int tank = Math.round(100.0F * car.getFuel() / Math.max(1.0F, car.getMaxFuel()));
        if (car.isElectric()) {
            lines.add(Component.translatable("jade.car.charge", tank).withStyle(ChatFormatting.AQUA));
        } else {
            lines.add(Component.translatable("jade.car.fuel", tank,
                    Component.translatable("fuel.car." + car.getTankFuel().key())).withStyle(ChatFormatting.GOLD));
            if (car.hasFuel() && car.getDirtyFuel() >= 0.25F) {
                lines.add(Component.translatable("obd.car.dirty_fuel").withStyle(ChatFormatting.RED));
            }
        }
        lines.add(Component.translatable("jade.car.body", Math.round(100.0F * car.damage().getHealthFraction())));
        CarPart worst = null;
        for (CarPart part : CarPart.values()) {
            if (!car.hasPart(part)) {
                continue;
            }
            if (worst == null || car.parts().getConditionFraction(part) < car.parts().getConditionFraction(worst)) {
                worst = part;
            }
        }
        if (worst != null && car.parts().getConditionFraction(worst) < 0.5F) {
            Component name = Component.translatable("part.car." + worst.key());
            lines.add(car.parts().isFitted(worst)
                    ? Component.translatable("jade.car.worst", name, Math.round(100.0F * car.parts().getConditionFraction(worst)))
                            .withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("jade.car.missing", name).withStyle(ChatFormatting.RED));
        }
        lines.add(Component.translatable("jade.car.odometer", CarEntity.formatOdometer(car.parts().getOdometer()))
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
