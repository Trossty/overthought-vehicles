package me.trossty.car.entity;

/**
 * The rubber a slide leaves on the road, and the ruts a wheel cuts in soft
 * ground: strips laid flat where the tyres went, fading over a minute.
 *
 * <p>Only a picture, so only the client keeps them, and each client lays its
 * own from what it sees the cars do. This class is just the list; the client
 * draws it.
 */
public final class TyreMarks {

    /** How long a mark lasts, in ticks of game time; it fades over the last third. */
    public static final long LIFETIME = 1200L;
    private static final int CAPACITY = 4096;

    /**
     * One strip of tread.
     *
     * @param dark how dark it is at its freshest, 0 to 1
     * @param born the game time it was laid
     */
    public record Mark(double x1, double y1, double z1, double x2, double y2, double z2,
                       float width, float dark, long born) {
    }

    private static final Mark[] MARKS = new Mark[CAPACITY];
    private static int next;

    private TyreMarks() {
    }

    public static void add(Mark mark) {
        MARKS[next] = mark;
        next = (next + 1) % CAPACITY;
    }

    /** Every mark still fresh enough to see at this game time. */
    public static void forEach(long now, java.util.function.Consumer<Mark> each) {
        for (Mark mark : MARKS) {
            if (mark != null && now - mark.born() < LIFETIME && now >= mark.born()) {
                each.accept(mark);
            }
        }
    }

    /** A new world starts with clean roads. */
    public static void clear() {
        java.util.Arrays.fill(MARKS, null);
        next = 0;
    }
}
