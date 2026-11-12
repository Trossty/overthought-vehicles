package me.trossty.car.workbench;

import me.trossty.car.entity.CarPart;
import me.trossty.car.registry.ModComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * One thing the vehicle workbench can build.
 *
 * <p>These are plain code, not datapack recipes, because the workbench picks
 * from a fixed list the player clicks through rather than matching a grid.
 */
public record VehicleRecipe(Item result, int resultDamage, List<Material> materials,
                           boolean refurbished, int variant) {

    public VehicleRecipe(Item result, int resultDamage, List<Material> materials,
                         boolean refurbished) {
        this(result, resultDamage, materials, refurbished, 0);
    }

    /** One kind of a part that comes in kinds: this engine, this tyre. */
    public static VehicleRecipe ofKind(Item result, int variant, List<Material> materials) {
        return new VehicleRecipe(result, 0, materials, false, variant);
    }

    public VehicleRecipe(Item result, List<Material> materials) {
        this(result, 0, materials, false);
    }

    public VehicleRecipe(Item result, int resultDamage, List<Material> materials) {
        this(result, resultDamage, materials, false);
    }

    /**
     * A rebuild: the tired part itself plus half of what a new one costs, and
     * what comes back is a part at {@link CarPart#REFURBISHED}.
     *
     * <p>The extras are taken from the bench's own recipe rather than written
     * out again, so a rebuild can never drift away from what it is a rebuild
     * of.
     */
    public static VehicleRecipe refurbish(CarPart part, VehicleRecipe fresh) {
        List<Material> materials = new java.util.ArrayList<>();
        materials.add(new Material(part.item(), 1));
        for (Material material : fresh.materials()) {
            materials.add(new Material(material.item(), Math.max(1, material.count() / 2)));
        }
        return new VehicleRecipe(part.item(), 0, List.copyOf(materials), true);
    }

    /**
     * A fresh result. The damage matters for the jerry can: a bench builds the
     * can, not what goes in it, so it comes out empty and the refinery is the
     * only thing that fills it.
     */
    public ItemStack createResult() {
        ItemStack stack = new ItemStack(this.result);
        if (this.resultDamage > 0) {
            stack.setDamage(this.resultDamage);
        }
        if (this.variant != 0) {
            stack.set(ModComponents.PART_VARIANT, this.variant);
        }
        if (this.refurbished) {
            // On the catalogue entry as well as on the finished part, so the
            // tooltip says what you are going to get before you pay for it.
            stack.set(ModComponents.PART_CONDITION, Math.round(CarPart.REFURBISHED));
        }
        return stack;
    }

    /** A required item and how many of it the build consumes. */
    public record Material(Item item, int count) {

        public ItemStack toDisplayStack() {
            return new ItemStack(this.item, this.count);
        }
    }
}
