// Toyota Hilux double cab, the seventh generation (2005-15): a four-door
// cab on a ladder frame with an open bed behind. A chrome-barred grille
// between swept-back lamps, a scoop on the bonnet, wings that stand out
// over the wheels, tall lamps at the bed's corners, a chrome step bumper.
const P = 'paint', PD = 'paint_dark';
const H = 16.0, FLOOR = 9.0, BELT = 19.5, TOP = 31.7;
const FZ = 29.5, RZ = -29.0, WY = 7.02, AR = 8.0;
const NOSE = 46.6, CAB = -10.8, BED = -50.3, LID = 13.0, RAIL = 20.0;

const BON = [[46.8, 17.5], [42, 18.4], [34, 19.05], [23.5, 19.5]];
const FEN = BON.map(([z, y]) => [z, y - 0.1]).concat([[21.8, 19.45]]);

const sx = y => 15.3 - (y - BELT) * 1.0 / 12.2;
const nS = [12.2, 1.0, 0];
const zW = y => 21.8 - (y - 19.6) * 7.6 / 12.1;     // windscreen
const zR = y => -10.3 + (y - BELT) * 1.2 / 12.2;    // the cab's back
const zC = y => -8.0 + (y - BELT) * 1.6 / 12.2;     // the back edge of the rear door's glass

// ---- the cab's floor and sides ---------------------------------------------------
k.g('body');
k.box(-H, 7.5, CAB, H, FLOOR, FZ - AR, PD);
k.pair(14.9, FLOOR, CAB, H, BELT, FZ - AR, P);
k.box(-14.9, FLOOR, CAB, 14.9, BELT, CAB + 1.1, P);
for (const z of [19.9, 5.4]) {
    k.pair(H, 9.5, z - 0.1, H + 0.08, BELT - 0.3, z + 0.1, PD);
}
k.pair(H, 7.6, CAB - 0.15, H + 0.08, RAIL, CAB + 0.15, PD);
k.pair(15.25, 19.35, -8.2, H, 19.62, 21.6, C.BLACK);
k.handles(H, 16.5, 7.6);
k.handles(H, 16.5, -7.6);
k.pair(H, 7.5, -9.7, 17.7, 8.6, 21.0, C.BLACK);
k.pair(H + 0.2, 8.6, -9.2, 17.5, 8.72, 20.5, C.STEEL);
k.mirrors({x: H - 0.1, y: 19.9, z: 17.4, w: 2.4, h: 2.6, d: 1.4, color: C.CHROME});

// ---- the front end -------------------------------------------------------------
k.arch({z: FZ, y: WY, r: AR, half: H, inner: 11.0, floor: 7.0, top: z => k.at(FEN, z) - 0.3, n: 8, flare: 0.6,
    lipThick: 0.9});
k.box(-H, 7.0, FZ + AR, H, 17.0, NOSE, P);
k.loft(LID + 0.05, H, FEN, 1.3, P);
k.loft(-H, -LID - 0.05, FEN, 1.3, P);
// the shut line down each side of the bonnet
k.loft(LID + 0.05, LID + 0.3, FEN, 0.04, PD, 1);
k.loft(-LID - 0.3, -LID - 0.05, FEN, 0.04, PD, 1);
k.box(-LID, 18.8, 21.8, LID, 19.45, 23.5, C.BLACK);
k.wipers({y: 19.55, z: 22.8});
k.box(-H, FLOOR, FZ - AR, H, 18.8, FZ - AR + 0.5, P);

// the face: a chrome-barred grille, lamps swept round the corners
k.box(-8.8, 11.6, NOSE, 8.8, 17.0, NOSE + 0.15, C.BLACK);
k.box(-9.0, 16.2, NOSE, 9.0, 17.1, NOSE + 0.45, C.CHROME);
k.box(-8.4, 14.4, NOSE + 0.1, 8.4, 15.0, NOSE + 0.4, C.CHROME);
k.box(-7.8, 12.8, NOSE + 0.1, 7.8, 13.4, NOSE + 0.4, C.CHROME);
k.box(-7.4, 11.4, NOSE, 7.4, 11.7, NOSE + 0.4, C.CHROME);
k.pair(8.8, 11.6, NOSE, 9.0, 16.2, NOSE + 0.4, C.CHROME);
k.box(-1.6, 13.5, NOSE + 0.4, 1.6, 15.5, NOSE + 0.55, C.CHROME);
k.box(-0.9, 14.0, NOSE + 0.55, 0.9, 15.0, NOSE + 0.62, '#2A2C30');
k.sym(() => {
    k.box(9.2, 13.9, NOSE - 0.1, H + 0.12, 17.1, NOSE + 0.3, C.BLACK);
    k.box(9.45, 14.15, NOSE + 0.3, 13.6, 16.85, NOSE + 0.5, C.LAMP);
    // two projectors behind the glass
    k.discZ(10.55, 15.5, 0.72, NOSE + 0.3, NOSE + 0.6, C.LAMP_GLOW);
    k.discZ(12.45, 15.5, 0.72, NOSE + 0.3, NOSE + 0.6, C.LAMP_GLOW);
    k.box(H, 13.9, 42.4, H + 0.12, 17.1, NOSE, C.BLACK);
    k.box(13.6, 14.15, 42.8, H + 0.24, 16.85, NOSE + 0.5, C.AMBER);
    // the lamp runs back along the top of the wing, rising and narrowing as it goes
    k.box(H, 14.8, 41.0, H + 0.12, 17.4, 42.4, C.BLACK);
    k.box(H, 15.8, 39.6, H + 0.12, 17.7, 41.0, C.BLACK);
    k.box(H, 15.05, 40.95, H + 0.24, 17.15, 42.8, C.LAMP);
    k.box(H, 16.05, 39.85, H + 0.24, 17.45, 41.05, C.LAMP);
});
k.bumper({front: true, z: 48.2, depth: 2.2, y1: 6.6, y2: 11.2, half: H + 0.25, cut: 2.4, wrap: FZ + AR + 0.6,
    color: P, strip: false});
k.box(-7.2, 7.4, 48.2, 7.2, 9.8, 48.32, C.BLACK);
k.plate(-3.4, 7.6, 3.4, 9.6, 48.3, true);
k.sym(() => {
    k.box(10.2, 7.7, 48.2, 13.4, 9.7, 48.32, C.BLACK);
    k.box(10.8, 8.1, 48.3, 12.8, 9.3, 48.42, '#D9D6C2');
});
k.box(-6.4, 6.55, 45.6, 6.4, 7.2, 48.3, C.BUMPER);

k.g('bonnet');
k.loft(-LID, LID, BON, 0.6, P);
k.box(-4.6, 18.9, 27.6, 4.6, 20.0, 35.4, P);
k.box(-4.0, 19.3, 35.4, 4.0, 19.85, 35.52, C.BLACK);

// ---- the bed ---------------------------------------------------------------------
k.g('body');
k.arch({z: RZ, y: WY, r: AR, half: H, inner: 11.0, floor: 7.0, top: RAIL, n: 8, flare: 0.6, lipThick: 0.9,
    open: true, skinIn: 14.9, hollow: true, tubColor: C.TRIM});
k.box(-H, 7.0, RZ + AR, H, 12.0, CAB, P);
k.box(-H, 7.0, BED, H, 12.0, RZ - AR, P);
k.box(-10.4, 8.0, RZ - AR, 10.4, 12.0, RZ + AR, PD);
k.pair(14.9, 12.0, RZ + AR, H, RAIL, CAB, P);
k.pair(14.9, 12.0, BED, H, RAIL, RZ - AR, P);
k.box(-14.9, 12.0, -49.8, 14.9, 13.0, CAB, C.TRIM);
for (let z = -46; z < -12; z += 4.2) {
    k.box(-10.2, 13.0, z, 10.2, 13.04, z + 0.5, '#232326');
}
k.pair(14.8, RAIL, BED - 0.6, 16.1, 20.5, CAB, C.BLACK);
k.box(-14.9, 19.4, CAB - 0.5, 14.9, 20.5, CAB, C.BLACK);
k.box(H, 16.4, -34.7, H + 0.08, 18.0, -33.0, PD);
// the tailgate, its handle and the maker's name
k.box(-14.9, 12.0, BED - 0.6, 14.9, RAIL, -49.8, P);
k.box(-14.9, RAIL, BED - 0.6, 14.9, 20.5, -49.8, C.BLACK);
k.box(-2.4, 17.9, BED - 0.85, 2.4, 18.9, BED - 0.6, C.BLACK);
for (let i = 0; i < 6; i++) {
    k.box(-6.9 + i * 2.4, 14.6, BED - 0.68, -5.3 + i * 2.4, 16.4, BED - 0.6, PD);
}
k.sym(() => {
    k.box(14.1, 13.3, BED - 0.9, H + 0.12, 19.7, BED + 0.2, C.BLACK);
    k.box(14.4, 17.2, BED - 1.05, H - 0.15, 19.4, BED - 0.9, C.TAIL);
    k.box(14.7, 17.7, BED - 1.12, H - 0.45, 18.9, BED - 1.05, C.TAIL_GLOW);
    k.box(14.4, 15.4, BED - 1.05, H - 0.15, 17.2, BED - 0.9, C.AMBER);
    k.box(14.4, 13.6, BED - 1.05, H - 0.15, 15.4, BED - 0.9, C.REVERSE);
});
k.bumper({front: false, z: -52.6, depth: 2.3, y1: 6.6, y2: 10.0, half: H + 0.25, cut: 1.8, wrap: RZ - AR - 0.6,
    color: C.CHROME, strip: false});
k.box(-9.0, 10.0, -52.5, 9.0, 10.25, BED - 0.6, C.BLACK);
k.plate(-3.4, 7.3, 3.4, 9.3, -52.6, false);
k.box(4.9, 6.6, -50.6, 6.0, 7.5, -44.0, C.EXHAUST);
k.box(5.1, 6.8, -50.66, 5.8, 7.3, -50.6, C.BLACK);

// ---- the cab above the waist ------------------------------------------------------
k.g('glass');
k.slab(-13.9, 13.9, 19.6, 21.8, (TOP - 0.5), zW((TOP - 0.5)), 0.3, C.GLASS);
k.slab(-12.6, 12.6, 20.6, zR(20.6) + 0.4, 30.6, zR(30.6) + 0.4, 0.3, C.GLASS);
k.sideGlass({xLow: sx(BELT) - 0.15, yLow: BELT, xHigh: sx(TOP) - 0.15, yHigh: TOP,
    rearLow: zC(BELT), rearHigh: zC(TOP), frontLow: zW(BELT) - 0.5, frontHigh: zW(TOP) - 0.5, bands: 10});

k.g('body');
k.planks([sx(BELT), BELT, zW(BELT) - 0.85], [sx((TOP - 0.7)), (TOP - 0.7), zW((TOP - 0.7)) - 0.85], nS, 1.9, 1.5, P);
k.planks([sx(BELT) + 0.05, BELT, 5.4], [sx((TOP - 0.7)) + 0.05, (TOP - 0.7), 5.4], nS, 1.9, 0.5, C.BLACK, 0);
k.sideGlass({xLow: sx(BELT), yLow: BELT, xHigh: sx(TOP), yHigh: TOP, rearLow: zR(BELT), rearHigh: zR(TOP),
    frontLow: zC(BELT), frontHigh: zC(TOP), bands: 6, thick: 0.9, color: P, off: -1});
k.planks([sx(BELT), BELT, zC(BELT)], [sx((TOP - 0.7)), (TOP - 0.7), zC((TOP - 0.7))], nS, 0.9, 1.0, C.BLACK, 0);
// the back of the cab round its window
k.slab(-sx(BELT), sx(BELT), BELT, zR(BELT) + 1.0, 20.6, zR(20.6) + 1.0, 1.0, P);
k.slab(-sx(TOP), sx(TOP), 30.6, zR(30.6) + 1.0, TOP, zR(TOP) + 1.0, 1.0, P);
k.slabs(12.6, sx(25.6), 20.6, zR(20.6) + 1.0, 30.6, zR(30.6) + 1.0, 1.0, P);

const ROOF = [[zW(TOP) + 0.6, 31.8], [8.0, 32.5], [-4.0, 32.6], [zR(TOP) + 0.9, 32.0]];
const EDGE = ROOF.map(([z, y]) => [z, y - 0.3]);
k.loft(-12.8, 12.8, ROOF, 0.9, P);
k.loft(12.8, 14.25, EDGE, 0.9, P);
k.loft(-14.25, -12.8, EDGE, 0.9, P);

// ---- inside -------------------------------------------------------------------------
k.g('interior');
k.box(-14.9, FLOOR, CAB + 1.1, 14.9, FLOOR + 0.15, FZ - AR, C.CARPET);
k.frontSeats({floor: FLOOR, top: 11.2, back: 5.0, front: 14.6, backTop: 24.5, x: 7.25, w: 4.25});
k.rearBench({half: 13.9, floor: FLOOR, top: 11.2, back: -9.2, front: 0.0, backTop: 21.2, places: 2, rake: 3,
    headrest: true});
k.dash({half: 14.9, back: 17.6, front: FZ - AR, bottom: 16.0, top: 19.4, reach: 21.8, driver: 6.8});
k.console({half: 2.1, floor: FLOOR, top: 11.4, back: 6.5, front: 17.2, lever: 12.7, brake: 8.2});
k.pedals(6.8, 18.8, FLOOR);
k.doorCards({x: 14.9, floor: FLOOR, top: 19.2, back: CAB + 1.1, front: FZ - AR - 0.5, arm: 15.4,
    doors: [[-9.0, 4.8], [6.0, 19.4]]});
k.pair(11.0, FLOOR, 20.2, 14.9, 16.0, FZ - AR, C.CARPET);
k.box(-1.6, 28.6, 15.2, 1.6, 29.8, 15.5, C.BLACK);
k.box(-1.4, 28.75, 15.12, 1.4, 29.65, 15.2, C.MIRROR);
