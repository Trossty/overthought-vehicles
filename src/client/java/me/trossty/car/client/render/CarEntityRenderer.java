package me.trossty.car.client.render;

import me.trossty.car.CarMain;
import me.trossty.car.entity.CarEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class CarEntityRenderer extends EntityRenderer<CarEntity, CarRenderState> {

    /**
     * One texture per dye colour, indexed by {@code DyeColor.getIndex()}. The car
     * is painted by swapping the texture rather than tinting the model, because a
     * tint would colour the tyres, glass and lights along with the bodywork.
     */
    private static final Identifier[] TEXTURES = buildTextures();

    private static Identifier[] buildTextures() {
        DyeColor[] colors = DyeColor.values();
        Identifier[] textures = new Identifier[colors.length];
        for (DyeColor color : colors) {
            textures[color.getIndex()] = CarMain.id("textures/entity/car/car_" + color.getId() + ".png");
        }
        return textures;
    }

    private final EntityModel<CarRenderState> model;

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
        float pace = MathHelper.clamp(speed / LEAN_FULL_SPEED, 0.0F, 1.0F);
        return -steerAngle * LEAN_PER_STEER * pace;
    }

    public CarEntityRenderer(EntityRendererFactory.Context context) {
        this(context, ModEntityModelLayers.CAR);
    }

    /** Subclasses differ only in which baked model they pick up. */
    protected CarEntityRenderer(EntityRendererFactory.Context context, EntityModelLayer layer) {
        super(context);
        this.model = new CarEntityModel(context.getPart(layer));
        this.shadowRadius = 0.9F;
    }

    /** Smoothstep: the pose settles rather than stopping dead. */
    private static float ease(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public CarRenderState createRenderState() {
        return new CarRenderState();
    }

    @Override
    public void updateRenderState(CarEntity car, CarRenderState state, float tickProgress) {
        super.updateRenderState(car, state, tickProgress);
        state.yaw = car.getLerpedYaw(tickProgress);
        state.color = car.getColor();
        state.paint = car.getPaint();
        state.vehicle = car.getDefinition();
        state.vehicleId = state.vehicle == null ? null : car.getModelId();
        state.tyre = me.trossty.car.entity.CarSetup.tyre(car.getSetup()).key();
        state.tyresOn = car.isFitted(me.trossty.car.entity.CarPart.WHEELS);
        state.discsOn = car.isFitted(me.trossty.car.entity.CarPart.BRAKE_DISCS);
        state.labels = CarServiceOverlay.labelsFor(car, tickProgress);
        state.wheelAngle = car.getWheelAngle(tickProgress);
        state.steerAngle = car.getSteerAngle(tickProgress);
        state.speed = car.getEngineSpeed();
        state.acceleration = car.getAcceleration(tickProgress);
        state.engineRunning = car.isEngineRunning();
        state.restPose = car.getRestPose(tickProgress);
        state.damageWobbleTicks = car.getDamageWobbleTicks() - tickProgress;
        state.damageWobbleSide = car.getDamageWobbleSide();
        state.damageWobbleStrength = Math.max(car.getDamageWobbleStrength() - tickProgress, 0.0F);
    }

    /**
     * Anything else drawn on the body, in the body's own frame (model space,
     * already turned and flipped): nothing for most vehicles.
     */
    protected void submitExtras(CarRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue) {
    }

    @Override
    public void render(CarRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                       CameraRenderState cameraState) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - state.yaw));

        // Two wheels lean into the turn, more the faster they go, about the
        // line where the tyres touch the road.
        if (state.vehicle != null
                && state.vehicle.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.MOTORCYCLE) {
            // The rider is drawn upright, so the bike swings about the saddle
            // rather than the tyres: the seat stays under them and the
            // wheels go out wide, which reads as the lean.
            matrices.translate(0.0F, LEAN_PIVOT, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(leanDegrees(state.steerAngle, state.speed)));
            matrices.translate(0.0F, -LEAN_PIVOT, 0.0F);
            // Parked, it rests over on its stand, pivoting where the tyres
            // meet the ground so they stay on it.
            if (state.restPose > 0.001F) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(STAND_TILT * ease(state.restPose)));
            }
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
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-NOSE_DROP * ease(state.restPose)));
            matrices.translate(0.0F, -axleY, -axleZ);
        }

        if (state.damageWobbleTicks > 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(state.damageWobbleTicks)
                            * state.damageWobbleTicks
                            * state.damageWobbleStrength / 10.0F
                            * state.damageWobbleSide));
        }

        // A vehicle a data pack describes brings its own shape and palette;
        // everything else here — the turn, the wobble, the light — is shared.
        EntityModel<CarRenderState> model = this.model;
        Identifier texture = TEXTURES[state.color.getIndex()];
        if (state.vehicle != null && state.vehicleId != null) {
            model = DefinedVehicleModel.of(state.vehicleId, state.vehicle);
            texture = DefinedVehicleModel.textureOf(state.vehicleId, state.vehicle,
                    state.paint >= 0 ? state.paint : state.color.getEntityColor());
        }

        matrices.scale(-1.0F, -1.0F, 1.0F);
        queue.submitModel(
                model,
                state,
                matrices,
                model.getLayer(texture),
                state.light,
                OverlayTexture.DEFAULT_UV,
                state.outlineColor,
                null);
        if (state.vehicle != null && state.vehicleId != null) {
            // The glass goes in a later batch: drawn before the people inside,
            // it would hide them from anyone looking in.
            EntityModel<CarRenderState> glass = DefinedVehicleModel.glassOf(state.vehicleId, state.vehicle);
            if (glass != null) {
                queue.getBatchingQueue(1).submitModel(
                        glass,
                        state,
                        matrices,
                        glass.getLayer(texture),
                        state.light,
                        OverlayTexture.DEFAULT_UV,
                        state.outlineColor,
                        null);
            }
        }
        this.submitExtras(state, matrices, queue);
        if (state.vehicle != null && state.vehicleId != null) {
            EntityModel<CarRenderState> wheels = DefinedVehicleModel.wheelsOf(state.vehicleId, state.vehicle);
            queue.submitModel(
                    wheels,
                    state,
                    matrices,
                    wheels.getLayer(DefinedVehicleModel.wheelTexture(state.tyre)),
                    state.light,
                    OverlayTexture.DEFAULT_UV,
                    state.outlineColor,
                    null);
        }
        matrices.pop();

        this.submitPlates(state, matrices, queue, cameraState);
        super.render(state, matrices, queue, cameraState);
    }

    private static final int PLATE_BACKGROUND = 0xA0101418;

    /**
     * The service plates, written into the world beside the car and turned
     * to face the camera, through the body so the bonnet does not hide the
     * one for the part under it.
     */
    private void submitPlates(CarRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                              CameraRenderState cameraState) {
        if (state.labels.isEmpty()) {
            return;
        }
        net.minecraft.client.font.TextRenderer font = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        for (CarServiceOverlay.PartLabel label : state.labels) {
            matrices.push();
            matrices.translate(label.offset().x, label.offset().y, label.offset().z);
            matrices.multiply(cameraState.orientation);
            matrices.scale(CarServiceOverlay.PLATE_SCALE, -CarServiceOverlay.PLATE_SCALE, CarServiceOverlay.PLATE_SCALE);
            net.minecraft.text.OrderedText text = label.text().asOrderedText();
            queue.submitText(matrices, -font.getWidth(text) / 2.0F, -font.fontHeight / 2.0F, text, false,
                    net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH,
                    net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    label.colour(), PLATE_BACKGROUND, 0);
            matrices.pop();
        }
    }
}
