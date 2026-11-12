package me.trossty.car.item;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.TankerEntity;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModEntities;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.function.Consumer;

/** Places a {@link CarEntity} on the clicked block. */
public class CarItem extends Item {

    private final EntityType<? extends CarEntity> type;

    /** The entity this item puts down. */
    public EntityType<? extends CarEntity> entityType() {
        return this.type;
    }

    public CarItem(Settings settings) {
        this(settings, ModEntities.CAR);
    }

    /** The tanker is the same item, pointed at a different entity. */
    public CarItem(Settings settings, EntityType<? extends CarEntity> type) {
        super(settings);
        this.type = type;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos().offset(context.getSide());
        CarEntity car = this.type.create(serverWorld, SpawnReason.SPAWN_ITEM_USE);
        if (car == null) {
            return ActionResult.PASS;
        }

        Direction facing = context.getHorizontalPlayerFacing();
        car.refreshPositionAndAngles(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                facing.getPositiveHorizontalDegrees(),
                0.0F);

        if (!serverWorld.isSpaceEmpty(car, car.getBoundingBox())) {
            return ActionResult.PASS;
        }

        // Paint travels on the item, so a car you picked up comes back the
        // colour you painted it rather than reverting to the default.
        // Which vehicle it is comes first: its size decides whether it fits,
        // and its factory fittings are what the rest then overrides.
        net.minecraft.util.Identifier model = context.getStack().get(ModComponents.CAR_VEHICLE);
        if (model != null) {
            car.setModel(model);
            me.trossty.car.vehicle.VehicleDefinition defined = car.getDefinition();
            if (defined != null) {
                car.setSetup(defined.factory().setup());
                defined.color().ifPresent(car::setColor);
                car.calculateDimensions();
                car.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        facing.getPositiveHorizontalDegrees(), 0.0F);
                if (!serverWorld.isSpaceEmpty(car, car.getBoundingBox())) {
                    return ActionResult.PASS;
                }
            }
        }

        // The body's own colour, unless the item says it was repainted.
        if (model == null && car.getDefinition() != null) {
            car.getDefinition().color().ifPresent(car::setColor);
        }
        Integer colorIndex = context.getStack().get(ModComponents.CAR_COLOR);
        if (colorIndex != null) {
            car.setColor(DyeColor.byIndex(colorIndex));
        }
        Integer sprayed = context.getStack().get(ModComponents.CAR_PAINT);
        if (sprayed != null) {
            car.setPaint(sprayed);
        }
        Long lock = context.getStack().get(ModComponents.VEHICLE_KEY);
        if (lock != null) {
            car.setLockCode(lock);
        }

        // Dents travel on the item too, so a wreck you pocketed does not come
        // back repaired just because you put it down again.
        Float health = context.getStack().get(ModComponents.CAR_HEALTH);
        if (health != null) {
            car.setHealth(health);
        }

        // Tired parts come back tired, for the same reason dents do.
        java.util.List<Integer> parts = context.getStack().get(ModComponents.CAR_PARTS);
        if (parts != null) {
            car.setPartConditions(parts);
        }

        // So do the grades, so the good gearbox you paid for is still the one
        // in the car when you put it back down.
        Integer qualities = context.getStack().get(ModComponents.CAR_PART_QUALITY);
        if (qualities != null) {
            car.setQualityBits(qualities);
        }

        Integer setup = context.getStack().get(ModComponents.CAR_SETUP);
        if (setup != null) {
            car.setSetup(setup);
        }

        // And a tanker comes back with whatever was still sloshing about in it.
        if (car instanceof TankerEntity tanker) {
            tanker.loadFrom(context.getStack());
        }

        serverWorld.spawnEntity(car);

        ItemStack stack = context.getStack();
        if (context.getPlayer() == null || !context.getPlayer().isInCreativeMode()) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * The item icon is always a sound, red car, so without these lines a painted
     * or battered one would look identical to a factory-fresh one in the
     * inventory.
     */
    /** A data-pack vehicle is called what its pack calls it, not "Car". */
    @Override
    public Text getName(ItemStack stack) {
        return me.trossty.car.vehicle.VehicleDefinition.nameOf(this.modelOf(stack));
    }

    /** Which vehicle this stack is: the one it names, or what its machine is by default. */
    public net.minecraft.util.Identifier modelOf(ItemStack stack) {
        net.minecraft.util.Identifier model = stack.get(ModComponents.CAR_VEHICLE);
        if (model != null) {
            return model;
        }
        for (me.trossty.car.vehicle.VehicleDefinition.Chassis chassis
                : me.trossty.car.vehicle.VehicleDefinition.Chassis.values()) {
            if (itemFor(chassis) == this) {
                return me.trossty.car.vehicle.VehicleDefinition.defaultFor(chassis);
            }
        }
        return me.trossty.car.vehicle.VehicleDefinition.defaultFor(
                me.trossty.car.vehicle.VehicleDefinition.Chassis.CAR);
    }

    /** The item a vehicle of this chassis is carried as. */
    public static Item itemFor(me.trossty.car.vehicle.VehicleDefinition.Chassis chassis) {
        return switch (chassis) {
            case PICKUP -> me.trossty.car.registry.ModItems.PICKUP;
            case TANKER -> me.trossty.car.registry.ModItems.FUEL_TANKER;
            case TRAILER -> me.trossty.car.registry.ModItems.TRAILER;
            case MOTORCYCLE -> me.trossty.car.registry.ModItems.MOTORCYCLE;
            case CAR -> me.trossty.car.registry.ModItems.CAR;
        };
    }

    /** A new one of these, as it would sit in a showroom. */
    public static ItemStack stackOf(net.minecraft.util.Identifier id,
                                    me.trossty.car.vehicle.VehicleDefinition defined) {
        ItemStack stack = new ItemStack(itemFor(defined.chassis()));
        if (!id.equals(me.trossty.car.vehicle.VehicleDefinition.defaultFor(defined.chassis()))) {
            stack.set(ModComponents.CAR_VEHICLE, id);
        }
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, display, tooltip, type);
        net.minecraft.util.Identifier model = this.modelOf(stack);
        if (context.getRegistryLookup() != null) {
            context.getRegistryLookup()
                    .getOptional(me.trossty.car.vehicle.VehicleDefinition.KEY)
                    .flatMap(registry -> registry.getOptional(net.minecraft.registry.RegistryKey.of(
                            me.trossty.car.vehicle.VehicleDefinition.KEY, model)))
                    .ifPresent(entry -> {
                        me.trossty.car.vehicle.VehicleDefinition defined = entry.value();
                        tooltip.accept(Text.translatable("tooltip.car.vehicle_spec",
                                Math.round(defined.performance().speed() * 100.0F),
                                Math.round(defined.performance().acceleration() * 100.0F),
                                Math.round(defined.performance().fuelUse() * 100.0F),
                                Math.max(1, defined.seats().size())).formatted(Formatting.GRAY));
                        tooltip.accept(Text.translatable("tooltip.car.vehicle_factory",
                                Text.translatable("engine.car." + me.trossty.car.entity.CarSetup.engineKey(
                                        me.trossty.car.entity.CarSetup.variant(defined.factory().setup(),
                                                me.trossty.car.entity.CarPart.ENGINE))),
                                Text.translatable("fuel.car." + me.trossty.car.entity.CarSetup.engineFuel(
                                        defined.factory().setup()).key())).formatted(Formatting.DARK_GRAY));
                        if (defined.tank() > 0.0F) {
                            tooltip.accept(Text.translatable("tooltip.car.vehicle_tank",
                                    JerryCans.litres(defined.tank() / JerryCans.LITRES_PER_UNIT),
                                    me.trossty.car.entity.CarEntity.formatDistance(
                                            me.trossty.car.entity.CarEntity.estimateRange(defined)))
                                    .formatted(Formatting.DARK_GRAY));
                        }
                    });
        }
        if (stack.contains(ModComponents.VEHICLE_KEY)) {
            tooltip.accept(Text.translatable("tooltip.car.has_key").formatted(Formatting.GRAY));
        }
        Integer sprayed = stack.get(ModComponents.CAR_PAINT);
        Integer colorIndex = stack.get(ModComponents.CAR_COLOR);
        if (sprayed != null) {
            tooltip.accept(Text.translatable("tooltip.car.sprayed",
                            Text.literal(String.format("#%06X", sprayed)).withColor(sprayed))
                    .formatted(Formatting.GRAY));
        } else if (colorIndex != null) {
            DyeColor color = DyeColor.byIndex(colorIndex);
            tooltip.accept(Text.translatable("tooltip.car.painted",
                            Text.translatable("color.minecraft." + color.getId()))
                    .formatted(Formatting.GRAY));
        }
        Integer cargo = stack.get(ModComponents.TANKER_CARGO);
        if (cargo != null) {
            var liquid = stack.get(ModComponents.TANKER_LIQUID);
            Text name = liquid == null || liquid.isBlank()
                    ? Text.translatable("fluid.car.petrol")
                    : net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getName(liquid);
            tooltip.accept(Text.translatable("tooltip.car.cargo", cargo, name)
                    .formatted(Formatting.GRAY));
        }
        Float health = stack.get(ModComponents.CAR_HEALTH);
        if (health != null) {
            int percent = Math.round(health / CarEntity.MAX_HEALTH * 100.0F);
            tooltip.accept(Text.translatable("tooltip.car.condition", percent)
                    .formatted(percent < 35 ? Formatting.RED : Formatting.GRAY));
        }
    }
}
