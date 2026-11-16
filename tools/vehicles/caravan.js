// A small touring caravan in the Eriba mould: a rounded shell on one axle,
// white over a band of colour, a pop-top in the roof, the door on the kerb
// side, a locker on the drawbar.
const P = 'paint', PD = 'paint_dark';
const WHITE = '#F2F0EA', LOWER = '#D9D7D0', ROOF = '#E3E1DA', RUBBER = '#1B1B1D';
const DARK = '#2E3338', CURTAIN = '#C9B899', GALV = C.BUMPER;
const X = 16.0, Y0 = 10.0, TOP = 41.0;

// the shell seen from the side: the nose and the tail, floor to roof
const NOSE = [[23.6, 10], [28.0, 19.5], [28.0, 30.5], [26.6, 35.2], [23.4, 38.6], [18.0, TOP]];
const TAIL = [[-28.4, 10], [-32.0, 18.5], [-32.0, 31.0], [-30.8, 35.4], [-27.8, 38.8], [-22.5, TOP]];
const along = (pts, y) => {
    for (let i = 1; i < pts.length; i++) {
        if (y <= pts[i][1] + 1e-6) {
            const [z0, y0] = pts[i - 1], [z1, y1] = pts[i];
            return z0 + (z1 - z0) * (y - y0) / (y1 - y0);
        }
    }
    return pts[pts.length - 1][0];
};

// ---- the shell --------------------------------------------------------------------------
k.g('body');
const colourAt = y => y < 17.5 ? LOWER : y < 22 ? P : WHITE;
const CUTS = [10, 12, 14, 16, 17.5, 19, 20.5, 22, 24, 27, 30.5, 32, 33.6, 35.2, 36.4, 37.6, 38.6, 39.4, 40.2, TOP];
for (let i = 0; i + 1 < CUTS.length; i++) {
    const a = CUTS[i], b = CUTS[i + 1];
    const front = Math.min(along(NOSE, a), along(NOSE, b)) - 0.05;
    const back = Math.max(along(TAIL, a), along(TAIL, b)) + 0.05;
    k.box(-X, a, back, X, b, front, colourAt((a + b) / 2));
}
// the skin over each end, so the steps behind it never show
function skin(pts, rear) {
    for (let i = 0; i + 1 < pts.length; i++) {
        const [zA, yA] = pts[i], [zB, yB] = pts[i + 1];
        const cuts = [yA];
        for (const y of [17.5, 22]) {
            if (y > yA + 0.01 && y < yB - 0.01) {
                cuts.push(y);
            }
        }
        cuts.push(yB);
        for (let j = 0; j + 1 < cuts.length; j++) {
            const z0 = zA + (zB - zA) * (cuts[j] - yA) / (yB - yA);
            const z1 = zA + (zB - zA) * (cuts[j + 1] - yA) / (yB - yA);
            // the thickness goes inwards: back from the nose, forward from the tail
            const steep = Math.abs(yB - yA) >= Math.abs(zB - zA);
            const outIsUp = rear ? zB > zA : zB < zA;
            const off = steep && Math.abs(zB - zA) < 1e-6 ? (rear ? 1 : -1) : outIsUp ? -1 : 1;
            k.slab(-X, X, cuts[j], z0, cuts[j + 1], z1, 1.7, colourAt((cuts[j] + cuts[j + 1]) / 2), off);
        }
    }
}
skin(NOSE, false);
skin(TAIL, true);
k.box(-X, 9.0, -28.0, X, Y0, 23.4, C.TRIM);
// a pinstripe over the band, the awning rail under the roof
k.pair(X, 22.6, -31.6, X + 0.12, 23.2, 27.6, PD);
k.pair(X, 39.2, -26.0, X + 0.2, 39.6, 21.0, C.CHROME);
// the pop-top and a vent in it
k.box(-13.4, TOP, -19.0, 13.4, 42.3, 14.6, ROOF);
k.box(-13.8, 42.3, -19.4, 13.8, 43.0, 15.0, WHITE);
k.box(-13.5, TOP + 0.2, -19.1, 13.5, TOP + 0.6, 14.7, RUBBER);
k.box(-3.6, 43.0, -6.0, 3.6, 43.4, 0.6, ROOF);
for (const z of [-17.0, 12.6]) {
    k.pair(13.8, 41.6, z - 0.5, 14.2, 42.6, z + 0.5, C.CHROME);
}

// ---- the windows: one pane each, a curtain drawn back behind it ---------------
function window(x1, x2, y1, y2, z1, z2, s) {
    k.g('body');
    k.box(s * X, y1 - 0.5, z1 - 0.5, s * (X + 0.2), y2 + 0.5, z2 + 0.5, RUBBER);
    k.box(s * (X + 0.2), y1, z1, s * (X + 0.26), y2, z2, DARK);
    for (const z of [z1, z2 - 1.6]) {
        k.box(s * (X + 0.26), y1, z, s * (X + 0.3), y2, z + 1.6, CURTAIN);
    }
    k.g('glass');
    k.box(s * (X + 0.3), y1, z1, s * (X + 0.55), y2, z2, C.GLASS);
}
window(0, 0, 26.5, 35.0, -25.5, -10.0, 1);
window(0, 0, 26.5, 35.0, 5.0, 20.0, 1);
window(0, 0, 26.5, 35.0, -26.0, -12.0, -1);
window(0, 0, 26.5, 35.0, 9.0, 21.0, -1);
// the big one in the nose, a smaller one in the tail
k.g('body');
k.box(-12.6, 24.5, 28.0, 12.6, 30.5, 28.2, RUBBER);
k.box(-12.1, 25.0, 28.2, 12.1, 30.0, 28.26, DARK);
k.pair(8.6, 25.0, 28.26, 12.1, 30.0, 28.3, CURTAIN);
k.box(-9.6, 24.5, -32.2, 9.6, 30.5, -32.0, RUBBER);
k.box(-9.1, 25.0, -32.26, 9.1, 30.0, -32.2, DARK);
k.pair(6.0, 25.0, -32.3, 9.1, 30.0, -32.26, CURTAIN);
k.g('glass');
k.box(-12.1, 25.0, 28.3, 12.1, 30.0, 28.55, C.GLASS);
k.box(-9.1, 25.0, -32.55, 9.1, 30.0, -32.3, C.GLASS);

// ---- the door, on the kerb side -------------------------------------------------------
k.g('body');
k.box(-X - 0.35, 10.8, -6.2, -X, 38.2, 3.2, RUBBER);
k.box(-X - 0.5, 11.2, -5.8, -X - 0.35, 17.5, 2.8, LOWER);
k.box(-X - 0.5, 17.5, -5.8, -X - 0.35, 22.0, 2.8, P);
k.box(-X - 0.5, 22.0, -5.8, -X - 0.35, 37.8, 2.8, WHITE);
k.box(-X - 0.56, 28.0, -4.6, -X - 0.5, 35.4, 1.6, RUBBER);
k.box(-X - 0.62, 28.5, -4.1, -X - 0.56, 34.9, 1.1, DARK);
k.box(-X - 0.9, 22.6, 1.0, -X - 0.5, 23.8, 2.3, C.BLACK);
k.box(-X - 0.75, 12.2, -5.0, -X - 0.5, 16.2, -1.4, C.TRIM);
k.box(-19.0, 7.0, -6.0, -X, 8.0, 3.0, C.TRIM);
k.pair(16.4, 7.4, -5.6, 16.9, 9.4, -4.8, C.DARK_STEEL);
k.box(-X - 0.6, 36.6, 5.0, -X, 37.6, 7.4, '#EDE8D0');
k.g('glass');
k.box(-X - 0.85, 28.5, -4.1, -X - 0.62, 34.9, 1.1, C.GLASS);

// ---- the other side: the fridge vents and the mains inlet -------------------------
k.g('body');
for (const y of [14.0, 24.6]) {
    k.box(X, y, -6.4, X + 0.3, y + 3.0, 1.6, '#C8C6BF');
    for (let i = 0; i < 4; i++) {
        k.box(X + 0.3, y + 0.4 + i * 0.65, -6.0, X + 0.36, y + 0.7 + i * 0.65, 1.2, C.TRIM);
    }
}
k.box(X, 12.4, -22.0, X + 0.3, 15.0, -19.4, C.TRIM);

// ---- the wheels' arches and the legs -----------------------------------------------
k.sym(() => {
    const pts = [];
    for (const a of [25, 47, 68, 90, 112, 133, 155]) {
        pts.push([7.5 * Math.cos(a * Math.PI / 180), 6 + 7.5 * Math.sin(a * Math.PI / 180)]);
    }
    k.loft(X, 18.9, pts, 0.6, LOWER, 1);
    k.box(X, Y0, -7.6, X + 0.3, 14.2, 7.6, C.WELL);
    // a steady at each corner, wound up
    for (const z of [-27.0, 21.5]) {
        k.box(12.4, 7.4, z - 0.6, 13.6, 9.0, z + 0.6, C.DARK_STEEL);
        k.box(12.2, 6.8, z - 1.0, 13.8, 7.4, z + 1.0, C.DARK_STEEL);
    }
});
k.box(-14.0, 5.3, -0.8, 14.0, 6.7, 0.8, C.TRIM);

// ---- the tail ---------------------------------------------------------------------------
k.g('lamps');
k.box(-15.5, 8.6, -29.4, 15.5, 9.8, -28.2, RUBBER);
const LEAN = Math.atan2(3.6, 8.5) * 180 / Math.PI;
// a flat piece lying on the sloping panel under the tail, standing proud of it
function onTail(x1, y1, x2, y2, proud, color) {
    const z = along(TAIL, (y1 + y2) / 2) - proud / Math.cos(LEAN * Math.PI / 180);
    k.box(x1, y1, z - 0.2, x2, y2, z + 0.2, color, {tilt: LEAN});
}
k.sym(() => {
    onTail(11.0, 11.6, 14.8, 17.0, 0.0, RUBBER);
    onTail(11.4, 15.2, 14.4, 16.6, 0.15, C.AMBER);
    onTail(11.4, 13.1, 14.4, 15.2, 0.15, C.TAIL);
    onTail(11.9, 13.5, 13.9, 14.8, 0.25, C.TAIL_GLOW);
    onTail(11.4, 12.0, 14.4, 13.1, 0.15, C.REVERSE);
});
onTail(-3.8, 12.0, 3.8, 15.0, 0.0, RUBBER);
onTail(-3.5, 12.3, 3.5, 14.7, 0.12, C.PLATE);

// ---- the drawbar ------------------------------------------------------------------------
k.g('drawbar');
k.sym(() => k.beam([8.4, 9.1, 23.4], [1.4, 9.1, 40.6], 1.5, 1.4, GALV));
k.box(-1.2, 8.4, 36.0, 1.2, 9.8, 43.2, GALV);
// the gas locker
k.box(-6.4, 9.8, 28.6, 6.4, 17.6, 34.6, WHITE);
k.slab(-6.4, 6.4, 17.6, 34.6, 19.4, 28.6, 0.6, LOWER);
k.box(-0.9, 13.2, 34.6, 0.9, 14.0, 34.8, C.CHROME);
k.coupler(46.4, 8.0);
k.jockey(2.7, 38.6, 12.0, 4.8);
// the handbrake
k.beam([-0.9, 9.8, 37.4], [-0.9, 13.6, 35.8], 0.5, 0.6, C.DARK_STEEL);
k.box(-1.3, 13.2, 35.2, -0.5, 14.4, 36.2, C.BLACK);
