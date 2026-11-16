// Jeep Wrangler JK, two doors, 2007-18: a box on a ladder frame. A narrow
// flat bonnet between black wings that stand clear of the body, seven
// slots between round lamps, an upright flat windscreen, half doors with
// their hinges showing, a black hardtop, and the spare on the tailgate.
const P = 'paint', PD = 'paint_dark';
const H = 15.0, HOOD = 11.2, FLOOR = 9.1, BELT = 19.6, TOP = 31.8;
const FZ = 26.22, RZ = -25.22;
const COWL = 17.7, BACK = -38.7, FACE = 37.0;
const HARD = '#27282C', WING = C.TRIM, SEAM = '#151517';

const zW = y => 16.6 - (y - 20.1) * 3.3 / 11.8;     // the windscreen leans back a little

// a wing over a wheel: flat on top, sloping down at each end
const wing = (zc, x1, x2) => {
    k.pair(x1, 15.4, zc - 5.0, x2, 16.6, zc + 5.0, WING);
    k.slabs(x1, x2, 16.6, zc + 5.0, 12.4, zc + 9.1, 1.2, WING);
    k.slabs(x1, x2, 12.4, zc - 9.1, 16.6, zc - 5.0, 1.2, WING);
};
// how high the body is cut away under a wing, dz from the axle
const under = dz => Math.abs(dz) <= 5 ? 15.9 : 15.9 - (Math.abs(dz) - 5) * 4.2 / 4.1;

// ---- the tub -----------------------------------------------------------------
k.g('body');
k.box(-H, 7.6, RZ + 8.9, H, FLOOR, COWL, PD);
k.box(-11.7, 7.6, RZ - 8.9, 11.7, FLOOR, RZ + 8.9, PD);
k.box(-H, 7.6, BACK, H, FLOOR, RZ - 8.9, PD);
k.pair(13.9, FLOOR, RZ + 8.9, H, BELT, 16.5, P);
k.pair(13.9, FLOOR, BACK + 1.2, H, BELT, RZ - 8.9, P);
k.box(-H, FLOOR, 16.5, H, BELT, COWL, P);
k.box(-H, FLOOR, BACK, H, BELT, BACK + 1.2, P);
// the body over the back wheels, cut to the line of the wing, and the housings inside
{
    const cuts = [-8.9, -7.9, -6.95, -5.97, -5.0, 5.0, 5.97, 6.95, 7.9, 8.9];
    for (let i = 0; i + 1 < cuts.length; i++) {
        const low = Math.min(under(cuts[i]), under(cuts[i + 1]));
        k.pair(13.9, low, RZ + cuts[i], H, BELT, RZ + cuts[i + 1], P);
    }
    k.pair(11.1, 7.6, RZ - 8.9, 11.7, 16.4, RZ + 8.9, PD);
    k.pair(11.7, 15.8, RZ - 8.9, 13.9, 16.4, RZ + 8.9, PD);
    k.pair(11.7, 7.6, RZ - 8.9, 13.9, 16.4, RZ - 8.3, PD);
    k.pair(11.7, 7.6, RZ + 8.3, 13.9, 16.4, RZ + 8.9, PD);
}
wing(RZ, 14.6, 18.4);
// the doors: seams, hinges, a handle at the back edge
for (const z of [15.9, -2.5]) {
    k.pair(H, FLOOR + 0.5, z - 0.1, H + 0.08, BELT, z + 0.1, PD);
}
k.pair(H, FLOOR + 0.4, -2.5, H + 0.08, FLOOR + 0.6, 15.9, PD);
for (const y of [12.2, 17.4]) {
    k.pair(H, y, 15.5, H + 0.35, y + 0.9, 17.3, C.BLACK);
}
k.pair(H, 16.5, -1.9, H + 0.3, 17.5, 0.9, C.BLACK);
k.pair(H, 7.6, -15.9, 16.7, 9.0, 16.9, C.BLACK);
k.box(H, 16.9, -31.0, H + 0.08, 18.7, -29.2, C.BLACK);
k.mirrors({x: H - 0.1, y: 20.2, z: 14.4, w: 2.6, h: 2.6, d: 1.3});

// ---- the front end -------------------------------------------------------------
k.box(-HOOD, 8.6, COWL, HOOD, 11.0, 34.2, PD);
k.box(-HOOD, 11.0, COWL, HOOD, 19.5, 34.2, P);
k.box(-HOOD, 9.6, 34.2, HOOD, 20.1, FACE, P);
wing(FZ, HOOD, 18.4);
k.pair(HOOD, 17.6, 29.6, HOOD + 0.3, 19.3, 30.7, C.BLACK);
k.box(-H, BELT, 16.5, H, 20.1, COWL, C.BLACK);
k.wipers({y: 20.25, z: 17.25, len: 8.4, pivots: [1.0, -9.0]});

// the face: seven slots, a round lamp each side, an amber one under it
for (let i = -3; i <= 3; i++) {
    k.box(i * 1.72 - 0.5, 12.8, FACE, i * 1.72 + 0.5, 18.4, FACE + 0.12, C.BLACK);
}
k.sym(() => {
    k.box(6.75, 14.75, FACE, 10.45, 18.45, FACE + 0.3, C.BLACK);
    k.box(7.05, 15.6, FACE + 0.3, 10.15, 17.6, FACE + 0.5, C.LAMP);
    k.box(7.6, 15.05, FACE + 0.3, 9.6, 18.15, FACE + 0.5, C.LAMP);
    k.box(7.9, 15.9, FACE + 0.5, 9.3, 17.3, FACE + 0.6, C.LAMP_GLOW);
    k.box(8.5, 12.5, FACE, 10.3, 14.1, FACE + 0.3, C.AMBER);
});
k.box(-8.6, 9.0, FACE, 8.6, 10.9, 38.6, C.BLACK);
k.bumper({front: true, z: 41.9, depth: 3.3, y1: 8.6, y2: 11.4, half: 16.3, cut: 2.4, color: '#26272B', strip: false});
k.plate(-3.2, 9.0, 3.2, 11.0, 41.9, true);
k.sym(() => {
    k.box(9.4, 9.2, 41.9, 11.4, 10.8, 42.05, '#D9D6C2');
    k.box(4.5, 11.4, 39.4, 5.4, 12.3, 41.4, C.DARK_STEEL);
});

k.g('bonnet');
k.box(-HOOD, 19.5, COWL, HOOD, 20.1, 34.2, P);
k.box(-5.0, 20.1, 19.4, 5.0, 20.45, 33.2, P);

// ---- the back end ---------------------------------------------------------------
k.g('body');
k.sym(() => {
    k.box(11.9, 13.4, BACK, H + 0.2, 18.6, BACK - 0.5, C.BLACK);
    k.box(12.3, 16.6, BACK - 0.5, H - 0.2, 18.2, BACK - 0.7, C.TAIL);
    k.box(12.3, 15.4, BACK - 0.5, H - 0.2, 16.6, BACK - 0.7, C.REVERSE);
    k.box(12.3, 13.8, BACK - 0.5, H - 0.2, 15.4, BACK - 0.7, C.TAIL);
    k.box(12.8, 17.0, BACK - 0.7, H - 0.7, 17.8, BACK - 0.78, C.TAIL_GLOW);
});
k.box(-0.1, FLOOR, BACK - 0.06, 0.1, BELT, BACK, PD);
k.pair(-0.2, 11.0, BACK - 0.35, 1.6, 12.0, BACK, C.BLACK);
k.bumper({front: false, z: -42.0, depth: 3.3, y1: 8.6, y2: 11.4, half: 16.3, cut: 2.4, color: '#26272B', strip: false});
k.plate(7.6, 11.5, 11.6, 13.2, BACK, false);
k.box(4.9, 7.65, -41.2, 6.0, 8.6, -36.0, C.EXHAUST);
k.box(5.1, 7.85, -41.26, 5.8, 8.4, -41.2, C.BLACK);
// the spare: an octagon of rubber on a carrier, a grey wheel in it
{
    const y = 18.6, z1 = -43.9, z2 = -39.6;
    k.box(-2.4, 14.0, -39.6, 2.4, 22.6, BACK, C.BLACK);
    for (const a of [0, 45, -45]) {
        k.box(-7.0, y - 2.9, z1, 7.0, y + 2.9, z2, C.RUBBER, {roll: a});
        k.box(-3.7, y - 1.53, z1 - 0.25, 3.7, y + 1.53, z1, '#8C9197', {roll: a});
    }
    k.box(-2.9, y - 7.0, z1, 2.9, y + 7.0, z2, C.RUBBER);
    k.box(-1.53, y - 3.7, z1 - 0.25, 1.53, y + 3.7, z1, '#8C9197');
    k.box(-1.2, y - 1.2, z1 - 0.45, 1.2, y + 1.2, z1 - 0.25, C.BLACK);
    k.box(-0.25, y + 6.6, -41.6, 0.25, 27.0, -41.2, C.BLACK);
    k.box(-1.5, 26.6, -42.0, 1.5, 27.4, -41.2, C.TAIL);
}

// ---- the windscreen, the doors' glass and the hardtop ----------------------------
k.g('glass');
k.slab(-13.7, 13.7, 20.9, zW(20.9) - 0.35, 30.9, zW(30.9) - 0.35, 0.3, C.GLASS);
k.pair(14.3, BELT, -2.4, 14.6, 30.9, 12.9, C.GLASS);
k.pair(14.3, 20.8, -35.4, 14.6, 30.2, -6.2, C.GLASS);
k.box(-12.6, 21.0, -38.3, 12.6, 30.2, -38.0, C.GLASS);

k.g('body');
k.slabs(13.7, H, 20.1, zW(20.1), TOP, zW(TOP), 1.1, HARD);
k.slab(-13.7, 13.7, 20.1, zW(20.1), 20.9, zW(20.9), 1.1, HARD);
k.slab(-13.7, 13.7, 30.9, zW(30.9), TOP, zW(TOP), 1.1, HARD);
// the black corner the mirror sits on, ahead of the glass
k.sideGlass({xLow: 14.45, yLow: BELT, xHigh: 14.45, yHigh: 30.9, rearLow: 12.9, rearHigh: 12.9,
    frontLow: zW(BELT) - 0.4, frontHigh: zW(30.9) - 0.4, bands: 6, thick: 0.7, color: C.BLACK});
k.pair(13.9, 30.9, -2.4, H, TOP, zW(TOP) - 0.5, HARD);
k.pair(13.9, BELT, -6.2, H, TOP, -2.4, HARD);
k.pair(13.9, BELT, -35.4, H, 20.8, -6.2, HARD);
k.pair(13.9, 30.2, -35.4, H, TOP, -6.2, HARD);
k.pair(13.9, BELT, BACK, H, TOP, -35.4, HARD);
k.box(-13.9, BELT, BACK, 13.9, 21.0, -37.7, HARD);
k.box(-13.9, 30.2, BACK, 13.9, TOP, -37.7, HARD);
k.pair(12.6, 21.0, BACK, 13.9, 30.2, -37.7, HARD);
k.box(-H, TOP, BACK + 0.3, H, 32.5, 14.7, HARD);
k.box(-14.2, 32.5, BACK + 0.9, 14.2, 33.3, 14.0, HARD);
k.box(-14.2, 33.3, -3.6, 14.2, 33.34, -3.4, SEAM);
k.box(-0.1, 33.3, -3.4, 0.1, 33.34, 14.0, SEAM);
k.pair(H, BELT, -2.5, H + 0.06, TOP, -2.3, SEAM);

// ---- inside -------------------------------------------------------------------------
k.g('interior');
k.box(-13.9, FLOOR, RZ + 8.9, 13.9, FLOOR + 0.12, 16.5, C.CARPET);
k.box(-11.1, FLOOR, BACK + 1.2, 11.1, FLOOR + 0.12, RZ + 8.9, C.CARPET);
k.frontSeats({floor: FLOOR, top: 11.2, back: -2.9, front: 9.1, backTop: 24.6, x: 7.3, w: 4.3});
k.rearBench({half: 11.1, floor: FLOOR, top: 11.2, back: -17.7, front: -6.0, backTop: 24.1, places: 2, headrest: true});
k.dash({half: 13.9, back: 13.2, front: 16.5, bottom: 16.6, top: 20.0, driver: 6.4});
k.box(-13.9, FLOOR, 15.4, 13.9, 16.6, 16.5, C.CARPET);
k.box(-10.6, 18.3, 12.9, -4.2, 18.9, 13.2, C.BLACK);
k.console({half: 2.1, floor: FLOOR, top: 11.5, back: -1.3, front: 13.0, lever: 8.4, brake: 3.0});
k.pedals(6.4, 14.6, FLOOR);
k.doorCards({x: 13.9, floor: FLOOR, top: 19.3, back: -2.4, front: 16.3, arm: 15.8, doors: [[-2.4, 16.2]]});
// the bar over the seats
k.pair(11.9, BELT, -5.0, 13.3, 31.0, -3.6, '#2C2D31');
k.box(-13.3, 30.2, -5.0, 13.3, 31.6, -3.6, '#2C2D31');
k.pair(11.9, 30.2, -34.0, 13.3, 31.6, -5.0, '#2C2D31');
k.box(-1.6, 28.2, 11.6, 1.6, 29.4, 11.9, C.BLACK);
k.box(-1.4, 28.35, 11.52, 1.4, 29.25, 11.6, C.MIRROR);
