package me.trossty.car.item;

import net.minecraft.item.ItemStack;

/**
 * What is in a jerry can, in the same units a car's tank is measured in.
 *
 * <p>The can's durability is its gauge, read backwards: damage 0 is full and
 * {@link #CAPACITY} is empty. It used to count four whole pours, and a pour
 * into a tank with less room than that lost the rest — which was nothing for
 * a family car and half the can for a scooter that holds eight litres. Now
 * the can holds tank units, so a pour takes exactly what fits and the rest
 * stays in the can.
 */
public final class JerryCans {

    /** A full can, in tank units: one family car's tank, fifty litres. */
    public static final int CAPACITY = 100;

    /** What one click pours into a car, a quarter of the can. */
    public static final int POUR = 25;

    /** Litres in one tank unit. A family car's hundred units are fifty litres. */
    public static final float LITRES_PER_UNIT = 0.5F;

    private JerryCans() {
    }

    /** Units of fuel in the can. */
    public static int units(ItemStack can) {
        return Math.max(0, can.getMaxDamage() - can.getDamage());
    }

    /** Units it would still take. */
    public static int room(ItemStack can) {
        return Math.max(0, can.getDamage());
    }

    public static boolean isEmpty(ItemStack can) {
        return units(can) <= 0;
    }

    public static boolean isFull(ItemStack can) {
        return room(can) <= 0;
    }

    /** Sets the contents. setDamage, never damage(): an emptied can survives to be refilled. */
    public static void setUnits(ItemStack can, int units) {
        can.setDamage(can.getMaxDamage() - Math.max(0, Math.min(can.getMaxDamage(), units)));
    }

    /** Takes up to {@code amount} out and reports how much came. */
    public static int drain(ItemStack can, int amount) {
        int taken = Math.min(amount, units(can));
        setUnits(can, units(can) - taken);
        return taken;
    }

    /** Puts up to {@code amount} in and reports how much went. */
    public static int fill(ItemStack can, int amount) {
        int put = Math.min(amount, room(can));
        setUnits(can, units(can) + put);
        return put;
    }

    /**
     * Tops the can up out of a store of petrol, as far as both go.
     *
     * @param available petrol the store holds
     * @param drain     takes that much petrol out of the store
     * @return units that went into the can
     */
    public static int topUp(ItemStack can, int available, java.util.function.IntUnaryOperator drain) {
        int perUnit = me.trossty.car.block.entity.FuelPumpBlockEntity.PETROL_PER_FUEL_UNIT;
        int units = Math.min(room(can), available / perUnit);
        if (units <= 0) {
            return 0;
        }
        int given = drain.applyAsInt(units * perUnit) / perUnit;
        return fill(can, given);
    }

    /**
     * Empties the can into a store of petrol, as far as it has room.
     *
     * @param space room the store has, in petrol
     * @param fill  puts that much petrol in and reports what it took
     * @return units that left the can
     */
    public static int emptyInto(ItemStack can, int space, java.util.function.IntUnaryOperator fill) {
        int perUnit = me.trossty.car.block.entity.FuelPumpBlockEntity.PETROL_PER_FUEL_UNIT;
        int units = Math.min(units(can), space / perUnit);
        if (units <= 0) {
            return 0;
        }
        int taken = fill.applyAsInt(units * perUnit) / perUnit;
        return drain(can, taken);
    }

    /** A number of tank units as litres, for people. */
    public static String litres(float units) {
        float litres = units * LITRES_PER_UNIT;
        return litres >= 10.0F || litres == Math.round(litres)
                ? String.valueOf(Math.round(litres))
                : String.format(java.util.Locale.ROOT, "%.1f", litres);
    }
}
