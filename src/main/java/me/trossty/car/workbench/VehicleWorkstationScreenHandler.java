package me.trossty.car.workbench;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.CarParts;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.item.CarItem;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import me.trossty.car.registry.ModScreenHandlers;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The vehicle workstation: pick any vehicle the loaded data packs describe,
 * pick a paint, and build it out of what is in your pockets.
 *
 * <p>Every vehicle of a chassis takes the same running gear — a body, an
 * engine, a gearbox, springs and wheels from the parts bench — plus whatever
 * its own design adds ({@link VehicleDefinition#materials()}). The engine and
 * the wheels may be left out: the vehicle then comes off the stand without
 * them, to be finished at the lift, which is how a project car starts.
 *
 * <p>The window holds nothing of its own, so there is nothing to lose when it
 * closes.
 */
public class VehicleWorkstationScreenHandler extends AbstractContainerMenu {

    /** Ids from 0 pick that vehicle; these are the rest. */
    public static final int FACTORY_PAINT_ID = 900;
    /** This plus a dye index picks that dye. */
    public static final int DYE_ID = 901;
    public static final int BUILD_ID = 1000;

    public static final int PLAYER_INVENTORY_LEFT = 48;
    public static final int PLAYER_INVENTORY_TOP = 149;
    public static final int HOTBAR_TOP = 207;

    private final ContainerLevelAccess context;
    private final Player player;
    private final List<ResourceLocation> vehicles;
    private final DataSlot selected = DataSlot.standalone();
    /** -1 for the factory paint, else a dye index. */
    private final DataSlot paint = DataSlot.standalone();

    public VehicleWorkstationScreenHandler(int syncId, Inventory inventory) {
        this(syncId, inventory, ContainerLevelAccess.NULL);
    }

    public VehicleWorkstationScreenHandler(int syncId, Inventory inventory, ContainerLevelAccess context) {
        super(ModScreenHandlers.VEHICLE_WORKSTATION, syncId);
        this.context = context;
        this.player = inventory.player;
        this.vehicles = vehiclesIn(this.player.level().registryAccess());

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9,
                        PLAYER_INVENTORY_LEFT + column * 18, PLAYER_INVENTORY_TOP + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, PLAYER_INVENTORY_LEFT + column * 18, HOTBAR_TOP));
        }
        this.addDataSlot(this.selected);
        this.addDataSlot(this.paint);
        this.paint.set(-1);
    }

    /** Every vehicle there is, in a fixed order both sides agree on. */
    public static List<ResourceLocation> vehiclesIn(RegistryAccess registries) {
        return registries.registry(VehicleDefinition.KEY)
                .map(registry -> registry.keySet().stream()
                        .sorted(java.util.Comparator.comparing(ResourceLocation::toString))
                        .toList())
                .orElse(List.of());
    }

    public List<ResourceLocation> getVehicles() {
        return this.vehicles;
    }

    public int getSelectedIndex() {
        return this.vehicles.isEmpty() ? -1 : Math.floorMod(this.selected.get(), this.vehicles.size());
    }

    public ResourceLocation getSelectedId() {
        int index = this.getSelectedIndex();
        return index < 0 ? null : this.vehicles.get(index);
    }

    public VehicleDefinition getSelected() {
        ResourceLocation id = this.getSelectedId();
        if (id == null) {
            return null;
        }
        return this.player.level().registryAccess()
                .registry(VehicleDefinition.KEY)
                .map(registry -> registry.get(id))
                .orElse(null);
    }

    /** -1 for the factory paint, else a dye index. */
    public int getPaint() {
        return Math.max(-1, Math.min(15, this.paint.get()));
    }

    /** What the chosen vehicle in the chosen paint takes, each item once. */
    public List<VehicleRecipe.Material> getMaterials() {
        VehicleDefinition definition = this.getSelected();
        if (definition == null) {
            return List.of();
        }
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (VehicleRecipe.Material material : ModVehicleRecipes.chassisParts(definition.chassis())) {
            counts.merge(material.item(), material.count(), Integer::sum);
        }
        for (VehicleDefinition.Cost cost : definition.materials()) {
            counts.merge(cost.item(), cost.count(), Integer::sum);
        }
        if (this.getPaint() >= 0) {
            counts.merge(DyeItem.byColor(DyeColor.byId(this.getPaint())), 1, Integer::sum);
        }
        List<VehicleRecipe.Material> materials = new ArrayList<>();
        counts.forEach((item, count) -> materials.add(new VehicleRecipe.Material(item, count)));
        return materials;
    }

    /** The engine and the wheels can wait: the lift fits them later. */
    public static boolean isOptional(Item item) {
        return item == ModItems.ENGINE || item == ModItems.WHEEL;
    }

    public int have(Item item) {
        return BenchWork.count(this.player.getInventory(), item);
    }

    public boolean canBuild() {
        if (this.getSelected() == null) {
            return false;
        }
        for (VehicleRecipe.Material material : this.getMaterials()) {
            if (!isOptional(material.item()) && this.have(material.item()) < material.count()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < this.vehicles.size()) {
            this.selected.set(id);
            return true;
        }
        if (id == FACTORY_PAINT_ID) {
            this.paint.set(-1);
            return true;
        }
        if (id >= DYE_ID && id < DYE_ID + 16) {
            this.paint.set(id - DYE_ID);
            return true;
        }
        if (id == BUILD_ID) {
            this.context.execute((world, pos) -> {
                ItemStack built = this.build();
                if (built.isEmpty()) {
                    return;
                }
                // It leaves the shop crated: a wrench gets it out where it is wanted.
                ItemStack crate = me.trossty.car.block.entity.VehicleCrateBlockEntity.crateOf(built);
                if (!player.getInventory().add(crate)) {
                    player.drop(crate, false);
                }
                world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
                world.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            });
            return true;
        }
        return false;
    }

    /**
     * Takes the materials and makes the vehicle, or makes nothing and takes
     * nothing. The vehicle is what went into it: the engine's kind and every
     * part's tier carry over, as they do at the lift.
     */
    public ItemStack build() {
        if (!this.canBuild()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = this.getSelectedId();
        VehicleDefinition definition = this.getSelected();
        Inventory pockets = this.player.getInventory();
        ItemStack result = CarItem.stackOf(id, definition);

        int setup = definition.factory().setup();
        long tiers = CarEntity.defaultQualityBits();
        List<Integer> conditions = new ArrayList<>();
        for (CarPart ignored : CarPart.values()) {
            conditions.add(100);
        }
        boolean missing = false;
        for (VehicleRecipe.Material material : this.getMaterials()) {
            CarPart part = CarPart.forItem(material.item());
            if (isOptional(material.item()) && this.have(material.item()) < material.count()) {
                if (part != null) {
                    conditions.set(part.ordinal(), 0);
                    missing = true;
                }
                continue;
            }
            if (part != null) {
                setup = CarSetup.withVariant(setup, part, BenchWork.firstVariant(pockets, material.item()));
            }
            CarPart.Quality taken = BenchWork.consume(pockets, material.item(), material.count());
            if (part != null && taken != null) {
                tiers = CarParts.withQuality(tiers, part, taken);
            }
        }
        result.set(ModComponents.CAR_SETUP, setup);
        if (tiers != CarEntity.defaultQualityBits()) {
            result.set(ModComponents.CAR_PART_QUALITY, tiers);
        }
        if (missing) {
            result.set(ModComponents.CAR_PARTS, List.copyOf(conditions));
        }
        if (this.getPaint() >= 0) {
            result.set(ModComponents.CAR_COLOR, this.getPaint());
        }
        return result;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // Pockets to hotbar and back: there is nowhere else for it to go.
        boolean moved = index < 27
                ? this.moveItemStackTo(stack, 27, 36, false)
                : this.moveItemStackTo(stack, 0, 27, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.context, player, ModBlocks.VEHICLE_WORKSTATION);
    }
}
