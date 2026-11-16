// The four wheels, each a tyre on its rim after the real thing, the rim's face
// to the north where the item shows it.
const RUBBER = C.RUBBER, HOLE = '#2A2D31', GUNMETAL = '#62676E', PAINTED = '#A7ADB3';

/** The tyre: tread R across and wide, the sidewall a little in from it and a little wider, rubber all round. */
function tyre(R, wide) {
    k.g('tyre');
    k.round('z', 8, 8, 8, R - 0.2, wide, RUBBER, {sides: 16});
    k.round('z', 8, 8, 8, R - 0.7, wide + 0.4, RUBBER, {sides: 16});
}

/**
 * The rim's two faces: a polished lip r across standing just off the
 * sidewall, the dark barrel inside it, and the same lip on the back.
 */
function rim(r, front, back, lip, barrel) {
    k.g('rim');
    k.round('z', 8, 8, front - 0.1, r + 0.35, 0.2, lip, {sides: 16});
    k.round('z', 8, 8, front - 0.25, r, 0.1, barrel, {sides: 16});
    k.round('z', 8, 8, back + 0.1, r + 0.35, 0.2, lip, {sides: 16});
    k.round('z', 8, 8, back + 0.25, r, 0.1, barrel, {sides: 16});
}

/** n lug nuts on a circle r across at depth z. */
function lugs(n, r, z, paint) {
    k.g('lugs');
    for (let i = 0; i < n; i++) {
        const t = (90 + i * 360 / n) * Math.PI / 180, x = 8 + r * Math.cos(t), y = 8 + r * Math.sin(t);
        k.box(x - 0.25, y - 0.25, z - 0.15, x + 0.25, y + 0.25, z, paint);
    }
}

// ---- the road wheel: an eight-spoke silver alloy on a three-rib touring tyre ---------------

k.model('wheel', {gui: [18, 203, 0]}, () => {
    tyre(7, 5.0);
    k.g('tread');
    for (const z of [6.2, 9.8]) {
        k.round('z', 8, 8, z, 7.0, 1.4, RUBBER, {sides: 16});
    }
    k.round('z', 8, 8, 8, 7.0, 1.6, RUBBER, {sides: 16});
    k.round('z', 8, 8, 8, 6.85, 4.8, HOLE, {sides: 16});
    rim(4.4, 5.3, 10.7, C.CHROME, HOLE);
    k.g('spokes');
    for (let n = 0; n < 8; n++) {
        k.spin(n * 45, -0.5, 1.6, 4.75, 0.5, 4.45, 5.05, C.ALU);
    }
    k.round('z', 8, 8, 4.8, 2.0, 0.5, C.ALU, {sides: 16});
    k.round('z', 8, 8, 4.5, 0.9, 0.2, GUNMETAL, {sides: 16});
    lugs(5, 1.45, 4.55, C.CHROME);
});

// ---- the dirt wheel: a black beadlock steel rim on a knobbly mud tyre --------------------

k.model('wheel_dirt', {gui: [18, 203, 0]}, () => {
    tyre(7.0, 5.4);
    // the knobs: staggered blocks round the tread, a shoulder lug off every other one
    k.g('knobs');
    for (let n = 0; n < 16; n++) {
        const a = n * 22.5;
        if (n % 2) {
            k.spin(a, -1.0, 6.6, 7.0, 1.0, 7.4, 9.0, RUBBER);
        } else {
            k.spin(a, -1.0, 6.6, 5.3, 1.0, 7.4, 7.0, RUBBER);
            k.spin(a, -1.0, 6.6, 9.0, 1.0, 7.4, 10.7, RUBBER);
            k.spin(a, -0.6, 5.6, 4.9, 0.6, 6.6, 5.1, RUBBER);
            k.spin(a, -0.6, 5.6, 10.9, 0.6, 6.6, 11.1, RUBBER);
        }
    }
    rim(4.0, 5.1, 10.9, C.DARK_STEEL, C.BLACK);
    // the beadlock ring bolted round the lip, the dished centre with its holes, the hub and its six nuts
    k.g('beadlock');
    k.round('z', 8, 8, 4.7, 4.6, 0.2, C.STEEL, {sides: 16});
    for (let i = 0; i < 16; i++) {
        const t = i * 22.5 * Math.PI / 180, x = 8 + 4.15 * Math.cos(t), y = 8 + 4.15 * Math.sin(t);
        k.box(x - 0.18, y - 0.18, 4.45, x + 0.18, y + 0.18, 4.6, C.BLACK);
    }
    k.g('centre');
    k.round('z', 8, 8, 4.55, 3.7, 0.1, '#26282C', {sides: 16});
    for (let i = 0; i < 8; i++) {
        const t = (22.5 + i * 45) * Math.PI / 180, x = 8 + 2.7 * Math.cos(t), y = 8 + 2.7 * Math.sin(t);
        k.box(x - 0.4, y - 0.4, 4.45, x + 0.4, y + 0.4, 4.5, HOLE);
    }
    k.round('z', 8, 8, 4.35, 1.6, 0.3, C.DARK_STEEL, {sides: 16});
    k.round('z', 8, 8, 4.15, 0.7, 0.1, C.BLACK, {sides: 16});
    lugs(6, 1.2, 4.2, C.CHROME);
});

// ---- the snow wheel: a painted steel rim on a siped winter tyre with studs ----------------

k.model('wheel_snow', {gui: [18, 203, 0]}, () => {
    tyre(7, 5.2);
    // the blocks: two rows either side of a centre groove, staggered, each cut with a sipe; a stud in every other
    k.g('blocks');
    k.round('z', 8, 8, 8, 6.85, 5.2, HOLE, {sides: 16});
    for (let n = 0; n < 16; n++) {
        const a = n * 22.5, off = n % 2 ? 0.5 : 0;
        k.spin(a, -1.0, 6.5, 5.4 + off, 1.0, 7.05, 7.7, RUBBER);
        k.spin(a, -1.0, 6.5, 8.3, 1.0, 7.05, 10.6 - off, RUBBER);
        k.spin(a, -0.06, 6.9, 5.6 + off, 0.06, 7.1, 7.5, HOLE);
        k.spin(a, -0.06, 6.9, 8.5, 0.06, 7.1, 10.4 - off, HOLE);
        if (n % 2 === 0) {
            k.spin(a, 0.4, 7.05, 6.4, 0.7, 7.15, 6.7, C.CHROME);
            k.spin(a, -0.7, 7.05, 9.3, -0.4, 7.15, 9.6, C.CHROME);
        }
    }
    rim(4.2, 5.2, 10.8, PAINTED, HOLE);
    // the pressed steel centre: its ring of vent holes, the raised hub, the nuts and a black cap
    k.g('centre');
    k.round('z', 8, 8, 4.85, 3.9, 0.1, PAINTED, {sides: 16});
    for (let i = 0; i < 8; i++) {
        k.spin(i * 45, -0.35, 2.6, 4.75, 0.35, 3.4, 4.8, HOLE);
    }
    k.round('z', 8, 8, 4.6, 2.1, 0.4, PAINTED, {sides: 16});
    lugs(5, 1.5, 4.4, C.STEEL);
    k.round('z', 8, 8, 4.3, 0.9, 0.2, C.BLACK, {sides: 16});
});

// ---- the sport wheel: a wide gunmetal multi-spoke with a polished lip on a low tyre --------

k.model('wheel_sport', {gui: [18, 203, 0]}, () => {
    tyre(7.2, 6.0);
    k.g('tread');
    k.round('z', 8, 8, 8, 7.05, 6.0, HOLE, {sides: 16});
    k.round('z', 8, 8, 6.1, 7.2, 2.2, RUBBER, {sides: 16});
    k.round('z', 8, 8, 8, 7.2, 1.0, RUBBER, {sides: 16});
    k.round('z', 8, 8, 9.9, 7.2, 2.2, RUBBER, {sides: 16});
    rim(5.3, 4.8, 11.2, C.CHROME, HOLE);
    // ten spokes would not sit on the game's turns, so sixteen: eight heavy ones and a light one between each
    k.g('spokes');
    for (let n = 0; n < 16; n++) {
        const half = n % 2 ? 0.3 : 0.55;
        k.spin(n * 22.5, -half, 1.6, n % 2 ? 4.35 : 4.25, half, 5.4, 4.55, GUNMETAL);
    }
    k.round('z', 8, 8, 4.3, 1.9, 0.5, GUNMETAL, {sides: 16});
    k.round('z', 8, 8, 4.0, 0.9, 0.2, C.CHROME, {sides: 16});
    lugs(5, 1.35, 4.05, C.CHROME);
});
