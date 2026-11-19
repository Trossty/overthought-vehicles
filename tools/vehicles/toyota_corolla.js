// Toyota Corolla E90 saloon, 1987-92: a plain three-box family car. A low
// flat bonnet, an upright cabin with thin pillars, a short square boot;
// oblong lamps either side of a thin slatted grille, amber lamps wrapped
// round the corners, grey bumpers with a black strip.
const P = 'paint', PD = 'paint_dark';
const H = 15.0, SILL = 4.98, FLOOR = 6.48, BELT = 17.0;
const FZ = 25.2, RZ = -24.2, WY = 6.49, AR = 7.4;
const NOSE = 40.4, TAIL = -40.9, LID = 12.5, TOP = 29.4;

// the bonnet from the nose back to the scuttle, and the wings beside it
const BON = [[40.5, 14.7], [36.5, 15.6], [29, 16.4], [21, 17.0]];
const FEN = BON.map(([z, y]) => [z, y - 0.1]).concat([[19.3, 16.95]]);

// the cabin leans in above the waist; these are its lines seen from the side
const sx = y => 14.3 - (y - 17) * 1.3 / 12.4;
const nS = [12.4, 1.3, 0];
const zW = y => 19.4 - (y - 17.1) * 11 / 12.3;      // windscreen
const zR = y => -27.0 + (y - 18.2) * 10.6 / 11.3;   // rear window
const zC = y => -19.6 + (y - 17) * 4.7 / 12.4;      // the back edge of the rear door's glass

// ---- the floor and the sides ------------------------------------------------
k.g('body');
k.box(-H, SILL, RZ + AR, H, 6.3, FZ - AR, PD);
k.pair(13.9, 6.3, RZ + AR, H, BELT, FZ - AR, P);
for (const z of [17.3, -2.7]) {
    k.pair(H, 6.6, z - 0.1, H + 0.08, BELT - 0.4, z + 0.1, PD);
}
k.pair(H, 12.8, -16.5, H + 0.08, BELT - 0.4, -16.3, PD);
k.pair(H, 10.6, RZ + AR + 0.3, H + 0.22, 11.4, FZ - AR - 0.3, C.BLACK);
k.pair(H, 10.6, FZ + AR + 1.0, H + 0.22, 11.4, 38.3, C.BLACK);
k.pair(H, 10.6, -39.2, H + 0.22, 11.4, RZ - AR - 1.0, C.BLACK);
k.pair(14.25, 16.85, -19.4, H, 17.12, 19.2, C.BLACK);
k.handles(H, 14.3, -0.3);
k.handles(H, 14.3, -13.9);
k.mirrors({x: H - 0.1, y: 17.05, z: 16.2, w: 1.9, h: 1.5, d: 1.0});
k.box(H, 14.9, -30.0, H + 0.07, 16.6, -27.9, PD);
k.pair(13.3, 5.0, FZ - AR, 15.2, 8.0, FZ - AR + 0.3, C.BLACK);
k.pair(13.3, 5.0, RZ - AR, 15.2, 8.0, RZ - AR + 0.3, C.BLACK);

// ---- the front end -------------------------------------------------------------
k.arch({z: FZ, y: WY, r: AR, half: H, inner: 10.8, floor: 5.98, top: z => k.at(FEN, z) - 0.3, n: 8, flare: 0.3});
k.box(-H, 5.98, FZ + AR, H, 14.9, 37.0, P);
k.box(-H, 5.98, 37.0, H, 14.1, NOSE, P);
k.loft(LID + 0.05, H, FEN, 1.3, P);
k.loft(-H, -LID - 0.05, FEN, 1.3, P);
k.box(-LID, 16.3, 19.4, LID, 16.95, 21.0, C.BLACK);
k.wipers({y: 17.05, z: 20.2});
k.box(-13.9, 16.9, 20.3, -13.7, 22.5, 20.5, C.CHROME);

k.box(-6.3, 11.2, NOSE, 6.3, 13.7, NOSE + 0.15, C.BLACK);
for (const y of [11.55, 12.3, 13.05]) {
    k.box(-6.0, y, NOSE + 0.1, 6.0, y + 0.3, NOSE + 0.3, '#3A3C40');
}
k.box(-0.9, 12.0, NOSE + 0.2, 0.9, 12.95, NOSE + 0.42, C.CHROME);
k.sym(() => {
    k.lamp(6.7, 11.2, 12.7, 13.7, NOSE, C.LAMP, true);
    k.box(12.95, 11.2, 38.3, H + 0.12, 13.7, NOSE + 0.3, C.AMBER);
});
k.bumper({front: true, z: 41.45, depth: 1.7, y1: 6.3, y2: 10.0, half: H + 0.3, cut: 1.8, wrap: FZ + AR + 0.6});
k.plate(-3.4, 7.1, 3.4, 9.3, 41.6, true);
k.box(-13.2, 5.1, 38.6, 13.2, 6.3, 40.9, PD);
k.box(-7.0, 5.35, 40.85, 7.0, 5.95, 41.0, C.BLACK);

k.g('bonnet');
k.loft(-LID, LID, BON, 0.6, P);
k.pair(4.0, 16.7, 23.2, 4.6, 16.95, 23.8, C.BLACK);

// ---- the back end ---------------------------------------------------------------
k.g('body');
k.arch({z: RZ, y: WY, r: AR, half: H, inner: 10.8, floor: 5.98, top: BELT, n: 8, flare: 0.3,
    open: true, skinIn: 13.9, tubTo: -17.8});
k.pair(13.3, FLOOR, -17.8, 13.9, 14.3, RZ + AR, C.CARPET);
k.box(-H, 5.98, TAIL, H, BELT, RZ - AR, P);
k.pair(LID + 0.05, BELT, TAIL, H, 17.8, -19.4, P);
k.box(-LID - 0.05, BELT, TAIL, LID + 0.05, 17.6, TAIL + 0.6, P);

k.sym(() => {
    k.box(6.4, 12.5, TAIL + 0.1, H, 16.0, TAIL - 0.2, C.BLACK);
    k.box(12.4, 12.75, -38.6, H + 0.12, 15.75, TAIL - 0.35, C.AMBER);
    k.box(8.3, 12.75, TAIL, 12.4, 15.75, TAIL - 0.35, C.TAIL);
    k.box(9.0, 13.4, TAIL - 0.3, 11.7, 15.1, TAIL - 0.42, C.TAIL_GLOW);
    k.box(6.65, 12.75, TAIL, 8.3, 15.75, TAIL - 0.35, C.REVERSE);
    k.box(4.0, 15.3, TAIL - 0.12, 6.0, 15.7, TAIL - 0.22, C.CHROME);
});
k.box(-6.4, 12.5, TAIL, 6.4, 16.0, TAIL - 0.12, PD);
k.plate(-3.4, 12.9, 3.4, 15.1, TAIL - 0.12, false);
k.box(-0.3, 16.3, TAIL, 0.3, 16.8, TAIL - 0.12, C.CHROME);
k.bumper({front: false, z: -41.75, depth: 1.7, y1: 6.3, y2: 10.0, half: H + 0.3, cut: 1.8, wrap: RZ - AR - 0.6});
k.box(2.6, 5.0, -38.0, 8.6, 5.98, -33.2, C.DARK_STEEL);
k.box(4.9, 5.0, -41.4, 6.0, 5.95, -38.0, C.EXHAUST);
k.box(5.1, 5.2, -41.46, 5.8, 5.75, -41.4, C.BLACK);

k.g('boot');
k.box(-LID, 17.6, TAIL, LID, 18.2, -27.0, P);

// ---- the cabin --------------------------------------------------------------------
k.g('glass');
k.slab(-12.9, 12.9, 17.1, 19.4, TOP - 0.5, zW(TOP - 0.5), 0.3, C.GLASS);
k.slab(-12.9, 12.9, 18.2, -27.0, TOP - 0.5, zR(TOP - 0.5), 0.3, C.GLASS);
k.sideGlass({xLow: sx(17) - 0.15, yLow: 17, xHigh: sx(TOP) - 0.15, yHigh: TOP,
    rearLow: zC(17), rearHigh: zC(TOP), frontLow: zW(17) - 0.5, frontHigh: zW(TOP) - 0.5, bands: 10});

k.g('body');
k.planks([sx(17), 17, zW(17) - 0.93], [sx(TOP - 0.8), TOP - 0.8, zW(TOP - 0.8) - 0.93], nS, 1.9, 1.5, P);
k.planks([sx(17) + 0.05, 17, -2.7], [sx(TOP - 0.6) + 0.05, TOP - 0.6, -2.7], nS, 1.8, 0.5, C.BLACK, 0);
k.sideGlass({xLow: sx(17), yLow: 17, xHigh: sx(TOP), yHigh: TOP, rearLow: zR(17), rearHigh: zR(TOP),
    frontLow: zC(17), frontHigh: zC(TOP), bands: 10, thick: 0.8, color: P, off: -1});
k.planks([sx(17), 17, zC(17)], [sx(TOP - 0.6), TOP - 0.6, zC(TOP - 0.6)], nS, 0.9, 1.0, C.BLACK, 0);
k.planks([sx(17.6), 17.6, zR(17.6) + 0.93], [sx(TOP - 0.8), TOP - 0.8, zR(TOP - 0.8) + 0.93], nS, 1.9, 1.5, P);

const ROOF = [[zW(TOP) + 0.6, 29.5], [5.0, 30.2], [-12.0, 30.3], [zR(TOP) - 0.7, 29.6]];
const EDGE = ROOF.map(([z, y]) => [z, y - 0.3]);
k.loft(-11.6, 11.6, ROOF, 0.9, P);
k.loft(11.6, 13.05, EDGE, 0.9, P);
k.loft(-13.05, -11.6, EDGE, 0.9, P);

// ---- inside -------------------------------------------------------------------------
k.g('interior');
k.box(-13.9, 6.3, RZ + AR - 1.0, 13.9, FLOOR, FZ - AR - 0.8, C.CARPET);
k.frontSeats({floor: FLOOR, top: 8.58, back: -2.85, front: 9.14, backTop: 21.98, x: 7.29, w: 4.29});
k.rearBench({half: 13.39, floor: FLOOR, top: 8.58, back: -17.7, front: -5.98, backTop: 21.48, places: 2});
k.dash({half: 13.5, back: 14.3, front: FZ - AR, bottom: 12.6, top: 16.95, reach: 19.4, driver: 6.4});
k.console({floor: FLOOR, top: 9.0, back: -4.0, front: 13.8, lever: 7.4, brake: -1.5});
k.pedals(6.4, 15.6, FLOOR);
k.doorCards({x: 13.9, floor: FLOOR, top: 16.7, back: RZ + AR, front: FZ - AR - 0.8, arm: 12.0,
    doors: [[-16.0, -3.4], [-2.0, 16.4]]});
k.pair(10.8, FLOOR, 17.0, 13.9, 14.3, FZ - AR, C.CARPET);
k.box(-12.55, 17.3, -27.0, 12.55, 18.0, -19.4, C.TRIM);
k.pair(5.5, 18.0, -25.5, 9.5, 18.1, -22.5, C.BLACK);
k.box(-1.7, 26.4, 9.9, 1.7, 27.7, 10.2, C.BLACK);
k.box(-1.5, 26.55, 9.82, 1.5, 27.55, 9.9, C.MIRROR);
k.pair(3.4, 28.5, 5.0, 10.2, 28.85, 8.2, C.SEAT_CLOTH);
