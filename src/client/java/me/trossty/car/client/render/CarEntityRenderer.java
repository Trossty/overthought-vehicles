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
        this.shadowRadius = 0.9F;
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
        state.labels = CarServiceOverlay.labelsFor(car, tickProgress);
        state.wheelAngle = car.getWheelAngle(tickProgress);
        state.steerAngle = car.getSteerAngle(tickProgress);
        state.speed = car.getEngineSpeed();
        state.acceleration = car.getAcceleration(tickProgress);
        state.engineRunning = car.isEngineRunning();
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
