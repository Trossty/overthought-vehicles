// Meyers Manx, 1964: a fibreglass tub on a shortened Beetle floor pan. Big
// round wings stand clear of a low, open body; two lamps ride in pods on
// the nose, a small upright windscreen, two low buckets and a roll hoop,
// and the air-cooled flat four sits out in the open behind.
const P = 'paint', PD = 'paint_dark';
const DEG = Math.PI / 180;
const FW = {y: 5.0, z: 18.0}, RW = {y: 5.8, z: -14.5};
const FLOOR = 4.5, SEAT_TOP = 9.3;
const ALU = '#A9ADB2', TIN = '#2A2A2C', VINYL = '#26262A', VINYL_SEAM = '#3A3A3F';

// the top of the tub's side, back to front: up over the back wing, low by the seats, up over the front
const SIDE = [[-27.0, 11.0], [-22.0, 12.6], [-9.0, 12.6], [-6.0, 10.8], [5.0, 10.4], [9.5, 12.2], [26.0, 12.4],
    [29.0, 10.8]];
const side = z => k.at(SIDE, z);

// a wing: the skin round an arc about the axle, a rolled outer edge, the dark well under it
const wing = (w, r, from, to, x1, x2) => {
    const arc = (rad, a0, a1, n) => {
        const pts = [];
        for (let i = 0; i <= n; i++) {
            const a = (a0 + (a1 - a0) * i / n) * DEG;
            pts.push([w.z - Math.cos(a) * rad, w.y + Math.sin(a) * rad]);
        }
        return pts;
    };
    k.sym(() => {
        k.loft(x1, x2 - 0.9, arc(r, from, to, 10), 0.8, P);
        k.loft(x2 - 1.0, x2, arc(r - 0.5, from + 2, to - 2, 10), 0.8, P);
        k.loft(x1 + 0.4, x2 - 1.2, arc(r - 0.9, from + 4, to - 4, 8), 0.3, C.WELL);
    });
};

// ---- the pan and the tub ------------------------------------------------------------
k.g('body');
k.box(-8.2, FLOOR - 1.0, -12.0, 8.2, FLOOR, 16.0, C.BLACK);
k.box(-1.6, FLOOR, -10.0, 1.6, FLOOR + 1.6, 12.0, TIN);          // the backbone tunnel
k.box(-8.0, FLOOR, -9.0, 8.0, FLOOR + 0.1, 9.5, C.CARPET);
// the sides by the seats, cut low, and a rolled lip along their top
for (const [z1, z2] of [[-9.0, -6.0], [-6.0, 5.0], [5.0, 9.5]]) {
    k.pair(7.6, FLOOR - 0.5, z1, 8.4, Math.min(side(z1), side(z2)), z2, P);
}
k.sym(() => k.loft(7.4, 8.6, [[-9.0, 12.4], [-6.0, 10.8], [5.0, 10.4], [9.5, 12.2]], 0.5, P, 1));
// the sill outside them, between the wings
k.pair(8.4, FLOOR - 0.5, -6.0, 10.6, 6.6, 10.6, P);
k.pair(10.6, FLOOR - 0.5, -6.0, 10.9, 5.6, 10.6, PD);

// ---- the front: a short bonnet that rounds down to the nose ---------------------------
const HOOD = [[9.5, 12.4], [20.0, 12.6], [25.0, 12.1], [27.6, 11.0], [29.4, 9.2], [30.0, 7.0]];
k.loft(-6.6, 6.6, HOOD, 0.8, P);
k.sym(() => {
    k.loft(6.4, 8.2, HOOD.map(([z, y]) => [z, y - 0.2]), 0.8, P);
    k.lean(9.5, 28.2, 8.2, 6.0, 8.2, 12.2, 0.8, P);
});
k.box(-8.2, FLOOR - 0.5, 27.5, 8.2, 7.4, 29.4, P);
k.loft(-7.6, 7.6, [[29.4, 7.4], [30.0, 6.4], [30.0, 5.2], [29.4, 4.0]], 0.8, P);
k.box(-7.4, FLOOR - 0.5, 9.5, 7.4, 11.6, 27.5, PD);                // inside the nose, the tank
// the lamps in pods on the bonnet, and the indicators under the nose
k.sym(() => {
    const x = 5.2, z = 24.2;
    k.box(x - 1.8, 11.4, z - 2.6, x + 1.8, 14.2, z + 1.2, P);
    k.box(x - 1.6, 11.6, z + 1.2, x + 1.6, 14.0, z + 1.5, C.CHROME);
    k.box(x - 1.3, 11.9, z + 1.5, x + 1.3, 13.7, z + 1.7, C.LAMP);
    k.box(x - 0.9, 12.3, z + 1.7, x + 0.9, 13.3, z + 1.8, C.LAMP_GLOW);
    k.box(5.0, 6.0, 29.6, 6.6, 6.8, 29.9, C.AMBER);
});
// the bumper bar: a chrome tube on two struts
k.box(-9.0, 5.6, 31.0, 9.0, 6.6, 32.0, C.CHROME);
k.pair(5.0, 5.7, 29.6, 5.8, 6.5, 31.0, C.CHROME);
k.pair(8.6, 5.6, 29.8, 9.0, 6.6, 31.0, C.CHROME);

// ---- the wings --------------------------------------------------------------------------
wing(FW, 7.6, 8, 172, 8.2, 12.9);
wing(RW, 8.2, 8, 168, 7.4, 13.5);
// where each wing meets the tub, a skin from the wing down to the sill
k.pair(8.2, 6.0, 10.6, 9.0, 11.6, 12.4, P);
k.pair(8.2, 6.0, -8.4, 9.0, 12.6, -6.0, P);
k.pair(8.4, 10.0, -9.0, 9.0, 12.6, -6.0, P);
// the tail lamps on the backs of the rear wings
k.sym(() => {
    k.slab(10.2, 12.2, 9.2, -23.0, 11.0, -22.0, 0.3, C.TAIL, 1);
    k.slab(10.7, 11.7, 9.6, -23.1, 10.6, -22.6, 0.3, C.TAIL_GLOW, 1);
});

// ---- the cockpit -------------------------------------------------------------------------
// the dash, its three dials, and the windscreen in a chrome frame
k.box(-7.6, 9.0, 9.0, 7.6, 12.4, 10.2, VINYL);
k.box(-7.6, 12.0, 8.4, 7.6, 12.6, 10.2, C.BLACK);
for (const x of [3.2, 5.0, 6.8]) {
    k.box(x - 0.7, 10.0, 8.94, x + 0.7, 11.4, 9.0, C.DIAL);
}
k.box(4.9, 10.6, 8.9, 5.1, 11.3, 8.94, C.NEEDLE);
k.box(-1.0, 6.0, 8.6, 1.0, 9.0, 9.4, C.BLACK);
const zW = y => 10.0 - (y - 12.6) * Math.tan(22 * DEG);
k.g('glass');
k.slab(-6.8, 6.8, 12.9, zW(12.9), 18.4, zW(18.4), 0.25, C.GLASS, 0);
k.g('body');
k.sym(() => k.slab(6.8, 7.6, 12.4, zW(12.4), 18.9, zW(18.9), 0.6, C.CHROME, 0));
k.slab(-7.6, 7.6, 18.4, zW(18.4), 19.0, zW(19.0), 0.6, C.CHROME, 0);
k.slab(-7.6, 7.6, 12.4, zW(12.4), 12.9, zW(12.9), 0.6, C.CHROME, 0);
k.mirrors({x: 7.6, y: 16.0, z: zW(16.0) - 0.4, w: 1.6, h: 1.2, d: 0.8, color: C.CHROME});
k.pedals(5.0, 6.8, FLOOR);
// the gear lever and the handbrake on the tunnel
k.beam([0, FLOOR + 1.6, 2.0], [0, 10.6, 0.8], 0.4, 0.4, C.CHROME);
k.box(-0.6, 10.4, 0.3, 0.6, 11.4, 1.3, C.BLACK);
k.box(-0.4, FLOOR + 1.6, -4.0, 0.4, FLOOR + 2.3, 0.0, C.BLACK);
// two low buckets with tall backs, a seam down each
k.sym(() => {
    const x = 5.0;
    k.box(x - 3.0, FLOOR + 0.6, -6.5, x + 3.0, SEAT_TOP - 1.2, 0.5, VINYL);
    k.box(x - 3.2, SEAT_TOP - 1.2, -6.8, x + 3.2, SEAT_TOP, 0.8, VINYL);
    k.box(x - 3.4, SEAT_TOP - 0.8, -6.6, x - 2.6, SEAT_TOP + 0.7, 0.6, VINYL);
    k.box(x + 2.6, SEAT_TOP - 0.8, -6.6, x + 3.4, SEAT_TOP + 0.7, 0.6, VINYL);
    k.slab(x - 3.2, x + 3.2, SEAT_TOP - 0.8, -6.6, 22.6, -9.0, 1.3, VINYL, 1);
    k.slab(x - 0.25, x + 0.25, SEAT_TOP + 0.6, -6.62, 21.4, -8.82, 0.1, VINYL_SEAM, 1);
    k.slab(x - 3.4, x - 2.6, SEAT_TOP + 1.0, -6.8, 18.0, -8.0, 0.6, VINYL, 1);
    k.slab(x + 2.6, x + 3.4, SEAT_TOP + 1.0, -6.8, 18.0, -8.0, 0.6, VINYL, 1);
});
// the bulkhead behind them, and the roll hoop over it
k.box(-7.6, FLOOR - 0.5, -10.2, 7.6, 12.6, -9.0, P);
k.box(-7.6, 12.6, -10.2, 7.6, 13.0, -6.0, PD);
k.sym(() => {
    k.beam([8.0, 12.6, -10.6], [7.6, 23.6, -10.6], 1.0, 1.0, C.CHROME);
    k.beam([7.6, 23.6, -10.6], [6.2, 25.0, -10.6], 1.0, 1.0, C.CHROME);
    k.beam([7.6, 20.0, -11.0], [5.0, 12.8, -17.5], 0.7, 0.7, C.CHROME);
});
k.box(-6.4, 24.5, -11.1, 6.4, 25.5, -10.1, C.CHROME);

// ---- the engine, out in the open --------------------------------------------------------
// the rear deck lip, the frame horns either side and the gearbox in front
k.box(-7.0, FLOOR - 0.5, -12.0, 7.0, 8.0, -10.2, TIN);
k.pair(5.4, 3.6, -26.0, 6.6, 5.2, -12.0, C.DARK_STEEL);
k.box(-2.6, 3.2, -14.5, 2.6, 7.4, -10.2, ALU);
// the crankcase, a cylinder bank and its tin each side, the fan housing on top
k.box(-3.0, 3.6, -22.5, 3.0, 9.2, -14.5, ALU);
k.sym(() => {
    k.box(3.0, 4.6, -22.0, 6.2, 8.6, -15.0, ALU);
    for (let i = 0; i < 4; i++) {
        k.box(3.2, 4.4, -21.6 + i * 1.7, 6.6, 8.8, -21.0 + i * 1.7, '#8E9297');
    }
    k.box(6.2, 4.2, -22.4, 6.8, 9.2, -14.6, TIN);
    k.box(3.0, 8.6, -22.4, 6.8, 9.4, -14.6, TIN);
});
k.box(-5.6, 9.2, -21.0, 5.6, 13.6, -16.4, TIN);
k.box(-3.4, 13.6, -20.4, 3.4, 14.6, -17.0, TIN);
k.box(-2.4, 9.6, -22.8, 2.4, 13.0, -21.0, TIN);
k.disc(-0.4, 0.4, 11.3, -22.9, 1.6, C.BLACK);
// the alternator, the carburettor and the bright air cleaner on it
k.box(-1.4, 14.6, -19.4, 1.4, 16.0, -18.0, C.BLACK);
k.box(-0.6, 9.2, -15.6, 0.6, 15.0, -14.8, ALU);
k.box(-1.0, 14.8, -16.2, 1.0, 16.4, -14.2, C.DARK_STEEL);
k.disc(-2.4, 2.4, 17.4, -15.2, 2.4, C.CHROME);
k.box(-2.5, 16.8, -16.0, 2.5, 18.0, -14.4, C.CHROME);
// the oil cooler, the coil, the plugs' leads
k.box(-6.0, 9.4, -14.8, -4.0, 12.0, -13.4, C.BLACK);
k.box(4.4, 9.4, -15.2, 5.4, 11.6, -14.2, C.BLACK);
// the heat exchangers under it, and the twin pipes out the back
k.box(-6.0, 3.0, -24.4, 6.0, 4.2, -22.8, C.EXHAUST);
k.pair(2.6, 3.4, -28.4, 3.6, 4.4, -24.4, C.EXHAUST);
k.pair(2.7, 3.5, -28.5, 3.5, 4.3, -28.4, C.BLACK);
// the nerf bar behind it all, and the plate on the bulkhead
k.box(-8.6, 5.4, -27.4, 8.6, 6.4, -26.4, C.CHROME);
k.pair(6.4, 5.5, -26.4, 7.2, 6.3, -24.0, C.CHROME);
k.plate(-2.6, 9.2, 2.6, 11.0, -12.0, false);

// ---- the steering column: on from the game's wheel down through the top of the dash ---------------
k.g('body');
const col = k.column();
k.beam(col.end, col.end.map((v, i) => v + col.dir[i] * 3.0), 0.9, 0.9, C.BLACK);
