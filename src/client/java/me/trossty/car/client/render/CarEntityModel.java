package me.trossty.car.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The car model. Built with negative Y pointing up and negative Z pointing
 * forward, which is what the renderer's {@code scale(-1, -1, 1)} expects.
 *
 * <p>The cabin is hollow — four walls and a roof rather than one solid box —
 * because the occupants are rendered inside it. A seated player reaches 1.4
 * above the seat point (which is their hips, not their feet — see
 * {@code CarEntity.SEAT_HEIGHT}), and the seat is level with the cabin floor at
 * 0.75, so the head tops out at 2.0. The interior ceiling is therefore at 2.125
 * and the roof at 2.1875.
 *
 * <p>Model units are sixteenths of a block, so the cabin's {@code y} range of
 * -34..-12 is the world range 0.75..2.125.
 *
 * <h2>Why the tree has a {@code chassis} in it</h2>
 * Everything bolted to the bodyshell hangs off {@code chassis}, and the four
 * wheels do not. That is the whole point: when the body leans into a corner or
 * squats under power it is turning about a point inside itself, while the wheels
 * stay flat on the road. One part for the whole car could not do that.
 *
 * <p>{@code chassis} pivots at {@link #BODY_PIVOT_Y}, and {@code frame}
 * immediately translates back by the same amount, so every part below it keeps
 * the plain absolute coordinates it would have had anyway.
 *
 * <h2>The steering wheel</h2>
 * One flat box, textured as a whole steering wheel with the gaps between the
 * spokes left at alpha 0. {@code EntityModel} draws on
 * {@code entityCutoutNoCull}, so those pixels become real holes — the same trick
 * the windows already use — and a single box reads as a ring with spokes without
 * costing seven.
 *
 * <p>It is a child of {@code steering_mount}, which carries the column's
 * {@link #STEERING_TILT}. Nesting is what makes the animation work:
 * {@code ModelPart} applies roll outside pitch, so a part that both tilts and
 * spins would spin about the cabin's axis rather than the column's. Below a
 * tilted parent, the child's roll is the column's own axis.
 */
public class CarEntityModel extends EntityModel<CarRenderState> {

    private static final String CHASSIS = "chassis";
    private static final String FRAME = "frame";
    private static final String BODY = "body";
    private static final String CABIN_FRONT = "cabin_front";
    private static final String CABIN_BACK = "cabin_back";
    private static final String CABIN_LEFT = "cabin_left";
    private static final String CABIN_RIGHT = "cabin_right";
    private static final String CABIN_ROOF = "cabin_roof";
    private static final String DASHBOARD = "dashboard";
    private static final String STEERING_MOUNT = "steering_mount";
    private static final String STEERING_COLUMN = "steering_column";
    private static final String STEERING_WHEEL = "steering_wheel";
    private static final String SEAT_LEFT = "seat_left";
    private static final String SEAT_RIGHT = "seat_right";
    private static final String TANKER_BARREL = "tanker_barrel";
    private static final String TANKER_DECK = "tanker_deck";
    private static final String TANKER_CAP = "tanker_cap";
    private static final String BED_FLOOR = "bed_floor";
    private static final String BED_LEFT = "bed_left";
    private static final String BED_RIGHT = "bed_right";
    private static final String BED_TAIL = "bed_tail";
    private static final String BED_DECK = "bed_deck";
    private static final String WHEEL_FRONT_LEFT = "wheel_front_left";
    private static final String WHEEL_FRONT_RIGHT = "wheel_front_right";
    private static final String WHEEL_BACK_LEFT = "wheel_back_left";
    private static final String WHEEL_BACK_RIGHT = "wheel_back_right";
    private static final String WHEEL_MID_LEFT = "wheel_mid_left";
    private static final String WHEEL_MID_RIGHT = "wheel_mid_right";

    /** Texture sheet size; the hollow cabin needs more room than one solid box did. */
    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    private static final float DEGREES = (float) Math.PI / 180.0F;

    /**
     * The height the body leans and pitches about, world 0.875 — roughly where a
     * car's roll centre sits, between the floor pan and the window line. Pivoting
     * at the ground instead would swing the roof about like a metronome.
     */
    private static final float BODY_PIVOT_Y = -14.0F;

    /** Where the driver sits sideways, matching {@code CarEntity.SEAT_SIDEWAYS}. */
    private static final float DRIVER_X = -3.5F;

    /** How far the column lays the wheel back towards the driver, in radians. */
    private static final float STEERING_TILT = 0.45F;

    /**
     * How much further the wheel turns than the road wheels. A real rack is
     * geared about 15:1; four is enough to read clearly from the passenger seat
     * without the spokes blurring.
     */
    private static final float STEERING_RATIO = 4.0F;

    /** Radians of lean per degree of steering, at full speed. */
    private static final float LEAN_PER_STEER_DEGREE = 0.0025F;

    /** Radians of pitch per block-per-tick-squared of acceleration. */
    private static final float PITCH_PER_ACCELERATION = 2.0F;

    /** As far as the body is allowed to dive under the brakes, in radians. */
    private static final float MAX_PITCH = 0.06F;

    /** Model units the idling engine shakes the bodyshell by. */
    private static final float IDLE_SHAKE = 0.16F;

    /** Above this speed the idle shake is gone; the road takes over. */
    private static final float IDLE_FADE_SPEED = 0.08F;

    /** Every wheel on this particular vehicle: four on a car, six on a tanker. */
    private final ModelPart[] rollingWheels;
    private final ModelPart chassis;
    private final ModelPart steeringWheel;
    private final ModelPart frontLeftWheel;
    private final ModelPart frontRightWheel;
    private final ModelPart backLeftWheel;
    private final ModelPart backRightWheel;

    public CarEntityModel(ModelPart root) {
        super(root);
        this.chassis = root.getChild(CHASSIS);
        this.steeringWheel = this.chassis
                .getChild(FRAME)
                .getChild(STEERING_MOUNT)
                .getChild(STEERING_WHEEL);
        this.frontLeftWheel = root.getChild(WHEEL_FRONT_LEFT);
        this.frontRightWheel = root.getChild(WHEEL_FRONT_RIGHT);
        this.backLeftWheel = root.getChild(WHEEL_BACK_LEFT);
        this.backRightWheel = root.getChild(WHEEL_BACK_RIGHT);
        this.rollingWheels = root.hasChild(WHEEL_MID_LEFT)
                ? new ModelPart[] {this.frontLeftWheel, this.frontRightWheel,
                        this.backLeftWheel, this.backRightWheel,
                        root.getChild(WHEEL_MID_LEFT), root.getChild(WHEEL_MID_RIGHT)}
                : new ModelPart[] {this.frontLeftWheel, this.frontRightWheel,
                        this.backLeftWheel, this.backRightWheel};
    }

    /**
     * Which body is bolted to the shared chassis. Everything in front of the
     * cabin's back wall is the same for all three, which is the point: the
     * seats, the steering column and the seating arithmetic are shared rather
     * than copied three times.
     */
    private enum Variant {
        /** Four wheels, nothing behind the cabin. */
        CAR,
        /** A barrel, a rail to carry it and a third axle under it. */
        TANKER,
        /** An open cargo bed, a longer tail and the back axle moved under it. */
        PICKUP
    }

    public static LayerDefinition getTexturedModelData() {
        return build(Variant.CAR);
    }

    /**
     * The same vehicle with a barrel on the back, a chassis rail to carry it and
     * a third axle under it. Everything in front of the back wall is identical,
     * which is the point: the cabin, the seats and the whole seating arithmetic
     * are shared rather than copied.
     */
    public static LayerDefinition getTankerModelData() {
        return build(Variant.TANKER);
    }

    /**
     * The same vehicle with its roof cut short at the back and a bed in its
     * place: four walls, a tailgate and a chassis rail under the overhang. The
     * back axle moves back with the bed, so the load sits over the wheels
     * instead of behind them.
     */
    public static LayerDefinition getPickupModelData() {
        return build(Variant.PICKUP);
    }

    private static LayerDefinition build(Variant variant) {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition root = modelData.getRoot();

        PartDefinition chassis = root.addOrReplaceChild(
                CHASSIS, CubeListBuilder.create(), PartPose.offset(0.0F, BODY_PIVOT_Y, 0.0F));
        // Undoes the pivot, so everything below writes plain absolute coordinates.
        PartDefinition frame = chassis.addOrReplaceChild(
                FRAME, CubeListBuilder.create(), PartPose.offset(0.0F, -BODY_PIVOT_Y, 0.0F));

        // Floor pan and bodywork: world y 0.375 .. 0.75, the full 2.5 blocks long.
        frame.addOrReplaceChild(
                BODY,
                CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -12.0F, -20.0F, 20, 6, 40),
                PartPose.ZERO);

        // Cabin walls, 2 units thick, enclosing x -8..8 and z -10..12.
        frame.addOrReplaceChild(CABIN_FRONT,
                CubeListBuilder.create().texOffs(0, 64).addBox(-10.0F, -34.0F, -12.0F, 20, 22, 2),
                PartPose.ZERO);
        frame.addOrReplaceChild(CABIN_BACK,
                CubeListBuilder.create().texOffs(48, 64).addBox(-10.0F, -34.0F, 12.0F, 20, 22, 2),
                PartPose.ZERO);
        frame.addOrReplaceChild(CABIN_LEFT,
                CubeListBuilder.create().texOffs(0, 96).addBox(-10.0F, -34.0F, -10.0F, 2, 22, 22),
                PartPose.ZERO);
        frame.addOrReplaceChild(CABIN_RIGHT,
                CubeListBuilder.create().texOffs(52, 96).addBox(8.0F, -34.0F, -10.0F, 2, 22, 22),
                PartPose.ZERO);
        frame.addOrReplaceChild(CABIN_ROOF,
                CubeListBuilder.create().texOffs(96, 64).addBox(-10.0F, -35.0F, -12.0F, 20, 1, 26),
                PartPose.ZERO);

        // --- interior ---

        // Across the cabin front, world y 0.9375 .. 1.3125. It stops short of the
        // seats in z so the riders' legs, which fold forward almost flat from the
        // hips, pass under it rather than through it.
        frame.addOrReplaceChild(DASHBOARD,
                CubeListBuilder.create().texOffs(0, 144).addBox(-8.0F, -21.0F, -10.0F, 16, 6, 2),
                PartPose.ZERO);

        // The hub is as far forward as the column will reach, and the wheel is
        // 7 units rather than a realistic 9. At a 70 degree field of view a
        // correctly placed wheel fills half the windscreen: the driver's eye is
        // genuinely about that close to it, and the only fix is to cheat.
        PartDefinition steeringMount = frame.addOrReplaceChild(
                STEERING_MOUNT,
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(DRIVER_X, -20.0F, -6.0F, STEERING_TILT, 0.0F, 0.0F));
        // Runs forward from the hub into the dashboard; the mount's tilt is what
        // drops its far end, so it is drawn straight here.
        steeringMount.addOrReplaceChild(STEERING_COLUMN,
                CubeListBuilder.create().texOffs(44, 144).addBox(-1.0F, -1.0F, -4.0F, 2, 2, 3),
                PartPose.ZERO);
        steeringMount.addOrReplaceChild(STEERING_WHEEL,
                CubeListBuilder.create().texOffs(68, 144).addBox(-3.5F, -3.5F, -0.5F, 7, 7, 1),
                PartPose.ZERO);

        // Backrests only. A cushion cannot rise above the floor without
        // swallowing the rider, whose hips sit level with it at 0.75, so the seat
        // pads are painted onto the floor pan's top face instead.
        frame.addOrReplaceChild(SEAT_LEFT,
                CubeListBuilder.create().texOffs(96, 144).addBox(-6.5F, -26.0F, 8.0F, 6, 14, 2),
                PartPose.ZERO);
        frame.addOrReplaceChild(SEAT_RIGHT,
                CubeListBuilder.create().texOffs(116, 144).addBox(0.5F, -26.0F, 8.0F, 6, 14, 2),
                PartPose.ZERO);

        // --- running gear, outside the chassis so the road keeps it level ---

        root.addOrReplaceChild(WHEEL_FRONT_LEFT, CubeListBuilder.create().texOffs(0, 48).addBox(-1.5F, -3.0F, -3.0F, 3, 6, 6),
                PartPose.offset(-11.0F, -3.0F, -14.0F));
        root.addOrReplaceChild(WHEEL_FRONT_RIGHT, CubeListBuilder.create().texOffs(20, 48).addBox(-1.5F, -3.0F, -3.0F, 3, 6, 6),
                PartPose.offset(11.0F, -3.0F, -14.0F));
        // The pickup's back axle follows its bed; everything else keeps the
        // car's wheelbase so the steering geometry stays honest.
        float backAxleZ = variant == Variant.PICKUP ? 24.0F : 14.0F;
        root.addOrReplaceChild(WHEEL_BACK_LEFT, CubeListBuilder.create().texOffs(40, 48).addBox(-1.5F, -3.0F, -3.0F, 3, 6, 6),
                PartPose.offset(-11.0F, -3.0F, backAxleZ));
        root.addOrReplaceChild(WHEEL_BACK_RIGHT, CubeListBuilder.create().texOffs(60, 48).addBox(-1.5F, -3.0F, -3.0F, 3, 6, 6),
                PartPose.offset(11.0F, -3.0F, backAxleZ));

        if (variant == Variant.PICKUP) {
            // A bed is four walls and a floor laid on the pan, nothing clever.
            // The floor sits one unit proud of the pan's top so the pan's paint
            // never shows through the planks.
            frame.addOrReplaceChild(BED_FLOOR,
                    CubeListBuilder.create().texOffs(0, 204).addBox(-10.0F, -14.0F, 14.0F, 20, 2, 20),
                    PartPose.ZERO);
            frame.addOrReplaceChild(BED_LEFT,
                    CubeListBuilder.create().texOffs(84, 204).addBox(-10.0F, -26.0F, 14.0F, 2, 12, 22),
                    PartPose.ZERO);
            frame.addOrReplaceChild(BED_RIGHT,
                    CubeListBuilder.create().texOffs(136, 204).addBox(8.0F, -26.0F, 14.0F, 2, 12, 22),
                    PartPose.ZERO);
            frame.addOrReplaceChild(BED_TAIL,
                    CubeListBuilder.create().texOffs(188, 204).addBox(-10.0F, -26.0F, 34.0F, 20, 12, 2),
                    PartPose.ZERO);
            // The pan stops at z 20; this rail carries the rest of the bed. It
            // butts straight up against the bed floor, which the tanker's own
            // rail does not need to do and this one does — a gap under a flat
            // bed reads as two vehicles parked in the same place.
            frame.addOrReplaceChild(BED_DECK,
                    CubeListBuilder.create().texOffs(188, 224).addBox(-9.0F, -12.0F, 20.0F, 18, 4, 14),
                    PartPose.ZERO);
        }

        if (variant == Variant.TANKER) {
            // The barrel starts flush with the back wall (z 14) and overhangs the
            // floor pan, so the rail below it is what it is standing on.
            frame.addOrReplaceChild(TANKER_BARREL,
                    CubeListBuilder.create().texOffs(0, 168).addBox(-9.0F, -30.0F, 14.0F, 18, 18, 18),
                    PartPose.ZERO);
            frame.addOrReplaceChild(TANKER_DECK,
                    CubeListBuilder.create().texOffs(80, 168).addBox(-8.0F, -10.0F, 20.0F, 16, 4, 12),
                    PartPose.ZERO);
            frame.addOrReplaceChild(TANKER_CAP,
                    CubeListBuilder.create().texOffs(150, 168).addBox(-3.0F, -32.0F, 20.0F, 6, 2, 6),
                    PartPose.ZERO);

            // A third axle under the load, reusing the back wheels' own texture.
            root.addOrReplaceChild(WHEEL_MID_LEFT,
                    CubeListBuilder.create().texOffs(40, 48).addBox(-1.5F, -3.0F, -3.0F, 3, 6, 6),
                    PartPose.offset(-11.0F, -3.0F, 26.0F));
            root.addOrReplaceChild(WHEEL_MID_RIGHT,
                    CubeListBuilder.create().texOffs(60, 48).addBox(-1.5F, -3.0F, -3.0F, 3, 6, 6),
                    PartPose.offset(11.0F, -3.0F, 26.0F));
        }

        return LayerDefinition.create(modelData, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void setupAnim(CarRenderState state) {
        super.setupAnim(state);

        float roll = state.wheelAngle * DEGREES;
        float steer = state.steerAngle * DEGREES;

        for (ModelPart wheel : this.rollingWheels) {
            wheel.xRot = roll;
        }

        this.frontLeftWheel.yRot = steer;
        this.frontRightWheel.yRot = steer;

        // Negated: steering left is a negative steerAngle, and a positive roll
        // about the column takes the top of the wheel to the driver's left.
        this.steeringWheel.zRot = -state.steerAngle * STEERING_RATIO * DEGREES;

        this.chassis.xRot = Mth.clamp(
                -state.acceleration * PITCH_PER_ACCELERATION, -MAX_PITCH, MAX_PITCH);
        // Leans away from the corner, and only once there is speed to lean with.
        float speedFraction = Math.min(1.0F, Math.abs(state.speed) / 0.4F);
        this.chassis.zRot = state.steerAngle * LEAN_PER_STEER_DEGREE * speedFraction;

        float idle = state.engineRunning
                ? 1.0F - Math.min(1.0F, Math.abs(state.speed) / IDLE_FADE_SPEED)
                : 0.0F;
        float shake = idle * IDLE_SHAKE;
        this.chassis.x = Mth.cos(state.ageInTicks * 2.3F) * shake * 0.6F;
        this.chassis.y = BODY_PIVOT_Y + Mth.sin(state.ageInTicks * 1.9F) * shake;
    }
}
