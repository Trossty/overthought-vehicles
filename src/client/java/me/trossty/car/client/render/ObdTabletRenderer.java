package me.trossty.car.client.render;

import me.trossty.car.CarMain;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarParts;
import me.trossty.car.entity.FaultCodes;
import me.trossty.car.item.ObdScannerItem;
import me.trossty.car.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;

/**
 * The OBD tablet's screen, drawn where a held map's paper would be (see
 * HeldItemRendererMixin). The frame is lit by the world; the screen glows.
 * It reads the car live from what the client already knows of it, the way
 * a reader on a real car's port does: the codes, worst first, and the
 * gauges a mechanic checks first.
 *
 * <p>Coordinates are the map's: 128 units across the screen, y down.
 */
public final class ObdTabletRenderer {

    private static final RenderType FRAME = RenderType.text(CarMain.id("textures/gui/obd_tablet.png"));
    private static final RenderType FILL = RenderType.text(CarMain.id("textures/gui/obd_tablet_fill.png"));
    private static final RenderType BACK = RenderType.text(CarMain.id("textures/gui/obd_tablet_back.png"));
    private static final int GLOW = LightTexture.FULL_BRIGHT;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int CYAN = 0xFF4FD6E8;
    private static final int TEXT = 0xFFE6EEF2;
    private static final int DIM = 0xFF7F95A1;
    private static final int RULE = 0xFF1E3A47;
    private static final int TRACK = 0xFF132A34;
    private static final int GOOD = 0xFF5BE36B;
    private static final int AMBER = 0xFFF0A53A;
    private static final int BAD = 0xFFE8483B;

    /** The frame's edge and corner radius, and how thick the tablet is, in screen units. */
    private static final float EDGE_LO = -8.0F;
    private static final float EDGE_HI = 136.0F;
    private static final float RADIUS = 12.0F;
    private static final float THICK = 9.0F;
    /** How far in from the edge the rubber corner bumpers reach. */
    private static final float BUMPER = 22.0F;
    private static final int SIDE = 0xFF2A2D33;
    private static final int SIDE_RUBBER = 0xFFC2701F;

    /** Fault lines that fit under the gauges. */
    private static final int LINES = 6;

    private ObdTabletRenderer() {
    }

    public static void draw(PoseStack matrices, MultiBufferSource queue, int light, ItemStack stack) {
        // Placed exactly as the map's paper is.
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees(180.0F));
        matrices.scale(0.38F, 0.38F, 0.38F);
        matrices.translate(-0.5F, -0.5F, 0.0F);
        matrices.scale(0.0078125F, 0.0078125F, 0.0078125F);

        Screen screen = new Screen(matrices, queue);
        body(screen, light);
        screen.quad(FRAME, -8, -8, 136, 136, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, WHITE, light);
        screen.quad(FRAME, 0, 0, 128, 128, -0.01F, 4 / 72.0F, 4 / 72.0F, 68 / 72.0F, 68 / 72.0F, WHITE, GLOW);

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        CarEntity car = ObdScannerItem.reading(stack, client.player);
        boolean blink = (client.player.tickCount / 10) % 2 == 0;

        screen.text(Component.literal("OBD-II"), 5, 4, 0.75F, CYAN, 80);
        Component status = Component.translatable(car != null ? "obd.car.tablet.online" : "obd.car.tablet.offline");
        screen.textRight(status, 112, 5, 0.55F, DIM);
        screen.fill(115, 5, 120, 10, -0.02F, car != null ? GOOD : blink ? BAD : RULE);
        screen.fill(5, 13, 123, 14, -0.02F, RULE);

        if (car == null) {
            boolean paired = stack.has(ModComponents.OBD_LINK);
            screen.textCentred(Component.translatable(paired ? "obd.car.tablet.no_signal" : "obd.car.tablet.unpaired"),
                    54, 0.9F, paired ? AMBER : TEXT);
            screen.textCentred(Component.translatable(paired ? "obd.car.tablet.out_of_range" : "obd.car.tablet.hint"),
                    68, 0.55F, DIM);
            return;
        }

        // Which car, and how far it has been.
        screen.text(car.getVehicleName(), 5, 17, 0.85F, TEXT, 118);
        Component sub = car.isElectric()
                ? Component.translatable("obd.car.tablet.electric")
                : Component.translatable("fuel.car." + car.getTankFuel().key());
        screen.text(Component.empty().append(CarEntity.formatOdometer(car.parts().getOdometer())).append("  ·  ").append(sub),
                5, 27, 0.6F, DIM, 118);

        // The three gauges a mechanic looks at first.
        float tank = car.getFuel() / Math.max(1.0F, car.getMaxFuel());
        float body = car.damage().getHealthFraction();
        float temp = car.getEngineTemp();
        screen.gauge(Component.translatable(car.isElectric() ? "obd.car.tablet.charge" : "obd.car.tablet.fuel"),
                35, tank, tank < 0.15F ? BAD : car.getDirtyFuel() >= 0.25F ? AMBER : CYAN);
        screen.gauge(Component.translatable("obd.car.tablet.body"), 43, body,
                body < 0.35F ? BAD : body < 0.6F ? AMBER : GOOD);
        screen.gauge(Component.translatable("obd.car.tablet.temp"), 51, temp,
                temp >= 0.85F ? BAD : temp > 0.7F ? AMBER : CYAN);
        screen.fill(5, 59, 123, 60, -0.02F, RULE);

        // The stored codes, worst first.
        List<FaultCodes.Fault> faults = FaultCodes.read(car);
        screen.text(faults.isEmpty() ? Component.translatable("obd.car.clear")
                : Component.translatable("obd.car.count", faults.size()), 5, 63, 0.6F, faults.isEmpty() ? GOOD : TEXT, 118);
        int shown = faults.size() > LINES ? LINES - 1 : faults.size();
        float y = 72.0F;
        for (int i = 0; i < shown; i++) {
            FaultCodes.Fault fault = faults.get(i);
            screen.fill(5, y + 0.5F, 7, y + 4.5F, -0.02F, 0xFF000000 | fault.severity().colour.getColor());
            screen.text(fault.describe(), 9, y, 0.58F, TEXT, 114);
            y += 7.0F;
        }
        if (faults.size() > shown) {
            screen.text(Component.translatable("obd.car.more", faults.size() - shown).withStyle(ChatFormatting.ITALIC),
                    5, y, 0.55F, DIM, 118);
            y += 7.0F;
        }
        // Where to start looking: the worst fault's place on the car, which a
        // scanner in hand also lights up on the car itself.
        Component where = faults.isEmpty() ? null : faults.get(0).where();
        if (where != null && y < 114.0F) {
            screen.text(where, 5, y + 1.0F, 0.55F, AMBER, 118);
        }
        float mileage = CarParts.mileageFactor(car.parts().getOdometer());
        if (mileage > 1.05F) {
            screen.text(Component.translatable("obd.car.high_mileage", Math.round((mileage - 1.0F) * 100.0F)),
                    5, 120, 0.5F, DIM, 118);
        }
    }

    /**
     * The tablet's body behind the screen: the back panel, and the edge all the
     * way round it, following the frame's rounded corners, rubber where the
     * corner bumpers are. The screen faces -z; the body goes back into +z.
     */
    private static void body(Screen screen, int light) {
        // Seen from behind, so the back is drawn mirrored to read the right way round.
        // Both windings, so it shows whichever way the layer culls.
        screen.quad(BACK, EDGE_LO, EDGE_LO, EDGE_HI, EDGE_HI, THICK, 1.0F, 0.0F, 0.0F, 1.0F, WHITE, light);
        screen.quad(BACK, EDGE_HI, EDGE_LO, EDGE_LO, EDGE_HI, THICK, 0.0F, 0.0F, 1.0F, 1.0F, WHITE, light);

        List<float[]> rim = rim();
        for (int i = 0; i < rim.size(); i++) {
            float[] p = rim.get(i);
            float[] q = rim.get((i + 1) % rim.size());
            float mx = (p[0] + q[0]) / 2.0F;
            float my = (p[1] + q[1]) / 2.0F;
            boolean rubber = (mx < EDGE_LO + BUMPER || mx > EDGE_HI - BUMPER)
                    && (my < EDGE_LO + BUMPER || my > EDGE_HI - BUMPER);
            // The top and bottom edges catch less light than the sides.
            boolean flat = Math.abs(q[1] - p[1]) < 0.01F;
            int colour = rubber ? SIDE_RUBBER : SIDE;
            if (flat) {
                colour = darken(colour, my > 64 ? 0.7F : 0.85F);
            }
            screen.side(p[0], p[1], q[0], q[1], 0.0F, THICK, colour, light);
        }
    }

    /** The frame's outline: straight edges cut where the bumpers start, and arcs at the corners. */
    private static List<float[]> rim() {
        List<float[]> points = new java.util.ArrayList<>();
        float lo = EDGE_LO + RADIUS;
        float hi = EDGE_HI - RADIUS;
        float[][] centres = {{lo, lo}, {hi, lo}, {hi, hi}, {lo, hi}};
        for (int corner = 0; corner < 4; corner++) {
            float start = 180.0F + corner * 90.0F;
            for (int step = 0; step <= 4; step++) {
                double angle = Math.toRadians(start + step * 22.5F);
                points.add(new float[]{centres[corner][0] + RADIUS * (float) Math.cos(angle),
                        centres[corner][1] + RADIUS * (float) Math.sin(angle)});
            }
            // Along the straight edge to the next corner, breaking where the rubber stops and starts.
            float[] from = points.get(points.size() - 1);
            float a = EDGE_LO + BUMPER;
            float b = EDGE_HI - BUMPER;
            switch (corner) {
                case 0 -> { points.add(new float[]{a, from[1]}); points.add(new float[]{b, from[1]}); }
                case 1 -> { points.add(new float[]{from[0], a}); points.add(new float[]{from[0], b}); }
                case 2 -> { points.add(new float[]{b, from[1]}); points.add(new float[]{a, from[1]}); }
                default -> { points.add(new float[]{from[0], b}); points.add(new float[]{from[0], a}); }
            }
        }
        return points;
    }

    private static int darken(int argb, float by) {
        int r = (int) (((argb >> 16) & 0xFF) * by);
        int g = (int) (((argb >> 8) & 0xFF) * by);
        int b = (int) ((argb & 0xFF) * by);
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    /** Drawing on the screen's 128-unit square. */
    private record Screen(PoseStack matrices, MultiBufferSource queue) {

        void quad(RenderType layer, float x0, float y0, float x1, float y1, float z,
                  float u0, float v0, float u1, float v1, int colour, int light) {
            {
                PoseStack.Pose entry = this.matrices.last();
                com.mojang.blaze3d.vertex.VertexConsumer vertices = this.queue.getBuffer(layer);
                vertices.addVertex(entry, x0, y1, z).setColor(colour).setUv(u0, v1).setLight(light);
                vertices.addVertex(entry, x1, y1, z).setColor(colour).setUv(u1, v1).setLight(light);
                vertices.addVertex(entry, x1, y0, z).setColor(colour).setUv(u1, v0).setLight(light);
                vertices.addVertex(entry, x0, y0, z).setColor(colour).setUv(u0, v0).setLight(light);
            }
        }

        /** An edge strip from (x0, y0) to (x1, y1), running back from z0 to z1; seen from either side. */
        void side(float x0, float y0, float x1, float y1, float z0, float z1, int colour, int light) {
            {
                PoseStack.Pose entry = this.matrices.last();
                com.mojang.blaze3d.vertex.VertexConsumer vertices = this.queue.getBuffer(FILL);
                vertices.addVertex(entry, x0, y0, z0).setColor(colour).setUv(0, 0).setLight(light);
                vertices.addVertex(entry, x1, y1, z0).setColor(colour).setUv(1, 0).setLight(light);
                vertices.addVertex(entry, x1, y1, z1).setColor(colour).setUv(1, 1).setLight(light);
                vertices.addVertex(entry, x0, y0, z1).setColor(colour).setUv(0, 1).setLight(light);
                vertices.addVertex(entry, x0, y0, z1).setColor(colour).setUv(0, 1).setLight(light);
                vertices.addVertex(entry, x1, y1, z1).setColor(colour).setUv(1, 1).setLight(light);
                vertices.addVertex(entry, x1, y1, z0).setColor(colour).setUv(1, 0).setLight(light);
                vertices.addVertex(entry, x0, y0, z0).setColor(colour).setUv(0, 0).setLight(light);
            }
        }

        void fill(float x0, float y0, float x1, float y1, float z, int colour) {
            this.quad(FILL, x0, y0, x1, y1, z, 0.0F, 0.0F, 1.0F, 1.0F, colour, GLOW);
        }

        /** A labelled bar: the label, the bar, and the percentage after it. */
        void gauge(Component label, float y, float fraction, int colour) {
            float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
            this.text(label, 5, y, 0.55F, DIM, 34);
            this.fill(40, y + 0.5F, 104, y + 4.5F, -0.015F, TRACK);
            if (clamped > 0.0F) {
                this.fill(40, y + 0.5F, 40 + 64 * clamped, y + 4.5F, -0.02F, colour);
            }
            this.textRight(Component.literal(Math.round(clamped * 100.0F) + "%"), 123, y, 0.55F, TEXT);
        }

        /** Text from (x, y), cut off at this width on the screen. */
        void text(Component text, float x, float y, float scale, int colour, float width) {
            Font font = Minecraft.getInstance().font;
            List<FormattedCharSequence> lines = font.split(text, Math.max(1, (int) (width / scale)));
            if (!lines.isEmpty()) {
                this.submit(lines.get(0), x, y, scale, colour);
            }
        }

        void textRight(Component text, float right, float y, float scale, int colour) {
            Font font = Minecraft.getInstance().font;
            this.submit(text.getVisualOrderText(), right - font.width(text) * scale, y, scale, colour);
        }

        void textCentred(Component text, float y, float scale, int colour) {
            Font font = Minecraft.getInstance().font;
            List<FormattedCharSequence> lines = font.split(text, (int) (118 / scale));
            for (FormattedCharSequence line : lines) {
                this.submit(line, 64 - font.width(line) * scale / 2.0F, y, scale, colour);
                y += 10 * scale;
            }
        }

        private void submit(FormattedCharSequence text, float x, float y, float scale, int colour) {
            this.matrices.pushPose();
            this.matrices.translate(x, y, -0.025F);
            this.matrices.scale(scale, scale, -1.0F);
            this.matrices.translate(0.0F, 0.0F, 0.1F);
            net.minecraft.client.Minecraft.getInstance().font.drawInBatch(text, 0.0F, 0.0F, colour, false, this.matrices.last().pose(), this.queue, Font.DisplayMode.NORMAL, 0, GLOW);
            this.matrices.popPose();
        }
    }
}
