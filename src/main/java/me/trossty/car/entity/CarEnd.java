package me.trossty.car.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * The bonnet or the boot, as far as aiming goes. A car's own hit box is a
 * square as wide as the car round its middle, so the two ends hang out past
 * it; without these a click on the bumper went to the grass under it. Every
 * click and hit lands on the car itself.
 */
public final class CarEnd extends PartEntity<CarEntity> {

    private final boolean front;

    CarEnd(CarEntity car, boolean front) {
        super(car);
        this.front = front;
        this.noPhysics = true;
    }

    /** Sits over its end of the body, turned with it. */
    void place(double x, double y, double z, double fx, double fz, double reach, double width, double height) {
        double half = width / 2.0;
        double sign = this.front ? 1.0 : -1.0;
        double length = Math.max(0.0, reach - half) / 2.0;
        double mid = (half + length) * sign;
        double cx = x + fx * mid;
        double cz = z + fz * mid;
        double ex = Math.abs(fx) * length + Math.abs(fz) * half;
        double ez = Math.abs(fz) * length + Math.abs(fx) * half;
        this.setPosRaw(cx, y, cz);
        this.xo = this.xOld = cx;
        this.yo = this.yOld = y;
        this.zo = this.zOld = cz;
        this.setBoundingBox(new AABB(cx - ex, y, cz - ez, cx + ex, y + height, cz + ez));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return !this.getParent().isRemoved() && this.getParent().isPickable();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return this.getParent().interact(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.getParent().hurt(source, amount);
    }

    @Override
    public ItemStack getPickResult() {
        return this.getParent().getPickResult();
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
