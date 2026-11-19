// Ford Mustang fastback, 1967: a long bonnet, a short deck and a roof that
// runs down to the tail in one sweep. A wide dark grille with the pony in
// its corral, a round lamp at each end of it, thin chrome bumpers, a
// scoop ahead of each back wheel, three upright lamps each side of the
// filler cap, and two stripes over the top.
const P = 'paint', PD = 'paint_dark', A = C.ACCENT;
const H = 15.0, FLOOR = 5.46, BELT = 14.4;
const FZ = 26.28, RZ = -25.28, WY = 6.48, AR = 7.3;
const NOSE = 40.4, TAIL = -39.9, LID = 12.0, TOP = 24.9;

const BON = [[41.0, 13.5], [36, 14.2], [28, 14.75], [19.5, 15.05]];
const FEN = BON.map(([z, y]) => [z, y - 0.1]).concat([[19.2, 15.0]]);

const sx = y => 14.3 - (y - 14.4) * 1.2 / 10.5;
const nS = [10.5, 1.2, 0];
const zW = y => 19.3 - (y - 15.0) * 11.7 / 9.9;      // windscreen
const zR = y => -31.3 + (y - 15.2) * 21.3 / 10.0;    // the fastback
const zQ = y => -16.5 + (y - 14.4) * 6.7 / 10.5;     // the back edge of the quarter glass

// five strips across the car: paint, stripe, paint, stripe, paint
const striped = (half, draw) => {
    for (const [a, b, c] of [[-half, -3.6, P], [-3.6, -1.3, A], [-1.3, 1.3, P], [1.3, 3.6, A], [3.6, half, P]]) {
        draw(a, b, c);
    }
};

// ---- the floor and the sides ------------------------------------------------
k.g('body');
k.box(-H, 4.0, RZ + AR, H, 5.3, FZ - AR, PD);
k.pair(13.9, 5.3, RZ + AR, H, BELT, FZ - AR, P);
k.pair(14.7, 4.0, RZ + AR, 15.25, 5.5, FZ - AR, C.BLACK);
for (const z of [17.0, -3.3]) {
    k.pair(H, 5.9, z - 0.1, H + 0.08, BELT - 0.3, z + 0.1, PD);
}
// the stripe along the sill, and the crease that runs back into the scoop
k.pair(H, 6.2, RZ + AR + 0.4, H + 0.12, 7.1, FZ - AR - 0.4, A);
k.pair(H, 6.2, FZ + AR + 0.8, H + 0.12, 7.1, 38.6, A);
k.pair(H, 6.2, -38.2, H + 0.12, 7.1, RZ - AR - 0.8, A);
k.pair(H, 11.7, -12.4, H + 0.07, 11.9, FZ - AR - 0.2, PD);
k.pair(H, 8.3, -12.4, H + 0.07, 8.5, 8.0, PD);
k.pair(H, 8.3, -16.6, H + 0.3, 11.9, -12.4, C.BLACK);
k.pair(H + 0.3, 8.3, -14.7, H + 0.42, 11.9, -14.3, P);
k.pair(H + 0.28, 8.1, -16.8, H + 0.4, 8.3, -12.2, C.CHROME);
k.pair(H + 0.28, 11.9, -16.8, H + 0.4, 12.1, -12.2, C.CHROME);
k.pair(14.25, 14.25, -16.7, H, 14.52, 19.1, C.CHROME);
k.pair(H, 11.7, 1.4, H + 0.3, 12.3, 3.7, C.CHROME);
k.mirrors({x: H - 0.1, y: 14.5, z: 14.4, w: 1.7, h: 1.4, d: 0.9, color: C.CHROME, arm: C.CHROME});

// ---- the front end -------------------------------------------------------------
k.arch({z: FZ, y: WY, r: AR, half: H, inner: 10.3, floor: 5.0, top: z => k.at(FEN, z) - 0.3, n: 8, flare: 0.45});
k.box(-H, 5.0, FZ + AR, H, 13.2, NOSE, P);
k.loft(LID + 0.05, H, FEN, 1.3, P);
k.loft(-H, -LID - 0.05, FEN, 1.3, P);
// the shut line down each side of the bonnet
k.loft(LID + 0.05, LID + 0.3, FEN, 0.04, PD, 1);
k.loft(-LID - 0.3, -LID - 0.05, FEN, 0.04, PD, 1);
k.box(-LID, 14.4, 19.3, LID, 15.0, 20.9, C.BLACK);
k.wipers({y: 15.1, z: 20.1});

// the face: the grille, the pony in its corral, a lamp at each end
k.box(-9.9, 7.4, NOSE, 9.9, 12.2, NOSE + 0.15, C.BLACK);
for (const y of [8.2, 9.0, 10.4, 11.2]) {
    k.box(-9.6, y, NOSE + 0.1, 9.6, y + 0.2, NOSE + 0.24, '#33353A');
}
k.box(-10.1, 12.2, NOSE, 10.1, 12.5, NOSE + 0.4, C.CHROME);
k.box(-10.1, 7.1, NOSE, 10.1, 7.4, NOSE + 0.4, C.CHROME);
k.pair(9.9, 7.4, NOSE, 10.1, 12.2, NOSE + 0.4, C.CHROME);
k.pair(2.0, 9.65, NOSE + 0.15, 9.7, 9.9, NOSE + 0.4, C.CHROME);
k.box(-2.0, 8.5, NOSE + 0.15, 2.0, 8.75, NOSE + 0.45, C.CHROME);
k.box(-2.0, 10.85, NOSE + 0.15, 2.0, 11.1, NOSE + 0.45, C.CHROME);
k.pair(1.75, 8.5, NOSE + 0.15, 2.0, 11.1, NOSE + 0.45, C.CHROME);
k.box(-1.1, 9.4, NOSE + 0.15, 1.1, 10.3, NOSE + 0.5, C.CHROME);
k.sym(() => {
    // each lamp round, in a bucket at the end of the wing, a chrome ring round the glass
    k.discZ(12.3, 9.95, 2.2, NOSE - 0.2, NOSE + 0.55, P);
    k.roundLamp(12.3, 9.95, 1.55, NOSE + 0.55, C.LAMP, true, {bezel: C.CHROME, ring: 0.32, depth: 0.3});
    k.box(9.9, 4.75, 42.3, 12.6, 5.75, 42.5, C.AMBER);
});
k.bumper({front: true, z: 42.3, depth: 1.9, y1: 4.4, y2: 6.1, half: H + 0.3, cut: 1.8, wrap: 37.4,
    color: C.CHROME, strip: false});
k.plate(-3.0, 4.35, 3.0, 6.15, 42.3, true);
k.box(-13.2, 3.9, 37.6, 13.2, 5.0, 40.9, PD);

k.g('bonnet');
striped(LID, (a, b, c) => k.loft(a, b, BON, 0.6, c));
k.box(-4.0, 14.6, 22.4, 4.0, 15.65, 28.6, P);
k.box(-3.5, 14.95, 28.6, 3.5, 15.45, 28.72, C.BLACK);

// ---- the back end ---------------------------------------------------------------
k.g('body');
k.arch({z: RZ, y: WY, r: AR, half: H, inner: 10.3, floor: 5.0, top: BELT, n: 8, flare: 0.45,
    open: true, skinIn: 13.9});
k.box(-H, 5.0, TAIL, H, BELT, RZ - AR, P);
k.pair(LID + 0.05, BELT, TAIL, H, 15.3, -31.3, P);
k.pair(LID + 0.05, 15.3, TAIL, H - 0.3, 15.9, -38.5, P);
k.box(-LID - 0.05, BELT, TAIL, LID + 0.05, 14.7, TAIL + 0.6, P);

k.box(-13.5, 8.4, TAIL, 13.5, 13.4, TAIL - 0.15, C.BLACK);
k.box(-13.7, 13.4, TAIL, 13.7, 13.65, TAIL - 0.3, C.CHROME);
k.box(-13.7, 8.15, TAIL, 13.7, 8.4, TAIL - 0.3, C.CHROME);
k.sym(() => {
    for (const x of [4.0, 7.2, 10.4]) {
        k.lamp(x, 9.1, x + 2.5, 12.7, TAIL - 0.15, C.TAIL, false, {bezel: C.CHROME, depth: 0.35});
    }
});
k.box(-1.5, 9.5, TAIL - 0.15, 1.5, 12.3, TAIL - 0.45, C.CHROME);
k.box(-0.8, 10.2, TAIL - 0.45, 0.8, 11.6, TAIL - 0.52, '#2B4FA8');
k.bumper({front: false, z: -42.0, depth: 2.1, y1: 4.4, y2: 6.2, half: H + 0.3, cut: 1.8, wrap: -36.6,
    color: C.CHROME, strip: false});
k.plate(-3.0, 4.4, 3.0, 6.2, -42.0, false);
k.box(-13.2, 3.9, -40.4, 13.2, 5.0, -37.4, PD);
k.sym(() => {
    k.box(5.4, 3.4, -41.7, 7.7, 4.4, -37.7, C.CHROME);
    k.box(5.7, 3.6, -41.76, 7.4, 4.2, -41.7, C.BLACK);
});

k.g('boot');
striped(LID, (a, b, c) => {
    k.box(a, 14.7, TAIL, b, 15.3, -31.3, c);
    k.box(a, 15.3, TAIL, b, 15.9, -38.5, c);
});

// ---- the cabin --------------------------------------------------------------------
k.g('glass');
k.slab(-12.6, 12.6, 15.0, 19.3, (TOP - 0.5), zW((TOP - 0.5)), 0.3, C.GLASS);
k.slab(-12.3, 12.3, 17.45, zR(17.6), 24.15, zR(24.3), 0.3, C.GLASS);
k.sideGlass({xLow: sx(BELT) - 0.15, yLow: BELT, xHigh: sx(TOP) - 0.15, yHigh: TOP,
    rearLow: zQ(BELT), rearHigh: zQ(TOP), frontLow: zW(BELT) - 0.5, frontHigh: zW(TOP) - 0.5, bands: 10});

k.g('body');
k.planks([sx(BELT), BELT, zW(BELT) - 0.8], [sx((TOP - 0.7)), (TOP - 0.7), zW((TOP - 0.7)) - 0.8], nS, 1.7, 1.4, P);
k.planks([sx(BELT) + 0.02, BELT, -3.3], [sx((TOP - 0.7)) + 0.02, (TOP - 0.7), -3.3], nS, 0.5, 0.4, C.BLACK, 0);
// the sail panel between the quarter glass and the slope, with its louvres
k.sideGlass({xLow: sx(BELT), yLow: BELT, xHigh: sx(25.2), yHigh: 25.2, rearLow: zR(BELT), rearHigh: zR(25.2),
    frontLow: zQ(BELT), frontHigh: zQ(25.2), bands: 12, thick: 0.8, color: P, off: -1});
k.planks([sx(BELT), BELT, zQ(BELT)], [sx((TOP - 0.7)), (TOP - 0.7), zQ((TOP - 0.7))], nS, 0.7, 0.9, C.BLACK, 0);
for (const d of [2.2, 3.4, 4.6]) {
    k.planks([sx(17.6), 17.6, zQ(17.6) - d], [sx(21.4), 21.4, zQ(21.4) - d], nS, 0.5, 0.16, C.BLACK, 1);
}
// the slope: a rail down each side, a panel above the glass and one below
k.planks([sx(15.2) - 1.0, 15.2, zR(15.2)], [sx(25.2) - 1.0, 25.2, zR(25.2)], [0, 21.3, -10.0], 2.0, 1.4, P);
striped(12.4, (a, b, c) => {
    k.slab(a, b, 15.2, zR(15.2), 17.6, zR(17.6), 0.6, c);
    k.slab(a, b, 24.3, zR(24.3), 25.2, zR(25.2), 0.6, c);
});

const ROOF = [[zW(TOP) + 0.6, 25.0], [1.0, 25.7], [-6.0, 25.75], [zR(25.2) + 0.2, 25.25]];
const EDGE = ROOF.map(([z, y]) => [z, y - 0.3]);
striped(11.4, (a, b, c) => k.loft(a, b, ROOF, 0.9, c));
k.loft(11.4, 12.95, EDGE, 0.9, P);
k.loft(-12.95, -11.4, EDGE, 0.9, P);

// ---- inside -------------------------------------------------------------------------
k.g('interior');
k.box(-13.9, 5.3, RZ + AR - 0.6, 13.9, FLOOR, FZ - AR - 0.8, C.CARPET);
k.frontSeats({floor: FLOOR, top: 7.36, back: -2.9, front: 9.14, backTop: 19.46, x: 7.285, w: 4.285,
    cloth: '#2F3036'});
k.rearBench({half: 13.4, floor: FLOOR, top: 7.4, back: -17.8, front: -7.1, backTop: 17.5, places: 2, rake: 8,
    cloth: '#2F3036'});
k.dash({half: 13.5, back: 14.6, front: FZ - AR, bottom: 10.6, top: 14.4, reach: 19.3, driver: 6.4});
k.console({half: 1.8, floor: FLOOR, top: 7.9, back: -1.3, front: 14.1, lever: 9.8, brake: 4.0});
k.pedals(6.4, 16.2, FLOOR);
k.doorCards({x: 13.9, floor: FLOOR, top: 14.1, back: RZ + AR, front: FZ - AR - 0.8, arm: 10.6,
    doors: [[-2.9, 16.6]]});
k.pair(10.3, FLOOR, 17.4, 13.9, 12.0, FZ - AR, C.CARPET);
k.box(-13.4, 14.2, -31.3, 13.4, 14.6, -18.6, C.TRIM);
k.box(-1.6, 22.4, 9.4, 1.6, 23.5, 9.7, C.BLACK);
k.box(-1.4, 22.55, 9.32, 1.4, 23.35, 9.4, C.MIRROR);
