// A tripod jack for a plane's belly: a red cylinder on a cross-shaped foot,
// four legs braced to its collar, a chrome ram with a cupped saddle, and a
// pump with its lever on the east side. Raised, the ram stands eight
// further out, which is AircraftJackBlock.RISE.
const RED = C.RED;

function jack(rise) {
    k.g('foot');
    k.box(2, 0, 6.6, 14, 1.2, 9.4, RED);
    k.box(6.6, 0, 2, 9.4, 1.2, 14, RED);
    for (const [x, z] of [[2.5, 8], [13.5, 8], [8, 2.5], [8, 13.5]]) {
        k.box(x - 1.3, 0, z - 1.3, x + 1.3, 0.5, z + 1.3, C.DARK_STEEL);
        k.box(x - 0.4, 1.2, z - 0.4, x + 0.4, 1.5, z + 0.4, C.CHROME);
    }

    // ---- the legs -------------------------------------------------------------------
    k.g('legs');
    k.sym(() => {
        k.box(3.1, 0.4, 7.5, 4.1, 6.4, 8.5, RED, {rot: ['z', -45, [3.6, 1.2, 8]]});
    });
    k.sym(() => {
        k.box(7.5, 0.4, 3.1, 8.5, 6.4, 4.1, RED, {rot: ['x', 45, [8, 1.2, 3.6]]});
    }, 'z');

    // ---- the cylinder ---------------------------------------------------------------
    k.g('cylinder');
    k.round('y', 8, 3.9, 8, 2.6, 5.4, RED, {sides: 16});
    k.round('y', 8, 6.2, 8, 3, 0.9, C.DARK_STEEL, {sides: 16});
    k.round('y', 8, 1.6, 8, 3, 0.8, C.RED_DARK, {sides: 16});
    k.round('y', 8, 6.9, 8, 1.9, 0.5, C.STEEL);

    // ---- the ram and its saddle -------------------------------------------------------
    k.g('ram');
    k.round('y', 8, 7.1 + (0.9 + rise) / 2, 8, 1.2, 0.9 + rise, C.CHROME);
    k.round('y', 8, 8.3 + rise, 8, 2.6, 0.6, C.DARK_STEEL, {sides: 16});
    k.round('y', 8, 8.8 + rise, 8, 2.2, 0.4, C.RUBBER, {sides: 16});

    // ---- the pump ---------------------------------------------------------------------
    k.g('pump');
    k.box(10.4, 2, 7, 12, 4.4, 9, C.DARK_STEEL);
    k.box(11, 4.4, 7.5, 11.8, 5.2, 8.5, C.STEEL);
    k.round('x', 13.6, 4.8, 8, 0.4, 3.8, C.CHROME);
    k.round('x', 15.4, 4.8, 8, 0.55, 1, C.BLACK);
    // the gauge, facing north
    k.round('z', 11.2, 3.2, 6.7, 0.9, 0.6, C.STEEL);
    k.box(10.7, 2.7, 6.3, 11.7, 3.7, 6.4, C.DIAL);
    k.box(11.1, 3.1, 6.22, 11.6, 3.3, 6.3, C.NEEDLE);
}

k.model('aircraft_jack', {particle: 'overthought_vehicles:block/shop_red'}, () => jack(0));
k.model('aircraft_jack_raised', {particle: 'overthought_vehicles:block/shop_red'}, () => jack(8));
