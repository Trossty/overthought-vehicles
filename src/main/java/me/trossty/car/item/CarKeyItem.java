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
 * will not start unless the driver has it on them. Clicked at nothing, it
 * finds its car, like the button on a fob: the car within earshot blips its
 * horn and flashes, and the holder is told how far off it is and which way.
 */
public class CarKeyItem extends Item {

    public CarKeyItem(Properties settings) {
        super(settings);
    }

    /** How far a key's button reaches, in blocks. */
    public static final double FOB_RANGE = 96.0;

    private static final String[] HEADINGS = {"south", "southwest", "west", "northwest",
            "north", "northeast", "east", "southeast"};

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level world,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Long code = stack.get(ModComponents.VEHICLE_KEY);
        if (code == null) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        if (!world.isClientSide()) {
            me.trossty.car.entity.CarEntity car = find(world, player, code);
            if (car == null) {
                player.displayClientMessage(Component.translatable("message.overthought_vehicles.key_no_answer"), true);
            } else {
                car.honk();
                world.broadcastEntityEvent(car, me.trossty.car.entity.CarEntity.STATUS_UNLOCKED);
                double dx = car.getX() - player.getX();
                double dz = car.getZ() - player.getZ();
                // Minecraft's yaw: 0 is south, a quarter turn is west.
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                String heading = HEADINGS[Math.floorMod(Math.round(yaw / 45.0F), 8)];
                player.displayClientMessage(Component.translatable("message.overthought_vehicles.key_found", car.getDisplayName(),
                        (int) Math.round(Math.sqrt(dx * dx + dz * dz)),
                        Component.translatable("direction.overthought_vehicles." + heading)), true);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
    }

    /** The nearest vehicle this key opens within the button's reach, or null. */
    @org.jetbrains.annotations.Nullable
    public static me.trossty.car.entity.CarEntity find(
            net.minecraft.world.level.Level world, net.minecraft.world.entity.player.Player player, long code) {
        return world.getEntitiesOfClass(me.trossty.car.entity.CarEntity.class,
                        player.getBoundingBox().inflate(FOB_RANGE),
                        car -> car.getLockCode() == code && !car.isRemoved()
                                && car.distanceToSqr(player) <= FOB_RANGE * FOB_RANGE)
                .stream().min(java.util.Comparator.comparingDouble(car -> car.distanceToSqr(player)))
                .orElse(null);
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
            tooltip.accept(Component.translatable("tooltip.overthought_vehicles.key_blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        ResourceLocation vehicle = stack.get(ModComponents.CAR_VEHICLE);
        Component name = vehicle == null
                ? Component.translatable("item.overthought_vehicles.car")
                : me.trossty.car.vehicle.VehicleDefinition.nameOf(vehicle);
        tooltip.accept(Component.translatable("tooltip.overthought_vehicles.key_for", name).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.overthought_vehicles.key_use").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.overthought_vehicles.key_find").withStyle(ChatFormatting.DARK_GRAY));
    }
}
