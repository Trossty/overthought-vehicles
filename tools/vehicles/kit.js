/*
 * The drawing kit for the mod's vehicles. It runs inside Blockbench.
 *
 * A vehicle is drawn by a script next to this file (sedan.js and so
 * on). The script describes the car in the game's own terms: sixteenths of a
 * block, x across (left is +x), y up from the road, z towards the nose. The
 * kit turns that into a Blockbench project of cubes, saves it as a .bbmodel,
 * and writes the same cubes back out as the "shape" of the vehicle's
 * data-pack definition. The .bbmodel is the drawing; open it, move things,
 * and CarKit.exportOpen() writes the definition again from what is on screen.
 *
 * In Blockbench the car faces north, as models there do, so x and z are
 * turned round on the way in and on the way out.
 *
 * Groups carry what a box does: everything under a top-level group called
 * "bonnet" or "boot" swings up with that lid, "steer" turns with the front
 * wheel, "propeller" spins with the engine about the line along the body,
 * "rotor" about the upright and "tail_rotor" about the line across, each
 * about the middle of its group's first box. A group whose name begins with
 * an underscore is for looking at only (the wheels, a seated figure) and is
 * left out of the definition.
 *
 * Load it from Blockbench's console, or through the MCP:
 *   CAR_REPO = '/path/to/car'; eval(require('fs').readFileSync(CAR_REPO + '/tools/vehicles/kit.js', 'utf8'));
 *   CarKit.run('sedan');
 */
(function () {
    const fs = require('fs');
    const REPO = window.CAR_REPO;
    const DIR = REPO + '/tools/vehicles/';
    const DATA = REPO + '/src/main/resources/data/overthought_vehicles/overthought_vehicles/vehicle/';

    const SPECIAL = ['paint', 'paint_dark', 'paint_light', 'cargo'];
    const PALETTE = 16;
    const DYES = {
        white: '#F9FFFE', light_gray: '#9D9D97', gray: '#474F52', black: '#1D1D21', red: '#B02E26',
        orange: '#F9801D', yellow: '#FED83D', lime: '#80C71F', green: '#5E7C16', cyan: '#169C9C',
        light_blue: '#3AB3DA', blue: '#3C44AA', purple: '#8932B8', magenta: '#C74EBD', pink: '#F38BAA',
        brown: '#835432'
    };
    const FLAGS = {
        bonnet: {opens: 'bonnet'}, boot: {opens: 'boot'}, steer: {steers: true}, propeller: {spins: true},
        rotor: {spins: true, spin_axis: 'y'}, tail_rotor: {spins: true, spin_axis: 'x'}
    };
    const SPINNERS = {z: 'propeller', y: 'rotor', x: 'tail_rotor'};
    const DEG = Math.PI / 180;

    /** The colours every vehicle shares, by name. */
    const C = {
        BLACK: '#1B1B1D', TRIM: '#2A2A2C', CHROME: '#B4B8BD', LAMP: '#F4F0D0', TAIL: '#C0201C',
        LAMP_GLOW: '#FFFDF2', TAIL_GLOW: '#EE4A3C', GLASS: '#55A9D3E6', ACCENT: '#E3E1DA',
        BUMPER: '#8C9197', SEAT: '#3B3B40', SEAT_CLOTH: '#566070', CARPET: '#2C2B2E', DIAL: '#E6E1CF',
        NEEDLE: '#D8402C', DISPLAY: '#3FB6A8', AMBER: '#E8901B', AMBER_SIDE: '#E38C1F', REVERSE: '#E8E8E8',
        RUBBER: '#141416', WELL: '#19191B', STEEL: '#6E7379', DARK_STEEL: '#45494E', PLATE: '#ECECE6',
        MIRROR: '#9FB6C4', PLASTIC: '#222225', RUST: '#5A4A3F', EXHAUST: '#7C7F84',
        SIGHT_GLASS: '#30DCE8F0', CARGO: 'cargo'
    };

    function r2(v) {
        const n = Math.round(v * 100) / 100;
        return Object.is(n, -0) ? 0 : n;
    }

    function shadeHex(hex, f) {
        const n = parseInt(hex.slice(1), 16);
        const ch = s => Math.max(0, Math.min(255, Math.round(((n >> s) & 255) * f)));
        return '#' + [16, 8, 0].map(s => ch(s).toString(16).padStart(2, '0')).join('').toUpperCase();
    }

    class Kit {
        constructor(name) {
            this.name = name;
            this.boxes = [];
            this.group = 'body';
            this.C = C;
            this.def = JSON.parse(fs.readFileSync(DATA + name + '.json', 'utf8'));
            this.overrides = {};
            this.mirroring = false;
        }

        /** Boxes added from here on go into this group; "a/b" nests. */
        g(path) {
            this.group = path;
            return this;
        }

        /** Changes to the rest of the definition: wheels, seats, load and so on. */
        set(fields) {
            Object.assign(this.overrides, fields);
            return this;
        }

        get wheels() {
            return this.overrides.wheels || this.def.wheels || [];
        }

        get seats() {
            return this.overrides.seats || this.def.seats || [];
        }

        // ---- the one primitive -------------------------------------------------

        /** A box, corner to corner, leant as {tilt, yaw, roll} say. */
        box(x1, y1, z1, x2, y2, z2, color, rot) {
            rot = rot || {};
            const b = {
                from: [Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)],
                to: [Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)],
                color: color || 'paint', tilt: rot.tilt || 0, yaw: rot.yaw || 0, roll: rot.roll || 0,
                group: this.group, name: rot.name || this.group.split('/').pop()
            };
            this.boxes.push(b);
            return b;
        }

        /** The same box on both sides of the car. */
        pair(x1, y1, z1, x2, y2, z2, color, rot) {
            rot = rot || {};
            this.box(x1, y1, z1, x2, y2, z2, color, rot);
            this.box(-x2, y1, z1, -x1, y2, z2, color,
                Object.assign({}, rot, {yaw: -(rot.yaw || 0), roll: -(rot.roll || 0)}));
        }

        /** Runs {@code draw}, then adds a mirror image of everything it drew. */
        sym(draw) {
            const start = this.boxes.length;
            draw();
            const made = this.boxes.slice(start);
            for (const b of made) {
                this.boxes.push(Object.assign({}, b, {
                    from: [-b.to[0], b.from[1], b.from[2]], to: [-b.from[0], b.to[1], b.to[2]],
                    yaw: -b.yaw, roll: -b.roll
                }));
            }
        }

        // ---- panels ------------------------------------------------------------

        /**
         * A panel seen from the side: from (yA, zA) to (yB, zB) in the car's
         * profile, x1 to x2 across. The line is its outer face and the
         * thickness goes in under it; off 0 centres it on the line, off 1
         * puts it outside.
         */
        slab(x1, x2, yA, zA, yB, zB, thick, color, off, extra) {
            off = off === undefined ? -1 : off;
            const dy = yB - yA, dz = zB - zA;
            const len = Math.hypot(dy, dz);
            // the normal that points up (or forward, for a wall)
            let ny = dz / len, nz = -dy / len;
            if (ny < -1e-9 || (Math.abs(ny) < 1e-9 && nz < 0)) {
                ny = -ny;
                nz = -nz;
            }
            const cy = (yA + yB) / 2 + ny * thick / 2 * off;
            const cz = (zA + zB) / 2 + nz * thick / 2 * off;
            if (Math.abs(dy) >= Math.abs(dz)) {
                const bottomZ = yA < yB ? zA : zB, topZ = yA < yB ? zB : zA;
                const tilt = Math.atan2(bottomZ - topZ, Math.abs(dy)) / DEG;
                return this.box(x1, cy - len / 2, cz - thick / 2, x2, cy + len / 2, cz + thick / 2, color,
                    Object.assign({tilt: tilt}, extra));
            }
            const rearY = zA < zB ? yA : yB, frontY = zA < zB ? yB : yA;
            const tilt = Math.atan2(frontY - rearY, Math.abs(dz)) / DEG;
            return this.box(x1, cy - thick / 2, cz - len / 2, x2, cy + thick / 2, cz + len / 2, color,
                Object.assign({tilt: tilt}, extra));
        }

        slabs(x1, x2, yA, zA, yB, zB, thick, color, off) {
            this.slab(x1, x2, yA, zA, yB, zB, thick, color, off);
            this.slab(-x2, -x1, yA, zA, yB, zB, thick, color, off);
        }

        /** A run of slabs through the points [z, y] of a profile, lapped a little at each joint. */
        loft(x1, x2, pts, thick, color, off) {
            for (let i = 0; i + 1 < pts.length; i++) {
                let [zA, yA] = pts[i], [zB, yB] = pts[i + 1];
                const len = Math.hypot(zB - zA, yB - yA);
                const lap = Math.min(0.12, len * 0.1);
                const ux = (zB - zA) / len, uy = (yB - yA) / len;
                if (i > 0) {
                    zA -= ux * lap;
                    yA -= uy * lap;
                }
                if (i + 2 < pts.length) {
                    zB += ux * lap;
                    yB += uy * lap;
                }
                this.slab(x1, x2, yA, zA, yB, zB, thick, color, off);
            }
        }

        /**
         * A panel seen from the front: from (xA, yA) to (xB, yB) across the
         * car, z1 to z2 along it, leant by rolling. The line is its outer
         * face (the side further from the middle of the car, or the top).
         */
        lean(z1, z2, xA, yA, xB, yB, thick, color, off, extra) {
            off = off === undefined ? -1 : off;
            const dx = xB - xA, dy = yB - yA;
            const len = Math.hypot(dx, dy);
            let nx = dy / len, ny = -dx / len;
            const mx = (xA + xB) / 2;
            // outwards: away from the centre line, or up if the panel lies on it
            if (nx * Math.sign(mx || 1) < -1e-9 || (Math.abs(nx) < 1e-9 && ny < 0)) {
                nx = -nx;
                ny = -ny;
            }
            const cx = mx + nx * thick / 2 * off;
            const cy = (yA + yB) / 2 + ny * thick / 2 * off;
            if (Math.abs(dy) >= Math.abs(dx)) {
                const [bx, tx] = yA < yB ? [xA, xB] : [xB, xA];
                const roll = Math.atan2(-(tx - bx), Math.abs(dy)) / DEG;
                return this.box(cx - thick / 2, cy - len / 2, z1, cx + thick / 2, cy + len / 2, z2, color,
                    Object.assign({roll: roll}, extra));
            }
            const [ly, ry] = xA < xB ? [yA, yB] : [yB, yA];
            const roll = Math.atan2(ry - ly, Math.abs(dx)) / DEG;
            return this.box(cx - len / 2, cy - thick / 2, z1, cx + len / 2, cy + thick / 2, z2, color,
                Object.assign({roll: roll}, extra));
        }

        leans(z1, z2, xA, yA, xB, yB, thick, color, off) {
            this.lean(z1, z2, xA, yA, xB, yB, thick, color, off);
            this.lean(z1, z2, -xA, yA, -xB, yB, thick, color, off);
        }

        /**
         * A wall seen from above: from (xA, zA) to (xB, zB) on the plan,
         * y1 to y2 tall, turned by yawing. The line is its outer face.
         */
        wall(y1, y2, xA, zA, xB, zB, thick, color, off, extra) {
            off = off === undefined ? -1 : off;
            const dx = xB - xA, dz = zB - zA;
            const len = Math.hypot(dx, dz);
            const mx = (xA + xB) / 2, mz = (zA + zB) / 2;
            let nx = dz / len, nz = -dx / len;
            // outwards: away from the middle of the car
            if (nx * mx + nz * mz < 0) {
                nx = -nx;
                nz = -nz;
            }
            const cx = mx + nx * thick / 2 * off;
            const cz = mz + nz * thick / 2 * off;
            if (Math.abs(dz) >= Math.abs(dx)) {
                const [rx, fx] = zA < zB ? [xA, xB] : [xB, xA];
                const yaw = Math.atan2(fx - rx, Math.abs(dz)) / DEG;
                return this.box(cx - thick / 2, y1, cz - len / 2, cx + thick / 2, y2, cz + len / 2, color,
                    Object.assign({yaw: yaw}, extra));
            }
            const [rz, lz] = xA < xB ? [zA, zB] : [zB, zA];
            const yaw = Math.atan2(-(lz - rz), Math.abs(dx)) / DEG;
            return this.box(cx - len / 2, y1, cz - thick / 2, cx + len / 2, y2, cz + thick / 2, color,
                Object.assign({yaw: yaw}, extra));
        }

        walls(y1, y2, xA, zA, xB, zB, thick, color, off) {
            this.wall(y1, y2, xA, zA, xB, zB, thick, color, off);
            this.wall(y1, y2, -xA, zA, -xB, zB, thick, color, off);
        }

        /**
         * A bar from point A to point B, w wide and h deep: a pillar, a
         * strut, a pipe, a wiper. Upright bars are w across the car and h
         * along it; level ones w across and h tall.
         */
        beam(A, B, w, h, color, extra) {
            let d = [B[0] - A[0], B[1] - A[1], B[2] - A[2]];
            const len = Math.hypot(d[0], d[1], d[2]);
            const c = [(A[0] + B[0]) / 2, (A[1] + B[1]) / 2, (A[2] + B[2]) / 2];
            const ax = [Math.abs(d[0]), Math.abs(d[1]), Math.abs(d[2])];
            const main = ax[1] >= ax[0] && ax[1] >= ax[2] ? 1 : ax[2] >= ax[0] ? 2 : 0;
            if (d[main] < 0) {
                d = d.map(v => -v);
            }
            d = d.map(v => v / len);
            if (main === 1) {
                const tilt = Math.asin(Math.max(-1, Math.min(1, -d[2]))) / DEG;
                const roll = Math.atan2(-d[0], d[1]) / DEG;
                return this.box(c[0] - w / 2, c[1] - len / 2, c[2] - h / 2, c[0] + w / 2, c[1] + len / 2, c[2] + h / 2,
                    color, Object.assign({tilt: tilt, roll: roll}, extra));
            }
            if (main === 2) {
                const tilt = Math.asin(Math.max(-1, Math.min(1, d[1]))) / DEG;
                const yaw = Math.atan2(d[0], d[2]) / DEG;
                return this.box(c[0] - w / 2, c[1] - h / 2, c[2] - len / 2, c[0] + w / 2, c[1] + h / 2, c[2] + len / 2,
                    color, Object.assign({tilt: tilt, yaw: yaw}, extra));
            }
            const yaw = Math.asin(Math.max(-1, Math.min(1, -d[2]))) / DEG;
            const roll = Math.atan2(d[1], d[0]) / DEG;
            return this.box(c[0] - len / 2, c[1] - h / 2, c[2] - w / 2, c[0] + len / 2, c[1] + h / 2, c[2] + w / 2,
                color, Object.assign({yaw: yaw, roll: roll}, extra));
        }

        beams(A, B, w, h, color) {
            this.beam(A, B, w, h, color);
            this.beam([-A[0], A[1], A[2]], [-B[0], B[1], B[2]], w, h, color);
        }

        // ---- things every car has -----------------------------------------------

        /**
         * The body over one axle. The skin is cut to a round opening, in
         * upright strips from one side of the car to the other; a dark tub
         * behind the tyres closes it; and a lip follows the true circle so
         * the stepped edge of the strips never shows.
         *
         * o: {z, y, r (the opening's radius), half (the skin's half width),
         * inner (the tub's half width), top (how high the strips go),
         * floor (the tub's bottom), n (strips), lip (its colour, false for
         * none), flare (how far the lip stands out), lipDeep, lipThick,
         * from/to (degrees of the lip, 0 at the back, 180 at the front),
         * skin (the strips' colour), tubTop}
         */
        arch(o) {
            const n = o.n || 8, r = o.r, half = o.half;
            const top = o.top, skin = o.skin || 'paint';
            const group = this.group;
            // the tub: closed over the top, open below
            const tubTop = o.tubTop !== undefined ? o.tubTop : o.y + r + 0.4;
            const tubA = o.tubFrom !== undefined ? o.tubFrom : o.z - r, tubB = o.tubTo !== undefined ? o.tubTo : o.z + r;
            const well = o.tubColor || C.WELL;
            if (o.hollow) {
                // a housing each side and nothing between: a load bed or a cabin floor runs through
                this.pair(o.inner - 0.6, o.floor, tubA, o.inner, tubTop, tubB, well);
                this.pair(o.inner, o.floor, tubA, o.skinIn, tubTop, tubA + 0.6, well);
                this.pair(o.inner, o.floor, tubB - 0.6, o.skinIn, tubTop, tubB, well);
            } else {
                this.box(-o.inner, o.floor, tubA, o.inner, tubTop, tubB, well);
            }
            const topAt = typeof top === 'function' ? top : () => top;
            const step = 2 * r / n;
            for (let i = 0; i < n; i++) {
                const a = o.z - r + step * i, b = a + step;
                // the opening is highest at the end of the strip nearer the axle
                const near = a <= o.z && b >= o.z ? 0 : Math.min(Math.abs(a - o.z), Math.abs(b - o.z));
                const cut = o.y + Math.sqrt(Math.max(0, r * r - near * near));
                const up = Math.min(topAt(a), topAt(b));
                if (cut < up - 0.05) {
                    if (o.open) {
                        // only the skin each side: the cabin or the boot is between
                        this.pair(o.skinIn, cut, a, half, up, b, skin);
                    } else {
                        this.box(-half, cut, a, half, up, b, skin);
                    }
                }
            }
            if (o.open) {
                // a roof over each housing, from the tub out to the skin
                this.pair(o.inner, tubTop - 0.4, tubA, o.skinIn, tubTop, tubB, well);
            }
            if (o.lip !== false) {
                const segs = o.lipSegs || 9;
                const from = (o.from === undefined ? 8 : o.from) * DEG;
                const to = (o.to === undefined ? 172 : o.to) * DEG;
                const t = o.lipThick || 0.7, deep = o.lipDeep || 1.2, flare = o.flare === undefined ? 0.25 : o.flare;
                const rr = r + t / 2 - 0.05;
                const halfStep = (to - from) / segs / 2;
                const reach = rr / Math.cos(halfStep);
                // each flat runs on past the corner by as much as its outer edge falls short of the next
                const over = t / 2 * Math.tan(halfStep) + 0.02;
                for (let i = 0; i < segs; i++) {
                    const a = from + (to - from) * i / segs, b = from + (to - from) * (i + 1) / segs;
                    // corner to corner on the circumscribed polygon, so the flats close up
                    let zA = o.z - Math.cos(a) * reach, yA = o.y + Math.sin(a) * reach;
                    let zB = o.z - Math.cos(b) * reach, yB = o.y + Math.sin(b) * reach;
                    const len = Math.hypot(zB - zA, yB - yA), uz = (zB - zA) / len, uy = (yB - yA) / len;
                    zA -= uz * over;
                    yA -= uy * over;
                    zB += uz * over;
                    yB += uy * over;
                    this.slabs(half + flare - deep, half + flare, yA, zA, yB, zB, t, o.lip || 'paint', 0);
                }
            }
            this.group = group;
        }

        /**
         * A lamp as a thin plate facing along z: a black bezel round it, the
         * lens, and a brighter middle. front true for a head lamp's side of
         * the car.
         */
        lamp(x1, y1, x2, y2, z, color, front, o) {
            o = o || {};
            const out = front ? 1 : -1;
            const glow = color === C.LAMP ? C.LAMP_GLOW : color === C.TAIL ? C.TAIL_GLOW : null;
            const d = o.depth || 0.4;
            if (o.bezel !== false) {
                this.box(x1 - 0.25, y1 - 0.25, z - out * 0.1, x2 + 0.25, y2 + 0.25, z + out * (d - 0.1), o.bezel || C.BLACK);
            }
            this.box(x1, y1, z, x2, y2, z + out * d, color);
            if (glow) {
                // o.cells: that many reflectors side by side instead of one bright middle
                const n = o.cells || 1, gap = 0.3;
                const ix = (x2 - x1) * (n > 1 ? 0.08 : 0.25), iy = (y2 - y1) * 0.25;
                const w = (x2 - x1 - 2 * ix - (n - 1) * gap) / n;
                for (let i = 0; i < n; i++) {
                    const a = x1 + ix + i * (w + gap);
                    this.box(a, y1 + iy, z + out * 0.05, a + w, y2 - iy, z + out * (d + 0.05), glow);
                }
            }
        }

        /** A disc facing along z: eight flats round (cx, cy), r to each flat, z1 to z2 deep. */
        discZ(cx, cy, r, z1, z2, color) {
            const flat = 2 * r * Math.tan(Math.PI / 8);
            for (let i = 0; i < 4; i++) {
                this.box(cx - r, cy - flat / 2, z1, cx + r, cy + flat / 2, z2, color, {roll: i * 45 - 45});
            }
        }

        /**
         * A round lamp facing along z, as lamp() is a square one: a ring
         * round it (o.bezel, o.ring wide), the lens r across the flats and
         * a brighter middle.
         */
        roundLamp(cx, cy, r, z, color, front, o) {
            o = o || {};
            const out = front ? 1 : -1;
            const glow = color === C.LAMP ? C.LAMP_GLOW : color === C.TAIL ? C.TAIL_GLOW : null;
            const d = o.depth || 0.4;
            if (o.bezel !== false) {
                this.discZ(cx, cy, r + (o.ring || 0.3), z - out * 0.1, z + out * (d - 0.1), o.bezel || C.BLACK);
            }
            this.discZ(cx, cy, r, z, z + out * d, color);
            if (glow) {
                this.discZ(cx, cy, r * 0.5, z + out * 0.05, z + out * (d + 0.05), glow);
            }
        }

        /**
         * Glass in a leaning side window, both sides. The pane lies on the
         * line from (xLow, yLow) to (xHigh, yHigh) seen from the front; its
         * ends follow raked pillars in steps the pillars hide. The pieces
         * are a hair thick and lie in one plane, so no joint shows: it
         * reads as a single pane. (No thickness at all and the two faces
         * of each piece fight.) With a colour and a thickness it is a
         * panel of bodywork instead.
         */
        sideGlass(o) {
            const bands = o.bands || 8;
            const lerp = (a, b, t) => a + (b - a) * t;
            for (let i = 0; i < bands; i++) {
                const ta = i / bands, tb = (i + 1) / bands;
                const rear = Math.max(lerp(o.rearLow, o.rearHigh, ta), lerp(o.rearLow, o.rearHigh, tb));
                const front = Math.min(lerp(o.frontLow, o.frontHigh, ta), lerp(o.frontLow, o.frontHigh, tb));
                if (front > rear) {
                    this.leans(rear, front, lerp(o.xLow, o.xHigh, ta), lerp(o.yLow, o.yHigh, ta),
                        lerp(o.xLow, o.xHigh, tb), lerp(o.yLow, o.yHigh, tb), o.thick || 0.05, o.color || C.GLASS,
                        o.off === undefined ? 0 : o.off);
                }
            }
        }

        /**
         * A bumper: a bar across the car with its corners cut off at an
         * angle and a return down each side towards the wheel arch, and a
         * black rubbing strip along the lot. o: {front, z (the outer face),
         * depth, y1, y2, half, cut (how much corner comes off), wrap (the z
         * the returns run to), color, strip (false for none), stripColor}
         */
        bumper(o) {
            const s = o.front ? 1 : -1;
            const face = o.z, cut = o.cut === undefined ? 2 : o.cut, half = o.half;
            const col = o.color || C.BUMPER, d = o.depth || 1.8;
            const part = (y1, y2, proud, colour, thick) => {
                this.box(-(half - cut), y1, face + s * proud, half - cut, y2, face + s * proud - s * thick, colour);
                this.walls(y1, y2, half - cut, face + s * proud, half + proud, face - s * cut, Math.min(thick, 1.5), colour);
                if (o.wrap !== undefined) {
                    this.pair(half + proud - Math.min(thick, 1.0), y1, Math.min(face - s * cut, o.wrap),
                        half + proud, y2, Math.max(face - s * cut, o.wrap), colour);
                }
            };
            part(o.y1, o.y2, 0, col, d);
            if (o.strip !== false) {
                const mid = (o.y1 + o.y2) / 2, h = Math.min(0.5, (o.y2 - o.y1) / 4);
                part(mid - h, mid + h, 0.15, o.stripColor || C.BLACK, 0.3);
            }
        }

        /** A door mirror each side: a housing on a short arm, glass on its back face. */
        mirrors(o) {
            const w = o.w || 2.0, h = o.h || 1.5, d = o.d || 1.0;
            this.sym(() => {
                this.box(o.x - 0.2, o.y + h * 0.2, o.z - d * 0.4, o.x + 0.5, o.y + h * 0.7, o.z + d * 0.4, o.arm || C.BLACK);
                this.box(o.x + 0.4, o.y, o.z - d / 2, o.x + 0.4 + w, o.y + h, o.z + d / 2, o.color || C.BLACK);
                this.box(o.x + 0.6, o.y + 0.2, o.z - d / 2 - 0.08, o.x + 0.2 + w, o.y + h - 0.2, o.z - d / 2, C.MIRROR);
            });
        }

        /**
         * A number plate on a face of the car: white, a blue band at its
         * left blank: no band, no letters.
         */
        plate(x1, y1, x2, y2, z, front) {
            const s = front ? 1 : -1;
            this.box(x1, y1, z, x2, y2, z + s * 0.15, C.PLATE);
        }

        /** A wheel or drum seen from the side: eight flats round (y, z), x1 to x2 across. */
        disc(x1, x2, y, z, r, color) {
            const flat = 2 * r * Math.tan(Math.PI / 8);
            for (let i = 0; i < 4; i++) {
                this.box(x1, y - r, z - flat / 2, x2, y + r, z + flat / 2, color, {tilt: i * 45 - 45});
            }
        }

        /** The ball coupling on the end of a drawbar: its tip at z, its underside at y. */
        coupler(z, y) {
            this.box(-1.2, y + 0.3, z - 5.6, 1.2, y + 2.1, z - 3.2, C.DARK_STEEL);
            this.box(-1.8, y, z - 3.4, 1.8, y + 2.6, z, C.CHROME);
            this.box(-1.3, y + 2.6, z - 3.0, 1.3, y + 3.0, z - 0.5, C.DARK_STEEL);
            this.slab(-0.45, 0.45, y + 2.9, z - 2.4, y + 3.9, z - 5.4, 0.6, C.BLACK, 0);
        }

        /** A jockey wheel clamped beside a drawbar: its tube from the ground up to top. */
        jockey(x, z, top, ground) {
            const r = 1.5;
            this.box(x - 0.6, ground + 2 * r - 0.4, z - 0.6, x + 0.6, top, z + 0.6, C.BUMPER);
            this.box(x - 1.0, top - 4.0, z - 1.0, x + 1.0, top - 2.6, z + 1.0, C.DARK_STEEL);
            this.pairAt(x, 0.75, ground + r - 0.3, z - 0.5, 0.95, ground + 2 * r + 0.4, z + 0.5, C.DARK_STEEL);
            this.disc(x - 0.6, x + 0.6, ground + r, z, r, C.RUBBER);
            this.box(x - 0.7, ground + r - 0.5, z - 0.5, x + 0.7, ground + r + 0.5, z + 0.5, C.BUMPER);
            this.box(x - 0.25, top, z - 0.25, x + 0.25, top + 0.7, z + 0.25, C.DARK_STEEL);
            this.box(x - 0.25, top + 0.5, z - 0.25, x + 2.0, top + 0.9, z + 0.25, C.DARK_STEEL);
            this.box(x + 1.6, top + 0.9, z - 0.35, x + 2.3, top + 1.9, z + 0.35, C.BLACK);
        }

        /** The same box either side of x rather than of the middle of the car. */
        pairAt(x, d1, y1, z1, d2, y2, z2, color) {
            this.box(x + d1, y1, z1, x + d2, y2, z2, color);
            this.box(x - d2, y1, z1, x - d1, y2, z2, color);
        }

        /** The height of a profile [[z, y], ...] at z. */
        at(pts, z) {
            const p = pts.slice().sort((a, b) => a[0] - b[0]);
            if (z <= p[0][0]) {
                return p[0][1];
            }
            for (let i = 0; i + 1 < p.length; i++) {
                if (z <= p[i + 1][0]) {
                    const t = (z - p[i][0]) / (p[i + 1][0] - p[i][0]);
                    return p[i][1] + (p[i + 1][1] - p[i][1]) * t;
                }
            }
            return p[p.length - 1][1];
        }

        /**
         * A plank from A to B whose face looks along {@code normal}: w
         * across that face, t deep. The line A-B runs down the middle of
         * the face; the depth goes in behind it (off 0 centres it, 1 puts
         * it in front). Any direction: a pillar that leans two ways, a
         * chamfer along a sloping edge.
         */
        plank(A, B, normal, w, t, color, off, extra) {
            off = off === undefined ? -1 : off;
            const sub = (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
            const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
            const unit = a => {
                const l = Math.hypot(a[0], a[1], a[2]);
                return a.map(v => v / l);
            };
            const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
            const len = Math.hypot(...sub(B, A));
            const L = unit(sub(B, A));
            const N = unit(sub(normal, L.map(v => v * dot(normal, L))));
            const W = cross(L, N);
            const c = [0, 1, 2].map(i => (A[i] + B[i]) / 2 + N[i] * t / 2 * off);
            const vecs = [[L, len], [N, t], [W, w]];
            // which of the box's own axes each one lies nearest: the least turning
            let best = null;
            for (const perm of [[0, 1, 2], [0, 2, 1], [1, 0, 2], [1, 2, 0], [2, 0, 1], [2, 1, 0]]) {
                const cols = perm.map((vi, axis) => {
                    const v = vecs[vi][0];
                    return v[axis] < 0 ? v.map(x => -x) : v;
                });
                const det = dot(cols[0], cross(cols[1], cols[2]));
                const score = cols[0][0] + cols[1][1] + cols[2][2];
                if (det > 0 && (!best || score > best.score)) {
                    best = {perm: perm, cols: cols, score: score};
                }
            }
            const [ax, ay, az] = best.cols;
            const size = best.perm.map(vi => vecs[vi][1]);
            const yaw = Math.asin(Math.max(-1, Math.min(1, -ax[2]))) / DEG;
            const tilt = -Math.atan2(ay[2], az[2]) / DEG;
            const roll = Math.atan2(ax[1], ax[0]) / DEG;
            return this.box(c[0] - size[0] / 2, c[1] - size[1] / 2, c[2] - size[2] / 2,
                c[0] + size[0] / 2, c[1] + size[1] / 2, c[2] + size[2] / 2, color,
                Object.assign({tilt: tilt, yaw: yaw, roll: roll}, extra));
        }

        /** Runs another vehicle's drawing script on this one: a variant starts from its base. */
        include(name) {
            new Function('k', 'C', fs.readFileSync(DIR + name + '.js', 'utf8'))(this, C);
        }

        /** The same plank on both sides of the car. */
        planks(A, B, normal, w, t, color, off) {
            this.plank(A, B, normal, w, t, color, off);
            this.plank([-A[0], A[1], A[2]], [-B[0], B[1], B[2]], [-normal[0], normal[1], normal[2]], w, t, color, off);
        }

        /**
         * A dash across the car, its face towards the seats at z = back: a
         * centre stack with a radio and heater knobs, vents, a glovebox, and
         * a hooded cluster with two dials in front of the driver. The hood's
         * top, top + 1, is what the game hangs the steering wheel from.
         */
        dash(o) {
            const {half, back, front, bottom, top} = o;
            const side = o.driver === undefined ? 6.4 : o.driver;
            const T = o.color || C.TRIM;
            this.box(-half, bottom, back, half, top, front, T);
            if (o.reach !== undefined) {
                this.box(-half, top - 0.7, front, half, top, o.reach, T);
            }
            // the centre stack
            this.box(-2.4, bottom - 2.4, back - 0.5, 2.4, top - 0.3, back + 0.3, C.PLASTIC);
            this.box(-1.9, top - 2.4, back - 0.62, 1.9, top - 1.3, back - 0.5, C.BLACK);
            this.box(-1.5, top - 2.1, back - 0.7, 0.2, top - 1.6, back - 0.62, C.DISPLAY);
            this.box(0.8, top - 2.1, back - 0.74, 1.3, top - 1.6, back - 0.62, C.CHROME);
            for (const x of [-1.4, 0, 1.4]) {
                this.box(x - 0.3, top - 3.7, back - 0.72, x + 0.3, top - 3.1, back - 0.5, C.CHROME);
            }
            this.pair(0.3, top - 1.0, back - 0.58, 2.1, top - 0.5, back - 0.5, C.BLACK);
            this.pair(half - 3.2, top - 1.4, back - 0.08, half - 0.9, top - 0.7, back, C.BLACK);
            // the glovebox, on the passenger's side
            const p = side > 0 ? -1 : 1;
            this.box(p * 3.6, bottom + 0.5, back - 0.08, p * (half - 1.2), top - 1.9, back, '#34353A');
            this.box(p * 7.2, top - 2.6, back - 0.16, p * 8.6, top - 2.3, back - 0.08, C.CHROME);
            // the cluster: a hood over a black face with two dials
            this.box(side - 2.9, top - 0.1, back - 2.1, side + 2.9, top + 1.0, back + 0.6, T);
            this.box(side - 2.6, top - 2.3, back - 1.3, side + 2.6, top - 0.1, back, C.BLACK);
            for (const x of [-1.25, 1.25]) {
                this.box(side + x - 0.95, top - 2.0, back - 1.38, side + x + 0.95, top - 0.4, back - 1.3, C.DIAL);
                this.box(side + x - 0.1, top - 1.3, back - 1.44, side + x + 0.7, top - 1.1, back - 1.38, C.NEEDLE,
                    {roll: x < 0 ? 35 : -30});
            }
        }

        /** The tunnel between the front seats, with a gear lever and a handbrake on it. */
        console(o) {
            const half = o.half || 1.5;
            this.box(-half, o.floor, o.back, half, o.top, o.front, o.color || C.PLASTIC);
            if (o.lever !== undefined) {
                this.box(-0.8, o.top, o.lever - 0.9, 0.8, o.top + 0.3, o.lever + 0.9, C.BLACK);
                this.beam([0, o.top + 0.2, o.lever], [0, o.top + 2.6, o.lever - 0.5], 0.3, 0.3, C.CHROME);
                this.box(-0.5, o.top + 2.5, o.lever - 1.0, 0.5, o.top + 3.3, o.lever, C.BLACK);
            }
            if (o.brake !== undefined) {
                this.beam([0, o.top + 0.2, o.brake], [0, o.top + 1.1, o.brake + 3.2], 0.6, 0.5, C.BLACK);
                this.box(-0.35, o.top + 1.0, o.brake + 3.0, 0.35, o.top + 1.5, o.brake + 3.5, C.CHROME);
            }
        }

        /** Pedals in front of the driver at x: the accelerator towards the middle of the car. */
        pedals(x, z, floor, clutch) {
            const s = x > 0 ? -1 : 1;
            const pad = (at, w, h, colour) => this.box(at - w / 2, floor + 0.9, z - 0.15, at + w / 2, floor + 0.9 + h,
                z + 0.15, colour, {tilt: -28});
            pad(x + s * 2.0, 0.7, 1.9, C.BLACK);
            pad(x + s * 0.4, 1.1, 1.2, C.BLACK);
            if (clutch !== false) {
                pad(x - s * 1.3, 1.0, 1.2, C.BLACK);
            }
        }

        /** Wipers parked along the foot of the windscreen. */
        wipers(o) {
            const y = o.y, z = o.z, len = o.len || 9;
            for (const x of o.pivots || [1.2, -9.2]) {
                this.box(x - 0.35, y - 0.15, z - 0.35, x + 0.35, y + 0.25, z + 0.35, C.BLACK);
                this.beam([x, y + 0.2, z], [x + len * 0.55, y + 0.3, z - 0.25], 0.25, 0.2, C.BLACK);
                this.beam([x + 0.6, y + 0.12, z - 0.45], [x + len, y + 0.12, z - 0.45], 0.4, 0.3, C.BLACK);
            }
        }

        /** A door handle on each side: a dark recess with a flap in it. */
        handles(x, y, z, colour) {
            this.pair(x, y, z - 1.1, x + 0.12, y + 0.8, z + 1.1, C.BLACK);
            this.pair(x + 0.1, y + 0.2, z - 0.85, x + 0.26, y + 0.6, z + 0.85, colour || C.CHROME);
        }

        /**
         * The trim on the inside of the doors, both sides: a card from the
         * floor to the sill, an armrest and a winder for each door.
         * doors: [[zRear, zFront], ...]
         */
        doorCards(o) {
            const col = o.color || '#3C3F46';
            this.pair(o.x - 0.4, o.floor, o.back, o.x, o.top, o.front, col);
            for (const [a, b] of o.doors) {
                const m = (a + b) / 2, l = (b - a) * 0.3;
                this.pair(o.x - 1.2, o.arm, m - l, o.x - 0.4, o.arm + 0.8, m + l, C.SEAT);
                this.pair(o.x - 0.7, o.arm - 2.4, b - (b - a) * 0.22, o.x - 0.4, o.arm - 1.8, b - (b - a) * 0.22 + 0.6, C.CHROME);
                this.pair(o.x - 0.5, o.floor + 1.0, a + 0.8, o.x - 0.4, o.arm - 3.2, b - 0.8, C.SEAT_CLOTH);
            }
        }

        /** Front seats, the driver's at +x: see GenVehicles.frontSeats, in finished coordinates. */
        frontSeats(o) {
            const {floor, top, back, front, backTop} = o;
            const mid = o.x === undefined ? 6.8 : o.x, w = o.w || 4.0;
            const out = mid - w, edge = mid + w, inn = out + 1.2, m2 = edge - 1.2;
            const rake = o.rake === undefined ? 8 : o.rake;
            const S = o.seat || C.SEAT, CL = o.cloth || C.SEAT_CLOTH;
            this.sym(() => {
                // the cushion: base, cloth, raised bolsters, a front lip
                this.box(out, floor, back + 1.4, edge, top - 0.5, front, S);
                this.box(inn, top - 0.5, back + 1.6, m2, top, front - 0.5, CL);
                this.box(out, top - 0.5, back + 1.4, inn, top + 0.5, front, S);
                this.box(m2, top - 0.5, back + 1.4, edge, top + 0.5, front, S);
                this.box(inn, top - 0.5, front - 0.5, m2, top, front, S);
                // the backrest, leant back a little
                const lean = Math.tan(rake * DEG) * (backTop - top);
                this.slab(out, edge, top - 0.2, back + 1.2, backTop, back + 1.2 - lean, 1.2, S, -1);
                this.slab(inn, m2, top + 0.8, back + 1.3, backTop - 1.0, back + 1.3 - lean * 0.85, 0.3, CL, 1);
                this.slab(out, inn, top, back + 1.3, backTop - 0.4, back + 1.3 - lean * 0.95, 0.8, S, 1);
                this.slab(m2, edge, top, back + 1.3, backTop - 0.4, back + 1.3 - lean * 0.95, 0.8, S, 1);
                if (o.headrest !== false) {
                    const hz = back + 0.6 - lean;
                    this.box(mid - 1.2, backTop, hz - 0.2, mid - 0.8, backTop + 0.9, hz + 0.2, C.CHROME);
                    this.box(mid + 0.8, backTop, hz - 0.2, mid + 1.2, backTop + 0.9, hz + 0.2, C.CHROME);
                    this.box(mid - 2.0, backTop + 0.8, hz - 0.7, mid + 2.0, backTop + 3.2, hz + 0.6, S, {tilt: rake});
                    this.box(mid - 1.4, backTop + 1.3, hz + 0.55, mid + 1.4, backTop + 2.7, hz + 0.75, CL, {tilt: rake});
                }
            });
        }

        /** A bench across the car: see GenVehicles.rearBench. */
        rearBench(o) {
            const {half, floor, top, back, front, backTop} = o;
            const places = o.places || 2;
            const rake = o.rake === undefined ? 10 : o.rake;
            const S = o.seat || C.SEAT, CL = o.cloth || C.SEAT_CLOTH;
            const lean = Math.tan(rake * DEG) * (backTop - top);
            this.box(-half, floor, back, half, top - 0.4, front, S);
            this.slab(-half, half, top - 0.6, back + 1.2, backTop, back + 1.2 - lean, 1.2, S, -1);
            this.pair(half - 0.9, top - 0.4, back + 1.2, half, top + 0.3, front, S);
            const width = 2 * half / places;
            for (let i = 0; i < places; i++) {
                const a = -half + width * i + 0.7, b = -half + width * (i + 1) - 0.7;
                this.box(Math.max(a, -half + 0.9), top - 0.4, back + 1.4, Math.min(b, half - 0.9), top, front - 0.5, CL);
                this.slab(a + 0.1, b - 0.1, top + 0.6, back + 1.3, backTop - 0.9, back + 1.3 - lean * 0.9, 0.3, CL, 1);
                if (o.headrest) {
                    const mx = (a + b) / 2, hz = back + 0.6 - lean;
                    this.box(mx - 1.8, backTop, hz - 0.6, mx + 1.8, backTop + 2.2, hz + 0.6, S, {tilt: rake});
                }
            }
        }

        // ---- checks, then Blockbench ------------------------------------------

        summary() {
            const lo = [1e9, 1e9, 1e9], hi = [-1e9, -1e9, -1e9];
            const colours = new Set();
            for (const b of this.boxes) {
                if (b.group.split('/')[0].startsWith('_')) {
                    continue;
                }
                for (let i = 0; i < 3; i++) {
                    lo[i] = Math.min(lo[i], b.from[i]);
                    hi[i] = Math.max(hi[i], b.to[i]);
                }
                colours.add(b.color);
            }
            return {
                name: this.name, boxes: this.boxes.filter(b => !b.group.split('/')[0].startsWith('_')).length,
                colours: colours.size, min: lo.map(r2), max: hi.map(r2)
            };
        }

        /** Draws the wheels the game will draw, to look at: they are not part of the shape. */
        referenceWheels() {
            const keep = this.group;
            this.group = '_wheels';
            for (const w of this.wheels) {
                const [x, y, z] = w.at, r = w.radius, hw = w.width / 2;
                const n = 8;
                const flat = 2 * r * Math.tan(Math.PI / (2 * n)) * 0.98;
                for (let i = 0; i < n; i++) {
                    this.box(x - hw, y - r * 0.992, z - flat / 2, x + hw, y + r * 0.992, z + flat / 2, C.RUBBER,
                        {tilt: i * 180 / n, name: 'tyre'});
                }
                const hr = r * 0.58, hf = 2 * hr * Math.tan(Math.PI / (2 * n));
                for (let i = 0; i < n; i++) {
                    this.box(x - hw - 0.15, y - hr, z - hf / 2, x + hw + 0.15, y + hr, z + hf / 2, '#A9AEB4',
                        {tilt: i * 180 / n, name: 'hub'});
                }
            }
            this.group = keep;
        }

        /**
         * Draws, to look at, a player in every seat and the steering wheel the
         * game puts in front of the driver. A seat is where the hips go: the
         * feet hang 9.6 below it, the body starts 2.4 over it, and a player is
         * drawn a pixel to a sixteenth, so the figure is the game's own size.
         */
        referenceRiders() {
            const keep = this.group;
            this.group = '_riders';
            const SKIN = '#C69C7A', SHIRT = '#3E8FB0', LEGS = '#3B3F8C';
            for (const [x, y, z] of this.seats) {
                const hip = y + 2.4;
                this.box(x - 4, hip, z - 2, x + 4, hip + 12, z + 2, SHIRT, {name: 'body'});
                this.box(x - 4, hip + 12, z - 4, x + 4, hip + 20, z + 4, SKIN, {name: 'head'});
                for (const s of [-1, 1]) {
                    // the legs out in front, a little down at the feet, as the game sits a rider
                    this.beam([x + s * 2, hip, z], [x + s * 2, hip - 1.9, z + 11.8], 4, 4, LEGS, {name: 'leg'});
                    this.box(x + s * 4, hip, z - 2, x + s * 8, hip + 12, z + 2, SHIRT, {name: 'arm'});
                }
            }
            const wheel = this.steeringWheel();
            if (wheel) {
                const [x, y, z] = wheel;
                this.box(x - 2.8, y - 2.8, z - 0.35, x + 2.8, y + 2.8, z + 0.35, '#1E1E21',
                    {tilt: this.wheelRake(), name: 'wheel'});
            }
            this.group = keep;
        }

        /**
         * Where the game puts the middle of the steering wheel, as
         * DefinedVehicleModel works it out from the seat and the dash in
         * front of it; null for what steers with bars, a stick or a yoke.
         */
        steeringWheel() {
            const def = Object.assign({}, this.def, this.overrides);
            const bars = def.handlebars || (def.controls && def.controls.handlebars);
            if (!this.seats.length || bars || ['motorcycle', 'plane', 'helicopter', 'trailer'].includes(def.chassis)) {
                return null;
            }
            const [sx, sy, sz] = this.seats[0];
            const span = 2.8 + 1.0, rake = this.wheelRake() * DEG;
            const solid = this.boxes.filter(b => !b.group.split('/')[0].startsWith('_')
                && !/^#[0-9A-Fa-f]{8}$/.test(b.color));
            let rise = def.steering_wheel_rise;
            if (rise === undefined) {
                let dash = -Infinity;
                for (const b of solid) {
                    if (b.color !== 'paint' && b.to[0] > sx - span && b.from[0] < sx + span && b.from[2] > sz + 2
                        && b.from[2] < sz + 16 && b.to[1] > sy && b.to[1] < sy + 22) {
                        dash = Math.max(dash, b.to[1] - sy);
                    }
                }
                rise = Math.max(9, dash - 2.6);
            }
            const wy = sy + rise, rim = 2.8 + 0.6;
            const top = wy + rim * Math.cos(rake), bottom = wy - rim * Math.cos(rake), lean = rim * Math.sin(rake);
            let reach = 10;
            for (const b of solid) {
                if (b.to[0] > sx - span && b.from[0] < sx + span && b.to[1] > bottom && b.from[1] < top
                    && b.from[2] > sz + 2) {
                    const ahead = b.to[1] > wy ? lean * (Math.min(b.to[1], top) - wy) / (top - wy) : 0;
                    reach = Math.min(reach, b.from[2] - sz - 0.8 - Math.max(ahead, 0.4));
                }
            }
            return [sx, wy, sz + Math.max(4, reach)];
        }

        /** Degrees the steering wheel leans back from upright: the definition's, or a car's 28. */
        wheelRake() {
            const def = Object.assign({}, this.def, this.overrides);
            const rake = def.steering_wheel_rake !== undefined ? def.steering_wheel_rake
                : def.controls && def.controls.steering_wheel_rake;
            return rake === undefined ? 28 : rake;
        }

        /**
         * Where the column the game draws under the steering wheel ends, and
         * the way it runs on from there (forward and down, as long as the
         * wheel leans back), so a model can carry it on into the dash or the
         * frame; null where nothing has a wheel.
         */
        column() {
            const wheel = this.steeringWheel();
            if (!wheel) {
                return null;
            }
            const rake = this.wheelRake() * DEG;
            const dir = [0, -Math.sin(rake), Math.cos(rake)];
            return {wheel: wheel, dir: dir, end: wheel.map((v, i) => v + dir[i] * 3.5)};
        }

        /** Builds the Blockbench project from the boxes. */
        build() {
            const sum = this.summary();
            if (sum.boxes > 512) {
                throw new Error(this.name + ': ' + sum.boxes + ' boxes, the game takes 512');
            }
            // the palette
            const colours = SPECIAL.slice();
            for (const b of this.boxes) {
                if (!colours.includes(b.color)) {
                    colours.push(b.color);
                }
            }
            if (colours.length > PALETTE * PALETTE) {
                throw new Error(this.name + ': too many colours');
            }
            const paint = DYES[(this.overrides.color || this.def.color || 'white')] || '#F9FFFE';
            const shown = c => c === 'paint' ? paint : c === 'paint_dark' ? shadeHex(paint, 0.78)
                : c === 'paint_light' ? shadeHex(paint, 1.15).replace(/^#/, '#') : c === 'cargo' ? '#80D08A2E' : c;
            const canvas = document.createElement('canvas');
            canvas.width = PALETTE;
            canvas.height = PALETTE;
            const ctx = canvas.getContext('2d');
            const image = ctx.createImageData(PALETTE, PALETTE);
            colours.forEach((c, i) => {
                const hex = shown(c).slice(1);
                const argb = hex.length === 8 ? hex : 'FF' + hex;
                image.data[i * 4] = parseInt(argb.slice(2, 4), 16);
                image.data[i * 4 + 1] = parseInt(argb.slice(4, 6), 16);
                image.data[i * 4 + 2] = parseInt(argb.slice(6, 8), 16);
                image.data[i * 4 + 3] = parseInt(argb.slice(0, 2), 16);
            });
            ctx.putImageData(image, 0, 0);

            // a fresh project, replacing an earlier drawing of the same vehicle
            const old = ModelProject.all.find(p => p.name === this.name);
            if (old) {
                old.select();
                old.saved = true;
                old.close(true);
            }
            newProject(Formats.free);
            Project.name = this.name;
            Project.texture_width = PALETTE;
            Project.texture_height = PALETTE;
            const tex = new Texture({name: 'palette.png'}).fromDataURL(canvas.toDataURL('image/png')).add(false);
            tex.uv_width = PALETTE;
            tex.uv_height = PALETTE;

            const groups = {};
            const groupFor = path => {
                if (groups[path]) {
                    return groups[path];
                }
                const cut = path.lastIndexOf('/');
                const g = new Group({name: cut < 0 ? path : path.slice(cut + 1), origin: [0, 0, 0]});
                if (cut >= 0) {
                    g.addTo(groupFor(path.slice(0, cut)));
                }
                g.init();
                g.isOpen = false;
                groups[path] = g;
                return g;
            };
            const lift = this.lifts();
            for (const [n, b] of this.boxes.entries()) {
                const i = colours.indexOf(b.color);
                const u = i % PALETTE, v = Math.floor(i / PALETTE);
                const uv = [u + 0.25, v + 0.25, u + 0.75, v + 0.75];
                const from = [-b.to[0], b.from[1], -b.to[2]], to = [-b.from[0], b.to[1], -b.from[2]];
                const cube = new Cube({
                    name: b.name, from: from, to: to, box_uv: false, autouv: 0,
                    origin: [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2],
                    rotation: [b.tilt, b.yaw, -b.roll]
                });
                // a pane of glass has two faces and no edges, as the game draws it
                const size = [to[0] - from[0], to[1] - from[1], to[2] - from[2]];
                const thin = Math.min(...size);
                const pane = /^#[0-9A-Fa-f]{8}$/.test(b.color) && thin <= 0.1
                    ? [['east', 'west'], ['up', 'down'], ['north', 'south']][size.indexOf(thin)] : null;
                for (const f in cube.faces) {
                    cube.faces[f].extend({uv: uv, texture: pane && !pane.includes(f) ? null : tex.uuid});
                }
                cube.inflate = pane ? 0 : lift[n];
                cube.addTo(groupFor(b.group)).init();
            }
            for (const path in groups) {
                if (path.split('/')[0].startsWith('_')) {
                    groups[path].export = false;
                }
            }
            CarKit.palettes[Project.uuid] = colours;
            Canvas.updateAll();
            return sum;
        }

        /**
         * As the game does: a box that shares a face with a bigger one of
         * another colour is pushed out a hair past it, a step more for each
         * box it sits on in turn, so the two never fight over the face.
         */
        lifts() {
            const B = this.boxes, n = B.length;
            const vol = b => (b.to[0] - b.from[0]) * (b.to[1] - b.from[1]) * (b.to[2] - b.from[2]);
            const order = B.map((b, i) => i).sort((p, q) => vol(B[q]) - vol(B[p]));
            const shares = (a, b) => {
                if (a.color === b.color || a.tilt !== b.tilt || a.roll !== b.roll) {
                    return false;
                }
                for (let axis = 0; axis < 3; axis++) {
                    if (Math.abs(a.from[axis] - b.from[axis]) >= 1e-3 && Math.abs(a.to[axis] - b.to[axis]) >= 1e-3) {
                        continue;
                    }
                    let overlaps = true;
                    for (let other = 0; other < 3; other++) {
                        if (other !== axis && Math.min(a.to[other], b.to[other]) - Math.max(a.from[other], b.from[other]) <= 1e-3) {
                            overlaps = false;
                        }
                    }
                    if (overlaps) {
                        return true;
                    }
                }
                return false;
            };
            const level = new Array(n).fill(0);
            for (let p = 0; p < n; p++) {
                const i = order[p];
                for (let q = 0; q < p; q++) {
                    const j = order[q];
                    if (level[j] + 1 > level[i] && shares(B[i], B[j])) {
                        level[i] = level[j] + 1;
                    }
                }
            }
            return level.map(l => l * 0.05);
        }

        /** Builds it, saves the .bbmodel next to the scripts and writes the definition's shape. */
        finish(o) {
            o = o || {};
            if (o.wheels !== false) {
                this.referenceWheels();
            }
            if (o.riders) {
                this.referenceRiders();
            }
            const sum = this.build();
            const wheel = this.steeringWheel();
            if (wheel) {
                sum.steeringWheel = wheel.map(r2);
            }
            CarKit.save();
            if (o.write !== false) {
                CarKit.exportOpen(this.overrides);
            }
            return sum;
        }
    }

    const CarKit = {
        C: C,
        palettes: (window.CarKit && window.CarKit.palettes) || {},

        /** Runs the drawing script for a vehicle. */
        run(name, o) {
            const code = fs.readFileSync(DIR + name + '.js', 'utf8');
            const k = new Kit(name);
            new Function('k', 'C', code)(k, C);
            return JSON.stringify(k.finish(o));
        },

        /**
         * Draws a vehicle as its definition has it now, without saving
         * anything: for looking at what is there, and for checking that what
         * goes into Blockbench comes out the same.
         */
        importDef(name) {
            const k = new Kit(name);
            for (const b of k.def.shape) {
                k.g(b.opens ? b.opens : b.steers ? 'steer' : b.spins ? SPINNERS[b.spin_axis || 'z'] : 'body');
                k.box(b.from[0], b.from[1], b.from[2], b.to[0], b.to[1], b.to[2], b.color || 'paint',
                    {tilt: b.tilt, yaw: b.yaw, roll: b.roll});
            }
            k.referenceWheels();
            k.build();
            const key = b => [b.from, b.to, b.color || 'paint', b.tilt || 0, b.yaw || 0, b.roll || 0,
                b.opens || '', !!b.steers, b.spins ? b.spin_axis || 'z' : ''].join('|');
            const norm = b => Object.assign({}, b, {
                from: b.from.map((v, i) => Math.min(v, b.to[i])), to: b.to.map((v, i) => Math.max(v, b.from[i]))
            });
            const was = k.def.shape.map(norm).map(key).sort(), is = CarKit.shape().map(key).sort();
            const differ = was.filter((v, i) => v !== is[i]);
            return JSON.stringify({boxes: was.length, out: is.length, differ: differ.length, first: differ[0],
                got: is[was.indexOf(differ[0])]});
        },

        /** Saves the open project as tools/vehicles/<name>.bbmodel. */
        save() {
            const path = DIR + Project.name + '.bbmodel';
            fs.writeFileSync(path, Codecs.project.compile());
            Project.save_path = path;
            Project.saved = true;
            return path;
        },

        /** Opens a saved drawing. */
        open(name) {
            const path = DIR + name + '.bbmodel';
            loadModelFile({path: path, content: fs.readFileSync(path, 'utf8')});
            return Project.name;
        },

        /** The colour of each palette cell of the open project. */
        colours() {
            if (CarKit.palettes[Project.uuid]) {
                return CarKit.palettes[Project.uuid];
            }
            const tex = Texture.all[0];
            const data = tex.canvas.getContext('2d').getImageData(0, 0, PALETTE, PALETTE).data;
            const known = Object.values(C);
            const out = SPECIAL.slice();
            for (let i = SPECIAL.length; i < PALETTE * PALETTE; i++) {
                const [r, g, b, a] = [data[i * 4], data[i * 4 + 1], data[i * 4 + 2], data[i * 4 + 3]];
                const hex = v => v.toString(16).padStart(2, '0').toUpperCase();
                let colour = '#' + (a < 255 ? hex(a) : '') + hex(r) + hex(g) + hex(b);
                if (a < 255) {
                    // a canvas keeps see-through colours only roughly: snap to the one that was meant
                    for (const k of known) {
                        if (k.length === 9 && parseInt(k.slice(1, 3), 16) === a
                            && [3, 5, 7].every((p, j) => Math.abs(parseInt(k.slice(p, p + 2), 16) - [r, g, b][j]) <= 4)) {
                            colour = k;
                        }
                    }
                }
                out.push(colour);
            }
            return out;
        },

        /** The open project's cubes, as the boxes of a definition's shape. */
        shape() {
            const colours = CarKit.colours();
            const out = [];
            const problems = [];
            const walk = (node, flags, hidden) => {
                if (node instanceof Group) {
                    if (node.rotation.some(v => Math.abs(v) > 1e-4)) {
                        problems.push('group ' + node.name + ' is rotated: rotate its cubes instead');
                    }
                    const top = node.parent === 'root';
                    const f = Object.assign({}, flags);
                    if (top && FLAGS[node.name]) {
                        Object.assign(f, FLAGS[node.name]);
                    }
                    const hide = hidden || (top && node.name.startsWith('_'));
                    node.children.forEach(child => walk(child, f, hide));
                    return;
                }
                if (!(node instanceof Cube) || hidden) {
                    return;
                }
                const from = node.from, to = node.to, rot = node.rotation, o = node.origin;
                let c = [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2];
                const size = [Math.abs(to[0] - from[0]), Math.abs(to[1] - from[1]), Math.abs(to[2] - from[2])];
                if (rot.some(v => Math.abs(v) > 1e-4)) {
                    // turned about its pivot: the middle moves, the game turns a box about its middle
                    const v = new THREE.Vector3(c[0] - o[0], c[1] - o[1], c[2] - o[2]);
                    v.applyEuler(new THREE.Euler(rot[0] * DEG, rot[1] * DEG, rot[2] * DEG, 'ZYX'));
                    c = [o[0] + v.x, o[1] + v.y, o[2] + v.z];
                    if (rot.some(a => Math.abs(a) > 90.001)) {
                        problems.push('cube ' + node.name + ' is turned more than 90 degrees');
                    }
                }
                const face = node.faces.north.texture !== null ? node.faces.north : Object.values(node.faces)[0];
                const u = Math.floor((face.uv[0] + face.uv[2]) / 2), v = Math.floor((face.uv[1] + face.uv[3]) / 2);
                const colour = colours[v * PALETTE + u] || 'paint';
                // back to the game's frame: x and z turned round
                const box = {
                    from: [r2(-c[0] - size[0] / 2), r2(c[1] - size[1] / 2), r2(-c[2] - size[2] / 2)],
                    to: [r2(-c[0] + size[0] / 2), r2(c[1] + size[1] / 2), r2(-c[2] + size[2] / 2)],
                    color: colour
                };
                if (Math.abs(rot[0]) > 1e-4) {
                    box.tilt = r2(rot[0]);
                }
                if (Math.abs(rot[1]) > 1e-4) {
                    box.yaw = r2(rot[1]);
                }
                if (Math.abs(rot[2]) > 1e-4) {
                    box.roll = r2(-rot[2]);
                }
                Object.assign(box, flags);
                out.push(box);
            };
            Outliner.root.forEach(node => walk(node, {}, false));
            if (problems.length) {
                throw new Error(problems.join('; '));
            }
            return out;
        },

        /** Writes the open project into its vehicle's definition, keeping everything else in the file. */
        exportOpen(overrides) {
            const file = DATA + Project.name + '.json';
            const def = Object.assign(JSON.parse(fs.readFileSync(file, 'utf8')), overrides || {});
            def.shape = CarKit.shape();
            if (def.shape.length > 512) {
                throw new Error(def.shape.length + ' boxes, the game takes 512');
            }
            const one = v => JSON.stringify(v).replace(/"(\w+)":/g, '"$1": ').replace(/,(?=\S)/g, ', ')
                .replace(/^\{/, '{ ').replace(/\}$/, ' }');
            const lines = [];
            for (const key of Object.keys(def)) {
                const v = def[key];
                if (Array.isArray(v) && v.length && typeof v[0] === 'object') {
                    lines.push('  "' + key + '": [\n' + v.map(e => '    ' + (Array.isArray(e)
                        ? JSON.stringify(e).replace(/,/g, ', ') : one(e))).join(',\n') + '\n  ]');
                } else {
                    lines.push('  "' + key + '": ' + (v !== null && typeof v === 'object' ? one(v) : JSON.stringify(v)));
                }
            }
            fs.writeFileSync(file, '{\n' + lines.join(',\n') + '\n}\n');
            return file + ': ' + def.shape.length + ' boxes';
        },

        /**
         * Photographs the open project from several sides onto one sheet.
         * views: [[x, y, z], ...] where the camera stands, in the game's
         * frame and in blocks from the middle of the car; ortho for a flat
         * view.
         */
        shots(file, views, o) {
            o = o || {};
            const preview = Preview.selected;
            const w = preview.canvas.width, h = preview.canvas.height;
            const cols = o.cols || Math.min(views.length, 2), rows = Math.ceil(views.length / cols);
            // each tile is the middle of the view, cut to 16:9
            const tw = o.tile || 720, th = Math.round(tw * 9 / 16);
            const sw = Math.min(w, h * 16 / 9), sh = sw * 9 / 16;
            const sheet = document.createElement('canvas');
            sheet.width = tw * cols;
            sheet.height = th * rows;
            const ctx = sheet.getContext('2d');
            const target = o.target || [0, 14, 0];
            const cam = preview.camPers;
            const keep = {pos: cam.position.clone(), target: preview.controls.target.clone(), fov: cam.fov};
            if (preview.isOrtho) {
                preview.setProjectionMode(false);
            }
            views.forEach((v, i) => {
                // the fov is for the tile, the canvas is taller than the tile
                const fov = v[3] || o.fov || 30;
                cam.fov = 2 * Math.atan(Math.tan(fov * DEG / 2) * h / sh) / DEG;
                cam.updateProjectionMatrix();
                cam.position.set(-v[0] * 16, v[1] * 16, -v[2] * 16);
                preview.controls.target.set(-target[0], target[1], -target[2]);
                cam.lookAt(preview.controls.target);
                cam.updateMatrixWorld();
                preview.render();
                ctx.drawImage(preview.canvas, (w - sw) / 2, (h - sh) / 2, sw, sh,
                    (i % cols) * tw, Math.floor(i / cols) * th, tw, th);
            });
            cam.fov = keep.fov;
            cam.updateProjectionMatrix();
            cam.position.copy(keep.pos);
            preview.controls.target.copy(keep.target);
            preview.render();
            const data = sheet.toDataURL('image/png').split(',')[1];
            fs.writeFileSync(file, Buffer.from(data, 'base64'));
            return file + ' ' + sheet.width + 'x' + sheet.height;
        }
    };

    window.CarKit = CarKit;
    window.CarKitClass = Kit;
})();
