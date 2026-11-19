// The three portable generators, one drawing in three liveries: a tube
// frame on two wheels and two feet, the engine on the east half with its
// silencer outside it, the alternator behind a control panel on the west
// half, and the fuel on top. The coal one is red and has a firebox door and
// a hopper of coal; the biofuel one is green with a filler cap and a pull
// start; the diesel one is yellow with a finned head. The coal and the diesel one
// breathe out through a stack; the biofuel one makes do with its silencer.
// Running, the lamp on the panel is lit, and the coal one's fire shows.
const LIVERY = {
    generator: [C.RED, C.RED_DARK, 'minecraft:block/red_concrete'],
    biofuel_generator: [C.GREEN, C.GREEN_DARK, 'minecraft:block/lime_concrete'],
    diesel_generator: [C.YELLOW, C.YELLOW_DARK, 'minecraft:block/yellow_concrete']
};

function generator(kind, lit) {
    const [BODY, SHADE] = LIVERY[kind], FRAME = C.BLACK;

    // ---- the frame --------------------------------------------------------------
    k.g('frame');
    k.sym(() => {
        k.box(1, 1.4, 1, 2, 2.4, 15, FRAME);
        k.box(1, 13.6, 1, 2, 14.6, 15, FRAME);
        k.box(1, 2.4, 1, 2, 13.6, 2, FRAME);
        k.box(1, 2.4, 14, 2, 13.6, 15, FRAME);
        // a foot at the front, a wheel at the back
        k.box(0.9, 0, 1.6, 2.1, 1.4, 3.2, C.RUBBER);
        k.round('x', 0.55, 2, 12.2, 2, 0.9, C.RUBBER, {sides: 16});
        k.round('x', 0.1, 2, 12.2, 0.9, 0.2, C.ALU);
    });
    k.box(2, 1.4, 1, 14, 2.4, 2, FRAME);
    k.box(2, 1.4, 14, 14, 2.4, 15, FRAME);
    k.box(2, 13.6, 1, 14, 14.6, 2, FRAME);
    k.box(2, 13.6, 14, 14, 14.6, 15, FRAME);
    k.box(2, 2.4, 2.4, 14, 3, 13.6, C.DARK_STEEL);

    // ---- the alternator and its panel -------------------------------------------
    k.g('alternator');
    k.round('z', 5.4, 6.6, 9.6, 3.1, 6.4, C.DARK_STEEL, {sides: 16});
    k.round('z', 5.4, 6.6, 13, 2.2, 0.6, C.STEEL);
    k.g('panel');
    k.box(2.4, 3, 2.6, 8.6, 10.4, 6.4, C.BLACK);
    k.box(2.7, 3.4, 2.5, 8.3, 10, 2.6, SHADE, {only: ['north']});
    k.box(3, 3.7, 2.45, 8, 9.7, 2.5, C.TRIM, {only: ['north']});
    k.box(3.5, 7.6, 2.2, 4.3, 9, 2.5, lit ? C.GREEN : C.RED);
    k.box(3.5, 5.5, 2.3, 4.3, 6.3, 2.5, lit ? C.LED_GREEN : C.LED_OFF, {shade: !lit});
    k.box(5, 7.4, 2.35, 6.5, 9.1, 2.5, C.DIAL);
    k.box(5.6, 7.9, 2.3, 5.8, 8.9, 2.35, C.NEEDLE, {only: ['north'], rot: lit ? ['z', -22.5, [5.7, 7.9, 2.3]] : null});
    k.box(6.9, 7.6, 2.3, 7.7, 8.8, 2.5, C.STEEL);
    for (const x of [4.9, 6.7]) {
        k.box(x, 4.3, 2.3, x + 1.4, 6.2, 2.5, C.LED_BLUE);
        k.box(x + 0.4, 4.8, 2.25, x + 1, 5.7, 2.3, C.BLACK, {only: ['north']});
    }

    // ---- the engine -------------------------------------------------------------
    k.g('engine');
    k.box(9, 3, 4, 12.8, 8.6, 12.4, C.ALU);
    k.box(9.4, 8.6, 5.6, 12.4, 10.4, 10.6, C.IRON);
    k.box(10.4, 10.4, 7.4, 11.4, 10.8, 8.6, C.BLACK);
    k.g('silencer');
    k.box(12.8, 4, 4.8, 14.3, 9.4, 11.2, C.DARK_STEEL);
    for (const z of [5.6, 7.1, 8.6, 10.1]) {
        k.box(14.3, 4.8, z, 14.36, 8.6, z + 0.5, C.BLACK, {only: ['east']});
    }

    // ---- the tank ---------------------------------------------------------------
    k.g('tank');
    k.box(2.4, 10.8, 2.6, 12.6, 13.6, 13.4, BODY);
    k.box(3.2, 13.6, 3.4, 11.8, 14.3, 12.6, BODY);
    k.box(2.35, 11.6, 2.55, 12.65, 12.5, 13.45, SHADE, {no: ['up', 'down']});

    if (kind === 'generator') {
        // the firebox door, and the hopper
        k.g('firebox');
        k.box(9.3, 3.6, 3.4, 12.5, 8.2, 4, C.DARK_STEEL);
        for (const y of [4.3, 5.4, 6.5]) {
            k.box(9.9, y, 3.34, 11.9, y + 0.6, 3.4, lit ? C.LED_AMBER : C.BLACK, {only: ['north'], shade: !lit});
        }
        k.box(9.5, 7.3, 3.1, 10.1, 7.9, 3.4, C.CHROME);
        k.g('hopper');
        k.box(4.2, 14.3, 4.6, 10.8, 15.3, 11.4, C.DARK_STEEL);
        k.box(4.8, 15.3, 5.2, 10.2, 15.32, 10.8, C.BLACK, {only: ['up'], faces: {up: 'minecraft:block/coal_block'}});
    } else {
        // the pull start
        k.g('starter');
        k.round('z', 10.9, 5.8, 3.5, 1.9, 1, BODY, {sides: 16});
        k.round('z', 10.9, 5.8, 2.9, 0.8, 0.3, C.BLACK);
        k.box(11.9, 7, 3.2, 12.9, 7.5, 3.6, C.BLACK);
        k.g('cap');
        k.round('y', 5.6, 14.6, 8, 1.3, 0.6, C.BLACK);
        k.box(8, 14.3, 7.2, 10.8, 14.4, 8.8, C.WHITE, {only: ['up']});
    }
    if (kind === 'biofuel_generator') {
        k.box(5.6, 11.3, 2.5, 10.4, 13.1, 2.6, C.WHITE, {only: ['north']});
        k.box(7.3, 11.7, 2.45, 8.7, 12.7, 2.5, C.GREEN_DARK, {only: ['north']});
    }
    if (kind === 'diesel_generator') {
        k.g('fins');
        for (const y of [8.9, 9.5, 10.1]) {
            k.box(9.1, y, 5.3, 12.7, y + 0.3, 10.9, C.STEEL);
        }
    }
    if (kind !== 'biofuel_generator') {
        k.g('stack');
        k.round('y', 13.3, 12.3, 10, 0.6, 5.8, C.DARK_STEEL);
        k.round('y', 13.3, 15.4, 10, 0.8, 0.4, C.BLACK);
    }
}

for (const kind in LIVERY) {
    k.model(kind, {particle: LIVERY[kind][2]}, () => generator(kind, false));
    k.model(kind + '_lit', {particle: LIVERY[kind][2]}, () => generator(kind, true));
}
