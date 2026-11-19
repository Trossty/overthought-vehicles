package me.trossty.car.item;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.TankerEntity;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

/** Places a {@link CarEntity} on the clicked block. */
public class CarItem extends Item {

    private final EntityType<? extends CarEntity> type;

    /** The entity this item puts down. */
    public EntityType<? extends CarEntity> entityType() {
        return this.type;
    }

    public CarItem(Properties settings) {
        this(settings, ModEntities.CAR);
    }

    /** The tanker is the same item, pointed at a different entity. */
    public CarItem(Properties settings, EntityType<? extends CarEntity> type) {
        super(settings);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // A boat aimed at open water goes on the water, not on the bed under it.
        if (context.getPlayer() != null && this.waterAhead(context.getLevel(), context.getPlayer()) != null) {
            return InteractionResult.PASS;
        }
        Level world = context.getLevel();
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        return this.placeAt(serverWorld, context.getItemInHand(), context.getPlayer(),
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getHorizontalDirection().toYRot());
    }

    /** A boat is set down on the water it is aimed at, the way a vanilla boat is. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level world, net.minecraft.world.entity.player.Player player,
                                                                    net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        net.minecraft.world.phys.BlockHitResult water = this.waterAhead(world, player);
        if (water == null) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        if (world instanceof ServerLevel serverWorld) {
            BlockPos surface = water.getBlockPos();
            InteractionResult placed = this.placeAt(serverWorld, stack, player,
                    water.getLocation().x, surface.getY() + 1.0 - 0.6, water.getLocation().z,
                    player.getDirection().toYRot());
            return placed.consumesAction() ? net.minecraft.world.InteractionResultHolder.success(stack)
                    : net.minecraft.world.InteractionResultHolder.fail(stack);
        }
        return net.minecraft.world.InteractionResultHolder.success(stack);
    }

    /** The water surface a boat item is aimed at within reach, or null: only boats float. */
    private net.minecraft.world.phys.BlockHitResult waterAhead(Level world, net.minecraft.world.entity.player.Player player) {
        if (!(this.modelChassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.BOAT)) {
            return null;
        }
        net.minecraft.world.phys.BlockHitResult hit = getPlayerPOVHitResult(world, player,
                net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                || !world.getFluidState(hit.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER)) {
            return null;
        }
        return hit;
    }

    /** Which kind of vehicle this item carries. */
    private me.trossty.car.vehicle.VehicleDefinition.Chassis modelChassis() {
        for (me.trossty.car.vehicle.VehicleDefinition.Chassis chassis
                : me.trossty.car.vehicle.VehicleDefinition.Chassis.values()) {
            if (itemFor(chassis) == this) {
                return chassis;
            }
        }
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.CAR;
    }

    /** Puts the vehicle this stack carries down here, if it fits; uses the stack up unless in creative. */
    private InteractionResult placeAt(ServerLevel serverWorld, ItemStack stack,
                                      net.minecraft.world.entity.player.Player player,
                                      double x, double y, double z, float yaw) {
        CarEntity car = vehicleFrom(serverWorld, stack, new net.minecraft.world.phys.Vec3(x, y, z), yaw,
                placed -> serverWorld.noCollision(placed, placed.getBoundingBox()));
        if (car == null) {
            return InteractionResult.PASS;
        }
        serverWorld.addFreshEntity(car);

        if (player == null || !player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The vehicle a vehicle stack holds, stood at this spot facing this way and
     * ready to be added to the world: or null if the stack is not a vehicle or
     * the vehicle does not fit there. Shared by the item and the crate.
     */
    public static @org.jetbrains.annotations.Nullable CarEntity vehicleFrom(
            ServerLevel serverWorld, ItemStack stack, net.minecraft.world.phys.Vec3 at, float yaw,
            java.util.function.Predicate<CarEntity> fits) {
        if (!(stack.getItem() instanceof CarItem item)) {
            return null;
        }
        CarEntity car = item.type.create(serverWorld);
        if (car == null) {
            return null;
        }
        car.moveTo(at.x, at.y, at.z, yaw, 0.0F);

        // Paint travels on the item, so a car you picked up comes back the
        // colour you painted it rather than reverting to the default.
        // Which vehicle it is comes first: its size decides whether it fits,
        // and its factory fittings are what the rest then overrides.
        net.minecraft.resources.ResourceLocation model = stack.get(ModComponents.CAR_VEHICLE);
        if (model != null) {
            car.setModel(model);
            me.trossty.car.vehicle.VehicleDefinition defined = car.getDefinition();
            if (defined != null) {
                car.setSetup(defined.factory().setup());
                defined.color().ifPresent(car::setColor);
                car.refreshDimensions();
                car.moveTo(at.x, at.y, at.z, yaw, 0.0F);
            }
        }
        if (!fits.test(car)) {
            return null;
        }

        // The body's own colour, unless the item says it was repainted.
        if (model == null && car.getDefinition() != null) {
            car.getDefinition().color().ifPresent(car::setColor);
        }
        Integer colorIndex = stack.get(ModComponents.CAR_COLOR);
        if (colorIndex != null) {
            car.setColor(DyeColor.byId(colorIndex));
        }
        Integer sprayed = stack.get(ModComponents.CAR_PAINT);
        if (sprayed != null) {
            car.setPaint(sprayed);
        }
        Long lock = stack.get(ModComponents.VEHICLE_KEY);
        if (lock != null) {
            car.setLockCode(lock);
        }

        // Dents travel on the item too, so a wreck you pocketed does not come
        // back repaired just because you put it down again.
        Float health = stack.get(ModComponents.CAR_HEALTH);
        if (health != null) {
            car.damage().setHealth(health);
        }

        // Tired parts come back tired, for the same reason dents do.
        java.util.List<Integer> parts = stack.get(ModComponents.CAR_PARTS);
        if (parts != null) {
            car.setPartConditions(parts);
        }

        // So do the grades, so the good gearbox you paid for is still the one
        // in the car when you put it back down.
        Long qualities = stack.get(ModComponents.CAR_PART_QUALITY);
        if (qualities != null) {
            car.parts().setQualityBits(qualities);
        }

        Integer setup = stack.get(ModComponents.CAR_SETUP);
        if (setup != null) {
            car.setSetup(setup);
        }

        // And a tanker comes back with whatever was still sloshing about in it.
        if (car instanceof TankerEntity tanker) {
            tanker.loadFrom(stack);
        }

        return car;
    }

    /**
     * The item icon is always a sound, red car, so without these lines a painted
     * or battered one would look identical to a factory-fresh one in the
     * inventory.
     */
    /** A data-pack vehicle is called what its pack calls it, not "Car". */
    @Override
    public Component getName(ItemStack stack) {
        return me.trossty.car.vehicle.VehicleDefinition.nameOf(this.modelOf(stack));
    }

    /** Which vehicle this stack is: the one it names, or what its machine is by default. */
    public net.minecraft.resources.ResourceLocation modelOf(ItemStack stack) {
        net.minecraft.resources.ResourceLocation model = stack.get(ModComponents.CAR_VEHICLE);
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
            case BOAT -> me.trossty.car.registry.ModItems.MOTORBOAT;
            case PLANE -> me.trossty.car.registry.ModItems.AIRPLANE;
            case HELICOPTER -> me.trossty.car.registry.ModItems.HELICOPTER;
            case CAR -> me.trossty.car.registry.ModItems.CAR;
        };
    }

    /** A new one of these, as it would sit in a showroom. */
    public static ItemStack stackOf(net.minecraft.resources.ResourceLocation id,
                                    me.trossty.car.vehicle.VehicleDefinition defined) {
        ItemStack stack = new ItemStack(itemFor(defined.chassis()));
        if (!id.equals(me.trossty.car.vehicle.VehicleDefinition.defaultFor(defined.chassis()))) {
            stack.set(ModComponents.CAR_VEHICLE, id);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                java.util.List<Component> lines, TooltipFlag type) {
        super.appendHoverText(stack, context, lines, type);
        Consumer<Component> tooltip = lines::add;
        net.minecraft.resources.ResourceLocation model = this.modelOf(stack);
        if (context.registries() != null) {
            context.registries()
                    .lookup(me.trossty.car.vehicle.VehicleDefinition.KEY)
                    .flatMap(registry -> registry.get(net.minecraft.resources.ResourceKey.create(
                            me.trossty.car.vehicle.VehicleDefinition.KEY, model)))
                    .ifPresent(entry -> {
                        me.trossty.car.vehicle.VehicleDefinition defined = entry.value();
                        tooltip.accept(Component.translatable("tooltip.car.vehicle_spec",
                                Math.round(defined.performance().speed() * 100.0F),
                                Math.round(defined.performance().acceleration() * 100.0F),
                                Math.round(defined.performance().fuelUse() * 100.0F),
                                Math.max(1, defined.seats().size())).withStyle(ChatFormatting.GRAY));
                        tooltip.accept(Component.translatable("tooltip.car.vehicle_factory",
                                Component.translatable("engine.car." + me.trossty.car.entity.CarSetup.engineKey(
                                        me.trossty.car.entity.CarSetup.variant(defined.factory().setup(),
                                                me.trossty.car.entity.CarPart.ENGINE))),
                                Component.translatable("fuel.car." + me.trossty.car.entity.CarSetup.engineFuel(
                                        defined.factory().setup()).key())).withStyle(ChatFormatting.DARK_GRAY));
                        if (defined.tank() > 0.0F) {
                            tooltip.accept(Component.translatable("tooltip.car.vehicle_tank",
                                    JerryCans.litres(defined.tank() / JerryCans.LITRES_PER_UNIT),
                                    me.trossty.car.entity.CarEntity.formatDistance(
                                            me.trossty.car.entity.CarEntity.estimateRange(defined)))
                                    .withStyle(ChatFormatting.DARK_GRAY));
                        }
                    });
        }
        if (stack.has(ModComponents.VEHICLE_KEY)) {
            tooltip.accept(Component.translatable("tooltip.car.has_key").withStyle(ChatFormatting.GRAY));
        }
        Integer sprayed = stack.get(ModComponents.CAR_PAINT);
        Integer colorIndex = stack.get(ModComponents.CAR_COLOR);
        if (sprayed != null) {
            tooltip.accept(Component.translatable("tooltip.car.sprayed",
                            Component.literal(String.format("#%06X", sprayed)).withColor(sprayed))
                    .withStyle(ChatFormatting.GRAY));
        } else if (colorIndex != null) {
            DyeColor color = DyeColor.byId(colorIndex);
            tooltip.accept(Component.translatable("tooltip.car.painted",
                            Component.translatable("color.minecraft." + color.getName()))
                    .withStyle(ChatFormatting.GRAY));
        }
        Integer cargo = stack.get(ModComponents.TANKER_CARGO);
        if (cargo != null) {
            var liquid = stack.get(ModComponents.TANKER_LIQUID);
            Component name = me.trossty.car.fluid.Liquids.isBlank(liquid)
                    ? Component.translatable("fluid.car.petrol")
                    : me.trossty.car.fluid.Liquids.name(liquid);
            tooltip.accept(Component.translatable("tooltip.car.cargo", cargo, name)
                    .withStyle(ChatFormatting.GRAY));
        }
        Float health = stack.get(ModComponents.CAR_HEALTH);
        if (health != null) {
            int percent = Math.round(health / CarEntity.MAX_HEALTH * 100.0F);
            tooltip.accept(Component.translatable("tooltip.car.condition", percent)
                    .withStyle(percent < 35 ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
    }
}
