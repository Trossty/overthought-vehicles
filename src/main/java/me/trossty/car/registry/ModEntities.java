package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.PickupEntity;
import me.trossty.car.entity.TankerEntity;
import me.trossty.car.entity.TrailerEntity;
import me.trossty.car.entity.MotorcycleEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModEntities {

    public static final EntityType<CarEntity> CAR = register(
            "car",
            EntityType.Builder.create(CarEntity::new, SpawnGroup.MISC)
                    // Roof height. A seated player reaches 2.0 above the seat,
                    // and the seat is level with the cabin floor at 0.75.
                    .dimensions(1.6F, 2.1875F)
                    .eyeHeight(1.4F)
                    .maxTrackingRange(10));

    /** Same shell as a car, so the cabin and the seats stay exactly as they are. */
    public static final EntityType<TankerEntity> FUEL_TANKER = register(
            "fuel_tanker",
            EntityType.Builder.create(TankerEntity::new, SpawnGroup.MISC)
                    .dimensions(1.6F, 2.1875F)
                    .eyeHeight(1.4F)
                    .maxTrackingRange(10));

    /**
     * Longer than a car and taller at the back, but the cabin is the car's, so
     * the collision box stays the car's too — the bed hangs over it the way the
     * tanker's barrel does.
     */
    public static final EntityType<PickupEntity> PICKUP = register(
            "pickup",
            EntityType.Builder.create(PickupEntity::new, SpawnGroup.MISC)
                    .dimensions(1.6F, 2.1875F)
                    .eyeHeight(1.4F)
                    .maxTrackingRange(10));

    /** Lower than a car, and no cabin: nobody stands up inside a trailer. */
    public static final EntityType<TrailerEntity> TRAILER = register(
            "trailer",
            EntityType.Builder.create(TrailerEntity::new, SpawnGroup.MISC)
                    .dimensions(1.6F, 1.3F)
                    .maxTrackingRange(10));

    /** Narrow, so it threads between things a car cannot. */
    public static final EntityType<MotorcycleEntity> MOTORCYCLE = register(
            "motorcycle",
            EntityType.Builder.create(MotorcycleEntity::new, SpawnGroup.MISC)
                    .dimensions(0.9F, 1.5F)
                    .eyeHeight(1.2F)
                    .maxTrackingRange(10));

    private ModEntities() {
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> register(
            String name, EntityType.Builder<T> builder) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, CarMain.id(name));
        return Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
    }

    /** Forces class loading so the static fields above are registered. */
    public static void register() {
    }
}
