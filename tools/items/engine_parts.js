// What goes on and in an engine, each after the real part, lying as it sits
// in a car (its front to the north, towards the grille) or, for the loose
// insides, as it would lie on the bench.
const CASE = '#26282C', LEAD = '#8E9196', PLUS = '#C0392B', ALU_CAST = '#9EA3A9',
    CORE = '#7F858B', FIN = '#B3B8BD', CAST = '#6A5A50', FORGED = '#5B6168',
    FRICTION = '#7A6450', BRASS = '#B8933F', PLASTIC = C.PLASTIC, RUBBER = C.RUBBER;

// ---- radiator -------------------------------------------------------------------

k.model('radiator', {gui: [18, 203, 0]}, () => {
    // the aluminium core, its tubes running across between the two tanks
    k.g('core');
    k.box(3.0, 2.6, 7.0, 13.0, 13.4, 8.4, CORE);
    for (let y = 3.2; y < 13.2; y += 0.9) {
        k.box(3.0, y, 6.85, 13.0, y + 0.25, 7.0, FIN, {only: ['north', 'up', 'down']});
    }
    // the black plastic end tanks, crimped on with an aluminium seam
    k.g('tanks');
    k.sym(() => {
        k.box(1.5, 2.1, 6.6, 3.0, 13.9, 8.8, PLASTIC);
        k.box(2.9, 2.1, 6.5, 3.2, 13.9, 8.9, C.ALU);
        k.box(1.8, 1.3, 7.3, 2.6, 2.1, 8.1, RUBBER);
    });
    // the cap on the left tank, the top hose out of it, the bottom one out of the right
    k.g('necks');
    k.round('y', 2.25, 14.2, 7.7, 0.6, 0.6, C.CHROME);
    k.round('z', 2.25, 12.4, 9.6, 0.6, 1.6, PLASTIC);
    k.round('z', 13.75, 3.6, 9.6, 0.6, 1.6, PLASTIC);
    // the electric fan on the back in its shroud
    k.g('fan');
    k.box(3.4, 3.0, 8.4, 12.6, 13.0, 8.9, PLASTIC);
    k.round('z', 8, 8, 9.1, 4.4, 0.5, PLASTIC, {sides: 16});
    for (const turn of [0, 45, -45]) {
        k.box(4.4, 7.6, 9.2, 11.6, 8.4, 9.5, C.TRIM, {rot: ['z', turn, [8, 8, 9.35]]});
    }
    k.box(7.6, 4.4, 9.2, 8.4, 11.6, 9.5, C.TRIM);
    k.round('z', 8, 8, 9.7, 1.3, 1.0, C.BLACK);
});

// ---- battery --------------------------------------------------------------------

k.model('battery', {}, () => {
    k.g('case');
    k.box(2.6, 3.5, 4.8, 13.4, 11.2, 11.2, CASE);
    k.box(2.3, 11.2, 4.6, 13.7, 12.2, 11.4, '#34373C');
    // the maker's blue band round the front, left blank
    k.box(2.6, 6.0, 4.75, 13.4, 9.6, 4.8, C.BLUE, {only: ['north']});
    k.box(2.6, 9.6, 4.75, 13.4, 9.9, 4.8, C.WHITE, {only: ['north']});
    // the posts, red cap on the plus, and the green charge eye between the vent caps
    k.g('posts');
    k.round('y', 4.1, 12.6, 6.3, 0.55, 0.8, LEAD);
    k.box(10.9, 12.2, 5.3, 12.9, 13.0, 7.3, PLUS, {no: ['down']});
    k.round('y', 9.6, 12.25, 6.2, 0.45, 0.1, C.LED_GREEN, {sides: 16});
    for (const x of [4.6, 6.0, 10.0, 11.4]) {
        k.box(x - 0.45, 12.2, 8.6, x + 0.45, 12.4, 9.5, PLASTIC);
    }
    // the carrying strap lying flat across the lid
    k.g('handle');
    k.box(7.3, 12.2, 4.5, 8.7, 12.5, 11.5, C.BLACK);
    k.box(7.3, 9.6, 11.2, 8.7, 12.5, 11.5, C.BLACK);
    k.box(7.3, 9.6, 4.5, 8.7, 12.5, 4.8, C.BLACK);
});

// ---- alternator -----------------------------------------------------------------

k.model('alternator', {}, () => {
    // two cast halves round the shaft, a dark seam between, a vent slot on the front
    k.g('housing');
    k.round('z', 8, 8, 6.9, 4.2, 3.0, ALU_CAST, {sides: 16});
    k.round('z', 8, 8, 8.5, 4.3, 0.2, C.DARK_STEEL, {sides: 16});
    k.round('z', 8, 8, 10.0, 4.0, 2.8, ALU_CAST, {sides: 16});
    for (const turn of [0, 45, -45]) {
        k.box(4.0, 7.7, 5.3, 12.0, 8.3, 5.4, C.BLACK, {rot: ['z', turn, [8, 8, 5.35]], only: ['north']});
    }
    k.box(7.7, 4.0, 5.3, 8.3, 12.0, 5.4, C.BLACK, {only: ['north']});
    // the fan and the ribbed pulley on the shaft
    k.g('pulley');
    k.round('z', 8, 8, 5.1, 3.0, 0.6, C.STEEL, {sides: 16});
    k.round('z', 8, 8, 3.95, 1.3, 1.6, FORGED, {sides: 16});
    for (const z of [3.45, 3.95, 4.45]) {
        k.round('z', 8, 8, z, 1.45, 0.2, C.DARK_STEEL, {sides: 16});
    }
    k.round('z', 8, 8, 3.0, 0.5, 0.6, C.CHROME);
    // the ears it bolts on by, above and below
    k.g('ears');
    k.box(6.9, 11.8, 6.0, 9.1, 14.2, 7.6, ALU_CAST);
    k.round('z', 8, 13.3, 6.8, 0.5, 1.8, C.STEEL);
    k.box(6.9, 1.8, 9.2, 9.1, 4.2, 10.8, ALU_CAST);
    k.round('z', 8, 2.7, 10.0, 0.5, 1.8, C.STEEL);
    // the black back cover with the charge stud in its red boot and the plug
    k.g('back');
    k.round('z', 8, 8, 11.9, 3.3, 1.0, PLASTIC, {sides: 16});
    k.box(9.4, 9.2, 12.4, 10.6, 10.4, 12.9, PLUS);
    k.box(5.2, 7.4, 12.4, 6.6, 9.0, 12.9, '#5A5E64');
});

// ---- oil filter -----------------------------------------------------------------

k.model('oil_filter', {}, () => {
    k.g('can');
    k.round('y', 8, 7.4, 8, 3.6, 7.4, C.BLUE, {sides: 16});
    k.round('y', 8, 11.3, 8, 3.2, 0.4, C.BLUE, {sides: 16});
    // a pale label band, and the gripping flutes round the domed end
    k.round('y', 8, 7.0, 8, 3.65, 3.2, C.WHITE, {sides: 16});
    k.round('y', 8, 7.0, 8, 3.7, 0.6, C.BLUE_DARK, {sides: 16});
    for (const turn of [0, 22.5, 45, -22.5]) {
        k.box(4.3, 10.0, 7.7, 11.7, 11.1, 8.3, C.BLUE_DARK, {rot: ['y', turn, [8, 10.5, 8]]});
    }
    // the rolled seam and the base plate it screws on by
    k.g('base');
    k.round('y', 8, 3.9, 8, 3.7, 0.5, C.ALU, {sides: 16});
    k.round('y', 8, 3.55, 8, 3.0, 0.3, RUBBER, {sides: 16});
});

// ---- spark plugs ----------------------------------------------------------------

k.model('spark_plugs', {}, () => {
    for (const x of [3.4, 6.5, 9.5, 12.6]) {
        k.g('plug');
        k.box(x - 0.1, 3.4, 7.7, x + 0.5, 3.7, 8.3, C.STEEL);
        k.round('y', x, 4.7, 8, 0.6, 2.0, C.DARK_STEEL);
        k.round('y', x, 5.75, 8, 0.85, 0.3, C.CHROME);
        k.round('y', x, 6.5, 8, 0.95, 1.2, C.ALU);
        k.round('y', x, 9.4, 8, 0.6, 4.6, C.WHITE);
        for (const y of [9.6, 10.4, 11.2]) {
            k.round('y', x, y, 8, 0.68, 0.3, '#D3D5D8');
        }
        k.round('y', x, 12.0, 8, 0.35, 0.7, C.CHROME);
    }
});

// ---- air filter -----------------------------------------------------------------

k.model('air_filter', {}, () => {
    // the black air box: a lower half and a lid held on by four clips
    k.g('box');
    k.box(2.6, 4.9, 2.6, 13.4, 7.9, 13.4, PLASTIC);
    k.box(2.4, 7.9, 2.4, 13.6, 8.3, 13.6, C.TRIM);
    k.box(2.8, 8.3, 2.8, 13.2, 10.1, 13.2, PLASTIC);
    for (const x of [4.0, 6.0, 8.0, 10.0, 12.0]) {
        k.box(x - 0.2, 10.1, 3.8, x + 0.2, 10.3, 12.2, C.TRIM);
    }
    k.sym(() => {
        for (const z of [5.0, 11.0]) {
            k.box(2.2, 7.0, z - 0.5, 2.6, 9.2, z + 0.5, C.STEEL);
        }
    });
    // the snout to the grille and the duct out to the engine
    k.g('ducts');
    k.box(6.0, 5.6, 0.6, 10.0, 7.6, 2.6, PLASTIC);
    k.round('x', 1.6, 8.4, 9.6, 1.6, 2.0, RUBBER, {sides: 16});
    for (const x of [0.9, 1.6, 2.3]) {
        k.round('x', x, 8.4, 9.6, 1.75, 0.25, RUBBER, {sides: 16});
    }
});

k.model('air_filter_open', {}, () => {
    // a cone filter for the end of the intake pipe: a red pleated element
    // between a rubber flange and a domed cap
    k.g('flange');
    k.round('x', 2.4, 8, 8, 1.7, 1.6, RUBBER, {sides: 16});
    k.round('x', 1.9, 8, 8, 1.85, 0.4, C.CHROME, {sides: 16});
    k.round('x', 3.6, 8, 8, 3.1, 0.6, RUBBER, {sides: 16});
    k.g('element');
    const steps = [[4.6, 3.0], [5.8, 2.85], [7.0, 2.7], [8.2, 2.55], [9.4, 2.4], [10.6, 2.25], [11.8, 2.1]];
    for (const [x, r] of steps) {
        k.round('x', x, 8, 8, r, 1.2, C.RED, {sides: 16});
        k.round('x', x + 0.6, 8, 8, r + 0.1, 0.15, C.RED_DARK, {sides: 16});
    }
    k.g('cap');
    k.round('x', 12.7, 8, 8, 2.1, 0.6, RUBBER, {sides: 16});
    k.round('x', 13.2, 8, 8, 1.6, 0.4, C.CHROME, {sides: 16});
});

// ---- turbo ----------------------------------------------------------------------

k.model('turbo', {}, () => {
    // the hot side: a cast-iron snail, its flange underneath, the outlet out the end
    k.g('turbine');
    k.round('x', 5.0, 8, 8, 3.3, 2.6, CAST, {sides: 16});
    k.box(4.0, 3.6, 8.6, 6.0, 6.0, 11.2, CAST);
    k.box(3.7, 3.2, 8.4, 6.3, 3.6, 11.4, C.DARK_STEEL);
    k.round('x', 3.2, 8, 8, 1.9, 1.0, CAST, {sides: 16});
    k.round('x', 2.6, 8, 8, 2.3, 0.3, C.DARK_STEEL, {sides: 16});
    // the oiled centre between them
    k.g('centre');
    k.round('x', 7.6, 8, 8, 1.7, 2.6, C.STEEL, {sides: 16});
    k.box(7.3, 9.6, 7.7, 7.9, 12.2, 8.3, C.CHROME);
    // the cold side: an aluminium snail, the wheel showing in its mouth, the outlet up and forward
    k.g('compressor');
    k.round('x', 10.3, 8, 8, 3.5, 2.6, C.ALU, {sides: 16});
    k.round('x', 12.2, 8, 8, 2.2, 1.4, C.ALU, {sides: 16});
    k.round('x', 12.6, 8, 8, 1.8, 0.7, C.DARK_STEEL, {sides: 16});
    k.round('x', 12.75, 8, 8, 0.5, 0.6, C.CHROME);
    k.box(9.4, 9.8, 4.6, 11.2, 12.0, 8.0, C.ALU);
    k.round('z', 10.3, 10.9, 4.1, 1.1, 1.0, C.ALU);
    // the wastegate can, plated brass, on a bracket off the compressor's side; its rod runs back to the arm on the hot side
    k.g('wastegate');
    k.round('x', 10.9, 6.4, 3.7, 1.05, 1.8, BRASS, {sides: 16});
    k.round('x', 10.9, 6.4, 3.7, 1.2, 0.25, '#94742F', {sides: 16});
    k.box(11.0, 7.3, 3.5, 11.4, 9.6, 3.9, RUBBER);
    k.box(11.0, 9.3, 3.5, 11.4, 9.7, 4.8, RUBBER);
    k.box(9.9, 5.8, 3.9, 10.2, 7.0, 4.8, C.DARK_STEEL);
    k.box(5.7, 6.25, 3.45, 10.1, 6.55, 3.75, C.CHROME);
    k.box(5.3, 5.6, 3.4, 5.7, 6.8, 4.9, C.STEEL);
});

// ---- timing belt ----------------------------------------------------------------

k.model('timing_belt', {}, () => {
    // a toothed belt as it comes in its box, a long loop on its edge
    const R = 3.3, w = 0.6, half = 0.3;
    k.g('belt');
    for (const [yc, up] of [[11.6, 1], [4.0, -1]]) {
        for (const phi of [-45, -22.5, 0, 22.5, 45]) {
            const a = phi * Math.PI / 180;
            const y = yc + up * R * Math.cos(a), z = 8 + R * Math.sin(a);
            k.box(7, y - w / 2, z - 0.7, 9, y + w / 2, z + 0.7, RUBBER, {rot: ['x', up * phi, [8, y, z]]});
        }
        for (const side of [-1, 1]) {
            const a = 67.5 * Math.PI / 180;
            const y = yc + up * R * Math.cos(a), z = 8 + side * R * Math.sin(a);
            k.box(7, y - 0.7, z - w / 2, 9, y + 0.7, z + w / 2, RUBBER, {rot: ['x', -side * up * 22.5, [8, y, z]]});
        }
    }
    k.box(7, 3.4, 8 - R - half, 9, 12.2, 8 - R + half, RUBBER);
    k.box(7, 3.4, 8 + R - half, 9, 12.2, 8 + R + half, RUBBER);
    // the teeth inside the straight runs
    for (let y = 4.4; y < 11.4; y += 0.8) {
        k.box(7.1, y, 8 - R + half, 8.9, y + 0.35, 8 - R + half + 0.25, C.TRIM);
        k.box(7.1, y, 8 + R - half - 0.25, 8.9, y + 0.35, 8 + R - half, C.TRIM);
    }
});

// ---- head gasket ----------------------------------------------------------------

k.model('head_gasket', {gui: [62, 200, 0]}, () => {
    // a multi-layer steel sheet: four bores ringed in fire rings, bolt holes
    // round them, the water and oil ways between
    const y0 = 7.5, y1 = 7.8, d = 2.9, pitch = 3.35, h = d / 2;
    const xs = [8 - 1.5 * pitch, 8 - 0.5 * pitch, 8 + 0.5 * pitch, 8 + 1.5 * pitch];
    const SHEET = '#9DA2A8', FIRE = '#C7CCD1', HOLE = '#2A2C30';
    k.g('sheet');
    k.box(0.6, y0, 4, 15.4, y1, 8 - h, SHEET);
    k.box(0.6, y0, 8 + h, 15.4, y1, 12, SHEET);
    k.box(0.6, y0, 8 - h, xs[0] - h, y1, 8 + h, SHEET);
    for (let i = 0; i < 3; i++) {
        k.box(xs[i] + h, y0, 8 - h, xs[i + 1] - h, y1, 8 + h, SHEET);
    }
    k.box(xs[3] + h, y0, 8 - h, 15.4, y1, 8 + h, SHEET);
    const leg = d / (2 + Math.SQRT2), s = leg * Math.SQRT2;
    for (const x of xs) {
        k.g('bores');
        for (const [dx, dz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) {
            const cx = x + dx * h, cz = 8 + dz * h;
            k.box(cx - s / 2, y0, cz - s / 2, cx + s / 2, y1, cz + s / 2, SHEET, {rot: ['y', 45, [cx, y0, cz]]});
        }
        // the fire ring just outside the bore
        const r = h + 0.12, side = 2 * r * Math.tan(Math.PI / 8);
        k.box(x - side / 2, y1, 8 - r - 0.12, x + side / 2, y1 + 0.1, 8 - r + 0.12, FIRE);
        k.box(x - side / 2, y1, 8 + r - 0.12, x + side / 2, y1 + 0.1, 8 + r + 0.12, FIRE);
        k.box(x - r - 0.12, y1, 8 - side / 2, x - r + 0.12, y1 + 0.1, 8 + side / 2, FIRE);
        k.box(x + r - 0.12, y1, 8 - side / 2, x + r + 0.12, y1 + 0.1, 8 + side / 2, FIRE);
        const diag = r * Math.SQRT1_2;
        for (const [dx, dz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) {
            const cx = x + dx * diag, cz = 8 + dz * diag;
            k.box(cx - side / 2, y1, cz - 0.12, cx + side / 2, y1 + 0.1, cz + 0.12, FIRE,
                {rot: ['y', dx * dz > 0 ? 45 : -45, [cx, y1, cz]]});
        }
    }
    k.g('holes');
    for (const x of [1.9, 8 - pitch, 8, 8 + pitch, 14.1]) {
        for (const z of [4.7, 11.3]) {
            k.round('y', x, y0 + 0.16, z, 0.4, 0.34, HOLE);
        }
    }
    for (const x of [xs[0] + 0.9, xs[1] + 0.9, xs[2] + 0.9, xs[3] - 0.9]) {
        k.box(x - 0.35, y0 + 0.01, 10.0, x + 0.35, y1 + 0.01, 10.5, HOLE, {only: ['up', 'down']});
    }
});

// ---- crankshaft -----------------------------------------------------------------

k.model('crankshaft', {}, () => {
    // a four-throw crank on its side: five main journals, the pins two up and
    // two down, each between a pair of webs with its counterweight opposite
    const throws = [3.6, 6.4, 9.6, 12.4], ups = [1, -1, -1, 1];
    k.g('mains');
    for (const x of [2.2, 5.0, 8.0, 11.0, 13.8]) {
        k.round('x', x, 8, 8, 0.85, 1.0, C.CHROME, {sides: 16});
    }
    throws.forEach((x, i) => {
        const up = ups[i];
        k.g('throw');
        k.round('x', x, 8 + up * 2.0, 8, 0.8, 1.0, C.CHROME, {sides: 16});
        for (const side of [-1, 1]) {
            const wx = x + side * 0.8;
            // the web out to the pin, and the counterweight, wider, the other way
            k.box(wx - 0.3, 8 - up * 0.7, 6.9, wx + 0.3, 8 + up * 3.0, 9.1, FORGED);
            k.box(wx - 0.3, 8 - up * 0.4, 5.8, wx + 0.3, 8 - up * 2.8, 10.2, FORGED);
            k.box(wx - 0.3, 8 - up * 2.4, 6.6, wx + 0.3, 8 - up * 3.1, 9.4, FORGED);
        }
    });
    // the timing gear and snout at the front, the flywheel flange at the back
    k.g('ends');
    k.round('x', 1.2, 8, 8, 1.3, 0.8, C.DARK_STEEL, {sides: 16});
    k.round('x', 0.4, 8, 8, 0.55, 0.9, C.CHROME);
    k.round('x', 14.9, 8, 8, 2.4, 0.8, FORGED, {sides: 16});
    for (const [dy, dz] of [[1.5, 0], [-1.5, 0], [0, 1.5], [0, -1.5]]) {
        k.box(15.25, 8 + dy - 0.25, 8 + dz - 0.25, 15.35, 8 + dy + 0.25, 8 + dz + 0.25, C.BLACK, {only: ['east']});
    }
});

// ---- pistons --------------------------------------------------------------------

k.model('pistons', {}, () => {
    // a set of four, each on its connecting rod
    for (const x of [3.0, 6.33, 9.67, 13.0]) {
        k.g('piston');
        k.round('y', x, 10.0, 8, 1.5, 3.4, C.ALU, {sides: 16});
        for (const y of [11.0, 10.55, 10.1]) {
            k.round('y', x, y, 8, 1.53, 0.15, C.DARK_STEEL, {sides: 16});
        }
        k.round('y', x, 11.75, 8, 1.35, 0.1, '#B9BDC2', {sides: 16});
        k.round('z', x, 9.0, 8, 0.45, 3.1, C.CHROME);
        k.g('rod');
        k.box(x - 0.45, 3.4, 7.6, x + 0.45, 8.6, 8.4, FORGED);
        k.box(x - 0.2, 3.8, 7.5, x + 0.2, 8.2, 8.5, C.STEEL);
        k.round('z', x, 3.0, 8, 1.2, 1.0, FORGED, {sides: 16});
        k.round('z', x, 3.0, 8, 0.65, 1.1, C.DARK_STEEL);
        k.box(x - 1.1, 2.3, 7.6, x + 1.1, 2.6, 8.4, C.STEEL);
    }
});

// ---- clutch ---------------------------------------------------------------------

k.model('clutch', {gui: [18, 203, 0]}, () => {
    // the pressure plate as you see it from the engine, the disc behind it
    k.g('disc');
    k.round('z', 8, 8, 8.75, 6.1, 0.5, FRICTION, {sides: 16});
    k.round('z', 8, 8, 9.15, 1.4, 0.5, C.STEEL, {sides: 16});
    k.g('cover');
    k.round('z', 8, 8, 7.8, 6.5, 1.4, C.STEEL, {sides: 16});
    k.round('z', 8, 8, 7.05, 4.4, 0.2, C.ALU, {sides: 16});
    for (const turn of [0, 45, -45]) {
        k.box(1.2, 7.4, 7.4, 14.8, 8.6, 8.4, C.STEEL, {rot: ['z', turn, [8, 8, 7.9]]});
    }
    // the diaphragm spring's fingers pointing in to the release hole
    k.g('fingers');
    for (const turn of [0, 22.5, 45, -22.5, -45]) {
        for (const up of [1, -1]) {
            const a = up > 0 ? [9.2, 12.0] : [4.0, 6.8];
            k.box(7.7, a[0], 6.75, 8.3, a[1], 6.95, C.CHROME, {rot: ['z', turn, [8, 8, 6.85]]});
        }
    }
    for (const turn of [22.5, -22.5]) {
        k.box(9.2, 7.7, 6.75, 12.0, 8.3, 6.95, C.CHROME, {rot: ['z', turn, [8, 8, 6.85]]});
        k.box(4.0, 7.7, 6.75, 6.8, 8.3, 6.95, C.CHROME, {rot: ['z', turn, [8, 8, 6.85]]});
    }
    k.box(9.2, 7.7, 6.75, 12.0, 8.3, 6.95, C.CHROME);
    k.box(4.0, 7.7, 6.75, 6.8, 8.3, 6.95, C.CHROME);
    k.round('z', 8, 8, 6.85, 1.2, 0.3, C.DARK_STEEL, {sides: 16});
});

// ---- driveshaft -----------------------------------------------------------------

k.model('driveshaft', {}, () => {
    // a prop shaft: the tube, a universal joint at each end, the slip yoke in front, a flange behind
    k.g('tube');
    k.round('x', 8.6, 8, 8, 1.15, 11.4, C.DARK_STEEL, {sides: 16});
    for (const x of [3.1, 14.1]) {
        k.round('x', x, 8, 8, 1.3, 0.4, C.STEEL, {sides: 16});
    }
    for (const [c, dir] of [[1.9, -1], [15.3, 1]]) {
        k.g('joint');
        // the cross, its upright arms in the tube's yoke, its level ones in the end's
        k.box(c - 0.4, 6.0, 7.5, c + 0.4, 10.0, 8.5, C.CHROME);
        k.box(c - 0.4, 7.5, 6.0, c + 0.4, 8.5, 10.0, C.CHROME);
        const tube = c - dir * 1.15, end = c + dir * 1.15;
        k.box(tube - 0.3, 6.2, 7.0, tube + 0.3, 9.8, 9.0, C.STEEL);
        for (const [y0, y1] of [[5.8, 6.6], [9.4, 10.2]]) {
            k.box(Math.min(c - 0.6, tube), y0, 7.2, Math.max(c + 0.6, tube), y1, 8.8, C.STEEL);
        }
        k.box(end - 0.3, 7.0, 6.2, end + 0.3, 9.0, 9.8, C.STEEL);
        for (const [z0, z1] of [[5.8, 6.6], [9.4, 10.2]]) {
            k.box(Math.min(c - 0.6, end), 7.2, z0, Math.max(c + 0.6, end), 8.8, z1, C.STEEL);
        }
    }
    k.g('ends');
    k.round('x', -0.1, 8, 8, 1.0, 1.6, C.CHROME, {sides: 16});
    for (const x of [-0.6, 0.0, 0.6]) {
        k.round('x', x, 8, 8, 1.05, 0.15, C.STEEL, {sides: 16});
    }
    k.round('x', 16.6, 8, 8, 2.2, 0.4, C.STEEL, {sides: 16});
});

// ---- gearbox --------------------------------------------------------------------

k.model('transmission', {}, () => {
    // a five-speed manual for a front-engined car: the bell housing flaring to
    // the engine, the ribbed case, the tail housing, the lever on top
    k.g('bell');
    k.round('z', 8, 7.6, 2.0, 5.2, 2.2, ALU_CAST, {sides: 16});
    k.round('z', 8, 7.6, 3.9, 4.2, 1.6, ALU_CAST, {sides: 16});
    k.round('z', 8, 7.6, 0.95, 5.4, 0.3, C.STEEL, {sides: 16});
    k.round('z', 8, 7.6, 0.9, 1.0, 0.6, C.CHROME);
    k.g('case');
    k.box(4.9, 3.6, 4.6, 11.1, 10.4, 11.4, ALU_CAST);
    for (const z of [6.0, 7.6, 9.2, 10.8]) {
        k.box(4.6, 4.0, z - 0.2, 11.4, 10.0, z + 0.2, '#8E949A');
    }
    k.round('x', 4.6, 4.6, 9.8, 0.4, 0.4, C.DARK_STEEL);
    k.g('tail');
    k.round('z', 8, 7.0, 13.3, 1.9, 3.8, ALU_CAST, {sides: 16});
    k.round('z', 8, 7.0, 15.4, 1.2, 0.6, C.STEEL, {sides: 16});
    k.round('z', 8, 7.0, 14.8, 0.4, 1.2, C.DARK_STEEL);
    k.g('lever');
    k.box(6.6, 10.4, 7.6, 9.4, 11.0, 10.4, ALU_CAST);
    k.box(7.0, 11.0, 8.0, 9.0, 12.0, 10.0, RUBBER);
    k.box(7.6, 12.0, 8.6, 8.4, 12.6, 9.4, RUBBER);
    k.box(7.8, 12.6, 8.8, 8.2, 13.8, 9.2, C.CHROME);
    k.round('y', 8, 14.1, 9.0, 0.6, 0.7, C.BLACK, {sides: 16});
    k.g('mount');
    k.box(6.8, 4.3, 12.4, 9.2, 5.1, 14.0, RUBBER);
});
