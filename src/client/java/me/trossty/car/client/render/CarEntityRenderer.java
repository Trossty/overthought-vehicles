package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.trossty.car.CarMain;
import me.trossty.car.entity.CarEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class CarEntityRenderer extends EntityRenderer<CarEntity> {

    /**
     * One texture per dye colour, indexed by {@code DyeColor.getIndex()}. The car
     * is painted by swapping the texture rather than tinting the model, because a
     * tint would colour the tyres, glass and lights along with the bodywork.
     */
    private static final ResourceLocation[] TEXTURES = buildTextures();

    private static ResourceLocation[] buildTextures() {
        DyeColor[] colors = DyeColor.values();
        ResourceLocation[] textures = new ResourceLocation[colors.length];
        for (DyeColor color : colors) {
            textures[color.getId()] = CarMain.id("textures/entity/car/car_" + color.getName() + ".png");
        }
        return textures;
    }

    private final VehicleModel model;

    /** Speed, in blocks a tick, at which a two-wheeler leans all the way. */
    private static final float LEAN_FULL_SPEED = 0.4F;
    /** Degrees of lean per degree of steering, at full lean. */
    private static final float LEAN_PER_STEER = 0.8F;
    /** Height of the saddle the lean swings about, in blocks. */
    public static final float LEAN_PIVOT = 0.85F;
    /** Degrees a parked bike leans over onto its side stand. */
    private static final float STAND_TILT = 9.0F;
    /** Degrees an unhitched trailer tips forward onto its jockey wheel. */
    private static final float NOSE_DROP = 6.5F;

    /**
     * How far a two-wheeler leans, in degrees about its saddle: into the turn,
     * more the faster it goes. Shared with whoever rides it, so they lean too.
     */
    public static float leanDegrees(float steerAngle, float speed) {
        float pace = Mth.clamp(speed / LEAN_FULL_SPEED, 0.0F, 1.0F);
        return -steerAngle * LEAN_PER_STEER * pace;
    }

    public CarEntityRenderer(EntityRendererProvider.Context context) {
        this(context, ModEntityModelLayers.CAR);
    }

    /** Subclasses differ only in which baked model they pick up. */
    protected CarEntityRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context);
        this.model = new CarEntityModel(context.bakeLayer(layer));
        // The round shadow vanilla draws is lost under a car; it gets its own.
        this.shadowRadius = 0.0F;
    }

    /** Smoothstep: the pose settles rather than stopping dead. */
    private static float ease(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    /** A fresh state to fill; a subclass with more to draw returns its own kind. */
    public CarRenderState createRenderState() {
        return new CarRenderState();
    }

    /** Everything the frame needs from the car, read once. */
    public void extractRenderState(CarEntity car, CarRenderState state, float tickProgress) {
        state.ageInTicks = car.tickCount + tickProgress;
        state.yaw = car.getViewYRot(tickProgress);
        state.color = car.getColor();
        state.paint = car.getPaint();
        state.vehicle = car.getDefinition();
        state.vehicleId = state.vehicle == null ? null : car.getModelId();
        state.tyre = me.trossty.car.entity.CarSetup.tyre(car.getSetup()).key();
        state.tyresOn = car.isFitted(me.trossty.car.entity.CarPart.WHEELS);
        state.discsOn = car.isFitted(me.trossty.car.entity.CarPart.BRAKE_DISCS);
        state.tyresOff = car.tyresOff();
        state.flatTyre = car.getFlatTyre();
        state.labels = CarServiceOverlay.labelsFor(car, tickProgress);
        state.wheelAngle = car.getWheelAngle(tickProgress);
        state.steerAngle = car.getSteerAngle(tickProgress);
        state.speed = car.getEngineSpeed();
        state.acceleration = car.getAcceleration(tickProgress);
        state.engineRunning = car.isEngineRunning();
        state.headlights = lampsOn(car);
        state.braking = car.areBrakeLampsOn();
        state.restPose = car.getRestPose(tickProgress);
        state.damageWobbleTicks = car.getHurtTime() - tickProgress;
        state.damageWobbleSide = car.getHurtDir();
        state.damageWobbleStrength = Math.max(car.getDamage() - tickProgress, 0.0F);
    }

    /**
     * Anything else drawn on the body, in the body's own frame (model space,
     * already turned and flipped): nothing for most vehicles.
     */
    protected void submitExtras(CarRenderState state, PoseStack matrices, MultiBufferSource buffers) {
    }

    /**
     * What rides on a flat bed, drawn in the bed's own frame (turned, and
     * tipped with it when it rests on its jockey wheel): a vehicle by its
     * own renderer, an engine as the part.
     */
    private static void drawCargo(me.trossty.car.entity.TrailerEntity trailer, float yaw, float tickProgress,
                                  PoseStack matrices, MultiBufferSource buffers, int light) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        float deck = trailer.deckHeight();
        net.minecraft.world.entity.Entity cargo = trailer.cargoView();
        matrices.pushPose();
        matrices.translate(0.0F, deck, 0.0F);
        if (cargo != null) {
            // Stand the copy where the bed is, so anything it looks up about
            // its place (its light, its shadow) is the bed's.
            cargo.setPos(trailer.getX(), trailer.getY() + deck, trailer.getZ());
            cargo.xo = cargo.xOld = cargo.getX();
            cargo.yo = cargo.yOld = cargo.getY();
            cargo.zo = cargo.zOld = cargo.getZ();
            cargo.setYRot(yaw);
            cargo.yRotO = yaw;
            if (trailer.isDolly() && cargo instanceof CarEntity towed) {
                // Front wheels in the dolly's trays, back ones on the road
                // behind: the car leans nose up about its front tyres.
                float[] axles = axles(towed);
                float wheelbase = axles[0] - axles[1];
                float lean = (float) Math.toDegrees(Math.asin(Mth.clamp(deck / wheelbase, 0.0F, 1.0F)));
                // Forward is -z in this frame; a turn about +x lifts it.
                matrices.mulPose(Axis.XP.rotationDegrees(lean));
                matrices.translate(0.0F, 0.0F, axles[0]);
            }
            // Its renderer turns it to the yaw itself; the bed is already turned.
            matrices.mulPose(Axis.YP.rotationDegrees(-(180.0F - yaw)));
            client.getEntityRenderDispatcher().render(cargo, 0.0, 0.0, 0.0, yaw, tickProgress,
                    matrices, buffers, light);
        } else {
            matrices.translate(0.0F, 0.42F, 0.0F);
            matrices.scale(1.3F, 1.3F, 1.3F);
            client.getItemRenderer().renderStatic(trailer.getCargoItem(),
                    net.minecraft.world.item.ItemDisplayContext.FIXED, light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, matrices, buffers,
                    trailer.level(), 0);
        }
        matrices.popPose();
    }

    /** How much floor each thing in an open bed gets, in blocks. */
    private static final float LOAD_CELL = 0.42F;

    /**
     * What is in an open bed or box, sitting in it: one of each thing it
     * holds, in rows from the front of the bed back, a block as a small
     * block and anything flat lying down. A full bed is stacked a layer
     * higher where the sides are tall enough.
     */
    private static void drawLoad(CarEntity car, me.trossty.car.vehicle.VehicleDefinition.Area area,
                                 PoseStack matrices, MultiBufferSource buffers, int light) {
        java.util.List<net.minecraft.world.item.ItemStack> load = car.shownLoad();
        if (load.isEmpty()) {
            return;
        }
        float minX = Math.min(area.from().x(), area.to().x()) / 16.0F;
        float maxX = Math.max(area.from().x(), area.to().x()) / 16.0F;
        float minY = Math.min(area.from().y(), area.to().y()) / 16.0F;
        float maxY = Math.max(area.from().y(), area.to().y()) / 16.0F;
        float minZ = Math.min(area.from().z(), area.to().z()) / 16.0F;
        float maxZ = Math.max(area.from().z(), area.to().z()) / 16.0F;
        int cols = Math.max(1, (int) ((maxX - minX) / LOAD_CELL));
        int rows = Math.max(1, (int) ((maxZ - minZ) / LOAD_CELL));
        int layers = Math.max(1, (int) ((maxY - minY) / LOAD_CELL));
        float stepX = (maxX - minX) / cols;
        float stepZ = (maxZ - minZ) / rows;
        net.minecraft.client.renderer.entity.ItemRenderer items =
                net.minecraft.client.Minecraft.getInstance().getItemRenderer();
        int shown = Math.min(load.size(), cols * rows * layers);
        for (int i = 0; i < shown; i++) {
            net.minecraft.world.item.ItemStack stack = load.get(i);
            int layer = i / (cols * rows);
            int cell = i % (cols * rows);
            float x = minX + (cell % cols + 0.5F) * stepX;
            float z = maxZ - (cell / cols + 0.5F) * stepZ;
            float y = minY + layer * LOAD_CELL;
            matrices.pushPose();
            // The model's x and z run the other way to this frame's: its
            // front is +z, this frame's is -z.
            matrices.translate(-x, y, -z);
            // Not lined up like a shop shelf: each a little askew.
            matrices.mulPose(Axis.YP.rotationDegrees(((i * 37) % 7 - 3) * 6.0F));
            boolean block = items.getModel(stack, car.level(), null, 0).isGui3d();
            if (block) {
                // A block in a frame is a half-block cube about its middle.
                matrices.translate(0.0F, 0.18F, 0.0F);
                matrices.scale(0.72F, 0.72F, 0.72F);
            } else {
                matrices.translate(0.0F, 0.02F, 0.0F);
                matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
                matrices.scale(0.4F, 0.4F, 0.4F);
            }
            items.renderStatic(stack, net.minecraft.world.item.ItemDisplayContext.FIXED, light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, matrices, buffers,
                    car.level(), car.getId() * 31 + i);
            matrices.popPose();
        }
    }

    /**
     * How far ahead of its middle a car's front axle is, and its back axle,
     * in blocks (the back one negative): from its steering wheels and the
     * rest, or a family car's when it gives none.
     */
    private static float[] axles(CarEntity car) {
        me.trossty.car.vehicle.VehicleDefinition defined = car.getDefinition();
        float front = 0.0F;
        float back = 0.0F;
        int fronts = 0;
        int backs = 0;
        if (defined != null) {
            for (me.trossty.car.vehicle.VehicleDefinition.Wheel wheel : defined.wheels()) {
                if (wheel.steers()) {
                    front += wheel.at().z();
                    fronts++;
                } else {
                    back += wheel.at().z();
                    backs++;
                }
            }
        }
        if (fronts == 0 || backs == 0) {
            return new float[] {1.6F, -1.6F};
        }
        return new float[] {front / fronts / 16.0F, back / backs / 16.0F};
    }

    @Override
    public ResourceLocation getTextureLocation(CarEntity car) {
        return TEXTURES[car.getColor().getId()];
    }

    @Override
    public void render(CarEntity car, float entityYaw, float tickProgress, PoseStack matrices,
                       MultiBufferSource buffers, int light) {
        CarRenderState state = this.createRenderState();
        this.extractRenderState(car, state, tickProgress);
        state.lightCoords = light;
        this.drawShadow(car, state, matrices, buffers);
        if (state.headlights) {
            this.drawBeam(car, state, matrices, buffers);
            if (state.braking) {
                this.drawBrakeGlow(car, state, matrices, buffers);
            }
        }
        matrices.pushPose();
        matrices.translate(0.0F, car.getStepOffset(tickProgress), 0.0F);
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));

        // Two wheels lean into the turn, more the faster they go, about the
        // line where the tyres touch the road.
        if (state.vehicle != null
                && state.vehicle.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.MOTORCYCLE) {
            // The rider is drawn upright, so the bike swings about the saddle
            // rather than the tyres: the seat stays under them and the
            // wheels go out wide, which reads as the lean.
            matrices.translate(0.0F, LEAN_PIVOT, 0.0F);
            matrices.mulPose(Axis.ZP.rotationDegrees(leanDegrees(state.steerAngle, state.speed)));
            matrices.translate(0.0F, -LEAN_PIVOT, 0.0F);
            // Parked, it rests over on its stand, pivoting where the tyres
            // meet the ground so they stay on it.
            if (state.restPose > 0.001F) {
                matrices.mulPose(Axis.ZP.rotationDegrees(STAND_TILT * ease(state.restPose)));
            }
        }

        // An aircraft noses up into a climb and banks into a turn, about its middle.
        float pitch = car.getBodyPitch(tickProgress);
        float roll = car.getBodyRoll(tickProgress);
        if (pitch != 0.0F || roll != 0.0F) {
            float middle = car.getBbHeight() / 2.0F;
            matrices.translate(0.0F, middle, 0.0F);
            matrices.mulPose(Axis.XP.rotationDegrees(pitch));
            // Banked right is the right wing down: the right side is -x here.
            matrices.mulPose(Axis.ZP.rotationDegrees(-roll));
            matrices.translate(0.0F, -middle, 0.0F);
        }

        // An unhitched trailer drops its nose onto the jockey wheel, tipping
        // about its axle.
        if (state.vehicle != null && state.restPose > 0.001F
                && state.vehicle.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.TRAILER
                && !state.vehicle.wheels().isEmpty()) {
            me.trossty.car.vehicle.VehicleDefinition.Wheel axle = state.vehicle.wheels().get(0);
            float axleY = axle.at().y() / 16.0F;
            float axleZ = -axle.at().z() / 16.0F;
            matrices.translate(0.0F, axleY, axleZ);
            matrices.mulPose(Axis.XP.rotationDegrees(-NOSE_DROP * ease(state.restPose)));
            matrices.translate(0.0F, -axleY, -axleZ);
        }

        if (car instanceof me.trossty.car.entity.TrailerEntity trailer && trailer.hasCargo()) {
            drawCargo(trailer, state.yaw, tickProgress, matrices, buffers, state.lightCoords);
        }
        if (state.vehicle != null && state.vehicle.load().isPresent()) {
            drawLoad(car, state.vehicle.load().get(), matrices, buffers, state.lightCoords);
        }

        if (state.damageWobbleTicks > 0.0F) {
            matrices.mulPose(Axis.XP.rotationDegrees(
                    Mth.sin(state.damageWobbleTicks)
                            * state.damageWobbleTicks
                            * state.damageWobbleStrength / 10.0F
                            * state.damageWobbleSide));
        }

        // A vehicle a data pack describes brings its own shape and palette;
        // everything else here — the turn, the wobble, the light — is shared.
        VehicleModel model = this.model;
        ResourceLocation texture = TEXTURES[state.color.getId()];
        if (state.vehicle != null && state.vehicleId != null) {
            model = DefinedVehicleModel.of(state.vehicleId, state.vehicle);
            texture = DefinedVehicleModel.textureOf(state.vehicleId, state.vehicle,
                    state.paint >= 0 ? state.paint : state.color.getTextureDiffuseColor());
        }

        matrices.scale(-1.0F, -1.0F, 1.0F);
        draw(model, state, matrices, buffers, model.renderType(texture));
        if (state.vehicle != null && state.vehicleId != null) {
            // The glass goes in a later batch: drawn before the people inside,
            // it would hide them from anyone looking in.
            VehicleModel glass = DefinedVehicleModel.glassOf(state.vehicleId, state.vehicle);
            if (glass != null) {
                draw(glass, state, matrices, buffers, glass.renderType(texture));
            }
            // The lamps drawn over themselves at full brightness: lit.
            VehicleModel head = DefinedVehicleModel.headLampsOf(state.vehicleId, state.vehicle);
            if (head != null && state.headlights) {
                drawLit(head, state, matrices, buffers, head.renderType(texture));
            }
            VehicleModel tail = DefinedVehicleModel.tailLampsOf(state.vehicleId, state.vehicle);
            if (tail != null && state.braking) {
                drawLit(tail, state, matrices, buffers, tail.renderType(texture));
            } else if (tail != null && state.headlights) {
                // Only the running lights: lit, but nowhere near as bright as
                // the brakes, or a driver behind could not tell the two apart.
                drawLit(tail, state, matrices, buffers, tail.renderType(texture), RUNNING_LIGHT);
            }
            // Full brightness is no change at all by day, so the brakes add a
            // glow on top: bright red against a bright sky, brighter still at night.
            if (tail != null && state.braking) {
                drawLit(tail, state, matrices, buffers, net.minecraft.client.renderer.RenderType.eyes(texture));
            }
        }
        this.submitExtras(state, matrices, buffers);
        if (state.vehicle != null && state.vehicleId != null) {
            VehicleModel wheels = DefinedVehicleModel.wheelsOf(state.vehicleId, state.vehicle);
            draw(wheels, state, matrices, buffers, wheels.renderType(DefinedVehicleModel.wheelTexture(state.tyre)));
        }
        matrices.popPose();

        this.submitPlates(state, matrices, buffers);
        super.render(car, entityYaw, tickProgress, matrices, buffers, light);
    }

    private static final net.minecraft.client.renderer.RenderType SHADOW = net.minecraft.client.renderer.RenderType
            .entityShadow(ResourceLocation.withDefaultNamespace("textures/misc/shadow.png"));
    /** How much past the body the soft edge of the shadow reaches. */
    private static final float SHADOW_SPREAD = 1.15F;
    private static final float SHADOW_ALPHA = 0.45F;
    /** Over this many blocks of air the shadow has faded out. */
    private static final double SHADOW_FADE = 3.0;
    private static final java.util.Map<me.trossty.car.vehicle.VehicleDefinition, float[]> FOOTPRINTS =
            new java.util.IdentityHashMap<>();

    /**
     * The body's outline seen from above, in blocks: {min x, max x, min z,
     * max z} in the car's own frame, front along +z.
     */
    private static float[] footprint(me.trossty.car.vehicle.VehicleDefinition vehicle) {
        return FOOTPRINTS.computeIfAbsent(vehicle, v -> {
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (me.trossty.car.vehicle.VehicleDefinition.Box box : v.shape()) {
                minX = Math.min(minX, Math.min(box.from().x(), box.to().x()));
                maxX = Math.max(maxX, Math.max(box.from().x(), box.to().x()));
                minZ = Math.min(minZ, Math.min(box.from().z(), box.to().z()));
                maxZ = Math.max(maxZ, Math.max(box.from().z(), box.to().z()));
            }
            return new float[] {minX / 16.0F, maxX / 16.0F, minZ / 16.0F, maxZ / 16.0F};
        });
    }

    /**
     * A soft shadow the shape of the car, laid on the ground under it and
     * turned with it, fading as the car leaves the ground.
     */
    private void drawShadow(CarEntity car, CarRenderState state, PoseStack matrices, MultiBufferSource buffers) {
        if (!net.minecraft.client.Minecraft.getInstance().options.entityShadows().get() || car.isInvisible()) {
            return;
        }
        float[] at = state.vehicle == null ? new float[] {-1.0F, 1.0F, -2.2F, 2.2F} : footprint(state.vehicle);
        double ground = groundBelow(car);
        float alpha = SHADOW_ALPHA * (float) Mth.clamp(1.0 - (car.getY() - ground) / SHADOW_FADE, 0.0, 1.0);
        if (alpha <= 0.01F) {
            return;
        }
        float midX = (at[0] + at[1]) / 2.0F, midZ = (at[2] + at[3]) / 2.0F;
        float halfX = (at[1] - at[0]) / 2.0F * SHADOW_SPREAD, halfZ = (at[3] - at[2]) / 2.0F * SHADOW_SPREAD;
        matrices.pushPose();
        matrices.translate(0.0, ground - car.getY() + 0.015, 0.0);
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        // The model is drawn mirrored in x and z, so is its outline.
        PoseStack.Pose pose = matrices.last();
        com.mojang.blaze3d.vertex.VertexConsumer out = buffers.getBuffer(SHADOW);
        int colour = net.minecraft.util.FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);
        float x0 = -(midX + halfX), x1 = -(midX - halfX), z0 = -(midZ + halfZ), z1 = -(midZ - halfZ);
        shadowVertex(pose, out, colour, x0, z0, 0.0F, 0.0F);
        shadowVertex(pose, out, colour, x0, z1, 0.0F, 1.0F);
        shadowVertex(pose, out, colour, x1, z1, 1.0F, 1.0F);
        shadowVertex(pose, out, colour, x1, z0, 1.0F, 0.0F);
        matrices.popPose();
    }

    private static void shadowVertex(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer out, int colour,
                                     float x, float z, float u, float v) {
        org.joml.Vector3f at = pose.pose().transformPosition(x, 0.0F, z, new org.joml.Vector3f());
        out.addVertex(at.x(), at.y(), at.z(), colour, u, v, OverlayTexture.NO_OVERLAY,
                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
    }

    /** Whether this car's head lamps are on: somebody at the wheel, fuel, and dark enough to want them. */
    public static boolean lampsOn(CarEntity car) {
        return car.getControllingPassenger() != null && car.hasFuel() && lightAt(car) < LIGHTS_ON_BELOW;
    }

    /** Below this much light at the car (0 to 15), the lights come on by themselves. */
    private static final int LIGHTS_ON_BELOW = 8;
    private static final net.minecraft.client.renderer.RenderType BEAM = net.minecraft.client.renderer.RenderType
            .eyes(ResourceLocation.fromNamespaceAndPath(me.trossty.car.CarMain.MOD_ID, "textures/misc/headlight_beam.png"));

    /**
     * The headlamps' light on the road ahead: a pool laid on the ground,
     * widening away from the bumper, added to whatever is drawn there. It
     * is only a picture of light, so it lights nothing else.
     */
    private void drawBeam(CarEntity car, CarRenderState state, PoseStack matrices, MultiBufferSource buffers) {
        float[] at = state.vehicle == null ? new float[] {-1.0F, 1.0F, -2.2F, 2.2F} : footprint(state.vehicle);
        double ground = groundBelow(car);
        if (car.getY() - ground > SHADOW_FADE) {
            return;
        }
        me.trossty.car.vehicle.VehicleDefinition.Lights lights = state.vehicle == null
                ? me.trossty.car.vehicle.VehicleDefinition.Lights.PLAIN : state.vehicle.lights();
        float half = (at[1] - at[0]) / 2.0F * 0.8F;
        float near = at[3] - 0.3F;
        float far = at[3] + lights.length();
        // Laid over the ground in strips, each at the height of the road under
        // it, so the pool runs up a hill or down a dip instead of cutting into
        // it. A step taller than the car can climb is a wall: the light stops.
        float yawRad = state.yaw * ((float) Math.PI / 180.0F);
        double dirX = -Mth.sin(yawRad);
        double dirZ = Mth.cos(yawRad);
        float[] heights = new float[BEAM_STRIPS + 1];
        int strips = 0;
        double last = ground;
        for (int i = 0; i <= BEAM_STRIPS; i++) {
            float d = Mth.lerp((float) i / BEAM_STRIPS, near, far);
            Double here = groundAt(car.level(), car.getX() + dirX * d, car.getZ() + dirZ * d, last);
            if (here == null) {
                break;
            }
            heights[i] = (float) (here - car.getY());
            last = here;
            strips = i;
        }
        if (strips == 0) {
            return;
        }
        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        PoseStack.Pose pose = matrices.last();
        com.mojang.blaze3d.vertex.VertexConsumer out = buffers.getBuffer(BEAM);
        int colour = 0xFFFFFFFF;
        float spread = lights.spread();
        for (int i = 0; i < strips; i++) {
            float t0 = (float) i / BEAM_STRIPS;
            float t1 = (float) (i + 1) / BEAM_STRIPS;
            float z0 = Mth.lerp(t0, near, far);
            float z1 = Mth.lerp(t1, near, far);
            float w0 = half + spread * t0;
            float w1 = half + spread * t1;
            float y0 = heights[i] + 0.02F;
            float y1 = heights[i + 1] + 0.02F;
            // Mirrored like the model: the car's front is -z here.
            // Wound to face up, as the shadow is, or it is culled from above.
            beamVertex(pose, out, colour, -w1, y1, -z1, 0.0F, t1);
            beamVertex(pose, out, colour, -w0, y0, -z0, 0.0F, t0);
            beamVertex(pose, out, colour, w0, y0, -z0, 1.0F, t0);
            beamVertex(pose, out, colour, w1, y1, -z1, 1.0F, t1);
        }
        matrices.popPose();
    }

    /** How many strips the beam is laid in, to follow the ground. */
    private static final int BEAM_STRIPS = 12;
    /** The tallest rise in the road the light carries on up, in blocks. */
    private static final double BEAM_CLIMB = 1.1;

    /**
     * The top of the ground at this spot, looked for from a little above the
     * last height down to a few blocks below it; null when a wall stands there
     * or the ground falls away out of reach.
     */
    private static Double groundAt(net.minecraft.world.level.Level level, double x, double z, double from) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos(
                Mth.floor(x), Mth.floor(from + BEAM_CLIMB), Mth.floor(z));
        for (int i = 0; i < 6; i++) {
            net.minecraft.world.phys.shapes.VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                double top = pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                // Solid where the light would have to come through: a wall.
                return top > from + BEAM_CLIMB ? null : top;
            }
            pos.move(net.minecraft.core.Direction.DOWN);
        }
        return null;
    }

    private static void beamVertex(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer out, int colour,
                                   float x, float y, float z, float u, float v) {
        org.joml.Vector3f at = pose.pose().transformPosition(x, y, z, new org.joml.Vector3f());
        out.addVertex(at.x(), at.y(), at.z(), colour, u, v, OverlayTexture.NO_OVERLAY,
                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
    }

    /** How far behind the bumper the brake lamps redden the road, in blocks. */
    private static final float BRAKE_GLOW_LENGTH = 2.5F;
    private static final int BRAKE_GLOW = 0xFFD02818;

    /** The brake lamps' red on the road behind, the way the beam is drawn in front. */
    private void drawBrakeGlow(CarEntity car, CarRenderState state, PoseStack matrices, MultiBufferSource buffers) {
        float[] at = state.vehicle == null ? new float[] {-1.0F, 1.0F, -2.2F, 2.2F} : footprint(state.vehicle);
        double ground = groundBelow(car);
        if (car.getY() - ground > SHADOW_FADE) {
            return;
        }
        float half = (at[1] - at[0]) / 2.0F * 0.7F;
        float near = -at[2] - 0.2F;
        float far = -at[2] + BRAKE_GLOW_LENGTH;
        float spread = 0.4F;
        matrices.pushPose();
        matrices.translate(0.0, ground - car.getY() + 0.02, 0.0);
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        PoseStack.Pose pose = matrices.last();
        com.mojang.blaze3d.vertex.VertexConsumer out = buffers.getBuffer(BEAM);
        // The rear is +z here; x runs the other way round too, to keep the face up.
        shadowVertex(pose, out, BRAKE_GLOW, half + spread, far, 0.0F, 1.0F);
        shadowVertex(pose, out, BRAKE_GLOW, half, near, 0.0F, 0.0F);
        shadowVertex(pose, out, BRAKE_GLOW, -half, near, 1.0F, 0.0F);
        shadowVertex(pose, out, BRAKE_GLOW, -(half + spread), far, 1.0F, 1.0F);
        matrices.popPose();
    }

    /**
     * How light it is where the car stands, 0 to 15: lamps and the sky as
     * the time of day dims it. Worked out here, because a client level never
     * updates its own sky darkening; the sum is the one the server uses.
     */
    private static int lightAt(CarEntity car) {
        net.minecraft.world.level.Level level = car.level();
        net.minecraft.core.BlockPos pos = car.blockPosition().above();
        double rain = 1.0 - level.getRainLevel(1.0F) * 5.0F / 16.0;
        double thunder = 1.0 - level.getThunderLevel(1.0F) * 5.0F / 16.0;
        double day = 0.5 + 2.0 * Mth.clamp(Mth.cos(level.getTimeOfDay(1.0F) * (float) (Math.PI * 2)), -0.25, 0.25);
        int darken = level.dimensionType().hasSkyLight() ? (int) ((1.0 - day * rain * thunder) * 11.0) : 0;
        return Math.max(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos),
                level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) - darken);
    }

    /** The top of whatever the car stands over, up to four blocks down. */
    private static double groundBelow(CarEntity car) {
        net.minecraft.core.BlockPos below = car.blockPosition();
        double ground = car.getY();
        for (int i = 0; i < 4; i++) {
            net.minecraft.world.phys.shapes.VoxelShape shape = car.level().getBlockState(below)
                    .getCollisionShape(car.level(), below);
            if (!shape.isEmpty()) {
                return Math.min(car.getY(), below.getY() + shape.max(net.minecraft.core.Direction.Axis.Y));
            }
            below = below.below();
            ground = below.getY();
        }
        return ground;
    }

    /** How bright the tail lamps are with the lights on and the brakes off. */
    private static final int RUNNING_LIGHT = net.minecraft.client.renderer.LightTexture.pack(8, 8);

    private static void drawLit(VehicleModel model, CarRenderState state, PoseStack matrices,
                                MultiBufferSource buffers, net.minecraft.client.renderer.RenderType type) {
        drawLit(model, state, matrices, buffers, type, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
    }

    private static void drawLit(VehicleModel model, CarRenderState state, PoseStack matrices,
                                MultiBufferSource buffers, net.minecraft.client.renderer.RenderType type, int light) {
        model.setupAnim(state);
        model.renderToBuffer(matrices, buffers.getBuffer(type), net.minecraft.client.renderer.LightTexture.pack(
                        Math.max(net.minecraft.client.renderer.LightTexture.block(light),
                                net.minecraft.client.renderer.LightTexture.block(state.lightCoords)),
                        Math.max(net.minecraft.client.renderer.LightTexture.sky(light),
                                net.minecraft.client.renderer.LightTexture.sky(state.lightCoords))),
                OverlayTexture.NO_OVERLAY, -1);
    }

    private static void draw(VehicleModel model, CarRenderState state, PoseStack matrices,
                             MultiBufferSource buffers, net.minecraft.client.renderer.RenderType type) {
        model.setupAnim(state);
        model.renderToBuffer(matrices, buffers.getBuffer(type), state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
    }

    private static final int PLATE_BACKGROUND = 0xA0101418;

    /**
     * The service plates, written into the world beside the car and turned
     * to face the camera, through the body so the bonnet does not hide the
     * one for the part under it.
     */
    private void submitPlates(CarRenderState state, PoseStack matrices, MultiBufferSource buffers) {
        if (state.labels.isEmpty()) {
            return;
        }
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        for (CarServiceOverlay.PartLabel label : state.labels) {
            matrices.pushPose();
            matrices.translate(label.offset().x, label.offset().y, label.offset().z);
            matrices.mulPose(this.entityRenderDispatcher.cameraOrientation());
            matrices.scale(CarServiceOverlay.PLATE_SCALE, -CarServiceOverlay.PLATE_SCALE, CarServiceOverlay.PLATE_SCALE);
            net.minecraft.util.FormattedCharSequence text = label.text().getVisualOrderText();
            font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, label.colour(), false,
                    matrices.last().pose(), buffers, net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
                    PLATE_BACKGROUND, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
            matrices.popPose();
        }
    }
}
