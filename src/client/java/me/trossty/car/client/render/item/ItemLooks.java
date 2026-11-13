package me.trossty.car.client.render.item;

import me.trossty.car.CarMain;
import me.trossty.car.client.render.LiquidColours;
import me.trossty.car.item.CarItem;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.List;

/**
 * How the mod's items look in hand: which model a stack picks (the
 * overrides in models/item read the properties registered here) and what
 * colour its tinted faces are.
 */
public final class ItemLooks {

    /**
     * Vehicles in the order the item models number them ({@code car:vehicle}
     * 1 is the first). tools/port_item_models.py printed this list; a
     * vehicle added later goes on the end, here and in the model's overrides.
     */
    public static final List<ResourceLocation> VEHICLES = List.of(
            CarMain.id("toyota_corolla"), CarMain.id("tofas_sahin"), CarMain.id("jeep_wrangler"),
            CarMain.id("ford_mustang"), CarMain.id("mercedes_atego"),
            CarMain.id("vespa_primavera"), CarMain.id("honda_cbr600rr"), CarMain.id("toyota_hilux"),
            CarMain.id("box_trailer"));

    /** Bare steel: an empty drum's paint. */
    public static final int EMPTY_DRUM = 0x9AA1A8;

    /** The grey a spray can's cap is when it was never filled. */
    private static final int EMPTY_SPRAY = 0x939393;

    /** The three tones a vehicle is drawn in, by tint index. */
    private static final String[] SHADES = {"paint", "paint_dark", "paint_light"};

    private ItemLooks() {
    }

    /** Model properties. Runs during client setup. */
    public static void registerProperties() {
        ResourceLocation vehicle = CarMain.id("vehicle");
        for (Item item : List.of(ModItems.CAR, ModItems.FUEL_TANKER, ModItems.PICKUP,
                ModItems.TRAILER, ModItems.MOTORCYCLE)) {
            ItemProperties.register(item, vehicle, (stack, level, entity, seed) -> {
                ResourceLocation id = stack.get(ModComponents.CAR_VEHICLE);
                return id == null ? 0 : VEHICLES.indexOf(id) + 1;
            });
        }
        ResourceLocation variant = CarMain.id("part_variant");
        for (Item item : List.of(ModItems.AIR_FILTER, ModItems.ENGINE, ModItems.WHEEL)) {
            ItemProperties.register(item, variant,
                    (stack, level, entity, seed) -> stack.getOrDefault(ModComponents.PART_VARIANT, 0));
        }
        ItemProperties.register(ModItems.JERRY_CAN, CarMain.id("fuel_kind"),
                (stack, level, entity, seed) -> stack.getOrDefault(ModComponents.FUEL_KIND, 0));
    }

    public static void registerColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint < SHADES.length ? paint(stack, SHADES[tint]) : -1,
                ModItems.CAR, ModItems.FUEL_TANKER, ModItems.PICKUP, ModItems.TRAILER, ModItems.MOTORCYCLE);
        event.register((stack, tint) -> {
            if (tint != 0) {
                return -1;
            }
            ModComponents.StoredFluid held = stack.get(ModComponents.STORED_FLUID);
            return 0xFF000000 | LiquidColours.of(held == null ? null : held.fluid(), EMPTY_DRUM);
        }, ModBlocks.FUEL_DRUM);
        event.register((stack, tint) -> tint == 1
                ? 0xFF000000 | DyedItemColor.getOrDefault(stack, 0xFF000000 | EMPTY_SPRAY)
                : -1, ModItems.SPRAY_CAN);
    }

    /**
     * The paint on a vehicle item: the colour it was sprayed, else the dye it
     * wore, else its factory colour, in the given tone.
     */
    private static int paint(ItemStack stack, String shade) {
        Integer sprayed = stack.get(ModComponents.CAR_PAINT);
        Integer index = stack.get(ModComponents.CAR_COLOR);
        int paint = sprayed != null ? sprayed
                : index != null ? DyeColor.byId(index).getTextureDiffuseColor()
                : factoryPaint(stack);
        return 0xFF000000 | VehicleDefinition.resolve(shade, paint & 0xFFFFFF);
    }

    private static int factoryPaint(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(stack.getItem() instanceof CarItem item)) {
            return DyeColor.WHITE.getTextureDiffuseColor();
        }
        return level.registryAccess().registry(VehicleDefinition.KEY)
                .map(registry -> registry.get(item.modelOf(stack)))
                .flatMap(VehicleDefinition::color)
                .orElse(DyeColor.WHITE)
                .getTextureDiffuseColor();
    }
}
