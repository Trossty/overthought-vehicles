// Bayliner 175 bowrider: a small runabout. A deep-V hull, an open bow with
// seats round it, a walk-through windscreen between two consoles, two
// bucket seats and a bench, and a stern drive under a swim platform.
const P = 'paint';
const WHITE = '#F2F2F0', LINER = '#E6E6E2', NAVY = '#2B3A55', RAIL = '#1B1B1D';
const CUSH = '#D8CCB0', CUSH_DARK = '#C4B695', FLOOR = '#C9C1AE', ALU = '#3B3B40';
const DRIVE = '#1F2022', PROP = '#9AA0A6';
const TR = -42, DECK = 16.0, SOLE = 9.3, WAIST = 13.4;

// half the beam along the bow, seen from above
const PL = [[10, 17], [22, 15.7], [32, 12.8], [40, 8.2], [46, 3.0]];
const hw = z => {
    if (z <= PL[0][0]) {
        return PL[0][1];
    }
    for (let i = 1; i < PL.length; i++) {
        if (z <= PL[i][0]) {
            const [z0, x0] = PL[i - 1], [z1, x1] = PL[i];
            return x0 + (x1 - x0) * (z - z0) / (z1 - z0);
        }
    }
    return PL[PL.length - 1][1];
};
const STEPS = [10, 22, 27, 32, 34.7, 37.4, 40, 42, 44, 46];
// what fills the bow behind its skin, between two heights
function fill(y1, y2, color, inset, zFrom, zTo, inner) {
    for (let i = 0; i < STEPS.length - 1; i++) {
        const a = Math.max(STEPS[i], zFrom), b = Math.min(STEPS[i + 1], zTo);
        const half = hw(b) - inset;
        if (b <= a || half <= (inner || 0) + 0.3) {
            continue;
        }
        if (inner) {
            k.pair(inner, y1, a, half, y2, b, color);
        } else {
            k.box(-half, y1, a, half, y2, b, color);
        }
    }
}
// the bow's skin: a run of walls along the plan
function skin(y1, y2, thick, color, off) {
    for (let i = 0; i < PL.length - 1; i++) {
        k.walls(y1, y2, PL[i][1], PL[i][0], PL[i + 1][1], PL[i + 1][0], thick, color, off);
    }
}

// ---- the hull -------------------------------------------------------------------------
k.g('hull');
// the bottom: a V from the keel out to the chine, drawn up to the stem
k.sym(() => k.lean(TR, 10, 0, 0.2, 16.6, 4.8, 1.6, NAVY));
k.box(-0.7, 0, TR, 0.7, 1.4, 10, NAVY);
k.box(-12.0, 3.0, TR, 12.0, 4.6, 10, NAVY);
for (const s of [1, -1]) {
    for (const x of [1.8, 5.6, 9.4, 13.2]) {
        k.beam([s * x, 0.9 + 0.28 * x, 10], [s * 0.4, 5.2, 44.6], 4.2, 1.5, NAVY);
    }
}
k.beam([0, 0.7, 10], [0, 5.0, 45.2], 1.4, 1.4, NAVY);
// the sides, in the boat's colour up to the waist and white above
k.box(-17, 4.6, TR, 17, SOLE, 10, P);
k.pair(15.4, SOLE, TR, 17, WAIST, 10, P);
k.pair(15.4, WAIST, TR, 17, 15.4, 10, WHITE);
k.box(-15.4, SOLE, TR, 15.4, WAIST, -40.6, P);
k.box(-15.4, WAIST, TR, 15.4, 15.4, -40.6, WHITE);
skin(4.6, WAIST, 1.6, P);
skin(WAIST, 15.4, 1.6, WHITE);
fill(4.6, SOLE, P, 0.1, 10, 46);
k.box(-3.0, 4.6, 46, 3.0, DECK, 46.7, WHITE);
k.box(-1.7, 4.6, 46.7, 1.7, DECK, 47.3, WHITE);
// a dark stripe under the waist, and the rubbing strake
k.pair(17, 12.3, TR, 17.12, 13.0, 10, NAVY);
skin(12.3, 13.0, 0.12, NAVY, 1);
k.box(-17, 12.3, TR - 0.12, 17, 13.0, TR, NAVY);
k.pair(17, 14.3, TR - 0.3, 17.3, 15.0, 10, RAIL);
skin(14.3, 15.0, 0.3, RAIL, 1);
k.box(-17, 14.3, TR - 0.3, 17, 15.0, TR, RAIL);
k.box(-1.9, 14.3, 47.3, 1.9, 15.0, 47.5, RAIL);
// the gunwale round the top
k.pair(14.2, 15.4, TR, 17, DECK, 10, WHITE);
skin(15.4, DECK, 2.8, WHITE);
k.box(-14.2, 15.4, TR, 14.2, DECK, -38.6, WHITE);
fill(14.6, DECK, WHITE, 0.1, 38.4, 46);
// on the foredeck: a locker hatch, a cleat, the lamps
k.box(-2.6, DECK, 39.4, 2.6, DECK + 0.08, 43.0, LINER);
k.box(-0.3, DECK, 44.2, 0.3, DECK + 0.5, 45.6, C.CHROME);
k.box(-1.0, DECK + 0.5, 44.6, 1.0, DECK + 0.7, 45.2, C.CHROME);
k.box(0.5, 15.2, 47.3, 1.6, 15.9, 47.5, C.TAIL_GLOW);
k.box(-1.6, 15.2, 47.3, -0.5, 15.9, 47.5, C.DISPLAY);
k.box(-0.9, 13.4, 47.3, 0.9, 14.2, 47.5, C.LAMP_GLOW);
// a rail to hold along each side of the bow
k.sym(() => {
    const pts = [[20, hw(20) - 1.3], [30, hw(30) - 1.3], [38.5, hw(38.5) - 1.5]];
    for (let i = 0; i < pts.length; i++) {
        const [z, x] = pts[i];
        k.box(x - 0.2, DECK, z - 0.2, x + 0.2, DECK + 1.3, z + 0.2, C.CHROME);
        if (i > 0) {
            k.beam([pts[i - 1][1], DECK + 1.3, pts[i - 1][0]], [x, DECK + 1.3, z], 0.4, 0.4, C.CHROME);
        }
    }
    // a cleat on each quarter, cup holders in the gunwale
    k.box(15.3, DECK, -37.6, 16.1, DECK + 0.5, -35.4, C.CHROME);
    k.box(14.6, DECK, -8.4, 15.8, DECK + 0.06, -7.2, RAIL);
});
k.box(15.2, DECK, -30.6, 16.4, DECK + 0.25, -29.4, C.CHROME);

// ---- the stern --------------------------------------------------------------------------
k.box(-11.5, 6.0, -46.6, 11.5, 6.9, TR, WHITE);
k.box(-10.5, 6.9, -46.2, 10.5, 7.0, TR - 0.3, FLOOR);
k.sym(() => k.slab(8.6, 9.6, 6.0, -46.0, 3.6, TR, 0.8, WHITE));
// the boarding ladder folded on the platform
k.pair(6.6, 7.0, -46.0, 6.9, 7.3, -42.8, C.CHROME);
for (const z of [-45.4, -44.2]) {
    k.box(6.9, 7.0, z, 9.0, 7.25, z + 0.4, C.CHROME);
}
k.box(9.0, 7.0, -46.0, 9.3, 7.3, -42.8, C.CHROME);
// the drive: its shield on the transom, the leg, the plate, the screw
k.box(-2.8, 1.8, -43.4, 2.8, 5.6, TR, DRIVE);
k.box(-1.7, 0.4, -48.2, 1.7, 5.2, -43.4, DRIVE);
k.box(-1.0, -3.2, -47.6, 1.0, 0.4, -44.8, DRIVE);
k.box(-2.6, -0.5, -50.4, 2.6, -0.1, -44.4, DRIVE);
k.box(-1.3, -4.4, -48.8, 1.3, -2.8, -44.2, DRIVE);
k.box(-0.25, -5.0, -47.4, 0.25, -4.4, -45.0, DRIVE);
k.box(-0.8, -4.3, -50.3, 0.8, -2.9, -48.8, PROP);
k.box(-2.5, -3.9, -50.0, 2.5, -3.3, -49.3, PROP);
k.box(-0.3, -5.0, -50.0, 0.3, -2.2, -49.3, PROP);
k.box(-0.5, -3.9, -50.8, 0.5, -3.3, -50.3, C.CHROME);
// a towing eye, the stern light on its pole, the filler
k.box(-0.5, DECK, -41.4, 0.5, DECK + 1.3, -40.8, C.CHROME);
k.box(12.6, DECK, -41.2, 13.0, DECK + 5.4, -40.8, C.CHROME);
k.box(12.4, DECK + 5.4, -41.4, 13.2, DECK + 6.2, -40.6, '#ECECE6');

// ---- the cockpit ------------------------------------------------------------------------
k.g('interior');
k.pair(15.2, SOLE, -40.6, 15.4, 15.4, 10, LINER);
k.box(-15.2, SOLE, -40.6, 15.2, SOLE + 0.15, 10, FLOOR);
k.box(-4.2, SOLE, 10, 4.2, SOLE + 0.15, 33, FLOOR);
k.box(-2.4, SOLE + 0.15, -24, 2.4, SOLE + 0.22, 4, LINER);
// the bow: a seat round three sides with the well between
fill(SOLE, 12.0, LINER, 0.1, 16, 33, 4.2);
fill(SOLE, 12.0, LINER, 0.1, 33, 38.4);
fill(12.0, 13.2, CUSH, 0.5, 16.2, 33, 4.4);
fill(12.0, 13.2, CUSH, 0.5, 33, 38.0);
k.pair(4.4, 13.2, 14.9, 13.6, 15.6, 16.2, CUSH);
k.pair(5.6, 13.6, 16.2, 12.4, 15.0, 16.3, CUSH_DARK);
// a console each side of the way through
k.pair(4.2, SOLE, 6.5, 15.4, DECK, 14.6, LINER);
k.pair(4.2, 10.4, 6.38, 4.32, 15.2, 14.4, FLOOR);
// the helm: a pod with the dials behind the wheel, switches, the throttle
k.box(-11.4, DECK, 7.0, -4.8, 22.4, 10.6, LINER);
k.slab(-11.4, -4.8, DECK, 13.4, 22.4, 10.6, 0.5, LINER);
k.box(-11.0, 17.2, 6.9, -5.2, 22.0, 7.0, C.BLACK);
for (const x of [-10.6, -7.0]) {
    k.box(x, 19.8, 6.8, x + 1.6, 21.4, 6.9, C.DIAL);
    k.box(x + 0.7, 20.5, 6.74, x + 0.9, 21.3, 6.8, C.NEEDLE);
}
for (const x of [-10.5, -9.2, -7.9, -6.6]) {
    k.box(x, 17.6, 6.8, x + 0.9, 18.5, 6.9, C.DIAL);
}
k.box(-8.7, 19.9, 6.8, -7.3, 20.5, 6.9, C.DISPLAY);
k.box(-14.4, 9.6, 6.3, -5.0, 15.4, 6.5, FLOOR);
k.box(-15.2, 14.2, 0.4, -14.2, DECK + 0.6, 3.4, C.BLACK);
k.beam([-14.5, DECK + 0.4, 2.0], [-14.5, 19.6, 3.4], 0.4, 0.5, C.CHROME);
k.box(-14.9, 19.2, 3.0, -14.1, 20.2, 4.0, C.BLACK);
// across from it: a locker door, the radio, a handle
k.box(5.2, 10.6, 6.38, 14.2, 15.0, 6.5, FLOOR);
k.box(12.6, 12.4, 6.3, 13.4, 13.2, 6.38, C.CHROME);
k.box(6.0, 13.6, 6.3, 9.4, 14.6, 6.38, C.BLACK);
k.box(6.4, 13.85, 6.24, 8.4, 14.35, 6.3, C.DISPLAY);
k.beam([5.4, DECK + 0.7, 8.0], [10.6, DECK + 0.7, 8.0], 0.4, 0.4, C.CHROME);
for (const x of [5.4, 10.6]) {
    k.box(x - 0.2, DECK, 7.8, x + 0.2, DECK + 0.7, 8.2, C.CHROME);
}
// two seats on pedestals
k.sym(() => {
    k.box(7.0, SOLE, -1.4, 9.0, 10.4, 1.4, ALU);
    k.box(4.9, 10.4, -3.6, 11.1, 12.0, 3.6, CUSH);
    k.box(5.9, 12.0, -2.4, 10.1, 12.1, 3.0, CUSH_DARK);
    k.pair(4.9, 12.0, -3.2, 5.7, 12.8, 3.2, CUSH);
    k.box(10.3, 12.0, -3.2, 11.1, 12.8, 3.2, CUSH);
    k.slab(4.9, 11.1, 11.6, -3.2, 20.2, -4.9, 1.5, CUSH);
    k.slab(5.9, 10.1, 13.2, -3.4, 18.8, -4.5, 0.2, CUSH_DARK, 1);
});
// the bench across the back, a pad over the engine behind it
k.box(-14.4, SOLE, -37.2, 14.4, 10.8, -28.6, LINER);
k.box(-14.6, 10.8, -37.4, 14.6, 12.0, -28.0, CUSH);
k.pair(1.2, 12.0, -36.4, 13.4, 12.1, -28.8, CUSH_DARK);
k.slab(-14.6, 14.6, 11.8, -37.0, 17.2, -38.2, 1.5, CUSH);
k.sym(() => k.slab(1.2, 13.4, 13.0, -37.1, 16.4, -37.9, 0.2, CUSH_DARK, 1));
k.box(-9.0, DECK, -41.4, 9.0, DECK + 0.6, -38.9, CUSH);

// ---- the windscreen ---------------------------------------------------------------------
const WB = 14.0, WT = 11.3, WY = 23.0;
k.g('glass');
k.slab(4.5, 15.2, DECK, WB, WY, WT, 0.3, C.GLASS);
k.slab(-15.2, -4.5, DECK, WB, WY, WT, 0.3, C.GLASS);
k.slab(-4.1, 4.1, DECK, WB, WY, WT, 0.3, C.GLASS);
k.sideGlass({xLow: 15.6, yLow: DECK, xHigh: 15.6, yHigh: 22.4, rearLow: 1.0, rearHigh: 7.6,
    frontLow: WB - 0.2, frontHigh: 11.3, bands: 8});

k.g('frame');
const wz = y => WB - (y - DECK) * (WB - WT) / (WY - DECK);
k.slab(-15.6, 15.6, WY - 0.5, wz(WY - 0.5) + 0.1, WY + 0.1, wz(WY + 0.1) + 0.1, 0.5, ALU);
k.sym(() => {
    k.slab(4.1, 4.5, DECK, WB + 0.1, WY, WT + 0.1, 0.5, ALU);
    k.slab(15.2, 15.9, DECK, WB + 0.1, WY, WT + 0.1, 0.6, ALU);
    k.beam([15.6, 22.5, 11.3], [15.6, 22.5, 7.4], 0.5, 0.5, ALU);
    k.beam([15.6, DECK + 0.1, 0.9], [15.6, 22.6, 7.6], 0.5, 0.6, ALU);
    k.box(15.3, DECK, 0.8, 15.9, DECK + 0.4, 14.0, ALU);
});
k.pair(4.5, DECK, 13.5, 15.2, DECK + 0.4, 14.1, ALU);
k.box(-4.1, DECK, 13.5, 4.1, DECK + 0.3, 14.1, ALU);
