// Honda FourTrax Rancher, 2010s: a farm quad. Red plastics: round wings
// over all four wheels, a nose with two lamps between the front pair, the
// tank under the bars and side covers under the seat. Black steel racks
// front and back, black footboards, a long black saddle; the engine sits
// low in the middle, the silencer under the back rack on the right. The
// bars, with the lamp pod and the gauge on them, turn with the front wheels.
const P = 'paint';
const DEG = Math.PI / 180;
const SEAT = '#1F1F22', ENGINE = '#8E9297', RACK = '#232326';
const FW = {y: 4.9, z: 10.0}, RW = {y: 4.9, z: -10.0};

// a round wing over a wheel: the skin, its rolled edge and the dark well under it
const wing = (w, r, from, to, x1, x2) => {
    const arc = (rad, a0, a1, n) => {
        const pts = [];
        for (let i = 0; i <= n; i++) {
            const a = (a0 + (a1 - a0) * i / n) * DEG;
            pts.push([w.z - Math.cos(a) * rad, w.y + Math.sin(a) * rad]);
        }
        return pts;
    };
    k.sym(() => {
        k.loft(x1, x2, arc(r, from, to, 10), 0.8, P, 1);
        k.loft(x2 - 0.2, x2 + 0.5, arc(r - 0.2, from, to, 10), 1.2, P, 1);
        k.loft(x1, x2, arc(r - 0.1, from + 3, to - 3, 8), 0.3, C.WELL, -1);
    });
};

// ---- the frame, the engine, the footboards -----------------------------------------------------
k.g('body');
k.pair(2.6, 2.0, -16.0, 3.4, 3.0, 16.0, C.BLACK);
k.box(-3.4, 1.6, -6.0, 3.4, 2.0, 8.0, C.DARK_STEEL);                  // the skid plate
k.box(-3.2, 3.0, -8.0, 3.2, 9.0, 3.6, ENGINE);
k.box(-2.4, 7.0, 1.0, 2.4, 11.4, 4.8, C.DARK_STEEL, {tilt: -20});    // the cylinder, leant forward
for (let i = 0; i < 4; i++) {
    k.box(-2.8, 7.6 + i * 0.9, 1.4, 2.8, 8.0 + i * 0.9, 4.6, C.BLACK, {tilt: -20});
}
k.disc(3.2, 3.8, 5.6, -3.0, 2.4, ENGINE);                             // the clutch cover
k.box(-4.0, 3.6, -6.0, -3.2, 6.0, -1.0, C.BLACK);                     // the starter
k.sym(() => {
    k.box(3.4, 5.0, -4.6, 9.4, 5.6, 4.6, C.BLACK);
    for (let z = -4.0; z < 4.4; z += 1.4) {
        k.box(3.8, 5.6, z, 9.0, 5.8, z + 0.6, RACK);
    }
    k.box(8.8, 5.6, -4.6, 9.4, 6.4, 4.6, C.BLACK);                    // its outer lip
});
k.box(-5.4, 3.8, -2.0, -3.2, 4.6, 0.2, C.DARK_STEEL);                 // the foot brake
k.box(-6.4, 5.6, -0.8, -4.6, 6.0, 0.2, C.DARK_STEEL);
k.box(4.4, 5.6, 0.0, 6.4, 6.0, 1.0, C.DARK_STEEL);                    // the gear pedal

// ---- the front: the wings, the nose with its lamps, the bumper, the rack --------------------------
wing(FW, 6.0, 12, 168, 3.4, 9.2);
k.box(-3.4, 6.2, 9.0, 3.4, 12.0, 16.8, P);
k.slab(-3.4, 3.4, 12.0, 9.0, 10.6, 17.4, 0.8, P, -1);
k.box(-3.4, 6.2, 16.8, 3.4, 10.0, 17.6, P);
k.sym(() => k.lamp(1.0, 7.6, 3.0, 9.0, 17.6, C.LAMP, true, {depth: 0.3}));
k.box(-1.0, 6.4, 17.6, 1.0, 7.4, 17.8, C.BLACK);                       // the vent between them
k.box(-6.4, 3.6, 18.2, 6.4, 4.6, 19.2, RACK);                          // the bumper
k.pair(5.4, 3.6, 16.6, 6.4, 11.0, 17.6, RACK);
k.beams([5.9, 4.1, 18.6], [5.9, 4.1, 15.6], 0.9, 0.9, RACK);
k.box(-2.4, 2.6, 15.6, 2.4, 3.6, 18.6, C.DARK_STEEL);
// the front rack: a tube frame over both wings on short posts, all of it ahead of the bar's post
k.pair(7.2, 12.6, 10.6, 8.0, 13.4, 17.6, RACK);
k.box(-8.0, 12.6, 16.8, 8.0, 13.4, 17.6, RACK);
k.box(-8.0, 12.6, 10.6, 8.0, 13.4, 11.4, RACK);
for (const z of [12.9, 14.3, 15.7]) {
    k.box(-7.2, 12.8, z, 7.2, 13.2, z + 0.5, RACK);
}
k.pair(5.4, 11.2, 10.8, 6.2, 12.6, 11.6, RACK);
k.pair(5.4, 11.4, 16.2, 6.2, 12.6, 17.0, RACK);

// ---- the tank, the saddle, the side covers ----------------------------------------------------------
k.box(-3.6, 9.4, 1.6, 3.6, 12.6, 7.8, P);
k.slab(-3.6, 3.6, 12.6, 1.6, 12.0, 7.8, 0.6, P, -1);
k.leans(1.6, 7.8, 3.6, 9.4, 4.4, 11.6, 0.8, P);
k.box(-0.9, 12.6, 3.4, 0.9, 13.2, 5.0, C.BLACK);                       // the filler cap
k.box(-4.2, 12.4, -14.6, 4.2, 15.6, 1.2, SEAT);
k.slab(-4.2, 4.2, 15.6, 1.2, 12.8, 2.6, 0.8, SEAT, -1);
k.slab(-4.2, 4.2, 15.6, -14.6, 13.4, -16.2, 0.8, SEAT, -1);
k.box(-0.1, 15.6, -14.0, 0.1, 15.64, 0.6, '#3A3A3F');                  // the seam on its top
k.sym(() => {
    k.box(3.8, 8.6, -9.0, 4.6, 12.6, 1.6, P);                          // the side cover
    k.box(4.6, 9.6, -6.6, 4.7, 11.4, -1.0, C.BLACK);                   // its grille
});

// ---- the back: the wings, the rack, the tail lamp, the silencer -------------------------------------
wing(RW, 6.0, 12, 168, 3.4, 9.2);
k.box(-3.4, 8.4, -17.4, 3.4, 12.4, -12.6, P);
k.slab(-3.4, 3.4, 12.4, -12.6, 11.0, -17.6, 0.8, P, -1);
k.lamp(-1.6, 9.2, 1.6, 10.6, -17.6, C.TAIL, false, {depth: 0.3});
k.plate(-1.8, 6.4, 1.8, 8.4, -17.4, false);
k.box(-2.0, 3.0, -18.4, 2.0, 4.4, -16.0, RACK);                        // the hitch receiver
k.box(-0.8, 3.2, -19.0, 0.8, 4.2, -18.4, C.DARK_STEEL);
k.pair(7.2, 13.0, -18.6, 8.0, 13.8, -4.6, RACK);
k.box(-8.0, 13.0, -18.6, 8.0, 13.8, -17.8, RACK);
k.box(-8.0, 13.0, -5.4, 8.0, 13.8, -4.6, RACK);
for (const z of [-15.2, -12.0, -8.8]) {
    k.box(-7.2, 13.2, z, 7.2, 13.6, z + 0.5, RACK);
}
k.pair(5.4, 11.0, -5.2, 6.2, 13.0, -4.4, RACK);
k.pair(5.4, 11.4, -18.2, 6.2, 13.0, -17.4, RACK);
k.box(-6.8, 7.6, -17.2, -4.0, 10.2, -9.4, C.EXHAUST);
k.box(-5.9, 8.4, -18.2, -4.9, 9.4, -17.2, C.BLACK);
k.beam([-3.2, 6.0, -1.0], [-5.4, 8.4, -9.4], 1.0, 1.0, C.EXHAUST);

// ---- the bars, which turn with the front wheels ----------------------------------------------------------
k.g('steer');
k.beam([0, 11.4, 9.2], [0, 18.0, 8.4], 1.0, 1.0, RACK);               // the post
k.box(-2.8, 17.8, 6.8, 2.8, 20.0, 10.0, P);                            // the pod over the clamp
k.slab(-2.8, 2.8, 20.0, 6.8, 20.4, 9.2, 0.4, P, 1);
k.box(-1.6, 20.0, 7.0, 1.6, 20.5, 8.4, C.DIAL);                        // the gauge
k.lamp(-1.4, 18.4, 1.4, 19.4, 10.0, C.LAMP, true, {depth: 0.3});
k.beams([2.8, 19.0, 8.4], [6.6, 19.4, 7.4], 0.7, 0.7, C.BLACK);
k.sym(() => {
    k.box(6.4, 18.8, 6.8, 8.6, 20.0, 8.0, C.BLACK);                    // the grip
    k.box(8.6, 18.9, 6.9, 8.9, 19.9, 7.9, C.DARK_STEEL);
    k.box(5.6, 18.6, 7.8, 6.6, 20.2, 8.6, C.DARK_STEEL);               // the switch block
    k.beam([6.2, 19.6, 8.4], [8.4, 19.2, 9.2], 0.4, 0.4, C.DARK_STEEL); // the lever
});
k.box(-5.6, 19.6, 6.6, -4.8, 20.2, 7.4, C.BLACK);                      // the thumb throttle
