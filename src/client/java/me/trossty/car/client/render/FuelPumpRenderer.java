package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.registry.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The two live parts of a pump's face: the petrol standing in the glass
 * box low down, and the display up top reading out what it holds.
 * Everything else is the block model.
 */
public class FuelPumpRenderer implements BlockEntityRenderer<FuelPumpBlockEntity> {

    // The window and the display, in the model's pixels (it faces north).
    // The box behind the glass the petrol stands in.
    private static final float WINDOW_X1 = 5.02F / 16;
    private static final float WINDOW_X2 = 10.98F / 16;
    private static final float WINDOW_Y1 = 2.52F / 16;
    private static final float WINDOW_Y2 = 8.98F / 16;
    private static final float WINDOW_Z1 = 4.05F / 16;
    private static final float WINDOW_Z2 = 8.98F / 16;
    private static final float SCREEN_Y = 23.0F / 16;
    private static final float SCREEN_Z = 3.45F / 16;
    private static final float TEXT_SCALE = 0.0105F;
    private static final int LCD_GREEN = 0xFF5AF078;
    private static final int LCD_AMBER = 0xFFF0B030;

    public static class State extends BlockEntityRenderState {
        float fill;
        Direction facing = Direction.NORTH;
    }

    public FuelPumpRenderer(BlockEntityRendererProvider.Context context) {
    }

    public State createRenderState() {
        return new State();
    }

    private void extractRenderState(FuelPumpBlockEntity pump, State state, float tickProgress) {
        state.fill = Math.min(1.0F, (float) pump.getPetrol() / pump.getCapacity());
        if (state.blockState.getBlock() instanceof FuelPumpBlock) {
            state.facing = state.blockState.getValue(FuelPumpBlock.FACING);
        }
    }

    @Override
    public void render(FuelPumpBlockEntity blockEntity, float tickProgress, PoseStack matrices, MultiBufferSource buffers,
                       int light, int overlay) {
        State state = this.createRenderState();
        state.blockState = blockEntity.getBlockState();
        state.blockPos = blockEntity.getBlockPos();
        state.lightCoords = light;
        this.extractRenderState(blockEntity, state, tickProgress);
        this.submit(state, matrices, buffers);
    }

    private void submit(State state, PoseStack matrices, MultiBufferSource queue) {
        matrices.pushPose();
        // Turn the model's north-facing frame the way the block faces.
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.mulPose(Axis.YP.rotationDegrees(-turn(state.facing)));
        matrices.translate(-0.5F, 0.0F, -0.5F);

        this.window(state, matrices, queue);
        this.display(state, matrices, queue);
        matrices.popPose();
    }

    /** Degrees a north-facing model is turned (clockwise, from above) to face this way. */
    private static float turn(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private void window(State state, PoseStack matrices, MultiBufferSource queue) {
        if (state.fill <= 0.0F) {
            return;
        }
        net.minecraft.world.level.material.Fluid petrol = ModFluids.PETROL;
        TextureAtlasSprite sprite = LiquidColours.sprite(petrol);
        if (sprite == null) {
            return;
        }
        int colour = 0xFF000000 | LiquidColours.tint(petrol);
        float top = WINDOW_Y1 + (WINDOW_Y2 - WINDOW_Y1) * state.fill;
        int light = state.lightCoords;
        // A body of petrol standing in the box: its face against the glass and
        // its surface. Drawn solid, so it reads as a full body of liquid
        // however see-through the fluid's own texture is.
        {
                PoseStack.Pose entry = matrices.last();
                com.mojang.blaze3d.vertex.VertexConsumer consumer = queue.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
                    float u1 = sprite.getU0();
                    float u2 = sprite.getU(WINDOW_X2 - WINDOW_X1);
                    float v1 = sprite.getV0();
                    float vFront = sprite.getV(top - WINDOW_Y1);
                    float vTop = sprite.getV(WINDOW_Z2 - WINDOW_Z1);
                    vertex(consumer, entry, colour, light, 0, 0, -1, WINDOW_X2, top, WINDOW_Z1, u1, v1);
                    vertex(consumer, entry, colour, light, 0, 0, -1, WINDOW_X2, WINDOW_Y1, WINDOW_Z1, u1, vFront);
                    vertex(consumer, entry, colour, light, 0, 0, -1, WINDOW_X1, WINDOW_Y1, WINDOW_Z1, u2, vFront);
                    vertex(consumer, entry, colour, light, 0, 0, -1, WINDOW_X1, top, WINDOW_Z1, u2, v1);
                    if (top < WINDOW_Y2) {
                        vertex(consumer, entry, colour, light, 0, 1, 0, WINDOW_X1, top, WINDOW_Z1, u1, v1);
                        vertex(consumer, entry, colour, light, 0, 1, 0, WINDOW_X1, top, WINDOW_Z2, u1, vTop);
                        vertex(consumer, entry, colour, light, 0, 1, 0, WINDOW_X2, top, WINDOW_Z2, u2, vTop);
                        vertex(consumer, entry, colour, light, 0, 1, 0, WINDOW_X2, top, WINDOW_Z1, u2, v1);
                    }
                }
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer consumer, PoseStack.Pose entry,
                               int colour, int light, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        consumer.addVertex(entry, x, y, z)
                .setColor(colour)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(entry, nx, ny, nz);
    }

    /** The fuel's name over how full the pump is, lit up whatever the daylight. */
    private void display(State state, PoseStack matrices, MultiBufferSource queue) {
        Font font = Minecraft.getInstance().font;
        int percent = Math.round(state.fill * 100.0F);
        FormattedCharSequence name = Component.translatable("fluid.overthought_vehicles.petrol").getVisualOrderText();
        FormattedCharSequence amount = Component.literal(percent + "%").getVisualOrderText();
        matrices.pushPose();
        matrices.translate(0.5F, SCREEN_Y, SCREEN_Z);
        // Text is drawn facing south; the display faces north.
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
        matrices.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        net.minecraft.client.Minecraft.getInstance().font.drawInBatch(name, -font.width(name) / 2.0F, -10.0F, LCD_GREEN, false, matrices.last().pose(), queue, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        net.minecraft.client.Minecraft.getInstance().font.drawInBatch(amount, -font.width(amount) / 2.0F, 1.0F, percent < 15 ? LCD_AMBER : LCD_GREEN, false, matrices.last().pose(), queue, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        matrices.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(FuelPumpBlockEntity blockEntity) {
        return true;
    }
}
