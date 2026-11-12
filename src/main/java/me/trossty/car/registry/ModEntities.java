package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.PickupEntity;
import me.trossty.car.entity.TankerEntity;
import me.trossty.car.entity.TrailerEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import me.trossty.car.entity.MotorcycleEntity;

public final class ModEntities {

    public static final EntityType<CarEntity> CAR = register(
            "car",
            EntityType.Builder.of(CarEntity::new, MobCategory.MISC)
                    // Roof height. A seated player reaches 2.0 above the seat,
                    // and the seat is level with the cabin floor at 0.75.
                    .sized(1.6F, 2.1875F)
                    .eyeHeight(1.4F)
                    .clientTrackingRange(10));

    /** Same shell as a car, so the cabin and the seats stay exactly as they are. */
    public static final EntityType<TankerEntity> FUEL_TANKER = register(
            "fuel_tanker",
            EntityType.Builder.of(TankerEntity::new, MobCategory.MISC)
                    .sized(1.6F, 2.1875F)
                    .eyeHeight(1.4F)
                    .clientTrackingRange(10));

    /**
     * Longer than a car and taller at the back, but the cabin is the car's, so
     * the collision box stays the car's too — the bed hangs over it the way the
     * tanker's barrel does.
     */
    public static final EntityType<PickupEntity> PICKUP = register(
            "pickup",
            EntityType.Builder.of(PickupEntity::new, MobCategory.MISC)
                    .sized(1.6F, 2.1875F)
                    .eyeHeight(1.4F)
                    .clientTrackingRange(10));

    /** Lower than a car, and no cabin: nobody stands up inside a trailer. */
    public static final EntityType<TrailerEntity> TRAILER = register(
            "trailer",
            EntityType.Builder.of(TrailerEntity::new, MobCategory.MISC)
                    .sized(1.6F, 1.3F)
                    .clientTrackingRange(10));

    /** Narrow, so it threads between things a car cannot. */
    public static final EntityType<MotorcycleEntity> MOTORCYCLE = register(
            "motorcycle",
            EntityType.Builder.of(MotorcycleEntity::new, MobCategory.MISC)
                    .sized(0.9F, 1.5F)
                    .eyeHeight(1.2F)
                    .clientTrackingRange(10));

    private ModEntities() {
    }

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(
            String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, CarMain.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key.location().toString()));
    }

    /** Forces class loading so the static fields above are registered. */
    public static void register() {
    }
}
