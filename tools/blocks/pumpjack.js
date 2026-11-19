// A beam pump over its well, after a conventional crank-balanced unit, one
// block wide, three long and three high, facing north. The well is in the
// front block: casing flange, tee with its flowline and gauge, stuffing box
// and the polished rod. A concrete pad carries a steel skid; on it the
// Samson post, two legs leaning back from the front and one leaning forward
// from behind, with a ladder up its face to the platform and the bearing at
// the top. Behind it the gear reducer with the crank shaft through its hump,
// the motor on its base behind that, its belt guard on the west side, and
// the starter panel with its running lamp at the back corner.
//
// The walking beam and the crank are models of their own, turned by the
// renderer: the beam about (8, 8, 8) of the block above the post's (whole
// (8, 40, 24)), the crank about (8, 2, 8) of the back block in the middle
// row (whole (8, 18, 40)). The horse head's face is an arc about the beam's
// pivot, so the bridle hangs over the well, and the crank's counterweights
// ride on the arms with the pins the pitman arms hang on.
const BODY = C.YELLOW, BODY_DARK = C.YELLOW_DARK, STEEL = C.DARK_STEEL;
const SIN = Math.sin(Math.PI / 8), COS = Math.cos(Math.PI / 8);

/** Runs draw, then moves what it drew by d. */
function moved(d, draw) {
    const start = k.boxes.length;
    draw();
    for (const b of k.boxes.slice(start)) {
        for (let a = 0; a < 3; a++) {
            b.from[a] += d[a];
            b.to[a] += d[a];
        }
        if (b.rot) {
            b.rot = Object.assign({}, b.rot, {origin: b.rot.origin.map((v, a) => v + d[a])});
        }
    }
}

/**
 * A leg leaning 22.5 degrees in the y-z plane, from its foot at (y1, z1) up
 * to y2, its top towards +z (lean 1) or -z (lean -1); cut into pieces so
 * each block of the machine gets the part of it that is in it.
 */
function leg(x1, x2, y1, z1, y2, thick, lean, paint, pieces) {
    const len = (y2 - y1) / COS, part = len / pieces;
    for (let i = 0; i < pieces; i++) {
        const s = part * (i + 0.5);
        const y = y1 + s * COS, z = z1 + lean * s * SIN;
        k.box(x1, y - part / 2, z - thick / 2, x2, y + part / 2, z + thick / 2, paint,
            {rot: ['x', lean * 22.5, [(x1 + x2) / 2, y, z]]});
    }
}

/** Where a front leg's middle line is at height y. */
const frontZ = y => 9.8 + (y - 3.7) * SIN / COS;

function jack(lit) {
    // ---- the pad and the skid -------------------------------------------------------
    k.g('pad');
    k.box(0, 0, 0, 16, 1.5, 48, C.CONCRETE);
    k.g('skid');
    k.sym(() => {
        k.box(1, 1.5, 9, 3.4, 1.9, 47, STEEL);
        k.box(1.9, 1.9, 9, 2.5, 3.3, 47, STEEL);
        k.box(1, 3.3, 9, 3.4, 3.7, 47, STEEL);
    });
    for (const [z1, z2] of [[13, 14.4], [28, 29.4], [36.6, 38.6], [45.6, 47]]) {
        k.box(3.4, 1.5, z1, 12.6, 3.7, z2, STEEL);
    }

    // ---- the well -----------------------------------------------------------------
    k.g('well');
    k.round('y', 8, 2, 8, 4.4, 1, C.STEEL, {sides: 16});
    for (const [x, z] of [[8, 4.3], [8, 11.1], [4.3, 8], [11.1, 8]]) {
        k.box(x - 0.35, 2.5, z - 0.35, x + 0.35, 2.9, z + 0.35, C.CHROME);
    }
    k.round('y', 8, 4, 8, 2.4, 3, C.STEEL, {sides: 16});
    k.round('y', 8, 5.6, 8, 2.8, 0.4, STEEL, {sides: 16});
    k.round('y', 8, 7, 8, 1.8, 2.8, C.STEEL, {sides: 16});
    k.round('y', 8, 8.6, 8, 2.2, 0.4, STEEL, {sides: 16});
    // the stuffing box and its gland
    k.round('y', 8, 9.3, 8, 1.4, 1, C.STEEL);
    k.round('y', 8, 10.05, 8, 1, 0.5, C.BRASS);
    // the polished rod, cut where the blocks meet
    k.round('y', 8, 13.15, 8, 0.45, 5.7, C.CHROME);
    k.round('y', 8, 23, 8, 0.45, 14, C.CHROME);
    // the flowline off the tee, east, with its gate valve, then down
    k.g('flowline');
    k.round('x', 11.7, 7, 8, 0.8, 4.2, STEEL);
    k.box(11.2, 6, 7, 12.6, 8, 9, C.RED_DARK);
    k.round('y', 11.9, 8.4, 8, 0.25, 0.8, C.STEEL);
    k.round('y', 11.9, 8.9, 8, 1.1, 0.25, C.RED, {sides: 16});
    k.round('y', 14.6, 4.6, 8, 0.8, 6.2, STEEL);
    k.box(13.8, 6.2, 7.2, 15.4, 7.8, 8.8, STEEL);
    k.round('y', 14.6, 1.75, 8, 1.3, 0.5, C.STEEL, {sides: 16});
    // the gauge on the west outlet
    k.g('gauge');
    k.round('x', 5, 7, 8, 0.5, 1.8, C.STEEL);
    k.round('z', 4, 7.4, 7.2, 1.1, 0.5, C.CHROME, {sides: 16});
    k.box(3.3, 6.7, 6.9, 4.7, 8.1, 6.95, C.DIAL, {only: ['north']});
    k.box(3.95, 7.3, 6.86, 4.05, 7.95, 6.9, C.NEEDLE, {only: ['north'], rot: ['z', -22.5, [4, 7.4, 6.88]]});

    // ---- the Samson post ------------------------------------------------------------
    k.g('post');
    k.sym(() => {
        leg(1.25, 3.25, 3.7, 9.8, 35.6, 2, 1, BODY, 5);
        k.box(0.9, 3.7, 9, 3.6, 4.1, 11.6, STEEL);
    });
    leg(6.75, 9.25, 3.7, 37.6, 35.6, 2.5, -1, BODY, 5);
    k.box(6.2, 3.7, 36.4, 9.8, 4.1, 38.8, STEEL);
    // braces across the front legs and back to the rear one
    for (const y of [14, 26]) {
        const z = frontZ(y + 0.6);
        k.box(3.25, y, z - 0.6, 12.75, y + 1.2, z + 0.6, BODY_DARK);
    }
    k.box(7.4, 14, frontZ(14.6) + 0.6, 8.6, 15.2, 24.4 + (36 - 14.6) * SIN / COS - 1, BODY_DARK);
    // the ladder up the front, from the first brace
    k.g('post/ladder');
    for (const x of [6.2, 9.4]) {
        leg(x, x + 0.4, 9, frontZ(9) - 0.9, 35.6, 0.4, 1, C.STEEL, 4);
    }
    for (let y = 10.5; y < 35; y += 2.4) {
        const z = frontZ(y) - 0.9;
        k.box(6.6, y - 0.2, z - 0.2, 9.4, y + 0.2, z + 0.2, C.STEEL);
    }
    // the cap, its platform and rails, and the bearing on top
    k.g('post/cap');
    k.box(1, 34.8, 21.2, 15, 35.6, 26.8, BODY_DARK);
    k.box(0.4, 35.6, 20.4, 15.6, 36, 27.6, C.STEEL);
    k.sym(() => {
        for (const z of [20.4, 27.2]) {
            k.box(0.4, 36, z, 0.8, 40, z + 0.4, BODY);
        }
        k.box(0.4, 39.6, 20.8, 0.8, 40, 27.2, BODY);
        k.box(0.4, 37.8, 20.8, 0.8, 38.1, 27.2, BODY);
        // a pillow block
        k.box(3.4, 36, 22.2, 5.4, 38.6, 25.8, STEEL);
        k.round('x', 4.4, 40, 24, 1.6, 2, STEEL, {sides: 16});
        k.box(3.2, 36, 21.6, 5.6, 36.6, 26.4, STEEL);
        for (const z of [22, 25.6]) {
            k.box(3.6, 36.6, z, 4.2, 37, z + 0.4, C.CHROME);
        }
    });
    k.round('x', 8, 40, 24, 0.6, 9.8, C.CHROME);

    // ---- the gear reducer, on its pedestal ---------------------------------------------
    k.g('reducer');
    k.box(4.6, 1.5, 39.2, 11.4, 9, 43.6, STEEL);
    k.sym(() => {
        k.box(4.55, 2.6, 39.8, 4.6, 8, 43, C.BLACK, {only: ['west']});
    });
    k.box(5.2, 9, 37.4, 10.8, 14.4, 46, C.STEEL);
    k.box(5, 14.4, 37.2, 11, 15, 46.2, STEEL);
    k.round('x', 8, 18, 40, 3.6, 5.2, C.STEEL, {sides: 16});
    k.round('x', 8, 15.4, 43.4, 2.4, 5.2, C.STEEL, {sides: 16});
    // an inspection cover, the breather, the bearing caps the shafts run in
    k.box(6.2, 17.6, 41.6, 9.8, 17.9, 43.4, STEEL, {rot: ['x', -22.5, [8, 17.75, 42.5]]});
    k.round('y', 9.2, 18.5, 41.4, 0.4, 0.8, C.BRASS);
    k.sym(() => {
        k.round('x', 5, 18, 40, 2, 0.8, STEEL, {sides: 16});
        k.round('x', 4.85, 15.4, 43.4, 1.2, 0.5, STEEL, {sides: 16});
    });
    // a plate on its flank, and the drain under it
    k.box(10.8, 10, 39, 10.85, 12.6, 42.4, C.ALU, {only: ['east']});
    k.round('y', 9, 8.8, 45, 0.4, 0.4, C.BRASS);

    // ---- the motor, under the reducer's back, and its belt guard ----------------------
    k.g('motor');
    k.sym(() => {
        k.box(5.6, 1.5, 43.8, 6.4, 3.4, 47.6, STEEL);
    });
    k.round('x', 8, 5.4, 45.8, 2, 4.6, C.GREEN_DARK, {sides: 16});
    for (const x of [6.4, 7.4, 8.4, 9.4]) {
        k.box(x, 7.2, 44.6, x + 0.4, 7.5, 47, C.GREEN_DARK);
    }
    k.box(7.4, 7.2, 46.2, 8.8, 8.4, 47.4, C.GREEN_DARK);
    k.round('x', 10.9, 5.4, 45.8, 1.9, 1, STEEL, {sides: 16});
    k.g('motor/guard');
    k.round('x', 4.65, 5.4, 45.8, 2.15, 0.7, C.YELLOW, {sides: 16});
    k.round('x', 4.65, 12, 45, 1.8, 0.7, C.YELLOW, {sides: 16});
    k.box(4.3, 5.4, 43.6, 5, 12, 47.6, C.YELLOW);

    // ---- the starter panel, its lamp in the back block ---------------------------------
    k.g('panel');
    k.box(14, 3.7, 46.4, 14.6, 4.6, 47, STEEL);
    k.box(12.8, 4.6, 45.6, 15.6, 8, 47.6, C.STEEL);
    k.box(15.6, 6.4, 46, 15.9, 7.4, 46.6, lit ? C.LED_GREEN : C.LED_OFF, {shade: !lit});
    k.box(15.6, 5.2, 46.9, 15.8, 6, 47.3, C.RED);
    k.box(13.2, 8, 46, 15.2, 8.3, 47.2, STEEL);
}

// ---- the walking beam, in the frame of the block over the post: pivot (8, 8, 8) ----
function beam() {
    k.g('beam');
    k.box(5.4, 9.6, -4, 10.6, 10.2, 26.4, BODY);
    k.box(7.3, 10.2, -4, 8.7, 12.6, 26.4, BODY_DARK);
    k.box(5.4, 12.6, -4, 10.6, 13.2, 26.4, BODY);
    for (const z of [2, 14, 20]) {
        k.box(5.6, 10.2, z, 10.4, 12.6, z + 0.4, BODY);
    }
    // the saddle and its bearing
    k.g('beam/saddle');
    k.box(5.4, 9.2, 5, 10.6, 9.6, 11, STEEL);
    for (const x of [5.6, 9.6]) {
        k.box(x, 6.8, 6.6, x + 0.8, 9.2, 9.4, STEEL);
    }
    k.round('x', 8, 8, 8, 1.25, 4.8, C.STEEL, {sides: 16});

    // the horse head: its face an arc about the pivot, sixteen out, in three flats
    k.g('beam/head');
    const half = 16 * Math.tan(Math.PI / 16);
    for (const [turn, lo, hi] of [[0, -half, half], [22.5, -half, 1.7], [-22.5, -half, half]]) {
        const rot = turn ? ['x', turn, [8, 8, 8]] : null;
        k.box(5, 8 + lo, -8, 11, 8 + hi, -2.5, BODY, {rot: rot, faces: {north: STEEL}});
        // the wire ropes over it
        for (const x of [6, 9.6]) {
            k.box(x, 8 + lo, -8.35, x + 0.4, 8 + hi, -8, C.IRON, {rot: rot});
        }
    }
    k.box(5, 2.2, -5, 11, 13.2, -2, BODY);
    k.box(5.6, 1.4, -3.4, 10.4, 9.6, -1.2, BODY_DARK);
    // the bridle down to the carrier bar, the clamp on the rod
    k.g('beam/bridle');
    for (const x of [6, 9.6]) {
        k.box(x, -4.4, -8.35, x + 0.4, 8, -8, C.IRON);
    }
    k.box(5.4, -5.4, -9.2, 10.6, -4.4, -7.2, STEEL);
    k.box(7.2, -4.4, -8.8, 8.8, -3.4, -7.2, C.STEEL);
    k.round('y', 8, -2.9, -8, 0.45, 1, C.CHROME);

    // the equaliser across the tail, and the pitman arms down to the crank pins
    k.g('beam/equaliser');
    k.box(6, 8.8, 22.6, 10, 9.6, 25.4, STEEL);
    k.box(0.2, 7.2, 23, 15.8, 8.8, 25, STEEL);
    k.sym(() => {
        k.box(0.4, -7, 23.3, 1.6, 7.2, 24.7, BODY);
        k.round('x', 1, 8, 24, 1, 1.4, STEEL, {sides: 16});
        k.round('x', 1, -8, 24, 1.3, 1.2, STEEL, {sides: 16});
    });
}

// ---- the crank, in the frame of the back block in the middle row: shaft (8, 2, 8) ----
function crank() {
    k.g('crank');
    k.sym(() => {
        k.round('x', 3.2, 2, 8, 2.2, 2, STEEL, {sides: 16});
        k.round('x', 1.95, 2, 8, 1, 0.5, C.STEEL);
        k.round('x', 4.5, 2, 8, 0.9, 0.6, C.STEEL);
        k.box(2.4, 2, 6.4, 3.8, 8.5, 9.6, STEEL);
        // the counterweight: a fan of three plates about the shaft
        for (const turn of [0, 22.5, -22.5]) {
            k.box(2, 8.5, 5.9, 4.2, 12, 10.1, C.RED, {rot: turn ? ['x', turn, [8, 2, 8]] : null});
        }
        k.box(1.95, 9.6, 7, 2, 11.2, 9, C.WHITE, {only: ['west']});
        // the crank pin the pitman arm hangs on
        k.round('x', 1.6, 8, 8, 0.7, 1.2, C.CHROME);
    });
}

k.model('pumpjack/p', {
    cells: {'0': [0, 0, 0], '1': [0, 0, 1], '2': [0, 0, 2], '3': [0, 1, 0], '4': [0, 1, 1], '5': [0, 1, 2], '6': [0, 2, 1]}
}, () => jack(false));
k.model('pumpjack/p2_lit', {cells: {'': [0, 0, 2]}}, () => jack(true));
k.model('pumpjack/beam', {}, beam);
k.model('pumpjack/crank', {}, crank);
k.model('pumpjack', {file: '../item/pumpjack', shrink: 24}, () => {
    jack(false);
    moved([0, 32, 16], beam);
    moved([0, 16, 32], crank);
});

// the whole machine standing, beam and crank in place: only to look at
k.model('pumpjack/assembled', {preview: true}, () => {
    jack(false);
    moved([0, 32, 16], beam);
    moved([0, 16, 32], crank);
});
