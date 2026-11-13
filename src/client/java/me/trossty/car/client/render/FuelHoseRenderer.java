package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The two things you see while filling up: the pump's hose, hanging from the
 * pump to whoever has its nozzle, and a ring on the filler cap of the car in
 * front of you while there is a can or a nozzle in your hand, so you know
 * where to point it.
 */
public final class FuelHoseRenderer {

    private static final int HOSE_COLOUR = 0xFF262626;
    /** Its colour at the very end of its reach. */
    private static final int HOSE_STRAINED = 0xFFB02418;
    private static final double HOSE_RADIUS = 0.045;
    private static final RenderType HOSE = RenderType.entitySolid(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"));
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
        float delta = client.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 camera = context.camera();
        PoseStack matrices = context.matrices();

        for (AbstractClientPlayer player : client.level.players()) {
            if (player.distanceToSqr(camera) > 64 * 64) {
                continue;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                BlockPos pump = held.is(ModItems.FUEL_NOZZLE) ? held.get(ModComponents.PUMP) : null;
                Vec3 outlet = pump == null ? null : FuelPumpBlock.hoseOutlet(client.level, pump);
                if (outlet != null) {
                    drawHose(matrices, consumers.getBuffer(HOSE), camera, outlet, handOf(client, player, hand, delta), pump.getY());
                }
            }
        }

        if (!client.options.hideGui && !self.isPassenger()
                && (holdsFiller(self.getMainHandItem()) || holdsFiller(self.getOffhandItem()))) {
            VertexConsumer lines = consumers.getBuffer(RenderType.lines());
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
                context.outline(lines, box, lit ? CAP_AIMED : CAP_IDLE);
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

    /**
     * The hose as a rubber tube: a Hermite curve that leaves the pump heading
     * down and reaches the nozzle from below, so it hangs under its own weight,
     * never through the forecourt, drawn as a square tube lit by the world. It
     * reddens as it nears the end of its reach.
     */
    private static void drawHose(PoseStack matrices, VertexConsumer tube, Vec3 camera,
                                 Vec3 from, Vec3 to, int groundY) {
        Minecraft client = Minecraft.getInstance();
        double length = from.distanceTo(to);
        double pull = Math.max(1.0, length * 1.4);
        Vec3 leave = new Vec3(0.0, -pull, 0.0);
        Vec3 arrive = new Vec3(0.0, pull * 0.6, 0.0);
        double floor = groundY + HOSE_RADIUS;

        Vec3[] at = new Vec3[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            double t = i / (double) SEGMENTS;
            Vec3 p = hermite(from, leave, to, arrive, t);
            at[i] = new Vec3(p.x, Math.max(p.y, floor), p.z);
        }
        float strain = (float) Math.pow(Math.min(1.0, length / FuelPumpBlockEntity.HOSE_LENGTH), 6.0);
        int colour = FastColor.ARGB32.lerp(strain, HOSE_COLOUR, HOSE_STRAINED);

        PoseStack.Pose pose = matrices.last();
        Vec3[] ring = null;
        for (int i = 0; i <= SEGMENTS; i++) {
            Vec3 along = at[Math.min(i + 1, SEGMENTS)].subtract(at[Math.max(i - 1, 0)]);
            if (along.lengthSqr() < 1.0E-10) {
                along = new Vec3(0.0, -1.0, 0.0);
            }
            along = along.normalize();
            Vec3 side = along.cross(new Vec3(0.0, 1.0, 0.0));
            side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
            Vec3 up = side.cross(along).normalize();
            // The four corners' directions from the centre line, round the tube.
            Vec3[] next = {side.add(up), up.subtract(side), side.add(up).reverse(), side.subtract(up)};
            if (ring != null) {
                int light = LevelRenderer.getLightColor(client.level, BlockPos.containing(at[i]));
                for (int k = 0; k < 4; k++) {
                    int n = (k + 1) % 4;
                    Vec3 normal = ring[k].add(ring[n]).normalize();
                    corner(tube, pose, camera, at[i - 1], ring[k], normal, colour, light);
                    corner(tube, pose, camera, at[i], next[k], normal, colour, light);
                    corner(tube, pose, camera, at[i], next[n], normal, colour, light);
                    corner(tube, pose, camera, at[i - 1], ring[n], normal, colour, light);
                }
            }
            ring = next;
        }
    }

    private static Vec3 hermite(Vec3 p0, Vec3 m0, Vec3 p1, Vec3 m1, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return p0.scale(2 * t3 - 3 * t2 + 1)
                .add(m0.scale(t3 - 2 * t2 + t))
                .add(p1.scale(-2 * t3 + 3 * t2))
                .add(m1.scale(t3 - t2));
    }

    private static void corner(VertexConsumer tube, PoseStack.Pose pose, Vec3 camera, Vec3 centre, Vec3 out,
                               Vec3 normal, int colour, int light) {
        double r = HOSE_RADIUS / Math.sqrt(2.0);
        tube.addVertex(pose, (float) (centre.x + out.x * r - camera.x), (float) (centre.y + out.y * r - camera.y),
                        (float) (centre.z + out.z * r - camera.z))
                .setColor(colour)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
