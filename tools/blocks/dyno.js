// A rolling road let into the floor: a steel deck with two pits across it,
// a roller in each. The deck is the block; DynoRenderer draws the rollers
// from dyno_roller, turning, their axles three up at z 5.5 and 10.5.
const DECK = C.DARK_STEEL, HAZARD = 'overthought_vehicles:block/shop_hazard';

k.model('dyno', {particle: 'minecraft:block/gray_concrete'}, () => {
    k.g('deck');
    k.box(0, 0, 0, 16, 0.8, 16, C.BLACK);
    k.box(0, 0.8, 0, 16, 3, 3.8, DECK);
    k.box(0, 0.8, 7.2, 16, 3, 8.8, DECK);
    k.box(0, 0.8, 12.2, 16, 3, 16, DECK);
    // the cheeks the axles run in
    k.sym(() => {
        k.box(0, 0.8, 3.8, 0.7, 3, 7.2, DECK);
        k.box(0, 0.8, 8.8, 0.7, 3, 12.2, DECK);
        for (const z of [5.5, 10.5]) {
            k.box(0.1, 2, z - 1.1, 0.75, 4, z + 1.1, C.STEEL);
            k.round('x', 0.4, 3, z, 0.6, 0.9, C.CHROME);
        }
    });

    // ---- the tread and its markings ------------------------------------------------
    k.g('tread');
    k.box(0, 3, 0, 16, 3.15, 1.4, HAZARD);
    k.box(0, 3, 14.6, 16, 3.15, 16, HAZARD);
    k.box(1, 3, 1.8, 15, 3.12, 3.4, 'overthought_vehicles:block/lift_plate');
    k.box(1, 3, 12.6, 15, 3.12, 14.2, 'overthought_vehicles:block/lift_plate');
    k.box(1, 3, 7.5, 15, 3.1, 8.5, C.STEEL);
    for (const x of [1.4, 14]) {
        for (const z of [7.7, 8]) {
            k.box(x, 3.1, z, x + 0.5, 3.25, z + 0.3, C.CHROME);
        }
    }
});

k.model('dyno_roller', {particle: 'minecraft:block/iron_block'}, () => {
    k.g('roller');
    k.round('x', 8, 3, 8, 1.45, 14.4, C.ALU, {sides: 16, caps: C.STEEL});
    // knurled bands, and one painted flat to see it turn by
    for (const x of [3, 8, 13]) {
        k.round('x', x, 3, 8, 1.5, 0.5, C.IRON, {sides: 16});
    }
    k.box(0.8, 1.55, 7.7, 15.2, 4.45, 8.3, C.YELLOW);
});
