package me.trossty.car.item;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

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

    public ObdScannerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(user.getVehicle() instanceof CarEntity car)) {
            return ActionResult.PASS;
        }
        pair(user.getStackInHand(hand), car, user);
        return ActionResult.SUCCESS;
    }

    /** Pairs the tablet with this car: a beep, and the car's name across the screen bottom. */
    public static void pair(ItemStack tablet, CarEntity car, PlayerEntity player) {
        if (car.getEntityWorld().isClient()) {
            return;
        }
        tablet.set(ModComponents.OBD_LINK, car.getUuid());
        player.sendMessage(Text.translatable("obd.car.paired", car.getVehicleName()).formatted(Formatting.AQUA), true);
        car.getEntityWorld().playSound(null, car.getX(), car.getY() + 0.8, car.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 0.5F, 2.0F);
    }

    /**
     * The car this tablet reads for this player: the one they sit in, else the
     * one it is paired with if that is near enough; null when there is none.
     */
    public static @Nullable CarEntity reading(ItemStack tablet, PlayerEntity player) {
        if (player.getVehicle() instanceof CarEntity car) {
            return car;
        }
        UUID link = tablet.get(ModComponents.OBD_LINK);
        if (link == null) {
            return null;
        }
        for (Entity entity : player.getEntityWorld().getOtherEntities(player,
                player.getBoundingBox().expand(RANGE), e -> e instanceof CarEntity)) {
            if (link.equals(entity.getUuid())) {
                return (CarEntity) entity;
            }
        }
        return null;
    }
}
