// The fuel refinery, four blocks by four on one concrete pad and six high at
// the column, facing north. Across the front: the fired heater on the west,
// its burners showing through the peepholes and a short stack on its
// convection section; the control room on the east, its door lamp lit while
// the plant runs. Behind the control room the crude tank on its two charge
// pumps; behind the heater the distillation column with two railed platforms,
// a caged ladder and a davit on top, its overhead line down to the air cooler
// and the reflux drum under it; the stripper in the back west corner with its
// own platform; the reboiler and the pipe rack behind the column; the boiler
// that raises the steam, and its brick chimney in the far corner with a
// gallery and aviation lamps. Every box lands in one of the sixty blocks the
// refinery has always had, so placed refineries keep their parts.
const PAD = C.CONCRETE, KERB = '#A4A6A9', FOOT = '#7D7F82';
const RAIL = C.YELLOW, FRAME = C.DARK_STEEL, GRATE = C.DARK_STEEL, SKIN = C.ALU, PIPE = C.COPPER;
const WALL = '#D2CCBA', GLASS = '#6F94A8', ROOF = '#BFC3C7', SOOT = '#2B2522', CASING = C.IRON;
const BRICK = 'minecraft:block/bricks';
const S = 16;

// the blocks, in the order their parts are numbered
const CELLS = [];
for (let y = 0; y < 2; y++) {
    for (let z = 0; z < 4; z++) {
        for (let x = 0; x < 4; x++) {
            CELLS.push([x, y, z]);
        }
    }
}
CELLS.push([1, 2, 2], [2, 2, 2], [3, 2, 2], [0, 2, 3], [3, 2, 3],
    [0, 3, 1], [1, 3, 1], [2, 3, 1], [0, 3, 2], [1, 3, 2], [2, 3, 2], [0, 3, 3], [1, 3, 3], [2, 3, 3], [3, 3, 3],
    [0, 4, 1], [1, 4, 1], [2, 4, 1], [0, 4, 2], [1, 4, 2], [2, 4, 2], [3, 4, 2], [0, 4, 3], [1, 4, 3], [2, 4, 3], [3, 4, 3],
    [1, 5, 2], [3, 5, 3]);
// the parts that change while it runs: the heater's peepholes, the door
// lamp, the boiler's sight glass, and the aviation lamps on the column and the chimney
const LIT = [0, 1, 3, 14, 58, 59];

/** [a, b] cut where the blocks meet. */
function cuts(a, b) {
    const out = [];
    let s = a;
    for (let m = (Math.floor(a / S + 1e-6) + 1) * S; m < b - 1e-6; m += S) {
        out.push([s, m]);
        s = m;
    }
    out.push([s, b]);
    return out;
}

/** A round thing along an axis from a to b, cut where the blocks meet; p, q are the other two coordinates in xyz order. */
function rod(axis, a, b, p, q, r, paint, o) {
    for (const [s, e] of cuts(Math.min(a, b), Math.max(a, b))) {
        const m = (s + e) / 2;
        if (axis === 'x') {
            k.round('x', m, p, q, r, e - s, paint, o);
        } else if (axis === 'y') {
            k.round('y', p, m, q, r, e - s, paint, o);
        } else {
            k.round('z', p, q, m, r, e - s, paint, o);
        }
    }
}

/** A fitting where a line turns. */
function fit(p, r, paint) {
    const f = r + 0.18;
    k.box(p[0] - f, p[1] - f, p[2] - f, p[0] + f, p[1] + f, p[2] + f, paint || C.STEEL);
}

/** A line through square-cornered points, a fitting at every turn. */
function line(points, r, paint, fitting) {
    for (let i = 0; i + 1 < points.length; i++) {
        const p = points[i], q = points[i + 1];
        const a = [0, 1, 2].find(j => Math.abs(p[j] - q[j]) > 1e-6);
        const [u, v] = [0, 1, 2].filter(j => j !== a);
        rod('xyz'[a], p[a], q[a], p[u], p[v], r, paint);
        if (i > 0) {
            fit(p, r, fitting);
        }
    }
}

/** A gate valve on a line along an axis, its wheel up (or north, on an upright line). */
function valve(axis, x, y, z, r) {
    const b = r + 0.35, c = [x, y, z], a = 'xyz'.indexOf(axis);
    const lo = [x - b, y - b, z - b], hi = [x + b, y + b, z + b];
    lo[a] = c[a] - 0.7;
    hi[a] = c[a] + 0.7;
    k.box(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2], C.RED_DARK);
    if (axis === 'y') {
        k.box(x - 0.2, y - 0.2, z - b - 1, x + 0.2, y + 0.2, z - b, C.STEEL);
        k.round('z', x, y, z - b - 1.1, 0.9, 0.2, C.RED);
    } else {
        k.box(x - 0.2, y + b, z - 0.2, x + 0.2, y + b + 1, z + 0.2, C.STEEL);
        k.round('y', x, y + b + 1.1, z, 0.9, 0.2, C.RED);
    }
}

/** A straight handrail along x or z standing on y: posts, a knee rail, a top rail and a toe board. */
function rail(x1, z1, x2, z2, y, ends) {
    const alongX = Math.abs(z1 - z2) < 1e-6, w = 0.15;
    const bar = (y1, y2) => alongX ? k.box(x1, y1, z1 - w, x2, y2, z1 + w, RAIL) : k.box(x1 - w, y1, z1, x1 + w, y2, z2, RAIL);
    bar(y + 3.3, y + 3.6);
    bar(y + 1.75, y + 2);
    bar(y, y + 0.45);
    const len = alongX ? x2 - x1 : z2 - z1, n = Math.max(1, Math.round(len / 5));
    for (let i = ends === false ? 1 : 0; i <= (ends === false ? n - 1 : n); i++) {
        const t = (alongX ? x1 : z1) + len * i / n;
        if (alongX) {
            k.box(t - 0.2, y, z1 - 0.2, t + 0.2, y + 3.3, z1 + 0.2, RAIL);
        } else {
            k.box(x1 - 0.2, y, t - 0.2, x1 + 0.2, y + 3.3, t + 0.2, RAIL);
        }
    }
}

/** Handrails round the four edges of a deck. */
function rails(x1, z1, x2, z2, y) {
    const i = 0.25;
    rail(x1 + i, z1 + i, x2 - i, z1 + i, y);
    rail(x1 + i, z2 - i, x2 - i, z2 - i, y);
    rail(x1 + i, z1 + i, x1 + i, z2 - i, y, false);
    rail(x2 - i, z1 + i, x2 - i, z2 - i, y, false);
}

/** A ladder standing at x, its stiles at z1 and z2. */
function ladder(x, z1, z2, y1, y2) {
    k.box(x - 0.2, y1, z1 - 0.4, x + 0.2, y2, z1, RAIL);
    k.box(x - 0.2, y1, z2, x + 0.2, y2, z2 + 0.4, RAIL);
    for (let y = y1 + 1.4; y < y2 - 0.6; y += 2.4) {
        k.box(x - 0.15, y, z1, x + 0.15, y + 0.3, z2, C.STEEL);
    }
}

/** The cage round a ladder at x, reaching out to x = out. */
function cage(x, out, z1, z2, y1, y2) {
    const lo = Math.min(x, out), hi = Math.max(x, out), back = out > x ? [out - 0.3, out] : [out, out + 0.3];
    for (let y = y1; y <= y2 + 1e-6; y += 4.5) {
        k.box(lo, y, z1 - 1, hi, y + 0.3, z1 - 0.7, RAIL);
        k.box(lo, y, z2 + 0.7, hi, y + 0.3, z2 + 1, RAIL);
        k.box(back[0], y, z1 - 1, back[1], y + 0.3, z2 + 1, RAIL);
    }
    for (const z of [z1 - 0.85, (z1 + z2) / 2, z2 + 0.85]) {
        k.box(back[0], y1, z - 0.12, back[1], y2 + 0.3, z + 0.12, RAIL);
    }
}

/** A charge or reflux pump along z: motor at z1, volute at the far end. */
function pump(x, y, z1, z2) {
    k.box(x - 2, 1.5, z1 - 0.2, x + 2, 2.2, z2 + 0.2, KERB);
    k.box(x - 1.8, 2.2, z1, x + 1.8, 2.6, z2, FRAME);
    const m = z1 + 1.4, len = z2 - z1;
    k.round('z', x, y, z1 + 0.2, 1.1, 0.4, C.STEEL);
    k.round('z', x, y, m, 1.4, 2.4, C.BLUE, {sides: 16});
    k.box(x - 1.3, 2.6, z1 + 0.4, x + 1.3, y - 1, m + 1, FRAME);
    k.box(x + 1.2, y - 0.2, m - 0.5, x + 1.9, y + 1, m + 0.5, C.BLUE);
    k.box(x - 0.9, y - 1.1, m + 1.2, x + 0.9, y + 0.9, m + 2, C.YELLOW);
    k.round('z', x, y, m + 2.6, 0.75, 1.2, C.BLUE_DARK);
    k.round('z', x, y, z2 - 0.65, 1.6, 1.3, C.BLUE_DARK, {sides: 16});
    k.box(x - 0.8, 2.6, z2 - 1.2, x + 0.8, y - 1, z2 - 0.1, C.BLUE_DARK);
    return len;
}

function plant(lit) {
    // ---- the pad ----------------------------------------------------------------------
    k.g('pad');
    k.box(0, 0, 0, 64, 1.5, 64, PAD);
    k.box(0, 1.5, 0, 64, 2, 0.6, KERB);
    k.box(0, 1.5, 63.4, 64, 2, 64, KERB);
    k.box(0, 1.5, 0.6, 0.6, 2, 63.4, KERB);
    k.box(63.4, 1.5, 0.6, 64, 2, 63.4, KERB);

    heater(lit);
    controlRoom(lit);
    tank();
    column(lit);
    cooler();
    stripper();
    rackAndReboiler();
    boiler(lit);
    chimney(lit);
}

// ---- the fired heater, front west --------------------------------------------------------
function heater(lit) {
    const STAYS = [2, 7.6, 13.2, 18.8, 24.4, 30], BAYS = [4.8, 10.4, 16, 21.6, 27.2];
    k.g('heater/legs');
    for (const x of STAYS) {
        k.box(x - 0.4, 1.5, 2, x + 0.4, 4.4, 2.8, FRAME);
        k.box(x - 0.4, 1.5, 20.2, x + 0.4, 4.4, 21, FRAME);
    }
    k.box(1.6, 4.4, 1.6, 30.4, 5, 21.4, FRAME);
    // the burners under the floor, on their fuel gas header
    k.g('heater/burners');
    rod('x', 2.6, 29.4, 2.6, 7, 0.3, C.YELLOW);
    for (const x of BAYS) {
        k.box(x - 0.15, 2.45, 7, x + 0.15, 2.75, 10.6, C.YELLOW);
        k.round('y', x, 3.4, 11.5, 1, 2, C.DARK_STEEL);
    }
    // the radiant box, its buckstays and the peepholes along the front
    k.g('heater/box');
    k.box(2, 5, 2, 30, 17, 21, CASING);
    for (const x of STAYS) {
        k.box(x - 0.4, 4.4, 1.4, x + 0.4, 17.4, 2, FRAME);
        k.box(x - 0.4, 4.4, 21, x + 0.4, 17.4, 21.6, FRAME);
    }
    for (const z of [8.3, 14.7]) {
        k.box(1.4, 4.4, z - 0.4, 2, 17.4, z + 0.4, FRAME);
    }
    k.box(1.4, 16.6, 1.4, 30.6, 17.6, 21.6, FRAME);
    k.g('heater/peepholes');
    for (const x of BAYS) {
        for (const y of [8.4, 12.8]) {
            k.box(x - 0.9, y - 0.9, 1.75, x + 0.9, y + 0.9, 2, C.STEEL);
            k.box(x - 0.45, y - 0.45, 1.7, x + 0.45, y + 0.45, 1.75, lit ? C.LED_AMBER : C.BLACK,
                {only: ['north'], shade: !lit});
        }
    }
    // the return bends' box on the east end
    k.box(30, 6, 4, 32, 16, 19, C.STEEL);
    // the convection section, its hood and stack
    k.g('heater/convection');
    k.box(7, 17.6, 5, 25, 22.6, 18, CASING);
    for (const x of [7.4, 13.2, 18.8, 24.6]) {
        k.box(x - 0.35, 17.6, 4.4, x + 0.35, 22.8, 5, FRAME);
        k.box(x - 0.35, 17.6, 18, x + 0.35, 22.8, 18.6, FRAME);
    }
    k.box(5.4, 18, 6, 7, 22.2, 17, C.STEEL);
    k.box(25, 18, 6, 26.6, 22.2, 17, C.STEEL);
    k.box(6.6, 22.4, 4.6, 25.4, 23, 18.4, FRAME);
    k.box(10, 23, 7.5, 22, 24, 16, C.STEEL);
    k.box(12.6, 24, 9, 19.4, 25, 14.5, C.STEEL);
    k.g('heater/stack');
    rod('y', 25, 30.8, 16, 11.75, 2.2, FRAME, {sides: 16});
    k.round('y', 16, 27, 11.75, 2.45, 0.5, C.STEEL, {sides: 16});
    k.round('y', 16, 31.1, 11.75, 2.45, 0.6, C.STEEL, {sides: 16, caps: C.BLACK});
    k.box(18.3, 26.4, 11.3, 19.1, 27.8, 12.2, C.RED);
    // the roof's handrails and the ladder up the west end
    k.g('heater/rails');
    rail(1.7, 1.7, 30.3, 1.7, 17.6);
    rail(1.7, 21.3, 30.3, 21.3, 17.6);
    rail(1.7, 1.7, 1.7, 9, 17.6, false);
    rail(1.7, 13.2, 1.7, 21.3, 17.6, false);
    k.box(1.5, 17.6, 8.8, 1.9, 20.9, 9.2, RAIL);
    k.box(1.5, 17.6, 13, 1.9, 20.9, 13.4, RAIL);
    rail(30.3, 1.7, 30.3, 21.3, 17.6, false);
    ladder(1, 9.6, 12.6, 1.5, 21);

    // the transfer line, lagged, from the back of the heater into the column
    k.g('heater/transfer');
    line([[21.6, 11, 21], [21.6, 11, 29.5], [21.6, 15.5, 29.5], [21.6, 15.5, 35]], 0.9, SKIN, C.ALU);
    k.round('z', 21.6, 15.5, 34.2, 1.3, 0.4, C.STEEL);
}

// ---- the control room, front east --------------------------------------------------------
function controlRoom(lit) {
    k.g('control/walls');
    k.box(34.3, 1.5, 1.3, 62.7, 2.6, 16.7, FOOT);
    k.box(34.5, 2.6, 1.5, 62.5, 15, 16.5, WALL);
    k.box(34.2, 15, 1.2, 62.8, 15.6, 16.8, C.STEEL);
    k.box(34.2, 15.6, 1.2, 62.8, 16.4, 1.7, C.STEEL);
    k.box(34.2, 15.6, 16.3, 62.8, 16.4, 16.8, C.STEEL);
    k.box(34.2, 15.6, 1.7, 34.7, 16.4, 16.3, C.STEEL);
    k.box(62.3, 15.6, 1.7, 62.8, 16.4, 16.3, C.STEEL);
    k.box(34.7, 15.6, 1.7, 62.3, 15.62, 16.3, C.DARK_STEEL, {only: ['up']});
    // the windows, each one pane
    k.g('control/windows');
    for (const [x1, x2] of [[36.8, 47.7], [57.2, 61.2]]) {
        k.box(x1 - 0.4, 6.6, 1.35, x2 + 0.4, 12.4, 1.5, C.DARK_STEEL, {only: ['north']});
        k.box(x1, 7, 1.3, x2, 12, 1.35, GLASS, {only: ['north']});
        k.box(x1 - 0.6, 6.2, 0.9, x2 + 0.6, 6.6, 1.5, C.ALU);
    }
    k.box(34.35, 6.6, 4.4, 34.5, 12.4, 13.6, C.DARK_STEEL, {only: ['west']});
    k.box(34.3, 7, 4.8, 34.35, 12, 13.2, GLASS, {only: ['west']});
    k.box(62.5, 6.6, 4.4, 62.65, 12.4, 13.6, C.DARK_STEEL, {only: ['east']});
    k.box(62.65, 7, 4.8, 62.7, 12, 13.2, GLASS, {only: ['east']});
    k.box(38.4, 13, 1.32, 46.4, 14.4, 1.5, C.BLUE_DARK);
    // the door, its steps, canopy and lamp
    k.g('control/door');
    k.box(50.6, 2.6, 1.4, 55.4, 11.2, 1.5, C.DARK_STEEL, {only: ['north']});
    k.box(51, 2.6, 1.3, 55, 10.8, 1.4, C.STEEL);
    k.box(52.2, 7.4, 1.25, 53.8, 9.8, 1.3, GLASS, {only: ['north']});
    k.box(51, 2.6, 1.26, 55, 3.6, 1.3, C.ALU, {only: ['north']});
    k.box(54.1, 5.8, 1, 54.6, 6.2, 1.3, C.CHROME);
    k.box(50.4, 1.5, 0.3, 55.6, 2, 1.3, KERB);
    k.box(50.4, 2, 0.8, 55.6, 2.6, 1.3, KERB);
    k.box(50.2, 11.6, 0.2, 55.8, 12.1, 1.5, C.STEEL);
    k.box(52.5, 11, 0.7, 53.5, 11.6, 1.2, lit ? C.LED_GREEN : C.LED_OFF, {shade: !lit});
    // on the roof: two air conditioners, a hatch and the radio mast
    k.g('control/roof');
    for (const x of [38, 46.5]) {
        k.box(x, 15.6, 6.5, x + 6.5, 18.6, 12.5, C.ALU);
        k.round('y', x + 3.25, 18.65, 9.5, 2.2, 0.1, C.DARK_STEEL, {sides: 16, caps: C.BLACK});
        k.box(x + 1.1, 18.7, 9.35, x + 5.4, 18.76, 9.65, C.STEEL);
        k.box(x + 3.1, 18.7, 7.35, x + 3.4, 18.76, 11.65, C.STEEL);
    }
    k.box(54, 15.6, 3.5, 56.5, 16.3, 6, C.STEEL);
    k.box(58.2, 15.6, 12.2, 59.8, 16.2, 13.8, FRAME);
    k.box(58.8, 16.2, 12.8, 59.2, 28, 13.2, C.STEEL);
    k.box(57, 26.4, 12.85, 61, 26.7, 13.15, C.STEEL);
    k.box(58.6, 28, 12.6, 59.4, 28.6, 13.4, C.RED);
}

// ---- the crude tank and its charge pumps --------------------------------------------------
function tank() {
    const X = 48, Z = 37, R = 10;
    k.g('tank/shell');
    k.round('y', X, 1.9, Z, R + 0.8, 0.8, KERB, {sides: 16});
    rod('y', 2.3, 29.6, X, Z, R, C.WHITE, {sides: 16});
    for (const y of [9.2, 15.4, 22.6]) {
        k.round('y', X, y, Z, R + 0.1, 0.25, ROOF, {sides: 16});
    }
    k.round('y', X, 28.8, Z, R + 0.6, 0.5, C.STEEL, {sides: 16});
    // the roof, a shallow cone, and its breather vent
    k.g('tank/roof');
    [[9.9, 29.6, 30.2], [8.2, 30.2, 30.8], [6.5, 30.8, 31.4], [5, 31.4, 32], [3.6, 32, 32.5], [2.2, 32.5, 33]]
        .forEach(([r, y1, y2]) => k.round('y', X, (y1 + y2) / 2, Z, r, y2 - y1, ROOF, {sides: 16}));
    k.round('y', X, 34.2, Z, 0.7, 2.4, C.STEEL);
    k.box(X - 1.3, 35.4, Z - 1.3, X + 1.3, 36, Z + 1.3, C.STEEL);
    // the ladder up the east side and the landing at its head
    k.g('tank/ladder');
    ladder(58.8, 35.6, 38.4, 2.3, 34.8);
    cage(58.8, 61.4, 35.6, 38.4, 9, 27);
    k.box(54.4, 30.8, 34.6, 58.4, 31.2, 39.4, GRATE);
    rail(54.6, 34.8, 58.2, 34.8, 31.2);
    rail(54.6, 39.2, 58.2, 39.2, 31.2);
    rail(54.6, 34.8, 54.6, 39.2, 31.2, false);
    // a manway, the level gauge and the fill line from the east
    k.g('tank/fittings');
    k.round('x', 37.9, 5.2, Z, 1.6, 0.6, C.STEEL, {sides: 16});
    k.round('x', 37.5, 5.2, Z, 1.2, 0.2, C.STEEL, {sides: 16});
    rod('y', 3.4, 28.4, 46.6, 26.9, 0.3, C.STEEL);
    k.box(45.8, 3.6, 26.2, 47.4, 5.6, 27.2, C.STEEL);
    k.box(46.1, 4.1, 26.15, 47.1, 5.1, 26.2, C.DIAL, {only: ['north']});
    rod('x', 56.4, 63.4, 6, 42, 0.7, PIPE);
    k.round('x', 63.1, 6, 42, 1.1, 0.5, C.STEEL);
    valve('x', 60.6, 6, 42, 0.7);
    k.box(61.6, 1.5, 41.5, 62.4, 5.3, 42.5, FRAME);

    // two charge pumps between the tank and the control room, and the line to the heater
    k.g('tank/pumps');
    for (const x of [43, 53]) {
        pump(x, 4.8, 17.4, 23.2);
        rod('z', 23.2, 28.8, x, 4.8, 0.7, PIPE);
        valve('z', x, 4.8, 25.6, 0.7);
        line([[x, 6.4, 22.55], [x, 10, 22.55]], 0.6, PIPE);
        valve('y', x, 8.1, 22.55, 0.6);
        fit([x, 10, 22.55], 0.6);
    }
    k.g('tank/charge');
    line([[53, 10, 22.55], [33.5, 10, 22.55], [33.5, 23.4, 22.55], [33.5, 23.4, 11.5], [25.8, 23.4, 11.5], [25.8, 22.2, 11.5]],
        0.6, PIPE);
}

// ---- the distillation column ---------------------------------------------------------------
const CX = 24, CZ = 40;

function columnPlatform(y) {
    k.g('column/platform');
    for (const [x1, z1, x2, z2] of [[14, 30, 31, 50], [31, 30, 35, 37.4], [31, 42.6, 35, 50]]) {
        k.box(x1, y - 0.6, z1, x2, y, z2, GRATE);
    }
    k.box(14, y - 1.4, 30, 35, y - 0.6, 30.5, FRAME);
    k.box(14, y - 1.4, 49.5, 35, y - 0.6, 50, FRAME);
    k.box(14, y - 1.4, 30.5, 14.5, y - 0.6, 49.5, FRAME);
    k.box(34.5, y - 1.4, 30.5, 35, y - 0.6, 49.5, FRAME);
    k.box(14.5, y - 1.4, 33.2, 34.5, y - 0.6, 33.7, FRAME);
    k.box(14.5, y - 1.4, 46.3, 34.5, y - 0.6, 46.8, FRAME);
    k.box(CX - 0.3, y - 4.7, 32.25, CX + 0.3, y + 0.1, 32.55, FRAME, {rot: ['x', -45, [CX, y - 2.3, 32.4]]});
    k.box(CX - 0.3, y - 4.7, 47.45, CX + 0.3, y + 0.1, 47.75, FRAME, {rot: ['x', 45, [CX, y - 2.3, 47.6]]});
    rails(14, 30, 35, 50, y);
}

function column(lit) {
    k.g('column/skirt');
    k.round('y', CX, 2.05, CZ, 7.4, 1.1, KERB, {sides: 16});
    rod('y', 2.6, 10.4, CX, CZ, 6.1, C.STEEL, {sides: 16});
    k.round('y', CX, 2.9, CZ, 6.5, 0.6, FRAME, {sides: 16});
    k.box(22.2, 2.9, 33.95, 25.8, 8, 34.05, FRAME, {only: ['north']});
    k.box(22.6, 3.3, 33.9, 25.4, 7.6, 33.95, C.BLACK, {only: ['north']});
    k.g('column/shell');
    rod('y', 10.4, 88, CX, CZ, 6, SKIN, {sides: 16});
    for (const y of [21, 34, 45, 60, 68, 84]) {
        k.round('y', CX, y, CZ, 6.12, 0.3, C.STEEL, {sides: 16});
    }
    // the head, the vapour nozzle and a relief valve
    k.g('column/head');
    k.round('y', CX, 88.6, CZ, 5.4, 1.2, SKIN, {sides: 16});
    k.round('y', CX, 89.7, CZ, 4.3, 1, SKIN, {sides: 16});
    k.round('y', CX, 90.55, CZ, 2.8, 0.7, SKIN, {sides: 16});
    k.round('y', CX, 91.6, CZ, 1, 1.4, C.STEEL);
    k.box(27, 89.2, 36.8, 28.2, 90.8, 38, C.RED);
    k.box(27.3, 90.8, 37.1, 27.9, 92.2, 37.7, C.RED_DARK);
    // the davit on top, and the aviation lamp on it
    k.g('column/davit');
    k.round('y', 29.4, 90.6, 43.4, 0.35, 9.2, C.STEEL);
    k.box(28.4, 86.4, 43, 29.4, 86.9, 43.8, FRAME);
    k.box(25.2, 94.8, 43.15, 29.75, 95.4, 43.65, C.STEEL);
    k.box(25.5, 92.6, 43.3, 25.7, 94.8, 43.5, FRAME);
    k.box(25.3, 92.2, 43.2, 25.9, 92.6, 43.6, C.YELLOW);
    k.box(29, 95.4, 43, 29.8, 96, 43.8, lit ? C.LED_RED : C.RED_DARK, {shade: !lit});
    // the manways up its front
    k.g('column/manways');
    for (const y of [26, 40, 63]) {
        k.round('z', CX, y, 33.75, 1.6, 0.6, C.STEEL);
        k.round('z', CX, y, 33.3, 1.25, 0.3, SKIN);
        k.box(25.4, y - 0.3, 33, 26.2, y + 0.3, 33.8, FRAME);
    }
    // the caged ladder up the east side, through both platforms
    k.g('column/ladder');
    ladder(31.4, 38.6, 41.4, 3.2, 79.6);
    cage(31.4, 34.5, 38.6, 41.4, 9, 78);
    for (const y of [8, 24, 40, 56, 68]) {
        k.box(29.8, y, 39.7, 31.2, y + 0.4, 40.3, FRAME);
    }
    columnPlatform(52);
    columnPlatform(76);

    // the overhead line, off the top and down the west side to the cooler
    k.g('column/overhead');
    line([[CX, 92.3, CZ], [CX, 93.2, CZ], [17.1, 93.2, CZ], [17.1, 23.6, CZ], [15.9, 23.6, CZ]], 0.75, PIPE);
    // the relief header, east along under the top platform to the chimney
    k.g('column/relief');
    k.round('x', 28.8, 72, 44, 0.6, 0.8, C.STEEL);
    k.box(29, 71.2, 43.2, 30.6, 73.4, 44.8, C.RED);
    k.box(29.5, 73.4, 43.7, 30.1, 74.8, 44.3, C.RED_DARK);
    line([[30.6, 72, 44], [58.5, 72, 44], [58.5, 72, 51.6]], 0.6, PIPE);
}

// ---- the air cooler and the reflux drum under it, west of the column ----------------------
function cooler() {
    k.g('cooler/frame');
    for (const x of [1, 14.6]) {
        for (const z of [32.8, 46.4]) {
            k.box(x, 1.5, z, x + 1, 21.4, z + 1, FRAME);
        }
        k.box(x, 21, 33.8, x + 1, 21.4, 46.4, FRAME);
    }
    k.box(2, 21, 32.8, 14.6, 21.4, 33.8, FRAME);
    k.box(2, 21, 46.4, 14.6, 21.4, 47.4, FRAME);
    k.g('cooler/bundle');
    k.box(1.4, 21.4, 33.4, 15.2, 24.4, 46.8, C.ALU);
    for (const y of [22.2, 23.2]) {
        k.box(1.8, y, 33.36, 14.8, y + 0.3, 33.4, C.STEEL, {only: ['north']});
        k.box(1.8, y, 46.8, 14.8, y + 0.3, 46.84, C.STEEL, {only: ['south']});
    }
    k.box(0.6, 21.4, 33.6, 1.8, 24.8, 46.6, C.STEEL);
    k.box(14.8, 21.4, 33.6, 16, 24.8, 46.6, C.STEEL);
    k.box(2, 24.4, 34, 14.6, 25.4, 46.2, C.STEEL);
    // two fans on top
    k.g('cooler/fans');
    for (const z of [37, 43.2]) {
        k.round('y', 8.3, 26.4, z, 3, 2, C.STEEL, {sides: 16, caps: C.BLACK});
        k.box(5.6, 27.4, z - 0.2, 11, 27.55, z + 0.2, C.STEEL);
        k.box(8.1, 27.4, z - 2.7, 8.5, 27.55, z + 2.7, C.STEEL);
        k.round('y', 8.3, 27.7, z, 0.6, 0.3, C.DARK_STEEL);
    }
    // the reflux drum on its saddles, the condensate down into it
    k.g('cooler/drum');
    for (const z of [36, 43]) {
        k.box(5.8, 1.5, z, 10.2, 6.8, z + 1, FRAME);
    }
    rod('z', 34.6, 45.4, 8, 9, 2.6, CASING, {sides: 16});
    k.round('z', 8, 9, 34.3, 2.1, 0.6, CASING, {sides: 16});
    k.round('z', 8, 9, 45.7, 2.1, 0.6, CASING, {sides: 16});
    k.box(7.4, 11.4, 38.4, 8.6, 13, 39.6, C.RED);
    line([[1.2, 21.4, 44], [1.2, 9, 44], [5.6, 9, 44]], 0.45, PIPE);
    // the reflux pump, and the reflux line back up to the column's top tray
    k.g('cooler/reflux');
    pump(13, 4, 35.8, 42);
    line([[8, 6.4, 45.2], [8, 4, 45.2], [13, 4, 45.2], [13, 4, 42]], 0.45, PIPE);
    line([[13, 5.5, 41.2], [13, 12.4, 41.2], [13, 12.4, 36.6], [17.6, 12.4, 36.6], [17.6, 84, 36.6], [19.3, 84, 36.6]],
        0.45, PIPE);
    valve('y', 17.6, 20, 36.6, 0.45);
}

// ---- the stripper, back west, with its own platform ----------------------------------------
function stripper() {
    const X = 8, Z = 56, Y = 62;
    k.g('stripper/shell');
    k.round('y', X, 2.05, Z, 5, 1.1, KERB, {sides: 16});
    rod('y', 2.6, 8, X, Z, 4.1, C.STEEL, {sides: 16});
    rod('y', 8, 66, X, Z, 4, SKIN, {sides: 16});
    for (const y of [22, 40, 56]) {
        k.round('y', X, y, Z, 4.1, 0.3, C.STEEL, {sides: 16});
    }
    k.round('y', X, 66.4, Z, 3.5, 0.8, SKIN, {sides: 16});
    k.round('y', X, 67.15, Z, 2.6, 0.7, SKIN, {sides: 16});
    k.round('y', X, 67.75, Z, 1.5, 0.5, SKIN, {sides: 16});
    // its platform near the top
    k.g('stripper/platform');
    for (const [x1, z1, x2, z2] of [[3.6, 49, 14, 63], [1, 49, 3.6, 53.4], [1, 58.6, 3.6, 63]]) {
        k.box(x1, Y - 0.6, z1, x2, Y, z2, GRATE);
    }
    k.box(1, Y - 1.4, 49, 14, Y - 0.6, 49.5, FRAME);
    k.box(1, Y - 1.4, 62.5, 14, Y - 0.6, 63, FRAME);
    k.box(13.5, Y - 1.4, 49.5, 14, Y - 0.6, 62.5, FRAME);
    k.box(X - 0.3, Y - 4, 49.55, X + 0.3, Y - 0.6, 51.25, FRAME);
    k.box(X - 0.3, Y - 4.7, 50.25, X + 0.3, Y + 0.1, 50.55, FRAME, {rot: ['x', -45, [X, Y - 2.3, 50.4]]});
    k.box(X - 0.3, Y - 4.7, 61.25, X + 0.3, Y + 0.1, 61.55, FRAME, {rot: ['x', 45, [X, Y - 2.3, 61.4]]});
    rails(1, 49, 14, 63, Y);
    // the caged ladder up its west side
    k.g('stripper/ladder');
    ladder(2.8, 54.6, 57.4, 2.6, Y + 3.6);
    cage(2.8, 0.6, 54.6, 57.4, 9, 58.5);
    // the vapour back to the column, and the side draw from it
    k.g('stripper/lines');
    k.round('y', X, 68.6, Z, 0.7, 1.2, C.STEEL);
    line([[X, 69.2, Z], [X, 70.2, Z], [20, 70.2, Z], [20, 70.2, 44.2]], 0.6, PIPE);
    line([[21, 58, 45.1], [21, 58, Z], [11.9, 58, Z]], 0.5, PIPE);
    valve('x', 16.5, 58, Z, 0.5);
}

// ---- the reboiler and the pipe rack behind the column --------------------------------------
function rackAndReboiler() {
    k.g('reboiler');
    for (const x of [20.4, 26.6]) {
        k.box(x, 1.5, 50.2, x + 1, 5.2, 54.6, FRAME);
    }
    rod('x', 18.5, 29.5, 7.4, 52.4, 2.8, CASING, {sides: 16});
    k.round('x', 29.8, 7.4, 52.4, 2.2, 0.6, CASING, {sides: 16});
    k.round('x', 18.2, 7.4, 52.4, 3.1, 0.6, C.STEEL, {sides: 16});
    k.round('x', 17.2, 7.4, 52.4, 2.6, 1.4, C.STEEL, {sides: 16});
    line([[27.6, 11.2, 45.1], [27.6, 11.2, 52.4], [27.6, 10.1, 52.4]], 0.5, PIPE);
    line([[20.4, 10.1, 52.4], [20.4, 13.4, 52.4], [20.4, 13.4, 44.6]], 0.6, PIPE);

    k.g('rack');
    for (const x of [19.1, 28.6]) {
        k.box(x - 0.9, 1.5, 58.1, x + 0.9, 1.8, 59.9, FRAME);
        k.box(x - 0.5, 1.8, 58.5, x + 0.5, 20.4, 59.5, FRAME);
        k.box(x - 0.5, 20.4, 55.4, x + 0.5, 21, 62.6, FRAME);
    }
    // the steam main from the boiler to the stripper, with its branch to the reboiler
    k.g('rack/steam');
    line([[41, 14.4, 56], [41, 21.6, 56], [41, 21.6, 61], [8, 21.6, 61], [8, 9.6, 61], [8, 9.6, 59.8]], 0.6, PIPE);
    fit([17.4, 21.6, 61], 0.6);
    line([[17.4, 21.6, 61], [17.4, 12, 61], [17.4, 12, 52.4], [17.4, 9.8, 52.4]], 0.45, PIPE);
    // the product line, out of the stripper's foot, over the rack and down to its loading valve
    k.g('rack/product');
    line([[11.4, 4, 57.8], [15, 4, 57.8], [15, 21.5, 57.8], [30.8, 21.5, 57.8], [30.8, 3.6, 57.8]], 0.5, PIPE);
    valve('y', 30.8, 7, 57.8, 0.5);
    k.round('y', 30.8, 3.3, 57.8, 1, 0.4, C.STEEL);
}

// ---- the boiler at the back, firing into the chimney ----------------------------------------
function boiler(lit) {
    const Y = 8.2, Z = 56;
    k.g('boiler');
    for (const x of [37.2, 44.2]) {
        k.box(x, 1.5, 52.4, x + 1.2, 5, 59.6, FRAME);
    }
    rod('x', 35.6, 47, Y, Z, 4.2, C.GREEN_DARK, {sides: 16});
    for (const x of [38.6, 44.8]) {
        k.round('x', x, Y, Z, 4.35, 0.4, C.DARK_STEEL, {sides: 16});
    }
    k.round('x', 35.3, Y, Z, 4.4, 0.6, C.DARK_STEEL, {sides: 16});
    // the burner on its front, the sight glass glowing while it fires
    k.g('boiler/burner');
    k.box(32.6, 6.2, 54.4, 35, 10, 57.6, C.RED);
    k.box(33, 10, 55, 34.6, 11.6, 57, C.RED_DARK);
    k.box(32.5, 8.1, 56.3, 32.6, 9.1, 57.3, lit ? C.LED_AMBER : C.BLACK, {only: ['west'], shade: !lit});
    k.round('x', 34.9, 11.2, 58.8, 0.6, 0.2, C.CHROME);
    k.box(34.78, 10.8, 58.4, 34.8, 11.6, 59.2, C.DIAL, {only: ['west']});
    // the steam dome and the safety valves
    k.g('boiler/top');
    k.round('y', 41, 13.2, Z, 1.4, 1.6, C.GREEN_DARK, {sides: 16});
    k.round('y', 41, 14.2, Z, 1.6, 0.4, C.STEEL, {sides: 16});
    for (const x of [38.6, 43.4]) {
        k.round('y', x, 13.2, Z, 0.45, 1.6, C.BRASS);
        k.box(x - 0.4, 14, Z - 0.4, x + 0.4, 14.6, Z + 0.4, C.BRASS);
    }
    // the smoke box at the back and the breeching into the chimney
    k.box(47, 3.6, 51.6, 49.6, 12.8, 60.4, C.DARK_STEEL);
    k.box(49.6, 6.4, 53.6, 50.2, 11, 58.4, C.DARK_STEEL);
}

// ---- the chimney, far east corner ----------------------------------------------------------
function chimney(lit) {
    k.g('chimney');
    k.box(48.6, 1.5, 48.6, 63.4, 3.4, 63.4, KERB);
    const SHAFT = [[49.5, 3.4, 24], [50, 24, 46], [50.75, 46, 68], [51.5, 68, 84]];
    for (const [a, y1, y2] of SHAFT) {
        k.box(a, y1, a, 112 - a, y2, 112 - a, BRICK);
    }
    k.box(51.5, 84, 51.5, 60.5, 88, 60.5, SOOT);
    k.box(51, 88, 51, 61, 88.8, 61, C.DARK_STEEL);
    k.box(52.6, 88.8, 52.6, 59.4, 88.85, 59.4, C.BLACK, {only: ['up']});
    for (const [a, y] of [[49.5, 14], [50, 35], [50.75, 57], [51.5, 83.7]]) {
        k.box(a - 0.15, y - 0.3, a - 0.15, 112 - a + 0.15, y + 0.3, 112 - a + 0.15, FRAME);
    }
    // the climbing irons up the north face
    k.g('chimney/irons');
    for (let y = 5; y < 87; y += 2.4) {
        const face = SHAFT.find(([, y1, y2]) => y >= y1 && y < y2);
        const z = face ? face[0] : 51.5;
        k.box(55.3, y, z - 0.7, 56.7, y + 0.3, z, C.DARK_STEEL);
    }
    // the gallery round it, on four brackets
    k.g('chimney/gallery');
    k.box(49, 75.4, 49, 63, 76, 63, GRATE);
    k.box(49, 74.6, 49, 63, 75.4, 49.5, FRAME);
    k.box(49, 74.6, 62.5, 63, 75.4, 63, FRAME);
    k.box(49, 74.6, 49.5, 49.5, 75.4, 62.5, FRAME);
    k.box(62.5, 74.6, 49.5, 63, 75.4, 62.5, FRAME);
    k.box(52.7, 71.7, 50.2, 53.3, 76.3, 50.5, FRAME, {rot: ['x', -45, [53, 74, 50.35]]});
    k.box(55.7, 71.7, 61.5, 56.3, 76.3, 61.8, FRAME, {rot: ['x', 45, [56, 74, 61.65]]});
    k.box(50.2, 71.7, 55.7, 50.5, 76.3, 56.3, FRAME, {rot: ['z', 45, [50.35, 74, 56]]});
    k.box(61.5, 71.7, 55.7, 61.8, 76.3, 56.3, FRAME, {rot: ['z', -45, [61.65, 74, 56]]});
    rails(49, 49, 63, 63, 76);
    // the aviation lamps on top
    k.g('chimney/lamps');
    for (const a of [51.1, 60.1]) {
        k.box(a, 88.8, a, a + 0.8, 89.6, a + 0.8, lit ? C.LED_RED : C.RED_DARK, {shade: !lit});
    }
}

const parts = {};
CELLS.forEach((at, i) => {
    parts[String(i)] = at;
});
k.model('fuel_refinery/p', {cells: parts}, () => plant(false));
for (const i of LIT) {
    k.model('fuel_refinery/p' + i + '_lit', {cells: {'': CELLS[i]}}, () => plant(true));
}
k.model('fuel_refinery', {file: '../item/fuel_refinery', shrink: 24}, () => plant(false));
k.model('fuel_refinery/whole', {preview: true}, () => plant(false));
