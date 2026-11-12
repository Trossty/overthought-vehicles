package me.trossty.car.client.render;

import me.trossty.car.CarMain;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
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
public class DefinedVehicleModel extends EntityModel<CarRenderState> {

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
    private static final String STEERING_WHEEL_COLOUR = "#1E1E21";
    /** From the driver's seat to the middle of the wheel: up, and forwards. */
    private static final float WHEEL_RISE = 6.5F;
    private static final float WHEEL_REACH = 10.0F;
    /** Degrees the wheel leans back towards the driver. */
    private static final float WHEEL_RAKE = 28.0F;
    private static final float WHEEL_RIM = 2.8F;
    /** The rim is this many pieces round, and this thick. */
    private static final int RIM_PIECES = 16;
    private static final float RIM_THICK = 0.6F;
    /** Turns of the wheel per turn of the road wheels. */
    private static final float WHEEL_RATIO = 2.5F;

    /** How many slabs are crossed to make the tread: eight make a sixteen-sided wheel. */
    private static final int TREAD_SLABS = 8;

    /** How long each flat of the tread is, along the rim. */
    private static final float TREAD_FLAT = 3.0F;

    /** From the axle to the middle of a flat, for sixteen flats of that length. */
    private static final float TREAD_REACH = (float) (TREAD_FLAT / 2.0 / Math.tan(Math.PI / 16.0));

    /** The sidewall's bulge: this far out from the middle, either side, and this far round. */
    private static final float WALL_HALF = 2.25F;
    private static final float WALL_REACH = 6.9F;
    private static final float WALL_FLAT = (float) (2.0 * WALL_REACH * Math.tan(Math.PI / 16.0));

    /**
     * The hub: a big sixteen-sided disc a little proud of the sidewall, and a
     * fine saw edge of small teeth round it.
     */
    private static final int HUB_SLABS = 8;
    private static final float HUB_HALF = 2.6F;
    private static final float HUB_REACH = 4.4F;
    private static final float HUB_FLAT = (float) (2.0 * HUB_REACH * Math.tan(Math.PI / 16.0));
    private static final int HUB_TEETH = 24;
    private static final float TOOTH_HEIGHT = 0.45F;
    private static final float TOOTH_WIDTH = 0.55F;

    /** One built vehicle, kept for as long as its definition is the loaded one. */
    private record Built(VehicleDefinition definition, DefinedVehicleModel model,
                         @org.jetbrains.annotations.Nullable DefinedVehicleModel glass,
                         DefinedVehicleModel wheels,
                         List<String> colours, Map<Integer, Identifier> textures) {
    }

    private static final Map<Identifier, Built> CACHE = new HashMap<>();

    private final ModelPart chassis;
    /** The fork and bars, turned with the front wheel; null if nothing steers. */
    private final ModelPart steerTurn;
    /** The steering wheel in the cabin, turned with the road wheels; null if there is none. */
    private final ModelPart steeringWheel;
    private final List<ModelPart> wheels = new ArrayList<>();
    private final List<ModelPart> steered = new ArrayList<>();
    /** Each wheel's size as {width, radius} scales, put back every frame. */
    private final List<float[]> wheelScales = new ArrayList<>();
    /** The rubber of every wheel, and the hubs, so a removed tyre or disc can leave the picture. */
    private final List<ModelPart> rubber = new ArrayList<>();
    private final List<ModelPart> hubs = new ArrayList<>();

    private DefinedVehicleModel(ModelPart root, VehicleDefinition definition, boolean glass) {
        // The bodywork is solid; the glass is its own model, drawn see-through
        // and after everything else, so whoever sits behind it still shows.
        super(root, glass ? net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucent
                : net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull);
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
        for (int index = 0; this.chassis == null && index < definition.wheels().size(); index++) {
            ModelPart wheel = root.getChild("wheel" + index);
            VehicleDefinition.Wheel drawn = definition.wheels().get(index);
            this.wheelScales.add(new float[] {drawn.width() / 4.0F, drawn.radius() / 7.6F});
            this.wheels.add(wheel);
            this.rubber.add(wheel.getChild("face"));
            for (int slab = 0; slab < TREAD_SLABS; slab++) {
                this.rubber.add(wheel.getChild("wall" + slab));
                this.rubber.add(wheel.getChild("tread" + slab));
            }
            for (int slab = 0; slab < HUB_SLABS; slab++) {
                this.hubs.add(wheel.getChild("hub" + slab));
            }
            for (int tooth = 0; tooth < HUB_TEETH; tooth++) {
                this.hubs.add(wheel.getChild("tooth" + tooth));
            }
            if (definition.wheels().get(index).steers()) {
                this.steered.add(wheel);
            }
        }
    }

    /** The model for this vehicle, built now if it has not been seen before. */
    public static DefinedVehicleModel of(Identifier id, VehicleDefinition definition) {
        return built(id, definition).model();
    }

    /** The same vehicle's wheels, which are drawn with a sheet of their own. */
    public static DefinedVehicleModel wheelsOf(Identifier id, VehicleDefinition definition) {
        return built(id, definition).wheels();
    }

    /** The sheet for a kind of tyre: what is fitted is what is seen. */
    public static Identifier wheelTexture(String tyre) {
        return CarMain.id("textures/entity/wheel/" + tyre + ".png");
    }

    /** The palette for this vehicle in this paint, made now if it is the first. */
    public static Identifier textureOf(Identifier id, VehicleDefinition definition, int paint) {
        Built built = built(id, definition);
        return built.textures().computeIfAbsent(paint & 0xFFFFFF, rgb -> {
            NativeImage image = new NativeImage(PALETTE, PALETTE, false);
            for (int index = 0; index < PALETTE * PALETTE; index++) {
                String colour = built.colours().get(Math.min(index, built.colours().size() - 1));
                image.setPixel(index % PALETTE, index / PALETTE,
                        VehicleDefinition.resolve(colour, rgb));
            }
            Identifier texture = CarMain.id("dynamic/vehicle/" + id.getNamespace() + "/"
                    + id.getPath() + "/" + String.format("%06x", rgb));
            Minecraft.getInstance().getTextureManager().register(
                    texture, new DynamicTexture(texture::toString, image));
            return texture;
        });
    }

    private static Built built(Identifier id, VehicleDefinition definition) {
        Built known = CACHE.get(id);
        // Compared by identity: joining another world hands out new definitions
        // under the same ids, and they need not say the same thing.
        if (known != null && known.definition() == definition) {
            return known;
        }
        if (known != null) {
            for (Identifier stale : known.textures().values()) {
                Minecraft.getInstance().getTextureManager().release(stale);
            }
        }
        List<String> colours = new ArrayList<>();
        ModelPart root = build(definition, colours, false);
        boolean anyGlass = definition.shape().stream().anyMatch(box -> isGlass(box.color()));
        Built fresh = new Built(definition, new DefinedVehicleModel(root, definition, false),
                anyGlass ? new DefinedVehicleModel(build(definition, colours, true), definition, true) : null,
                new DefinedVehicleModel(buildWheels(definition), definition, false),
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

    /** The see-through part of this vehicle, or null if it has none. */
    @org.jetbrains.annotations.Nullable
    public static DefinedVehicleModel glassOf(Identifier id, VehicleDefinition definition) {
        return built(id, definition).glass();
    }

    /**
     * How far in front of the driver's hips the steering wheel goes: its
     * usual reach, or less if the dashboard comes closer than that, so the
     * rim stands just proud of it rather than buried in it.
     */
    private static float wheelReach(VehicleDefinition definition, net.minecraft.world.phys.Vec3 seat) {
        float wheelY = (float) seat.y + WHEEL_RISE;
        float reach = WHEEL_REACH;
        for (VehicleDefinition.Box box : definition.shape()) {
            if (isGlass(box.color())) {
                continue;
            }
            float minX = Math.min(box.from().x(), box.to().x());
            float maxX = Math.max(box.from().x(), box.to().x());
            float minY = Math.min(box.from().y(), box.to().y());
            float maxY = Math.max(box.from().y(), box.to().y());
            float minZ = Math.min(box.from().z(), box.to().z());
            if (maxX > seat.x - 2.5 && minX < seat.x + 2.5 && maxY > wheelY - 2.0F && minY < wheelY + 2.0F
                    && minZ > seat.z + 2.0) {
                reach = Math.min(reach, minZ - (float) seat.z - 1.2F);
            }
        }
        return Math.max(4.0F, reach);
    }

    private static ModelPart build(VehicleDefinition definition, List<String> colours, boolean glass) {
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
        PartDefinition steerTurn = null;
        float hubX = 0.0F;
        float hubY = 0.0F;
        float hubZ = 0.0F;
        VehicleDefinition.Wheel front = null;
        for (VehicleDefinition.Wheel wheel : definition.wheels()) {
            if (wheel.steers() && front == null) {
                front = wheel;
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
        if (front != null && top != null) {
            hubX = front.at().x();
            hubY = front.at().y();
            hubZ = front.at().z();
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
        // sits a little above it and a forearm and a half forward.
        if (!glass && !definition.seats().isEmpty()
                && definition.chassis() != VehicleDefinition.Chassis.MOTORCYCLE
                && definition.chassis() != VehicleDefinition.Chassis.TRAILER) {
            net.minecraft.world.phys.Vec3 seat = definition.seats().get(0);
            int[] rim = uv(colours, STEERING_WHEEL_COLOUR);
            int[] badge = uv(colours, "#C9CDD2");
            PartDefinition mount = frame.addOrReplaceChild(STEERING_WHEEL, CubeListBuilder.create(),
                    PartPose.offsetAndRotation((float) seat.x, -(float) seat.y - WHEEL_RISE,
                            -(float) seat.z - wheelReach(definition, seat), WHEEL_RAKE * DEGREES, 0.0F, 0.0F));
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

        int count = 0;
        for (VehicleDefinition.Box box : definition.shape()) {
            if (isGlass(box.color()) != glass || isCargo(box.color())) {
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
            (turns ? steerHead : frame).addOrReplaceChild("box" + count++,
                    CubeListBuilder.create().texOffs(at[0], at[1])
                            .addBox(-sizeX / 2.0F, -sizeY / 2.0F, -sizeZ / 2.0F, sizeX, sizeY, sizeZ),
                    PartPose.offsetAndRotation(x + sizeX / 2.0F - (turns ? hubX : 0.0F),
                            -(y + sizeY / 2.0F) + (turns ? hubY : 0.0F),
                            -(z + sizeZ / 2.0F) + (turns ? hubZ : 0.0F),
                            -box.tilt() * DEGREES, 0.0F, 0.0F));
        }

        return LayerDefinition.create(data, PALETTE * CELL, PALETTE * CELL).bakeRoot();
    }

    /**
     * The wheels, on their own so that they can wear a real texture. Each is
     * the same wheel, built in layers so it has depth: a round painted face
     * either side, eight slabs crossed into sixteen flats for the tread, a
     * ring of slabs for the bulge of the sidewall, and a thick toothed hub
     * standing out of the middle — scaled to the size the definition asks for.
     */
    private static ModelPart buildWheels(VehicleDefinition definition) {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        for (int index = 0; index < definition.wheels().size(); index++) {
            VehicleDefinition.Wheel wheel = definition.wheels().get(index);
            PartDefinition hub = root.addOrReplaceChild("wheel" + index, CubeListBuilder.create(),
                    PartPose.offset(wheel.at().x(), -wheel.at().y(), -wheel.at().z()));
            hub.addOrReplaceChild("face",
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-2.0F, -8.0F, -8.0F, 4.0F, 16.0F, 16.0F,
                                    java.util.Set.of(net.minecraft.core.Direction.EAST,
                                            net.minecraft.core.Direction.WEST)),
                    PartPose.ZERO);
            // The sidewall: a ring of slabs just proud of the face, a little
            // short of the tread, so the tyre has a shoulder and a bulge.
            for (int slab = 0; slab < TREAD_SLABS; slab++) {
                hub.addOrReplaceChild("wall" + slab,
                        CubeListBuilder.create().texOffs(40, 22)
                                .addBox(-WALL_HALF, -WALL_REACH, -WALL_FLAT / 2.0F,
                                        WALL_HALF * 2.0F, WALL_REACH * 2.0F, WALL_FLAT),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (slab * 22.5F + 11.25F) * DEGREES, 0.0F, 0.0F));
            }
            // The hub: a thick grey disc standing out of the tyre, with a gear's
            // teeth round its edge. Real depth, so its sides catch the shade.
            for (int slab = 0; slab < HUB_SLABS; slab++) {
                hub.addOrReplaceChild("hub" + slab,
                        CubeListBuilder.create().texOffs(0, 34)
                                .addBox(-HUB_HALF, -HUB_REACH, -HUB_FLAT / 2.0F,
                                        HUB_HALF * 2.0F, HUB_REACH * 2.0F, HUB_FLAT),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, slab * (180.0F / HUB_SLABS) * DEGREES, 0.0F, 0.0F));
            }
            for (int tooth = 0; tooth < HUB_TEETH; tooth++) {
                hub.addOrReplaceChild("tooth" + tooth,
                        CubeListBuilder.create().texOffs(20, 34)
                                .addBox(-HUB_HALF, -HUB_REACH - TOOTH_HEIGHT, -TOOTH_WIDTH / 2.0F,
                                        HUB_HALF * 2.0F, TOOTH_HEIGHT + 0.3F, TOOTH_WIDTH),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                                (tooth + 0.5F) * (360.0F / HUB_TEETH) * DEGREES, 0.0F, 0.0F));
            }
            for (int slab = 0; slab < TREAD_SLABS; slab++) {
                hub.addOrReplaceChild("tread" + slab,
                        CubeListBuilder.create().texOffs(40, 0)
                                .addBox(-1.95F, -TREAD_REACH, -TREAD_FLAT / 2.0F,
                                        3.9F, TREAD_REACH * 2.0F, TREAD_FLAT),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, slab * 22.5F * DEGREES, 0.0F, 0.0F));
            }
        }
        return LayerDefinition.create(data, 64, 64).bakeRoot();
    }

    @Override
    public void setupAnim(CarRenderState state) {
        // This resets every part, scale included, so the wheels' sizes are
        // set again below; set only once, every wheel was drawn the same size.
        super.setupAnim(state);

        // Taken off on the lift, a tyre is gone from the car; what is left on
        // the hub is the brake disc, and with that off too, nothing.
        for (ModelPart part : this.rubber) {
            part.visible = state.tyresOn;
        }
        for (ModelPart part : this.hubs) {
            part.visible = state.tyresOn || state.discsOn;
        }

        float roll = state.wheelAngle * DEGREES;
        for (int index = 0; index < this.wheels.size(); index++) {
            ModelPart wheel = this.wheels.get(index);
            float[] scale = this.wheelScales.get(index);
            wheel.xScale = scale[0];
            wheel.yScale = scale[1];
            wheel.zScale = scale[1];
            wheel.xRot = roll;
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
