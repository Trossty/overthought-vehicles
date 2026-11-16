// What a car stops, steers and rides on, what carries its exhaust away, and the
// big pieces a vehicle is built from, each after the real part, its front to
// the north.
const RUBBER = C.RUBBER, ROTOR = '#9A9EA3', HAT = '#5F6368', HOLE = '#2A2D31',
    CALIPER = '#C0392B', CALIPER_FACE = '#D5463A', FRICTION = '#7A6450', BACKING = '#4A4E54',
    SPRING = '#C0392B', DAMPER = '#2E3135', ALU_CAST = '#9EA3A9', FORGED = '#5B6168',
    STAINLESS = '#B9BEC3', SHIELD = '#CDD1D5', PRIMER = '#8D939A', SEAM = '#5E646A';

/** A flat eight-sided ring about the upright through (cx, cz): its bars a from the middle, b thick, h tall. */
function ring(cx, y, cz, a, b, h, paint) {
    const side = 2 * (a + b / 2) * Math.tan(Math.PI / 8) + 0.04;
    for (const s of [-1, 1]) {
        k.box(cx - side / 2, y, cz + s * a - b / 2, cx + side / 2, y + h, cz + s * a + b / 2, paint);
        k.box(cx + s * a - b / 2, y, cz - side / 2, cx + s * a + b / 2, y + h, cz + side / 2, paint);
    }
    const d = a * Math.SQRT1_2;
    for (const [dx, dz] of [[1, 1], [1, -1], [-1, 1], [-1, -1]]) {
        const px = cx + dx * d, pz = cz + dz * d;
        k.box(px - side / 2, y, pz - b / 2, px + side / 2, y + h, pz + b / 2, paint,
            {rot: ['y', dx * dz > 0 ? 45 : -45, [px, y + h / 2, pz]]});
    }
}

// ---- brake disc and caliper -------------------------------------------------------

k.model('brake_discs', {gui: [18, 203, 0]}, () => {
    // the vented rotor: two cast faces with the dark vanes between them, the hat standing out in front
    k.g('rotor');
    k.round('z', 8, 8, 8, 6.3, 2.4, HOLE, {sides: 16});
    k.round('z', 8, 8, 6.4, 6.5, 0.8, ROTOR, {sides: 16});
    k.round('z', 8, 8, 9.6, 6.5, 0.8, ROTOR, {sides: 16});
    k.round('z', 8, 8, 5.2, 3.1, 1.6, HAT, {sides: 16});
    k.round('z', 8, 8, 4.35, 1.0, 0.1, HOLE, {sides: 16});
    for (let i = 0; i < 5; i++) {
        const t = (90 + i * 72) * Math.PI / 180, x = 8 + 2.0 * Math.cos(t), y = 8 + 2.0 * Math.sin(t);
        k.box(x - 0.3, y - 0.3, 4.3, x + 0.3, y + 0.3, 4.4, HOLE);
    }
    // the swept faces: cross-drilled holes on every other line, a slot on the lines between
    k.g('drilling');
    for (let n = 0; n < 16; n++) {
        if (n % 2) {
            k.spin(n * 22.5, -0.15, 3.8, 5.9, 0.15, 5.9, 6.0, HOLE);
            k.spin(n * 22.5, -0.15, 3.8, 10.0, 0.15, 5.9, 10.1, HOLE);
            continue;
        }
        const t = n * 22.5 * Math.PI / 180;
        for (const r of [4.2, 5.5]) {
            const x = 8 - r * Math.sin(t), y = 8 + r * Math.cos(t);
            k.box(x - 0.25, y - 0.25, 5.9, x + 0.25, y + 0.25, 10.1, HOLE, {only: ['north', 'south']});
        }
    }
    // the caliper astride the rim, high on one side, a raised blank face and the bleed nipple on top
    k.g('caliper');
    k.spin(-22.5, -2.6, 4.4, 5.2, 2.6, 7.3, 10.8, CALIPER);
    k.spin(-22.5, -2.0, 4.9, 4.9, 2.0, 6.9, 5.2, CALIPER_FACE);
    k.spin(-22.5, -0.3, 7.3, 9.4, 0.3, 7.9, 10.0, C.CHROME);
    k.spin(-22.5, -1.6, 7.3, 10.8, 1.6, 7.6, 11.1, HOLE);
});

// ---- brake pads ---------------------------------------------------------------------

k.model('brake_pads', {}, () => {
    // a pair laid out as they come in the box, linings up, their spines facing: each a steel backing
    // plate with its ears, the lining bonded on with the groove down its middle
    for (const [spine, dir] of [[7.6, -1], [8.4, 1]]) {
        const z = v => spine + dir * v;
        k.g('backing');
        k.box(3.6, 7.0, z(0), 12.4, 7.6, z(4.4), BACKING);
        k.box(4.8, 7.0, z(4.4), 11.2, 7.6, z(5.0), BACKING);
        k.box(2.8, 7.0, z(1.6), 3.6, 7.6, z(3.0), BACKING);
        k.box(12.4, 7.0, z(1.6), 13.2, 7.6, z(3.0), BACKING);
        k.g('lining');
        k.box(4.6, 7.6, z(0.4), 11.4, 8.8, z(4.0), FRICTION);
        k.box(5.4, 7.6, z(4.0), 10.6, 8.8, z(4.6), FRICTION);
        k.box(7.85, 8.75, z(0.4), 8.15, 8.85, z(4.6), HOLE);
    }
    // the wear warning clip bent over the end of one
    k.g('clips');
    k.box(3.0, 7.0, 4.4, 3.8, 8.4, 5.0, C.CHROME);
});

// ---- brake fluid -----------------------------------------------------------------

k.model('brake_fluid', {}, () => {
    // a milky plastic bottle with soft corners, a shoulder, the neck and a black cap
    k.g('bottle');
    const BOTTLE = '#E8E6DC';
    k.box(5.0, 0, 6.4, 11.0, 8.6, 9.6, BOTTLE);
    k.box(5.4, 0, 6.0, 10.6, 8.6, 10.0, BOTTLE);
    k.box(5.6, 8.6, 6.5, 10.4, 9.3, 9.5, BOTTLE);
    k.box(6.6, 9.3, 7.0, 9.4, 9.8, 9.0, BOTTLE);
    k.round('y', 8, 10.2, 8, 0.9, 0.8, BOTTLE);
    k.g('cap');
    k.round('y', 8, 11.5, 8, 1.3, 2.0, C.BLACK, {sides: 16});
    k.round('y', 8, 12.55, 8, 0.9, 0.1, C.TRIM);
    // the label wrapped round, blank, with an amber band; the clear strip down the side shows the level
    k.g('label');
    k.box(4.9, 2.6, 6.3, 11.1, 7.0, 9.7, C.YELLOW);
    k.box(5.3, 2.6, 5.9, 10.7, 7.0, 10.1, C.YELLOW);
    k.box(4.85, 5.6, 6.25, 11.15, 6.1, 9.75, '#D49A1E');
    k.box(5.25, 5.6, 5.85, 10.75, 6.1, 10.15, '#D49A1E');
    k.box(11.0, 0.6, 7.6, 11.2, 8.2, 8.4, '#D9B45A');
});

// ---- suspension strut -----------------------------------------------------------------

k.model('suspension', {}, () => {
    // the bracket that clamps the knuckle, two bolts through its ears
    k.g('bracket');
    k.box(6.8, 2.8, 6.8, 9.2, 3.6, 9.2, FORGED);
    k.box(6.4, 0.9, 6.6, 6.9, 3.2, 9.4, FORGED);
    k.box(9.1, 0.9, 6.6, 9.6, 3.2, 9.4, FORGED);
    for (const y of [1.6, 2.6]) {
        k.box(6.15, y - 0.25, 7.7, 6.4, y + 0.25, 8.3, C.CHROME);
        k.box(9.6, y - 0.25, 7.7, 9.85, y + 0.25, 8.3, C.CHROME);
    }
    // the damper with its threaded collar and the lower spring seat
    k.g('damper');
    k.round('y', 8, 6.8, 8, 1.15, 6.8, DAMPER, {sides: 16});
    k.round('y', 8, 4.0, 8, 1.5, 0.8, C.ALU, {sides: 16});
    k.round('y', 8, 4.75, 8, 2.9, 0.45, C.ALU, {sides: 16});
    // the coil spring, the bump stop's boot showing inside it
    k.g('spring');
    for (let i = 0; i < 7; i++) {
        ring(8, 5.0 + i * 1.05, 8, 2.25, 0.55, 0.55, SPRING);
    }
    k.round('y', 8, 10.6, 8, 0.8, 2.8, RUBBER);
    // the upper seat, the rubber top mount, its plate and studs, the rod and its nut
    k.g('top');
    k.round('y', 8, 12.35, 8, 2.9, 0.45, C.STEEL, {sides: 16});
    k.round('y', 8, 13.0, 8, 2.4, 0.8, RUBBER, {sides: 16});
    k.round('y', 8, 13.7, 8, 2.7, 0.3, FORGED, {sides: 16});
    for (const t of [90, 210, 330]) {
        const x = 8 + 2.0 * Math.cos(t * Math.PI / 180), z = 8 + 2.0 * Math.sin(t * Math.PI / 180);
        k.box(x - 0.2, 13.85, z - 0.2, x + 0.2, 14.5, z + 0.2, C.CHROME);
    }
    k.round('y', 8, 14.25, 8, 0.35, 0.9, C.CHROME);
    k.round('y', 8, 14.85, 8, 0.55, 0.5, C.STEEL);
});

// ---- steering rack ---------------------------------------------------------------------

k.model('steering_rack', {}, () => {
    // the cast tube the rack slides in, the pinion housing at the driver's end with its splined shaft
    k.g('housing');
    k.round('x', 8, 8, 8, 0.95, 10, ALU_CAST);
    k.box(3.6, 7.2, 7.0, 5.8, 9.6, 9.4, ALU_CAST);
    k.round('y', 4.7, 10.2, 8.6, 0.45, 1.2, C.CHROME);
    k.round('y', 4.7, 10.1, 8.6, 0.5, 0.2, C.STEEL);
    k.round('y', 4.7, 10.5, 8.6, 0.5, 0.2, C.STEEL);
    k.sym(() => {
        // a rubber-bushed clamp each side, its strap bolted down through ears front and back
        k.g('mounts');
        k.round('x', 6.3, 8, 8, 1.2, 1.0, RUBBER);
        k.box(5.8, 8.8, 6.6, 6.8, 9.4, 9.4, C.STEEL);
        k.box(5.8, 7.6, 6.2, 6.8, 8.0, 9.8, C.STEEL);
        k.box(6.1, 8.0, 6.3, 6.5, 8.05, 6.7, HOLE);
        k.box(6.1, 8.0, 9.3, 6.5, 8.05, 9.7, HOLE);
        // the bellows over each end, then the tie rod out to its forged end and the ball joint pointing down
        k.g('boots');
        for (let i = 0; i < 6; i++) {
            k.round('x', 0.8 + i * 0.4, 8, 8, i % 2 ? 1.0 : 1.25, 0.4, RUBBER);
        }
        k.g('tie_rods');
        k.round('x', 0.25, 8, 8, 0.35, 1.1, C.CHROME);
        k.round('x', -0.15, 8, 8, 0.5, 0.3, C.STEEL);
        k.box(-1.5, 7.4, 7.2, -0.3, 8.6, 8.8, FORGED);
        k.round('y', -0.9, 7.0, 8, 0.7, 0.8, FORGED);
        k.round('y', -0.9, 6.6, 8, 0.6, 0.4, RUBBER);
        k.round('y', -0.9, 5.8, 8, 0.3, 1.2, C.CHROME);
        k.round('y', -0.9, 5.45, 8, 0.45, 0.5, C.STEEL);
    });
});

// ---- catalytic converter -----------------------------------------------------------

k.model('catalytic_converter', {}, () => {
    // the heat shield over the ceramic brick, pressed with three ribs, its welded ends
    k.g('shell');
    k.round('x', 8, 8, 8, 3.3, 7.6, SHIELD, {sides: 16});
    for (const x of [5.4, 8.0, 10.6]) {
        k.round('x', x, 8, 8, 3.45, 0.35, STAINLESS, {sides: 16});
    }
    k.sym(() => {
        k.round('x', 4.3, 8, 8, 3.4, 0.2, SEAM, {sides: 16});
        // a stepped cone down to the pipe, its two-bolt flange at the end
        k.g('cones');
        k.round('x', 3.9, 8, 8, 2.7, 0.6, STAINLESS, {sides: 16});
        k.round('x', 3.3, 8, 8, 2.0, 0.6, STAINLESS, {sides: 16});
        k.round('x', 2.7, 8, 8, 1.3, 0.6, STAINLESS);
        k.g('pipes');
        k.round('x', 1.2, 8, 8, 0.85, 2.4, C.STEEL);
        k.box(0, 6.2, 6.6, 0.4, 9.8, 9.4, C.DARK_STEEL);
        k.box(-0.1, 6.5, 7.75, 0.0, 7.0, 8.25, C.CHROME);
        k.box(-0.1, 9.0, 7.75, 0.0, 9.5, 8.25, C.CHROME);
        // an oxygen sensor screwed in each side of the brick, its lead off to the back
        k.g('sensors');
        k.round('y', 3.3, 10.4, 8, 0.55, 0.6, C.STEEL);
        k.round('y', 3.3, 11.1, 8, 0.4, 0.8, C.CHROME);
        k.round('y', 3.3, 12.0, 8, 0.5, 1.0, C.BLACK);
        k.box(3.1, 12.2, 8.0, 3.5, 12.6, 9.8, C.BLACK);
    });
});

// ---- exhaust ---------------------------------------------------------------------

k.model('exhaust', {}, () => {
    // the pipe in from the cat, flanged, through a resonator, into the back box, out the chrome tip
    k.g('pipe');
    k.round('x', 0.9, 8, 8, 0.7, 3.4, C.STEEL);
    k.round('x', -0.6, 8, 8, 1.2, 0.4, C.DARK_STEEL);
    k.round('x', 6.6, 8, 8, 0.7, 1.6, C.STEEL);
    k.round('x', 15.0, 8, 8, 0.75, 1.2, C.STEEL);
    k.g('resonator');
    k.round('x', 4.2, 8, 8, 1.5, 3.2, STAINLESS, {sides: 16});
    k.round('x', 2.7, 8, 8, 1.6, 0.2, SEAM, {sides: 16});
    k.round('x', 5.7, 8, 8, 1.6, 0.2, SEAM, {sides: 16});
    // the silencer: rolled seams round both ends and a pressed rib round the middle
    k.g('silencer');
    k.round('x', 10.9, 8, 8, 3.1, 7.0, STAINLESS, {sides: 16});
    k.round('x', 7.55, 8, 8, 3.2, 0.3, SEAM, {sides: 16});
    k.round('x', 14.25, 8, 8, 3.2, 0.3, SEAM, {sides: 16});
    k.round('x', 10.9, 8, 8, 3.2, 0.4, C.ALU, {sides: 16});
    // the hanger on top of the resonator, a rubber block on a rod
    k.g('hanger');
    k.box(4.0, 9.4, 7.8, 4.4, 10.4, 8.2, C.STEEL);
    k.box(3.7, 10.4, 7.5, 4.7, 11.1, 8.5, RUBBER);
    k.g('tip');
    k.round('x', 16.15, 8, 8, 1.0, 1.3, C.CHROME, {sides: 16});
    k.round('x', 16.83, 8, 8, 0.75, 0.06, HOLE, {sides: 16});
});

// ---- car body -------------------------------------------------------------------

k.model('car_body', {}, () => {
    // the shell as it leaves the body shop, grey in primer: no glass, no lights, no wheels
    k.g('lower');
    k.box(4.4, 5.2, 0.5, 11.6, 8.0, 2.2, PRIMER);
    k.box(4.4, 6.9, 2.2, 11.6, 8.0, 4.8, PRIMER);
    k.box(4.4, 4.8, 4.8, 11.6, 8.0, 11.0, PRIMER);
    k.box(4.4, 6.9, 11.0, 11.6, 8.0, 13.6, PRIMER);
    k.box(4.4, 5.2, 13.6, 11.6, 8.0, 15.5, PRIMER);
    // the arches: rounded at their corners, the wells dark inside them
    k.g('arches');
    for (const [z0, z1] of [[2.2, 4.8], [11.0, 13.6]]) {
        k.box(4.4, 6.3, z0, 11.6, 6.9, z0 + 0.45, PRIMER);
        k.box(4.4, 6.3, z1 - 0.45, 11.6, 6.9, z1, PRIMER);
        k.box(4.9, 5.2, z0, 11.1, 6.9, z1, HOLE);
    }
    // the cabin: pillars leaning in at windscreen and back light, the roof across them, open windows
    k.g('cabin');
    k.sym(() => {
        k.box(5.0, 8.0, 4.6, 5.5, 11.0, 5.1, PRIMER, {rot: ['x', 22.5, [5.25, 8.0, 4.85]]});
        k.box(5.0, 8.0, 8.0, 5.4, 10.9, 8.5, PRIMER);
        k.box(5.0, 8.0, 11.1, 5.6, 11.0, 11.9, PRIMER, {rot: ['x', -22.5, [5.3, 8.0, 11.5]]});
    });
    k.box(5.1, 10.6, 5.8, 10.9, 11.2, 10.5, PRIMER);
    k.box(5.0, 8.0, 4.4, 11.0, 8.3, 4.9, SEAM);
    k.box(5.5, 8.0, 5.1, 10.5, 8.08, 11.0, HOLE);
    // shut lines for the bonnet, the doors and the boot lid
    k.g('seams');
    k.sym(() => {
        for (const z of [4.85, 8.25, 11.0]) {
            k.box(4.35, 5.2, z - 0.05, 4.4, 8.0, z + 0.05, SEAM);
        }
        k.box(4.9, 8.0, 0.5, 5.0, 8.05, 4.4, SEAM);
        k.box(4.9, 8.0, 12.0, 5.0, 8.05, 15.5, SEAM);
    });
    k.box(4.9, 8.0, 11.95, 11.1, 8.05, 12.05, SEAM);
    // the empty sockets for the lamps and the grille, front and back
    k.g('sockets');
    k.sym(() => {
        k.box(4.8, 6.8, 0.45, 6.6, 7.6, 0.5, HOLE);
        k.box(4.8, 6.8, 15.5, 6.4, 7.6, 15.55, HOLE);
    });
    k.box(6.9, 5.9, 0.45, 9.1, 7.5, 0.5, HOLE);
});

// ---- pickup bed -----------------------------------------------------------------

k.model('pickup_bed', {}, () => {
    // the ribbed floor, the bulkhead at the cab end, the tailgate with its handle at the other
    k.g('floor');
    k.box(4.9, 6.0, 3.4, 11.1, 6.5, 12.6, PRIMER);
    for (const x of [5.9, 7.1, 8.9, 10.1]) {
        k.box(x - 0.15, 6.5, 3.6, x + 0.15, 6.65, 12.4, SHIELD);
    }
    k.box(4.4, 6.0, 3.0, 11.6, 9.4, 3.4, PRIMER);
    k.box(4.9, 6.0, 12.6, 11.1, 9.4, 13.0, PRIMER);
    k.box(7.2, 8.5, 13.0, 8.8, 8.9, 13.05, HOLE);
    k.box(4.4, 9.4, 3.0, 11.6, 9.7, 3.5, C.TRIM);
    k.sym(() => {
        // each side: the wall with the wheel arch cut out of it, the hump inside, the rail cap on top
        k.g('sides');
        k.box(4.4, 6.0, 3.4, 4.9, 9.4, 6.4, PRIMER);
        k.box(4.4, 7.0, 6.4, 4.9, 9.4, 10.0, PRIMER);
        k.box(4.4, 6.0, 10.0, 4.9, 9.4, 13.0, PRIMER);
        k.box(4.5, 6.0, 6.4, 6.0, 7.0, 10.0, HOLE);
        k.box(4.9, 6.5, 6.6, 6.0, 7.5, 9.8, PRIMER);
        k.box(4.3, 9.4, 3.5, 5.0, 9.7, 13.0, C.TRIM);
        k.box(4.5, 9.7, 3.7, 4.8, 9.75, 4.0, HOLE);
        k.box(4.5, 9.7, 12.4, 4.8, 9.75, 12.7, HOLE);
        // the tail lamp up the back edge, the shut line beside the tailgate
        k.g('lamps');
        k.box(4.4, 7.4, 13.0, 4.9, 9.2, 13.15, C.RED);
        k.box(4.9, 6.0, 12.95, 4.95, 9.4, 13.0, SEAM);
    });
});

// ---- tanker barrel ---------------------------------------------------------------

k.model('tanker_barrel', {}, () => {
    // the stainless tank with three rings round it and dished ends
    k.g('tank');
    k.round('z', 8, 7.6, 8, 4.1, 10, STAINLESS, {sides: 16});
    for (const z of [4.6, 8.0, 11.4]) {
        k.round('z', 8, 7.6, z, 4.25, 0.3, '#9AA0A6', {sides: 16});
    }
    k.sym(() => {
        k.round('z', 8, 7.6, 2.8, 3.5, 0.4, STAINLESS, {sides: 16});
        k.round('z', 8, 7.6, 2.45, 2.4, 0.3, STAINLESS, {sides: 16});
    }, 'z');
    // two manholes on top, the walkway either side of them, the yellow handrail along it
    k.g('top');
    for (const z of [5.8, 10.2]) {
        k.round('y', 8, 11.8, z, 1.1, 0.6, C.CHROME, {sides: 16});
        k.round('y', 8, 12.2, z, 0.85, 0.2, STAINLESS, {sides: 16});
        k.box(7.7, 11.8, z + 1.0, 8.3, 12.3, z + 1.3, C.DARK_STEEL);
    }
    k.sym(() => {
        k.box(6.0, 11.4, 3.4, 6.8, 11.6, 12.6, C.DARK_STEEL);
        for (const z of [4.0, 8.0, 12.0]) {
            k.box(5.9, 11.4, z - 0.1, 6.1, 12.9, z + 0.1, C.YELLOW);
        }
        k.box(5.9, 12.7, 3.9, 6.1, 12.9, 12.1, C.YELLOW);
    });
    // the ladder up the back end, stood off the dished end on brackets
    k.g('ladder');
    for (const x of [6.95, 9.05]) {
        k.box(x - 0.15, 4.2, 13.8, x + 0.15, 11.6, 14.1, C.STEEL);
        k.box(x - 0.1, 10.8, 13.0, x + 0.1, 11.0, 13.8, C.STEEL);
        k.box(x - 0.1, 5.0, 13.0, x + 0.1, 5.2, 13.8, C.STEEL);
    }
    for (let y = 5.0; y < 11.5; y += 1.2) {
        k.box(7.1, y, 13.85, 8.9, y + 0.2, 14.05, C.STEEL);
    }
    // the outlet valve under the middle, its red handle, the pipe out to the side with a capped coupling
    k.g('outlet');
    k.box(7.4, 3.0, 7.4, 8.6, 3.6, 8.6, C.DARK_STEEL);
    k.box(7.8, 2.8, 8.4, 8.2, 3.0, 10.0, C.RED);
    k.round('x', 10.1, 3.3, 8, 0.4, 3.0, C.STEEL);
    k.round('x', 11.7, 3.3, 8, 0.55, 0.4, C.CHROME);
});
