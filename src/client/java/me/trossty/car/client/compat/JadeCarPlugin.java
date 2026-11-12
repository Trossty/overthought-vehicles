package me.trossty.car.client.compat;

import me.trossty.car.CarMain;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * What Jade shows when you look at a car: fuel or charge, the body, the part
 * in the worst shape, and how far it has been. Everything here is already
 * tracked on the client, so no server side is needed.
 *
 * <p>Loaded only by Jade, through the {@code jade} entrypoint: without Jade
 * this class is never touched and the mod runs the same.
 */
public class JadeCarPlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(CarInfo.INSTANCE, CarEntity.class);
    }

    enum CarInfo implements IEntityComponentProvider {
        INSTANCE;

        private static final Identifier UID = CarMain.id("car_info");

        @Override
        public Identifier getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof CarEntity car)) {
                return;
            }
            int tank = Math.round(100.0F * car.getFuel() / Math.max(1.0F, car.getMaxFuel()));
            if (car.isElectric()) {
                tooltip.add(Text.translatable("jade.car.charge", tank).formatted(Formatting.AQUA));
            } else {
                tooltip.add(Text.translatable("jade.car.fuel", tank,
                        Text.translatable("fuel.car." + car.getTankFuel().key())).formatted(Formatting.GOLD));
                if (car.hasFuel() && car.getDirtyFuel() >= 0.25F) {
                    tooltip.add(Text.translatable("obd.car.dirty_fuel").formatted(Formatting.RED));
                }
            }
            tooltip.add(Text.translatable("jade.car.body", Math.round(100.0F * car.getHealthFraction())));
            CarPart worst = null;
            for (CarPart part : CarPart.values()) {
                if (!car.hasPart(part)) {
                    continue;
                }
                if (worst == null || car.getConditionFraction(part) < car.getConditionFraction(worst)) {
                    worst = part;
                }
            }
            if (worst != null && car.getConditionFraction(worst) < 0.5F) {
                Text name = Text.translatable("part.car." + worst.key());
                tooltip.add(car.isFitted(worst)
                        ? Text.translatable("jade.car.worst", name, Math.round(100.0F * car.getConditionFraction(worst)))
                                .formatted(Formatting.YELLOW)
                        : Text.translatable("jade.car.missing", name).formatted(Formatting.RED));
            }
            tooltip.add(Text.translatable("jade.car.odometer", (int) (car.getOdometer() / 1000.0F))
                    .formatted(Formatting.GRAY));
        }
    }
}
