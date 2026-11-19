// Cessna 172 Skyhawk: a high-wing four-seater. The wing sits on the cabin
// roof and is held by a strut each side; three wheels in spats; a tail
// cone that tapers straight back to a swept fin; a window behind the rear
// seats; a two-blade propeller.
const P = 'paint', PD = 'paint_dark';
const DEG = Math.PI / 180;
const WHITE = '#F2F2F0', LINE = '#D2D4D6', GREY = '#8C9197', HIDE = '#5A3A26', PANEL = '#2A2A2C';

// ---- the propeller: the spinner first, everything turns about its middle ----------
k.g('propeller');
k.box(-2.6, 15.4, 63.0, 2.6, 20.6, 67.0, P);
k.box(-1.4, 16.6, 67.0, 1.4, 19.4, 69.0, P);
k.box(-1.3, 3.0, 64.0, 1.3, 33.0, 64.8, PANEL);
k.box(-1.35, 3.0, 63.98, 1.35, 5.0, 64.82, '#C42620');
k.box(-1.35, 31.0, 63.98, 1.35, 33.0, 64.82, '#C42620');
k.box(-1.35, 5.6, 63.98, 1.35, 6.2, 64.82, WHITE);
k.box(-1.35, 29.8, 63.98, 1.35, 30.4, 64.82, WHITE);

// ---- the cowl: it carries the cabin's lines forward to the nose --------------------
k.g('body');
{
    const zA = 47.0, zB = 63.0;
    // rear: 12 half wide, 10.2 to 23.2; front: 6.9 half wide, 13.6 to 20.6
    for (const s of [-1, 1]) {
        k.beam([s * 5.1, 22.9, zA], [0, 20.3, zB], 13.8, 0.6, WHITE);
        k.beam([s * 5.1, 10.5, zA], [0, 13.9, zB], 13.8, 0.6, WHITE);
        for (const y of [13.7, 19.7]) {
            k.beam([s * 11.7, y, zA], [s * 6.6, 17.1, zB], 0.6, 7.0, WHITE);
        }
        // the stripe runs on along it
        k.beam([s * 12.05, 16.0, zA], [s * 6.95, 16.6, zB], 0.3, 2.0, P);
        k.beam([s * 12.05, 17.85, zA], [s * 6.95, 18.0, zB], 0.3, 0.5, PD);
    }
    k.box(-6.9, 13.6, 62.4, 6.9, 20.6, 63.0, WHITE);
    k.box(-8.0, 12.0, 47.0, 8.0, 22.0, 62.4, WHITE);
}
k.pair(3.0, 15.4, 63.0, 6.3, 18.6, 63.2, C.BLACK);
k.box(-2.6, 13.9, 63.0, 2.6, 15.0, 63.2, C.BLACK);
k.box(-2.9, 15.0, 62.6, 2.9, 21.0, 63.1, LINE);
k.box(-4.6, 9.4, 50.0, -3.4, 10.8, 56.0, '#5A5A5A');
// a seam where the cowl comes off, and a row of fasteners
k.pair(12.0, 11.0, 46.9, 12.56, 23.0, 47.1, LINE);

// ---- the cabin ---------------------------------------------------------------------
const zW = y => 46.6 - (y - 23.2) * 11.0 / 12.4;     // the windscreen
k.box(-12.5, 9.0, 7.0, 12.5, 10.5, 47.0, WHITE);
k.pair(11.9, 10.5, 7.0, 12.5, 21.0, 47.0, WHITE);
k.box(-11.9, 10.5, 45.0, 11.9, 23.2, 47.0, WHITE);
k.box(-11.9, 10.5, 7.0, 11.9, 30.5, 8.0, WHITE);
k.box(-12.5, 35.5, 7.0, 12.5, 36.0, 36.0, WHITE);
k.sym(() => {
    k.box(11.9, 21.0, 7.0, 12.5, 35.5, 12.5, WHITE);
    k.box(11.9, 21.0, 25.0, 12.5, 35.5, 27.5, WHITE);
    k.box(11.9, 21.0, 37.5, 12.5, 32.6, 38.5, WHITE);
    k.box(11.9, 34.5, 12.5, 12.5, 35.5, 36.0, WHITE);
    k.box(11.9, 21.0, 12.5, 12.5, 21.6, 46.0, WHITE);
    // the post beside the windscreen
    k.slab(11.6, 12.5, 23.2, zW(23.2), 35.6, zW(35.6), 1.1, WHITE);
    k.slab(11.9, 12.5, 21.0, 47.0, 23.2, zW(23.2), 1.1, WHITE);
    // the stripe
    k.box(12.5, 15.0, 7.0, 12.62, 17.0, 47.0, P);
    k.box(12.5, 17.6, 7.0, 12.62, 18.1, 47.0, PD);
    // the door: its seams, a handle, a step on the gear leg below it
    k.box(12.5, 10.8, 27.3, 12.56, 21.0, 27.5, LINE);
    k.box(12.5, 10.8, 37.5, 12.56, 21.0, 37.7, LINE);
    k.box(12.5, 10.8, 27.3, 12.56, 11.0, 37.7, LINE);
    k.box(12.5, 19.0, 28.4, 12.75, 19.7, 30.6, C.CHROME);
    k.box(12.5, 7.0, 27.0, 15.0, 7.6, 30.0, C.CHROME);
    k.box(12.5, 7.6, 28.1, 13.1, 9.4, 28.9, C.CHROME);
});

k.g('glass');
// one pane for the windscreen, one in each door, one behind it, one at the back
k.slab(-11.6, 11.6, 23.2, zW(23.2) - 0.4, 35.6, zW(35.6) - 0.4, 0.3, C.GLASS);
k.sym(() => {
    k.box(12.05, 21.6, 12.5, 12.35, 34.5, 25.0, C.GLASS);
    k.box(12.05, 21.6, 27.5, 12.35, 34.5, 37.5, C.GLASS);
    k.sideGlass({xLow: 12.2, yLow: 21.6, xHigh: 12.2, yHigh: 32.4, rearLow: 38.5, rearHigh: 38.5,
        frontLow: zW(21.6) - 1.0, frontHigh: zW(32.4) - 1.0});
});
k.slab(-10.6, 10.6, 35.6, 8.4, 30.1, -2.2, 0.3, C.GLASS);

k.g('body');
k.sym(() => {
    k.sideGlass({xLow: 11.0, yLow: 30.2, xHigh: 11.0, yHigh: 35.5, rearLow: -2.0, rearHigh: 7.6,
        frontLow: 8.0, frontHigh: 8.0, bands: 5, thick: 0.8, color: WHITE});
    k.beam([11.0, 35.7, 8.4], [11.0, 30.2, -2.2], 1.0, 0.9, WHITE);
});

// ---- the tail cone: four flat faces, each a fan of long planks --------------------
{
    const zA = 7.0, zB = -66.0;
    for (const x of [-9.8, -4.9, 0, 4.9, 9.8]) {
        k.beam([x, 30.2, zA], [0, 26.3, zB], 5.4, 0.6, WHITE);
        k.beam([x, 9.3, zA], [0, 23.1, zB], 5.4, 0.6, WHITE);
    }
    for (const s of [-1, 1]) {
        for (let i = 0; i < 6; i++) {
            k.beam([s * 12.2, 10.9 + i * 3.54, zA], [s * 2.4, 24.7, zB], 0.6, 3.8, WHITE);
        }
        k.beam([s * 12.56, 16.0, zA], [s * 2.76, 24.4, zB], 0.3, 2.0, P);
        k.beam([s * 12.56, 17.85, zA], [s * 2.76, 25.5, zB], 0.3, 0.5, PD);
    }
    k.box(-2.7, 22.8, -66.8, 2.7, 26.6, -65.8, WHITE);
}

// ---- the wing -----------------------------------------------------------------------
k.box(-40.0, 36.0, 8.0, 40.0, 39.5, 35.0, WHITE);
k.sym(() => {
    k.box(40.0, 36.4, 16.0, 86.0, 39.1, 33.0, WHITE);
    // the taper: most of it in the trailing edge
    k.wall(36.4, 39.1, 38.0, 8.0, 86.0, 16.0, 8.0, WHITE, 1);
    k.wall(36.4, 39.1, 38.0, 35.0, 86.0, 33.0, 4.0, WHITE, -1);
    k.box(86.0, 36.3, 15.6, 88.0, 39.2, 33.2, P);
    // flap and aileron seams, a fuel cap
    k.box(13.0, 39.5, 13.0, 40.0, 39.56, 13.3, LINE);
    k.box(40.0, 39.1, 20.6, 80.0, 39.16, 20.9, LINE);
    k.box(39.8, 39.1, 8.6, 40.1, 39.56, 13.3, LINE);
    k.box(23.0, 39.5, 23.0, 25.2, 39.6, 25.2, GREY);
    k.box(13.0, 35.9, 9.0, 40.0, 36.0, 13.0, LINE);
    // the strut, with a cuff at each end
    k.beam([12.3, 11.6, 23.2], [50.0, 36.5, 22.4], 2.2, 1.0, WHITE);
    k.box(48.0, 35.6, 21.0, 52.0, 36.4, 23.8, WHITE);
    k.box(12.5, 10.6, 21.8, 14.2, 12.8, 24.6, WHITE);
});
// navigation lights: red to port, green to starboard; a landing lamp in the port wing
k.box(88.0, 37.0, 29.0, 88.6, 38.4, 32.0, C.TAIL_GLOW);
k.box(-88.6, 37.0, 29.0, -88.0, 38.4, 32.0, C.DISPLAY);
k.box(29.6, 36.7, 35.0, 34.4, 38.8, 35.2, C.BLACK);
k.box(30.0, 37.0, 35.2, 34.0, 38.5, 35.4, C.LAMP_GLOW);
k.beam([52.0, 35.8, 33.0], [52.0, 35.8, 37.6], 0.4, 0.4, C.CHROME);
k.box(51.8, 35.8, 32.0, 52.2, 36.4, 33.0, C.CHROME);
// aerials on the roof
k.slab(-0.25, 0.25, 39.5, 14.0, 44.5, 11.0, 0.7, C.BLACK, 0);
k.box(-6.4, 39.5, 20.0, -5.6, 40.3, 24.0, WHITE);

// ---- the tail ---------------------------------------------------------------------------
k.box(-26.0, 24.2, -67.0, 26.0, 25.4, -55.0, WHITE);
k.box(-4.0, 24.0, -67.0, 4.0, 25.6, -47.0, WHITE);
k.sym(() => {
    k.wall(24.2, 25.4, 2.0, -46.5, 26.0, -54.6, 8.0, WHITE, 1);
    k.box(26.0, 24.1, -67.2, 28.0, 25.5, -54.6, P);
    k.box(4.0, 25.4, -61.3, 26.0, 25.46, -61.0, LINE);
});
// the fin, swept, a fillet running up to it and a painted cap
for (let i = 0; i < 4; i++) {
    k.beam([0, 26.4, -63.7 + i * 4.6], [0, 50.0, -63.7], 1.8, 4.6, WHITE);
}
for (const z of [-63.95, -60.4]) {
    k.beam([0, 44.0, z], [0, 50.0, -63.95], 2.1, 5.1, P);
}
k.box(-0.9, 26.4, -68.0, 0.9, 44.2, -65.6, WHITE);
k.box(-1.05, 44.0, -68.0, 1.05, 50.0, -65.6, P);
k.box(-0.95, 27.5, -65.5, 0.95, 44.0, -65.3, LINE);
for (const [y, z] of [[28.6, -50.5], [31.0, -52.6], [33.6, -54.2]]) {
    k.beam([0, 28.6, -27.0], [0, y, z], 1.2, 2.4, WHITE);
}
k.box(-0.6, 50.0, -65.4, 0.6, 51.0, -63.4, C.TAIL_GLOW);
k.box(-0.5, 22.0, -66.0, 0.5, 22.8, -62.0, C.CHROME);

// ---- the wheels' legs and spats ------------------------------------------------------
k.sym(() => {
    k.beam([8.6, 9.6, 18.5], [19.2, 4.4, 18.5], 2.6, 0.9, WHITE);
    k.box(18.3, 1.5, 15.0, 21.7, 6.8, 22.2, P);
    k.box(18.6, 2.2, 22.2, 21.4, 6.2, 23.6, P);
    k.slab(18.6, 21.4, 6.8, 15.2, 4.6, 11.4, 1.6, P);
    k.box(18.25, 3.9, 14.6, 21.75, 4.3, 22.8, PD);
});
k.g('steer');
k.box(-0.8, 5.5, 51.0, 0.8, 11.6, 53.0, C.CHROME);
k.box(-1.6, 2.0, 49.6, 1.6, 6.6, 55.0, P);
k.slab(-1.3, 1.3, 6.6, 49.8, 4.6, 47.4, 1.4, P);
k.box(-1.3, 2.6, 55.0, 1.3, 6.0, 55.8, P);

// ---- inside ---------------------------------------------------------------------------
k.g('interior');
k.box(-11.9, 10.5, 8.0, 11.9, 10.62, 45.0, C.CARPET);
// the panel under its shade, six dials in front of the pilot, radios in the middle
k.box(-11.9, 16.6, 43.0, 11.9, 23.0, 45.0, PANEL);
k.box(-11.9, 23.0, 41.8, 11.9, 23.6, 46.4, C.BLACK);
for (const x of [2.4, 5.0, 7.6]) {
    for (const y of [17.4, 20.1]) {
        k.box(x, y, 42.82, x + 2.1, y + 2.1, 43.0, '#D8DADC');
        k.box(x + 0.35, y + 0.35, 42.76, x + 1.75, y + 1.75, 42.82, C.BLACK);
    }
}
for (const y of [17.4, 20.1]) {
    k.box(-9.6, y, 42.82, -7.5, y + 2.1, 43.0, '#D8DADC');
    k.box(-9.25, y + 0.35, 42.76, -7.85, y + 1.75, 42.82, C.BLACK);
}
k.box(-1.6, 17.0, 42.8, 1.6, 22.6, 43.0, '#3A3A3E');
k.box(-1.2, 20.6, 42.74, 1.2, 22.0, 42.8, C.DISPLAY);
k.box(-1.2, 18.6, 42.74, 1.2, 19.8, 42.8, '#E0A63A');
k.box(-6.4, 17.6, 42.8, -3.0, 22.0, 43.0, '#3A3A3E');
k.box(-1.0, 13.0, 41.4, 1.0, 16.6, 43.0, PANEL);
k.box(-0.3, 15.4, 40.2, 0.3, 16.0, 41.4, C.BLACK);
k.box(0.6, 15.4, 40.4, 1.2, 16.0, 41.4, '#C42620');
// a yoke for each front seat
k.sym(() => {
    k.box(4.1, 18.0, 40.4, 4.9, 18.8, 43.0, C.BLACK);
    k.box(2.6, 18.0, 39.9, 6.4, 18.8, 40.5, C.BLACK);
    k.box(2.2, 18.0, 39.9, 2.9, 20.4, 40.5, C.BLACK);
    k.box(6.1, 18.0, 39.9, 6.8, 20.4, 40.5, C.BLACK);
    k.box(3.2, 10.6, 42.0, 4.0, 12.6, 42.6, C.CHROME, {tilt: -20});
    k.box(5.0, 10.6, 42.0, 5.8, 12.6, 42.6, C.CHROME, {tilt: -20});
});
// four seats in hide
for (const z of [25.5, 8.6]) {
    k.sym(() => {
        k.box(1.2, 10.5, z, 8.0, 12.4, z + 7.0, HIDE);
        k.box(1.2, 12.2, z - 0.4, 8.0, 23.6, z + 1.6, HIDE, {tilt: 8});
        k.box(2.6, 23.4, z - 1.5, 6.6, 26.6, z - 0.1, HIDE, {tilt: 8});
        k.box(2.2, 12.41, z + 1.4, 7.0, 12.47, z + 6.4, '#6B4730');
    });
}
k.pair(11.6, 10.6, 12.5, 11.9, 20.6, 44.0, '#C9CBCF');
k.box(-11.9, 10.5, 8.0, 11.9, 16.0, 8.2, '#C9CBCF');
