package me.trossty.car.block.entity;

import me.trossty.car.block.VehicleCrateBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.item.CarItem;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What is in a crate, and how far it has been opened. The vehicle is kept as
 * the very stack the workstation made, so everything the item carries — the
 * engine, the grades, the paint — is what drives out of it.
 *
 * <p>Each turn of the wrench lifts the lid a little; the last one takes it
 * off, and the sides fall away over {@link #OPEN_TICKS}. The clients count
 * the fall themselves from the moment they are told it began.
 */
public class VehicleCrateBlockEntity extends BlockEntity {

    /** Turns of the wrench it takes to get the lid off. */
    public static final int PRIES = 4;
    /** How long the sides take to fall, after which the vehicle is out. */
    public static final int OPEN_TICKS = 30;

    private ItemStack vehicle = ItemStack.EMPTY;
    private int pries;
    /** Ticks since the lid came off, or -1 while it is still on. */
    private int opening = -1;

    public VehicleCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VEHICLE_CRATE, pos, state);
    }

    public ItemStack getVehicle() {
        return this.vehicle;
    }

    public void setVehicle(ItemStack stack) {
        this.vehicle = stack.copyWithCount(1);
        this.changed();
    }

    public int getPries() {
        return this.pries;
    }

    /** Ticks since the lid came off, or -1 while it is on. */
    public int getOpening() {
        return this.opening;
    }

    /** A turn of the wrench: the lid lifts, and on the last turn comes off. */
    public void pry(Player player) {
        if (!(this.level instanceof ServerLevel world) || this.opening >= 0) {
            return;
        }
        BlockPos pos = this.worldPosition;
        if (this.vehicle.isEmpty()) {
            // Nothing in it: it just comes apart.
            world.destroyBlock(pos, false);
            return;
        }
        // Better to find out now than after the sides are down.
        if (this.unpack(world) == null) {
            player.displayClientMessage(Component.translatable("message.car.crate_no_room"), true);
            world.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8F, 1.0F);
            return;
        }
        this.pries++;
        world.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS,
                0.35F, 0.9F + world.random.nextFloat() * 0.3F);
        world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.4, 0.05, 0.4, 0.05);
        if (this.pries >= PRIES) {
            this.opening = 0;
            world.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        this.changed();
    }

    public static void tick(Level world, BlockPos pos, BlockState state, VehicleCrateBlockEntity crate) {
        if (crate.opening < 0) {
            return;
        }
        crate.opening++;
        if (crate.opening == OPEN_TICKS / 2 && !world.isClientSide()) {
            world.playSound(null, pos, SoundEvents.WOOD_FALL, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        if (crate.opening >= OPEN_TICKS && world instanceof ServerLevel server) {
            crate.finish(server);
        }
    }

    /** The sides are down: the vehicle takes the crate's place. */
    private void finish(ServerLevel world) {
        BlockPos pos = this.worldPosition;
        CarEntity car = this.unpack(world);
        if (car == null) {
            // Something has moved in since the lid came off. Put it back on.
            this.opening = -1;
            this.pries = 0;
            this.changed();
            for (Player near : world.getEntitiesOfClass(Player.class, new AABB(pos).inflate(8.0))) {
                near.displayClientMessage(Component.translatable("message.car.crate_no_room"), true);
            }
            return;
        }
        this.vehicle = ItemStack.EMPTY;
        world.removeBlock(pos, false);
        world.addFreshEntity(car);
        world.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                30, 0.8, 0.4, 0.8, 0.02);
        world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 40, 0.9, 0.1, 0.9, 0.1);
        world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    /** The vehicle as it would stand where the crate is, or null if it would not fit there. */
    private @Nullable CarEntity unpack(ServerLevel world) {
        BlockPos pos = this.worldPosition;
        float yaw = this.getBlockState().getValue(VehicleCrateBlock.FACING).toYRot();
        return CarItem.vehicleFrom(world, this.vehicle, Vec3.atBottomCenterOf(pos), yaw,
                car -> fitsBeside(world, car, pos));
    }

    /** Whether the vehicle fits with nothing in its way but the crate it is coming out of. */
    private static boolean fitsBeside(ServerLevel world, CarEntity car, BlockPos crate) {
        AABB box = car.getBoundingBox();
        if (!world.getWorldBorder().isWithinBounds(box) || !world.getEntityCollisions(car, box).isEmpty()) {
            return false;
        }
        CollisionContext context = CollisionContext.of(car);
        for (BlockPos at : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX - 1.0E-7), Mth.floor(box.maxY - 1.0E-7), Mth.floor(box.maxZ - 1.0E-7))) {
            if (at.equals(crate)) {
                continue;
            }
            VoxelShape shape = world.getBlockState(at).getCollisionShape(world, at, context);
            for (AABB part : shape.toAabbs()) {
                if (part.move(at).intersects(box)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void changed() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    // ---- saving, and travelling in the item ----

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.vehicle = tag.contains("Vehicle")
                ? ItemStack.parseOptional(registries, tag.getCompound("Vehicle"))
                : ItemStack.EMPTY;
        this.pries = tag.getInt("Pries");
        this.opening = tag.contains("Opening") ? tag.getInt("Opening") : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.vehicle.isEmpty()) {
            tag.put("Vehicle", this.vehicle.save(registries));
        }
        if (this.pries > 0) {
            tag.putInt("Pries", this.pries);
        }
        if (this.opening >= 0) {
            tag.putInt("Opening", this.opening);
        }
    }

    @Override
    public net.minecraft.network.protocol.@Nullable Packet<
            net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        ItemContainerContents held = components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        this.vehicle = held.copyOne();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!this.vehicle.isEmpty()) {
            builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(this.vehicle)));
        }
    }

    /** The vehicle travels in the component; the item's copy of the block data leaves it out. */
    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Vehicle");
        tag.remove("Pries");
        tag.remove("Opening");
    }

    /** A crate holding this vehicle, ready to be carried and set down. */
    public static ItemStack crateOf(ItemStack vehicle) {
        ItemStack crate = new ItemStack(me.trossty.car.registry.ModBlocks.VEHICLE_CRATE);
        crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(vehicle.copyWithCount(1))));
        return crate;
    }

    /** The vehicle a crate stack holds, or empty. */
    public static ItemStack vehicleIn(ItemStack crate) {
        return crate.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }
}
