// A hydrotreater on its skid, two blocks tall, facing north: the reactor in
// its aluminium cladding standing on a skirt, a domed head with the feed
// nozzle and a red relief valve, the feed pump at the front corner sending
// oil up a copper line over the top, two hydrogen bottles racked at the back
// with their line into the reactor's side, the product line out of the
// skirt on the west, a ladder up the west side, and the control panel at the
// front with its running lamp.
const SKID = C.DARK_STEEL, SKIN = C.ALU, PIPE = C.COPPER;
const CX = 8, CZ = 8.6, R = 4.6;

/** A round thing standing upright, cut where the two blocks meet. */
function upright(x, z, r, y1, y2, paint, o) {
    for (const [a, b] of [[y1, Math.min(y2, 16)], [Math.max(y1, 16), y2]]) {
        if (b > a) {
            k.round('y', x, (a + b) / 2, z, r, b - a, paint, o);
        }
    }
}

function treater(lit) {
    // ---- the skid ---------------------------------------------------------------------
    k.g('skid');
    k.box(0, 0, 0.4, 16, 0.6, 15.6, SKID);
    k.sym(() => {
        k.box(0, 0, 0.4, 0.8, 1.2, 15.6, SKID);
    });
    k.box(0.8, 0.6, 0.4, 15.2, 1.2, 1.2, SKID);
    k.box(0.8, 0.6, 14.8, 15.2, 1.2, 15.6, SKID);
    k.box(0.8, 0.6, 1.2, 15.2, 0.62, 14.8, C.STEEL, {only: ['up']});

    // ---- the reactor ------------------------------------------------------------------
    k.g('reactor');
    upright(CX, CZ, 4, 0.6, 3.2, SKID, {sides: 16});
    k.box(6.4, 1, CZ - 4.05, 9.6, 2.8, CZ - 3.95, C.BLACK, {only: ['north']});
    upright(CX, CZ, 4.9, 3.2, 3.8, C.STEEL, {sides: 16});
    upright(CX, CZ, R, 3.8, 26, SKIN, {sides: 16});
    for (const y of [8.4, 13.4, 18.8, 23.2]) {
        upright(CX, CZ, R + 0.12, y, y + 0.3, C.STEEL, {sides: 16});
    }
    // the domed head, its nozzle and the relief valve
    k.g('reactor/head');
    k.round('y', CX, 26.5, CZ, 4.1, 1, SKIN, {sides: 16});
    k.round('y', CX, 27.3, CZ, 3.2, 0.6, SKIN, {sides: 16});
    k.round('y', CX, 27.85, CZ, 2, 0.5, SKIN, {sides: 16});
    k.round('y', CX, 28.6, CZ, 1, 1, C.STEEL);
    k.round('y', CX, 29.25, CZ, 1.4, 0.3, C.STEEL, {sides: 16});
    k.round('y', 6, 28.4, 10.4, 0.5, 1, C.STEEL);
    k.box(5.3, 28.9, 9.7, 6.7, 30.6, 11.1, C.RED);
    k.round('y', 6, 31, 10.4, 0.35, 0.8, C.RED_DARK);
    k.round('z', 6, 30, 11.6, 0.4, 1, C.RED_DARK);
    // a manway, a gauge and a plate on its front
    k.g('reactor/front');
    k.round('z', CX, 20.6, CZ - R - 0.2, 1.6, 0.6, C.STEEL, {sides: 16});
    k.round('z', CX, 20.6, CZ - R - 0.55, 1.1, 0.2, SKIN, {sides: 16});
    k.round('z', 10.6, 14.6, 4.5, 0.9, 0.4, C.CHROME, {sides: 16});
    k.box(9.9, 13.9, 4.25, 11.3, 15.3, 4.3, C.DIAL, {only: ['north']});
    k.box(10.55, 14.6, 4.2, 10.65, 15.2, 4.25, C.NEEDLE, {only: ['north'], rot: ['z', 22.5, [10.6, 14.6, 4.22]]});
    k.box(6.6, 10, 4.04, 9.4, 11.6, 4.09, C.STEEL, {only: ['north']});

    // ---- the feed: the pump at the front corner, the line up and over the top ---------
    k.g('feed/pump');
    k.box(10.2, 1.2, 0.8, 15.4, 1.8, 3.8, SKID);
    k.round('x', 13.8, 3.2, 2.3, 1.2, 2.8, C.BLUE, {sides: 16});
    k.round('x', 15.3, 3.2, 2.3, 0.9, 0.3, SKID, {sides: 16});
    k.round('x', 11.5, 3.2, 2.3, 1.5, 1.4, C.BLUE_DARK, {sides: 16});
    k.round('z', 11.5, 3.2, 1.1, 0.6, 1, C.BLUE_DARK);
    k.g('feed/line');
    k.round('y', 11.5, 4.9, 2.3, 0.55, 1.4, PIPE);
    k.round('x', 12.65, 5.6, 2.3, 0.55, 2.3, PIPE);
    k.round('z', 13.8, 5.6, 3.75, 0.55, 2.9, PIPE);
    upright(13.8, 5.2, 0.55, 5.6, 29.6, PIPE);
    k.round('x', 10.9, 29.6, 5.2, 0.55, 5.8, PIPE);
    k.round('z', 8, 29.6, 6.9, 0.55, 3.4, PIPE);
    // the bends, each in a steel fitting
    for (const [x, y, z] of [[11.5, 5.6, 2.3], [13.8, 5.6, 2.3], [13.8, 5.6, 5.2], [13.8, 29.6, 5.2], [8, 29.6, 5.2], [8, 29.6, CZ]]) {
        k.box(x - 0.7, y - 0.7, z - 0.7, x + 0.7, y + 0.7, z + 0.7, C.STEEL);
    }
    // a valve on the riser
    k.box(13.1, 9.6, 4.5, 14.5, 11.2, 5.9, C.RED_DARK);
    k.round('x', 14.8, 10.4, 5.2, 0.25, 0.6, C.STEEL);
    k.round('x', 15.2, 10.4, 5.2, 1, 0.25, C.RED, {sides: 16});

    // ---- the hydrogen: two bottles racked at the back ---------------------------------
    k.g('hydrogen');
    k.box(12.2, 1.2, 9.4, 15.8, 1.6, 15.2, SKID);
    for (const z of [10.9, 13.7]) {
        upright(14.1, z, 1.3, 1.6, 15.4, C.WHITE, {sides: 16});
        k.round('y', 14.1, 15.8, z, 1, 0.8, C.WHITE);
        k.round('y', 14.1, 16.6, z, 0.6, 0.8, C.RED);
        k.round('y', 14.1, 17.3, z, 0.4, 0.6, C.BRASS);
    }
    for (const y of [5, 12]) {
        k.box(12.6, y, 9.4, 12.8, y + 0.8, 15.2, SKID);
        k.box(12.8, y, 15, 15.6, y + 0.8, 15.2, SKID);
    }
    // their line into the reactor's side
    k.round('z', 14.1, 17.5, 12.3, 0.3, 2.8, PIPE);
    k.round('x', 12.35, 17.5, 12.3, 0.3, 3.5, PIPE);

    // ---- the product line out of the skirt, west ---------------------------------------
    k.g('product');
    k.round('x', 1.9, 2.4, CZ, 0.7, 3.8, PIPE);
    k.round('x', 0.3, 2.4, CZ, 1.2, 0.6, C.STEEL, {sides: 16});

    // ---- the ladder up the west side ---------------------------------------------------
    k.g('ladder');
    for (const z of [CZ - 1.7, CZ + 1.3]) {
        upright(2.6, z + 0.2, 0.2, 1.2, 30, C.STEEL);
    }
    for (let y = 4.2; y < 30; y += 2.2) {
        k.box(2.4, y, CZ - 1.5, 2.8, y + 0.4, CZ + 1.5, C.STEEL);
    }
    // the cage over its upper part
    for (const y of [16.6, 21, 25.4, 29.6]) {
        k.box(0.6, y, CZ - 2.2, 3.2, y + 0.3, CZ - 1.9, C.STEEL);
        k.box(0.6, y, CZ + 1.9, 3.2, y + 0.3, CZ + 2.2, C.STEEL);
        k.box(0.6, y, CZ - 1.9, 0.9, y + 0.3, CZ + 1.9, C.STEEL);
    }
    upright(0.75, CZ, 0.15, 16.6, 29.9, C.STEEL);

    // ---- the control panel at the front, its lamp ---------------------------------------
    k.g('panel');
    k.box(2.6, 1.2, 1.4, 3.4, 6, 2.2, SKID);
    k.box(1, 6, 0.8, 7, 12, 2.6, C.STEEL);
    k.box(1.4, 6.4, 0.75, 6.6, 11.6, 0.8, C.BLACK, {only: ['north']});
    k.box(1.8, 9.4, 0.5, 3, 10.6, 0.75, lit ? C.LED_GREEN : C.LED_OFF, {shade: !lit});
    k.box(3.6, 9.4, 0.55, 4.6, 10.6, 0.75, C.RED);
    k.box(5, 9.2, 0.6, 6.2, 10.8, 0.75, C.DIAL);
    k.box(1.8, 6.9, 0.6, 6.2, 8.6, 0.75, lit ? C.LED_BLUE : C.SCREEN, {shade: !lit});
    k.box(0.8, 12, 0.6, 7.2, 12.4, 2.8, SKID);
}

k.model('hydrotreater/p', {cells: {'0': [0, 0, 0], '1': [0, 1, 0]}}, () => treater(false));
k.model('hydrotreater/p0_lit', {cells: {'': [0, 0, 0]}}, () => treater(true));
k.model('hydrotreater', {file: '../item/hydrotreater', shrink: 20}, () => treater(false));
