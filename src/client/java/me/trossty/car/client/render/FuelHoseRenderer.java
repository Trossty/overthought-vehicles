package me.trossty.car.client.render;

import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

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
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity self = client.player;
        VertexConsumerProvider consumers = context.consumers();
        if (self == null || client.world == null || consumers == null) {
            return;
        }
        float delta = client.getRenderTickCounter().getTickProgress(true);
        Vec3d camera = context.worldState().cameraRenderState.pos;
        MatrixStack matrices = context.matrices();
        VertexConsumer lines = consumers.getBuffer(RenderLayers.lines());

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player.squaredDistanceTo(camera) > 64 * 64) {
                continue;
            }
            for (Hand hand : Hand.values()) {
                ItemStack held = player.getStackInHand(hand);
                BlockPos pump = held.isOf(ModItems.FUEL_NOZZLE) ? held.get(ModComponents.PUMP) : null;
                Vec3d outlet = pump == null ? null : FuelPumpBlock.hoseOutlet(client.world, pump);
                if (outlet != null) {
                    drawHose(matrices, lines, camera, outlet, handOf(client, player, hand, delta), pump.getY());
                }
            }
        }

        if (!client.options.hudHidden && !self.hasVehicle()
                && (holdsFiller(self.getMainHandStack()) || holdsFiller(self.getOffHandStack()))) {
            CarEntity aimed = CarEntity.fillerInSight(self);
            for (CarEntity car : client.world.getEntitiesByClass(
                    CarEntity.class, self.getBoundingBox().expand(6.0), CarEntity::isAlive)) {
                Vec3d cap = car.fillerPos();
                if (cap == null) {
                    continue;
                }
                boolean lit = car == aimed;
                double size = lit ? CAP_SIZE * 1.25 : CAP_SIZE;
                Box box = Box.of(cap, size, size, size);
                VertexRendering.drawOutline(matrices, lines,
                        VoxelShapes.cuboid(box.offset(-box.minX, -box.minY, -box.minZ)),
                        box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z,
                        lit ? CAP_AIMED : CAP_IDLE, lit ? 3.0F : 1.5F);
            }
        }
    }

    private static boolean holdsFiller(ItemStack stack) {
        return stack.isOf(ModItems.JERRY_CAN) || stack.isOf(ModItems.FUEL_NOZZLE);
    }

    /**
     * Where the nozzle is: in front of the camera, low and to the side, in
     * first person; at the hand of the body everyone else sees.
     */
    private static Vec3d handOf(MinecraftClient client, AbstractClientPlayerEntity player, Hand hand, float delta) {
        boolean right = (hand == Hand.MAIN_HAND) == (player.getMainArm() == Arm.RIGHT);
        double side = right ? 1.0 : -1.0;
        if (player == client.player && client.options.getPerspective().isFirstPerson()) {
            Vec3d forward = player.getRotationVec(delta);
            Vec3d across = forward.crossProduct(new Vec3d(0.0, 1.0, 0.0));
            across = across.lengthSquared() < 1.0E-4 ? new Vec3d(1.0, 0.0, 0.0) : across.normalize();
            Vec3d up = across.crossProduct(forward).normalize();
            return player.getCameraPosVec(delta)
                    .add(forward.multiply(0.5))
                    .add(across.multiply(0.36 * side))
                    .add(up.multiply(-0.34));
        }
        float bodyYaw = MathHelper.lerp(delta, player.lastBodyYaw, player.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        double height = player.isInSneakingPose() ? 0.55 : 0.72;
        // Our frame: +x is the left, +z the way the body faces.
        Vec3d local = new Vec3d(-0.36 * side, height, 0.28);
        return player.getLerpedPos(delta).add(local.rotateY(-bodyYaw));
    }

    /** A hose hangs: the middle sags with its length, but never through the forecourt. */
    private static void drawHose(MatrixStack matrices, VertexConsumer lines, Vec3d camera,
                                 Vec3d from, Vec3d to, int groundY) {
        double span = Math.sqrt(MathHelper.square(to.x - from.x) + MathHelper.square(to.z - from.z));
        double sag = 0.15 + span * 0.18;
        double floor = groundY + 0.04;
        Vec3d last = null;
        MatrixStack.Entry entry = matrices.peek();
        for (int i = 0; i <= SEGMENTS; i++) {
            double t = i / (double) SEGMENTS;
            Vec3d at = from.lerp(to, t).add(0.0, -sag * 4.0 * t * (1.0 - t), 0.0);
            at = new Vec3d(at.x, Math.max(at.y, floor), at.z);
            if (last != null) {
                Vec3d along = at.subtract(last);
                if (along.lengthSquared() > 1.0E-8) {
                    along = along.normalize();
                    double distance = Math.max(0.5, last.lerp(at, 0.5).distanceTo(camera));
                    float width = (float) MathHelper.clamp(9.0 / distance, 1.5, 12.0);
                    vertex(lines, entry, last, camera, along, width);
                    vertex(lines, entry, at, camera, along, width);
                }
            }
            last = at;
        }
    }

    private static void vertex(VertexConsumer lines, MatrixStack.Entry entry, Vec3d at, Vec3d camera,
                               Vec3d along, float width) {
        lines.vertex(entry, (float) (at.x - camera.x), (float) (at.y - camera.y), (float) (at.z - camera.z))
                .color(HOSE_COLOUR)
                .normal(entry, (float) along.x, (float) along.y, (float) along.z)
                .lineWidth(width);
    }
}
