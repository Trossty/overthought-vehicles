import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the dashboard dial faces, and the ring cell an electric car's
 * battery gauge is drawn from.
 *
 * Everything that never changes is baked here — bezel, face, tick marks, the
 * fuel gauge's red band and its (unlit) pump icon — so the HUD only has to draw
 * the needle, the numbers and the warning lamp each frame. Drawing the whole
 * dial with rectangles every frame would cost a few hundred draw calls for an
 * image that is identical from one frame to the next.
 *
 * Geometry here and in DashboardHud must agree; the shared values are repeated
 * in both with the same names.
 *
 * Usage: java tools/GenGaugeTextures.java [outputDir] [previewFile]
 */
public class GenGaugeTextures {

    // ---- geometry (mirrored in DashboardHud) ----
    static final int TEX = 72;
    static final int C = 36;
    static final int RADIUS = 34;
    static final float START_ANGLE = 150.0F;
    static final float SWEEP = 240.0F;
    static final int TICK_OUTER = RADIUS - 5;
    static final int TICK_MINOR_INNER = TICK_OUTER - 3;
    static final int TICK_MAJOR_INNER = TICK_OUTER - 5;

    static final int SPEED_STEPS = 12;
    static final int FUEL_STEPS = 8;
    static final int MAJOR_EVERY = 2;
    static final float DANGER_FRACTION = 0.125F;

    static final int ICON_OFFSET_Y = 11;

    // The temperature dial is an ancillary gauge, so it is baked at its own
    // smaller size rather than drawn from the big face scaled down: a resampled
    // bezel next to two sharp ones is the one thing that would give it away.
    static final int TEMP_TEX = 48;
    static final int TEMP_C = 24;
    static final int TEMP_RADIUS = 22;
    static final int TEMP_TICK_OUTER = TEMP_RADIUS - 3;
    static final int TEMP_TICK_MINOR_INNER = TEMP_TICK_OUTER - 2;
    static final int TEMP_TICK_MAJOR_INNER = TEMP_TICK_OUTER - 4;
    static final int TEMP_STEPS = 8;
    static final int TEMP_ICON_OFFSET_Y = 6;
    /** Where the red band starts; matches CarEntity.OVERHEAT_AT. */
    static final float OVERHEAT_FRACTION = 0.85F;

    // ---- palette (mirrored in DashboardHud) ----
    static final int BEZEL_DARK = 0xFF15181A;
    static final int BEZEL = 0xFF585F64;
    static final int BEZEL_HIGHLIGHT = 0xFF8A9298;
    static final int FACE = 0xFF0E1112;
    static final int FACE_RIM = 0xFF1D2325;
    static final int TICK = 0xFF9BA4A9;
    static final int TICK_MAJOR = 0xFFF2F5F6;
    static final int DANGER = 0xFFBE2B20;
    static final int ICON_DIM = 0xFF3A4044;

    /**
     * Low-fuel pump pictogram. The HUD redraws this same shape in amber when the
     * tank runs low, so the two copies must stay identical.
     */
    static final String[] PUMP_ICON = {
            ".#######...",
            ".#.....#.#.",
            ".#.###.#.#.",
            ".#.###.#.#.",
            ".#.....###.",
            ".#.....#...",
            ".#.....#...",
            ".#.....#...",
            ".#######...",
            "###########",
    };

    /**
     * Service pictogram for the speed dial's warning lamp. The HUD redraws this
     * same shape in red when the car is badly knocked about, so the two copies
     * must stay identical.
     */
    static final String[] WRENCH_ICON = {
            "..##...##..",
            "..##...##..",
            "..#######..",
            "..#######..",
            "...#####...",
            "....###....",
            "....###....",
            "....###....",
            "....###....",
            "....###....",
    };

    /**
     * Coolant pictogram for the temperature dial. The HUD redraws this same
     * shape in red when the engine boils, so the two copies must stay
     * identical. Filled rather than outlined: at this size an outline closes up.
     */
    static final String[] THERMOMETER_ICON = {
            "..###..",
            "..###..",
            "..###..",
            "..###..",
            ".#####.",
            "#######",
            "#######",
            "#######",
            ".#####.",
    };

    static BufferedImage img;

    static void px(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
            img.setRGB(x, y, c);
        }
    }

    static void fill(int x0, int y0, int x1, int y1, int c) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                px(x, y, c);
            }
        }
    }

    static void disc(int cx, int cy, int r, int c) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, c);
        }
    }

    static void ring(int cx, int cy, int rOuter, int rInner, int c) {
        for (int dy = -rOuter; dy <= rOuter; dy++) {
            int outer = (int) Math.sqrt((double) rOuter * rOuter - (double) dy * dy);
            if (Math.abs(dy) >= rInner) {
                fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, c);
            } else {
                int inner = (int) Math.sqrt((double) rInner * rInner - (double) dy * dy);
                fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, c);
                fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, c);
            }
        }
    }

    /** A rectangle given in the dial's local frame, rotated about the centre. */
    static void rotRect(int cx, int cy, float angleDeg, float lx0, float ly0, float lx1, float ly1, int c) {
        double a = Math.toRadians(angleDeg);
        double cos = Math.cos(a);
        double sin = Math.sin(a);
        int reach = (int) Math.ceil(Math.max(Math.abs(lx0), Math.abs(lx1))
                + Math.max(Math.abs(ly0), Math.abs(ly1))) + 2;
        for (int x = cx - reach; x <= cx + reach; x++) {
            for (int y = cy - reach; y <= cy + reach; y++) {
                double dx = x - cx + 0.5;
                double dy = y - cy + 0.5;
                double lx = dx * cos + dy * sin;
                double ly = -dx * sin + dy * cos;
                if (lx >= lx0 && lx < lx1 && ly >= ly0 && ly < ly1) {
                    px(x, y, c);
                }
            }
        }
    }

    static float angleAt(float t) {
        return START_ANGLE + t * SWEEP;
    }

    static void drawBody() {
        drawBody(C, RADIUS);
    }

    static void drawBody(int c, int radius) {
        ring(c, c, radius, radius - 3, BEZEL);
        ring(c, c, radius, radius - 1, BEZEL_DARK);
        // A lighter arc across the upper left so the bezel reads as metal.
        for (int i = 0; i < 40; i++) {
            rotRect(c, c, 170 + i * 2.2F, radius - 3, -1, radius - 2, 0, BEZEL_HIGHLIGHT);
        }
        disc(c, c, radius - 3, FACE);
        ring(c, c, radius - 3, radius - 4, FACE_RIM);
    }

    static void drawTicks(int steps) {
        drawTicks(C, steps, TICK_OUTER, TICK_MINOR_INNER, TICK_MAJOR_INNER);
    }

    static void drawTicks(int c, int steps, int outer, int minorInner, int majorInner) {
        for (int i = 0; i <= steps; i++) {
            float a = angleAt((float) i / steps);
            if (i % MAJOR_EVERY == 0) {
                rotRect(c, c, a, majorInner, -1, outer, 1, TICK_MAJOR);
            } else {
                rotRect(c, c, a, minorInner, -0.5F, outer, 0.5F, TICK);
            }
        }
    }

    static void drawIcon(String[] icon, int color) {
        drawIcon(icon, color, C, ICON_OFFSET_Y);
    }

    static void drawIcon(String[] icon, int color, int c, int offsetY) {
        int w = icon[0].length();
        int x0 = c - w / 2;
        int y0 = c + offsetY;
        for (int row = 0; row < icon.length; row++) {
            for (int col = 0; col < w; col++) {
                if (icon[row].charAt(col) == '#') {
                    px(x0 + col, y0 + row, color);
                }
            }
        }
    }

    static BufferedImage speedFace() {
        img = new BufferedImage(TEX, TEX, BufferedImage.TYPE_INT_ARGB);
        drawBody();
        drawTicks(SPEED_STEPS);
        drawIcon(WRENCH_ICON, ICON_DIM);
        return img;
    }

    static BufferedImage fuelFace() {
        img = new BufferedImage(TEX, TEX, BufferedImage.TYPE_INT_ARGB);
        drawBody();
        // Red band over the bottom eighth of the sweep, drawn under the ticks so
        // the tick marks stay readable against it.
        for (int i = 0; i <= 32; i++) {
            rotRect(C, C, angleAt(i / 32.0F * DANGER_FRACTION), TICK_OUTER - 3, -1, TICK_OUTER, 1, DANGER);
        }
        drawTicks(FUEL_STEPS);
        drawIcon(PUMP_ICON, ICON_DIM);
        return img;
    }

    /**
     * The temperature dial. Where the fuel gauge warns at the bottom of its
     * sweep, this one warns at the top: the red band starts where the engine
     * starts to boil, so the needle entering it is the same event as the power
     * dropping away.
     */
    static BufferedImage tempFace() {
        img = new BufferedImage(TEMP_TEX, TEMP_TEX, BufferedImage.TYPE_INT_ARGB);
        drawBody(TEMP_C, TEMP_RADIUS);
        for (int i = 0; i <= 32; i++) {
            float t = OVERHEAT_FRACTION + (1.0F - OVERHEAT_FRACTION) * (i / 32.0F);
            rotRect(TEMP_C, TEMP_C, angleAt(t),
                    TEMP_TICK_OUTER - 2, -1, TEMP_TICK_OUTER, 1, DANGER);
        }
        drawTicks(TEMP_C, TEMP_STEPS, TEMP_TICK_OUTER, TEMP_TICK_MINOR_INNER, TEMP_TICK_MAJOR_INNER);
        drawIcon(THERMOMETER_ICON, ICON_DIM, TEMP_C, TEMP_ICON_OFFSET_Y);
        return img;
    }

    // ---- the battery gauge's ring (mirrored in DashboardHud) ----
    // An electric car's gauge is a ring of cells round the needles' sweep.
    // Every cell is the same wedge, so the HUD draws each one from this single
    // white texture, rotated into place and tinted lit or unlit. It is baked
    // finer than the dial, CELL_SCALE texels to a dial pixel, because it is
    // drawn turned and a coarse wedge would come out ragged.
    static final int CELL_SCALE = 4;
    static final float CELL_INNER = 23.0F;
    static final float CELL_OUTER = 29.0F;
    /** Half a cell's width in degrees: 9 of each cell's 12, the rest the gap. */
    static final float CELL_HALF_ANGLE = 4.5F;
    /** The texture's box in dial pixels along the cell's own centre line. */
    static final int CELL_X0 = 22;
    static final int CELL_W = 8;
    static final int CELL_H = 6;

    static BufferedImage cellTexture() {
        BufferedImage cell = new BufferedImage(CELL_W * CELL_SCALE, CELL_H * CELL_SCALE, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < cell.getWidth(); x++) {
            for (int y = 0; y < cell.getHeight(); y++) {
                double lx = CELL_X0 + (x + 0.5) / CELL_SCALE;
                double ly = -CELL_H / 2.0 + (y + 0.5) / CELL_SCALE;
                double rho = Math.hypot(lx, ly);
                double angle = Math.abs(Math.toDegrees(Math.atan2(ly, lx)));
                if (rho >= CELL_INNER && rho < CELL_OUTER && angle < CELL_HALF_ANGLE) {
                    cell.setRGB(x, y, 0xFFFFFFFF);
                }
            }
        }
        return cell;
    }

    static BufferedImage scale(BufferedImage src, int f) {
        BufferedImage out = new BufferedImage(src.getWidth() * f, src.getHeight() * f, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < out.getWidth(); x++) {
            for (int y = 0; y < out.getHeight(); y++) {
                out.setRGB(x, y, src.getRGB(x / f, y / f));
            }
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : "src/main/resources/assets/car/textures/gui";
        new File(dir).mkdirs();

        BufferedImage speed = speedFace();
        ImageIO.write(speed, "PNG", new File(dir, "gauge_speed.png"));
        BufferedImage fuel = fuelFace();
        ImageIO.write(fuel, "PNG", new File(dir, "gauge_fuel.png"));
        BufferedImage temp = tempFace();
        ImageIO.write(temp, "PNG", new File(dir, "gauge_temp.png"));
        // The rev counter's face is bare: its scale and red band depend on the
        // engine fitted, so the HUD draws them.
        img = new BufferedImage(TEX, TEX, BufferedImage.TYPE_INT_ARGB);
        drawBody();
        ImageIO.write(img, "PNG", new File(dir, "gauge_rpm.png"));
        // The battery gauge sits on the same bare face; this is its ring cell.
        ImageIO.write(cellTexture(), "PNG", new File(dir, "gauge_cell.png"));
        System.out.println("wrote " + dir + "/gauge_{speed,fuel,temp,rpm,cell}.png");

        if (args.length > 1) {
            BufferedImage sheet = new BufferedImage(TEX * 2 + TEMP_TEX, TEX, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < TEX; x++) {
                for (int y = 0; y < TEX; y++) {
                    sheet.setRGB(x, y, speed.getRGB(x, y));
                    sheet.setRGB(x + TEX, y, fuel.getRGB(x, y));
                }
            }
            // The small dial sits on the baseline of the other two.
            for (int x = 0; x < TEMP_TEX; x++) {
                for (int y = 0; y < TEMP_TEX; y++) {
                    sheet.setRGB(x + TEX * 2, y + TEX - TEMP_TEX, temp.getRGB(x, y));
                }
            }
            ImageIO.write(scale(sheet, 4), "PNG", new File(args[1]));
            System.out.println("wrote preview " + args[1]);
        }
    }
}
