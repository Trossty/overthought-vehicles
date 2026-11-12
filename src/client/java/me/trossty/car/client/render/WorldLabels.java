package me.trossty.car.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The names hanging in the air this frame, whoever hung them there.
 *
 * <p>Anything that marks up the world — the parts on a car, the buttons on a
 * bench top — says here where its label belongs, and the HUD draws them all
 * flat on the screen at the point each one projects to. They are not drawn in
 * the world itself for two reasons: text written into the world buffer during
 * this pass never reaches the screen in this version, and a label that the
 * thing it labels can hide is not much of a label.
 */
public final class WorldLabels {

    /**
     * One label.
     *
     * @param at     where in the world it belongs
     * @param text   what it says
     * @param colour what colour it says it in
     * @param lit    whether the tooltip beside the crosshair is speaking for it
     * @param icon   an item to draw over the words, or an empty stack for none
     */
    public record Label(Vec3d at, Text text, int colour, boolean lit, ItemStack icon) {
    }

    private static final List<Label> LABELS = new ArrayList<>();
    private static Vec3d camera = Vec3d.ZERO;
    private static Matrix4f view = new Matrix4f();

    private WorldLabels() {
    }

    /** Clears last frame's labels and notes where this frame is being seen from. */
    public static void begin(WorldRenderContext context) {
        LABELS.clear();
        camera = context.worldState().cameraRenderState.pos;
        view = new Matrix4f(context.matrices().peek().getPositionMatrix());
    }

    public static void add(Vec3d at, Text text, int colour, boolean lit) {
        LABELS.add(new Label(at, text, colour, lit, ItemStack.EMPTY));
    }

    public static void add(Vec3d at, Text text, int colour, ItemStack icon) {
        LABELS.add(new Label(at, text, colour, false, icon));
    }

    public static List<Label> labels() {
        return Collections.unmodifiableList(LABELS);
    }

    public static Vec3d camera() {
        return camera;
    }

    public static Matrix4f view() {
        return view;
    }
}
