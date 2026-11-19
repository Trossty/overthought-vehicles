// Tofaş Şahin, 1988-2002: the Fiat 131 as Bursa built it. Square as a
// shoebox: a flat bonnet and a flat boot almost level with the waist, an
// upright cabin, big oblong lamps with a black slatted grille between,
// black plastic bumpers.
const P = 'paint', PD = 'paint_dark';
const H = 14.5, SILL = 6.0, FLOOR = 7.5, BELT = 17.5;
const FZ = 24.18, RZ = -24.18, WY = 6.25, AR = 7.1;
const NOSE = 37.8, TAIL = -37.8, LID = 12.0, TOP = 30.0;
const BUMP = '#2E2F33';

const BON = [[37.9, 16.3], [30, 16.9], [19.2, 17.3]];
const FEN = BON.map(([z, y]) => [z, y - 0.1]).concat([[17.5, 17.25]]);

const sx = y => 13.8 - (y - 17.5) * 0.9 / 12.5;
const nS = [12.5, 0.9, 0];
const zW = y => 17.7 - (y - 17.6) * 8.0 / 12.4;      // windscreen
const zR = y => -23.8 + (y - 18.3) * 8.2 / 11.7;     // rear window
const zC = y => -17.4 + (y - 17.5) * 3.6 / 12.5;     // the back edge of the rear door's glass

// ---- the floor and the sides ------------------------------------------------
k.g('body');
k.box(-H, SILL, RZ + AR, H, 7.3, FZ - AR, PD);
k.pair(13.4, 7.3, RZ + AR, H, BELT, FZ - AR, P);
for (const z of [16.6, -2.9]) {
    k.pair(H, 7.6, z - 0.1, H + 0.08, BELT - 0.4, z + 0.1, PD);
}
k.pair(H, 13.0, -16.7, H + 0.08, BELT - 0.4, -16.5, PD);
k.pair(H, 11.5, RZ + AR + 0.3, H + 0.24, 12.3, FZ - AR - 0.3, C.BLACK);
k.pair(H, 11.5, FZ + AR + 1.0, H + 0.24, 12.3, 35.6, C.BLACK);
k.pair(H, 11.5, -36.6, H + 0.24, 12.3, RZ - AR - 1.0, C.BLACK);
k.pair(13.75, 17.35, -17.6, H, 17.62, 17.6, C.BLACK);
k.handles(H, 15.0, -0.5);
k.handles(H, 15.0, -14.0);
k.mirrors({x: H - 0.1, y: 17.55, z: 14.4, w: 1.8, h: 1.5, d: 1.0});
k.box(H, 15.2, -29.6, H + 0.07, 16.9, -27.6, PD);
k.pair(12.7, 6.0, FZ - AR, 14.7, 8.6, FZ - AR + 0.3, C.BLACK);
k.pair(12.7, 6.0, RZ - AR, 14.7, 8.6, RZ - AR + 0.3, C.BLACK);

// ---- the front end -------------------------------------------------------------
k.arch({z: FZ, y: WY, r: AR, half: H, inner: 10.3, floor: 7.0, top: z => k.at(FEN, z) - 0.3, n: 8, flare: 0.25});
k.box(-H, 7.0, FZ + AR, H, 15.6, NOSE, P);
k.loft(LID + 0.05, H, FEN, 1.3, P);
k.loft(-H, -LID - 0.05, FEN, 1.3, P);
k.box(-LID, 16.6, 17.7, LID, 17.25, 19.2, C.BLACK);
k.wipers({y: 17.35, z: 18.6, pivots: [1.0, -9.0], len: 8.6});

// the face: two big lamps, the grille between them, amber round the corners
k.box(-9.2, 11.6, NOSE, 9.2, 15.4, NOSE + 0.15, C.BLACK);
for (const y of [12.0, 12.9, 13.8, 14.7]) {
    k.box(-8.9, y, NOSE + 0.1, 8.9, y + 0.35, NOSE + 0.32, '#34363A');
}
k.box(-1.3, 12.9, NOSE + 0.25, 1.3, 14.1, NOSE + 0.45, C.CHROME);
k.box(-0.9, 13.2, NOSE + 0.4, 0.9, 13.8, NOSE + 0.5, '#2B4FA8');
k.sym(() => {
    k.lamp(9.5, 11.6, 13.2, 15.4, NOSE, C.LAMP, true);
    k.box(13.45, 11.6, 35.6, H + 0.12, 15.4, NOSE + 0.3, C.AMBER);
});
k.bumper({front: true, z: 39.25, depth: 1.8, y1: 7.0, y2: 10.9, half: H + 0.35, cut: 1.6, wrap: FZ + AR + 0.6,
    color: BUMP, stripColor: '#4A4C52'});
k.plate(-3.4, 7.7, 3.4, 9.9, 39.4, true);
k.box(-12.6, 6.1, 36.2, 12.6, 7.0, 38.3, PD);

k.g('bonnet');
k.loft(-LID, LID, BON, 0.6, P);
k.pair(3.6, 17.0, 21.0, 4.2, 17.25, 21.6, C.BLACK);

// ---- the back end ---------------------------------------------------------------
k.g('body');
k.arch({z: RZ, y: WY, r: AR, half: H, inner: 10.3, floor: 7.0, top: BELT, n: 8, flare: 0.25,
    open: true, skinIn: 13.4, tubTo: -17.75});
k.pair(12.85, FLOOR, -17.75, 13.4, 14.0, RZ + AR, C.CARPET);
k.box(-H, 7.0, TAIL, H, BELT, RZ - AR, P);
k.pair(LID + 0.05, BELT, TAIL, H, 18.1, -17.2, P);
k.box(-LID - 0.05, BELT, TAIL, LID + 0.05, 17.7, TAIL + 0.6, P);

k.sym(() => {
    k.box(7.6, 11.9, TAIL + 0.1, H, 16.4, TAIL - 0.2, C.BLACK);
    k.box(11.9, 14.3, -35.6, H + 0.12, 16.15, TAIL - 0.35, C.AMBER);
    k.box(7.85, 12.15, TAIL, H, 14.3, TAIL - 0.35, C.TAIL);
    k.box(9.0, 12.6, TAIL - 0.3, 13.4, 13.85, TAIL - 0.42, C.TAIL_GLOW);
    k.box(7.85, 14.3, TAIL, 9.6, 16.15, TAIL - 0.35, C.REVERSE);
    k.box(9.6, 14.3, TAIL, 11.9, 16.15, TAIL - 0.35, C.TAIL);
});
k.plate(-3.4, 12.6, 3.4, 14.8, TAIL, false);
k.box(-2.2, 15.6, TAIL, 2.2, 16.1, TAIL - 0.1, C.CHROME);
k.box(-0.3, 16.6, TAIL, 0.3, 17.1, TAIL - 0.12, C.CHROME);
k.bumper({front: false, z: -39.4, depth: 1.8, y1: 7.0, y2: 10.9, half: H + 0.35, cut: 1.6, wrap: RZ - AR - 0.6,
    color: BUMP, stripColor: '#4A4C52'});
k.box(2.4, 6.05, -35.6, 8.0, 7.0, -31.6, C.DARK_STEEL);
k.box(4.9, 6.05, -39.0, 5.9, 6.95, -35.6, C.EXHAUST);
k.box(5.1, 6.25, -39.06, 5.7, 6.75, -39.0, C.BLACK);

k.g('boot');
k.box(-LID, 17.7, TAIL, LID, 18.3, -23.8, P);

// ---- the cabin --------------------------------------------------------------------
k.g('glass');
k.slab(-12.7, 12.7, 17.6, 17.7, (TOP - 0.5), zW((TOP - 0.5)), 0.3, C.GLASS);
k.slab(-12.7, 12.7, 18.3, -23.8, (TOP - 0.5), zR((TOP - 0.5)), 0.3, C.GLASS);
k.sideGlass({xLow: sx(17.5) - 0.15, yLow: 17.5, xHigh: sx(TOP) - 0.15, yHigh: TOP,
    rearLow: zC(17.5), rearHigh: zC(TOP), frontLow: zW(17.5) - 0.5, frontHigh: zW(TOP) - 0.5, bands: 8});

k.g('body');
k.planks([sx(17.5), 17.5, zW(17.5) - 0.8], [sx((TOP - 0.7)), (TOP - 0.7), zW((TOP - 0.7)) - 0.8], nS, 1.8, 1.4, P);
k.planks([sx(17.5) + 0.05, 17.5, -2.9], [sx((TOP - 0.7)) + 0.05, (TOP - 0.7), -2.9], nS, 1.8, 0.5, C.BLACK, 0);
k.sideGlass({xLow: sx(17.5), yLow: 17.5, xHigh: sx(TOP), yHigh: TOP, rearLow: zR(17.5), rearHigh: zR(TOP),
    frontLow: zC(17.5), frontHigh: zC(TOP), bands: 8, thick: 0.8, color: P, off: -1});
k.planks([sx(17.5), 17.5, zC(17.5)], [sx((TOP - 0.7)), (TOP - 0.7), zC((TOP - 0.7))], nS, 0.9, 1.0, C.BLACK, 0);
k.planks([sx(18.1), 18.1, zR(18.1) + 0.8], [sx((TOP - 0.7)), (TOP - 0.7), zR((TOP - 0.7)) + 0.8], nS, 1.8, 1.4, P);

const ROOF = [[zW(TOP) + 0.6, 30.1], [6.5, 30.7], [-12.5, 30.8], [zR(TOP) - 0.7, 30.2]];
const EDGE = ROOF.map(([z, y]) => [z, y - 0.3]);
k.loft(-11.5, 11.5, ROOF, 0.9, P);
k.loft(11.5, 12.95, EDGE, 0.9, P);
k.loft(-12.95, -11.5, EDGE, 0.9, P);
// rain gutters along the roof
k.pair(12.95, 29.5, -15.0, 13.15, 29.8, 9.4, PD);

// ---- inside -------------------------------------------------------------------------
k.g('interior');
k.box(-13.4, 7.3, RZ + AR - 0.7, 13.4, FLOOR, FZ - AR - 0.8, C.CARPET);
k.frontSeats({floor: FLOOR, top: 9.6, back: -2.7, front: 9.18, backTop: 22.5, x: 7.045, w: 4.145});
k.rearBench({half: 12.95, floor: FLOOR, top: 9.6, back: -17.68, front: -5.94, backTop: 22.0, places: 2});
k.dash({half: 13.0, back: 13.3, front: FZ - AR, bottom: 13.6, top: 17.45, reach: 17.7, driver: 6.0});
k.console({half: 1.6, floor: FLOOR, top: 9.9, back: -1.3, front: 12.8, lever: 8.4, brake: 2.4});
k.pedals(6.0, 15.2, FLOOR);
k.doorCards({x: 13.4, floor: FLOOR, top: 17.2, back: RZ + AR, front: FZ - AR - 0.8, arm: 12.8,
    doors: [[-16.2, -3.6], [-2.2, 15.8]]});
k.pair(10.3, FLOOR, 16.3, 13.4, 14.0, FZ - AR, C.CARPET);
k.box(-12.05, 17.6, -23.8, 12.05, 18.2, -17.4, C.TRIM);
k.pair(5.5, 18.2, -22.6, 9.3, 18.3, -19.6, C.BLACK);
k.box(-1.6, 27.3, 11.0, 1.6, 28.5, 11.3, C.BLACK);
k.box(-1.4, 27.45, 10.92, 1.4, 28.35, 11.0, C.MIRROR);
k.pair(3.2, 29.1, 6.8, 9.8, 29.45, 9.6, C.SEAT_CLOTH);
