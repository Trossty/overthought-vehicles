package me.trossty.car.workbench;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.item.CarItem;
import me.trossty.car.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * What a bench actually does, apart from any window it does it in.
 *
 * <p>There are two ways to build at a bench now: the window, where the
 * materials are laid out in the bench's own row of slots, and the buttons on
 * the bench top, where they come straight out of your pockets. Both are the
 * same job — count what is there, take it, hand over the result — and they
 * differ only in where "there" is. So the job is written once, against
 * whatever holds the materials, and the two ways in cannot drift apart: a
 * recipe that costs four ingots through the window costs four at the bench top.
 */
public final class BenchWork {

    private BenchWork() {
    }

    /** Whether this recipe is built to a grade: a new part, and not a rebuild. */
    public static boolean choosesQuality(VehicleRecipe recipe) {
        return !recipe.refurbished() && CarPart.forItem(recipe.result()) != null;
    }

    /**
     * Everything this recipe takes at this tier: the plain recipe, and on top
     * of it the tier's own material — gold ingots, diamonds, a netherite ingot.
     */
    public static List<VehicleRecipe.Material> materialsFor(VehicleRecipe recipe,
                                                             CarPart.Quality quality) {
        if (!choosesQuality(recipe) || quality == CarPart.Quality.IRON) {
            return recipe.materials();
        }
        int plain = 0;
        for (VehicleRecipe.Material material : recipe.materials()) {
            plain += material.count();
        }
        List<VehicleRecipe.Material> all = new java.util.ArrayList<>(recipe.materials());
        for (ItemStack extra : quality.extra(plain)) {
            all.add(new VehicleRecipe.Material(extra.getItem(), extra.getCount()));
        }
        return List.copyOf(all);
    }

    public static int count(Inventory source, Item item) {
        int total = 0;
        for (int slot = 0; slot < source.size(); slot++) {
            ItemStack stack = source.getStack(slot);
            if (stack.isOf(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Only the tired ones: a rebuild will not take a part that is still good. */
    public static int countWorn(Inventory source, Item item) {
        int total = 0;
        for (int slot = 0; slot < source.size(); slot++) {
            ItemStack stack = source.getStack(slot);
            if (stack.isOf(item) && CarEntity.conditionOf(stack) < CarPart.REFURBISHED) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * How many of a material are on hand that this recipe would accept. For
     * the part a rebuild is rebuilding that means worn ones only.
     */
    public static int have(Inventory source, VehicleRecipe recipe, VehicleRecipe.Material material) {
        return recipe.refurbished() && material.item() == recipe.result()
                ? countWorn(source, material.item())
                : count(source, material.item());
    }

    public static boolean hasMaterials(Inventory source, VehicleRecipe recipe,
                                       CarPart.Quality quality) {
        for (VehicleRecipe.Material material : materialsFor(recipe, quality)) {
            if (have(source, recipe, material) < material.count()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes the materials and returns what they made, or an empty stack — and
     * takes nothing — if they were not all there.
     */
    public static ItemStack build(Inventory source, VehicleRecipe recipe, CarPart.Quality quality) {
        if (!hasMaterials(source, recipe, quality)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = recipe.createResult();

        if (recipe.refurbished()) {
            // A rebuilt part is the part it was: it keeps the grade it was
            // made to, which is why the worn one's grade is read on the way in.
            CarPart.Quality grade = CarPart.Quality.DEFAULT;
            for (VehicleRecipe.Material material : recipe.materials()) {
                if (material.item() == recipe.result()) {
                    // And the kind it was: a rebuilt V8 is still a V8.
                    int kind = wornVariant(source, material.item());
                    if (kind != 0) {
                        result.set(ModComponents.PART_VARIANT, kind);
                    }
                    grade = takeWorn(source, material.item(), material.count());
                } else {
                    consume(source, material.item(), material.count());
                }
            }
            if (grade != CarPart.Quality.DEFAULT) {
                result.set(ModComponents.PART_QUALITY, grade.ordinal());
            }
            return result;
        }

        int assembledFrom = CarEntity.defaultQualityBits();
        int setup = CarSetup.DEFAULT;
        for (VehicleRecipe.Material material : materialsFor(recipe, quality)) {
            CarPart kinded = CarPart.forItem(material.item());
            if (kinded != null) {
                // Read before it is taken: the car is whatever went into it.
                setup = CarSetup.withVariant(setup, kinded, firstVariant(source, material.item()));
            }
            CarPart.Quality taken = consume(source, material.item(), material.count());
            CarPart built = CarPart.forItem(material.item());
            if (built != null && taken != null) {
                assembledFrom = CarEntity.withQuality(assembledFrom, built, taken);
            }
        }
        if (choosesQuality(recipe) && quality != CarPart.Quality.DEFAULT) {
            result.set(ModComponents.PART_QUALITY, quality.ordinal());
        }
        if (recipe.result() instanceof CarItem
                && assembledFrom != CarEntity.defaultQualityBits()) {
            // A car is as good as what it was assembled from.
            result.set(ModComponents.CAR_PART_QUALITY, assembledFrom);
        }
        if (recipe.result() instanceof CarItem && setup != CarSetup.DEFAULT) {
            result.set(ModComponents.CAR_SETUP, setup);
        }
        return result;
    }

    /**
     * Building at the bench top: the materials come out of the player's own
     * pockets and the result goes back into them.
     *
     * @return whether anything was built
     */
    public static boolean buildFromPockets(PlayerEntity player, BlockPos bench,
                                           List<VehicleRecipe> catalogue, int index,
                                           CarPart.Quality quality) {
        if (index < 0 || index >= catalogue.size()) {
            return false;
        }
        VehicleRecipe recipe = catalogue.get(index);
        ItemStack result = build(player.getInventory(), recipe, quality);
        if (result.isEmpty()) {
            player.sendMessage(Text.translatable("message.car.bench_short",
                    recipe.createResult().getName()), true);
            return false;
        }
        if (!player.getInventory().insertStack(result)) {
            player.dropItem(result, false);
        }
        player.getEntityWorld().playSound(null, bench,
                recipe.refurbished() ? SoundEvents.BLOCK_GRINDSTONE_USE : SoundEvents.BLOCK_ANVIL_USE,
                SoundCategory.BLOCKS, recipe.refurbished() ? 0.7F : 0.6F,
                recipe.refurbished() ? 1.0F : 1.6F);
        return true;
    }

    /** The kind of the first of these on hand, which is the first to be taken. */
    public static int firstVariant(Inventory source, Item item) {
        for (int slot = 0; slot < source.size(); slot++) {
            ItemStack stack = source.getStack(slot);
            if (stack.isOf(item)) {
                return CarEntity.variantOf(stack);
            }
        }
        return 0;
    }

    /** The same, for the worn one a rebuild is about to take. */
    private static int wornVariant(Inventory source, Item item) {
        for (int slot = 0; slot < source.size(); slot++) {
            ItemStack stack = source.getStack(slot);
            if (stack.isOf(item) && CarEntity.conditionOf(stack) < CarPart.REFURBISHED) {
                return CarEntity.variantOf(stack);
            }
        }
        return 0;
    }

    private static CarPart.Quality takeWorn(Inventory source, Item item, int count) {
        int remaining = count;
        CarPart.Quality taken = CarPart.Quality.DEFAULT;
        for (int slot = 0; slot < source.size() && remaining > 0; slot++) {
            ItemStack stack = source.getStack(slot);
            if (!stack.isOf(item) || CarEntity.conditionOf(stack) >= CarPart.REFURBISHED) {
                continue;
            }
            taken = CarEntity.qualityOf(stack);
            int moved = Math.min(remaining, stack.getCount());
            stack.decrement(moved);
            remaining -= moved;
        }
        return taken;
    }

    /** Takes the items and reports the worst grade among them, if they are parts. */
    public static CarPart.Quality consume(Inventory source, Item item, int count) {
        int remaining = count;
        CarPart.Quality worst = null;
        for (int slot = 0; slot < source.size() && remaining > 0; slot++) {
            ItemStack stack = source.getStack(slot);
            if (!stack.isOf(item)) {
                continue;
            }
            if (CarPart.forItem(item) != null) {
                CarPart.Quality here = CarEntity.qualityOf(stack);
                if (worst == null || here.rank() < worst.rank()) {
                    worst = here;
                }
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.decrement(taken);
            remaining -= taken;
        }
        return worst;
    }
}
