// Honda CBR600RR: a supersport. A pointed nose with two slanted lamps and
// an air intake between them, a bubble screen, a full fairing over a twin
// spar frame, a humped tank, a stepped seat and a tail that carries the
// silencer under it.
const P = 'paint', PD = 'paint_dark', PL = 'paint_light';
const DEG = Math.PI / 180;
const FW = [4.8, 12.0], RW = [4.8, -10.5];
const ALU = '#A9ADB3', CASE = '#2A2A2C', DARK = '#3A3B3F', GOLD = '#C8A63A', PIPE = '#8A8D92';

// ---- the engine and what hangs on it --------------------------------------------
k.g('engine');
k.box(-2.5, 4.4, -2.0, 2.5, 10.2, 6.2, CASE);
k.box(-2.3, 9.4, 1.4, 2.3, 12.2, 6.0, DARK, {tilt: -18});
k.box(-2.0, 3.2, -1.0, 2.0, 4.4, 5.4, C.BLACK);
k.box(-3.0, 4.8, 0.2, -2.5, 7.6, 3.0, DARK);
k.box(-3.1, 5.5, 0.9, -3.0, 6.9, 2.3, PIPE);
k.box(2.5, 5.0, 1.6, 3.0, 7.4, 4.0, DARK);
// the radiator, leaning forward, and four pipes down the front of the block
k.box(-2.6, 6.6, 7.0, 2.6, 11.6, 7.7, C.BLACK, {tilt: -10});
for (let y = 7.2; y < 11.2; y += 0.9) {
    k.box(-2.3, y, 7.72, 2.3, y + 0.3, 7.82, '#4A4C51', {tilt: -10});
}
for (const x of [-1.9, -0.65, 0.65, 1.9]) {
    k.slab(x - 0.3, x + 0.3, 9.8, 6.6, 4.4, 6.9, 0.6, PIPE, 1);
}
k.box(-1.5, 3.3, -3.6, 1.5, 4.0, 6.9, PIPE);
// the frame: a spar each side from the headstock to the swingarm pivot
k.g('frame');
k.sym(() => {
    k.beam([2.75, 14.6, 8.6], [2.75, 9.4, -2.0], 0.7, 2.6, ALU);
    k.box(2.4, 5.6, -4.0, 3.1, 10.6, -1.8, ALU);
    k.box(3.1, 7.3, -3.4, 3.2, 8.3, -2.4, DARK);
    // a peg for the rider on its plate
    k.box(3.1, 7.4, -4.9, 3.4, 9.4, -4.1, ALU);
    k.box(3.1, 7.4, -5.0, 4.9, 8.0, -4.2, C.BLACK);
    // and one for the passenger, hung from the tail
    k.beam([2.5, 12.6, -8.2], [2.7, 10.2, -9.6], 0.5, 0.6, ALU);
    k.box(2.5, 9.9, -10.0, 4.2, 10.4, -9.3, C.BLACK);
});
k.box(-1.0, 13.6, 7.6, 1.0, 16.0, 9.2, ALU);
// the shock and its spring
k.box(-0.7, 7.0, -4.9, 0.7, 12.2, -3.5, '#E3B21C', {tilt: 14});
k.box(-0.45, 6.2, -4.3, 0.45, 13.0, -3.6, DARK, {tilt: 14});
// the swingarm, the chain on the left and the disc on the right
k.sym(() => k.beam([2.05, 7.4, -2.8], [2.05, RW[0], RW[1]], 0.8, 1.9, ALU));
k.box(-2.4, 6.2, -5.6, 2.4, 7.8, -3.4, ALU);
k.sym(() => k.box(1.7, RW[0] - 0.8, RW[1] - 1.5, 2.6, RW[0] + 0.8, RW[1] + 0.5, DARK));
k.beam([2.75, 7.5, -2.4], [2.75, 6.7, RW[1]], 0.3, 0.5, C.BLACK);
k.beam([2.75, 6.0, -2.4], [2.75, 3.0, RW[1]], 0.3, 0.5, C.BLACK);
k.box(2.65, RW[0] - 1.9, RW[1] - 1.9, 2.85, RW[0] + 1.9, RW[1] + 1.9, PIPE);
k.box(2.65, RW[0] - 1.9, RW[1] - 1.9, 2.85, RW[0] + 1.9, RW[1] + 1.9, PIPE, {tilt: 45});
k.box(-1.95, RW[0] - 2.0, RW[1] - 2.0, -1.75, RW[0] + 2.0, RW[1] + 2.0, PIPE);
k.box(-1.95, RW[0] - 2.0, RW[1] - 2.0, -1.75, RW[0] + 2.0, RW[1] + 2.0, PIPE, {tilt: 45});
k.box(-2.35, 6.0, -9.3, -1.95, 7.0, -8.4, GOLD);
// a hugger over the back tyre
k.loft(-1.9, 1.9, [[-5.9, 8.2], [-7.8, 9.7], [-10.5, 10.3], [-13.0, 9.8]], 0.5, C.BLACK, 1);
// the stand, folded
k.beam([3.0, 4.4, -1.6], [3.3, 3.5, -7.6], 0.5, 0.5, C.BLACK);

// ---- the fairing ---------------------------------------------------------------------
k.g('body');
// the belly pan
k.box(-2.5, 2.9, -2.4, 2.5, 3.5, 6.2, PD);
k.slab(-2.5, 2.5, 2.9, 6.2, 5.0, 7.0, 0.6, PD);
k.sym(() => {
    k.box(2.5, 2.9, -2.4, 3.1, 5.4, 6.4, PD);
    // the side: lower half from behind the front tyre back to the frame,
    // upper half forward over the wheel to the nose
    k.sideGlass({xLow: 3.15, yLow: 5.4, xHigh: 3.15, yHigh: 10.8, rearLow: -1.6, rearHigh: 0.8,
        frontLow: 6.6, frontHigh: 9.6, bands: 7, thick: 0.6, color: P});
    k.sideGlass({xLow: 3.15, yLow: 10.8, xHigh: 3.15, yHigh: 14.4, rearLow: 0.8, rearHigh: 3.4,
        frontLow: 9.6, frontHigh: 13.4, bands: 5, thick: 0.6, color: P});
    k.box(2.85, 10.8, 9.4, 3.45, 14.4, 13.4, P);
    // edges that follow the cut of the panel
    k.beam([3.15, 5.3, -1.7], [3.15, 14.3, 3.2], 0.7, 0.7, P);
    k.beam([3.15, 5.3, 6.5], [3.15, 10.9, 9.7], 0.7, 0.7, P);
    // a white flash and three vents
    k.beam([3.47, 8.0, 3.0], [3.47, 12.6, 11.8], 0.06, 1.3, '#F4F4F2');
    for (let i = 0; i < 3; i++) {
        k.beam([3.47, 6.2 + i * 1.3, 1.2 + i * 0.7], [3.47, 7.0 + i * 1.3, 5.2 + i * 0.7], 0.08, 0.4, C.BLACK);
    }
    k.box(3.45, 3.6, -1.2, 3.5, 4.6, 4.8, C.BLACK);
});
// the nose: a wedge with the lamps in its face
k.box(-2.9, 10.8, 13.4, 2.9, 14.4, 14.6, P);
k.box(-2.3, 11.0, 14.6, 2.3, 14.2, 15.2, P);
k.sym(() => {
    k.wall(10.8, 14.4, 3.45, 13.4, 2.3, 15.2, 0.6, P);
    k.box(0.45, 12.0, 15.2, 2.75, 13.7, 15.3, C.BLACK, {roll: 12});
    k.box(0.7, 12.3, 15.3, 2.5, 13.4, 15.42, C.LAMP, {roll: 12});
    k.box(1.2, 12.55, 15.42, 2.0, 13.15, 15.5, C.LAMP_GLOW, {roll: 12});
});
k.box(-0.4, 11.5, 15.2, 0.4, 13.2, 15.32, C.BLACK);
k.slab(-2.9, 2.9, 14.4, 15.0, 16.5, 12.6, 0.6, P);
k.slab(-2.3, 2.3, 10.8, 15.0, 11.4, 15.3, 0.4, PD);
// the clocks behind the screen
k.box(-1.5, 15.2, 11.6, 1.5, 16.9, 12.6, C.BLACK);
k.box(-1.1, 15.6, 11.5, 1.1, 16.6, 11.6, C.DIAL);
k.box(-0.15, 15.7, 11.44, 0.15, 16.5, 11.5, C.NEEDLE);
// mirrors on stalks, each with an indicator in its face
k.sym(() => {
    k.beam([2.9, 14.9, 12.4], [4.6, 16.3, 12.0], 0.5, 0.5, C.BLACK);
    k.box(4.3, 15.8, 11.5, 6.0, 17.0, 12.3, C.BLACK);
    k.box(4.5, 16.0, 11.42, 5.8, 16.8, 11.5, C.MIRROR);
    k.box(4.7, 16.1, 12.3, 5.7, 16.7, 12.42, C.AMBER);
});

k.g('glass');
k.slab(-2.4, 2.4, 16.3, 12.9, 20.2, 9.8, 0.3, C.GLASS);

// ---- the tank and the seats -------------------------------------------------------
k.g('body');
k.box(-3.0, 12.2, -0.6, 3.0, 15.4, 8.2, P);
k.box(-2.6, 15.4, 0.4, 2.6, 16.6, 7.8, P);
k.box(-2.0, 16.6, 1.4, 2.0, 17.2, 6.8, PL);
k.slab(-2.6, 2.6, 16.9, 1.2, 14.6, -1.2, 0.8, P);
k.slab(-2.0, 2.0, 17.2, 6.6, 15.6, 8.4, 0.8, P);
k.box(-0.8, 17.2, 2.6, 0.8, 17.4, 4.2, C.BLACK);
k.box(-0.5, 17.4, 2.9, 0.5, 17.5, 3.9, C.CHROME);
k.sym(() => k.box(3.0, 12.6, -0.6, 3.06, 14.6, 2.6, PD));
k.box(-2.5, 13.6, -8.6, 2.5, 15.0, -0.6, C.SEAT);
k.box(-2.2, 15.0, -8.4, 2.2, 15.6, -0.9, C.SEAT);
k.box(-2.1, 15.2, -12.5, 2.1, 17.0, -8.6, C.SEAT);
// the tail: a panel each side rising to the back, a hump behind the pillion
k.sym(() => {
    k.beam([2.3, 13.0, -0.8], [2.3, 15.3, -15.6], 0.9, 2.4, P);
    k.beam([2.78, 13.3, -5.0], [2.78, 14.7, -14.0], 0.06, 0.5, '#F4F4F2');
});
k.box(-1.9, 13.4, -16.0, 1.9, 15.4, -8.6, P);
k.slab(-1.9, 1.9, 17.0, -12.5, 17.6, -15.8, 1.6, P);
k.box(-1.9, 15.4, -16.0, 1.9, 16.4, -12.5, P);
k.box(-1.5, 14.2, -16.25, 1.5, 15.9, -16.0, C.BLACK);
k.box(-1.2, 14.5, -16.4, 1.2, 15.6, -16.25, C.TAIL);
k.box(-0.6, 14.8, -16.5, 0.6, 15.3, -16.4, C.TAIL_GLOW);
// under it the tray and the silencer's mouth
k.box(-1.8, 12.4, -15.8, 1.8, 13.4, -6.2, C.BLACK);
k.beam([-1.3, 3.8, -3.2], [-1.3, 12.6, -8.6], 1.2, 1.2, PIPE);
k.box(-1.3, 12.5, -16.6, 1.3, 13.5, -15.8, PIPE);
k.box(-1.0, 12.7, -16.7, 1.0, 13.3, -16.6, C.BLACK);
// the plate on its hanger, an indicator on a stalk each side
k.box(-0.5, 11.4, -17.0, 0.5, 12.5, -15.6, C.BLACK);
k.box(-1.8, 9.6, -17.3, 1.8, 11.8, -17.0, C.BLACK);
k.box(-1.6, 9.8, -17.45, 1.6, 11.6, -17.3, '#F2F2F2');
k.sym(() => {
    k.box(0.5, 11.9, -16.8, 1.9, 12.3, -16.4, C.BLACK);
    k.box(1.9, 11.7, -17.0, 2.9, 12.5, -16.2, C.AMBER);
});

// ---- what turns with the bars -------------------------------------------------------
k.g('steer');
k.sym(() => {
    k.beam([1.6, 9.4, 10.55], [1.6, 16.4, 8.3], 0.9, 0.9, '#8E8F94');
    k.beam([1.6, FW[0], FW[1]], [1.6, 9.6, 10.5], 0.6, 0.6, C.CHROME);
    k.box(1.2, FW[0] - 0.9, FW[1] - 0.8, 2.1, FW[0] + 0.9, FW[1] + 0.8, DARK);
    k.box(1.36, 3.7, 9.5, 1.9, 4.8, 10.3, GOLD);
    k.box(1.22, FW[0] - 2.1, FW[1] - 2.1, 1.36, FW[0] + 2.1, FW[1] + 2.1, PIPE);
    k.box(1.22, FW[0] - 2.1, FW[1] - 2.1, 1.36, FW[0] + 2.1, FW[1] + 2.1, PIPE, {tilt: 45});
    // clip-ons, a grip and a lever
    k.beam([1.8, 15.9, 8.9], [5.3, 15.5, 8.2], 0.7, 0.7, C.BLACK);
    k.box(5.0, 15.1, 7.8, 5.5, 15.9, 8.6, C.CHROME);
    k.beam([2.6, 15.8, 9.8], [5.0, 15.5, 9.3], 0.3, 0.3, C.CHROME);
});
k.box(-2.3, 16.0, 7.6, 2.3, 16.6, 9.2, DARK);
k.box(-0.5, 16.6, 7.9, 0.5, 16.9, 8.9, C.CHROME);
k.box(-2.3, 11.0, 9.2, 2.3, 11.6, 10.6, DARK);
// a mudguard round the tyre
{
    const pts = [];
    for (const a of [50, 75, 100, 125, 150]) {
        pts.push([FW[1] + 5.3 * Math.cos(a * DEG), FW[0] + 5.3 * Math.sin(a * DEG)]);
    }
    k.loft(-1.5, 1.5, pts, 0.5, P, 1);
}
k.sym(() => k.box(1.5, 8.6, 10.2, 1.75, 10.2, 13.2, P));
