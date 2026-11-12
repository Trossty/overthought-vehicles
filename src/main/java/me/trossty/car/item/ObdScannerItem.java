package me.trossty.car.item;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The OBD tablet. Held, it is raised in both hands the way a map is and its
 * screen reads the car out live: fault codes worst first, fuel, body and
 * temperature (drawn client side, see the tablet renderer). Plugged into a
 * car, by using it on one, it stays paired with that car while in range;
 * from the driver's seat it always reads the car underneath.
 */
public class ObdScannerItem extends Item {

    /** How far the tablet still hears the car it is paired with, in blocks. */
    public static final double RANGE = 32.0;

    public ObdScannerItem(Properties settings) {
        super(settings);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (!(user.getVehicle() instanceof CarEntity car)) {
            return net.minecraft.world.InteractionResultHolder.pass(user.getItemInHand(hand));
        }
        pair(user.getItemInHand(hand), car, user);
        return net.minecraft.world.InteractionResultHolder.success(user.getItemInHand(hand));
    }

    /** Pairs the tablet with this car: a beep, and the car's name across the screen bottom. */
    public static void pair(ItemStack tablet, CarEntity car, Player player) {
        if (car.level().isClientSide()) {
            return;
        }
        tablet.set(ModComponents.OBD_LINK, car.getUUID());
        player.displayClientMessage(Component.translatable("obd.car.paired", car.getVehicleName()).withStyle(ChatFormatting.AQUA), true);
        car.level().playSound(null, car.getX(), car.getY() + 0.8, car.getZ(),
                SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.5F, 2.0F);
    }

    /**
     * The car this tablet reads for this player: the one they sit in, else the
     * one it is paired with if that is near enough; null when there is none.
     */
    public static @Nullable CarEntity reading(ItemStack tablet, Player player) {
        if (player.getVehicle() instanceof CarEntity car) {
            return car;
        }
        UUID link = tablet.get(ModComponents.OBD_LINK);
        if (link == null) {
            return null;
        }
        for (Entity entity : player.level().getEntities(player,
                player.getBoundingBox().inflate(RANGE), e -> e instanceof CarEntity)) {
            if (link.equals(entity.getUUID())) {
                return (CarEntity) entity;
            }
        }
        return null;
    }
}
