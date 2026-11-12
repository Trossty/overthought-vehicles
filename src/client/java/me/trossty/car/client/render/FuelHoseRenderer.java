package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * The two things you see while filling up: the pump's hose, hanging from the
 * pump to whoever has its nozzle, and a ring on the filler cap of the car in
 * front of you while there is a can or a nozzle in your hand, so you know
 * where to point it.
 */
public final class FuelHoseRenderer {

    private static final int HOSE_COLOUR = 0xFF202020;
    private static final int SEGMENTS = 24;
    private static final int CAP_IDLE = 0xC0E8E8E8;
    private static final int CAP_AIMED = 0xFFFFB020;
    private static final double CAP_SIZE = 0.16;

    private FuelHoseRenderer() {
    }

    public static void render(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer self = client.player;
        MultiBufferSource consumers = context.consumers();
        if (self == null || client.level == null || consumers == null) {
            return;
        }
        float delta = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 camera = context.worldState().cameraRenderState.pos;
        PoseStack matrices = context.matrices();
        VertexConsumer lines = consumers.getBuffer(RenderTypes.lines());

        for (AbstractClientPlayer player : client.level.players()) {
            if (player.distanceToSqr(camera) > 64 * 64) {
                continue;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                BlockPos pump = held.is(ModItems.FUEL_NOZZLE) ? held.get(ModComponents.PUMP) : null;
                Vec3 outlet = pump == null ? null : FuelPumpBlock.hoseOutlet(client.level, pump);
                if (outlet != null) {
                    drawHose(matrices, lines, camera, outlet, handOf(client, player, hand, delta), pump.getY());
                }
            }
        }

        if (!client.options.hideGui && !self.isPassenger()
                && (holdsFiller(self.getMainHandItem()) || holdsFiller(self.getOffhandItem()))) {
            CarEntity aimed = CarEntity.fillerInSight(self);
            for (CarEntity car : client.level.getEntitiesOfClass(
                    CarEntity.class, self.getBoundingBox().inflate(6.0), CarEntity::isAlive)) {
                Vec3 cap = car.fillerPos();
                if (cap == null) {
                    continue;
                }
                boolean lit = car == aimed;
                double size = lit ? CAP_SIZE * 1.25 : CAP_SIZE;
                AABB box = AABB.ofSize(cap, size, size, size);
                ShapeRenderer.renderShape(matrices, lines,
                        Shapes.create(box.move(-box.minX, -box.minY, -box.minZ)),
                        box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z,
                        lit ? CAP_AIMED : CAP_IDLE, lit ? 3.0F : 1.5F);
            }
        }
    }

    private static boolean holdsFiller(ItemStack stack) {
        return stack.is(ModItems.JERRY_CAN) || stack.is(ModItems.FUEL_NOZZLE);
    }

    /**
     * Where the nozzle is: in front of the camera, low and to the side, in
     * first person; at the hand of the body everyone else sees.
     */
    private static Vec3 handOf(Minecraft client, AbstractClientPlayer player, InteractionHand hand, float delta) {
        boolean right = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.RIGHT);
        double side = right ? 1.0 : -1.0;
        if (player == client.player && client.options.getCameraType().isFirstPerson()) {
            Vec3 forward = player.getViewVector(delta);
            Vec3 across = forward.cross(new Vec3(0.0, 1.0, 0.0));
            across = across.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : across.normalize();
            Vec3 up = across.cross(forward).normalize();
            return player.getEyePosition(delta)
                    .add(forward.scale(0.5))
                    .add(across.scale(0.36 * side))
                    .add(up.scale(-0.34));
        }
        float bodyYaw = Mth.lerp(delta, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double height = player.isCrouching() ? 0.55 : 0.72;
        // Our frame: +x is the left, +z the way the body faces.
        Vec3 local = new Vec3(-0.36 * side, height, 0.28);
        return player.getPosition(delta).add(local.yRot(-bodyYaw));
    }

    /** A hose hangs: the middle sags with its length, but never through the forecourt. */
    private static void drawHose(PoseStack matrices, VertexConsumer lines, Vec3 camera,
                                 Vec3 from, Vec3 to, int groundY) {
        double span = Math.sqrt(Mth.square(to.x - from.x) + Mth.square(to.z - from.z));
        double sag = 0.15 + span * 0.18;
        double floor = groundY + 0.04;
        Vec3 last = null;
        PoseStack.Pose entry = matrices.last();
        for (int i = 0; i <= SEGMENTS; i++) {
            double t = i / (double) SEGMENTS;
            Vec3 at = from.lerp(to, t).add(0.0, -sag * 4.0 * t * (1.0 - t), 0.0);
            at = new Vec3(at.x, Math.max(at.y, floor), at.z);
            if (last != null) {
                Vec3 along = at.subtract(last);
                if (along.lengthSqr() > 1.0E-8) {
                    along = along.normalize();
                    double distance = Math.max(0.5, last.lerp(at, 0.5).distanceTo(camera));
                    float width = (float) Mth.clamp(9.0 / distance, 1.5, 12.0);
                    vertex(lines, entry, last, camera, along, width);
                    vertex(lines, entry, at, camera, along, width);
                }
            }
            last = at;
        }
    }

    private static void vertex(VertexConsumer lines, PoseStack.Pose entry, Vec3 at, Vec3 camera,
                               Vec3 along, float width) {
        lines.addVertex(entry, (float) (at.x - camera.x), (float) (at.y - camera.y), (float) (at.z - camera.z))
                .setColor(HOSE_COLOUR)
                .setNormal(entry, (float) along.x, (float) along.y, (float) along.z)
                .setLineWidth(width);
    }
}
