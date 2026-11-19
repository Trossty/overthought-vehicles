// The mechanic's tools and the cans and tins of the trade, each after the real
// one. Most are held and hung in frames at the angles their old models were
// tuned for, so they keep their place in the 16 and their displays.
const RUBBER = C.RUBBER, PLASTIC = C.PLASTIC, HOLE = '#2A2D31', BRASS = '#B8933F',
    ALU_CAST = '#9EA3A9', GUNMETAL = '#62676E', PAPER = '#F1EEE6', BOARD = '#8A6A48';
const KEEP = {display: 'keep', centre: false};

/** A flat eight-sided ring round (cx, cy) across the z axis: its bars a from the middle, b wide, z1..z2 deep. */
function ringZ(cx, cy, a, b, z1, z2, paint) {
    const side = 2 * (a + b / 2) * Math.tan(Math.PI / 8) + 0.04;
    for (const s of [-1, 1]) {
        k.box(cx - side / 2, cy + s * a - b / 2, z1, cx + side / 2, cy + s * a + b / 2, z2, paint);
        k.box(cx + s * a - b / 2, cy - side / 2, z1, cx + s * a + b / 2, cy + side / 2, z2, paint);
    }
    const d = a * Math.SQRT1_2;
    for (const [dx, dy] of [[1, 1], [1, -1], [-1, 1], [-1, -1]]) {
        const px = cx + dx * d, py = cy + dy * d;
        k.box(px - side / 2, py - b / 2, z1, px + side / 2, py + b / 2, z2, paint,
            {rot: ['z', dx * dy > 0 ? -45 : 45, [px, py, (z1 + z2) / 2]]});
    }
}

/** A dial at (cx, cy) facing north from z: bezel, white face, ticks round it and a red needle. */
function dial(cx, cy, z, r) {
    k.g('dial');
    k.round('z', cx, cy, z + 0.15, r, 0.3, C.CHROME, {sides: 16});
    k.round('z', cx, cy, z - 0.05, r - 0.3, 0.1, C.WHITE, {sides: 16});
    for (let i = 0; i < 7; i++) {
        const t = (225 - i * 45) * Math.PI / 180, x = cx + (r - 0.6) * Math.cos(t), y = cy + (r - 0.6) * Math.sin(t);
        k.box(x - 0.1, y - 0.1, z - 0.15, x + 0.1, y + 0.1, z - 0.1, C.BLACK);
    }
    k.box(cx - 0.08, cy - 0.1, z - 0.2, cx + 0.08, cy + r - 0.6, z - 0.15, C.RED, {rot: ['z', -45, [cx, cy, z - 0.175]]});
    k.round('z', cx, cy, z - 0.2, 0.25, 0.1, C.BLACK);
}

// ---- combination spanner ----------------------------------------------------------

k.model('wrench', KEEP, () => {
    // the I-beam shank, its recessed middle a shade darker each side
    k.g('shank');
    k.box(7.3, 3.6, 7.4, 8.7, 12.6, 8.6, C.CHROME);
    k.box(7.6, 4.4, 7.35, 8.4, 11.8, 8.65, C.STEEL);
    // the open end, turned off the shank's line as real ones are: the head and its two jaws
    k.g('open_end');
    const turn = {rot: ['z', 22.5, [8, 14.2, 8]]};
    k.box(6.8, 11.8, 7.25, 9.2, 13.0, 8.75, C.CHROME);
    k.box(5.9, 12.9, 7.2, 10.1, 14.1, 8.8, C.CHROME, turn);
    k.box(5.9, 14.1, 7.2, 7.2, 16.3, 8.8, C.CHROME, turn);
    k.box(8.8, 14.1, 7.2, 10.1, 16.0, 8.8, C.CHROME, turn);
    // the ring end, twelve-pointed inside, thicker than the shank
    k.g('ring_end');
    k.box(7.0, 3.4, 7.25, 9.0, 4.2, 8.75, C.CHROME);
    ringZ(8, 2.05, 1.55, 0.9, 7.2, 8.8, C.CHROME);
});

// ---- compression tester -----------------------------------------------------------

k.model('compression_tester', KEEP, () => {
    // the gauge in its rubber boot, the release button on its side
    k.g('gauge');
    k.round('z', 8.5, 10.0, 8, 3.3, 2.4, C.BLACK, {sides: 16});
    dial(8.5, 10.0, 6.6, 3.0);
    k.box(11.7, 9.6, 7.6, 12.5, 10.4, 8.4, C.CHROME);
    // the hose down from it, the brass adapter that screws into a plug hole at the end
    k.g('hose');
    k.round('y', 8.5, 6.45, 8, 0.65, 0.7, C.CHROME);
    k.round('y', 8.5, 3.6, 8, 0.45, 5.0, RUBBER);
    k.round('y', 8.5, 0.95, 8, 0.7, 0.7, BRASS);
    k.round('y', 8.5, -0.2, 8, 0.45, 1.6, BRASS);
    for (const y of [-0.8, -0.4, 0.0]) {
        k.round('y', 8.5, y, 8, 0.5, 0.12, '#94742F');
    }
});

// ---- floor pump -----------------------------------------------------------------

k.model('tyre_pump', KEEP, () => {
    // the foot plate and the base with its gauge looking up at whoever pumps
    k.g('base');
    k.box(4.0, 0, 6.6, 12.0, 0.6, 9.4, PLASTIC);
    k.round('y', 8, 1.4, 8, 1.8, 1.6, PLASTIC, {sides: 16});
    k.round('z', 8, 1.6, 6.3, 1.3, 0.6, PLASTIC, {sides: 16});
    dial(8, 1.6, 5.85, 1.2);
    // the red barrel, its cap, the rod and the T handle with rubber grips
    k.g('barrel');
    k.round('y', 8, 7.4, 8, 1.3, 10.4, C.RED, {sides: 16});
    k.round('y', 8, 12.9, 8, 1.45, 0.6, PLASTIC, {sides: 16});
    k.round('y', 8, 13.5, 8, 0.3, 0.6, C.CHROME);
    k.box(5.6, 13.8, 7.4, 10.4, 14.8, 8.6, PLASTIC);
    k.box(4.0, 13.7, 7.3, 5.6, 14.9, 8.7, RUBBER);
    k.box(10.4, 13.7, 7.3, 12.0, 14.9, 8.7, RUBBER);
    // the hose up the barrel's side in a clip, the chuck parked at the top
    k.g('hose');
    k.box(9.5, 1.2, 7.8, 10.3, 1.6, 8.2, RUBBER);
    k.round('y', 9.95, 6.5, 8, 0.35, 10.2, RUBBER);
    k.box(9.2, 7.6, 7.6, 10.4, 8.0, 8.4, C.BLACK);
    k.box(9.5, 11.6, 7.5, 10.4, 13.0, 8.5, C.CHROME);
    k.box(9.6, 12.4, 8.5, 10.3, 12.8, 9.5, C.BLACK);
});

// ---- jumper cables ------------------------------------------------------------------

k.model('jumper_cables', KEEP, () => {
    // the pair of leads coiled flat, red and black turn about
    k.g('coil');
    const coil = (y, paint) => {
        const a = 3.6, b = 0.7, side = 2 * (a + b / 2) * Math.tan(Math.PI / 8) + 0.04, d = a * Math.SQRT1_2;
        for (const s of [-1, 1]) {
            k.box(8 - side / 2, y, 8 + s * a - b / 2, 8 + side / 2, y + b, 8 + s * a + b / 2, paint);
            k.box(8 + s * a - b / 2, y, 8 - side / 2, 8 + s * a + b / 2, y + b, 8 + side / 2, paint);
        }
        for (const [dx, dz] of [[1, 1], [1, -1], [-1, 1], [-1, -1]]) {
            const px = 8 + dx * d, pz = 8 + dz * d;
            k.box(px - side / 2, y, pz - b / 2, px + side / 2, y + b, pz + b / 2, paint,
                {rot: ['y', dx * dz > 0 ? 45 : -45, [px, y + b / 2, pz]]});
        }
    };
    coil(2.0, C.RED);
    coil(2.7, C.BLACK);
    coil(3.4, C.RED);
    coil(4.1, C.BLACK);
    // a clamp of each colour laid across the top: insulated handles, copper jaws, the spring at the pivot
    for (const [x0, dir, z, paint] of [[2.4, 1, 6.2, C.RED], [13.6, -1, 9.8, C.BLACK]]) {
        k.g('clamps');
        const x = v => x0 + dir * v;
        k.box(x(0), 4.8, z - 0.6, x(2.6), 5.4, z + 0.6, '#B86A3C');
        k.box(x(0), 5.6, z - 0.6, x(2.6), 6.2, z + 0.6, '#B86A3C');
        k.box(x(0.2), 5.4, z - 0.4, x(2.4), 5.6, z + 0.4, HOLE);
        k.box(x(2.6), 4.8, z - 0.7, x(5.6), 5.5, z + 0.7, paint);
        k.box(x(2.6), 5.9, z - 0.7, x(5.6), 6.6, z + 0.7, paint);
        k.round('z', x(3.0), 5.7, z, 0.45, 1.6, C.CHROME);
    }
});

// ---- seismic survey hammer ----------------------------------------------------------

k.model('seismic_survey', KEEP, () => {
    // a sledgehammer with a trigger switch taped under the head: the strike starts the recording
    k.g('handle');
    k.round('y', 8, 8.2, 8, 0.55, 8.4, C.YELLOW);
    k.round('y', 8, 2.0, 8, 0.7, 4.0, RUBBER);
    k.round('y', 8, 0.15, 8, 0.75, 0.3, RUBBER);
    k.round('y', 8, 12.4, 8, 0.75, 0.8, C.STEEL);
    k.g('head');
    k.round('x', 8, 14.25, 8, 1.5, 7.2, C.DARK_STEEL, {sides: 16});
    k.round('x', 4.3, 14.25, 8, 1.35, 0.6, C.STEEL, {sides: 16});
    k.round('x', 11.7, 14.25, 8, 1.35, 0.6, C.STEEL, {sides: 16});
    k.g('switch');
    k.box(7.3, 10.2, 8.4, 8.7, 11.6, 9.3, C.BLACK);
    k.box(7.4, 10.6, 8.4, 8.6, 11.0, 9.35, '#E07A1F');
    k.box(7.8, 4.0, 8.5, 8.2, 10.2, 8.9, '#E07A1F');
});

// ---- fuel nozzle ---------------------------------------------------------------------

k.model('fuel_nozzle', KEEP, () => {
    // the spout, bent down at its tip, the collar where it meets the cast body
    k.g('spout');
    k.round('z', 8, 9.0, 2.8, 0.45, 2.4, C.STEEL);
    k.box(7.6, 8.6, -1.0, 8.4, 9.4, 1.8, C.STEEL, {rot: ['x', -22.5, [8, 9.0, 1.8]]});
    k.round('z', 8, 9.0, 3.9, 0.75, 0.6, C.CHROME);
    // the aluminium valve body, the red grip cover behind it, the swivel the hose screws into
    k.g('body');
    k.box(6.9, 7.6, 4.2, 9.1, 10.6, 8.2, ALU_CAST);
    k.box(7.2, 7.9, 4.0, 8.8, 10.3, 4.2, ALU_CAST);
    k.box(6.7, 8.4, 8.2, 9.3, 10.8, 12.6, C.RED);
    k.box(7.0, 10.8, 8.6, 9.0, 11.1, 12.2, C.RED_DARK);
    k.round('z', 8, 9.6, 13.3, 0.8, 1.4, C.CHROME, {sides: 16});
    // the lever under the grip and the guard looped round it
    k.g('lever');
    k.box(7.5, 6.8, 5.4, 8.5, 7.6, 11.6, C.BLACK);
    k.box(7.6, 3.6, 4.4, 8.4, 7.6, 5.0, ALU_CAST);
    k.box(7.6, 3.6, 5.0, 8.4, 4.2, 12.6, ALU_CAST);
    k.box(7.6, 4.2, 12.0, 8.4, 8.4, 12.6, ALU_CAST);
});

// ---- charging plug ------------------------------------------------------------------

k.model('charging_plug', KEEP, () => {
    // the cable in through its bellows strain relief
    k.g('cable');
    k.round('y', 8, 1.2, 8, 0.8, 2.4, C.BLACK);
    for (let i = 0; i < 4; i++) {
        k.round('y', 8, 2.6 + i * 0.4, 8, i % 2 ? 0.85 : 1.0, 0.4, RUBBER);
    }
    // the round grip with rubber panels, the head with its green ring lit
    k.g('grip');
    k.round('y', 8, 6.7, 8, 1.7, 5.4, PLASTIC, {sides: 16});
    k.box(6.15, 5.0, 7.3, 6.35, 8.6, 8.7, RUBBER);
    k.box(9.65, 5.0, 7.3, 9.85, 8.6, 8.7, RUBBER);
    k.g('head');
    k.box(5.5, 9.4, 5.5, 10.5, 13.4, 10.5, '#3A3D42');
    k.box(5.45, 11.0, 5.45, 10.55, 11.3, 10.55, C.LED_GREEN, {shade: false});
    k.box(7.2, 13.4, 9.6, 8.8, 13.8, 10.4, C.BLACK);
    // the face: the round AC socket above, the two big DC pins' sockets below it
    k.g('face');
    k.round('y', 8, 13.9, 7.4, 1.95, 1.0, PLASTIC, {sides: 16});
    k.box(6.0, 13.4, 8.9, 10.0, 14.2, 10.3, PLASTIC);
    for (const [x, z] of [[7.2, 6.4], [8.8, 6.4], [6.8, 7.4], [8.0, 7.4], [9.2, 7.4], [7.4, 8.4], [8.6, 8.4]]) {
        k.box(x - 0.2, 14.4, z - 0.2, x + 0.2, 14.45, z + 0.2, HOLE);
    }
    for (const x of [7.0, 9.0]) {
        k.box(x - 0.4, 14.2, 9.2, x + 0.4, 14.25, 10.0, HOLE);
    }
});

// ---- spray can ---------------------------------------------------------------------

k.model('spray_can', KEEP, () => {
    // the can in the colour it holds (tinted by the game), its steel rims, the domed top
    k.g('can');
    k.round('y', 8, 5.85, 8, 2.0, 10.7, C.WHITE, {sides: 16, tint: 1});
    k.round('y', 8, 0.25, 8, 1.95, 0.5, C.CHROME, {sides: 16});
    k.round('y', 8, 11.0, 8, 2.05, 0.3, C.CHROME, {sides: 16});
    k.round('y', 8, 11.6, 8, 1.6, 0.9, C.CHROME, {sides: 16});
    k.round('y', 8, 12.3, 8, 0.9, 0.5, C.STEEL, {sides: 16});
    // the actuator, its colour the paint's, the nozzle hole out the front
    k.g('actuator');
    k.round('y', 8, 13.4, 8, 0.75, 1.7, C.WHITE, {sides: 16, tint: 1});
    k.box(7.7, 13.6, 8.6, 8.3, 14.0, 8.8, HOLE);
});

// ---- car key ------------------------------------------------------------------------

k.model('car_key', KEEP, () => {
    // the fob: black plastic, a chrome band round its edge, two rubber buttons and a blank badge
    k.g('fob');
    k.box(5.2, 8.6, 7.0, 10.8, 13.8, 9.0, PLASTIC);
    k.box(5.6, 8.2, 7.2, 10.4, 14.2, 8.8, PLASTIC);
    k.box(5.1, 8.5, 7.9, 10.9, 13.9, 8.1, C.CHROME);
    k.box(6.4, 11.4, 9.0, 7.9, 12.9, 9.3, RUBBER);
    k.box(8.1, 11.4, 9.0, 9.6, 12.9, 9.3, RUBBER);
    k.box(6.8, 12.0, 9.3, 7.5, 12.3, 9.35, C.WHITE);
    k.box(8.5, 12.0, 9.3, 9.2, 12.3, 9.35, C.WHITE);
    k.box(7.2, 9.4, 9.0, 8.8, 10.6, 9.2, C.CHROME);
    k.box(7.8, 13.2, 9.0, 8.2, 13.5, 9.1, C.RED, {shade: false});
    // the blade with its milled groove and cut teeth
    k.g('blade');
    k.box(7.1, 1.5, 7.6, 8.9, 8.2, 8.4, C.STEEL);
    k.box(7.1, 0.6, 7.6, 8.1, 1.5, 8.4, C.STEEL);
    k.box(7.5, 1.6, 8.4, 8.0, 7.6, 8.45, '#7E848A');
    for (const [y0, y1] of [[2.6, 3.4], [4.6, 5.6], [6.4, 7.2]]) {
        k.box(8.9, y0, 7.7, 9.4, y1, 8.3, C.STEEL);
    }
    // the split ring through the fob's eye
    k.g('ring');
    k.box(6.0, 14.0, 7.7, 6.6, 17.0, 8.3, C.CHROME);
    k.box(9.4, 14.0, 7.7, 10.0, 17.0, 8.3, C.CHROME);
    k.box(6.0, 16.4, 7.7, 10.0, 17.0, 8.3, C.CHROME);
});

// ---- diagnostic sheet -------------------------------------------------------------------

k.model('diagnostic_sheet', {display: 'keep', centre: false}, () => {
    // a clipboard: the board, the sheet clipped to it, the steel clip
    k.g('board');
    k.box(3.5, 1.75, 7.95, 12.5, 13.75, 8.55, BOARD);
    k.box(4.2, 2.25, 7.75, 11.8, 12.45, 7.95, PAPER);
    k.box(6.2, 12.35, 7.45, 9.8, 13.75, 8.05, C.CHROME);
    k.box(7.0, 13.45, 7.65, 9.0, 14.25, 8.25, C.CHROME);
    // the checklist: a box and a written line on each row, ticked green, one crossed red
    k.g('rows');
    const rows = [[10.4, 5.0, C.LED_GREEN], [8.7, 4.0, C.LED_GREEN], [7.0, 6.0, C.RED], [5.3, 3.0, C.LED_GREEN], [3.6, 5.6, null]];
    for (const [y, len, mark] of rows) {
        k.box(4.7, y, 7.7, 5.5, y + 0.8, 7.75, '#3B4250');
        k.box(4.85, y + 0.15, 7.68, 5.35, y + 0.65, 7.7, PAPER);
        if (mark) {
            k.box(4.9, y + 0.2, 7.66, 5.3, y + 0.6, 7.68, mark);
        }
        k.box(5.9, y + 0.25, 7.7, 5.9 + len, y + 0.55, 7.76, '#5A6170');
    }
});

// ---- motor oil --------------------------------------------------------------------------

k.model('motor_oil', KEEP, () => {
    // a five-litre jug: soft-cornered body, the shoulder up to a spout at one side, a hollow handle at the other
    k.g('jug');
    const JUG = '#E3B32A';
    k.box(4.0, 0, 5.9, 12.0, 10.6, 10.1, JUG);
    k.box(4.4, 0, 5.5, 11.6, 10.6, 10.5, JUG);
    k.box(4.4, 10.6, 6.0, 11.6, 11.2, 10.0, JUG);
    k.box(4.8, 11.2, 6.6, 7.6, 12.0, 9.4, JUG);
    k.round('y', 6.2, 12.4, 8, 1.0, 0.8, JUG);
    k.g('cap');
    k.round('y', 6.2, 13.7, 8, 1.3, 1.8, C.BLACK, {sides: 16});
    k.round('y', 6.2, 14.65, 8, 0.9, 0.1, C.TRIM);
    k.g('handle');
    k.box(9.6, 11.2, 7.3, 10.2, 13.0, 8.7, JUG);
    k.box(11.0, 11.2, 7.3, 11.6, 13.0, 8.7, JUG);
    k.box(9.6, 13.0, 7.3, 11.6, 13.8, 8.7, JUG);
    // the label front and back: black, a red stripe, nothing written; the level strip down the side
    k.g('label');
    k.box(4.5, 2.6, 5.4, 11.5, 8.2, 10.6, C.BLACK);
    k.box(4.5, 6.6, 5.35, 11.5, 7.2, 10.65, C.RED);
    k.box(12.0, 0.6, 7.6, 12.1, 10.2, 8.4, '#C9A65A');
});

// ---- jerry cans -------------------------------------------------------------------------

/** The pressed steel can: flat sides with the X stamped in, the welded seam round it, three handles, the cam-lever spout. */
function jerryCan(name, paint, dark) {
    k.model(name, {}, () => {
        k.g('body');
        k.box(4.0, 1.8, 6.3, 12.0, 11.8, 9.7, paint);
        k.box(4.3, 1.8, 6.1, 11.7, 11.6, 9.9, paint);
        k.box(3.85, 1.7, 7.85, 12.15, 11.9, 8.15, dark);
        for (const z of [6.0, 9.9]) {
            k.box(5.0, 3.1, z, 11.0, 10.1, z + 0.1, paint);
            k.box(4.4, 6.25, z - 0.05, 11.6, 6.95, z + 0.15, dark, {rot: ['z', 45, [8, 6.6, z + 0.05]]});
            k.box(4.4, 6.25, z - 0.05, 11.6, 6.95, z + 0.15, dark, {rot: ['z', -45, [8, 6.6, z + 0.05]]});
        }
        k.g('handles');
        for (const x of [4.9, 6.6, 8.3]) {
            k.box(x, 13.2, 6.7, x + 0.7, 13.9, 9.3, paint);
        }
        k.box(4.9, 11.8, 6.7, 9.0, 13.2, 7.3, paint);
        k.box(4.9, 11.8, 8.7, 9.0, 13.2, 9.3, paint);
        k.box(4.9, 11.8, 7.3, 9.0, 12.2, 8.7, dark);
        k.g('spout');
        k.round('y', 11.0, 12.7, 8, 0.95, 1.8, dark, {sides: 16});
        k.box(9.8, 13.6, 7.55, 11.8, 14.2, 8.45, paint);
        k.box(11.6, 12.6, 7.5, 12.0, 14.2, 8.5, C.CHROME);
    });
}
jerryCan('jerry_can', C.RED, '#8E2A22');
jerryCan('jerry_can_diesel', '#E0B21F', '#9C7A14');
jerryCan('jerry_can_bio', '#3F8E3A', '#2A6427');

// ---- road paint pails ---------------------------------------------------------------------

for (const [name, paint] of [['white_road_paint', '#F2F2EE'], ['yellow_road_paint', '#F2C12E'],
    ['red_road_paint', '#C0392B'], ['blue_road_paint', '#2E6FBF']]) {
    k.model(name, KEEP, () => {
        // a steel pail with rolled rims, its label band in the paint's colour, the lid pressed on
        k.g('pail');
        k.round('y', 8, 4.5, 8, 4.9, 9.0, C.STEEL, {sides: 16});
        k.round('y', 8, 0.2, 8, 5.05, 0.4, C.ALU, {sides: 16});
        k.round('y', 8, 8.8, 8, 5.05, 0.4, C.ALU, {sides: 16});
        k.round('y', 8, 4.4, 8, 5.0, 4.0, paint, {sides: 16});
        k.round('y', 8, 4.4, 8, 5.02, 0.4, C.BLACK, {sides: 16});
        k.round('y', 8, 9.15, 8, 4.6, 0.3, C.ALU, {sides: 16});
        // the paint run down from under the lid
        k.g('drips');
        k.box(6.6, 6.6, 3.0, 7.4, 9.1, 3.1, paint);
        k.box(6.7, 5.8, 3.0, 7.2, 6.6, 3.1, paint);
        k.box(12.9, 7.4, 8.8, 13.0, 9.1, 9.6, paint);
        k.box(8.6, 9.3, 3.9, 10.2, 9.35, 4.8, paint);
        // the wire bail up over the lid from its ears, a black grip on it
        k.g('bail');
        k.box(2.6, 6.0, 7.4, 3.2, 7.0, 8.6, C.DARK_STEEL);
        k.box(12.8, 6.0, 7.4, 13.4, 7.0, 8.6, C.DARK_STEEL);
        k.box(2.7, 6.5, 7.8, 3.0, 11.6, 8.2, C.CHROME);
        k.box(13.0, 6.5, 7.8, 13.3, 11.6, 8.2, C.CHROME);
        k.box(2.7, 11.4, 7.8, 13.3, 11.7, 8.2, C.CHROME);
        k.round('x', 8, 11.55, 8, 0.45, 3.6, C.BLACK);
    });
}

// ---- tar -----------------------------------------------------------------------------

k.model('tar', KEEP, () => {
    // a slumped heap of bitumen, glossy where the light catches it, run out in pools at its foot
    k.g('heap');
    const TAR = '#232427', SHINE = '#686C73';
    k.round('y', 8, 1.4, 8, 5.2, 2.8, TAR, {sides: 16});
    k.round('y', 7.8, 3.8, 8.1, 4.0, 2.0, TAR, {sides: 16});
    k.round('y', 8.2, 5.4, 7.8, 2.6, 1.4, TAR, {sides: 16});
    k.round('y', 8.0, 6.5, 8.0, 1.3, 0.9, TAR, {sides: 16});
    k.g('pools');
    k.box(11.5, 0, 6.0, 14.0, 0.6, 9.0, TAR);
    k.box(2.0, 0, 8.0, 4.0, 0.5, 11.0, TAR);
    k.box(5.0, 0, 12.4, 8.0, 0.4, 13.4, TAR);
    k.g('shine');
    k.box(6.6, 6.95, 7.4, 7.4, 7.0, 8.0, SHINE);
    k.box(5.4, 4.8, 6.0, 6.6, 4.85, 6.6, SHINE);
    k.box(9.8, 2.8, 4.6, 11.0, 2.85, 5.2, SHINE);
    k.box(12.2, 0.6, 7.0, 13.2, 0.65, 7.6, SHINE);
});
