import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;

/**
 * Generates the car's entity textures (one per dye colour) and its item texture.
 *
 * Minecraft box UV layout for a box of size (sx, sy, sz) placed at (u, v):
 *   row 1, height sz:  top (u+sz, v, sx, sz), bottom (u+sz+sx, v, sx, sz)
 *   row 2, height sy:  -x (u, v+sz, sz, sy), -z (u+sz, v+sz, sx, sy),
 *                      +x (u+sz+sx, v+sz, sz, sy), +z (u+sz+sx+sz, v+sz, sx, sy)
 *   total size: 2*(sx+sz) wide, sz+sy tall
 *
 * Only the body panels change colour; glass, tyres, lights and the grille stay
 * fixed, which is why the car is painted by swapping textures rather than by
 * tinting the whole model.
 */
public class GenCarTextures {

    /** Vanilla dye names and their diffuse colours, in DyeColor order. */
    static final Object[][] DYES = {
            {"white", 0xF9FFFE}, {"orange", 0xF9801D}, {"magenta", 0xC74EBD},
            {"light_blue", 0x3AB3DA}, {"yellow", 0xFED83D}, {"lime", 0x80C71F},
            {"pink", 0xF38BAA}, {"gray", 0x474F52}, {"light_gray", 0x9D9D97},
            {"cyan", 0x169C9C}, {"purple", 0x8932B8}, {"blue", 0x3C44AA},
            {"brown", 0x835432}, {"green", 0x5E7C16}, {"red", 0xB02E26},
            {"black", 0x1D1D21},
    };

    static int BODY;
    static int BODY_DARK;
    static int BODY_LIGHT;

    static final int GLASS = 0xFF1E2A30;
    static final int GLASS_LIGHT = 0xFF35505C;
    static final int TIRE = 0xFF141414;
    static final int RIM = 0xFF2A2A2A;
    static final int HUB = 0xFF9E9E9E;
    static final int LIGHT = 0xFFFFE9A8;
    static final int TAIL_LIGHT = 0xFFB3261E;
    static final int GRILLE = 0xFF2B2B2B;
    static final int INTERIOR = 0xFF2A2E31;
    static final int INTERIOR_DARK = 0xFF16191B;
    /** Fully transparent; the model's cutout layer turns these pixels into holes. */
    static final int CLEAR = 0x00000000;

    // Interior trim. The seats are the one warm note in an otherwise cold cabin,
    // so the eye lands on them through the window.
    static final int SEAT = 0xFF3A2F2A;
    static final int SEAT_LIGHT = 0xFF4E4038;
    static final int SEAT_DARK = 0xFF241D1A;
    static final int WHEEL_RIM = 0xFF1C1C1E;
    static final int WHEEL_RIM_LIGHT = 0xFF2E2E31;
    static final int WHEEL_SPOKE = 0xFF3A3A3D;
    static final int HORN_BOSS = 0xFF8A8A8E;
    static final int GAUGE_FACE = 0xFF14161A;
    static final int GAUGE_LIT = 0xFFE8A020;

    static BufferedImage img;

    /**
     * Blends towards white or black rather than scaling the channels, so a very
     * dark paint still gets a visible highlight and a very light one still gets
     * a visible shadow.
     */
    static int mix(int rgb, int target, float t) {
        int r = (int) (((rgb >> 16) & 0xFF) * (1 - t) + ((target >> 16) & 0xFF) * t);
        int g = (int) (((rgb >> 8) & 0xFF) * (1 - t) + ((target >> 8) & 0xFF) * t);
        int b = (int) ((rgb & 0xFF) * (1 - t) + (target & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static void setBody(int rgb) {
        BODY = 0xFF000000 | rgb;
        BODY_LIGHT = mix(rgb, 0xFFFFFF, 0.28F);
        BODY_DARK = mix(rgb, 0x000000, 0.32F);
    }

    static void rect(int x, int y, int w, int h, int c) {
        for (int i = x; i < x + w; i++)
            for (int j = y; j < y + h; j++)
                if (i >= 0 && j >= 0 && i < img.getWidth() && j < img.getHeight())
                    img.setRGB(i, j, c);
    }

    static void border(int x, int y, int w, int h, int c) {
        rect(x, y, w, 1, c);
        rect(x, y + h - 1, w, 1, c);
        rect(x, y, 1, h, c);
        rect(x + w - 1, y, 1, h, c);
    }

    /**
     * Outside of a cabin wall, 22 rows tall: roof trim, then the window opening
     * between two pillars, then the sill and the door panel.
     *
     * Rows 2..14 are left untouched, so they stay transparent and the window
     * becomes a real hole you can see the occupants through. That band is world
     * 2.0 down to 1.25, which frames a seated rider's head and shoulders.
     */
    static void wallOutside(int x, int y, int w) {
        rect(x, y, w, 2, BODY_DARK);            // roof trim
        rect(x, y + 2, 2, 12, BODY);            // leading pillar
        rect(x + w - 2, y + 2, 2, 12, BODY);    // trailing pillar
        rect(x, y + 14, w, 1, BODY_DARK);       // window sill
        rect(x, y + 15, w, 7, BODY);            // door panel
        rect(x, y + 16, w, 1, BODY_LIGHT);      // crease
        rect(x, y + 21, w, 1, BODY_DARK);       // shadow along the body
    }

    /** Inside of the same wall; the window rows are left clear here too. */
    static void wallInside(int x, int y, int w) {
        rect(x, y, w, 2, INTERIOR_DARK);
        rect(x, y + 2, 2, 12, INTERIOR);
        rect(x + w - 2, y + 2, 2, 12, INTERIOR);
        rect(x, y + 14, w, 1, INTERIOR_DARK);
        rect(x, y + 15, w, 7, INTERIOR);
    }

    static void drawEntityTexture() {
        img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        // ---- body: sx=20 sy=6 sz=40 at uv(0,0)  -> 120x46 ----
        rect(40, 0, 20, 40, BODY);          // top: hood + trunk deck
        border(40, 0, 20, 40, BODY_DARK);
        rect(60, 0, 20, 40, BODY_DARK);     // bottom: chassis underside
        rect(0, 40, 40, 6, BODY);           // -x flank
        rect(0, 40, 40, 1, BODY_LIGHT);
        rect(40, 40, 20, 6, BODY);          // -z front
        rect(60, 40, 40, 6, BODY);          // +x flank
        rect(60, 40, 40, 1, BODY_LIGHT);
        rect(100, 40, 20, 6, BODY);         // +z rear

        rect(42, 42, 4, 3, LIGHT);          // headlights
        rect(54, 42, 4, 3, LIGHT);
        rect(48, 42, 4, 3, GRILLE);         // grille
        rect(102, 42, 4, 3, TAIL_LIGHT);    // tail lights
        rect(114, 42, 4, 3, TAIL_LIGHT);

        // ---- wheels: sx=3 sy=6 sz=6 at uv(0|20|40|60, 48) -> 18x12 each ----
        for (int w = 0; w < 4; w++) {
            int u = w * 20;
            rect(u, 48, 18, 12, TIRE);
            // the two 6x6 side faces at (u, 54) and (u+9, 54) are the visible discs
            rect(u + 1, 55, 4, 4, RIM);
            rect(u + 2, 56, 2, 2, HUB);
            rect(u + 10, 55, 4, 4, RIM);
            rect(u + 11, 56, 2, 2, HUB);
        }

        // ---- cabin front wall: sx=20 sy=22 sz=2 at uv(0,64) ----
        // -z is the car's nose, so that face is the windscreen.
        rect(2, 64, 20, 2, BODY_DARK);      // top edge
        rect(22, 64, 20, 2, BODY_DARK);     // bottom edge
        rect(0, 66, 2, 22, BODY_DARK);      // side edges
        rect(22, 66, 2, 22, BODY_DARK);
        wallOutside(2, 66, 20);             // windscreen from outside
        wallInside(24, 66, 20);             // and from the driver's side

        // ---- cabin rear wall: sx=20 sy=22 sz=2 at uv(48,64) ----
        rect(50, 64, 20, 2, BODY_DARK);
        rect(70, 64, 20, 2, BODY_DARK);
        rect(48, 66, 2, 22, BODY_DARK);
        rect(70, 66, 2, 22, BODY_DARK);
        wallInside(50, 66, 20);             // -z faces into the cabin here
        wallOutside(72, 66, 20);            // +z is the rear window

        // ---- cabin roof: sx=20 sy=1 sz=26 at uv(96,64) ----
        rect(122, 64, 20, 26, BODY);        // outside
        border(122, 64, 20, 26, BODY_DARK);
        rect(142, 64, 20, 26, INTERIOR_DARK); // headlining
        rect(96, 90, 26, 1, BODY_DARK);     // roof edges
        rect(122, 90, 20, 1, BODY_DARK);
        rect(142, 90, 26, 1, BODY_DARK);
        rect(168, 90, 20, 1, BODY_DARK);

        // ---- cabin left wall: sx=2 sy=22 sz=22 at uv(0,96) ----
        rect(22, 96, 2, 22, BODY_DARK);     // top edge
        rect(24, 96, 2, 22, BODY_DARK);     // bottom edge
        rect(22, 118, 2, 22, BODY_DARK);    // front and rear edges
        rect(46, 118, 2, 22, BODY_DARK);
        wallOutside(0, 118, 22);            // -x is the left flank
        wallInside(24, 118, 22);

        // ---- cabin right wall: sx=2 sy=22 sz=22 at uv(52,96) ----
        rect(74, 96, 2, 22, BODY_DARK);
        rect(76, 96, 2, 22, BODY_DARK);
        rect(74, 118, 2, 22, BODY_DARK);
        rect(98, 118, 2, 22, BODY_DARK);
        wallInside(52, 118, 22);            // -x faces into the cabin here
        wallOutside(76, 118, 22);           // +x is the right flank

        drawInterior();
    }

    /**
     * One face of the steering wheel, 9x9, laid out by distance from the centre
     * so the rim comes out round rather than square.
     *
     * <p>Everything that is not rim, spoke or boss is left at alpha 0. The model
     * draws on {@code entityCutoutNoCull}, so those pixels are real holes: a
     * single flat box ends up reading as a three-spoke wheel you can see the
     * road through, which is what saves this from being seven cuboids.
     */
    static void steeringWheelFace(int x, int y) {
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                double dx = i - 3;
                double dy = j - 3;
                double d = Math.sqrt(dx * dx + dy * dy);
                int c = CLEAR;
                if (d >= 2.1 && d <= 3.55) {
                    c = j <= 2 ? WHEEL_RIM_LIGHT : WHEEL_RIM;   // the top catches the light
                } else if (d <= 1.0) {
                    c = HORN_BOSS;
                } else if (j == 3 && Math.abs(dx) <= 2.5) {
                    c = WHEEL_SPOKE;                            // the two side spokes
                } else if (i == 3 && dy >= 0 && dy <= 2.5) {
                    c = WHEEL_SPOKE;                            // and one down to the bottom
                }
                if (c != CLEAR) {
                    rect(x + i, y + j, 1, 1, c);
                }
            }
        }
    }

    /** A seat back: sx=6 sy=14 sz=2, so 16x16 of sheet. */
    static void seat(int x, int y) {
        rect(x, y, 16, 16, SEAT_DARK);          // edges and underside
        rect(x + 2, y, 6, 2, SEAT_LIGHT);       // top of the headrest

        int fx = x + 2;
        int fy = y + 2;
        rect(fx, fy, 6, 4, SEAT);               // headrest
        rect(fx, fy + 4, 6, 1, INTERIOR_DARK);  // the gap under it
        rect(fx, fy + 5, 6, 9, SEAT);           // backrest
        rect(fx + 1, fy + 6, 4, 7, SEAT_LIGHT); // centre panel
        rect(fx + 2, fy + 6, 1, 7, SEAT_DARK);  // stitching
        rect(fx + 3, fy + 6, 1, 7, SEAT_DARK);

        rect(x + 10, fy, 6, 14, SEAT_DARK);     // the back of the seat
    }

    /**
     * Dashboard, steering wheel, seats, and the seat pads painted onto the floor
     * pan. Runs after the bodywork because the pads overwrite part of the body's
     * own top face — only the rows between v 10 and v 32, which are the cabin
     * floor; outside that band the same face is the bonnet and the boot lid.
     */
    static void drawInterior() {
        // ---- dashboard: sx=16 sy=6 sz=2 at uv(0,144) -> 36x8 ----
        rect(0, 144, 36, 8, INTERIOR);
        rect(2, 144, 16, 2, INTERIOR);                      // top surface
        rect(2, 144, 16, 1, mix(INTERIOR, 0xFFFFFF, 0.16F)); // and its highlight
        rect(18, 144, 16, 2, INTERIOR_DARK);                // underside
        rect(2, 146, 16, 6, INTERIOR_DARK);                 // face towards the windscreen

        // The +z face at (20,146) is the side the driver reads. Its u runs the
        // other way round the box than -z does, so model x maps to 20 + (8 - x)
        // and the driver at x -3.5 lands at u 31.5.
        rect(20, 146, 16, 6, INTERIOR);
        rect(20, 146, 16, 1, mix(INTERIOR, 0xFFFFFF, 0.16F));
        rect(22, 148, 5, 2, INTERIOR_DARK);                 // vents, passenger side
        rect(29, 147, 6, 4, GAUGE_FACE);                    // instrument binnacle
        rect(30, 148, 2, 2, mix(GAUGE_FACE, GAUGE_LIT, 0.25F));
        rect(32, 148, 2, 2, mix(GAUGE_FACE, GAUGE_LIT, 0.25F));
        rect(30, 148, 1, 1, GAUGE_LIT);                     // the two needles
        rect(33, 148, 1, 1, GAUGE_LIT);

        // ---- steering column: sx=2 sy=2 sz=3 at uv(44,144) -> 10x5 ----
        rect(44, 144, 10, 5, INTERIOR_DARK);
        rect(47, 144, 2, 3, INTERIOR);                      // top of the shroud

        // ---- steering wheel: sx=7 sy=7 sz=1 at uv(68,144) -> 16x8 ----
        steeringWheelFace(69, 145);                         // away from the driver
        steeringWheelFace(77, 145);                         // and the side he holds
        // The four 1px edge strips, inked only where the rim actually reaches the
        // bounding square, so the wheel keeps a visible thickness edge-on.
        rect(71, 144, 3, 1, WHEEL_RIM_LIGHT);               // top face
        rect(78, 144, 3, 1, WHEEL_RIM);                     // bottom face
        rect(68, 147, 1, 3, WHEEL_RIM);                     // -x face
        rect(76, 147, 1, 3, WHEEL_RIM);                     // +x face

        // ---- seats: sx=6 sy=14 sz=2 at uv(96,144) and uv(116,144) ----
        seat(96, 144);
        seat(116, 144);

        // ---- cabin floor, on the body's own top face ----
        // u = 40 + (x + 10), v = (z + 20); the cabin is z -10..12.
        rect(40, 10, 20, 22, INTERIOR);
        rect(40, 10, 20, 8, INTERIOR_DARK);     // footwells, under the dashboard
        rect(44, 18, 5, 10, SEAT);              // driver's cushion
        rect(51, 18, 5, 10, SEAT);              // passenger's
        rect(45, 19, 3, 8, SEAT_LIGHT);
        rect(52, 19, 3, 8, SEAT_LIGHT);
        rect(49, 10, 2, 22, INTERIOR_DARK);     // transmission tunnel
    }


    // ---- the tanker's barrel ----
    //
    // Painted into every colour's sheet even though only the tanker model uses
    // these rectangles: one entity texture per dye, two models that read from
    // it. A car simply never asks for this corner of the sheet.

    static final int TANK = 0xFF9AA0A6;
    static final int TANK_DARK = 0xFF5C6166;
    static final int TANK_LIGHT = 0xFFC3C8CC;
    static final int HAZARD_A = 0xFFE8B021;

    /** Where each face of a box lands on the sheet, in Minecraft's own layout. */
    static int[] face(int u, int v, int sx, int sy, int sz, String which) {
        switch (which) {
            case "top":    return new int[] {u + sz, v, sx, sz};
            case "bottom": return new int[] {u + sz + sx, v, sx, sz};
            case "-x":     return new int[] {u, v + sz, sz, sy};
            case "-z":     return new int[] {u + sz, v + sz, sx, sy};
            case "+x":     return new int[] {u + sz + sx, v + sz, sz, sy};
            default:       return new int[] {u + sz + sx + sz, v + sz, sx, sy};
        }
    }

    static void fillFace(int u, int v, int sx, int sy, int sz, String which, int color) {
        int[] f = face(u, v, sx, sy, sz, which);
        rect(f[0], f[1], f[2], f[3], color);
    }

    /** A barrel side: ribs, a painted stripe and a bright top edge. */
    static void barrelSide(int u, int v, int w, int h) {
        rect(u, v, w, h, TANK);
        rect(u, v, w, 2, TANK_LIGHT);               // the curve catching the light
        rect(u, v + h - 3, w, 3, TANK_DARK);        // and the shaded underside
        for (int x = u + 3; x < u + w - 2; x += 6) {
            rect(x, v + 2, 1, h - 5, TANK_DARK);    // ribs
        }
        rect(u, v + h / 2 - 1, w, 3, BODY);         // the stripe takes the paint
        rect(u, v + h / 2 - 1, w, 1, BODY_LIGHT);
    }

    static void drawTanker() {
        // ---- barrel: sx=18 sy=18 sz=18 at uv(0,168) ----
        int u = 0;
        int v = 168;
        rect(u, v, 2 * (18 + 18), 18 + 18, TANK_DARK);
        int[] left = face(u, v, 18, 18, 18, "-x");
        barrelSide(left[0], left[1], left[2], left[3]);
        int[] right = face(u, v, 18, 18, 18, "+x");
        barrelSide(right[0], right[1], right[2], right[3]);

        int[] top = face(u, v, 18, 18, 18, "top");
        rect(top[0], top[1], top[2], top[3], TANK);
        rect(top[0], top[1], top[2], 3, TANK_DARK);
        rect(top[0] + 6, top[1] + 4, 6, 6, TANK_DARK);          // the filler ring
        rect(top[0] + 7, top[1] + 5, 4, 4, TANK_LIGHT);
        rect(top[0] + 2, top[1] + 12, 14, 4, TANK_DARK);        // the walkway
        for (int x = 0; x < 14; x += 3) {
            rect(top[0] + 2 + x, top[1] + 12, 1, 4, TANK_LIGHT);
        }

        // The back of the barrel is where the work happens: valve, hose, warning.
        int[] back = face(u, v, 18, 18, 18, "+z");
        rect(back[0], back[1], back[2], back[3], TANK);
        rect(back[0], back[1], back[2], 2, TANK_LIGHT);
        rect(back[0] + 4, back[1] + 4, 10, 8, TANK_DARK);       // the valve plate
        rect(back[0] + 6, back[1] + 6, 2, 2, BODY);
        rect(back[0] + 10, back[1] + 6, 2, 2, BODY);
        rect(back[0] + 5, back[1] + 9, 8, 2, INTERIOR_DARK);    // the hose coiled on it
        for (int x = 0; x < 18; x += 4) {                        // hazard chevrons
            rect(back[0] + x, back[1] + 14, 2, 4, HAZARD_A);
        }

        // The front butts against the cabin; nothing there is ever seen.
        fillFace(u, v, 18, 18, 18, "-z", TANK_DARK);

        // ---- deck rail: sx=16 sy=4 sz=12 at uv(80,168) ----
        rect(80, 168, 2 * (16 + 12), 12 + 4, BODY_DARK);
        int[] deckTop = face(80, 168, 16, 4, 12, "top");
        rect(deckTop[0], deckTop[1], deckTop[2], deckTop[3], BODY_DARK);
        for (int z = 0; z < 12; z += 3) {
            rect(deckTop[0], deckTop[1] + z, 16, 1, BODY);      // cross members
        }
        for (String side : new String[] {"-x", "+x"}) {
            int[] f = face(80, 168, 16, 4, 12, side);
            rect(f[0], f[1], f[2], f[3], BODY_DARK);
            rect(f[0], f[1], f[2], 1, BODY);
        }

        // ---- filler cap: sx=6 sy=2 sz=6 at uv(150,168) ----
        rect(150, 168, 2 * (6 + 6), 6 + 2, TANK_DARK);
        int[] capTop = face(150, 168, 6, 2, 6, "top");
        rect(capTop[0], capTop[1], capTop[2], capTop[3], TANK_LIGHT);
        rect(capTop[0] + 1, capTop[1] + 1, 4, 4, TANK);
        rect(capTop[0] + 2, capTop[1] + 2, 2, 2, HAZARD_A);
    }

    static void drawItemTexture() {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        rect(1, 9, 14, 4, BODY);            // lower body
        rect(3, 2, 10, 7, BODY);            // tall cabin
        rect(4, 4, 8, 3, GLASS_LIGHT);      // windows
        rect(1, 9, 14, 1, BODY_LIGHT);      // belt line highlight
        rect(1, 12, 14, 1, BODY_DARK);
        rect(3, 13, 3, 3, TIRE);            // front wheel
        rect(10, 13, 3, 3, TIRE);           // rear wheel
        rect(4, 14, 1, 1, HUB);
        rect(11, 14, 1, 1, HUB);
        rect(14, 10, 1, 1, LIGHT);          // headlight
    }

    static void save(File out) throws Exception {
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
        System.out.println("wrote " + out.getPath());
    }

    /** Must match {@code CarEntity.DEFAULT_COLOR}; an unpainted car shows this. */
    static final String DEFAULT_DYE = "red";

    static void writeText(File out, String body) throws Exception {
        out.getParentFile().mkdirs();
        try (PrintWriter w = new PrintWriter(out, "UTF-8")) {
            w.print(body);
        }
        System.out.println("wrote " + out.getPath());
    }

    // The item's model and definition come from tools/GenItemModels.java,
    // which builds one 3D car and tints it per dye.

    // ---- the pickup's bed ----
    //
    // Same arrangement as the barrel above: painted into every colour's sheet,
    // read only by the pickup's model. The walls take the paint on the outside
    // and show their planks on the inside, which is how you can tell at a
    // glance which way round you are looking at one.

    static final int WOOD = 0xFF9A6B3F;
    static final int WOOD_DARK = 0xFF6B4A2C;
    static final int WOOD_LIGHT = 0xFFB98A55;
    static final int FRAME = 0xFF5C6166;

    /** Bare planks, with their joints running the length of the face. */
    static void planks(int[] f, boolean lengthwise) {
        rect(f[0], f[1], f[2], f[3], WOOD);
        if (lengthwise) {
            for (int y = f[1] + 3; y < f[1] + f[3] - 1; y += 4) {
                rect(f[0], y, f[2], 1, WOOD_DARK);
            }
        } else {
            for (int x = f[0] + 3; x < f[0] + f[2] - 1; x += 4) {
                rect(x, f[1], 1, f[3], WOOD_DARK);
            }
        }
        rect(f[0], f[1], f[2], 1, WOOD_LIGHT);
    }

    /** A painted outer panel with a bright top rail and a shaded foot. */
    static void bedPanel(int[] f) {
        rect(f[0], f[1], f[2], f[3], BODY);
        rect(f[0], f[1], f[2], 1, BODY_LIGHT);
        rect(f[0], f[1] + f[3] - 2, f[2], 2, BODY_DARK);
        rect(f[0], f[1] + 2, f[2], 1, BODY_DARK);   // the pressing down the side
    }

    static void drawPickup() {
        // ---- floor: sx=20 sy=2 sz=20 at uv(0,204) ----
        int u = 0;
        int v = 204;
        rect(u, v, 2 * (20 + 20), 20 + 2, WOOD_DARK);
        planks(face(u, v, 20, 2, 20, "top"), false);        // the deck you load
        fillFace(u, v, 20, 2, 20, "bottom", FRAME);

        // ---- side walls: sx=2 sy=12 sz=22 ----
        for (int[] wall : new int[][] {{84, 204, -1}, {136, 204, 1}}) {
            int wu = wall[0];
            int wv = wall[1];
            rect(wu, wv, 2 * (2 + 22), 22 + 12, BODY_DARK);
            String outside = wall[2] < 0 ? "-x" : "+x";
            String inside = wall[2] < 0 ? "+x" : "-x";
            bedPanel(face(wu, wv, 2, 12, 22, outside));
            planks(face(wu, wv, 2, 12, 22, inside), true);
            int[] cap = face(wu, wv, 2, 12, 22, "top");
            rect(cap[0], cap[1], cap[2], cap[3], BODY_LIGHT);   // the rail cap
        }

        // ---- tailgate: sx=20 sy=12 sz=2 at uv(188,204) ----
        int tu = 188;
        int tv = 204;
        rect(tu, tv, 2 * (20 + 2), 2 + 12, BODY_DARK);
        int[] out = face(tu, tv, 20, 12, 2, "+z");
        bedPanel(out);
        rect(out[0] + 7, out[1] + 4, 6, 3, FRAME);              // the latch
        rect(out[0] + 8, out[1] + 5, 4, 1, BODY_LIGHT);
        planks(face(tu, tv, 20, 12, 2, "-z"), true);
        int[] tcap = face(tu, tv, 20, 12, 2, "top");
        rect(tcap[0], tcap[1], tcap[2], tcap[3], BODY_LIGHT);

        // ---- the rail under the overhang: sx=18 sy=4 sz=14 at uv(188,224) ----
        int du = 188;
        int dv = 224;
        rect(du, dv, 2 * (18 + 14), 14 + 4, BODY_DARK);
        for (String side : new String[] {"-x", "+x", "+z"}) {
            int[] f = face(du, dv, 18, 4, 14, side);
            rect(f[0], f[1], f[2], f[3], BODY_DARK);
            rect(f[0], f[1], f[2], 1, BODY);            // the sill catching light
            rect(f[0], f[1] + f[3] - 1, f[2], 1, FRAME);
        }
        fillFace(du, dv, 18, 4, 14, "bottom", FRAME);
    }

    public static void main(String[] args) throws Exception {
        for (Object[] dye : DYES) {
            String name = (String) dye[0];
            setBody((Integer) dye[1]);

            drawEntityTexture();
            drawTanker();
            drawPickup();
            save(new File("src/main/resources/assets/overthought_vehicles/textures/entity/car/car_" + name + ".png"));

            // The icon is painted too, so a stack shows the colour its tooltip
            // claims. One icon per dye rather than a tint over a grey one,
            // because these share the entity texture's own shading.
            drawItemTexture();
            save(new File("src/main/resources/assets/overthought_vehicles/textures/item/car_" + name + ".png"));
        }

    }
}
