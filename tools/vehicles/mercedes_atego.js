// Mercedes-Benz Atego with a tank body: a short cab over the engine, the
// star in a black slatted grille, lamps down in the bumper, tall mirrors;
// behind it a round aluminium barrel on a subframe, hoops round it, a
// walkway and two manholes on top, and a length of sight glass in each
// side that shows what it carries.
const P = 'paint', PD = 'paint_dark';
const DEG = Math.PI / 180;
const H = 17.0, FLOOR = 11.0, BELT = 24.0, TOP = 43.0;
const FZ = 35.72, RZ = -29.72, WY = 7.5, AR = 8.0;
const REAR = 27.7, FRONT = 52.5;
const SHELL = '#C3C9CF', HOOP = '#8E959C', FRAME = C.TRIM, DARK = '#2A2C31';

const zW = y => 51.9 - (y - BELT) * 3.35 / 19;      // the windscreen, almost upright

// ---- the frame ---------------------------------------------------------------
k.g('frame');
k.box(-8.0, 8.0, -53.2, 8.0, 11.0, REAR, FRAME);
k.box(-8.6, 11.0, -53.2, 8.6, 13.4, 25.7, FRAME);
k.box(-8.0, 8.0, REAR, 8.0, 9.0, 50.8, FRAME);
k.sym(() => {
    // side guards on their posts, a locker, and the mudguard over the back wheels
    k.box(15.0, 8.0, -15.4, 16.0, 9.6, 21.4, HOOP);
    for (const z of [-13.0, 2.0, 18.0]) {
        k.box(15.1, 9.6, z, 15.8, 12.4, z + 0.8, DARK);
        k.box(8.6, 11.6, z, 15.8, 12.4, z + 0.8, DARK);
    }
    k.box(12.0, 12.4, -13.2, 15.6, 16.6, -1.0, '#33353A');
    k.box(15.6, 12.8, -12.8, 15.68, 16.2, -1.4, DARK);
    k.box(15.68, 14.2, -8.1, 15.85, 14.8, -6.1, C.CHROME);
    const arc = [[-9.4, 9.4], [-8.9, 13.2], [-5.6, 16.4], [0, 17.6], [5.6, 16.4], [8.9, 13.2], [9.4, 10.6]];
    for (let i = 0; i + 1 < arc.length; i++) {
        k.slab(9.1, 17.4, arc[i][1], RZ + arc[i][0], arc[i + 1][1], RZ + arc[i + 1][0], 0.8, C.BLACK, 1);
    }
    k.box(9.3, 7.0, RZ - 9.9, 17.2, 10.0, RZ - 9.5, C.BLACK);
});
// the diesel tank on one side, the battery box on the other
k.box(12.0, 9.8, 3.0, 15.8, 14.2, 17.0, '#9BA1A8');
k.box(12.0, 9.6, 5.6, 15.9, 14.4, 6.4, DARK);
k.box(12.0, 9.6, 13.6, 15.9, 14.4, 14.4, DARK);
k.box(13.2, 14.2, 9.2, 14.6, 14.8, 10.6, C.BLACK);
k.box(-15.8, 10.2, 5.0, -12.0, 14.6, 15.0, '#2F3136');
k.box(-15.9, 10.6, 5.4, -15.8, 14.2, 14.6, DARK);
// the back: an underrun bar, lamps on a board above it, the plate between
k.box(-16.0, 7.0, -54.9, 16.0, 9.2, -53.2, C.BUMPER);
k.box(-15.7, 7.6, -55.05, 15.7, 8.6, -54.9, C.BLACK);
k.sym(() => {
    k.box(8.6, 9.3, -53.7, 14.6, 12.4, -53.2, C.BLACK);
    k.box(12.6, 9.7, -53.9, 14.3, 12.0, -53.7, C.AMBER);
    k.box(10.4, 9.7, -53.9, 12.6, 12.0, -53.7, C.TAIL);
    k.box(10.9, 10.2, -53.98, 12.1, 11.5, -53.9, C.TAIL_GLOW);
    k.box(8.9, 9.7, -53.9, 10.4, 12.0, -53.7, C.REVERSE);
});
k.box(-3.8, 9.3, -53.6, 3.8, 12.0, -53.2, C.BLACK);
k.plate(-3.4, 9.6, 3.4, 11.7, -53.6, false);

// ---- the barrel ----------------------------------------------------------------
k.g('tank');
const TY = 27.0, TA = 13.5, FW = 2 * Math.tan(11.25 * DEG);
// a sixteen-sided drum on the car's long axis
const drum = (a, z1, z2, colour) => {
    const w = a * FW;
    for (const r of [0, 22.5, 45, 67.5, -67.5, -45, -22.5]) {
        k.box(-a, TY - w / 2, z1, a, TY + w / 2, z2, colour, {roll: r});
    }
    k.box(-w / 2, TY - a, z1, w / 2, TY + a, z2, colour);
};
drum(TA, -52.4, -10.5, SHELL);
drum(TA, 17.0, 24.9, SHELL);
drum(11.2, -53.2, -52.4, SHELL);
drum(11.2, 24.9, 25.7, SHELL);
for (const z of [-46.5, -28.6, -11.7, 16.9]) {
    drum(TA + 0.3, z, z + 1.3, HOOP);
}
// between them the shell is plates over the top and underneath, and each
// side is cut flat for one tall pane of glass in a frame
{
    const w = TA * FW, t = 0.6, d = TA - t / 2;
    for (let j = 0; j < 16; j++) {
        const phi = j * 22.5, s = Math.sin(phi * DEG), c = Math.cos(phi * DEG);
        if (Math.abs(s) < 0.5) {
            continue;
        }
        if (j === 4 || j === 12) {
            k.box(-w / 2, TY + d * s - t / 2, -10.5, w / 2, TY + d * s + t / 2, 17.0, SHELL);
            continue;
        }
        let roll = phi % 180;
        if (roll > 90) {
            roll -= 180;
        }
        k.box(d * c - t / 2, TY + d * s - w / 2, -10.5, d * c + t / 2, TY + d * s + w / 2, 17.0, SHELL, {roll: roll});
    }
    const px = TA * Math.cos(33.75 * DEG), ph = TA * Math.sin(33.75 * DEG);
    k.sym(() => {
        k.box(px - 0.15, TY - ph + 0.5, -10.2, px + 0.15, TY + ph - 0.5, 16.7, C.SIGHT_GLASS);
        k.box(px - 0.3, TY + ph - 0.6, -10.5, px + 0.5, TY + ph + 0.3, 17.0, HOOP);
        k.box(px - 0.3, TY - ph - 0.3, -10.5, px + 0.5, TY - ph + 0.6, 17.0, HOOP);
    });
    k.box(-10.9, TY - 6.1, -10.1, 10.9, TY + 6.1, 16.6, C.CARGO);
    for (const a of [32, 45, 60, 75]) {
        const hx = 12.5 * Math.cos(a * DEG), hy = 12.5 * Math.sin(a * DEG);
        k.box(-hx, TY - hy, -10.1, hx, TY + hy, 16.6, C.CARGO);
    }
}
// on top: a walkway, two manholes, a handrail
k.box(-1.6, 40.5, -46.0, 1.6, 40.9, 19.0, HOOP);
for (const z of [-33.8, -0.4]) {
    k.box(-3.0, 40.4, z, 3.0, 41.5, z + 6.4, HOOP);
    k.box(-2.5, 41.5, z + 0.5, 2.5, 42.3, z + 5.9, '#A9AFB6');
    k.box(-0.3, 42.3, z + 2.2, 0.3, 42.8, z + 4.2, DARK);
}
for (const z of [-45.0, -29.0, -13.0, 3.0, 18.2]) {
    k.box(-5.2, 39.6, z, -4.8, 43.4, z + 0.4, DARK);
}
k.box(-5.25, 43.4, -45.2, -4.75, 43.9, 18.8, DARK);
// a ladder up the back, and the orange plate
k.sym(() => k.box(4.4 - 0.3, 12.0, -54.0, 4.4 + 0.3, 40.6, -53.5, DARK));
k.box(-9.0, 12.0, -54.0, -8.4, 38.6, -53.5, DARK);
k.box(-5.2, 12.0, -54.0, -4.6, 40.4, -53.5, DARK);
for (let y = 14.5; y < 38; y += 3.2) {
    k.box(-8.4, y, -53.95, -5.2, y + 0.4, -53.55, DARK);
}
k.box(4.8, 22.6, -53.5, 10.6, 26.6, -53.2, '#F07A1A');
k.box(4.8, 24.5, -53.56, 10.6, 24.7, -53.5, C.BLACK);
// the outlet and its valve
k.box(9.0, 12.2, 19.2, 13.6, 14.4, 22.6, HOOP);
k.box(13.6, 12.6, 20.0, 14.6, 14.0, 21.8, C.CHROME);
k.box(10.6, 14.4, 20.4, 12.0, 15.4, 21.4, '#B5221E');

// ---- the cab -------------------------------------------------------------------
k.g('body');
k.box(-11.0, 9.0, REAR, 11.0, FLOOR, FZ + AR, PD);
k.box(-H, 9.0, FZ + AR, H, FLOOR, 51.2, PD);
k.arch({z: FZ, y: WY, r: AR, half: H, inner: 11.0, floor: 9.0, top: BELT, n: 8, flare: 0.5, lip: C.BLACK,
    open: true, skinIn: 16.0, hollow: true, tubColor: PD});
k.pair(16.0, 9.0, FZ + AR, H, BELT, 51.2, P);
k.box(-H, FLOOR, 51.2, H, BELT, FRONT, P);
k.box(-H, FLOOR, REAR, H, TOP, REAR + 1.2, P);
k.pair(16.0, BELT, REAR + 1.2, H, TOP, 32.7, P);
k.pair(16.0, 42.0, 32.7, H, TOP, zW(42.5) - 0.6, P);
// the door: its seams, a handle, steps ahead of the wheel
for (const z of [33.0, 50.6]) {
    k.pair(H, 16.4, z - 0.1, H + 0.08, BELT, z + 0.1, PD);
}
k.pair(H, 12.0, 50.5, H + 0.08, 16.4, 50.7, PD);
k.pair(H, 18.4, 33.8, H + 0.35, 19.3, 36.6, C.BLACK);
k.pair(H - 0.4, 11.6, 44.6, H + 0.1, 12.0, 50.4, C.BLACK);
// the face
k.box(-11.6, 12.6, FRONT, 11.6, 21.2, FRONT + 0.15, C.BLACK);
for (const y of [13.4, 15.0, 18.8, 20.2]) {
    k.box(-11.2, y, FRONT + 0.1, 11.2, y + 0.45, FRONT + 0.3, '#34363A');
}
k.box(-11.2, 16.6, FRONT + 0.1, -2.6, 17.2, FRONT + 0.3, '#34363A');
k.box(2.6, 16.6, FRONT + 0.1, 11.2, 17.2, FRONT + 0.3, '#34363A');
// the star in its ring
k.box(-2.2, 14.7, FRONT + 0.15, 2.2, 19.1, FRONT + 0.3, C.BLACK);
k.box(-2.2, 18.7, FRONT + 0.3, 2.2, 19.1, FRONT + 0.5, C.CHROME);
k.box(-2.2, 14.7, FRONT + 0.3, 2.2, 15.1, FRONT + 0.5, C.CHROME);
k.pair(1.8, 15.1, FRONT + 0.3, 2.2, 18.7, FRONT + 0.5, C.CHROME);
k.box(-0.2, 16.9, FRONT + 0.3, 0.2, 18.7, FRONT + 0.5, C.CHROME);
k.box(-0.2, 15.6, FRONT + 0.3, 0.2, 17.2, FRONT + 0.5, C.CHROME, {roll: 60});
k.box(-0.2, 15.6, FRONT + 0.3, 0.2, 17.2, FRONT + 0.5, C.CHROME, {roll: -60});
k.pair(11.6, 12.6, FRONT, 13.0, 23.2, FRONT + 0.25, DARK);
k.bumper({front: true, z: 53.6, depth: 2.6, y1: 6.5, y2: 11.6, half: 17.3, cut: 2.2, wrap: FZ + AR + 0.7,
    color: C.BUMPER, strip: false});
k.box(-10.4, 6.5, 53.6, 10.4, 7.6, 53.72, C.BLACK);
k.plate(-3.4, 8.2, 3.4, 10.4, 53.6, true);
k.sym(() => {
    k.box(10.9, 7.5, 53.5, 15.4, 10.9, 53.7, C.BLACK);
    k.box(11.2, 7.8, 53.7, 14.2, 10.6, 53.85, C.LAMP);
    k.box(11.9, 8.5, 53.85, 13.5, 9.9, 53.95, C.LAMP_GLOW);
    k.box(14.2, 7.8, 53.7, 15.1, 10.6, 53.85, C.AMBER);
    k.box(H + 0.3, 8.6, 45.2, H + 0.42, 10.6, 50.2, C.BLACK);
    k.box(H, 12.6, 49.4, H + 0.14, 13.6, 51.4, C.AMBER_SIDE);
});

// the windscreen in its frame, a visor over it, the doors' glass
k.g('glass');
k.slab(-15.6, 15.6, 24.8, zW(24.8) - 0.35, 42.2, zW(42.2) - 0.35, 0.3, C.GLASS);
k.pair(16.3, BELT, 32.7, 16.6, 42.0, zW(42.0) - 0.9, C.GLASS);

k.g('body');
k.slabs(15.6, H, BELT, zW(BELT), TOP, zW(TOP), 1.3, P);
k.slab(-15.6, 15.6, BELT, zW(BELT), 24.8, zW(24.8), 1.3, C.BLACK);
k.slab(-15.6, 15.6, 42.2, zW(42.2), TOP, zW(TOP), 1.3, P);
k.sideGlass({xLow: 16.45, yLow: BELT, xHigh: 16.45, yHigh: 42.0, rearLow: zW(42.0) - 0.9, rearHigh: zW(42.0) - 0.9,
    frontLow: zW(BELT) - 0.5, frontHigh: zW(42.0) - 0.5, bands: 6, thick: 0.7, color: C.BLACK});
k.box(-16.4, 41.4, zW(42) + 0.1, 16.4, 42.4, zW(42) + 2.6, DARK);
k.wipers({y: 24.2, z: 52.45, len: 8.6, pivots: [6.0, -3.0, -12.0]});
// the roof, with a low cap and a hatch
k.box(-H, TOP, REAR, H, 44.5, zW(TOP) + 0.4, P);
k.box(-15.4, 44.5, REAR + 1.4, 15.4, 45.3, 47.6, P);
k.box(-5.0, 45.3, 33.6, 5.0, 45.8, 41.0, PD);
k.sym(() => k.box(11.6, 44.5, 47.6, 13.2, 45.0, 48.4, C.AMBER_SIDE));
// tall mirrors on two arms
k.sym(() => {
    k.box(H, 33.2, 49.7, 18.2, 33.8, 50.3, C.BLACK);
    k.box(H, 27.4, 49.7, 18.2, 28.0, 50.3, C.BLACK);
    k.box(17.9, 26.2, 49.4, 19.6, 35.0, 50.7, C.BLACK);
    k.box(18.1, 26.6, 49.32, 19.4, 34.6, 49.4, C.MIRROR);
});

// ---- inside -------------------------------------------------------------------------
k.g('interior');
k.box(-11.0, FLOOR, REAR + 1.2, 11.0, FLOOR + 0.12, FZ + AR, C.CARPET);
k.box(-16.0, FLOOR, FZ + AR, 16.0, FLOOR + 0.12, 51.2, C.CARPET);
k.frontSeats({floor: FLOOR, top: 15.2, back: 32.3, front: 42.7, backTop: 30.0, x: 6.8, w: 4.0});
k.dash({half: 16.0, back: 46.9, front: 51.2, bottom: 20.0, top: 24.0, driver: 7.0});
k.box(-16.0, FLOOR, 50.0, 16.0, 20.0, 51.2, C.CARPET);
k.console({half: 2.6, floor: FLOOR, top: 14.4, back: 30.0, front: 46.6, lever: 42.2, brake: 36.4});
k.pedals(7.0, 49.2, FLOOR);
k.doorCards({x: 16.0, floor: FLOOR, top: 23.6, back: FZ + AR, front: 51.2, arm: 19.4, doors: [[44.0, 51.0]]});
k.pair(15.6, 15.9, REAR + 1.2, 16.0, 23.6, FZ + AR, '#3C3F46');
k.box(-16.0, FLOOR, REAR + 1.2, 16.0, 30.0, REAR + 1.6, '#3C3F46');
