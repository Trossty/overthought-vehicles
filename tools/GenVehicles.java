import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws the three real cars the mod ships as data-pack vehicles, and writes
 * everything that follows from the drawing:
 *
 *   data/car/car/vehicle/NAME.json      the definition the game loads
 *   assets/car/models/item/vehicle_NAME.json   the same boxes, as an item icon
 *   assets/car/textures/item/vehicle_palette.png   the icons' colours
 *
 * They are ordinary definitions: nothing here is out of reach of a data pack,
 * which is the point of drawing them this way. Lengths are sixteenths of a
 * block, x across, y up, z towards the front.
 *
 * Run from the project root:  java tools/GenVehicles.java
 */
public class GenVehicles {

    static final String BLACK = "#1B1B1D";
    static final String TRIM = "#2A2A2C";
    static final String CHROME = "#B4B8BD";
    static final String LAMP = "#F4F0D0";
    static final String TAIL = "#C0201C";
    /** The bright middle of a lamp. */
    static final String LAMP_GLOW = "#FFFDF2";
    static final String TAIL_GLOW = "#EE4A3C";
    /** See-through: the cabin shows behind it, faintly blue. */
    static final String GLASS = "#55A9D3E6";
    /** Bumpers, wing flares, a hardtop: the pale trim that frames the paint. */
    static final String ACCENT = "#E3E1DA";
    /** Bumpers: satin grey, with a black rubbing strip along the face. */
    static final String BUMPER = "#8C9197";
    static final String SEAT = "#3B3B40";
    /** The cloth let into a seat between its bolsters. */
    static final String SEAT_CLOTH = "#566070";
    static final String CARPET = "#2C2B2E";
    static final String DIAL = "#E6E1CF";
    static final String NEEDLE = "#D8402C";
    /** The radio's lit display. */
    static final String DISPLAY = "#3FB6A8";
    static final String AMBER = "#E8901B";

    /** Where a tanker's load is drawn, by the renderer, in whatever it is carrying. */
    static final String CARGO = "cargo";
    /** A tanker's sight glass: clearer than a window, so the load reads through it. */
    static final String SIGHT_GLASS = "#30DCE8F0";
    /** The Atego barrel's sight section, between its middle bands. */
    static final double SIGHT_FROM = -9.5;
    static final double SIGHT_TO = 16.0;

    /**
     * How the cars and lorries are drawn out lengthways, between the axles
     * and beyond them. The toy-car squeeze (0.78 and 0.55) made every one of
     * them read short and tall, a hatchback on stilts; this is a touch longer
     * than written instead, the Corolla coming out at 5.2 blocks. Lengthways
     * only: the stance is untouched, so nothing sits any higher.
     */
    static final double LONG_BETWEEN = 1.08;
    static final double LONG_OVERHANG = 1.12;

    /**
     * One box of the body. A panel also keeps the two profile points it was
     * drawn between ({@code yA, zA, yB, zB, thick}), so it can be drawn again
     * between the same points once the body has been squeezed.
     */
    record Box(double x1, double y1, double z1, double x2, double y2, double z2, String color, double tilt,
               double[] profile, boolean steers) {
        Box(double x1, double y1, double z1, double x2, double y2, double z2, String color, double tilt) {
            this(x1, y1, z1, x2, y2, z2, color, tilt, null, false);
        }

        Box(double x1, double y1, double z1, double x2, double y2, double z2, String color, double tilt,
            double[] profile) {
            this(x1, y1, z1, x2, y2, z2, color, tilt, profile, false);
        }

        /** The same box, turning with the handlebars or not. */
        Box steering(boolean steers) {
            return new Box(x1, y1, z1, x2, y2, z2, color, tilt, profile, steers);
        }
    }

    /** A panel from one point of the profile to another, as one leant box. */
    static Box slab(double x1, double x2, double yA, double zA, double yB, double zB, double thick, String color) {
        double cy = (yA + yB) / 2;
        double cz = (zA + zB) / 2;
        double length = Math.hypot(yB - yA, zB - zA);
        double[] profile = {yA, zA, yB, zB, thick};
        if (Math.abs(yB - yA) >= Math.abs(zB - zA)) {
            // Mostly upright: a tall box, tipped back.
            double bottomZ = yA < yB ? zA : zB;
            double topZ = yA < yB ? zB : zA;
            double tilt = Math.toDegrees(Math.atan2(bottomZ - topZ, Math.abs(yB - yA)));
            return new Box(x1, cy - length / 2, cz - thick / 2, x2, cy + length / 2, cz + thick / 2,
                    color, tilt, profile);
        }
        // Mostly level: a long box, nose up or down.
        double rearY = zA < zB ? yA : yB;
        double frontY = zA < zB ? yB : yA;
        double tilt = Math.toDegrees(Math.atan2(frontY - rearY, Math.abs(zB - zA)));
        return new Box(x1, cy - thick / 2, cz - length / 2, x2, cy + thick / 2, cz + length / 2,
                color, tilt, profile);
    }

    record Wheel(double x, double y, double z, double radius, double width, boolean steers) {
    }

    static class Vehicle {
        final String name;
        String chassis = "car";
        String dye = "white";
        int paint = 0xF9FFFE;
        double width = 1.6;
        double height = 2.1875;
        String performance = "";
        String factory = "";
        /** The fuel tank, in litres, as the real thing has. */
        double tank = 50;
        /** How far down the road the head lamps light, and how wide, in blocks; 0 leaves the default. */
        double lightLength = 0;
        double lightSpread = 0;
        /** What the workstation takes on top of the chassis parts: item id, count, item id, count... */
        String[] materials = {};
        /** Every box and axle is drawn this much wider than written. */
        double stretch = 1;
        /** The wheels are drawn at this size of what is written (under 1: smaller, to suit the short body), and the body moved up or down with the tops of the tyres. */
        double grow = 1.18;
        /** And this much wider, outwards. */
        double widen = 1.1;
        /**
         * The toy-car squeeze: lengthways, the body is kept as written round
         * each wheel arch and shortened elsewhere, the overhangs most. Short
         * and stubby, like the toy, with every arch still round its tyre.
         */
        double between = 0.78;
        double overhang = 0.55;
        /** Where the axles were written, fixed before anything moves. */
        private double[] axleZ;
        final List<double[]> seats = new ArrayList<>();
        final List<Wheel> wheels = new ArrayList<>();
        final List<Box> boxes = new ArrayList<>();

        /** While set, the boxes added turn with the front wheel: fork, bars, headset. */
        boolean steering;

        Vehicle(String name) {
            this.name = name;
        }

        void box(double x1, double y1, double z1, double x2, double y2, double z2, String color) {
            this.add(x1, y1, z1, x2, y2, z2, color, 0);
        }

        void add(double x1, double y1, double z1, double x2, double y2, double z2, String color, double tilt) {
            this.boxes.add(new Box(x1 * this.stretch, y1, z1, x2 * this.stretch, y2, z2, color, tilt)
                    .steering(this.steering));
        }

        /** The same box on both sides of the car. */
        void pair(double x1, double y1, double z1, double x2, double y2, double z2, String color) {
            this.box(x1, y1, z1, x2, y2, z2, color);
            this.box(-x2, y1, z1, -x1, y2, z2, color);
        }

        /**
         * A flat panel from one point of the car's profile to another: a
         * bonnet, a windscreen pillar, a rear window. It is one box, leant
         * over as far as the two points say.
         */
        void panel(double x1, double x2, double yA, double zA, double yB, double zB,
                   double thick, String color) {
            this.boxes.add(slab(x1 * this.stretch, x2 * this.stretch, yA, zA, yB, zB, thick, color)
                    .steering(this.steering));
        }

        void panels(double x1, double x2, double yA, double zA, double yB, double zB,
                    double thick, String color) {
            this.panel(x1, x2, yA, zA, yB, zB, thick, color);
            this.panel(-x2, -x1, yA, zA, yB, zB, thick, color);
        }

        void axle(double x, double z, double radius, double width, boolean steers) {
            double out = x * this.stretch + (this.stretch - 1) * width / 2;
            this.wheels.add(new Wheel(-out, radius, z, radius, width, steers));
            this.wheels.add(new Wheel(out, radius, z, radius, width, steers));
        }

        /** One wheel on the centre line, for a two-wheeler. */
        void wheel(double z, double radius, double width, boolean steers) {
            this.wheels.add(new Wheel(0, radius, z, radius, width, steers));
        }

        void seat(double x, double y, double z) {
            this.seats.add(new double[] {x, y, z});
        }

        /**
         * Every lamp gets a black bezel round it and a brighter middle, so it
         * reads as a lamp in a housing rather than a painted patch. A lamp is
         * a thin plate: its thin side is the way it faces, outward.
         */
        void lamps() {
            List<Box> added = new ArrayList<>();
            for (Box b : this.boxes) {
                boolean head = b.color().equals(LAMP);
                if ((!head && !b.color().equals(TAIL)) || b.tilt() != 0 || b.profile() != null) {
                    continue;
                }
                double[] lo = {Math.min(b.x1(), b.x2()), Math.min(b.y1(), b.y2()), Math.min(b.z1(), b.z2())};
                double[] hi = {Math.max(b.x1(), b.x2()), Math.max(b.y1(), b.y2()), Math.max(b.z1(), b.z2())};
                int thin = 0;
                for (int a = 1; a < 3; a++) {
                    if (hi[a] - lo[a] < hi[thin] - lo[thin]) {
                        thin = a;
                    }
                }
                if (thin == 1 || hi[thin] - lo[thin] > 1.0) {
                    continue;
                }
                double out = Math.signum(lo[thin] + hi[thin]);
                double[] bl = lo.clone();
                double[] bh = hi.clone();
                double[] il = lo.clone();
                double[] ih = hi.clone();
                for (int a = 0; a < 3; a++) {
                    if (a == thin) {
                        bl[a] -= out * 0.1;
                        bh[a] -= out * 0.1;
                        il[a] += out * 0.05;
                        ih[a] += out * 0.05;
                    } else {
                        bl[a] -= 0.25;
                        bh[a] += 0.25;
                        double inset = (hi[a] - lo[a]) * 0.25;
                        il[a] += inset;
                        ih[a] -= inset;
                    }
                }
                added.add(new Box(bl[0], bl[1], bl[2], bh[0], bh[1], bh[2], BLACK, 0).steering(b.steers()));
                added.add(new Box(il[0], il[1], il[2], ih[0], ih[1], ih[2], head ? LAMP_GLOW : TAIL_GLOW, 0)
                        .steering(b.steers()));
            }
            this.boxes.addAll(added);
        }

        /**
         * A bumper: a satin grey bar and, a little proud of its outer face, a
         * black rubbing strip along the middle. The outer face is the end
         * further from the middle of the car.
         */
        void bumper(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.box(x1, y1, z1, x2, y2, z2, BUMPER);
            double mid = (y1 + y2) / 2;
            double strip = Math.min(0.5, (y2 - y1) / 4);
            boolean front = z1 + z2 > 0;
            double face = front ? Math.max(z1, z2) : Math.min(z1, z2);
            double proud = front ? 0.15 : -0.15;
            this.box(x1 + 0.3, mid - strip, Math.min(face, face + proud), x2 - 0.3, mid + strip,
                    Math.max(face, face + proud), BLACK);
        }

        // ---- the cabin: seats, dashboard, floor ----

        /**
         * Two front seats, the driver's at {@code +x}: a cushion from
         * {@code floor} to {@code top} reaching forward to {@code front}, and a
         * backrest from {@code back} up to {@code backTop}. Each is dark
         * bolsters either side of a panel of lighter cloth, and a headrest
         * stands on two posts if asked for.
         */
        void frontSeats(double floor, double top, double back, double front, double backTop, boolean headrest) {
            double out = 2.8, in = 4.0, mid = 9.6, edge = 10.8;
            // the cushion: base, cloth, raised bolsters, a front lip
            this.pair(out, floor, back + 1.4, edge, top - 0.5, front, SEAT);
            this.pair(in, top - 0.5, back + 1.6, mid, top, front - 0.5, SEAT_CLOTH);
            this.pair(out, top - 0.5, back + 1.4, in, top + 0.5, front, SEAT);
            this.pair(mid, top - 0.5, back + 1.4, edge, top + 0.5, front, SEAT);
            this.pair(in, top - 0.5, front - 0.5, mid, top, front, SEAT);
            // the backrest
            this.pair(out, top, back, edge, backTop, back + 1.0, SEAT);
            this.pair(in, top + 0.8, back + 1.0, mid, backTop - 1.0, back + 1.3, SEAT_CLOTH);
            this.pair(out, top, back + 1.0, in, backTop - 0.4, back + 1.9, SEAT);
            this.pair(mid, top, back + 1.0, edge, backTop - 0.4, back + 1.9, SEAT);
            if (headrest) {
                this.pair(5.6, backTop, back + 0.3, 6.0, backTop + 0.8, back + 0.7, CHROME);
                this.pair(7.6, backTop, back + 0.3, 8.0, backTop + 0.8, back + 0.7, CHROME);
                this.pair(4.8, backTop + 0.8, back - 0.1, 8.8, backTop + 3.2, back + 1.1, SEAT);
                this.pair(5.4, backTop + 1.3, back + 1.1, 8.2, backTop + 2.7, back + 1.25, SEAT_CLOTH);
            }
        }

        /**
         * A bench across the car, {@code half} either side of the middle,
         * stitched into {@code places} seats: a panel of cloth for each, on
         * the cushion and on the back, with the frame showing between them.
         */
        void rearBench(double half, double floor, double top, double back, double front, double backTop,
                       int places) {
            this.box(-half, floor, back, half, top - 0.4, front, SEAT);
            this.box(-half, top - 0.4, back, half, backTop, back + 1.2, SEAT);
            this.box(-half, top - 0.4, back + 1.2, -half + 0.9, top + 0.3, front, SEAT);
            this.box(half - 0.9, top - 0.4, back + 1.2, half, top + 0.3, front, SEAT);
            double width = 2 * half / places;
            for (int i = 0; i < places; i++) {
                double a = -half + width * i + 0.7;
                double b = -half + width * (i + 1) - 0.7;
                this.box(Math.max(a, -half + 0.9), top - 0.4, back + 1.4, Math.min(b, half - 0.9), top, front - 0.5,
                        SEAT_CLOTH);
                this.box(a + 0.1, top + 0.6, back + 1.2, b - 0.1, backTop - 0.9, back + 1.5, SEAT_CLOTH);
            }
        }

        /**
         * The dashboard: a moulding {@code half} either side of the middle,
         * from {@code low} to {@code high}, its face at {@code face} towards
         * the driver and running forward to {@code bulkhead}. On it a centre
         * stack with the radio and two vents, a vent at either end and the
         * glovebox; below it a console down to the floor and back between the
         * front seats to {@code seatBack}, with the gear lever and handbrake.
         */
        void dash(double half, double low, double high, double face, double bulkhead,
                  double floor, double seatBack) {
            this.box(-half, low, face, half, high, bulkhead, TRIM);
            // the footwell: carpet up the bulkhead under the dash, so no paint shows inside
            this.box(-half, floor, face, half, low, bulkhead, CARPET);
            this.box(-half, high - 0.5, face - 0.4, half, high, face, BLACK);       // the padded top
            // centre stack: radio display and two vents
            this.box(-2.4, low + 0.3, face - 0.3, 2.4, high - 0.5, face, BLACK);
            this.box(-1.7, low + 0.6, face - 0.4, 1.7, low + 1.5, face - 0.3, DISPLAY);
            this.pair(0.3, high - 1.5, face - 0.4, 1.9, high - 0.8, face - 0.3, CHROME);
            // a vent at either end, and the glovebox on the passenger's side
            this.pair(10.0, high - 1.5, face - 0.25, half - 1.0, high - 0.7, face, BLACK);
            this.box(-9.5, low + 0.4, face - 0.15, -3.4, high - 1.7, face, "#232326");
            this.box(-6.9, high - 2.3, face - 0.25, -6.0, high - 2.0, face - 0.15, CHROME);
            // the console, from the stack down to the floor and back between the seats
            this.box(-2.2, floor, face - 2.5, 2.2, low + 0.3, face, TRIM);
            this.box(-2.0, floor, seatBack + 1.4, 2.0, floor + 2.4, face - 2.5, TRIM);
            double lever = face - 4.5;
            this.box(-0.2, floor + 2.4, lever - 0.2, 0.2, floor + 4.4, lever + 0.2, CHROME);
            this.box(-0.55, floor + 4.4, lever - 0.55, 0.55, floor + 5.2, lever + 0.55, BLACK);
            this.box(-0.35, floor + 2.4, lever - 5.0, 0.35, floor + 3.0, lever - 2.0, BLACK);
        }

        /**
         * The instrument binnacle in front of the driver: a black hood
         * {@code height} tall leant back {@code tilt} degrees, and in it two
         * pale dials, the speedometer and the rev counter, each with its
         * needle.
         */
        void gauges(double x, double y, double z, double height, double tilt) {
            this.add(x, y, z, x + 5, y + height, z + 0.7, BLACK, tilt);
            double dial = Math.min(2.0, height - 0.8);
            double dy = y + (height - dial) / 2;
            for (double dx : new double[] {x + 0.4, x + 4.6 - dial}) {
                this.add(dx, dy, z - 0.12, dx + dial, dy + dial, z, DIAL, tilt);
                double cx = dx + dial / 2;
                this.add(cx - 0.1, dy + dial / 2, z - 0.18, cx + 0.1, dy + dial - 0.25, z - 0.12, NEEDLE, tilt);
            }
        }

        /** Dark carpet over the cabin floor. */
        void carpet(double half, double floor, double back, double front) {
            this.box(-half, floor, back, half, floor + 0.12, front, CARPET);
        }

        static double lerp(double a, double b, double t) {
            return a + (b - a) * t;
        }

        /**
         * Glass in a side window, both sides. The opening runs from
         * {@code yLow} to {@code yHigh}; its rear edge goes from
         * {@code rearLow} (at the bottom) to {@code rearHigh} (at the top) and
         * its front edge likewise, so a raked pillar is followed in steps
         * the pillar itself hides.
         */
        void sideWindows(double x, double yLow, double yHigh,
                         double rearLow, double rearHigh, double frontLow, double frontHigh) {
            int bands = 6;
            for (int i = 0; i < bands; i++) {
                double ta = (double) i / bands;
                double tb = (double) (i + 1) / bands;
                double rear = Math.max(lerp(rearLow, rearHigh, ta), lerp(rearLow, rearHigh, tb));
                double front = Math.min(lerp(frontLow, frontHigh, ta), lerp(frontLow, frontHigh, tb));
                if (front > rear) {
                    this.pair(x - 0.15, lerp(yLow, yHigh, ta), rear, x + 0.15, lerp(yLow, yHigh, tb), front, GLASS);
                }
            }
        }

        /**
         * A windscreen from {@code (yA, zA)} to {@code (yB, zB)} in profile,
         * {@code half} either side of the middle.
         */
        void screen(double half, double yA, double zA, double yB, double zB) {
            this.panel(-half, half, yA, zA, yB, zB, 0.3, GLASS);
        }

        /**
         * The toy-car stance: wheels bigger and fatter than written, and the
         * whole body lifted by as much as the tops of the wheels rose, so
         * every arch still clears its tyre and the car stands tall on them.
         */
        void finish() {
            this.lamps();
            this.axleZ = this.wheels.stream().mapToDouble(Wheel::z).distinct().sorted().toArray();
            double rise = this.wheels.get(0).radius() * (this.grow - 1) * 2;
            this.boxes.replaceAll(b -> {
                if (b.profile() != null) {
                    double[] p = b.profile();
                    return slab(b.x1(), b.x2(), p[0] + rise, this.squeeze(p[1]), p[2] + rise, this.squeeze(p[3]),
                            p[4], b.color()).steering(b.steers());
                }
                double z1;
                double z2;
                if (b.z2() - b.z1() < 1.0 || b.tilt() != 0) {
                    // thin things and leant ones keep their shape and only move
                    double shift = this.squeeze((b.z1() + b.z2()) / 2) - (b.z1() + b.z2()) / 2;
                    z1 = b.z1() + shift;
                    z2 = b.z2() + shift;
                } else {
                    z1 = this.squeeze(b.z1());
                    z2 = this.squeeze(b.z2());
                }
                return new Box(b.x1(), b.y1() + rise, z1, b.x2(), b.y2() + rise, z2, b.color(), b.tilt())
                        .steering(b.steers());
            });
            for (double[] seat : this.seats) {
                seat[1] += rise;
                seat[2] = this.squeeze(seat[2]);
            }
            this.wheels.replaceAll(w -> new Wheel(w.x(), w.y(), this.squeeze(w.z()), w.radius(), w.width(),
                    w.steers()));
            this.wheels.replaceAll(w -> {
                double radius = w.radius() * this.grow;
                double width = w.width() * this.widen;
                double out = Math.signum(w.x()) * (width - w.width()) / 2;
                return new Wheel(w.x() + out, radius, w.z(), radius, width, w.steers());
            });
            this.height += rise / 16;
        }

        /**
         * Where a point {@code z} along the car ends up after the squeeze:
         * unmoved in slope round each axle (out to the tyre and a little
         * more), {@link #between} between the axles and {@link #overhang}
         * beyond them, with the middle of the wheelbase standing still.
         */
        double squeeze(double z) {
            double[] axles = this.axleZ;
            double reach = this.wheels.get(0).radius() * Math.max(1.0, this.grow) + 2.0;
            double first = axles[0];
            double last = axles[axles.length - 1];
            double middle = (first + last) / 2;
            return middle + this.travel(middle, z, axles, reach, first, last);
        }

        /** The squeezed distance from {@code from} to {@code to}, integrating the slope. */
        private double travel(double from, double to, double[] axles, double reach, double first, double last) {
            double sign = Math.signum(to - from);
            double lo = Math.min(from, to);
            double hi = Math.max(from, to);
            // cut points where the slope changes
            List<Double> cuts = new ArrayList<>(List.of(lo, hi));
            for (double axle : axles) {
                for (double edge : new double[] {axle - reach, axle + reach}) {
                    if (edge > lo && edge < hi) {
                        cuts.add(edge);
                    }
                }
            }
            cuts.sort(null);
            double total = 0;
            for (int i = 0; i + 1 < cuts.size(); i++) {
                double a = cuts.get(i);
                double b = cuts.get(i + 1);
                double mid = (a + b) / 2;
                double slope;
                boolean nearAxle = false;
                for (double axle : axles) {
                    nearAxle |= Math.abs(mid - axle) <= reach;
                }
                if (nearAxle) {
                    slope = 1.0;
                } else if (mid > first && mid < last) {
                    slope = this.between;
                } else {
                    slope = this.overhang;
                }
                total += (b - a) * slope;
            }
            return sign * total;
        }

        /**
         * The body over one axle: dark and narrow behind the wheel, full
         * width above it, and the corners of the opening stepped in so that
         * it reads as an arch and not as a doorway.
         */
        void arch(double half, double inner, double bottom, double top, double crown,
                  double z, double reach) {
            double z1 = z - reach;
            double z2 = z + reach;
            this.box(-inner, bottom, z1, inner, crown, z2, "paint_dark");
            this.box(-half, crown, z1, half, top, z2, "paint");
            this.pair(inner, crown - 3.2, z1, half, crown, z1 + 1.0, "paint");
            this.pair(inner, crown - 1.4, z1 + 1.0, half, crown, z1 + 2.4, "paint");
            this.pair(inner, crown - 3.2, z2 - 1.0, half, crown, z2, "paint");
            this.pair(inner, crown - 1.4, z2 - 2.4, half, crown, z2 - 1.0, "paint");
        }
    }

    // ------------------------------------------------------------------
    // Toyota Corolla: the three-box saloon, drawn to its real proportions:
    // five blocks long, under two wide, and low enough to see over.
    // ------------------------------------------------------------------
    static Vehicle corolla() {
        Vehicle v = new Vehicle("toyota_corolla");
        v.tank = 50;
        v.dye = "cyan";
        v.paint = 0x169C9C;
        v.width = 1.9;
        v.height = 1.8;
        v.stretch = 15.0 / 14.0;
        // A saloon squeezed like a toy read as a hatchback on stilts, so this
        // one is drawn out instead: a longer wheelbase and full overhangs,
        // 5.2 blocks nose to tail, long enough for the three boxes to read
        // as three boxes. Lengthways only — the stance (grow) is the same,
        // so it sits no higher.
        v.between = LONG_BETWEEN;
        v.overhang = LONG_OVERHANG;
        v.performance = "\"speed\": 1.05, \"acceleration\": 1.0, \"fuel_use\": 0.85, "
                + "\"braking\": 1.05, \"steering\": 1.05, \"offroad\": 0.0";
        v.factory = "\"engine\": \"inline4\", \"fuel\": \"petrol\", \"tyres\": \"road\"";
        double roof = 27;

        // nose
        v.box(-14, 4, 30.5, 14, 13, 38.5, "paint");
        v.bumper(-14, 3, 37, 14, 5.6, 39.6);
        v.box(-8.5, 4.6, 38.5, 8.5, 8.2, 38.9, BLACK);
        v.box(-6, 10.2, 38.5, 6, 11, 38.9, BLACK);
        v.box(-1, 10, 38.9, 1, 11.3, 39.1, CHROME);
        v.pair(7, 10.4, 38.5, 13.6, 12.6, 38.9, LAMP);
        v.pair(14, 10.4, 35.5, 14.2, 12.6, 38.5, LAMP);
        v.pair(10.5, 6.6, 38.5, 13, 8, 38.9, AMBER);
        // front wings and the bonnet falling away to the nose
        v.arch(14, 9.5, 4, 13, 12, 24, 6.5);
        v.box(-14, 13, 17.5, 14, 14.3, 29, "paint");
        v.panel(-13.8, 13.8, 15.1, 17.3, 13.1, 38.4, 1.2, "paint");

        // the cabin tub: floor, doors, bulkheads
        v.box(-14, 3, -16.5, 14, 4.5, 17.5, "paint_dark");
        v.pair(13, 4.5, -16.5, 14, 15, 17.5, "paint");
        v.box(-13, 4.5, 16.3, 13, 15, 17.5, "paint");
        v.pair(13.7, 3, -16.5, 14.3, 4.6, 17.5, BLACK);
        v.pair(14, 5, 15.6, 14.1, 15, 15.8, "paint_dark");
        v.pair(14, 5, -2.6, 14.1, 15, -2.4, "paint_dark");
        v.pair(14, 12.3, 1.5, 14.3, 13, 4, "paint_dark");
        v.pair(14, 12.3, -14, 14.3, 13, -11.5, "paint_dark");

        // rear wings, boot, tail
        v.arch(14, 9.5, 4, 15, 12, -23, 6.5);
        v.box(-14, 4, -39, 14, 15, -29.5, "paint");
        v.box(-13.8, 15, -38.7, 13.8, 16.5, -24, "paint");
        v.box(-13, 16.5, -38.7, 13, 17, -37.2, "paint_dark");
        v.bumper(-14, 3, -39.6, 14, 5.6, -37);
        v.pair(7.5, 11.6, -39.4, 14, 14.4, -39, TAIL);
        v.pair(14, 11.6, -39, 14.2, 14.4, -36, TAIL);
        v.box(-3, 7.2, -39.4, 3, 9.6, -39, "#E8E8E8");
        v.box(-7.5, 12.6, -39.3, 7.5, 13.4, -39, CHROME);

        // glasshouse: raked screen, roof, raked rear window
        v.panels(12.2, 13.6, 15, 17.3, roof, 7.5, 1.3, "paint");
        v.box(-13.3, roof - 0.9, 6.4, 13.3, roof, 7.9, "paint");
        v.box(-13.3, roof, -16.2, 13.3, roof + 1.5, 7.9, "paint");
        v.pair(12.4, 15, -3.6, 13.4, roof, -1.8, BLACK);
        v.panels(12.2, 13.6, 16.3, -25.2, roof, -15.8, 1.6, "paint");
        v.panel(-12.2, 12.2, 16.3, -25, roof, -15.6, 0.6, GLASS);
        v.screen(12.2, 15, 17.3, roof, 7.5);
        v.sideWindows(12.9, 15, roof, -26.3, -15.8, 17.3, 7.5);
        v.pair(13.4, 15, 12.6, 16.2, 16.6, 14.2, "paint");
        v.pair(13.4, 15, 12.4, 16.2, 16.6, 12.6, BLACK);

        // inside
        v.frontSeats(4.5, 6.6, -2.6, 8.5, 20, true);
        v.rearBench(12.5, 4.5, 6.6, -16.5, -5.5, 19.5, 2);
        v.dash(13, 11.5, 15, 13.3, 16.3, 4.5, -2.6);
        v.gauges(3.5, 13, 11.3, 3.0, 20);
        v.carpet(13, 4.5, -16.5, 13.3);

        v.axle(12, 24, 5.5, 4, true);
        v.axle(12, -23, 5.5, 4, false);

        v.seat(6.4, 4.4, 3);
        v.seat(-6.4, 4.4, 3);
        v.seat(6.4, 4.4, -12.5);
        v.seat(-6.4, 4.4, -12.5);
        return v;
    }

    // ------------------------------------------------------------------
    // Jeep Wrangler: a long upright tub on big wheels, the wings standing
    // proud of it, seven slots in the grille and the spare on the tailgate.
    // ------------------------------------------------------------------
    static Vehicle wrangler() {
        Vehicle v = new Vehicle("jeep_wrangler");
        v.between = LONG_BETWEEN;
        v.overhang = LONG_OVERHANG;
        v.tank = 66;
        v.lightLength = 15;
        v.lightSpread = 4;
        v.grow = 1.04;
        v.dye = "green";
        v.paint = 0x5E7C16;
        v.width = 2.0;
        v.height = 2.1;
        v.stretch = 15.0 / 14.0;
        v.performance = "\"speed\": 0.92, \"acceleration\": 1.05, \"fuel_use\": 1.25, "
                + "\"braking\": 0.95, \"steering\": 0.9, \"offroad\": 0.8";
        v.factory = "\"engine\": \"v6\", \"fuel\": \"petrol\", \"tyres\": \"dirt\"";
        v.materials = new String[] {"minecraft:iron_block", "2", "minecraft:iron_bars", "2", "minecraft:leather", "2"};
        String top = ACCENT;
        double roof = 31.2;

        // grille, bonnet, the narrow nose between the wheels
        v.box(-10, 9, 33, 10, 19.5, 36, "paint");
        v.box(-10.5, 15, 16.5, 10.5, 19.5, 33, "paint");
        v.box(-10.5, 8, 16.5, 10.5, 15, 33, "paint_dark");
        v.box(-4, 19.5, 18, 4, 20, 30, "paint_dark");
        for (int slot = 0; slot < 7; slot++) {
            double x = -5.25 + slot * 1.6;
            v.box(x, 11.5, 36, x + 0.9, 18, 36.3, BLACK);
        }
        v.pair(6.6, 14.6, 36, 9.4, 17.4, 36.4, LAMP);
        v.pair(7.3, 14, 36, 8.7, 18, 36.35, LAMP);
        v.bumper(-15.5, 8, 37, 15.5, 10.8, 40);
        v.box(-8, 8.6, 36, 8, 10.2, 37, BLACK);
        v.pair(4, 9, 40, 5.5, 10, 40.4, CHROME);

        // flat wings over the front wheels
        v.pair(10.5, 14.8, 16.5, 17.2, 16, 33.5, ACCENT);
        v.pair(10.5, 11.5, 33.5, 17.2, 16, 35, ACCENT);
        v.pair(13.5, 12, 15.5, 17.2, 16, 17, ACCENT);
        v.pair(12, 13.6, 35, 15, 14.6, 35.4, AMBER);

        // the tub
        v.box(-14, 7, -16.5, 14, 8.5, 16.5, "paint_dark");
        v.box(-14, 8.5, 15.3, 14, 19.5, 16.5, "paint");
        v.pair(13, 8.5, -16.5, 14, 19, 15.3, "paint");
        v.pair(14, 9, 1.4, 14.1, 19, 1.6, "paint_dark");
        v.pair(14, 15.5, 4, 14.4, 16.4, 6.5, BLACK);
        v.pair(14, 15.5, -13, 14.4, 16.4, -10.5, BLACK);
        v.pair(14, 7, -15.5, 15.6, 8.4, 15.5, BLACK);
        // over and behind the back wheels
        v.box(-11, 7, -32, 11, 8.5, -16.5, "paint_dark");
        v.pair(10.4, 8.5, -32, 11.4, 15, -16.5, "paint_dark");
        v.pair(11, 14.6, -32, 14, 15.6, -16.5, "paint_dark");
        v.pair(13, 15, -37, 14, 19, -16.5, "paint");
        v.box(-14, 7, -37, 14, 8.5, -32, "paint_dark");
        v.pair(13, 8.5, -36, 14, 15, -32, "paint");
        v.box(-14, 7, -37, 14, 19, -36, "paint");
        v.pair(11.5, 14.8, -32.5, 17.2, 16, -15.5, ACCENT);
        v.pair(13.5, 11.5, -34, 17.2, 16, -32.5, ACCENT);
        v.pair(13.5, 11.5, -15.5, 17.2, 16, -14, ACCENT);
        v.bumper(-15.5, 8, -40, 15.5, 10.8, -37);
        v.pair(11.3, 13.8, -37.4, 14, 17.6, -37, TAIL);

        // upright screen and the hardtop
        v.panels(13, 14, 19.5, 15.9, roof, 12.9, 1.3, BLACK);
        v.box(-14, roof - 1, 12, 14, roof, 13.6, BLACK);
        v.box(-14, roof, -36.8, 14, roof + 1.5, 13.6, top);
        v.pair(13, 19, -4, 14, roof, -2.2, BLACK);
        v.pair(13, 19, -20.5, 14, roof, -19, top);
        v.pair(13, 19, -37, 14, roof, -35, top);
        v.pair(13.3, 19, -35, 13.7, roof, -20.5, GLASS);
        v.box(-13, 19, -36.8, 13, roof, -36.3, GLASS);
        v.pair(14, 19.5, 12.3, 16.8, 22.2, 13.8, BLACK);
        v.screen(13, 19.5, 15.9, roof, 12.9);
        v.sideWindows(13.5, 19, roof, -19.2, -19.2, 15.9, 12.9);

        // the spare
        v.box(-7, 15.1, -41.6, 7, 20.9, -37.4, BLACK);
        v.box(-5, 13, -41.6, 5, 23, -37.4, BLACK);
        v.box(-2.9, 11, -41.6, 2.9, 25, -37.4, BLACK);
        v.box(-3.2, 14.8, -42, 3.2, 21.2, -41.6, CHROME);
        v.box(-1.2, 16.8, -42.3, 1.2, 19.2, -42, TRIM);

        // inside
        v.frontSeats(8.5, 10.6, -2.6, 8.5, 24, true);
        v.rearBench(10.4, 8.5, 10.6, -16.5, -5.5, 23.5, 2);
        v.dash(13, 16, 19.5, 12.3, 15.3, 8.5, -2.6);
        v.gauges(3.5, 17, 10.3, 3.0, 20);
        v.carpet(13, 8.5, -16.5, 12.3);

        v.axle(14, 25, 7, 5, true);
        v.axle(14, -24, 7, 5, false);

        v.seat(6.4, 8.4, 3);
        v.seat(-6.4, 8.4, 3);
        v.seat(6.4, 8.4, -12.5);
        v.seat(-6.4, 8.4, -12.5);
        return v;
    }

    // ------------------------------------------------------------------
    // Toyota Hilux: a double cab on a ladder frame, nearly six blocks long,
    // with a bed behind it that has the wheel arches standing up in it.
    // ------------------------------------------------------------------
    static Vehicle hilux() {
        Vehicle v = new Vehicle("toyota_hilux");
        v.between = LONG_BETWEEN;
        v.overhang = LONG_OVERHANG;
        v.tank = 80;
        v.lightLength = 14;
        v.lightSpread = 3.5;
        v.grow = 1.08;
        v.chassis = "pickup";
        v.dye = "red";
        v.paint = 0xB02E26;
        v.width = 2.0;
        v.height = 2.05;
        v.stretch = 16.0 / 15.0;
        v.performance = "\"speed\": 0.95, \"acceleration\": 0.95, \"fuel_use\": 1.05, "
                + "\"braking\": 0.95, \"steering\": 0.92, \"offroad\": 0.5";
        v.factory = "\"engine\": \"inline4\", \"fuel\": \"diesel\", \"tyres\": \"road\", \"turbo\": true";
        double roof = 30.7;

        // face
        v.box(-15, 6, 35, 15, 16.5, 44.5, "paint");
        v.bumper(-15.3, 5.5, 43.5, 15.3, 9.6, 45.5);
        v.box(-8, 10, 44.5, 8, 16, 45, BLACK);
        v.box(-8, 12.8, 45, 8, 13.6, 45.3, CHROME);
        v.box(-1.3, 12.2, 45.3, 1.3, 14.2, 45.5, CHROME);
        v.pair(8.5, 13.6, 44.5, 14.6, 16, 45, LAMP);
        v.pair(15, 13.6, 41, 15.2, 16, 44.5, LAMP);
        v.pair(11, 6.8, 45.5, 13.5, 8.4, 45.8, AMBER);
        v.box(-6, 5.5, 45.5, 6, 6.8, 45.8, CHROME);
        // wings and bonnet
        v.arch(15, 10, 6, 16.5, 14.5, 28, 7.5);
        v.box(-15, 16.5, 20.5, 15, 17.6, 33, "paint");
        v.panel(-14.8, 14.8, 18.7, 20.2, 17, 44.4, 1.2, "paint");
        v.box(-4.5, 18.4, 26, 4.5, 19, 34, "paint_dark");

        // the cab
        v.box(-15, 6.5, -10, 15, 8, 20.5, "paint_dark");
        v.pair(14, 8, -10, 15, 18.5, 20.5, "paint");
        v.box(-14, 8, 19.3, 14, 18.5, 20.5, "paint");
        v.box(-14, 8, -10, 14, 18.5, -9, "paint");
        v.pair(15, 6.5, -9, 16.6, 7.6, 19.5, BLACK);
        v.pair(15, 8.5, 18.4, 15.1, 18.5, 18.6, "paint_dark");
        v.pair(15, 8.5, 4.9, 15.1, 18.5, 5.1, "paint_dark");
        v.pair(15, 15.6, 8, 15.4, 16.4, 10.5, CHROME);
        v.pair(15, 15.6, -6.5, 15.4, 16.4, -4, CHROME);
        v.panels(13.2, 14.6, 18.5, 20.2, roof, 13.2, 1.3, "paint");
        v.box(-14.5, roof - 0.9, 12.2, 14.5, roof, 13.7, "paint");
        v.box(-14.5, roof, -10, 14.5, roof + 1.5, 13.7, "paint");
        v.pair(13.4, 18.5, 4.2, 14.4, roof, 5.8, BLACK);
        v.pair(13.2, 18.5, -10, 14.6, roof, -8, "paint");
        v.box(-13.2, 18.5, -9.6, 13.2, roof, -9.1, GLASS);
        v.pair(14.6, 19, 15, 17.6, 21.8, 16.6, BLACK);
        v.screen(13.2, 18.5, 20.2, roof, 13.2);
        v.sideWindows(13.9, 18.5, roof, -8, -8, 20.2, 13.2);

        // the bed
        v.box(-15, 6, -20.5, 15, 12, -10, "paint");
        v.box(-10, 6, -34.5, 10, 11, -20.5, "paint_dark");
        v.box(-15, 6, -47.5, 15, 12, -34.5, "paint");
        v.box(-14, 11, -47, 14, 12, -10, TRIM);
        v.pair(10, 12, -34.5, 14, 14.5, -19.5, TRIM);
        v.pair(14, 12, -20.5, 15, 19, -10, "paint");
        v.pair(14, 14.5, -34.5, 15, 19, -20.5, "paint");
        v.pair(14, 12, -47.5, 15, 19, -34.5, "paint");
        v.pair(14, 11.3, -34.5, 15, 14.5, -33.5, "paint");
        v.pair(14, 13.1, -33.5, 15, 14.5, -32.1, "paint");
        v.pair(14, 11.3, -21.5, 15, 14.5, -20.5, "paint");
        v.pair(14, 13.1, -22.9, 15, 14.5, -21.5, "paint");
        v.box(-14, 12, -48, 14, 19, -47, "paint");
        v.pair(13.9, 19, -48, 15.1, 19.6, -10, BLACK);
        v.box(-2.2, 16.6, -48.3, 2.2, 17.6, -48, BLACK);
        v.pair(13.4, 12.6, -48.3, 15.1, 18.4, -47.6, TAIL);
        v.bumper(-15.3, 5.5, -49.5, 15.3, 9, -47.5);
        v.box(-3, 9.4, -48.3, 3, 11.4, -48, "#E8E8E8");

        // inside
        v.frontSeats(8, 10.2, 4.6, 13.5, 23.5, true);
        v.rearBench(13, 8, 10.2, -9, 0, 18.5, 2);
        v.dash(14, 15, 18.5, 16.3, 19.3, 8, 4.6);
        v.gauges(3.5, 16, 14.3, 3.0, 20);
        v.carpet(14, 8, -9, 16.3);

        v.axle(12.75, 28, 6.5, 4.5, true);
        v.axle(12.75, -27.5, 6.5, 4.5, false);

        v.seat(6.8, 8, 8);
        v.seat(-6.8, 8, 8);
        v.seat(6.8, 8, -6.5);
        v.seat(-6.8, 8, -6.5);
        return v;
    }

    // ------------------------------------------------------------------
    // Tofaş Şahin: the square saloon every Turkish street has had parked
    // on it for forty years. Flat bonnet, flat boot, upright glass.
    // ------------------------------------------------------------------
    static Vehicle sahin() {
        Vehicle v = new Vehicle("tofas_sahin");
        v.between = LONG_BETWEEN;
        v.overhang = LONG_OVERHANG;
        v.tank = 50;
        v.grow = 1.25;
        v.dye = "white";
        v.paint = 0xF9FFFE;
        v.width = 1.8;
        v.height = 1.8;
        v.stretch = 14.5 / 14.0;
        v.performance = "\"speed\": 0.97, \"acceleration\": 1.05, \"fuel_use\": 0.95, "
                + "\"braking\": 0.9, \"steering\": 1.1, \"offroad\": 0.1";
        v.factory = "\"engine\": \"inline4\", \"fuel\": \"petrol\", \"tyres\": \"road\"";
        v.materials = new String[] {"minecraft:iron_ingot", "2", "minecraft:iron_bars", "1"};
        double roof = 27.6;

        // nose: square lamps either side of a black grille, a rubber bumper
        v.box(-14, 4.5, 29.5, 14, 13.5, 36, "paint");
        v.bumper(-14.4, 4.5, 36, 14.4, 7.2, 37.5);
        v.box(-9, 8.5, 36, 9, 12.5, 36.4, BLACK);
        v.box(-9, 10.3, 36.4, 9, 10.7, 36.6, CHROME);
        v.pair(9, 8.5, 36, 13.6, 12.5, 36.4, LAMP);
        v.pair(14, 9, 33.5, 14.2, 11, 36, AMBER);
        v.arch(14, 9.5, 4.5, 13.5, 11.5, 23, 6.5);
        v.box(-14, 13.5, 16.5, 14, 14.6, 36, "paint");

        // the tub
        v.box(-14, 3.5, -16.5, 14, 5, 16.5, "paint_dark");
        v.pair(13, 5, -16.5, 14, 15, 16.5, "paint");
        v.box(-13, 5, 15.3, 13, 15, 16.5, "paint");
        v.pair(14, 9, -16.5, 14.25, 9.8, 16.5, BLACK);
        v.pair(14, 5.5, 14.6, 14.1, 15, 14.8, "paint_dark");
        v.pair(14, 5.5, -2.6, 14.1, 15, -2.4, "paint_dark");
        v.pair(14, 12.3, 1.5, 14.3, 13, 4, CHROME);
        v.pair(14, 12.3, -14, 14.3, 13, -11.5, CHROME);

        // boot and tail
        v.arch(14, 9.5, 4.5, 15, 11.5, -23, 6.5);
        v.box(-14, 4.5, -36, 14, 15, -29.5, "paint");
        v.box(-13.8, 15, -35.8, 13.8, 15.6, -23, "paint");
        v.bumper(-14.4, 4.5, -37.5, 14.4, 7.2, -36);
        v.pair(8, 9.5, -36.4, 13.8, 13.5, -36, TAIL);
        v.pair(8, 9.5, -36.5, 9.6, 13.5, -36.4, AMBER);
        v.box(-3, 9.8, -36.4, 3, 12.2, -36, "#E8E8E8");

        // glasshouse
        v.panels(12.4, 13.8, 15, 16.3, roof, 9, 1.3, "paint");
        v.box(-13.4, roof - 0.9, 8, 13.4, roof, 9.5, "paint");
        v.box(-13.4, roof, -15, 13.4, roof + 1.4, 9.5, "paint");
        v.pair(12.5, 15, -3.6, 13.5, roof, -1.8, BLACK);
        v.panels(12.4, 13.8, 15.6, -22.6, roof, -14.6, 1.8, "paint");
        v.panel(-12.4, 12.4, 15.6, -22.4, roof, -14.4, 0.6, GLASS);
        v.pair(13.5, 15, 12.6, 15.8, 16.5, 14, BLACK);
        v.screen(12.4, 15, 16.3, roof, 9);
        v.sideWindows(13.1, 15, roof, -23, -14.6, 16.3, 9);

        // inside
        v.frontSeats(5, 7.1, -2.6, 8.5, 20, false);
        v.rearBench(12.5, 5, 7.1, -16.5, -5.5, 19.5, 2);
        v.dash(13, 11.5, 15, 12.3, 15.3, 5, -2.6);
        v.gauges(3.5, 13, 10.3, 3.0, 20);
        v.carpet(13, 5, -16.5, 12.3);

        v.axle(11.75, 23, 5, 3.5, true);
        v.axle(11.75, -23, 5, 3.5, false);

        v.seat(6, 4.9, 3);
        v.seat(-6, 4.9, 3);
        v.seat(6, 4.9, -12.5);
        v.seat(-6, 4.9, -12.5);
        return v;
    }

    // ------------------------------------------------------------------
    // Ford Mustang, the 1967 fastback: long bonnet, short tail, the roof
    // running down in one line to a ducktail, and twin stripes over it all.
    // The race car.
    // ------------------------------------------------------------------
    static Vehicle mustang() {
        Vehicle v = new Vehicle("ford_mustang");
        v.between = LONG_BETWEEN;
        v.overhang = LONG_OVERHANG;
        v.tank = 61;
        v.lightLength = 13;
        v.lightSpread = 3;
        v.grow = 1.08;
        v.dye = "blue";
        v.paint = 0x3C44AA;
        v.width = 1.9;
        v.height = 1.65;
        v.stretch = 15.0 / 14.0;
        v.performance = "\"speed\": 1.3, \"acceleration\": 1.3, \"fuel_use\": 1.35, "
                + "\"braking\": 1.1, \"steering\": 1.0, \"offroad\": 0.0";
        v.factory = "\"engine\": \"v8\", \"fuel\": \"petrol\", \"tyres\": \"sport\"";
        v.materials = new String[] {"minecraft:iron_block", "1", "minecraft:iron_ingot", "2", "minecraft:redstone_block", "1"};
        double roof = 23.5;

        // nose: a wide black mouth with the lamps set in its ends
        v.box(-14, 4, 32, 14, 12.4, 39, "paint");
        v.box(-11.2, 5.4, 39, 11.2, 10.8, 39.4, BLACK);
        v.box(-11.4, 10.8, 39, 11.4, 11.3, 39.6, CHROME);
        v.box(-11.4, 4.9, 39, 11.4, 5.4, 39.6, CHROME);
        v.pair(7.8, 6.6, 39.4, 10.6, 9.6, 39.6, LAMP);
        v.box(-1.3, 7.2, 39.4, 1.3, 8.9, 39.8, CHROME);
        v.bumper(-14.4, 3.2, 39, 14.4, 5, 40.6);
        v.pair(9.5, 3.6, 40.6, 12, 4.6, 40.8, AMBER);
        // the long bonnet, and the stripes down it
        v.arch(14, 9.5, 4, 13, 12.4, 25, 7);
        v.box(-14, 12.4, 17.5, 14, 13.2, 32, "paint");
        v.panel(-13.8, 13.8, 14, 17.5, 12.9, 39, 1.2, "paint");
        v.panels(1.2, 3.4, 14, 17.5, 12.9, 39, 1.3, ACCENT);
        v.box(-4, 14.3, 21, 4, 14.8, 27, "paint_dark");

        // the cabin tub
        v.box(-14, 3, -17, 14, 4.5, 18, "paint_dark");
        v.pair(13, 4.5, -17, 14, 13.4, 18, "paint");
        v.box(-13, 4.5, 16.8, 13, 13.4, 18, "paint");
        v.pair(13.7, 3, -17, 14.3, 4.6, 18, BLACK);
        v.pair(14, 5, 15.8, 14.1, 13.4, 16, "paint_dark");
        v.pair(14, 5, -3, 14.1, 13.4, -2.8, "paint_dark");
        v.pair(14, 10.6, 1.5, 14.3, 11.2, 3.5, CHROME);
        v.pair(14, 6.8, -15.5, 14.4, 10, -11.5, BLACK);
        v.pair(14, 5.2, -30, 14.15, 6.2, 31, ACCENT);

        // the tail: ducktail, black panel, three lamps a side
        v.arch(14, 9.5, 4, 13, 12.4, -24, 7);
        v.box(-14, 4, -38, 14, 13, -31, "paint");
        v.box(-13.8, 13, -38, 13.8, 14.2, -30, "paint");
        v.box(-13.8, 14.2, -38.5, 13.8, 15.2, -36.6, "paint_dark");
        v.box(-13, 7.4, -38.3, 13, 12.2, -38, BLACK);
        for (int lamp = 0; lamp < 3; lamp++) {
            double x = 3.8 + lamp * 3.0;
            v.pair(x, 8, -38.5, x + 2.4, 11.6, -38.2, TAIL);
        }
        v.box(-1.2, 8.8, -38.5, 1.2, 10.8, -38.3, CHROME);
        v.bumper(-14.4, 3.2, -39.8, 14.4, 5.2, -38);
        v.pair(5, 2.4, -39.6, 7.2, 3.4, -36, CHROME);

        // glasshouse: raked screen, short roof, then one line down to the tail
        v.panels(12.2, 13.6, 13.4, 17.5, roof, 6.5, 1.3, "paint");
        v.box(-13.3, roof - 0.9, 5.6, 13.3, roof, 7, "paint");
        v.box(-13.3, roof, -10, 13.3, roof + 1.4, 7, "paint");
        v.pair(1.2, roof + 1.4, -10, 3.4, roof + 1.5, 7, ACCENT);
        v.panels(12.2, 13.6, roof + 0.6, -10, 14.2, -31, 1.6, "paint");
        v.panel(-12.2, 12.2, roof + 0.4, -10.4, 14.2, -30.4, 0.6, GLASS);
        v.panels(1.2, 3.4, roof + 0.7, -10, 14.3, -31, 0.4, ACCENT);
        // the sail behind the side glass, stepped under the fastback line
        int bands = 6;
        for (int i = 0; i < bands; i++) {
            double ya = Vehicle.lerp(13.4, roof, (double) i / bands);
            double yb = Vehicle.lerp(13.4, roof, (double) (i + 1) / bands);
            double back = Vehicle.lerp(-31, -10, (yb - 14.2) / (roof - 14.2));
            double front = Vehicle.lerp(-15, -9.5, (double) (i + 1) / bands);
            if (front > back) {
                v.pair(12.9, ya, back, 13.9, yb, front, "paint");
            }
        }
        v.screen(12.2, 13.4, 17.5, roof, 6.5);
        v.sideWindows(12.9, 13.4, roof, -15, -9.5, 17.5, 6.5);
        v.pair(13.4, 13.4, 12.6, 16.2, 15.4, 14.2, "paint");

        // inside
        v.frontSeats(4.5, 6.4, -2.6, 8.5, 18.5, false);
        v.rearBench(12.5, 4.5, 6.4, -16.5, -6.5, 16.5, 2);
        v.dash(13, 10.4, 13.4, 13.6, 16.8, 4.5, -2.6);
        v.gauges(3.5, 10.8, 11.6, 2.8, 20);
        v.carpet(13, 4.5, -16.5, 13.6);

        v.axle(12, 25, 6, 4.5, true);
        v.axle(12, -24, 6, 4.5, false);

        v.seat(6.4, 4.4, 3);
        v.seat(-6.4, 4.4, 3);
        v.seat(6.4, 4.4, -12);
        v.seat(-6.4, 4.4, -12);
        return v;
    }

    // ------------------------------------------------------------------
    // Mercedes-Benz Atego: a cab-over two-axle rigid with a fuel barrel
    // on its back. The tanker.
    // ------------------------------------------------------------------
    static Vehicle atego() {
        Vehicle v = new Vehicle("mercedes_atego");
        v.tank = 200;
        v.lightLength = 16;
        v.lightSpread = 4;
        v.chassis = "tanker";
        v.dye = "white";
        v.paint = 0xF9FFFE;
        v.width = 2.3;
        v.height = 2.8;
        v.grow = 1.0;
        v.between = LONG_BETWEEN;
        v.overhang = LONG_OVERHANG;
        v.performance = "\"speed\": 1.0, \"acceleration\": 1.0, \"fuel_use\": 2.4, "
                + "\"braking\": 1.0, \"steering\": 1.0, \"offroad\": 0.2";
        v.factory = "\"engine\": \"inline6\", \"fuel\": \"diesel\", \"tyres\": \"road\"";
        String tank = "#C3C9CF";
        String band = "#8E959C";

        // ladder frame
        v.box(-8, 8, -50, 8, 11, 26, TRIM);
        v.box(-12, 11, -50, 12, 14, 24, TRIM);

        // cab: floor between the wheels, face, sides
        v.box(-11, 9, 26, 11, 11, 42, "paint_dark");
        v.box(-17, 9, 42, 17, 11, 50, "paint_dark");
        v.pair(10.5, 9, 26, 11.5, 15.5, 42, "paint_dark");
        v.pair(11, 15.5, 26, 17, 16.5, 42, "paint_dark");
        v.pair(16.5, 14.5, 25.5, 17.6, 16.5, 42.5, BLACK);
        v.pair(16, 16.5, 26, 17, 24, 42, "paint");
        v.pair(16, 9, 42, 17, 24, 48.8, "paint");
        v.box(-17, 11, 48.8, 17, 24, 50, "paint");
        v.box(-17, 11, 26, 17, 43, 27.2, "paint");
        v.box(-11, 13, 50, 11, 20.5, 50.4, BLACK);
        v.box(-11, 16.4, 50.4, 11, 17, 50.6, CHROME);
        v.box(-1.6, 15, 50.4, 1.6, 18.4, 50.8, CHROME);
        v.bumper(-17.5, 6.5, 48.5, 17.5, 11.5, 51);
        v.pair(11.5, 7.6, 51, 16.5, 10.4, 51.3, LAMP);
        v.pair(17, 12, 47, 17.2, 13.5, 49.5, AMBER);
        v.pair(17, 12, 34, 17.1, 24, 34.2, "paint_dark");
        v.pair(17, 18.5, 43.5, 17.4, 19.4, 46, BLACK);
        // glass and roof
        v.panels(15.6, 17, 24, 49.6, 43, 46.6, 1.3, "paint");
        v.box(-17, 42, 45.6, 17, 43, 47.3, "paint");
        v.box(-17, 43, 26, 17, 44.5, 47.3, "paint");
        v.box(-13, 44.5, 30, 13, 46, 44, "paint_dark");
        v.pair(16, 24, 26, 17, 43, 31, "paint");
        v.pair(17, 26, 47.2, 19.6, 35, 48.4, BLACK);
        v.screen(15.6, 24, 49.6, 43, 46.6);
        v.sideWindows(16.3, 24, 43, 31, 31, 49.6, 46.6);
        // inside
        v.frontSeats(11, 15.2, 30.6, 41, 30, true);
        v.dash(16, 20, 24, 45, 48.8, 11, 30.6);
        v.gauges(4.5, 22.4, 42.4, 3.4, 35);
        v.carpet(10.5, 11, 27, 45);

        // the barrel, as round as three boxes make it, with its bands and hatches.
        // Between the middle bands it is glass, a sight section: whatever the
        // tanker is carrying shows through it, in its own colour, up to its
        // own level. The "cargo" boxes inside mark where the load is drawn.
        // Five boxes crossed, every fifteen degrees round a circle of 14, so
        // the end of the barrel is near enough round and not a staircase.
        double[][] round = new double[5][];
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(15 + 15 * i);
            double w = 14 * Math.cos(angle);
            double h = 14 * Math.sin(angle);
            round[i] = new double[] {w, 27 - h, 27 + h};
        }
        for (double[] r : round) {
            v.box(-r[0], r[1], -50, r[0], r[2], SIGHT_FROM, tank);
            v.box(-r[0], r[1], SIGHT_FROM, r[0], r[2], SIGHT_TO, SIGHT_GLASS);
            v.box(-r[0], r[1], SIGHT_TO, r[0], r[2], 24, tank);
            v.box(-r[0] + 0.7, r[1] + 0.7, SIGHT_FROM + 0.4, r[0] - 0.7, r[2] - 0.7, SIGHT_TO - 0.4, CARGO);
        }
        // the bands, and a frame either end of the sight glass
        for (double z : new double[] {-44, -13, SIGHT_FROM - 0.6, SIGHT_TO - 0.6, 18}) {
            for (double[] r : round) {
                v.box(-r[0] - 0.3, r[1] - 0.3, z, r[0] + 0.3, r[2] + 0.3, z + 1.2, band);
            }
        }
        v.box(-3, 41, -32, 3, 42.6, -26, band);
        v.box(-3, 41, 0, 3, 42.6, 6, band);
        v.box(-1, 41, -44, 1, 41.6, 18, band);
        v.box(-3.5, 23.5, -50.4, 3.5, 30.5, -50, AMBER);
        // valves, guards, tail
        v.pair(12, 12, -12, 15.5, 16, 2, TRIM);
        v.pair(15, 8, -14, 16, 9.6, 20, band);
        v.pair(8.5, 15.6, -38, 17.2, 16.8, -18, BLACK);
        v.pair(15.5, 11, -39.5, 17.2, 16.8, -38, BLACK);
        v.bumper(-16, 7, -51.5, 16, 9.2, -50);
        v.pair(9, 9.6, -50.5, 14, 12, -50, TAIL);
        v.box(-3, 9.6, -50.4, 3, 11.8, -50, "#E8E8E8");

        v.axle(14, 34, 7.5, 5, true);
        v.axle(13, -28, 7.5, 8, false);

        v.seat(7, 13, 36);
        v.seat(-7, 13, 36);
        return v;
    }

    /** The item definition for one machine: its default body, and every other body a pack gives it. */
    /**
     * One vehicle's item model, with its paint tinted: by the item's own
     * {@code car:color} when it has been dyed, otherwise the factory paint.
     */
    // ------------------------------------------------------------------
    // The box trailer: an open steel tub on one axle, mudguards over the
    // wheels and an A-frame drawbar out to the coupler. No make: every
    // farm and builder's yard has one of these.
    // ------------------------------------------------------------------
    static Vehicle trailer() {
        Vehicle v = new Vehicle("box_trailer");
        v.tank = 0;
        v.chassis = "trailer";
        v.dye = "light_gray";
        v.paint = 0x9D9D97;
        v.width = 1.6;
        v.height = 1.2;
        v.between = 1.0;
        v.overhang = 0.85;
        v.materials = new String[] {"minecraft:chest", "1", "minecraft:iron_ingot", "3", "minecraft:oak_slab", "3"};

        // the tub: floor, sides, front board and a drop tailgate
        v.box(-12, 7.5, -21, 12, 9, 13, "paint_dark");
        v.box(-11, 9, -20, 11, 9.4, 12, "#6E5536");
        v.pair(11, 9, -21, 12, 17, 13, "paint");
        v.box(-11, 9, 12, 11, 17, 13, "paint");
        v.box(-11, 9, -21, 11, 16, -20, "paint");
        v.pair(10.8, 16.6, -21.2, 12.3, 17.6, 13.2, ACCENT);
        v.box(-12.3, 16.6, 12, 12.3, 17.6, 13.2, ACCENT);
        v.box(-11, 15.6, -21.3, 11, 16.4, -20, ACCENT);
        // ribs pressed into the sides
        for (double z : new double[] {-13, -3, 7}) {
            v.pair(12, 10, z - 0.5, 12.3, 16.6, z + 0.5, "paint_dark");
        }

        // lamps and the plate on the tailgate
        v.pair(7.5, 10, -21.4, 11, 12, -21, TAIL);
        v.pair(7.5, 12, -21.4, 9, 13, -21, AMBER);
        v.box(-3.5, 10, -21.4, 3.5, 12.4, -21, "#E8E8E8");
        v.box(-11.5, 7, -21.6, 11.5, 8, -20.6, BLACK);

        // mudguards over the wheels, and the axle beam under the floor
        v.pair(12, 14, -10.5, 16.2, 14.8, 4.5, BLACK);
        v.pair(15.4, 10.5, -10.5, 16.2, 14, -9.7, BLACK);
        v.pair(15.4, 10.5, 3.7, 16.2, 14, 4.5, BLACK);
        v.box(-12, 4.4, -3.8, 12, 5.6, -2.2, TRIM);

        // A-frame drawbar out to the coupler
        v.add(-7.5, 6.6, 13, -6, 8, 26, TRIM, 0);
        v.add(6, 6.6, 13, 7.5, 8, 26, TRIM, 0);
        v.box(-7.5, 6.6, 22, 7.5, 8, 23.5, TRIM);
        v.box(-1.2, 6.6, 22, 1.2, 8, 31, TRIM);
        v.box(-1.8, 6.2, 30, 1.8, 8.8, 34, CHROME);
        v.box(-1.4, 8.8, 30.6, 1.4, 9.6, 33.4, BLACK);
        // jockey wheel, wound up for the road
        v.box(2, 4, 24, 3.4, 10, 25.4, TRIM);
        v.box(1.8, 3, 23.4, 3.6, 5.6, 26, BLACK);

        v.axle(13.5, -3, 5, 3.2, false);
        return v;
    }

    // ------------------------------------------------------------------
    // Vespa Primavera: the Italian scooter. Small wheels, a steel shell
    // round the engine at the back, a legshield to hide behind and a flat
    // floor for your feet.
    // ------------------------------------------------------------------
    static Vehicle vespa() {
        Vehicle v = new Vehicle("vespa_primavera");
        v.tank = 8;
        v.lightLength = 7;
        v.lightSpread = 1.5;
        v.chassis = "motorcycle";
        v.dye = "light_blue";
        v.paint = 0x3AB3DA;
        v.width = 0.9;
        v.height = 1.5;
        v.grow = 1;
        v.widen = 1;
        v.between = 1;
        v.overhang = 1;
        v.performance = "\"speed\": 0.72, \"acceleration\": 1.1, \"fuel_use\": 0.4, "
                + "\"braking\": 1.1, \"steering\": 1.4, \"offroad\": 0";
        v.factory = "\"engine\": \"inline4\", \"fuel\": \"petrol\", \"tyres\": \"road\"";
        v.materials = new String[] {"minecraft:iron_ingot", "2", "minecraft:leather", "1"};

        // legshield and the horn cover on it
        v.panel(-4, 4, 4.5, 11.5, 17.8, 9.2, 1.2, "paint");
        v.box(-1.4, 12.5, 11.4, 1.4, 14.5, 12.2, CHROME);
        v.box(-3.4, 5, 9.2, 3.4, 16.8, 9.8, "paint_dark");
        // the front mudguard over the front wheel, on the fork
        v.steering = true;
        v.box(-1.8, 6.6, 8.6, 1.8, 7.6, 14.2, "paint");
        v.box(-1.8, 5.2, 13.6, 1.8, 6.6, 14.2, "paint");
        v.steering = false;
        // the floor, its rubber strips, and the tunnel under it
        v.box(-3.6, 3.4, -2.5, 3.6, 4.6, 9.6, "paint");
        for (double x : new double[] {-2.4, -0.8, 0.8, 2.4}) {
            v.box(x - 0.3, 4.6, -1.5, x + 0.3, 4.8, 9, BLACK);
        }
        // the shell round the engine, swelling out over the back wheel
        v.box(-4.6, 4, -13.5, 4.6, 11.6, -2.5, "paint");
        v.box(-4, 4.6, -15.5, 4, 10.8, -13.5, "paint");
        v.box(-3, 5.4, -16.6, 3, 9.6, -15.5, "paint");
        v.pair(4.6, 6.5, -11, 4.8, 7, -5, CHROME);
        v.box(-1.4, 8.8, -16.9, 1.4, 10, -16.6, TAIL);
        v.box(-1.8, 7.2, -16.9, 1.8, 8.6, -16.6, "#E8E8E8");
        // the seat, a long saddle for two
        v.box(-3.2, 11.6, -14, 3.2, 13.4, -1.5, "#5A3A26");
        v.box(-3.4, 11.6, -14.6, 3.4, 12.2, -14, CHROME);
        // the engine and the silencer on the right
        v.box(-4, 2.4, -9, -2, 4.6, -4, TRIM);
        v.box(2.2, 2.6, -15, 4, 4.4, -7, CHROME);
        // steering column, headset with the lamp in it, bars and mirrors
        v.steering = true;
        v.panel(-0.8, 0.8, 7.6, 11.2, 18.6, 9.6, 1.4, TRIM);
        v.box(-3, 17.6, 8.4, 3, 20.4, 11.6, "paint");
        v.box(-1.6, 18.2, 11.6, 1.6, 20.2, 12, LAMP);
        v.pair(3, 18.6, 9.4, 6.4, 19.4, 10.4, BLACK);
        v.pair(4.4, 19.4, 9.6, 4.8, 22.4, 10, CHROME);
        v.pair(3.8, 22.4, 9.4, 5.6, 23.4, 10.2, CHROME);
        v.pair(3, 18, 10.6, 3.4, 19, 11.8, AMBER);
        v.steering = false;

        v.wheel(11, 3.6, 2.2, true);
        v.wheel(-10, 3.6, 2.4, false);

        v.seat(0, 11.2, -5);
        v.seat(0, 11.6, -11);
        return v;
    }

    // ------------------------------------------------------------------
    // Honda CBR600RR: the sports bike. A full fairing with twin lamps, a
    // bubble screen, a fat tank, a rear set high up and an exhaust tucked
    // under the tail. The fast one.
    // ------------------------------------------------------------------
    static Vehicle cbr() {
        Vehicle v = new Vehicle("honda_cbr600rr");
        v.tank = 18;
        v.lightLength = 10;
        v.lightSpread = 2;
        v.chassis = "motorcycle";
        v.dye = "red";
        v.paint = 0xB02E26;
        v.width = 0.9;
        v.height = 1.5;
        v.grow = 1;
        v.widen = 1;
        v.between = 1;
        v.overhang = 1;
        v.performance = "\"speed\": 1.25, \"acceleration\": 1.55, \"fuel_use\": 0.7, "
                + "\"braking\": 1.25, \"steering\": 1.3, \"offroad\": 0";
        v.factory = "\"engine\": \"inline4\", \"fuel\": \"petrol\", \"tyres\": \"sport\"";
        v.materials = new String[] {"minecraft:iron_ingot", "3", "minecraft:redstone", "2",
                "minecraft:leather", "1"};

        // the engine block and the frame spars over it
        v.box(-2.8, 4, -2, 2.8, 10.5, 7, TRIM);
        v.box(-2.2, 2.8, 0, 2.2, 4, 6, BLACK);
        v.pair(2.4, 9.5, -4, 3.2, 12, 9, CHROME);
        // the fairing: side panels, the nose, the twin lamps and the screen
        v.pair(2.9, 5.5, 2, 3.5, 13, 12.5, "paint");
        v.pair(3.5, 8, 6, 3.6, 9.4, 11.5, "#FFFFFF");
        v.box(-3.5, 4.6, 6.5, 3.5, 5.6, 12, "paint_dark");
        v.panel(-3.5, 3.5, 11, 12.5, 17.5, 14.4, 2.2, "paint");
        v.pair(0.4, 12.6, 15.2, 2.6, 14.4, 15.5, LAMP);
        v.box(-0.4, 12.4, 15.1, 0.4, 14.6, 15.5, BLACK);
        v.panel(-2.6, 2.6, 17.2, 13.2, 20.8, 9.8, 0.4, GLASS);
        v.pair(3.5, 15.4, 11.4, 5.2, 16.2, 12.4, BLACK);
        // the tank: wide at the knees, narrowing to its top, the filler cap and a stripe
        v.box(-3.4, 12, -0.5, 3.4, 14.8, 9, "paint");
        v.box(-2.8, 14.8, 0, 2.8, 16.4, 8.5, "paint");
        v.box(-2.2, 16.4, 0.8, 2.2, 16.9, 7.2, "paint_light");
        v.box(-0.8, 16.9, 1.5, 0.8, 17.1, 6.5, "#FFFFFF");
        v.box(-0.7, 16.9, 7.2, 0.7, 17.2, 8.4, CHROME);
        // a lower fairing over the engine, its vents, the radiator behind the wheel
        v.pair(2.9, 3.4, 1, 3.4, 5.5, 9, "paint_dark");
        v.pair(3.5, 7, 3, 3.6, 7.4, 8, BLACK);
        v.pair(3.5, 10.4, 3.5, 3.6, 10.8, 8.5, BLACK);
        v.box(-2.4, 7, 7, 2.4, 11.5, 7.6, BLACK);
        // the clocks behind the screen
        v.box(-1.4, 15.4, 12, 1.4, 17, 13, BLACK);
        v.box(-1, 15.8, 11.9, 1, 16.7, 12, DIAL);
        // the seat, the pillion pad and the tail
        v.box(-2.6, 14, -8.5, 2.6, 15.6, -0.5, SEAT);
        v.box(-2.2, 15.6, -12.5, 2.2, 17, -8.5, SEAT);
        v.box(-2.6, 13.2, -16, 2.6, 15.6, -8.5, "paint");
        v.box(-1.8, 15.6, -16, 1.8, 17.6, -12.5, "paint");
        v.box(-1.2, 14.4, -16.3, 1.2, 15.6, -16, TAIL);
        // the number plate on its hanger under the tail, indicators either side
        v.box(-0.4, 12.6, -17.2, 0.4, 13.4, -16, BLACK);
        v.box(-1.8, 10.6, -17.5, 1.8, 12.8, -17.2, "#F2F2F2");
        v.pair(1.2, 12.9, -17, 2.4, 13.5, -16.4, AMBER);
        // the panels under the seat, closing it down to the frame
        v.pair(1.6, 8.5, -12.5, 2.6, 14, -0.5, "paint");
        v.box(-1.6, 9.5, -12.5, 1.6, 13.2, -2, TRIM);
        // the exhaust can, tucked under the tail on the right
        v.add(2.6, 8, -13.5, 4, 9.8, -7.5, CHROME, 10);
        v.box(2.8, 8.4, -13.8, 3.8, 9.4, -13.5, BLACK);
        // the fork, the front mudguard and the clip-on bars
        v.steering = true;
        v.panels(1.2, 2, 4.6, 13.4, 16, 10.2, 1.0, CHROME);
        v.box(-1.4, 9.6, 9.6, 1.4, 10.4, 15.4, "paint");
        v.pair(2.2, 15.4, 8.6, 5.4, 16.2, 9.6, BLACK);
        v.steering = false;
        // the swingarm out to the back wheel, and the rear shock
        v.panels(1.4, 2.2, 5, -10.5, 7.2, -1, 1.4, TRIM);
        v.box(-0.6, 6.4, -5, 0.6, 12.4, -3.6, AMBER);
        // rear sets for the rider's feet
        v.pair(2.8, 7.6, -4.6, 5, 8.2, -3.6, CHROME);

        v.wheel(12, 4.8, 2.4, true);
        v.wheel(-10.5, 4.8, 3.2, false);

        v.seat(0, 13.4, -4);
        v.seat(0, 15, -10.5);
        return v;
    }

    static String itemModel(Vehicle v) {
        StringBuilder tints = new StringBuilder();
        String[] shades = {"paint", "paint_dark", "paint_light"};
        for (int i = 0; i < shades.length; i++) {
            tints.append(i == 0 ? "" : ", ").append("{ \"type\": \"car:paint\", \"shade\": \"").append(shades[i])
                    .append("\", \"default\": ").append(v.paint).append(" }");
        }
        return "{ \"type\": \"minecraft:model\", \"model\": \"car:item/vehicle_" + v.name
                + "\", \"tints\": [" + tints + "] }";
    }

    static void item(String item, List<Vehicle> all, String chassis, String fallback) throws Exception {
        StringBuilder out = new StringBuilder("{\n  \"model\": {\n    \"type\": \"minecraft:select\",\n"
                + "    \"property\": \"minecraft:component\",\n    \"component\": \"car:vehicle\",\n    \"cases\": [\n");
        List<Vehicle> mine = all.stream().filter(v -> v.chassis.equals(chassis)).toList();
        for (int i = 0; i < mine.size(); i++) {
            out.append("      { \"when\": \"car:").append(mine.get(i).name).append("\", \"model\": ")
                    .append(itemModel(mine.get(i))).append(" }").append(i + 1 < mine.size() ? ",\n" : "\n");
        }
        Vehicle back = mine.stream().filter(v -> v.name.equals(fallback)).findFirst().orElseThrow();
        out.append("    ],\n    \"fallback\": ").append(itemModel(back)).append("\n  }\n}\n");
        Files.writeString(new File("src/main/resources/assets/car/items/" + item + ".json").toPath(), out);
    }

    static String n(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        text = text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
        return text.equals("-0") ? "0" : text;
    }

    static void definition(Vehicle v) throws Exception {
        StringBuilder out = new StringBuilder("{\n");
        out.append("  \"chassis\": \"").append(v.chassis).append("\",\n");
        out.append("  \"width\": ").append(n(v.width)).append(",\n");
        out.append("  \"height\": ").append(n(v.height)).append(",\n");
        out.append("  \"color\": \"").append(v.dye).append("\",\n");
        out.append("  \"performance\": { ").append(v.performance).append(" },\n");
        out.append("  \"factory\": { ").append(v.factory).append(" },\n");
        out.append("  \"tank\": ").append(n(v.tank)).append(",\n");
        if (v.lightLength > 0) {
            out.append("  \"lights\": { \"length\": ").append(n(v.lightLength))
                    .append(", \"spread\": ").append(n(v.lightSpread)).append(" },\n");
        }
        if (v.materials.length > 0) {
            out.append("  \"materials\": [");
            for (int i = 0; i < v.materials.length; i += 2) {
                out.append(i == 0 ? " " : ", ").append("{ \"item\": \"").append(v.materials[i])
                        .append("\", \"count\": ").append(v.materials[i + 1]).append(" }");
            }
            out.append(" ],\n");
        }
        out.append("  \"seats\": [\n");
        for (int i = 0; i < v.seats.size(); i++) {
            double[] s = v.seats.get(i);
            out.append("    [").append(n(s[0])).append(", ").append(n(s[1])).append(", ").append(n(s[2]))
                    .append(i + 1 < v.seats.size() ? "],\n" : "]\n");
        }
        out.append("  ],\n  \"wheels\": [\n");
        for (int i = 0; i < v.wheels.size(); i++) {
            Wheel w = v.wheels.get(i);
            out.append("    { \"at\": [").append(n(w.x)).append(", ").append(n(w.y)).append(", ").append(n(w.z))
                    .append("], \"radius\": ").append(n(w.radius)).append(", \"width\": ").append(n(w.width))
                    .append(", \"steers\": ").append(w.steers)
                    .append(i + 1 < v.wheels.size() ? " },\n" : " }\n");
        }
        out.append("  ],\n  \"shape\": [\n");
        for (int i = 0; i < v.boxes.size(); i++) {
            Box b = v.boxes.get(i);
            out.append("    { \"from\": [").append(n(b.x1)).append(", ").append(n(b.y1)).append(", ").append(n(b.z1))
                    .append("], \"to\": [").append(n(b.x2)).append(", ").append(n(b.y2)).append(", ").append(n(b.z2))
                    .append("], \"color\": \"").append(b.color).append("\"")
                    .append(b.tilt == 0 ? "" : ", \"tilt\": " + n(b.tilt))
                    .append(b.steers ? ", \"steers\": true" : "").append(" }")
                    .append(i + 1 < v.boxes.size() ? ",\n" : "\n");
        }
        out.append("  ]\n}\n");
        File file = new File("src/main/resources/data/car/car/vehicle/" + v.name + ".json");
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), out);
        System.out.println("wrote " + file.getPath() + " (" + v.boxes.size() + " boxes)");
    }

    static final List<Integer> PALETTE = new ArrayList<>();

    static int shade(int rgb, double factor) {
        int r = (int) Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = (int) Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = (int) Math.min(255, Math.round((rgb & 0xFF) * factor));
        return r << 16 | g << 8 | b;
    }

    /** Which of the item's three paint tints a colour takes, or -1 for a fixed colour. */
    static int tintIndex(String color) {
        return switch (color) {
            case "paint" -> 0;
            case "paint_dark" -> 1;
            case "paint_light" -> 2;
            default -> -1;
        };
    }

    static int swatch(Vehicle v, String color) {
        // Paint is drawn white and tinted in the game by whatever the item
        // was dyed (car:paint), so a red Corolla is red in the hand too.
        int rgb = tintIndex(color) >= 0 ? 0xFFFFFF : Integer.parseInt(color.substring(1), 16);
        int index = PALETTE.indexOf(rgb);
        if (index < 0) {
            PALETTE.add(rgb);
            index = PALETTE.size() - 1;
        }
        return index;
    }

    static void element(StringBuilder out, Vehicle v, double scale, double lift,
                        double x1, double y1, double z1, double x2, double y2, double z2,
                        String color, boolean last) {
        element(out, v, scale, lift, x1, y1, z1, x2, y2, z2, color, 0, last);
    }

    /**
     * {@code turn} is about the across axis, in the item model's own sense,
     * and an item model only turns in steps of 22.5 up to 45.
     */
    static void element(StringBuilder out, Vehicle v, double scale, double lift,
                        double x1, double y1, double z1, double x2, double y2, double z2,
                        String color, double turn, boolean last) {
        int index = swatch(v, color);
        double u = index % 16 + 0.25;
        double w = index / 16 + 0.25;
        String uv = "[" + n(u) + ", " + n(w) + ", " + n(u + 0.5) + ", " + n(w + 0.5) + "]";
        // An item model has north as -z, which is where the nose goes.
        out.append("    { \"from\": [").append(n(8 + x1 * scale)).append(", ").append(n(lift + y1 * scale))
                .append(", ").append(n(8 - z2 * scale)).append("], \"to\": [").append(n(8 + x2 * scale))
                .append(", ").append(n(lift + y2 * scale)).append(", ").append(n(8 - z1 * scale))
                .append("], ");
        double snapped = Math.max(-45, Math.min(45, Math.round(turn / 22.5) * 22.5));
        if (snapped != 0) {
            out.append("\"rotation\": { \"origin\": [").append(n(8 + (x1 + x2) / 2 * scale)).append(", ")
                    .append(n(lift + (y1 + y2) / 2 * scale)).append(", ").append(n(8 - (z1 + z2) / 2 * scale))
                    .append("], \"axis\": \"x\", \"angle\": ").append(n(snapped)).append(" }, ");
        }
        out.append("\"faces\": {");
        String[] faces = {"north", "south", "east", "west", "up", "down"};
        for (int i = 0; i < faces.length; i++) {
            out.append(" \"").append(faces[i]).append("\": { \"uv\": ").append(uv)
                    .append(", \"texture\": \"#p\"")
                    .append(tintIndex(color) >= 0 ? ", \"tintindex\": " + tintIndex(color) : "").append(" }").append(i + 1 < faces.length ? "," : " ");
        }
        out.append("} }").append(last ? "\n" : ",\n");
    }

    static void icon(Vehicle v) throws Exception {
        double length = 0;
        double top = 0;
        for (Box b : v.boxes) {
            length = Math.max(length, Math.max(Math.abs(b.z1), Math.abs(b.z2)) * 2);
            top = Math.max(top, b.y2);
        }
        double scale = 22.0 / length;
        double lift = 8 - top * scale / 2;
        StringBuilder out = new StringBuilder("{\n  \"textures\": { \"particle\": \"car:item/vehicle_palette\","
                + " \"p\": \"car:item/vehicle_palette\" },\n  \"elements\": [\n");
        for (Box b : v.boxes) {
            if (b.color.length() == 9) {
                continue; // glass: an item is solid, and an empty window reads better than a grey one
            }
            // The load is not known to an item; its sight glass shows a pale blue-grey.
            String colour = CARGO.equals(b.color) ? "#A9C6D3" : b.color;
            element(out, v, scale, lift, b.x1, b.y1, b.z1, b.x2, b.y2, b.z2, colour, b.tilt, false);
        }
        for (int i = 0; i < v.wheels.size(); i++) {
            Wheel w = v.wheels.get(i);
            double h = w.width / 2;
            // Round, the way the car's own wheels are: eight slabs about the axle.
            double reach = w.radius * Math.cos(Math.PI / 16);
            double side = w.radius * Math.sin(Math.PI / 16);
            for (double turn : new double[] {-45, -22.5, 0, 22.5}) {
                element(out, v, scale, lift, w.x - h, w.y - reach, w.z - side,
                        w.x + h, w.y + reach, w.z + side, "#202023", turn, false);
                element(out, v, scale, lift, w.x - h, w.y - side, w.z - reach,
                        w.x + h, w.y + side, w.z + reach, "#202023", turn, false);
            }
            double rim = w.radius * 0.58;
            String hub = "#C4C8CD";
            element(out, v, scale, lift, w.x - h - 0.3, w.y - rim * 0.92, w.z - rim * 0.38,
                    w.x + h + 0.3, w.y + rim * 0.92, w.z + rim * 0.38, hub, false);
            element(out, v, scale, lift, w.x - h - 0.3, w.y - rim * 0.38, w.z - rim * 0.92,
                    w.x + h + 0.3, w.y + rim * 0.38, w.z + rim * 0.92, hub, false);
            element(out, v, scale, lift, w.x - h - 0.3, w.y - rim * 0.7, w.z - rim * 0.7,
                    w.x + h + 0.3, w.y + rim * 0.7, w.z + rim * 0.7, hub,
                    i + 1 == v.wheels.size());
        }
        out.append("  ],\n  \"display\": {\n"
                + "    \"gui\": { \"rotation\": [30, 225, 0], \"translation\": [0, 0, 0], \"scale\": [0.72, 0.72, 0.72] },\n"
                + "    \"ground\": { \"translation\": [0, 3, 0], \"scale\": [0.4, 0.4, 0.4] },\n"
                + "    \"fixed\": { \"rotation\": [0, 90, 0], \"scale\": [0.7, 0.7, 0.7] },\n"
                + "    \"head\": { \"scale\": [0.8, 0.8, 0.8] },\n"
                + "    \"thirdperson_righthand\": { \"rotation\": [75, 45, 0], \"translation\": [0, 2.5, 0], \"scale\": [0.35, 0.35, 0.35] },\n"
                + "    \"thirdperson_lefthand\": { \"rotation\": [75, 45, 0], \"translation\": [0, 2.5, 0], \"scale\": [0.35, 0.35, 0.35] },\n"
                + "    \"firstperson_righthand\": { \"rotation\": [0, 45, 0], \"translation\": [0, 3.5, 0], \"scale\": [0.24, 0.24, 0.24] },\n"
                + "    \"firstperson_lefthand\": { \"rotation\": [0, 225, 0], \"translation\": [0, 3.5, 0], \"scale\": [0.24, 0.24, 0.24] }\n"
                + "  }\n}\n");
        File file = new File("src/main/resources/assets/car/models/item/vehicle_" + v.name + ".json");
        Files.writeString(file.toPath(), out);
        System.out.println("wrote " + file.getPath());
    }

    public static void main(String[] args) throws Exception {
        List<Vehicle> all = List.of(corolla(), sahin(), wrangler(), mustang(), hilux(), atego(),
                trailer(), vespa(), cbr());
        item("car", all, "car", "toyota_corolla");
        item("pickup", all, "pickup", "toyota_hilux");
        item("fuel_tanker", all, "tanker", "mercedes_atego");
        item("trailer", all, "trailer", "box_trailer");
        item("motorcycle", all, "motorcycle", "honda_cbr600rr");
        for (Vehicle v : all) {
            v.finish();
            definition(v);
            icon(v);
        }
        BufferedImage palette = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 256; i++) {
            int rgb = PALETTE.get(Math.min(i, PALETTE.size() - 1));
            palette.setRGB(i % 16, i / 16, 0xFF000000 | rgb);
        }
        File file = new File("src/main/resources/assets/car/textures/item/vehicle_palette.png");
        ImageIO.write(palette, "PNG", file);
        System.out.println("wrote " + file.getPath() + " (" + PALETTE.size() + " colours)");
    }
}
