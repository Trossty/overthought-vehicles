// The engines, after real ones, each lying as it sits in a car: its pulleys
// to the north (towards the radiator), its flywheel to the south, the intake
// on the east and the exhaust on the west.
//   engine                a modern twin-cam four: red crinkle cam cover with
//                         a coil on each plug, a cast intake, a 4-1 header
//   engine_inline6        the long straight six: silver ribbed cover, a
//                         heat shield over the exhaust
//   engine_v6             a 90 degree V6 under a black plastic intake
//   engine_v8             the classic small-block: orange paint, chrome rocker
//                         covers and a round chrome air cleaner on a carburettor
//   ..._diesel            the same blocks in diesel dress: no coils, injectors
//                         and their steel pipes, a turbocharger, a pump
//   engine_electric       an EV drive unit: motor, gearbox, inverter, the
//                         orange high-voltage cables
const IRON = '#5D6167', IRON_SHADE = '#4E5258', HEAD = '#9EA3A9', PAN = '#3E4349',
    DIESEL = '#4B5563', DIESEL_SHADE = '#3F4854', ORANGE = '#C9581C', ORANGE_SHADE = '#A84816',
    CRINKLE = '#A3281F', HEADER = '#76655A', CAST = '#6E5246', TURBINE = '#6A5A50',
    PLASTIC = C.PLASTIC, RUBBER = C.RUBBER, HV = '#E8731C';

/** A pulley with its belt round it, on the front of the engine. */
function pulley(x, y, z, r) {
    k.round('z', x, y, z, r, 0.6, RUBBER, {sides: 16});
    k.round('z', x, y, z - 0.1, r - 0.35, 0.6, C.STEEL, {sides: r > 1 ? 16 : 8});
    k.round('z', x, y, z - 0.2, 0.35, 0.6, C.CHROME);
}

/** The front of an engine: a timing cover, the crank and water-pump pulleys and the belt over them. */
function front(z0, top, paint) {
    k.g('front');
    k.box(5.3, 2.9, z0 - 0.6, 10.7, top, z0, paint || PLASTIC);
    pulley(8, 3.6, z0 - 0.9, 1.4);
    pulley(8, 6.9, z0 - 0.9, 1.05);
    k.box(6.68, 3.6, z0 - 1.2, 6.95, 6.9, z0 - 0.6, RUBBER);
    k.box(9.05, 3.6, z0 - 1.2, 9.32, 6.9, z0 - 0.6, RUBBER);
}

/** The flywheel's housing flange at the back, where the gearbox bolts on. */
function back(z1, paint) {
    k.g('flywheel');
    k.round('z', 8, 3.8, z1 + 0.3, 3.1, 0.6, paint, {sides: 16});
    k.round('z', 8, 3.8, z1 + 0.45, 1.2, 0.6, C.DARK_STEEL, {sides: 16});
}

/** The rubber mounts either side of the block. */
function mounts(inner) {
    k.g('mounts');
    k.sym(() => k.box(inner - 1.0, 2.7, 7.2, inner, 3.9, 8.8, RUBBER));
}

/** A turbocharger on its side: the hot snail at z, the cold one in front of it. */
function turbo(x, y, z, axis) {
    k.g('turbo');
    const along = (d, r, len, paint) => axis === 'x'
        ? k.round('x', x + d, y, z, r, len, paint, {sides: 16})
        : k.round('z', x, y, z + d, r, len, paint, {sides: 16});
    along(0, 1.35, 1.1, TURBINE);
    along(-0.9, 0.6, 0.8, C.STEEL);
    along(-1.7, 1.35, 0.9, C.ALU);
    along(-2.35, 0.65, 0.5, C.DARK_STEEL);
}

// ---- in line --------------------------------------------------------------------

function inline(name, o) {
    const n = o.cyl, pitch = 2.25;
    const len = n * pitch + 1.2, z0 = 8 - len / 2, z1 = 8 + len / 2;
    const cz = i => z0 + 0.6 + pitch * (i + 0.5);
    k.model(name, {}, () => {
        k.g('sump');
        k.box(5.3, 0, z0 + len * 0.42, 10.7, 1.3, z1 - 0.4, PAN);
        k.box(5.0, 1.3, z0 + 0.4, 11.0, 2.4, z1 - 0.3, PAN);

        k.g('block');
        k.box(5.1, 2.4, z0, 10.9, 4.8, z1, o.block);
        k.box(4.6, 4.8, z0, 11.4, 7.7, z1, o.block);
        // a boss down the side over each bore
        k.sym(() => {
            for (let i = 0; i < n; i++) {
                k.box(4.35, 5.2, cz(i) - 0.65, 4.6, 7.4, cz(i) + 0.65, o.shade);
            }
        });

        k.g('head');
        k.box(4.8, 7.7, z0 + 0.15, 11.2, 9.5, z1 - 0.15, HEAD);
        k.g('cover');
        k.box(5.3, 9.5, z0 + 0.4, 10.7, 10.5, z1 - 0.4, o.cover);
        k.box(5.7, 10.5, z0 + 0.6, 10.3, 10.8, z1 - 0.6, o.cover);
        if (o.ribs) {
            for (const x of [6.0, 9.6]) {
                k.box(x, 10.8, z0 + 0.9, x + 0.4, 11.0, z1 - 2.2, C.CHROME);
            }
        }
        k.round('y', 6.4, 11.0, z1 - 1.4, 0.55, 0.4, o.diesel ? C.YELLOW : PLASTIC);

        if (o.diesel) {
            // an injector in each cylinder and its steel pipe from the rail
            k.g('injectors');
            k.box(9.9, 10.8, z0 + 1.0, 10.5, 11.4, z1 - 1.0, C.CHROME);
            for (let i = 0; i < n; i++) {
                k.round('y', 8, 11.1, cz(i), 0.4, 0.6, C.ALU);
                k.box(8.4, 11.1, cz(i) - 0.12, 9.9, 11.3, cz(i) + 0.12, C.CHROME);
            }
        } else {
            // a coil on every plug, down the black spine of the cover
            k.g('coils');
            k.box(7.1, 10.8, z0 + 0.8, 8.9, 10.9, z1 - 0.8, C.BLACK);
            for (let i = 0; i < n; i++) {
                k.box(7.4, 10.9, cz(i) - 0.55, 8.6, 11.6, cz(i) + 0.55, PLASTIC);
                k.box(8.6, 11.05, cz(i) - 0.3, 9.1, 11.45, cz(i) + 0.3, C.TRIM);
            }
            k.box(9.1, 10.8, cz(0) - 0.4, 9.5, 11.3, cz(n - 1) + 0.4, C.BLACK);
        }

        // the intake: a runner out of each port, down into the plenum, the throttle in front
        k.g('intake');
        for (let i = 0; i < n; i++) {
            k.box(11.2, 8.0, cz(i) - 0.5, 12.5, 9.1, cz(i) + 0.5, o.intake);
            k.box(12.5, 6.5, cz(i) - 0.5, 13.3, 9.1, cz(i) + 0.5, o.intake);
        }
        k.box(12.3, 4.9, z0 + 0.5, 13.7, 6.7, z1 - 0.5, o.intake);
        k.round('z', 13.0, 5.8, z0 - 0.1, 0.8, 1.2, C.ALU);
        k.box(11.4, 5.5, z0 + 0.25, 11.65, 10.3, z0 + 0.5, C.STEEL);
        k.box(11.15, 10.3, z0 + 0.1, 11.9, 10.75, z0 + 0.65, C.YELLOW);

        k.g('exhaust');
        if (o.exhaust === 'shield') {
            // a cast manifold behind a ribbed heat shield
            k.box(3.9, 7.0, z0 + 0.8, 4.8, 8.8, z1 - 0.8, CAST);
            k.box(3.0, 5.4, z0 + 0.6, 3.9, 9.2, z1 - 0.6, C.ALU);
            for (const y of [6.0, 7.1, 8.2]) {
                k.box(2.8, y, z0 + 0.8, 3.0, y + 0.45, z1 - 0.8, C.CHROME);
            }
            k.box(3.0, 1.8, z1 - 1.6, 4.0, 5.4, z1 - 0.6, CAST);
        } else if (o.exhaust === 'log') {
            // a cast log with a flange at each port, the turbo on its back end
            k.box(3.9, 7.0, z0 + 0.8, 4.8, 8.8, z1 - 0.8, CAST);
            for (let i = 0; i < n; i++) {
                k.box(3.6, 7.3, cz(i) - 0.45, 3.9, 8.5, cz(i) + 0.45, CAST);
            }
            k.box(3.0, 1.8, z1 - 1.5, 4.0, 7.0, z1 - 0.5, CAST);
        } else {
            // a stainless four-into-one header
            for (let i = 0; i < n; i++) {
                k.box(3.9, 8.0, cz(i) - 0.4, 4.8, 8.9, cz(i) + 0.4, HEADER);
                k.box(3.2, 5.2, cz(i) - 0.4, 4.1, 8.9, cz(i) + 0.4, HEADER);
            }
            k.box(3.1, 4.2, cz(0) - 0.5, 4.2, 5.3, z1 - 0.2, HEADER);
            k.box(3.15, 1.8, z1 - 1.3, 4.15, 4.2, z1 - 0.2, HEADER);
        }
        if (o.diesel) {
            turbo(3.6, 10.0, z1 - 1.0, 'z');
            // the injection pump, low on the intake side, driven off the front
            k.g('pump');
            k.box(10.9, 2.9, z0 + 0.4, 12.2, 4.8, z0 + 2.4, C.ALU);
            k.round('z', 11.55, 3.85, z0 - 0.05, 0.75, 0.9, C.STEEL);
        }

        front(z0, 10.2);
        back(z1, o.shade);
        mounts(5.1);
    });
}

inline('engine', {cyl: 4, block: IRON, shade: IRON_SHADE, cover: CRINKLE, intake: C.ALU, exhaust: 'header'});
inline('engine_inline6', {cyl: 6, block: IRON, shade: IRON_SHADE, cover: C.ALU, ribs: true, intake: PLASTIC,
    exhaust: 'shield'});
inline('engine_inline4_diesel', {cyl: 4, block: DIESEL, shade: DIESEL_SHADE, cover: PLASTIC, intake: C.DARK_STEEL,
    exhaust: 'log', diesel: true});
inline('engine_inline6_diesel', {cyl: 6, block: DIESEL, shade: DIESEL_SHADE, cover: PLASTIC, intake: C.DARK_STEEL,
    exhaust: 'log', diesel: true});

// ---- V ----------------------------------------------------------------------------

function vee(name, o) {
    const n = o.cyl / 2, pitch = 2.3, cy = 3.6;
    const len = n * pitch + 1.4, z0 = 8 - len / 2, z1 = 8 + len / 2;
    const cz = i => z0 + 0.7 + pitch * (i + 0.5);
    k.model(name, {}, () => {
        k.g('sump');
        k.box(5.1, 0, z0 + len * 0.4, 10.9, 1.3, z1 - 0.4, o.pan);
        k.box(4.8, 1.3, z0 + 0.4, 11.2, 2.4, z1 - 0.3, o.pan);
        k.g('block');
        k.box(4.6, 2.4, z0, 11.4, 5.4, z1, o.block);

        // each bank drawn upright over the crank, then leant out 45 degrees
        k.sym(() => {
            const r = {rot: ['z', 45, [8, cy, 8]]};
            k.g('bank');
            k.box(6.6, cy + 1.0, z0, 9.4, cy + 4.4, z1, o.block, r);
            k.g('head');
            k.box(6.5, cy + 4.4, z0 + 0.2, 9.5, cy + 5.6, z1 - 0.2, o.head, r);
            k.g('cover');
            k.box(6.8, cy + 5.6, z0 + 0.4, 9.2, cy + 6.4, z1 - 0.4, o.cover, r);
            if (o.diesel) {
                k.g('injectors');
                for (let i = 0; i < n; i++) {
                    k.box(7.6, cy + 6.4, cz(i) - 0.35, 8.4, cy + 6.9, cz(i) + 0.35, C.ALU, r);
                }
                k.box(8.6, cy + 6.4, z0 + 0.8, 9.0, cy + 6.8, z1 - 0.8, C.CHROME, r);
            } else if (o.coils) {
                k.g('coils');
                for (let i = 0; i < n; i++) {
                    k.box(7.5, cy + 6.4, cz(i) - 0.45, 8.5, cy + 7.0, cz(i) + 0.45, PLASTIC, r);
                }
            } else {
                // plug leads into the side of the head under the rocker cover
                k.g('plugs');
                for (let i = 0; i < n; i++) {
                    k.box(6.0, cy + 4.6, cz(i) - 0.25, 6.5, cy + 5.2, cz(i) + 0.25, C.BLACK, r);
                }
            }
            k.g('exhaust');
            k.box(5.6, cy + 1.6, z0 + 0.6, 6.6, cy + 3.4, z1 - 0.6, o.exhaust, r);
            k.box(3.6, 1.2, z1 - 1.4, 4.6, 4.0, z1 - 0.4, o.exhaust);
        });

        k.g('intake');
        if (o.classic) {
            // an aluminium manifold, a carburettor and the round air cleaner on it
            k.box(5.4, 6.2, z0 + 0.6, 10.6, 8.6, z1 - 0.6, C.ALU);
            k.box(6.9, 8.6, 6.9, 9.1, 9.8, 9.1, C.DARK_STEEL);
            k.g('air_cleaner');
            k.round('y', 8, 9.95, 8, 3.0, 0.3, C.CHROME, {sides: 16});
            k.round('y', 8, 10.45, 8, 2.8, 0.7, C.RED, {sides: 16});
            k.round('y', 8, 10.95, 8, 3.0, 0.3, C.CHROME, {sides: 16});
            k.box(7.3, 11.1, 7.85, 8.7, 11.45, 8.15, C.CHROME);
            k.round('y', 8, 9.0, z1 - 0.8, 0.8, 1.8, C.BLACK);
        } else {
            // a plenum down the valley with a runner to each port
            k.box(6.0, 6.6, z0 + 0.5, 10.0, 9.2, z1 - 0.5, o.plenum);
            k.sym(() => {
                for (let i = 0; i < n; i++) {
                    k.box(5.0, 7.0, cz(i) - 0.45, 6.0, 8.4, cz(i) + 0.45, o.plenum);
                }
            });
            k.round('z', 8, 8.0, z0 - 0.1, 1.0, 1.2, C.ALU, {sides: 16});
            if (o.diesel) {
                k.box(6.6, 9.2, z0 + 1.2, 9.4, 9.5, z1 - 1.2, o.trim);
                turbo(8, 10.3, z1 - 0.7, 'z');
            } else {
                // and over it all the moulded engine cover, a silver panel let into it
                k.box(6.2, 9.2, z0 + 0.9, 9.8, 11.2, z1 - 0.9, PLASTIC);
                k.box(6.8, 11.2, z0 + 1.5, 9.2, 11.45, z1 - 1.5, o.trim);
            }
        }

        front(z0, 8.2, o.classic ? o.block : PLASTIC);
        back(z1, o.shade);
        mounts(4.6);
    });
}

vee('engine_v6', {cyl: 6, block: '#8D9298', shade: '#7B8086', head: HEAD, cover: PLASTIC, pan: PAN,
    exhaust: CAST, coils: true, plenum: PLASTIC, trim: C.ALU});
vee('engine_v8', {cyl: 8, block: ORANGE, shade: ORANGE_SHADE, head: ORANGE, cover: C.CHROME, pan: ORANGE,
    exhaust: CAST, classic: true});
vee('engine_v6_diesel', {cyl: 6, block: DIESEL, shade: DIESEL_SHADE, head: HEAD, cover: PLASTIC, pan: PAN,
    exhaust: CAST, diesel: true, plenum: C.DARK_STEEL, trim: PLASTIC});
vee('engine_v8_diesel', {cyl: 8, block: DIESEL, shade: DIESEL_SHADE, head: HEAD, cover: C.TRIM, pan: PAN,
    exhaust: CAST, diesel: true, plenum: C.DARK_STEEL, trim: PLASTIC});

// ---- electric -----------------------------------------------------------------

k.model('engine_electric', {}, () => {
    // the motor: a finned aluminium can along the car
    k.g('motor');
    k.round('z', 8, 6.2, 7.2, 4.0, 8.4, C.ALU, {sides: 16});
    for (const z of [4.4, 6.2, 8.0, 9.8]) {
        k.round('z', 8, 6.2, z, 4.3, 0.45, '#8F959B', {sides: 16});
    }
    k.round('z', 8, 6.2, 2.75, 3.3, 0.5, C.STEEL, {sides: 16});
    k.round('z', 8, 6.2, 2.3, 1.0, 0.6, C.DARK_STEEL);

    // the reduction gearbox behind it, an output shaft out of each side
    k.g('gearbox');
    k.box(3.6, 1.0, 11.4, 12.4, 9.0, 14.0, '#7E848A');
    for (const x of [5.0, 8.0, 11.0]) {
        k.box(x - 0.25, 1.4, 14.0, x + 0.25, 8.6, 14.3, '#737980');
    }
    k.sym(() => {
        k.round('x', 3.0, 3.4, 12.7, 1.0, 1.4, C.DARK_STEEL);
        k.round('x', 2.0, 3.4, 12.7, 0.6, 0.8, C.STEEL);
    });

    // the inverter on top, its high-voltage cables coming down orange
    k.g('inverter');
    k.box(4.8, 10.2, 3.6, 11.2, 12.0, 11.6, '#3A3F45');
    k.box(5.1, 12.0, 3.9, 10.9, 12.3, 11.3, C.ALU);
    for (const x of [5.6, 6.8, 8.0, 9.2, 10.4]) {
        k.box(x - 0.2, 12.3, 4.4, x + 0.2, 12.6, 10.8, C.ALU);
    }
    k.g('cables');
    for (const x of [6.4, 8.0, 9.6]) {
        k.box(x - 0.35, 10.0, 11.6, x + 0.35, 10.7, 12.6, HV);
        k.box(x - 0.35, 9.0, 11.9, x + 0.35, 10.0, 12.6, HV);
    }
    k.box(9.8, 10.6, 2.6, 11.0, 11.8, 3.6, HV);
    k.box(10.0, 10.8, 1.6, 10.8, 11.6, 2.6, HV);
    k.g('coolant');
    k.sym(() => k.round('x', 4.4, 11.4, 6.0, 0.4, 0.8, C.BLUE));
    k.g('mounts');
    k.sym(() => k.box(3.4, 3.6, 6.4, 4.6, 5.0, 8.0, RUBBER));
});
