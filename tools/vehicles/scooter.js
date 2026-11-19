// Vespa Primavera: a pressed-steel scooter. A leg shield with the tie down
// its middle, a flat floor with rubber strips, round cowls over the engine
// and the back wheel, a long saddle, and the lamp in the handlebars.
const P = 'paint', PD = 'paint_dark';
const DEG = Math.PI / 180;
const FW = [3.6, 11.0], RW = [3.6, -10.0];
const SADDLE = '#5A3A26', SADDLE_LIGHT = '#6B4730', CASE = '#2A2A2C';

// ---- the leg shield ---------------------------------------------------------------------
k.g('body');
const LEAN = 10;                                   // it leans back as it rises
const zS = y => 11.9 - (y - 5.0) * Math.tan(LEAN * DEG);
k.slab(-2.0, 2.0, 8.7, zS(8.7), 17.4, zS(17.4), 0.7, P);
k.sym(() => {
    // each wing swept back from the middle
    k.wall(5.0, 17.4, 1.9, 10.77, 4.3, 9.4, 0.7, P, -1, {tilt: LEAN});
    k.wall(5.4, 16.8, 4.3, 9.4, 4.3, 8.8, 0.7, P, -1, {tilt: LEAN});
    // an indicator high on the wing
    k.wall(12.6, 14.2, 2.6, 10.03, 3.7, 9.4, 0.2, C.AMBER, 1, {tilt: LEAN});
});
// the tie, with the horn behind its slots and a crest at the top
k.slab(-0.8, 0.8, 7.4, zS(7.4) + 0.02, 16.6, zS(16.6) + 0.02, 0.3, P, 1);
k.slab(-0.2, 0.2, 9.6, zS(9.6) + 0.3, 15.0, zS(15.0) + 0.3, 0.12, C.CHROME, 1);
for (const y of [7.9, 8.5]) {
    k.slab(-0.55, 0.55, y, zS(y) + 0.3, y + 0.3, zS(y + 0.3) + 0.3, 0.08, C.BLACK, 1);
}
k.slab(-0.45, 0.45, 15.4, zS(15.4) + 0.3, 16.3, zS(16.3) + 0.3, 0.1, '#2B4C8C', 1);
// inside it the glovebox, and the column up to the bars
k.slab(-3.4, 3.4, 9.4, zS(9.4) - 1.6, 16.2, zS(16.2) - 1.6, 1.0, PD, 0);
k.box(-0.35, 13.0, zS(13.0) - 2.3, 0.35, 13.7, zS(13.0) - 2.1, C.CHROME);
k.box(-1.5, 16.6, 8.5, 1.5, 18.0, 10.0, P);

// ---- the floor ---------------------------------------------------------------------------
k.box(-3.6, 3.4, -2.5, 3.6, 4.6, 9.6, P);
k.sym(() => k.slab(1.9, 3.6, 4.6, 9.4, 6.4, 10.6, 0.9, P));
k.box(-1.0, 4.6, -2.5, 1.0, 5.0, 8.8, P);
for (const x of [1.4, 2.5]) {
    k.pair(x, 4.6, -1.6, x + 0.6, 4.8, 8.6, C.BLACK);
}
k.box(-2.6, 2.6, -2.0, 2.6, 3.4, 8.2, CASE);
k.sym(() => k.box(3.6, 3.6, -2.0, 3.72, 4.2, 9.0, C.CHROME));

// ---- the body over the engine ------------------------------------------------------
// a column under the saddle, a deck over the wheel, a cowl each side
k.box(-3.6, 4.6, -5.6, 3.6, 11.4, -2.5, P);
k.box(-3.4, 7.6, -15.0, 3.4, 11.4, -5.6, P);
k.box(-2.5, 7.6, -16.3, 2.5, 10.9, -15.0, P);
k.sym(() => {
    k.box(3.4, 5.4, -13.2, 4.5, 10.3, -5.6, P);
    k.wall(5.4, 10.3, 4.5, -5.6, 3.6, -2.6, 1.1, P);
    k.wall(5.6, 10.3, 4.5, -13.2, 2.5, -16.3, 1.1, P);
    k.lean(-13.2, -5.6, 4.5, 10.3, 3.4, 11.4, 0.9, P);
    k.lean(-13.2, -5.6, 4.5, 5.4, 3.7, 4.6, 0.8, P);
    // the dark of the arch behind the cowl
    k.box(1.4, 5.0, -14.2, 3.4, 7.6, -5.6, C.WELL);
    // a bright strip along the cowl
    k.box(4.5, 6.4, -12.6, 4.62, 6.8, -6.0, C.CHROME);
    k.wall(8.6, 9.5, 3.73, -14.4, 3.05, -15.45, 0.2, C.AMBER, 1);
});
// cooling slots in the left cowl
for (let i = 0; i < 4; i++) {
    k.box(4.5, 7.6, -11.4 + i * 1.0, 4.58, 9.4, -11.0 + i * 1.0, C.BLACK);
}
// the lamp in the tail, the plate on a flap below it
k.box(-1.5, 8.4, -16.5, 1.5, 10.4, -16.3, C.CHROME);
k.box(-1.2, 8.7, -16.62, 1.2, 10.1, -16.5, C.TAIL);
k.box(-0.6, 9.1, -16.7, 0.6, 9.7, -16.62, C.TAIL_GLOW);
k.box(-1.9, 5.2, -16.6, 1.9, 8.2, -16.3, C.BLACK);
k.box(-1.7, 5.5, -16.75, 1.7, 7.7, -16.6, '#E8E8E8');
// the engine and its belt case on one side, the silencer on the other
k.box(-3.9, 2.4, -9.6, -1.6, 5.4, -3.6, CASE);
k.box(-4.0, 3.0, -8.8, -3.9, 4.8, -4.4, '#3A3B3F');
k.box(-4.06, 3.6, -7.6, -4.0, 4.2, -5.6, C.CHROME);
k.box(2.2, 2.6, -14.6, 3.8, 4.2, -8.0, '#3A3B3F');
k.box(3.8, 2.9, -13.6, 3.92, 3.9, -9.0, C.CHROME);
k.box(2.6, 3.0, -15.0, 3.4, 3.8, -14.6, C.BLACK);

// ---- the saddle -------------------------------------------------------------------------
k.box(-3.0, 11.4, -14.2, 3.0, 12.3, -1.8, SADDLE);
k.box(-2.7, 12.3, -13.9, 2.7, 13.4, -2.6, SADDLE);
k.slab(-2.4, 2.4, 13.4, -2.8, 12.0, -1.5, 0.9, SADDLE);
k.box(-2.5, 13.4, -13.7, 2.5, 13.8, -8.4, SADDLE_LIGHT);
k.box(-3.05, 11.9, -14.25, 3.05, 12.1, -1.75, SADDLE_LIGHT);
// a bright rail round the back of it to hold on to
k.box(-3.3, 11.4, -15.4, 3.3, 11.9, -14.2, C.CHROME);
k.sym(() => k.beam([3.1, 11.8, -14.4], [3.1, 13.0, -15.4], 0.4, 0.4, C.CHROME));
k.box(-3.3, 12.8, -15.7, 3.3, 13.2, -15.2, C.CHROME);

// ---- what turns with the bars -------------------------------------------------------
k.g('steer');
// the column and the arm that carries the wheel on one side
k.beam([0, 7.6, 10.6], [0, 17.8, 9.0], 1.2, 1.2, CASE);
k.beam([-1.55, 7.4, 9.6], [-1.55, FW[0], FW[1]], 0.6, 0.9, C.CHROME);
k.beam([-1.9, 7.2, 12.2], [-1.9, 4.0, 11.6], 0.5, 0.5, '#B5221E');
k.box(-2.0, FW[0] - 0.7, FW[1] - 0.7, -1.1, FW[0] + 0.7, FW[1] + 0.7, C.CHROME);
// the mudguard: deep sides, a round top and a crest on it
{
    const pts = [];
    for (const a of [38, 62, 90, 118, 150]) {
        pts.push([FW[1] + 4.1 * Math.cos(a * DEG), FW[0] + 4.1 * Math.sin(a * DEG)]);
    }
    k.loft(-1.8, 1.8, pts, 0.6, P, 1);
}
k.sym(() => k.box(1.3, 5.4, 8.2, 1.8, 7.6, 13.6, P));
k.box(-0.2, 7.9, 10.4, 0.2, 8.4, 12.8, C.CHROME);
// the headset: the lamp in front, the clock on top, a grip each side
k.box(-2.6, 17.8, 8.5, 2.6, 20.2, 11.2, P);
k.box(-1.9, 20.2, 8.8, 1.9, 20.6, 10.9, P);
k.sym(() => {
    k.box(2.6, 18.2, 8.8, 3.6, 19.8, 10.8, P);
    k.box(3.6, 18.6, 9.4, 6.2, 19.4, 10.2, C.BLACK);
    k.box(6.2, 18.5, 9.3, 6.5, 19.5, 10.3, C.CHROME);
    k.beam([3.7, 19.1, 10.9], [6.0, 18.9, 11.3], 0.3, 0.3, C.CHROME);
    // a mirror on a stalk
    k.beam([4.4, 19.4, 9.8], [4.9, 22.6, 9.8], 0.35, 0.35, C.CHROME);
    k.box(4.0, 22.4, 9.5, 6.0, 23.6, 10.1, C.CHROME);
    k.box(4.5, 22.0, 9.5, 5.5, 24.0, 10.1, C.CHROME);
    k.box(4.2, 22.6, 9.42, 5.8, 23.4, 9.5, C.MIRROR);
});
k.box(-1.7, 18.2, 11.2, 1.7, 20.2, 11.45, C.CHROME);
k.box(-1.2, 17.8, 11.2, 1.2, 20.6, 11.45, C.CHROME);
k.box(-1.3, 18.5, 11.45, 1.3, 19.9, 11.6, C.LAMP);
k.box(-0.9, 18.1, 11.45, 0.9, 20.3, 11.6, C.LAMP);
k.box(-0.7, 18.7, 11.6, 0.7, 19.7, 11.7, C.LAMP_GLOW);
k.box(-1.2, 20.6, 9.0, 1.2, 20.8, 10.5, C.BLACK);
k.box(-0.9, 20.8, 9.2, 0.9, 20.86, 10.3, C.DIAL);
k.box(-0.1, 20.86, 9.5, 0.1, 20.92, 10.2, C.NEEDLE);
