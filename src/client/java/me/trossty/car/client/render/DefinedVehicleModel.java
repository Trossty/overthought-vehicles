package me.trossty.car.client.render;

import me.trossty.car.CarMain;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3fc;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A vehicle built at run time out of the boxes in its data-pack definition.
 *
 * <p>There is no model file and no texture file. The model is assembled from
 * the definition's boxes the first time one is seen, and its texture is a
 * 16×16 palette made on the spot: one pixel for every colour the definition
 * uses, repainted for each dye the car can wear. Every box is mapped onto the
 * middle of its own pixel, which is how a flat-coloured box is drawn with the
 * same machinery as everything else — light, damage flash and all.
 *
 * <p>The animation is the car's: wheels roll and steer, the body leans into
 * corners, squats under power and trembles at idle.
 */
public class DefinedVehicleModel extends VehicleModel {

    /** The palette is this many pixels square, so a vehicle has this many colours squared. */
    private static final int PALETTE = 16;

    /**
     * How many model texels one palette pixel is stretched over. A box's
     * unwrapped faces have to fit inside one pixel with room to spare, and the
     * biggest box a pack may draw unwraps to well under this.
     */
    private static final int CELL = 2048;

    private static final float DEGREES = (float) Math.PI / 180.0F;
    private static final float BODY_PIVOT_Y = -14.0F;
    private static final float LEAN_PER_STEER_DEGREE = 0.0025F;
    private static final float PITCH_PER_ACCELERATION = 2.0F;
    private static final float MAX_PITCH = 0.06F;
    private static final float IDLE_SHAKE = 0.16F;
    private static final float IDLE_FADE_SPEED = 0.08F;

    private static final String STEERING_WHEEL = "steering_wheel";
    private static final String PROPELLER = "propeller";

    /** The part a spinner hangs from: the plain name for a propeller, as it always was. */
    private static String spinnerName(net.minecraft.core.Direction.Axis axis) {
        return axis == net.minecraft.core.Direction.Axis.Z ? PROPELLER : PROPELLER + "_" + axis.getName();
    }
    /** A running propeller's turn, radians a tick: a blur, not a count. */
    private static final float PROP_SPEED = 1.9F;
    private static final String STEERING_WHEEL_COLOUR = "#1E1E21";
    /** From the driver's seat to the middle of the wheel: up, and forwards. */
    private static final float WHEEL_RISE = 9.0F;
    private static final float WHEEL_REACH = 10.0F;
    /** How far the top of the dash stands over the middle of the wheel, at the most: a car's 2.6. */
    private static final float CLUSTER_OVER_WHEEL = 2.6F;
    /** Degrees a car's wheel leans back towards the driver; a definition can say otherwise. */
    private static final float WHEEL_RAKE = 28.0F;
    private static final float WHEEL_RIM = 2.8F;
    /** The rim is this many pieces round, and this thick. */
    private static final int RIM_PIECES = 16;
    private static final float RIM_THICK = 0.6F;
    /** Turns of the wheel per turn of the road wheels. */
    private static final float WHEEL_RATIO = 2.5F;

    /** How many slabs are crossed to make the tread: sixteen make a 32-sided wheel, round to the eye. */
    private static final int TREAD_SLABS = 16;
    /** Degrees between one slab and the next. */
    private static final float SLAB_STEP = 180.0F / TREAD_SLABS;
    /** Half the angle one flat of the rim spans. */
    private static final double HALF_FLAT = Math.PI / (2.0 * TREAD_SLABS);

    /** From the axle to the middle of a flat of the tread, and how long each flat is. */
    private static final float TREAD_REACH = 7.54F;
    private static final float TREAD_FLAT = (float) (2.0 * TREAD_REACH * Math.tan(HALF_FLAT));

    /** The sidewall's bulge: this far out from the middle, either side, and this far round. */
    private static final float WALL_HALF = 2.25F;
    private static final float WALL_REACH = 6.9F;
    private static final float WALL_FLAT = (float) (2.0 * WALL_REACH * Math.tan(HALF_FLAT));

    /** The hub: a smooth, many-sided disc a little proud of the sidewall. */
    private static final float HUB_HALF = 2.6F;
    private static final float HUB_REACH = 4.4F;
    private static final float HUB_FLAT = (float) (2.0 * HUB_REACH * Math.tan(HALF_FLAT));

    /*
     * Where the wheel sheet keeps what a rim is drawn with (see
     * tools/gen_wheel_faces.py): the hub painted open, dark between the
     * centre cap and the lip, for spoked rims; and three patches of one
     * colour each, the rim's own, its wheel nuts', and the dark behind it.
     */
    private static final int[] HUB_OPEN_UV = {0, 0};
    private static final int[] RIM_UV = {14, 0};
    private static final int[] NUT_UV = {23, 0};
    private static final int[] DARK_UV = {31, 0};
    /** How far a spoke or a nut stands out of the face of the hub, and how far a hole or a slot. */
    private static final float RIM_PROUD = 0.25F;
    private static final float HOLE_PROUD = 0.03F;

    /** One built vehicle, kept for as long as its definition is the loaded one. */
    private record Built(VehicleDefinition definition, DefinedVehicleModel model,
                         @org.jetbrains.annotations.Nullable DefinedVehicleModel glass,
                         DefinedVehicleModel wheels,
                         @org.jetbrains.annotations.Nullable DefinedVehicleModel headLamps,
                         @org.jetbrains.annotations.Nullable DefinedVehicleModel tailLamps,
                         List<String> colours, Map<Integer, ResourceLocation> textures) {
    }

    private static final Map<ResourceLocation, Built> CACHE = new HashMap<>();

    private final ModelPart chassis;
    /** The fork and bars, turned with the front wheel; null if nothing steers. */
    private final ModelPart steerTurn;
    /** The steering wheel in the cabin, turned with the road wheels; null if there is none. */
    private final ModelPart steeringWheel;
    /** What spins while the engine runs, by the axis it spins about: a propeller, a rotor. */
    private final Map<net.minecraft.core.Direction.Axis, ModelPart> spinners =
            new java.util.EnumMap<>(net.minecraft.core.Direction.Axis.class);
    private final List<ModelPart> wheels = new ArrayList<>();
    private final List<ModelPart> steered = new ArrayList<>();
    /** Each wheel's size as {width, radius} scales, put back every frame. */
    private final List<float[]> wheelScales = new ArrayList<>();
    /** The radius, in pixels, the wheel is built to before it is scaled to size. */
    private static final float WHEEL_MODEL_RADIUS = 7.6F;
    /** How far a flat tyre's rim sinks, as a share of the radius, and how much fatter the tyre gets. */
    private static final float FLAT_SINK = 0.24F;
    private static final float FLAT_BULGE = 1.22F;
    /** The rubber of every wheel, and the hubs, so a removed tyre or disc can leave the picture. */
    private final List<ModelPart> rubber = new ArrayList<>();
    private final List<ModelPart> hubs = new ArrayList<>();
    /** Each wheel's spokes, nuts and the rest of its rim's face, which come off with the tyre. */
    private final List<List<ModelPart>> rims = new ArrayList<>();
    /** The bonnet and the boot lid, each hung on its hinge; null where there is none. */
    private final ModelPart bonnet;
    private final ModelPart boot;

    private DefinedVehicleModel(ModelPart root, VehicleDefinition definition, boolean glass) {
        // The bodywork is solid; the glass is its own model, drawn see-through
        // and after everything else, so whoever sits behind it still shows.
        super(root, glass ? VehicleRenderTypes::glass
                : net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.chassis = root.hasChild("chassis") ? root.getChild("chassis") : null;
        ModelPart turn = null;
        if (this.chassis != null) {
            ModelPart frame = this.chassis.getChild("frame");
            if (frame.hasChild("steer_axis")) {
                turn = frame.getChild("steer_axis").getChild("steer");
            }
        }
        this.steerTurn = turn;
        this.steeringWheel = this.chassis != null && this.chassis.getChild("frame").hasChild(STEERING_WHEEL)
                ? this.chassis.getChild("frame").getChild(STEERING_WHEEL).getChild("turn")
                : null;
        this.bonnet = this.chassis != null && this.chassis.getChild("frame").hasChild(VehicleDefinition.Box.BONNET)
                ? this.chassis.getChild("frame").getChild(VehicleDefinition.Box.BONNET)
                : null;
        this.boot = this.chassis != null && this.chassis.getChild("frame").hasChild(VehicleDefinition.Box.BOOT)
                ? this.chassis.getChild("frame").getChild(VehicleDefinition.Box.BOOT)
                : null;
        for (net.minecraft.core.Direction.Axis axis : net.minecraft.core.Direction.Axis.values()) {
            if (this.chassis != null && this.chassis.getChild("frame").hasChild(spinnerName(axis))) {
                this.spinners.put(axis, this.chassis.getChild("frame").getChild(spinnerName(axis)));
            }
        }
        for (int index = 0; this.chassis == null && index < definition.wheels().size(); index++) {
            ModelPart wheel = root.getChild("wheel" + index);
            VehicleDefinition.Wheel drawn = definition.wheels().get(index);
            this.wheelScales.add(new float[] {drawn.width() / 4.0F, drawn.radius() / 7.6F});
            this.wheels.add(wheel);
            for (int slab = 0; slab < TREAD_SLABS; slab++) {
                this.rubber.add(wheel.getChild("wall" + slab));
                this.rubber.add(wheel.getChild("tread" + slab));
            }
            for (int slab = 0; slab < TREAD_SLABS; slab++) {
                this.hubs.add(wheel.getChild("hub" + slab));
            }
            List<ModelPart> rim = new ArrayList<>();
            for (int piece = 0; wheel.hasChild("rim" + piece); piece++) {
                rim.add(wheel.getChild("rim" + piece));
            }
            this.rims.add(rim);
            if (definition.wheels().get(index).steers()) {
                this.steered.add(wheel);
            }
        }
    }

    /** The model for this vehicle, built now if it has not been seen before. */
    public static DefinedVehicleModel of(ResourceLocation id, VehicleDefinition definition) {
        return built(id, definition).model();
    }

    /** The same vehicle's wheels, which are drawn with a sheet of their own. */
    public static DefinedVehicleModel wheelsOf(ResourceLocation id, VehicleDefinition definition) {
        return built(id, definition).wheels();
    }

    /** The sheet for a kind of tyre: what is fitted is what is seen. */
    public static ResourceLocation wheelTexture(String tyre) {
        return CarMain.id("textures/entity/wheel/" + tyre + ".png");
    }

    /** The palette for this vehicle in this paint, made now if it is the first. */
    public static ResourceLocation textureOf(ResourceLocation id, VehicleDefinition definition, int paint) {
        Built built = built(id, definition);
        return built.textures().computeIfAbsent(paint & 0xFFFFFF, rgb -> {
            NativeImage image = new NativeImage(PALETTE, PALETTE, false);
            for (int index = 0; index < PALETTE * PALETTE; index++) {
                String colour = built.colours().get(Math.min(index, built.colours().size() - 1));
                image.setPixelRGBA(index % PALETTE, index / PALETTE,
                        net.minecraft.util.FastColor.ABGR32.fromArgb32(VehicleDefinition.resolve(colour, rgb)));
            }
            ResourceLocation texture = CarMain.id("dynamic/vehicle/" + id.getNamespace() + "/"
                    + id.getPath() + "/" + String.format("%06x", rgb));
            Minecraft.getInstance().getTextureManager().register(
                    texture, new DynamicTexture(image));
            return texture;
        });
    }

    private static Built built(ResourceLocation id, VehicleDefinition definition) {
        Built known = CACHE.get(id);
        // Compared by identity: joining another world hands out new definitions
        // under the same ids, and they need not say the same thing.
        if (known != null && known.definition() == definition) {
            return known;
        }
        if (known != null) {
            for (ResourceLocation stale : known.textures().values()) {
                Minecraft.getInstance().getTextureManager().release(stale);
            }
        }
        List<String> colours = new ArrayList<>();
        ModelPart root = build(definition, colours, false);
        boolean anyGlass = definition.shape().stream().anyMatch(box -> isGlass(box.color()));
        Built fresh = new Built(definition, new DefinedVehicleModel(root, definition, false),
                anyGlass ? new DefinedVehicleModel(build(definition, colours, true), definition, true) : null,
                new DefinedVehicleModel(buildWheels(definition), definition, false),
                lamps(definition, colours, HEAD_LAMPS), lamps(definition, colours, TAIL_LAMPS),
                colours, new HashMap<>());
        CACHE.put(id, fresh);
        return fresh;
    }

    private static int[] uv(List<String> colours, String colour) {
        int index = colours.indexOf(colour);
        if (index < 0) {
            if (colours.size() < PALETTE * PALETTE) {
                colours.add(colour);
            }
            index = colours.size() - 1;
        }
        // A quarter of the way into the pixel: the unwrap then stays inside it.
        return new int[] {(index % PALETTE) * CELL + CELL / 4, (index / PALETTE) * CELL + CELL / 4};
    }

    /**
     * Turns the definition's boxes into a model. A pack writes y up and z
     * forwards; a Minecraft model has both the other way round.
     */
    /** Where a tanker's load goes: drawn by the tanker, in the load, not by the body. */
    public static boolean isCargo(String colour) {
        return "cargo".equals(colour);
    }

    /**
     * Moves {@code matrices} onto the body frame the boxes are written in,
     * swaying as the body does in {@link #setAngles}: for drawing something
     * that has to stay inside the bodywork, a tanker's load.
     */
    public static void applyBody(com.mojang.blaze3d.vertex.PoseStack matrices, CarRenderState state) {
        float pitch = Mth.clamp(-state.acceleration * PITCH_PER_ACCELERATION, -MAX_PITCH, MAX_PITCH);
        float speedFraction = Math.min(1.0F, Math.abs(state.speed) / 0.4F);
        float roll = state.steerAngle * LEAN_PER_STEER_DEGREE * speedFraction;
        float idle = state.engineRunning
                ? 1.0F - Math.min(1.0F, Math.abs(state.speed) / IDLE_FADE_SPEED)
                : 0.0F;
        float shake = idle * IDLE_SHAKE;
        float originX = Mth.cos(state.ageInTicks * 2.3F) * shake * 0.6F;
        float originY = BODY_PIVOT_Y + Mth.sin(state.ageInTicks * 1.9F) * shake;
        matrices.translate(originX / 16.0F, originY / 16.0F, 0.0F);
        matrices.mulPose(new org.joml.Quaternionf().rotationZYX(roll, 0.0F, pitch));
        matrices.translate(0.0F, -BODY_PIVOT_Y / 16.0F, 0.0F);
    }

    /** An #AARRGGBB colour that is not fully opaque: glass. */
    static boolean isGlass(String colour) {
        if (!colour.startsWith("#") || colour.length() != 9) {
            return false;
        }
        try {
            return Integer.parseInt(colour.substring(1, 3), 16) < 0xFF;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** The colours every pack paints its headlamps and its tail lamps in. */
    private static final java.util.Set<String> HEAD_LAMPS = java.util.Set.of("#F4F0D0", "#FFFDF2");
    private static final java.util.Set<String> TAIL_LAMPS = java.util.Set.of("#C0201C", "#EE4A3C");

    @org.jetbrains.annotations.Nullable
    private static DefinedVehicleModel lamps(VehicleDefinition definition, List<String> colours,
                                             java.util.Set<String> lamp) {
        boolean any = definition.shape().stream().anyMatch(box -> lamp.contains(box.color()));
        return any ? new DefinedVehicleModel(build(definition, colours, false, lamp), definition, false) : null;
    }

    /** Just the headlamps, drawn again lit up after dark; null if it has none. */
    @org.jetbrains.annotations.Nullable
    public static DefinedVehicleModel headLampsOf(ResourceLocation id, VehicleDefinition definition) {
        return built(id, definition).headLamps();
    }

    /** Just the tail lamps, lit after dark and under braking; null if it has none. */
    @org.jetbrains.annotations.Nullable
    public static DefinedVehicleModel tailLampsOf(ResourceLocation id, VehicleDefinition definition) {
        return built(id, definition).tailLamps();
    }

    /** The see-through part of this vehicle, or null if it has none. */
    @org.jetbrains.annotations.Nullable
    public static DefinedVehicleModel glassOf(ResourceLocation id, VehicleDefinition definition) {
        return built(id, definition).glass();
    }

    /**
     * How far in front of the driver's hips the steering wheel goes: its
     * usual reach, or less if the dashboard comes closer than that, so the
     * rim stands just proud of it rather than buried in it.
     */
    private static float wheelReach(VehicleDefinition definition, net.minecraft.world.phys.Vec3 seat) {
        float wheelY = (float) seat.y + wheelRise(definition, seat);
        float reach = WHEEL_REACH;
        // The whole rim, not only its hub: raked back, its top leans forward
        // towards the dash by this much and its bottom away from it.
        float rake = wheelRake(definition) * DEGREES;
        float top = wheelY + (WHEEL_RIM + RIM_THICK) * Mth.cos(rake);
        float bottom = wheelY - (WHEEL_RIM + RIM_THICK) * Mth.cos(rake);
        float lean = (WHEEL_RIM + RIM_THICK) * Mth.sin(rake);
        float span = WHEEL_RIM + 1.0F;
        for (VehicleDefinition.Box box : definition.shape()) {
            if (isGlass(box.color())) {
                continue;
            }
            float minX = Math.min(box.from().x(), box.to().x());
            float maxX = Math.max(box.from().x(), box.to().x());
            float minY = Math.min(box.from().y(), box.to().y());
            float maxY = Math.max(box.from().y(), box.to().y());
            float minZ = Math.min(box.from().z(), box.to().z());
            if (maxX > seat.x - span && minX < seat.x + span && maxY > bottom && minY < top
                    && minZ > seat.z + 2.0) {
                // How far the rim reaches forward at the height this box starts.
                float ahead = maxY > wheelY ? lean * (Math.min(maxY, top) - wheelY) / (top - wheelY) : 0.0F;
                reach = Math.min(reach, minZ - (float) seat.z - 0.8F - Math.max(ahead, 0.4F));
            }
        }
        return Math.max(4.0F, reach);
    }

    /**
     * How far above the driver's hips the middle of the wheel goes: where the
     * vehicle says, or else the usual rise, or more where the dashboard
     * stands taller than that, so the top of the rim still shows over it from
     * the seat. A lorry's dash is high.
     */
    private static float wheelRise(VehicleDefinition definition, net.minecraft.world.phys.Vec3 seat) {
        if (definition.steeringWheelRise().isPresent()) {
            return definition.steeringWheelRise().get();
        }
        float span = WHEEL_RIM + 1.0F;
        float dash = Float.NEGATIVE_INFINITY;
        for (VehicleDefinition.Box box : definition.shape()) {
            // The dash is trim; paint in front of the seat is the bonnet.
            if (isGlass(box.color()) || "paint".equals(box.color())) {
                continue;
            }
            float minX = Math.min(box.from().x(), box.to().x());
            float maxX = Math.max(box.from().x(), box.to().x());
            float maxY = Math.max(box.from().y(), box.to().y());
            float minZ = Math.min(box.from().z(), box.to().z());
            // Just in front of the seat and about chest height: the dash, not the roof.
            if (maxX > seat.x - span && minX < seat.x + span && minZ > seat.z + 2.0 && minZ < seat.z + 16.0
                    && maxY > seat.y && maxY < seat.y + 22.0) {
                dash = Math.max(dash, maxY - (float) seat.y);
            }
        }
        return Math.max(WHEEL_RISE, dash - CLUSTER_OVER_WHEEL);
    }

    /** How far back from upright the wheel leans, in degrees: where the vehicle says, or a car's. */
    private static float wheelRake(VehicleDefinition definition) {
        return definition.steeringWheelRake().orElse(WHEEL_RAKE);
    }

    private static ModelPart build(VehicleDefinition definition, List<String> colours, boolean glass) {
        return build(definition, colours, glass, null);
    }

    /** @param only when given, nothing but the boxes painted in these colours */
    private static ModelPart build(VehicleDefinition definition, List<String> colours, boolean glass,
                                   @org.jetbrains.annotations.Nullable java.util.Set<String> only) {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        PartDefinition chassis = root.addOrReplaceChild(
                "chassis", CubeListBuilder.create(), PartPose.offset(0.0F, BODY_PIVOT_Y, 0.0F));
        // Undoes the pivot, so the boxes below are written where they are.
        PartDefinition frame = chassis.addOrReplaceChild(
                "frame", CubeListBuilder.create(), PartPose.offset(0.0F, -BODY_PIVOT_Y, 0.0F));

        // The fork, bars and headset turn about the steering head: the line
        // from the front wheel's hub up through the highest of them, raked
        // back as the fork is. Three parts so the turn happens about that
        // line: tip onto it, turn, tip back.
        // On anything with a pair of steered wheels, the line runs up the
        // middle between them.
        PartDefinition steerTurn = null;
        float hubX = 0.0F;
        float hubY = 0.0F;
        float hubZ = 0.0F;
        int steered = 0;
        for (VehicleDefinition.Wheel wheel : definition.wheels()) {
            if (wheel.steers()) {
                hubX += wheel.at().x();
                hubY += wheel.at().y();
                hubZ += wheel.at().z();
                steered++;
            }
        }
        VehicleDefinition.Box top = null;
        for (VehicleDefinition.Box box : definition.shape()) {
            if (box.steers() && (top == null
                    || box.from().y() + box.to().y() > top.from().y() + top.to().y())) {
                top = box;
            }
        }
        PartDefinition steerHead = null;
        if (top != null) {
            if (steered > 0) {
                hubX /= steered;
                hubY /= steered;
                hubZ /= steered;
            } else {
                // No wheel to steer, as on a jet ski: the bars turn about an
                // upright line through the middle of them.
                hubX = (top.from().x() + top.to().x()) / 2.0F;
                hubZ = (top.from().z() + top.to().z()) / 2.0F;
            }
            float rise = (top.from().y() + top.to().y()) / 2.0F - hubY;
            float back = (top.from().z() + top.to().z()) / 2.0F - hubZ;
            float rake = (float) Math.atan2(back, Math.max(rise, 0.1F));
            PartDefinition axis = frame.addOrReplaceChild("steer_axis", CubeListBuilder.create(),
                    PartPose.offsetAndRotation(hubX, -hubY, -hubZ, rake, 0.0F, 0.0F));
            steerTurn = axis.addOrReplaceChild("steer", CubeListBuilder.create(), PartPose.ZERO);
            steerHead = steerTurn.addOrReplaceChild("steer_head", CubeListBuilder.create(),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -rake, 0.0F, 0.0F));
        }

        // Anything with a cabin gets a steering wheel in front of the driver,
        // where their hands reach: the seat is where the hips go, so the rim
        // sits a little above it and a forearm and a half forward. A plane
        // flies on the yokes its own boxes draw.
        if (!glass && only == null && !definition.seats().isEmpty() && !definition.handlebars()
                && definition.chassis() != VehicleDefinition.Chassis.MOTORCYCLE
                && definition.chassis() != VehicleDefinition.Chassis.PLANE
                && definition.chassis() != VehicleDefinition.Chassis.HELICOPTER
                && definition.chassis() != VehicleDefinition.Chassis.TRAILER) {
            net.minecraft.world.phys.Vec3 seat = definition.seats().get(0);
            int[] rim = uv(colours, STEERING_WHEEL_COLOUR);
            int[] badge = uv(colours, "#C9CDD2");
            PartDefinition mount = frame.addOrReplaceChild(STEERING_WHEEL, CubeListBuilder.create(),
                    PartPose.offsetAndRotation((float) seat.x, -(float) seat.y - wheelRise(definition, seat),
                            -(float) seat.z - wheelReach(definition, seat), wheelRake(definition) * DEGREES,
                            0.0F, 0.0F));
            int[] spoke = uv(colours, "#3A3A3E");
            int[] shroud = uv(colours, "#2A2A2D");
            mount.addOrReplaceChild("shroud", CubeListBuilder.create().texOffs(shroud[0], shroud[1])
                            .addBox(-0.9F, -0.9F, -3.4F, 1.8F, 1.8F, 1.9F),
                    PartPose.ZERO);
            PartDefinition turn = mount.addOrReplaceChild("turn", CubeListBuilder.create().texOffs(rim[0], rim[1])
                            // the column, into the dash
                            .addBox(-0.45F, -0.45F, -3.5F, 0.9F, 0.9F, 3.2F),
                    PartPose.ZERO);
            // The rim: sixteen short pieces round a circle, each a touch
            // longer than its share so the joins close up.
            float r = WHEEL_RIM;
            float piece = (float) (2.0 * r * Math.tan(Math.PI / RIM_PIECES)) + 0.25F;
            for (int i = 0; i < RIM_PIECES; i++) {
                turn.addOrReplaceChild("rim" + i, CubeListBuilder.create().texOffs(rim[0], rim[1])
                                .addBox(-piece / 2.0F, -r, -0.35F, piece, RIM_THICK, 0.7F),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, i * (360.0F / RIM_PIECES) * DEGREES));
            }
            // Three spokes, left, right and down, a little behind the rim.
            turn.addOrReplaceChild("spokes", CubeListBuilder.create().texOffs(spoke[0], spoke[1])
                            .addBox(-r + 0.4F, -0.35F, -0.15F, r * 2.0F - 0.8F, 0.7F, 0.4F)
                            .addBox(-0.35F, 0.0F, -0.15F, 0.7F, r - 0.4F, 0.4F),
                    PartPose.ZERO);
            // An eight-sided boss, and a bright badge on it so the turn can be seen.
            for (int i = 0; i < 2; i++) {
                turn.addOrReplaceChild("boss" + i, CubeListBuilder.create().texOffs(rim[0], rim[1])
                                .addBox(-0.95F, -0.95F, -0.45F, 1.9F, 1.9F, 0.9F),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, i * 45.0F * DEGREES));
            }
            turn.addOrReplaceChild("badge", CubeListBuilder.create().texOffs(badge[0], badge[1])
                            .addBox(-0.5F, -0.5F, 0.45F, 1.0F, 1.0F, 0.15F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 45.0F * DEGREES));
        }

        // A propeller or a rotor turns about the middle of the first box that
        // spins about its axis: the spinner, or the hub.
        Map<net.minecraft.core.Direction.Axis, PartDefinition> props =
                new java.util.EnumMap<>(net.minecraft.core.Direction.Axis.class);
        Map<net.minecraft.core.Direction.Axis, float[]> pivots =
                new java.util.EnumMap<>(net.minecraft.core.Direction.Axis.class);
        if (!glass && only == null) {
            for (VehicleDefinition.Box box : definition.shape()) {
                if (box.spins() && !props.containsKey(box.spinAxis())) {
                    float[] pivot = {(box.from().x() + box.to().x()) / 2.0F,
                            -(box.from().y() + box.to().y()) / 2.0F,
                            -(box.from().z() + box.to().z()) / 2.0F};
                    pivots.put(box.spinAxis(), pivot);
                    props.put(box.spinAxis(), frame.addOrReplaceChild(spinnerName(box.spinAxis()),
                            CubeListBuilder.create(), PartPose.offset(pivot[0], pivot[1], pivot[2])));
                }
            }
        }

        // The bonnet swings on its back edge by the windscreen, the boot lid
        // on its front edge by the rear window, both along the top. Only the
        // bodywork has them: the glass and the lamps stay where they are.
        Map<String, float[]> hinges = new HashMap<>();
        if (!glass && only == null) {
            for (String lid : List.of(VehicleDefinition.Box.BONNET, VehicleDefinition.Box.BOOT)) {
                net.minecraft.world.phys.AABB closed = definition.lidBounds(lid);
                if (closed != null) {
                    float hingeY = (float) closed.maxY;
                    float hingeZ = (float) (lid.equals(VehicleDefinition.Box.BONNET) ? closed.minZ : closed.maxZ);
                    frame.addOrReplaceChild(lid, CubeListBuilder.create(),
                            PartPose.offset(0.0F, -hingeY, -hingeZ));
                    hinges.put(lid, new float[] {0.0F, -hingeY, -hingeZ});
                }
            }
        }

        int count = 0;
        // Hollowed out under the lids, for the engine and the luggage.
        List<VehicleDefinition.Box> shape = me.trossty.car.vehicle.LidCavity.carve(definition);
        float[] lift = lifts(shape);
        for (int index = 0; index < shape.size(); index++) {
            VehicleDefinition.Box box = shape.get(index);
            if (isGlass(box.color()) != glass || isCargo(box.color())
                    || only != null && !only.contains(box.color())) {
                continue;
            }
            Vector3fc from = box.from();
            Vector3fc to = box.to();
            float x = Math.min(from.x(), to.x());
            float y = Math.min(from.y(), to.y());
            float z = Math.min(from.z(), to.z());
            float sizeX = Math.abs(to.x() - from.x());
            float sizeY = Math.abs(to.y() - from.y());
            float sizeZ = Math.abs(to.z() - from.z());
            int[] at = uv(colours, box.color());
            // Written about its own middle, so that a tilt turns it in place.
            boolean turns = box.steers() && steerHead != null;
            boolean spins = box.spins() && props.containsKey(box.spinAxis());
            float[] pivot = spins ? pivots.get(box.spinAxis()) : null;
            float[] hinge = hinges.get(box.opens());
            PartDefinition parent = hinge != null ? frame.getChild(box.opens())
                    : spins ? props.get(box.spinAxis()) : turns ? steerHead : frame;
            float offX = hinge != null ? hinge[0] : spins ? pivot[0] : turns ? hubX : 0.0F;
            float offY = hinge != null ? hinge[1] : spins ? pivot[1] : turns ? -hubY : 0.0F;
            float offZ = hinge != null ? hinge[2] : spins ? pivot[2] : turns ? -hubZ : 0.0F;
            CubeListBuilder cube = CubeListBuilder.create().texOffs(at[0], at[1]);
            java.util.Set<net.minecraft.core.Direction> pane = glass ? paneFaces(sizeX, sizeY, sizeZ) : null;
            if (pane != null) {
                cube.addBox(-sizeX / 2.0F, -sizeY / 2.0F, -sizeZ / 2.0F, sizeX, sizeY, sizeZ, pane);
            } else {
                cube.addBox(-sizeX / 2.0F, -sizeY / 2.0F, -sizeZ / 2.0F, sizeX, sizeY, sizeZ,
                        new net.minecraft.client.model.geom.builders.CubeDeformation(lift[index]));
            }
            parent.addOrReplaceChild("box" + count++, cube,
                    PartPose.offsetAndRotation(x + sizeX / 2.0F - offX,
                            -(y + sizeY / 2.0F) - offY,
                            -(z + sizeZ / 2.0F) - offZ,
                            -box.tilt() * DEGREES, -box.yaw() * DEGREES, -box.roll() * DEGREES));
        }

        return LayerDefinition.create(data, PALETTE * CELL, PALETTE * CELL).bakeRoot();
    }

    /** Glass this thin is a pane: a sheet with two faces and no edges. */
    private static final float PANE = 0.1F;

    /**
     * A window that is not a rectangle is cut from several thin pieces of
     * glass lying edge to edge in one plane. Drawn as boxes, the edges where
     * they meet show through the glass as lines and the window looks
     * layered; drawn as their two big faces only, they are one pane.
     * Null for glass with real thickness, which keeps all six faces.
     */
    private static java.util.Set<net.minecraft.core.Direction> paneFaces(float sizeX, float sizeY, float sizeZ) {
        float thin = Math.min(sizeX, Math.min(sizeY, sizeZ));
        if (thin > PANE) {
            return null;
        }
        if (thin == sizeX) {
            return java.util.EnumSet.of(net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST);
        }
        if (thin == sizeY) {
            return java.util.EnumSet.of(net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.UP);
        }
        return java.util.EnumSet.of(net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH);
    }

    /** How far a box is lifted off a bigger one it shares a face with: well under a pixel. */
    private static final float LIFT_STEP = 0.05F;

    /**
     * Two boxes of different colours with a face in the same plane fight
     * over every pixel of it, and the winner flickers as the camera moves.
     * The smaller box — the trim, the badge, the light on the body — is
     * pushed out a hair past the bigger one it sits on, one step more for
     * each box it sits on in turn, so it always wins.
     */
    static float[] lifts(List<VehicleDefinition.Box> shape) {
        int n = shape.size();
        float[][] lo = new float[n][3], hi = new float[n][3];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            Vector3fc from = shape.get(i).from(), to = shape.get(i).to();
            lo[i] = new float[] {Math.min(from.x(), to.x()), Math.min(from.y(), to.y()), Math.min(from.z(), to.z())};
            hi[i] = new float[] {Math.max(from.x(), to.x()), Math.max(from.y(), to.y()), Math.max(from.z(), to.z())};
            order[i] = i;
        }
        java.util.function.ToDoubleFunction<Integer> volume = i ->
                (double) (hi[i][0] - lo[i][0]) * (hi[i][1] - lo[i][1]) * (hi[i][2] - lo[i][2]);
        java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(volume).reversed());
        int[] level = new int[n];
        for (int a = 0; a < n; a++) {
            int i = order[a];
            for (int b = 0; b < a; b++) {
                int j = order[b];
                if (level[j] + 1 > level[i] && sharesFace(shape.get(i), shape.get(j), lo[i], hi[i], lo[j], hi[j])) {
                    level[i] = level[j] + 1;
                }
            }
        }
        float[] lift = new float[n];
        for (int i = 0; i < n; i++) {
            lift[i] = level[i] * LIFT_STEP;
        }
        return lift;
    }

    private static boolean sharesFace(VehicleDefinition.Box a, VehicleDefinition.Box b,
                                      float[] aLo, float[] aHi, float[] bLo, float[] bHi) {
        if (a.color().equals(b.color()) || a.tilt() != b.tilt() || a.roll() != b.roll()) {
            return false;
        }
        for (int axis = 0; axis < 3; axis++) {
            boolean lows = Math.abs(aLo[axis] - bLo[axis]) < 1.0E-3F;
            boolean highs = Math.abs(aHi[axis] - bHi[axis]) < 1.0E-3F;
            if (!lows && !highs) {
                continue;
            }
            boolean overlaps = true;
            for (int other = 0; other < 3; other++) {
                if (other != axis && Math.min(aHi[other], bHi[other]) - Math.max(aLo[other], bLo[other]) <= 1.0E-3F) {
                    overlaps = false;
                }
            }
            if (overlaps) {
                return true;
            }
        }
        return false;
    }

    /**
     * The wheels, on their own so that they can wear a real texture. Each is
     * the same wheel, built in layers so it has depth: sixteen slabs crossed into 32 flats for the tread, a
     * ring of slabs for the bulge of the sidewall, and a thick smooth hub
     * standing out of the middle — scaled to the size the definition asks for.
     */
    private static ModelPart buildWheels(VehicleDefinition definition) {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        for (int index = 0; index < definition.wheels().size(); index++) {
            VehicleDefinition.Wheel wheel = definition.wheels().get(index);
            PartDefinition hub = root.addOrReplaceChild("wheel" + index, CubeListBuilder.create(),
                    PartPose.offset(wheel.at().x(), -wheel.at().y(), -wheel.at().z()));
            // The sidewall: a ring of slabs just proud of the face, a little
            // short of the tread, so the tyre has a shoulder and a bulge.
            for (int slab = 0; slab < TREAD_SLABS; slab++) {
                hub.addOrReplaceChild("wall" + slab,
                        CubeListBuilder.create().texOffs(40, 22)
                                .addBox(-WALL_HALF, -WALL_REACH, -WALL_FLAT / 2.0F,
                                        WALL_HALF * 2.0F, WALL_REACH * 2.0F, WALL_FLAT),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (slab + 0.5F) * SLAB_STEP * DEGREES, 0.0F, 0.0F));
            }
            // The hub: a thick grey disc standing out of the tyre. Real depth,
            // so its sides catch the shade. Behind spokes it is painted open.
            int[] face = wheel.rim().open() ? HUB_OPEN_UV : new int[] {0, 34};
            for (int slab = 0; slab < TREAD_SLABS; slab++) {
                hub.addOrReplaceChild("hub" + slab,
                        CubeListBuilder.create().texOffs(face[0], face[1])
                                .addBox(-HUB_HALF, -HUB_REACH, -HUB_FLAT / 2.0F,
                                        HUB_HALF * 2.0F, HUB_REACH * 2.0F, HUB_FLAT),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, slab * SLAB_STEP * DEGREES, 0.0F, 0.0F));
            }
            addRim(hub, wheel.rim());
            for (int slab = 0; slab < TREAD_SLABS; slab++) {
                hub.addOrReplaceChild("tread" + slab,
                        CubeListBuilder.create().texOffs(40, 0)
                                .addBox(-1.95F, -TREAD_REACH, -TREAD_FLAT / 2.0F,
                                        3.9F, TREAD_REACH * 2.0F, TREAD_FLAT),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, slab * SLAB_STEP * DEGREES, 0.0F, 0.0F));
            }
        }
        return LayerDefinition.create(data, 64, 64).bakeRoot();
    }

    /**
     * The face of a rim, on both sides of the hub, each piece named rim0,
     * rim1 and so on: its spokes, its nuts on their ring between the spokes,
     * holes through a steel face, slots round a cover, and a lorry's hub
     * standing out of the middle, eight-sided.
     */
    private static void addRim(PartDefinition hub, VehicleDefinition.Rim rim) {
        int[] count = {0};
        float spokeStep = rim.spokes > 0 ? 360.0F / rim.spokes : 0.0F;
        for (int i = 0; i < rim.spokes; i++) {
            rimPiece(hub, count, RIM_UV, i * spokeStep, 1.0F, 3.75F, rim.spokeWidth, RIM_PROUD);
        }
        for (int i = 0; i < rim.nuts; i++) {
            // between two spokes, where there are spokes
            float angle = (i + (rim.spokes > 0 ? 0.5F : 0.0F)) * 360.0F / rim.nuts;
            rimPiece(hub, count, NUT_UV, angle, 1.13F, 1.6F, 0.47F, RIM_PROUD + (rim.boss ? 0.15F : 0.0F));
        }
        for (int i = 0; i < rim.holes; i++) {
            rimPiece(hub, count, DARK_UV, (i + 0.5F) * 360.0F / rim.holes, 2.2F, 2.95F, 0.75F, HOLE_PROUD);
        }
        for (int i = 0; i < rim.slots; i++) {
            rimPiece(hub, count, DARK_UV, i * 360.0F / rim.slots, 2.25F, 3.2F, 0.4F, HOLE_PROUD);
        }
        if (rim.boss) {
            // an octagon: a bar across the axle, turned four ways an eighth apart
            float reach = 1.05F;
            float flat = (float) (2.0 * reach * Math.tan(Math.PI / 8.0));
            for (int turn = 0; turn < 4; turn++) {
                for (int side : new int[] {1, -1}) {
                    float inner = HUB_HALF - RIM_PROUD;
                    float outer = HUB_HALF + 0.9F;
                    hub.addOrReplaceChild("rim" + count[0]++,
                            CubeListBuilder.create().texOffs(RIM_UV[0], RIM_UV[1])
                                    .addBox(side > 0 ? inner : -outer, -reach, -flat / 2.0F,
                                            outer - inner, reach * 2.0F, flat),
                            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, turn * 45.0F * DEGREES, 0.0F, 0.0F));
                }
            }
        }
    }

    /**
     * One piece of a rim's face on each side of the hub: from {@code from} to
     * {@code to} out from the axle, {@code wide} across, {@code proud} out of
     * the face, turned {@code angle} degrees round the axle.
     */
    private static void rimPiece(PartDefinition hub, int[] count, int[] uv, float angle,
                                 float from, float to, float wide, float proud) {
        float sunk = 0.25F;
        for (int side : new int[] {1, -1}) {
            float inner = HUB_HALF - sunk;
            float outer = HUB_HALF + proud;
            hub.addOrReplaceChild("rim" + count[0]++,
                    CubeListBuilder.create().texOffs(uv[0], uv[1])
                            .addBox(side > 0 ? inner : -outer, -to, -wide / 2.0F, outer - inner, to - from, wide),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, angle * DEGREES, 0.0F, 0.0F));
        }
    }

    @Override
    public void setupAnim(CarRenderState state) {
        // This resets every part, scale included, so the wheels' sizes are
        // set again below; set only once, every wheel was drawn the same size.
        super.setupAnim(state);

        // Taken off on the lift, a tyre is gone from the car; what is left on
        // the hub is the brake disc, and with that off too, nothing.
        // Tyres come off one at a time, so each wheel is asked about its own.
        for (int index = 0; index < this.rubber.size(); index++) {
            this.rubber.get(index).visible = state.tyresOn
                    && (state.tyresOff >> (index / (2 * TREAD_SLABS)) & 1) == 0;
        }
        for (int index = 0; index < this.hubs.size(); index++) {
            this.hubs.get(index).visible = state.discsOn
                    || state.tyresOn && (state.tyresOff >> (index / TREAD_SLABS) & 1) == 0;
        }
        for (int index = 0; index < this.rims.size(); index++) {
            boolean on = state.tyresOn && (state.tyresOff >> index & 1) == 0;
            for (ModelPart piece : this.rims.get(index)) {
                piece.visible = on;
            }
        }

        float roll = state.wheelAngle * DEGREES;
        for (int index = 0; index < this.wheels.size(); index++) {
            ModelPart wheel = this.wheels.get(index);
            float[] scale = this.wheelScales.get(index);
            wheel.xScale = scale[0];
            wheel.yScale = scale[1];
            wheel.zScale = scale[1];
            wheel.xRot = roll;
            if (index == state.flatTyre) {
                // Let down: the rim sinks towards the road and the tyre
                // squats under it, its bottom flat on the ground and its
                // sidewalls bulging out. The wheel is dropped so the road
                // hides what would have been the round of the tread, and it
                // is not rolled round, so the flat stays at the bottom.
                wheel.xScale = scale[0] * FLAT_BULGE;
                wheel.xRot = 0.0F;
                // The model's y runs downward.
                wheel.y += scale[1] * WHEEL_MODEL_RADIUS * FLAT_SINK;
            }
        }
        float steer = state.steerAngle * DEGREES;
        for (ModelPart wheel : this.steered) {
            wheel.yRot = steer;
        }

        if (this.chassis == null) {
            return;
        }
        if (this.steerTurn != null) {
            this.steerTurn.yRot = steer;
        }
        // The model's y runs downward and its z backward, so the bonnet's
        // front edge comes up as it turns one way and the boot's back edge
        // as it turns the other.
        if (this.bonnet != null) {
            this.bonnet.xRot = -CarEntityRenderer.ease(state.bonnetOpen) * me.trossty.car.entity.CarEntity.BONNET_SWING_DEGREES * DEGREES;
        }
        if (this.boot != null) {
            this.boot.xRot = CarEntityRenderer.ease(state.bootOpen) * me.trossty.car.entity.CarEntity.BOOT_SWING_DEGREES * DEGREES;
        }
        float spin = state.engineRunning ? state.ageInTicks * PROP_SPEED % Mth.TWO_PI : 0.0F;
        this.spinners.forEach((axis, part) -> {
            switch (axis) {
                case X -> part.xRot = spin;
                case Y -> part.yRot = spin;
                case Z -> part.zRot = spin;
            }
        });
        if (this.steeringWheel != null) {
            // Turning right swings the top of the rim to the right, which is
            // the model's minus x.
            this.steeringWheel.zRot = -steer * WHEEL_RATIO;
        }
        this.chassis.xRot = Mth.clamp(
                -state.acceleration * PITCH_PER_ACCELERATION, -MAX_PITCH, MAX_PITCH);
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
