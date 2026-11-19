// A fuel tank of steel angle and glass, after a workshop sight tank: glass
// in one pane a side, angle-iron posts and rims bolted round it, a tread
// plate lid with a bolted manway, its filler cap and a vent, and a floor.
// Tanks set side by side or stacked join into one: each piece below is drawn
// only while the sides it stands on are open, the way blockstates/fuel_tank.json
// lists them, so the pieces keep their numbers.
//
// What FuelTankRenderer counts on: the glass and the walls 0.6 in from each
// side, the floor's top at 1.5 and the lid's underside at 14.5.
const FRAME = C.STEEL, BOLT = C.CHROME, FLOOR = C.DARK_STEEL, GLASS = 'overthought_vehicles:block/tank_glass',
    TREAD = 'overthought_vehicles:block/tank_lid';

/** An angle iron up the corner at (x, z): one flange along x, the other along z, bolted twice. */
function post(x, z) {
    const sx = x === 0 ? 1 : -1, sz = z === 0 ? 1 : -1;
    const x1 = x === 0 ? 0 : 16, z1 = z === 0 ? 0 : 16;
    k.box(x1, 0, z1, x1 + sx * 2, 16, z1 + sz * 0.6, FRAME);
    k.box(x1, 0, z1, x1 + sx * 0.6, 16, z1 + sz * 2, FRAME);
    for (const y of [4, 12]) {
        k.box(x1 + sx * 1.0, y - 0.3, z1 - sz * 0.15, x1 + sx * 1.6, y + 0.3, z1, BOLT);
        k.box(x1 - sx * 0.15, y - 0.3, z1 + sz * 1.0, x1, y + 0.3, z1 + sz * 1.6, BOLT);
    }
}

/** An angle iron along the top (y 16) or the bottom (y 0) of a side, bolted twice. */
function rim(side, top) {
    const y1 = top ? 16 : 0, sy = top ? -1 : 1;
    // drawn on the north side, then laid on the side asked for
    const boxes = [
        [0, y1, 0, 16, y1 + sy * 0.6, 2],
        [0, y1, 0, 16, y1 + sy * 2, 0.6]
    ];
    for (const x of [5, 11]) {
        boxes.push([x - 0.3, y1 + sy * 1.0, -0.15, x + 0.3, y1 + sy * 1.6, 0, BOLT]);
    }
    for (const [ax, ay, az, bx, by, bz, paint] of boxes) {
        const place = (px, pz) => ({north: [px, pz], south: [16 - px, 16 - pz], west: [pz, 16 - px], east: [16 - pz, px]})[side];
        const [x1, z1] = place(ax, az), [x2, z2] = place(bx, bz);
        k.box(x1, ay, z1, x2, by, z2, paint || FRAME);
    }
}

const PIECES = [
    () => k.box(0, 0, 0.5, 16, 16, 0.5, GLASS, {only: ['north']}),
    () => k.box(0, 0, 15.5, 16, 16, 15.5, GLASS, {only: ['south']}),
    () => k.box(0.5, 0, 0, 0.5, 16, 16, GLASS, {only: ['west']}),
    () => k.box(15.5, 0, 0, 15.5, 16, 16, GLASS, {only: ['east']}),
    () => post(0, 0), () => post(16, 0), () => post(0, 16), () => post(16, 16),
    () => rim('north', true), () => rim('south', true), () => rim('west', true), () => rim('east', true),
    () => rim('north', false), () => rim('south', false), () => rim('west', false), () => rim('east', false),
    () => {
        // the lid: a tread plate, the manway bolted down in its middle, the filler cap on it, a vent behind
        k.g('lid');
        k.box(0, 14.5, 0, 16, 15.5, 16, FLOOR, {faces: {up: TREAD}});
        k.round('y', 8, 15.8, 8, 3.0, 0.6, FRAME, {sides: 16});
        for (let i = 0; i < 8; i++) {
            const t = i * Math.PI / 4, x = 8 + 2.5 * Math.cos(t), z = 8 + 2.5 * Math.sin(t);
            k.box(x - 0.25, 16.1, z - 0.25, x + 0.25, 16.3, z + 0.25, BOLT);
        }
        k.round('y', 8, 16.5, 8, 1.1, 0.8, C.YELLOW, {sides: 16});
        k.box(6.6, 16.9, 7.7, 9.4, 17.3, 8.3, C.YELLOW_DARK);
        k.round('y', 12.6, 16.2, 3.4, 0.45, 1.4, FRAME);
        k.round('x', 12.15, 16.9, 3.4, 0.45, 1.4, FRAME);
    },
    () => {
        // the floor, the drain plug in it
        k.g('floor');
        k.box(0, 0.5, 0, 16, 1.5, 16, FLOOR);
        k.round('y', 8, 1.6, 8, 0.7, 0.2, FRAME);
    }
];

PIECES.forEach((draw, i) => k.model('tank/part_' + i, {particle: 'overthought_vehicles:block/tank_glass'}, () => {
    k.g('piece');
    draw();
}));
k.model('tank/inventory', {parent: 'minecraft:block/block', particle: 'overthought_vehicles:block/tank_glass'}, () => {
    for (const draw of PIECES) {
        draw();
    }
});
