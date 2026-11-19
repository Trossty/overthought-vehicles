// Tony Kart Racer 401: a CIK sprint kart with a water-cooled 125. A bare
// tube frame a finger off the ground, small wide slicks behind and narrow
// ones in front. The driver sits low in a moulded seat just ahead of the
// rear axle; the engine stands at the right hand, the radiator upright at
// the left, the air box behind the seat and the exhaust swept back over
// the right rear wheel. The steering wheel lies well back on a column that
// dives to the front of the frame, the tank under it ahead of the pedals,
// and the plastics are the CIK set: a low nose, a front panel along the
// column with a blank number board, long side pods and a rear bumper the
// width of the kart.
const P = 'paint';
const TUBE = '#B4B8BD', SHELL = '#1E1E21', ALU = '#B9BDC2', HEAD = '#4A4D52', HUB = '#8E9297';
const TANK = '#E6E4DA', CORE = '#3A3D42', TRAY = '#2A2A2C', PEDAL = '#8E9297';
const FY = 0.65;                                    // the middle of the frame tubes
const tube = (A, B, t) => k.beam(A, B, t || 0.5, t || 0.5, TUBE);
const hose = (A, B) => k.beam(A, B, 0.5, 0.5, C.BLACK);

k.g('body');
// ---- the frame: two rails, narrowing past the driver's knees and out again to the front corners ---------
k.pair(5.35, 0.4, -11.2, 5.85, 0.9, -2.2, TUBE);
tube([5.6, FY, -2.2], [4.4, FY, 2.0]);
tube([-5.6, FY, -2.2], [-4.4, FY, 2.0]);
k.pair(4.15, 0.4, 2.0, 4.65, 0.9, 5.6, TUBE);
tube([4.4, FY, 5.6], [6.4, FY, 7.6]);
tube([-4.4, FY, 5.6], [-6.4, FY, 7.6]);
k.pair(6.15, 0.4, 7.4, 6.65, 0.9, 9.1, TUBE);
k.box(-6.4, 0.4, 8.6, 6.4, 0.9, 9.1, TUBE);                         // the front cross member
k.box(-5.85, 0.4, -11.2, 5.85, 0.9, -10.7, TUBE);                   // the rear one
k.box(-5.35, 0.4, -4.0, 5.35, 0.9, -3.5, TUBE);                     // the two the seat sits on
k.box(-5.35, 0.4, -3.2, 5.35, 0.9, -2.7, TUBE);
// the bars out to the side pods
k.sym(() => {
    tube([5.6, FY, -5.4], [8.8, 0.9, -5.4]);
    tube([4.55, FY, 3.0], [8.8, 0.9, 3.0]);
});
// the floor tray under the driver's feet, with a lip at the front
k.box(-4.2, 0.3, -2.8, 4.2, 0.5, 8.6, TRAY);
k.box(-4.2, 0.5, 8.2, 4.2, 0.9, 8.6, TRAY);

// ---- the front: the C brackets, the king pins and stub axles, the steering arms ------------------------
k.sym(() => {
    k.box(6.15, 0.9, 7.6, 6.65, 2.7, 8.0, TUBE);                    // the bracket's upright
    k.box(6.15, 2.7, 7.5, 7.3, 3.1, 8.1, TUBE);
    k.box(6.65, 0.4, 7.5, 7.3, 0.8, 8.1, TUBE);
    k.box(6.85, 0.8, 7.65, 7.15, 2.7, 7.95, C.STEEL);              // the king pin
    k.box(6.7, 1.0, 7.5, 7.3, 2.5, 8.1, C.DARK_STEEL);             // the spindle on it
    k.box(7.3, 1.8, 7.55, 7.65, 2.3, 8.05, C.STEEL);               // its stub axle
    k.beam([7.0, 1.3, 8.0], [6.0, 1.3, 9.4], 0.4, 0.4, C.DARK_STEEL);
    k.beam([0.9, 1.3, 9.5], [6.0, 1.3, 9.4], 0.3, 0.3, C.STEEL);   // the track rod
    k.box(2.6, 0.4, 9.1, 3.1, 0.9, 10.2, TUBE);                     // the nose's mount
});
k.box(-1.0, 1.1, 9.0, 1.0, 1.5, 9.9, C.DARK_STEEL);                 // the plate on the column's foot
k.box(-0.6, 0.9, 9.0, 0.6, 1.1, 9.6, C.DARK_STEEL);

// ---- the rear axle: bearings, hubs, the brake disc on the left, the sprocket on the right -------------
k.box(-8.15, 1.85, -9.2, 8.15, 2.65, -8.4, C.STEEL);
k.pair(5.3, 0.9, -9.4, 5.9, 3.0, -8.2, C.DARK_STEEL);
k.pair(6.6, 1.5, -9.5, 8.15, 3.0, -8.1, HUB);
k.disc(4.7, 5.1, 2.25, -8.8, 1.55, C.STEEL);
k.box(4.6, 2.8, -8.2, 5.4, 3.9, -7.2, '#C0201C');                   // the caliper, over the disc's edge
k.box(5.4, 0.9, -8.0, 5.8, 3.4, -7.6, C.DARK_STEEL);
k.disc(-5.1, -4.8, 2.25, -8.8, 1.6, C.DARK_STEEL);
// the chain forward to the engine, and its guard over the top run and the sprocket
k.beam([-4.95, 3.85, -8.8], [-4.95, 2.55, -3.6], 0.3, 0.3, C.BLACK);
k.beam([-4.95, 0.65, -8.8], [-4.95, 1.45, -3.6], 0.3, 0.3, C.BLACK);
k.slab(-5.3, -4.6, 4.25, -8.8, 2.95, -3.8, 0.25, C.BLACK, 1);
k.box(-5.3, 3.6, -10.6, -4.6, 4.3, -8.8, C.BLACK);

// ---- the seat: a moulded shell, low, just ahead of the axle ------------------------------------------
k.box(-4.0, 1.0, -7.4, 4.0, 1.8, -2.8, SHELL);
k.slab(-4.3, 4.3, 1.8, -7.4, 11.0, -8.8, 0.6, SHELL, -1);
k.box(-3.8, 10.8, -9.3, 3.8, 11.3, -8.7, SHELL);
k.sym(() => {
    k.box(4.0, 1.0, -7.6, 4.5, 4.4, -4.6, SHELL);
    k.box(4.0, 1.0, -4.6, 4.5, 2.0, -2.8, SHELL);
    k.slab(4.0, 4.5, 4.4, -4.6, 2.0, -2.8, 1.5, SHELL, -1);
    k.slab(4.0, 4.5, 1.8, -7.4, 9.0, -8.5, 1.2, SHELL, 1);        // its rim up the back
});
// the stays from the back of the seat to the ends of the rails
tube([3.0, 8.8, -9.2], [5.6, 0.9, -11.0], 0.4);
tube([-3.0, 8.8, -9.2], [-5.6, 0.9, -11.0], 0.4);

// ---- the engine at the right hand: a smooth water-cooled cylinder, no fins ------------------------------
k.box(-7.2, 0.6, -4.8, -4.9, 0.9, -0.8, C.DARK_STEEL);              // the mount on the rail
k.box(-7.0, 0.9, -4.6, -5.2, 3.6, -1.0, ALU);                       // the crankcase
k.box(-6.8, 3.6, -3.8, -5.4, 6.4, -1.6, ALU);                       // the cylinder
k.box(-6.9, 6.4, -3.9, -5.3, 7.2, -1.5, HEAD);                      // the head, water round it
k.box(-6.4, 7.2, -3.4, -5.8, 7.9, -2.6, C.BLACK);                   // the plug cap
k.box(-6.3, 7.2, -2.2, -5.7, 7.7, -1.6, ALU);                       // the water outlet
k.box(-6.6, 4.4, -1.6, -5.6, 5.6, -1.2, HEAD);                      // the exhaust port
k.box(-6.2, 0.9, -1.0, -5.0, 1.8, 0.0, ALU);                        // the water pump
k.disc(-5.2, -4.95, 2.0, -3.6, 0.85, HUB);                          // the clutch drum
k.disc(-4.95, -4.75, 2.0, -3.6, 0.55, C.DARK_STEEL);                // its sprocket
k.box(-6.6, 1.6, -5.8, -5.4, 3.0, -4.6, C.DARK_STEEL);              // the carburettor
// the intake: a hose from the carburettor back to the air box behind the seat
hose([-6.0, 3.0, -5.6], [-5.6, 5.6, -10.0]);
hose([-5.6, 5.6, -10.0], [-3.6, 4.8, -11.8]);
k.box(-3.6, 3.6, -12.2, 2.4, 5.8, -10.4, C.BLACK);
k.box(-3.2, 4.2, -12.6, -1.8, 5.2, -12.2, C.BLACK);
k.box(1.0, 4.2, -12.6, 2.0, 5.2, -12.2, C.BLACK);
k.pair(1.2, 5.3, -10.4, 1.8, 5.8, -8.6, C.DARK_STEEL);              // its brackets to the seat
// the exhaust: out of the front of the cylinder, round and back over the right rear wheel
k.beam([-6.1, 5.0, -1.4], [-6.9, 4.8, 0.6], 0.9, 0.9, C.EXHAUST);
k.beam([-6.9, 4.8, 0.6], [-7.8, 5.2, 0.4], 1.1, 1.1, C.EXHAUST);
k.beam([-7.8, 5.3, 0.6], [-7.8, 5.5, -6.0], 1.4, 1.4, C.EXHAUST);
k.beam([-7.8, 5.5, -6.0], [-7.2, 5.4, -9.6], 1.0, 1.0, C.EXHAUST);
k.beam([-7.2, 5.3, -9.6], [-6.6, 5.0, -12.8], 1.4, 1.4, '#3A3B3F');
k.beam([-6.6, 5.0, -12.8], [-6.5, 4.9, -13.4], 0.6, 0.6, C.BLACK);
tube([-6.8, 4.4, -12.0], [-5.6, 1.2, -11.6], 0.3);

// ---- the radiator upright at the left hand, its hoses, the battery on the seat --------------------------
k.box(5.0, 3.4, -3.0, 8.4, 9.0, -2.2, CORE);
k.box(4.8, 3.2, -3.1, 5.0, 9.2, -2.1, C.DARK_STEEL);
k.box(8.4, 3.2, -3.1, 8.6, 9.2, -2.1, C.DARK_STEEL);
k.box(4.8, 9.0, -3.1, 8.6, 9.3, -2.1, C.DARK_STEEL);
k.box(4.8, 3.2, -3.1, 8.6, 3.4, -2.1, C.DARK_STEEL);
k.box(5.0, 7.2, -2.2, 8.4, 9.0, -2.1, TRAY);                        // the shutter, half drawn
k.box(6.2, 9.3, -2.9, 6.9, 9.7, -2.3, C.BLACK);
k.box(5.4, 0.9, -2.8, 5.8, 3.2, -2.4, TUBE);                        // its leg on the rail
tube([4.8, 8.6, -2.8], [4.5, 3.6, -4.0], 0.3);
// the top hose from the head round the back of the seat, the bottom one under the driver's knees
hose([-6.0, 7.5, -1.9], [-4.9, 7.5, -1.9]);
hose([-4.9, 7.5, -1.9], [-4.9, 4.8, -8.9]);
hose([-4.9, 4.8, -8.9], [4.9, 4.8, -8.9]);
hose([4.9, 4.8, -8.9], [4.9, 8.6, -3.6]);
hose([4.9, 8.6, -3.6], [5.2, 9.3, -2.6]);
hose([5.4, 3.2, -2.6], [4.6, 1.2, -0.5]);
hose([4.6, 1.2, -0.5], [-5.0, 1.2, -0.5]);
k.box(4.5, 2.2, -6.6, 5.5, 3.8, -5.0, C.BLACK);                     // the battery
k.box(4.9, 3.8, -6.2, 5.2, 4.0, -5.9, '#C0201C');

// ---- the tank under the column, the pedals, the brake rod back to the master cylinder ------------------
k.box(-1.7, 0.5, 6.8, 1.7, 2.1, 8.2, TANK);
k.box(-1.7, 2.1, 6.8, 1.7, 2.75, 7.2, TANK);
k.slab(-1.7, 1.7, 3.2, 6.8, 2.1, 8.2, 0.6, TANK, -1);
k.box(-0.5, 2.8, 6.9, 0.5, 3.4, 7.4, C.BLACK);                      // its cap
k.sym(() => {
    k.box(2.2, 0.5, 6.9, 3.8, 0.8, 7.5, C.DARK_STEEL);              // the pedal's hinge on the tray
    k.beam([3.0, 1.0, 6.5], [3.0, 0.8, 7.2], 0.4, 0.4, C.DARK_STEEL);
});
k.box(2.4, 1.0, 6.3, 3.6, 3.4, 6.6, PEDAL);                         // the brake, under the left foot
k.box(-3.6, 1.0, 6.3, -2.4, 3.2, 6.6, PEDAL);                       // the throttle, under the right
k.beam([3.0, 1.35, 6.9], [4.9, 1.35, 6.9], 0.2, 0.2, C.STEEL);
k.beam([4.9, 1.35, 6.9], [4.9, 1.35, -5.4], 0.2, 0.2, C.STEEL);
k.box(4.7, 0.9, -6.6, 5.3, 1.6, -5.4, C.DARK_STEEL);                // the master cylinder
k.beam([5.0, 1.6, -6.6], [5.0, 2.8, -7.6], 0.25, 0.25, C.BLACK);

// ---- the nose: low and wide, falling to the front, a rib down its middle --------------------------------
const NOSE = [[10.2, 3.0], [11.4, 2.9], [12.8, 2.4], [14.2, 1.8], [15.0, 1.3]];
k.box(-6.4, 0.5, 10.2, 6.4, 1.6, 14.6, P);
k.box(-6.4, 1.6, 10.2, 6.4, 2.3, 12.4, P);
k.loft(-6.4, 6.4, NOSE, 0.8, P, -1);
k.loft(-1.4, 1.4, NOSE.slice(0, 4), 0.4, P, 1);
k.sym(() => k.loft(3.6, 4.2, NOSE.slice(0, 4), 0.05, '#F2F2F0', 1));
k.box(-6.4, 0.3, 14.4, 6.4, 0.6, 15.1, C.BLACK);

// ---- the side pods: low at the front, rising to the back, a stripe along each --------------------------
const POD = [[5.0, 2.0], [2.0, 2.3], [-2.0, 2.6], [-4.6, 3.0], [-5.8, 3.6]];
k.sym(() => {
    k.box(7.2, 0.6, -5.8, 10.8, 1.8, 5.0, P);
    k.box(7.2, 1.8, -4.6, 10.8, 2.6, -3.0, P);
    k.box(7.2, 1.8, -5.8, 10.8, 2.9, -4.4, P);
    k.loft(7.2, 10.8, POD, 0.8, P, -1);
    k.loft(10.3, 10.8, POD, 0.3, P, 1);                             // the raised outer edge
    k.box(10.8, 1.0, -5.0, 10.85, 1.4, 4.2, '#F2F2F0');
    k.box(10.8, 0.5, -5.8, 10.9, 0.8, 5.0, C.BLACK);
});

// ---- the rear bumper, the width of the kart, on two brackets -----------------------------------------
k.pair(5.35, 0.4, -12.0, 5.85, 1.3, -11.2, TUBE);
k.box(-10.4, 0.5, -13.0, 10.4, 2.4, -12.0, P);
k.box(-10.4, 2.4, -13.0, 10.4, 2.8, -12.4, P);
k.walls(0.5, 2.6, 10.4, -12.6, 11.0, -11.3, 0.8, P);
k.box(-10.4, 0.3, -13.1, 10.4, 0.6, -12.0, C.BLACK);
k.plate(-2.0, 1.0, 2.0, 2.2, -13.0, false);

// ---- the steering column, carried on from where the game's ends down to its foot -------------------------
// the front panel lies along it, a hand above, with a blank number board
const col = k.column();
const along = (t, up) => [0, 1, 2].map(i => col.wheel[i] + col.dir[i] * t + [0, -col.dir[2], col.dir[1]][i] * -(up || 0));
const footT = (col.wheel[1] - 1.4) / -col.dir[1];
k.beam(col.end, along(footT), 0.9, 0.9, C.STEEL);
k.beam(along(3.2), along(4.0), 1.3, 1.3, C.DARK_STEEL);              // the upper bush in its support
k.sym(() => tube([2.6, 0.9, 8.9], [0.6, along(3.8)[1] - 0.2, along(3.8)[2]]));
const top = along(3.6, 1.0), bottom = along(7.6, 1.0), mid = along(5.6, 1.0);
k.slab(-2.6, 2.6, top[1], top[2], mid[1], mid[2], 0.5, P, -1);
k.slab(-3.4, 3.4, mid[1], mid[2], bottom[1], bottom[2], 0.5, P, -1);
const pt = along(4.0, 1.06), pb = along(6.4, 1.06);
k.slab(-2.2, 2.2, pt[1], pt[2], pb[1], pb[2], 0.06, C.PLATE, 1);
k.box(-0.4, 2.2, 9.4, 0.4, 2.8, 10.3, TUBE);                         // the panel's foot on the nose
