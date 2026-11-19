// The finer parts of the engine bay and the running gear, each after the real
// part and lying as it sits in a car, its front to the north (towards the
// grille). The bay's spots in PartLayout.BAY count on the lowest and highest
// pixel of the water pump, the thermostat, the injectors and the coils, which
// the run prints.
const CAST = '#9EA3A9', FORGED = '#5B6168', BRASS = '#B8933F', FRICTION = '#7A6450',
    BUCKET = '#3A3D42', CONNECTOR = '#4A4E54', SOCK = '#E8E6DC', BOOT = '#84221C', SEAL = '#35702A',
    PLASTIC = C.PLASTIC, RUBBER = C.RUBBER;

/** Where the four cylinders of an inline four sit along the head, as on the engine. */
const CYLINDERS = [3.4, 6.5, 9.5, 12.6];

// ---- water pump -----------------------------------------------------------------

k.model('water_pump', {}, () => {
    // the cast body round the impeller, stepping down to the bearing's snout
    k.g('body');
    k.round('z', 8, 8, 9.0, 3.7, 2.6, CAST, {sides: 16});
    k.round('z', 8, 8, 7.3, 2.6, 0.9, CAST, {sides: 16});
    k.round('z', 8, 8, 6.0, 1.4, 1.8, CAST, {sides: 16});
    // the lobed flange it bolts to the block by, and the paper gasket behind it
    k.g('flange');
    k.round('z', 8, 8, 10.7, 4.3, 0.8, CAST, {sides: 16});
    for (const [x, y] of [[4.5, 4.5], [11.5, 4.5], [4.5, 11.5], [11.5, 11.5]]) {
        k.round('z', x, y, 10.7, 1.2, 0.8, CAST, {sides: 16});
        k.round('z', x, y, 10.05, 0.6, 0.5, C.CHROME);
    }
    k.round('z', 8, 8, 11.2, 4.3, 0.2, FRICTION, {sides: 16});
    // the hose neck out of the side, beaded at its end and filleted into the body
    k.g('neck');
    k.round('x', 12.1, 9.2, 9.0, 1.0, 2.8, CAST, {sides: 16});
    k.round('x', 11.3, 9.2, 9.0, 1.35, 0.6, CAST, {sides: 16});
    k.round('x', 13.2, 9.2, 9.0, 1.15, 0.35, CAST, {sides: 16});
    // the hub on the shaft and the ribbed pulley bolted to it
    k.g('pulley');
    k.round('z', 8, 8, 4.85, 1.9, 0.5, C.STEEL, {sides: 16});
    k.round('z', 8, 8, 3.6, 3.3, 2.0, C.STEEL, {sides: 16});
    for (const z of [3.0, 3.6, 4.2]) {
        k.round('z', 8, 8, z, 3.34, 0.12, '#4E5258', {sides: 16});
    }
    k.round('z', 8, 8, 2.55, 2.5, 0.1, C.IRON, {sides: 16});
    for (const [x, y] of [[8, 9.3], [9.3, 8], [8, 6.7], [6.7, 8]]) {
        k.round('z', x, y, 2.35, 0.4, 0.3, C.CHROME);
    }
    k.round('z', 8, 8, 2.35, 0.6, 0.3, C.CHROME);
});

// ---- thermostat -----------------------------------------------------------------

k.model('thermostat', {}, () => {
    // the outlet housing: a cast pot with the hose neck out of its side and the temperature sender on top
    k.g('housing');
    k.round('y', 8, 9.8, 8, 2.4, 2.8, CAST, {sides: 16});
    k.round('y', 8, 11.4, 8, 1.9, 0.4, CAST, {sides: 16});
    k.round('x', 11.0, 10.0, 8, 1.3, 3.4, CAST, {sides: 16});
    k.round('x', 10.1, 10.0, 8, 1.6, 0.6, CAST, {sides: 16});
    k.round('x', 12.4, 10.0, 8, 1.5, 0.4, CAST, {sides: 16});
    k.round('y', 8, 11.9, 8, 0.7, 0.6, BRASS);
    k.box(7.3, 12.2, 7.3, 8.7, 13.2, 8.7, PLASTIC);
    k.box(7.5, 12.6, 8.7, 8.5, 13.0, 9.1, C.BLACK);
    // the flange with its two ears and their bolts, and the o-ring under it
    k.g('flange');
    k.round('y', 8, 8.0, 8, 3.0, 0.8, CAST, {sides: 16});
    for (const x of [4.6, 11.4]) {
        k.round('y', x, 8.0, 8, 1.1, 0.8, CAST, {sides: 16});
        k.round('y', x, 8.65, 8, 0.55, 0.5, C.CHROME);
    }
    k.round('y', 8, 7.45, 8, 2.6, 0.3, RUBBER, {sides: 16});
    // the thermostat itself: brass flange, the wax pellet in its spring, the yoke holding the spring
    k.g('thermostat');
    k.round('y', 8, 7.17, 8, 2.5, 0.25, BRASS, {sides: 16});
    k.round('y', 9.7, 7.35, 8, 0.25, 0.3, BRASS);
    k.round('y', 8, 5.6, 8, 0.95, 2.4, BRASS, {sides: 16});
    for (const y of [6.7, 6.15, 5.6, 5.05]) {
        k.round('y', 8, y, 8, 1.5, 0.22, C.STEEL, {sides: 16});
    }
    for (const x of [5.9, 9.7]) {
        k.box(x, 4.6, 7.6, x + 0.4, 7.05, 8.4, BRASS);
    }
    k.box(5.9, 4.2, 7.6, 10.1, 4.6, 8.4, BRASS);
    k.round('y', 8, 3.8, 8, 0.45, 0.8, BRASS);
});

// ---- fuel pump module, from inside the tank ---------------------------------------

k.model('tank_pump', {}, () => {
    // the flange that shuts the hole in the tank, held down by a steel lock ring
    k.g('flange');
    k.round('y', 8, 12.4, 8, 4.0, 0.8, PLASTIC, {sides: 16});
    k.round('y', 8, 12.1, 8, 4.4, 0.3, C.STEEL, {sides: 16});
    k.round('y', 8, 13.2, 8, 2.2, 0.8, PLASTIC, {sides: 16});
    // the plug for its wires, and the outlet bent over towards the engine
    k.box(4.4, 12.8, 6.9, 6.4, 14.2, 9.1, CONNECTOR);
    k.box(4.2, 13.6, 7.5, 4.4, 14.0, 8.5, C.BLACK);
    k.round('y', 10.0, 13.9, 8, 0.55, 1.4, C.STEEL);
    k.round('x', 10.9, 14.3, 8, 0.5, 1.8, C.STEEL);
    k.round('x', 11.6, 14.3, 8, 0.62, 0.3, C.STEEL);
    // two guide rods down to the bucket the pump stands in
    k.g('rods');
    for (const x of [5.1, 10.9]) {
        k.round('y', x, 10.4, 8, 0.3, 3.2, C.STEEL);
    }
    k.g('bucket');
    k.round('y', 8, 6.8, 8, 2.6, 5.4, BUCKET, {sides: 16});
    k.round('y', 8, 9.6, 8, 2.8, 0.4, BUCKET, {sides: 16});
    for (const x of [5.1, 10.9]) {
        k.box(x - 0.5, 8.2, 7.5, x + 0.5, 9.0, 8.5, BUCKET);
    }
    // the pump's top in the bucket, and its two leads up to the flange
    k.round('y', 8, 9.95, 8, 1.3, 0.5, C.ALU, {sides: 16});
    k.box(7.0, 10.2, 7.85, 7.3, 12.0, 8.15, C.RED);
    k.box(8.7, 10.2, 7.85, 9.0, 12.0, 8.15, C.BLACK);
    // the level sender on the bucket's side, its arm and float
    k.g('sender');
    k.box(7.1, 6.4, 10.4, 8.9, 8.8, 11.1, PLASTIC);
    k.box(7.85, 7.4, 11.1, 8.15, 7.7, 13.6, C.STEEL);
    k.box(7.0, 6.7, 13.6, 9.0, 8.3, 15.0, C.BLACK);
    // the filter sock it draws through
    k.g('sock');
    k.round('y', 8, 3.85, 8, 0.7, 0.5, BUCKET);
    k.box(5.2, 3.0, 5.6, 10.8, 3.6, 10.4, SOCK);
});

// ---- injectors on their fuel rail -------------------------------------------------

k.model('injectors', {}, () => {
    // the rail: the feed at one end, the pressure sensor and its plug at the other
    k.g('rail');
    k.box(1.8, 10.6, 6.9, 14.2, 12.0, 8.3, C.ALU);
    k.round('x', 1.2, 11.3, 7.6, 0.55, 1.2, C.STEEL);
    k.round('x', 0.75, 11.3, 7.6, 0.7, 0.3, C.STEEL);
    k.round('x', 14.6, 11.3, 7.6, 0.6, 0.8, C.STEEL);
    k.box(15.0, 10.8, 7.1, 16.0, 11.8, 8.1, PLASTIC);
    // the two brackets it bolts to the head by, between the cylinders
    for (const x of [5.0, 11.0]) {
        k.box(x - 0.6, 10.6, 8.3, x + 0.6, 11.6, 10.0, C.ALU);
        k.round('y', x, 11.8, 9.3, 0.55, 0.4, C.CHROME);
    }
    // each injector: the cup on the rail, the upper seal, the black body with its plug, the steel nose and its seal
    k.g('injectors');
    for (const x of CYLINDERS) {
        k.round('y', x, 10.2, 7.6, 0.95, 0.8, C.ALU, {sides: 16});
        k.round('y', x, 9.7, 7.6, 0.8, 0.2, SEAL, {sides: 16});
        k.round('y', x, 8.4, 7.6, 0.75, 2.4, PLASTIC, {sides: 16});
        k.box(x - 0.55, 8.4, 8.2, x + 0.55, 9.5, 9.8, CONNECTOR);
        k.box(x - 0.3, 9.5, 8.6, x + 0.3, 9.7, 9.6, C.BLACK);
        k.round('y', x, 6.5, 7.6, 0.5, 1.4, C.STEEL, {sides: 16});
        k.round('y', x, 5.85, 7.6, 0.56, 0.25, SEAL, {sides: 16});
        k.round('y', x, 5.3, 7.6, 0.3, 0.6, C.CHROME);
    }
    // the loom the plugs hang off
    k.g('loom');
    k.round('x', 8, 9.0, 10.6, 0.45, 12.6, C.BLACK);
    for (const x of CYLINDERS) {
        k.box(x - 0.2, 8.8, 9.8, x + 0.2, 9.2, 10.4, C.BLACK);
    }
});

// ---- ignition coils, one on each plug ---------------------------------------------

k.model('ignition_coils', {}, () => {
    for (const x of CYLINDERS) {
        // the head: the coil's case, its plug, and the ear it is bolted down by
        k.g('coil/head');
        k.box(x - 1.3, 11.0, 6.0, x + 1.3, 12.8, 9.6, PLASTIC);
        k.box(x - 1.1, 12.8, 6.2, x + 1.1, 13.1, 9.4, PLASTIC);
        k.box(x - 0.8, 11.4, 9.6, x + 0.8, 12.6, 10.9, CONNECTOR);
        k.box(x - 0.4, 12.6, 9.9, x + 0.4, 12.8, 10.7, C.BLACK);
        k.box(x - 0.6, 11.0, 4.6, x + 0.6, 11.6, 6.0, PLASTIC);
        k.round('y', x, 11.3, 5.2, 0.45, 0.7, C.ALU);
        k.round('y', x, 11.85, 5.2, 0.5, 0.4, C.CHROME);
        // the tube down the plug well, the silicone boot on the plug and the spring in it
        k.g('coil/tube');
        k.round('y', x, 10.75, 7.8, 1.0, 0.5, PLASTIC, {sides: 16});
        k.round('y', x, 8.1, 7.8, 0.8, 5.0, '#26282C', {sides: 16});
        k.round('y', x, 4.75, 7.8, 0.95, 1.7, BOOT, {sides: 16});
        k.round('y', x, 3.65, 7.8, 0.3, 0.5, C.STEEL);
    }
    // the loom the plugs hang off
    k.g('loom');
    k.round('x', 8, 12.0, 11.8, 0.45, 11.0, C.BLACK);
    for (const x of CYLINDERS) {
        k.box(x - 0.2, 11.8, 10.9, x + 0.2, 12.2, 11.4, C.BLACK);
    }
});

// ---- brake caliper on its carrier --------------------------------------------------

// The disc runs through the slot at z 7.5..8.9, its rim under the bridge and
// the hub below, so nothing crosses the slot under y 10.2.
k.model('brake_calipers', {}, () => {
    const RED = C.RED, CARRIER = C.DARK_STEEL;
    // the caliper: painted cast iron, the piston's bore inboard, the fingers outboard, a bridge over the rim
    k.g('caliper');
    k.box(4.4, 6.6, 5.4, 11.6, 11.0, 7.5, RED);
    k.round('z', 8, 8.8, 4.9, 2.3, 1.2, RED, {sides: 16});
    k.box(4.4, 10.2, 7.5, 6.6, 11.0, 8.9, RED);
    k.box(9.4, 10.2, 7.5, 11.6, 11.0, 8.9, RED);
    k.box(4.4, 7.4, 8.9, 11.6, 11.0, 10.2, RED);
    k.box(5.0, 6.6, 8.9, 7.4, 7.4, 10.2, RED);
    k.box(8.6, 6.6, 8.9, 11.0, 7.4, 10.2, RED);
    // its back rounded over in two steps, following the disc, round the window the pads show through
    for (const [y1, y2, inset] of [[11.0, 11.6, 0.4], [11.6, 11.9, 0.8]]) {
        k.box(4.4 + inset, y1, 5.4 + inset / 2, 6.6, y2, 10.2 - inset / 2, RED);
        k.box(9.4, y1, 5.4 + inset / 2, 11.6 - inset, y2, 10.2 - inset / 2, RED);
        k.box(6.6, y1, 5.4 + inset / 2, 9.4, y2, 7.5, RED);
        k.box(6.6, y1, 8.9, 9.4, y2, 10.2 - inset / 2, RED);
    }
    // the port the hose's banjo bolts to, and the hose
    k.box(7.2, 10.4, 4.4, 8.8, 11.6, 5.4, RED);
    k.box(6.9, 10.5, 3.6, 9.1, 11.5, 4.4, C.STEEL);
    k.round('z', 8, 11.0, 3.3, 0.55, 0.6, C.CHROME);
    k.round('y', 8, 12.6, 4.0, 0.45, 2.0, RUBBER);
    // the bleed nipple and its cap
    k.round('y', 10.4, 12.35, 6.4, 0.35, 0.9, C.STEEL);
    k.round('y', 10.4, 12.95, 6.4, 0.45, 0.4, RUBBER);
    // the pads either side of the disc, seen through the bridge's window
    k.g('pads');
    k.box(4.8, 7.0, 7.5, 11.2, 10.0, 7.8, FRICTION);
    k.box(4.8, 7.0, 8.6, 11.2, 10.0, 8.9, FRICTION);
    k.box(4.8, 10.0, 7.5, 11.2, 10.2, 7.7, C.STEEL);
    k.box(4.8, 10.0, 8.7, 11.2, 10.2, 8.9, C.STEEL);
    // the carrier bolted to the knuckle, its arms reaching over the rim, the slide pins in their boots
    k.g('carrier');
    k.sym(() => {
        k.box(2.4, 5.4, 5.6, 4.4, 10.2, 7.4, CARRIER);
        k.box(2.4, 10.2, 5.6, 4.4, 11.0, 9.4, CARRIER);
        k.box(2.4, 5.4, 4.8, 5.2, 6.6, 5.6, CARRIER);
        k.round('z', 3.3, 8.8, 5.0, 0.65, 1.2, RUBBER);
        k.round('z', 3.3, 8.8, 4.1, 0.55, 0.6, C.CHROME);
        k.round('z', 3.6, 6.0, 4.5, 0.55, 0.6, C.CHROME);
    });
});

// ---- ball joint ---------------------------------------------------------------------

k.model('ball_joints', {}, () => {
    // the flange it bolts to the arm by, three ears with their bolts
    k.g('flange');
    k.round('y', 8, 5.6, 8, 2.9, 0.9, FORGED, {sides: 16});
    for (const turn of [90, 210, 330]) {
        const x = 8 + 3.4 * Math.cos(turn * Math.PI / 180), z = 8 + 3.4 * Math.sin(turn * Math.PI / 180);
        k.round('y', x, 5.6, z, 1.1, 0.9, FORGED, {sides: 16});
        k.round('y', x, 6.3, z, 0.55, 0.5, C.CHROME);
    }
    // the housing round the ball, its cup below and the grease nipple
    k.g('housing');
    k.round('y', 8, 7.0, 8, 2.3, 1.9, C.IRON, {sides: 16});
    k.round('y', 8, 4.4, 8, 1.7, 1.5, C.IRON, {sides: 16});
    k.round('y', 8, 3.3, 8, 0.4, 0.7, BRASS);
    k.round('y', 8, 2.8, 8, 0.45, 0.3, C.YELLOW);
    // the rubber boot, folded like a bellows
    k.g('boot');
    k.round('y', 8, 8.3, 8, 2.1, 0.7, RUBBER, {sides: 16});
    k.round('y', 8, 8.95, 8, 1.7, 0.6, RUBBER, {sides: 16});
    k.round('y', 8, 9.5, 8, 1.9, 0.5, RUBBER, {sides: 16});
    k.round('y', 8, 10.0, 8, 1.3, 0.5, RUBBER, {sides: 16});
    k.round('y', 8, 10.4, 8, 1.0, 0.3, RUBBER, {sides: 16});
    // the tapered stud, the castle nut on its thread and the split pin through both
    k.g('stud');
    k.round('y', 8, 11.0, 8, 0.9, 0.9, C.STEEL);
    k.round('y', 8, 11.9, 8, 0.7, 0.9, C.STEEL);
    k.round('y', 8, 12.75, 8, 0.95, 0.8, C.STEEL);
    for (const [x, z] of [[7.25, 7.25], [8.35, 7.25], [7.25, 8.35], [8.35, 8.35]]) {
        k.box(x, 13.15, z, x + 0.4, 13.5, z + 0.4, C.STEEL);
    }
    k.round('y', 8, 13.45, 8, 0.45, 0.6, C.ALU);
    k.box(6.9, 13.25, 7.9, 9.1, 13.45, 8.1, C.ALU);
});

// ---- silencer -----------------------------------------------------------------------

k.model('muffler', {}, () => {
    // the oval drum: two half rounds and the flat between, the ends rolled over, a rib pressed round it
    const oval = (x, len, grow, paint) => {
        k.round('x', x, 8, 6.2, 2.4 + grow, len, paint, {sides: 16});
        k.round('x', x, 8, 9.8, 2.4 + grow, len, paint, {sides: 16});
        k.box(x - len / 2, 5.6 - grow, 6.2, x + len / 2, 10.4 + grow, 9.8, paint);
    };
    k.g('drum');
    oval(8, 9.6, 0, C.ALU);
    for (const x of [3.35, 12.65]) {
        oval(x, 0.5, 0.15, C.IRON);
    }
    for (const x of [6.4, 9.6]) {
        oval(x, 0.3, 0.1, C.ALU);
    }
    // the pipe in at one end, the pipe out at the other, each welded on with a collar
    k.g('pipes');
    k.round('x', 1.7, 8, 6.4, 0.95, 2.6, C.IRON, {sides: 16});
    k.round('x', 2.9, 8, 6.4, 1.2, 0.4, C.IRON, {sides: 16});
    k.round('x', 14.3, 8, 9.6, 0.95, 2.6, C.IRON, {sides: 16});
    k.round('x', 13.1, 8, 9.6, 1.2, 0.4, C.IRON, {sides: 16});
    // the two hanger rods welded to the top, each hooked through its rubber ring
    k.g('hangers');
    for (const x of [5.0, 11.0]) {
        k.round('y', x, 10.5, 8, 0.5, 0.2, C.STEEL);
        k.round('y', x, 11.3, 8, 0.3, 1.6, C.STEEL);
        k.round('x', x, 12.7, 8, 1.1, 0.7, RUBBER, {sides: 16});
        k.round('x', x, 12.7, 8, 0.45, 0.8, C.BLACK);
    }
});
