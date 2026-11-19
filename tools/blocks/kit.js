/*
 * The drawing kit for the mod's blocks. It runs inside Blockbench.
 *
 * A block is drawn by a script next to this file (toolbox.js and so on), in
 * the block's own terms: sixteenths, x to the east, y up, z to the south,
 * the front to the north, the block itself from 0 to 16 each way. One script
 * may draw several models (a generator and the same generator lit, a lift at
 * each of its stops) and one drawing may be cut into several (a pump two
 * blocks tall). The kit writes each as a block model, shows the first of
 * them in Blockbench and saves that as a .bbmodel beside the script.
 *
 * A box is painted with a colour ("#B02E26") or with a texture
 * ("overthought_vehicles:block/shop_hazard"). Colours live on one shared texture,
 * block/palette.png, a cell each; palette.json beside this file keeps their
 * order, and a new colour is only ever added to the end, so models written
 * earlier stay right. A cell is 16 texels square, drawn in the grain of what
 * the colour is made of (brushed or cast metal, chrome, rubber, plastic,
 * paint, wood, concrete, cloth, or flat for lights, glass and paper; see
 * MATERIAL), and a face takes the part of the cell where it sits in the
 * block, the way it would a texture of its own.
 *
 * A block model is poorer than a drawing: a box turns about one axis only,
 * by 22.5 or 45 degrees, and nothing may reach further than a block past
 * the block it belongs to. The kit refuses what the game would.
 *
 * A script in tools/items/ draws items the same way: its models go to
 * models/item/, are stood round the middle of their block (o.centre false
 * leaves them where drawn), keep the overrides the file already has and get
 * the hand, floor and inventory sizes worked out from what was drawn
 * (o.gui the inventory's angle; o.display 'keep' keeps what the file has).
 *
 * Load it from Blockbench's console, or through the MCP:
 *   CAR_REPO = '/path/to/car'; eval(require('fs').readFileSync(CAR_REPO + '/tools/blocks/kit.js', 'utf8'));
 *   BlockKit.run('toolbox');
 */
(function () {
    const fs = require('fs');
    const REPO = window.CAR_REPO;
    const DIR = REPO + '/tools/blocks/';
    const ITEMS = REPO + '/tools/items/';
    const ASSETS = REPO + '/src/main/resources/assets/overthought_vehicles/';
    const MODELS = ASSETS + 'models/block/';
    const ITEM_MODELS = ASSETS + 'models/item/';
    const PALETTE_FILE = ASSETS + 'textures/block/palette.png';
    const PALETTE_LIST = DIR + 'palette.json';
    const PALETTE = 16;
    const SIDES = ['north', 'south', 'east', 'west', 'up', 'down'];
    const AXES = 'xyz';
    const DEG = Math.PI / 180;
    const LIFT = 0.03;
    // which side of a box looks along which axis: [low, high]
    const FACING = [['west', 'east'], ['down', 'up'], ['north', 'south']];
    // what stands in for a vanilla texture in Blockbench, which has none of them
    const VANILLA = {
        bricks: '#96604F', iron_block: '#DCDCDC', cut_copper: '#BF6A51', smooth_stone: '#9E9E9E',
        redstone_lamp: '#5F3B1E', redstone_lamp_on: '#D8A45C', coal_block: '#101010'
    };

    /** The colours the blocks share, by name. */
    const C = {
        BLACK: '#1B1B1D', RUBBER: '#141416', TRIM: '#2A2A2C', PLASTIC: '#222225', DARK_STEEL: '#3A3F45',
        STEEL: '#5B6168', IRON: '#7C8288', ALU: '#A9AEB4', CHROME: '#C6CACE', WHITE: '#E4E6E8',
        RED: '#B02E26', RED_DARK: '#84221C', RED_LIGHT: '#CC4138', YELLOW: '#E8B620', YELLOW_DARK: '#B88C14',
        ORANGE: '#E07A1C', GREEN: '#4E9A3A', GREEN_DARK: '#35702A', BLUE: '#2F6FB5', BLUE_DARK: '#234F86',
        COPPER: '#BF6A51', BRASS: '#C9A23F', WOOD: '#96683E', WOOD_DARK: '#6E4A2A', OIL: '#1A1410',
        SCREEN: '#0E1512', LED_GREEN: '#56E06A', LED_RED: '#F0483A', LED_AMBER: '#F2B03A', LED_BLUE: '#4FC3F0',
        LED_OFF: '#2E3432', CONCRETE: '#8E9094', DIAL: '#E6E1CF', NEEDLE: '#D8402C'
    };

    function r3(v) {
        const n = Math.round(v * 1000) / 1000;
        return Object.is(n, -0) ? 0 : n;
    }

    function isColour(paint) {
        return /^#[0-9A-Fa-f]{6}$/.test(paint);
    }

    /** The colours on the shared palette, in the order of their cells. */
    function loadPalette() {
        return fs.existsSync(PALETTE_LIST) ? JSON.parse(fs.readFileSync(PALETTE_LIST, 'utf8')) : ['#FFFFFF'];
    }

    /**
     * What a colour is made of, where its name alone would guess wrong. Any
     * other colour goes by the rule in material(): near black is rubber,
     * grey is metal (bright grey worked, darker grey cast), anything with
     * colour in it is paint.
     */
    const MATERIAL = {
        // flat: lights, screens, glass (one pane, no grain), paper, dials, holes, oil
        '#FFFFFF': 'flat', '#0E1512': 'flat', '#56E06A': 'flat', '#F0483A': 'flat', '#F2B03A': 'flat',
        '#4FC3F0': 'flat', '#2E3432': 'flat', '#E6E1CF': 'flat', '#D8402C': 'flat', '#6F94A8': 'flat',
        '#F1EEE6': 'flat', '#F2F2EE': 'flat', '#2A2C30': 'flat', '#2A2D31': 'flat', '#1A1410': 'flat',
        '#3A2E1E': 'flat',
        chrome: ['#C6CACE'],
        brushed: ['#A9AEB4', '#5B6168', '#BFC3C7', '#BF6A51', '#C9A23F', '#B8933F', '#94742F', '#7F858B'],
        cast: ['#7C8288', '#3A3F45', '#3E4349', '#4E5258', '#5D6167', '#9EA3A9', '#A3281F', '#7A6450', '#2B2522'],
        plastic: ['#1B1B1D', '#2A2A2C', '#222225', '#1E1E20', '#26282C', '#E8731C', '#E8E6DC', '#E3B32A'],
        paint: ['#E4E6E8', '#F2F2F2', '#DADADA', '#C4C4C4', '#8D939A', '#62676E', '#A7ADB3', '#2E3135'],
        wood: ['#96683E', '#6E4A2A', '#8A6A48'],
        concrete: ['#8E9094', '#A4A6A9', '#7D7F82', '#D2CCBA'],
        cloth: ['#5E6A75', '#4A545E']
    };
    for (const kind of Object.keys(MATERIAL)) {
        if (Array.isArray(MATERIAL[kind])) {
            MATERIAL[kind].forEach(hex => MATERIAL[hex] = kind);
            delete MATERIAL[kind];
        }
    }

    function rgb(hex) {
        const n = parseInt(hex.slice(1), 16);
        return [n >> 16 & 255, n >> 8 & 255, n & 255];
    }

    function material(hex) {
        if (MATERIAL[hex]) {
            return MATERIAL[hex];
        }
        const c = rgb(hex), hi = Math.max(...c), lo = Math.min(...c);
        const sat = hi ? (hi - lo) / hi : 0, light = (hi + lo) / 2;
        return light < 40 ? 'rubber' : sat < 0.12 ? (light > 150 ? 'brushed' : 'cast') : 'paint';
    }

    /**
     * A colour's cell, PALETTE texels square, in its material's grain: each
     * texel as [how much brighter, how much light added]. The grain is drawn
     * from the colour itself, so a cell looks the same wherever it lands.
     */
    /** How strongly every grain is drawn, over what the patterns below say. */
    const BOLD = 1.2;

    function grain(kind, hex) {
        let s =(parseInt(hex.slice(1), 16) * 7919 + 13) >>> 0 || 1;
        const r = () => {
            s ^= s << 13;
            s >>>= 0;
            s ^= s >> 17;
            s ^= s << 5;
            s >>>= 0;
            return s / 4294967296;
        };
        const rows = [...Array(PALETTE)].map(r), out = [];
        for (let y = 0; y < PALETTE; y++) {
            // a soft band of light across the cell, brightest near the top, the way a shine sits on metal
            const sheen = Math.sin((y + 2) / PALETTE * Math.PI * 2);
            for (let x = 0; x < PALETTE; x++) {
                let f = 1, a = 0;
                const n = r() - 0.5;
                if (kind === 'paint') {
                    f = 1.02 - y * 0.0025 + n * 0.03 - (r() < 0.03 ? 0.04 : 0);
                } else if (kind === 'brushed') {
                    f = 1 + (rows[y] - 0.5) * 0.17 + n * 0.05 + sheen * 0.07;
                } else if (kind === 'chrome') {
                    f = 1 + sheen * 0.16 + n * 0.03 + (y === 3 || y === 4 ? 0.22 : 0);
                } else if (kind === 'cast') {
                    f = 1 + n * 0.10 - (r() < 0.04 ? 0.10 : 0);
                } else if (kind === 'rubber') {
                    // matte, with fine lines like a moulded tread
                    a = n * 5 + (y % 3 === 0 ? -4 : 2);
                } else if (kind === 'plastic') {
                    a = n * 4;
                } else if (kind === 'wood') {
                    const ring = Math.sin(y * 2.1 + Math.sin(x * 0.45 + rows[0] * 6) * 1.6);
                    f = 1 + 0.12 * ring - (ring < -0.7 ? 0.06 : 0) + n * 0.04;
                } else if (kind === 'concrete') {
                    // light and dark patches a few texels across, sand and stones in them, the odd pore
                    const patch = rows[(Math.floor(x / 4) + 4 * Math.floor(y / 4)) % PALETTE] - 0.5;
                    const stone = r();
                    f = 1 + patch * 0.10 + n * 0.16 + (stone < 0.08 ? -0.16 : stone > 0.93 ? 0.10 : 0);
                } else if (kind === 'cloth') {
                    f = 1 + n * 0.10 + ((x + y) % 2 ? 0.04 : -0.04);
                }
                if (kind !== 'flat') {
                    f = 1 + (f - 1) * BOLD;
                    a *= BOLD;
                }
                out.push([f, a]);
            }
        }
        return out;
    }

    /** The shared texture: a cell for each colour, PALETTE cells to a row, each in its material's grain. */
    function paletteCanvas(colours) {
        const size = PALETTE * PALETTE;
        const canvas = document.createElement('canvas');
        canvas.width = size;
        canvas.height = size;
        const ctx = canvas.getContext('2d');
        const image = ctx.createImageData(size, size);
        for (let i = 0; i < PALETTE * PALETTE; i++) {
            const hex = (colours[i] || colours[colours.length - 1]).toUpperCase();
            const base = rgb(hex), cell = grain(material(hex), hex);
            const u0 = (i % PALETTE) * PALETTE, v0 = Math.floor(i / PALETTE) * PALETTE;
            cell.forEach(([f, a], t) => {
                const o = ((v0 + Math.floor(t / PALETTE)) * size + u0 + t % PALETTE) * 4;
                for (let ch = 0; ch < 3; ch++) {
                    image.data[o + ch] = Math.max(0, Math.min(255, Math.round(base[ch] * f + a)));
                }
                image.data[o + 3] = 255;
            });
        }
        ctx.putImageData(image, 0, 0);
        return canvas;
    }

    /** p turned about the origin by degrees round one axis, the way the game turns a box or an item. */
    function turn(p, axis, degrees) {
        const c = Math.cos(degrees * DEG), s = Math.sin(degrees * DEG);
        return axis === 'x' ? [p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c]
            : axis === 'y' ? [p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c]
                : [p[0] * c - p[1] * s, p[0] * s + p[1] * c, p[2]];
    }

    /** Every corner of every box, after the box's own turn. */
    function corners(boxes) {
        const out = [];
        for (const b of boxes) {
            for (let i = 0; i < 8; i++) {
                let p = [i & 1 ? b.to[0] : b.from[0], i & 2 ? b.to[1] : b.from[1], i & 4 ? b.to[2] : b.from[2]];
                if (b.rot) {
                    const o = b.rot.origin;
                    p = turn(p.map((v, k) => v - o[k]), b.rot.axis, b.rot.angle).map((v, k) => v + o[k]);
                }
                out.push(p);
            }
        }
        return out;
    }

    /** The lowest and highest point of a drawing on each axis. */
    function extent(boxes) {
        const min = [Infinity, Infinity, Infinity], max = [-Infinity, -Infinity, -Infinity];
        for (const p of corners(boxes)) {
            for (let a = 0; a < 3; a++) {
                min[a] = Math.min(min[a], p[a]);
                max[a] = Math.max(max[a], p[a]);
            }
        }
        return {min: min, max: max};
    }

    /** The drawing moved, whole, so that its middle is the middle of the block. */
    function centred(boxes) {
        const e = extent(boxes);
        const shift = e.min.map((v, a) => 8 - (v + e.max[a]) / 2);
        const move = p => p.map((v, a) => v + shift[a]);
        return boxes.map(b => Object.assign({}, b, {
            from: move(b.from), to: move(b.to),
            rot: b.rot ? Object.assign({}, b.rot, {origin: move(b.rot.origin)}) : null
        }));
    }

    /**
     * An item's sizes: in the inventory as big as fits the slot at the angle
     * gui, and in a hand or on the floor about as big as a block is there,
     * smaller again in your own hand, whatever size it was drawn at.
     */
    function itemDisplay(boxes, gui) {
        const e = extent(boxes);
        const largest = Math.max(...e.max.map((v, a) => v - e.min[a]));
        const lo = [Infinity, Infinity], hi = [-Infinity, -Infinity];
        for (const p of corners(boxes)) {
            const v = turn(turn(turn(p.map(x => x - 8), 'z', gui[2]), 'y', gui[1]), 'x', gui[0]);
            for (let a = 0; a < 2; a++) {
                lo[a] = Math.min(lo[a], v[a]);
                hi[a] = Math.max(hi[a], v[a]);
            }
        }
        const fit = Math.min(1.6, 15 / Math.max(hi[0] - lo[0], hi[1] - lo[1]));
        const held = Math.max(0.35, Math.min(0.75, 6.5 / largest));
        const framed = Math.min(1, held * 2), third = held * 0.9, first = held * 0.6;
        const s = v => [r3(v), r3(v), r3(v)];
        return {
            gui: {rotation: gui, translation: [r3(-(lo[0] + hi[0]) / 2 * fit), r3(-(lo[1] + hi[1]) / 2 * fit), 0], scale: s(fit)},
            ground: {translation: [0, 3, 0], scale: s(held)},
            fixed: {scale: s(framed)},
            head: {scale: s(framed)},
            thirdperson_righthand: {rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: s(third)},
            thirdperson_lefthand: {rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: s(third)},
            firstperson_righthand: {rotation: [0, 45, 0], translation: [0, 3.5, 0], scale: s(first)},
            firstperson_lefthand: {rotation: [0, 225, 0], translation: [0, 3.5, 0], scale: s(first)}
        };
    }

    /** Where a face falls on a texture when nobody says: by where it sits in the block, folded back onto the texture. */
    function autoUv(from, to, side) {
        const [fx, fy, fz] = from, [tx, ty, tz] = to;
        const uv = {
            down: [fx, 16 - tz, tx, 16 - fz], up: [fx, fz, tx, tz],
            north: [16 - tx, 16 - ty, 16 - fx, 16 - fy], south: [fx, 16 - ty, tx, 16 - fy],
            west: [fz, 16 - ty, tz, 16 - fy], east: [16 - tz, 16 - ty, 16 - fz, 16 - fy]
        }[side];
        const fold = (a, b) => {
            if (b - a >= 16) {
                return [0, 16];
            }
            const shift = Math.floor(a / 16) * 16;
            a -= shift;
            b -= shift;
            return b > 16 ? [a - (b - 16), 16] : [a, b];
        };
        const [u1, u2] = fold(uv[0], uv[2]), [v1, v2] = fold(uv[1], uv[3]);
        return [r3(u1), r3(v1), r3(u2), r3(v2)];
    }

    /**
     * Where a face painted with the colour in cell i falls: inside that cell,
     * by where the face sits in the block, so the grain of one colour runs on
     * from box to box.
     */
    function cellUv(i, b, side) {
        const u = i % PALETTE, v = Math.floor(i / PALETTE), r = autoUv(b.from, b.to, side);
        const at = (cell, t) => Math.round((cell + t / 16) * 10000) / 10000;
        return [at(u, r[0]), at(v, r[1]), at(u, r[2]), at(v, r[3])];
    }

    class Kit {
        constructor(name) {
            this.name = name;
            this.models = [];
            this.boxes = null;
            this.group = 'body';
            this.C = C;
        }

        /** Boxes added from here on go into this group; "a/b" nests. */
        g(path) {
            this.group = path;
            return this;
        }

        /**
         * One model to write. o: parent, ao (false turns the block's soft
         * shadows off), display, particle (a texture), cells ({suffix:
         * [x, y, z]} cuts the drawing into a model per block), shrink (a
         * size in sixteenths: the whole drawing shrunk to it, for the item
         * of a machine bigger than a block), file (where it goes, relative
         * to models/block/, when not <name>), preview (true: drawn to look
         * at, never written). An item's: gui (its angle in the inventory),
         * centre false, display 'keep', overrides. What o leaves unsaid is
         * kept from the model already there.
         */
        model(name, o, draw) {
            this.boxes = [];
            this.group = 'body';
            draw();
            this.models.push({name: name, o: o || {}, boxes: this.boxes});
            this.boxes = null;
        }

        // ---- the one primitive -------------------------------------------------

        /**
         * A box, corner to corner. o: rot [axis, degrees, origin] (the origin
         * its own middle unless given), faces {side: paint or null}, only
         * [sides] or no [sides] to draw some of them, shade false for a face
         * that glows, uv {side: [u1, v1, u2, v2]}, tint.
         */
        box(x1, y1, z1, x2, y2, z2, paint, o) {
            o = o || {};
            const from = [Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)];
            const to = [Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)];
            const faces = {};
            for (const side of SIDES) {
                const off = (o.only && !o.only.includes(side)) || (o.no && o.no.includes(side));
                faces[side] = off ? null : paint;
            }
            Object.assign(faces, o.faces || {});
            let rot = null;
            if (o.rot && o.rot[1]) {
                if (![22.5, 45].includes(Math.abs(o.rot[1]))) {
                    throw new Error(this.name + ': a box turns by 22.5 or 45 degrees, not ' + o.rot[1]);
                }
                rot = {axis: o.rot[0], angle: o.rot[1], origin: o.rot[2] || from.map((v, i) => (v + to[i]) / 2)};
            }
            const b = {
                from: from, to: to, paint: paint, faces: faces, rot: rot, shade: o.shade !== false,
                uv: o.uv || {}, tint: o.tint, group: this.group, name: o.name || this.group.split('/').pop()
            };
            this.boxes.push(b);
            return b;
        }

        /** Runs draw, then adds a mirror image of everything it drew: across x (east to west) unless axis says 'z'. */
        sym(draw, axis) {
            const a = axis === 'z' ? 2 : 0;
            const start = this.boxes.length;
            draw();
            for (const b of this.boxes.slice(start)) {
                const from = b.from.slice(), to = b.to.slice();
                from[a] = 16 - b.to[a];
                to[a] = 16 - b.from[a];
                const [lo, hi] = FACING[a];
                const swap = map => Object.assign({}, map, {[lo]: map[hi], [hi]: map[lo]});
                let rot = null;
                if (b.rot) {
                    const origin = b.rot.origin.slice();
                    origin[a] = 16 - origin[a];
                    rot = {axis: b.rot.axis, angle: AXES.indexOf(b.rot.axis) === a ? b.rot.angle : -b.rot.angle, origin: origin};
                }
                const uv = swap(b.uv);
                for (const side of SIDES) {
                    if (uv[side] === undefined) {
                        delete uv[side];
                    }
                }
                this.boxes.push(Object.assign({}, b, {from: from, to: to, faces: swap(b.faces), rot: rot, uv: uv}));
            }
        }

        // ---- round things ------------------------------------------------------

        /**
         * A bar, a drum, a pipe: round about axis, its middle at (cx, cy, cz),
         * r across and len long. Eight sides, or o.sides 16; o.caps paints
         * the two ends, o.turn starts it off the square (22.5 puts a corner
         * where a flat was).
         */
        round(axis, cx, cy, cz, r, len, paint, o) {
            o = o || {};
            const n = o.sides || 8;
            const reach = r * Math.cos(Math.PI / n), half = r * Math.sin(Math.PI / n);
            const a = AXES.indexOf(axis), u = (a + 1) % 3, v = (a + 2) % 3;
            const c = [cx, cy, cz];
            const turns = n === 16 ? [-45, -22.5, 0, 22.5] : o.turn ? [-22.5, 22.5] : [0, 45];
            const ends = FACING[a];
            for (const turn of turns) {
                for (const [across, along] of [[u, v], [v, u]]) {
                    const lo = c.slice(), hi = c.slice();
                    lo[a] -= len / 2;
                    hi[a] += len / 2;
                    lo[across] -= reach;
                    hi[across] += reach;
                    lo[along] -= half;
                    hi[along] += half;
                    const faces = {};
                    faces[FACING[along][0]] = null;
                    faces[FACING[along][1]] = null;
                    faces[ends[0]] = o.caps === undefined ? paint : o.caps;
                    faces[ends[1]] = o.caps === undefined ? paint : o.caps;
                    this.box(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2], paint,
                        Object.assign({}, o, {faces: faces, rot: turn ? [axis, turn, c] : null}));
                }
            }
        }

        /**
         * A box set round the z axis through (8, 8) at a multiple of 22.5
         * degrees, the way a spoke, a knob or a caliper sits on a wheel: drawn
         * as if it stood at the top, u across and w out from the middle,
         * z1..z2 deep. Quarter turns swap its sides, the rest is the box's turn.
         */
        spin(a, u1, w1, z1, u2, w2, z2, paint, o) {
            const quarter = Math.round(a / 90), rest = a - quarter * 90;
            const at = (u, w) => [[u, w], [-w, u], [-u, -w], [w, -u]][((quarter % 4) + 4) % 4];
            const [x1, y1] = at(u1, w1), [x2, y2] = at(u2, w2);
            return this.box(8 + x1, 8 + y1, z1, 8 + x2, 8 + y2, z2, paint,
                Object.assign({}, o, rest ? {rot: ['z', rest, [8, 8, (z1 + z2) / 2]]} : {}));
        }

        // ---- checking ----------------------------------------------------------

        /**
         * A box that shares a face with a bigger one of another paint is
         * pushed out a hair past it, a step more for each box it sits on in
         * turn, so the two never fight over the face.
         */
        static lifts(B) {
            const vol = b => (b.to[0] - b.from[0]) * (b.to[1] - b.from[1]) * (b.to[2] - b.from[2]);
            const turn = b => b.rot ? b.rot.axis + b.rot.angle + b.rot.origin.join(',') : '';
            const order = B.map((b, i) => i).sort((p, q) => vol(B[q]) - vol(B[p]));
            const shares = (a, b) => {
                if (a.paint === b.paint || turn(a) !== turn(b)) {
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
            const level = new Array(B.length).fill(0);
            for (let p = 0; p < B.length; p++) {
                const i = order[p];
                for (let q = 0; q < p; q++) {
                    const j = order[q];
                    if (level[j] + 1 > level[i] && shares(B[i], B[j])) {
                        level[i] = level[j] + 1;
                    }
                }
            }
            return level.map(l => l * LIFT);
        }

        /**
         * The boxes of one block of a drawing, in that block's own frame:
         * clipped to it, a face on a cut left out. With no block named the
         * drawing is one model and is left whole.
         */
        static cell(boxes, at) {
            const lifts = Kit.lifts(boxes);
            if (!at) {
                return boxes.map((b, n) => Object.assign({}, b, {
                    from: b.from.map(v => v - lifts[n]), to: b.to.map(v => v + lifts[n])
                }));
            }
            const base = at.map(v => v * 16);
            const out = [];
            boxes.forEach((b, n) => {
                const from = b.from.slice(), to = b.to.slice();
                const faces = Object.assign({}, b.faces);
                let rot = b.rot;
                if (rot) {
                    const mid = from.map((v, i) => (v + to[i]) / 2);
                    if (mid.some((v, i) => v < base[i] || v >= base[i] + 16)) {
                        return;
                    }
                    rot = Object.assign({}, rot, {origin: rot.origin.map((v, i) => v - base[i])});
                }
                for (let i = 0; i < 3; i++) {
                    if (!rot) {
                        if (from[i] < base[i] - 1e-6) {
                            from[i] = base[i];
                            faces[FACING[i][0]] = null;
                        } else {
                            from[i] -= lifts[n];
                        }
                        if (to[i] > base[i] + 16 + 1e-6) {
                            to[i] = base[i] + 16;
                            faces[FACING[i][1]] = null;
                        } else {
                            to[i] += lifts[n];
                        }
                    } else {
                        from[i] -= lifts[n];
                        to[i] += lifts[n];
                    }
                    from[i] -= base[i];
                    to[i] -= base[i];
                }
                if (from.some((v, i) => to[i] - v <= 1e-6)) {
                    return;
                }
                out.push(Object.assign({}, b, {from: from, to: to, faces: faces, rot: rot}));
            });
            return out;
        }

        /**
         * A whole machine shrunk round its middle to fit size sixteenths
         * across and stood on the block's middle, for its item. A piece too
         * thin to see once shrunk is kept a sliver thick.
         */
        /**
         * A drawing cut into blocks must not lose any of a box between them: a
         * turned box goes whole to the block its middle is in, any other is
         * clipped to each block it reaches, and every one of those must be a
         * block of the machine.
         */
        static allPlaced(m, blocks) {
            const listed = new Set(blocks.map(at => at.join(',')));
            const placed = b => {
                if (b.rot) {
                    return listed.has(b.from.map((v, i) => Math.floor((v + b.to[i]) / 32)).join(','));
                }
                const lo = b.from.map(v => Math.floor(v / 16 + 1e-6)), hi = b.to.map(v => Math.ceil(v / 16 - 1e-6) - 1);
                for (let x = lo[0]; x <= hi[0]; x++) {
                    for (let y = lo[1]; y <= hi[1]; y++) {
                        for (let z = lo[2]; z <= hi[2]; z++) {
                            if (!listed.has(x + ',' + y + ',' + z)) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            };
            const lost = m.boxes.filter(b => !placed(b));
            if (lost.length) {
                throw new Error(m.name + ': ' + lost.length + ' boxes are in no block, the first ' + lost[0].group
                    + ' ' + lost[0].from.join(',') + ' to ' + lost[0].to.join(','));
            }
        }

        static shrunk(boxes, size) {
            const min = [Infinity, Infinity, Infinity], max = [-Infinity, -Infinity, -Infinity];
            for (const b of boxes) {
                for (let a = 0; a < 3; a++) {
                    min[a] = Math.min(min[a], b.from[a]);
                    max[a] = Math.max(max[a], b.to[a]);
                }
            }
            const s = size / Math.max(max[0] - min[0], max[1] - min[1], max[2] - min[2]);
            const bottom = 8 - (max[1] - min[1]) * s / 2;
            const at = (v, a) => a === 1 ? bottom + (v - min[1]) * s : 8 + (v - (min[a] + max[a]) / 2) * s;
            return boxes.map(b => {
                const from = b.from.map(at), to = b.to.map(at);
                for (let a = 0; a < 3; a++) {
                    if (to[a] - from[a] < 0.15) {
                        to[a] = from[a] + 0.15;
                    }
                }
                const rot = b.rot ? Object.assign({}, b.rot, {origin: b.rot.origin.map(at)}) : null;
                return Object.assign({}, b, {from: from, to: to, rot: rot});
            });
        }
    }

    const BlockKit = {
        C: C,

        /** Runs the drawing script for a block and writes its models. o.write false only draws. */
        run(name, o) {
            o = o || {};
            const dir = fs.existsSync(DIR + name + '.js') ? DIR : ITEMS;
            const k = new Kit(name);
            new Function('k', 'C', fs.readFileSync(dir + name + '.js', 'utf8'))(k, C);
            const palette = loadPalette();
            const written = [];
            for (const m of k.models) {
                m.item = dir === ITEMS;
                if (m.item && m.o.centre !== false) {
                    m.boxes = centred(m.boxes);
                }
                if (m.o.preview) {
                    continue;
                }
                if (m.item) {
                    const text = BlockKit.json(m, Kit.cell(m.boxes, null), palette, '');
                    if (o.write !== false) {
                        fs.writeFileSync(BlockKit.file(m, ''), text);
                    }
                    const e = extent(m.boxes), span = a => r3(e.min[a]) + '..' + r3(e.max[a]);
                    written.push(m.name + ' ' + m.boxes.length + ' x ' + span(0) + ' y ' + span(1) + ' z ' + span(2));
                    continue;
                }
                const cells = m.o.cells || {'': null};
                if (Object.keys(cells).length > 1) {
                    Kit.allPlaced(m, Object.values(cells));
                }
                for (const suffix in cells) {
                    const boxes = Kit.cell(m.o.shrink ? Kit.shrunk(m.boxes, m.o.shrink) : m.boxes, cells[suffix]);
                    const text = BlockKit.json(m, boxes, palette, suffix);
                    if (o.write !== false) {
                        fs.writeFileSync(BlockKit.file(m, suffix), text);
                    }
                    written.push(m.name + suffix + ' ' + boxes.length);
                }
            }
            if (palette.length > PALETTE * PALETTE) {
                throw new Error('the palette is full');
            }
            if (o.write !== false) {
                fs.writeFileSync(PALETTE_LIST, JSON.stringify(palette).replace(/,/g, ', ') + '\n');
                fs.writeFileSync(PALETTE_FILE, Buffer.from(paletteCanvas(palette).toDataURL('image/png').split(',')[1], 'base64'));
            }
            const shown = k.models.find(m => m.name === o.show) || k.models[0];
            BlockKit.show(name, shown, palette);
            const path = dir + name + '.bbmodel';
            fs.writeFileSync(path, Codecs.project.compile());
            Project.save_path = path;
            Project.saved = true;
            return JSON.stringify({models: written, shown: shown.name, colours: palette.length});
        },

        file(m, suffix) {
            return (m.item ? ITEM_MODELS : MODELS) + (m.o.file || m.name) + suffix + '.json';
        },

        /** One model's file, a box to a line. */
        json(m, boxes, palette, suffix) {
            const file = BlockKit.file(m, suffix);
            const was = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')) : {};
            const textures = {};
            const keyOf = paint => {
                if (isColour(paint)) {
                    textures.p = 'overthought_vehicles:block/palette';
                    return 'p';
                }
                const key = paint.split('/').pop();
                textures[key] = paint;
                return key;
            };
            const one = v => JSON.stringify(v).replace(/"(\w+)":/g, '"$1": ').replace(/,(?=\S)/g, ', ');
            const lines = [];
            for (const b of boxes) {
                if (b.from.some(v => v < -16 - 1e-6) || b.to.some(v => v > 32 + 1e-6)) {
                    throw new Error(m.name + suffix + ': ' + b.name + ' reaches past what a block model may: '
                        + b.from + ' to ' + b.to);
                }
                const e = {from: b.from.map(r3), to: b.to.map(r3)};
                if (b.rot) {
                    e.rotation = {origin: b.rot.origin.map(r3), axis: b.rot.axis, angle: b.rot.angle};
                }
                if (!b.shade) {
                    e.shade = false;
                }
                e.faces = {};
                for (const side of SIDES) {
                    const paint = b.faces[side];
                    if (!paint) {
                        continue;
                    }
                    const face = {};
                    if (isColour(paint)) {
                        let i = palette.indexOf(paint.toUpperCase());
                        if (i < 0) {
                            i = palette.push(paint.toUpperCase()) - 1;
                        }
                        face.uv = cellUv(i, b, side);
                    } else {
                        face.uv = b.uv[side] || autoUv(b.from, b.to, side);
                    }
                    face.texture = '#' + keyOf(paint);
                    const axis = FACING.findIndex(f => f.includes(side));
                    const edge = FACING[axis][0] === side ? Math.abs(b.from[axis]) < 1e-6 : Math.abs(b.to[axis] - 16) < 1e-6;
                    if (edge && !b.rot && !m.item) {
                        face.cullface = side;
                    }
                    if (b.tint !== undefined) {
                        face.tintindex = b.tint;
                    }
                    e.faces[side] = face;
                }
                if (Object.keys(e.faces).length) {
                    lines.push('    ' + one(e));
                }
            }
            const particle = m.o.particle || (was.textures && was.textures.particle) || 'overthought_vehicles:block/shop_steel';
            const head = {};
            const parent = m.o.parent || was.parent || (m.item ? null : 'minecraft:block/block');
            if (parent) {
                head.parent = parent;
            }
            const ao = m.o.ao !== undefined ? m.o.ao : was.ambientocclusion;
            if (ao === false) {
                head.ambientocclusion = false;
            }
            if (m.o.render_type || was.render_type) {
                head.render_type = m.o.render_type || was.render_type;
            }
            head.textures = Object.assign({particle: particle}, textures);
            let text = '{\n' + Object.keys(head).map(key => '  "' + key + '": ' + one(head[key])).join(',\n')
                + ',\n  "elements": [\n' + lines.join(',\n') + '\n  ]';
            if (!m.item) {
                const display = m.o.display || was.display;
                if (display) {
                    text += ',\n  "display": ' + one(display);
                }
                return text + '\n}\n';
            }
            // An item's sizes and its other looks, a line each.
            const display = (m.o.display === 'keep' && was.display)
                || (typeof m.o.display === 'object' && m.o.display) || itemDisplay(boxes, m.o.gui || [30, 225, 0]);
            const listed = (open, items, close) => open + '\n' + items.map(v => '    ' + v).join(',\n') + '\n  ' + close;
            text += ',\n  "display": ' + listed('{', Object.keys(display).map(key => '"' + key + '": ' + one(display[key])), '}');
            const overrides = m.o.overrides || was.overrides;
            if (overrides) {
                text += ',\n  "overrides": ' + listed('[', overrides.map(one), ']');
            }
            return text + '\n}\n';
        },

        /** Puts a drawing, whole, into a Blockbench project of its own. */
        show(name, m, palette) {
            const old = ModelProject.all.find(p => p.name === name);
            if (old) {
                old.select();
                old.saved = true;
                old.close(true);
            }
            const lifts = Kit.lifts(m.boxes);
            const fits = m.boxes.every(b => b.from.every(v => v >= -16) && b.to.every(v => v <= 32));
            newProject(fits ? Formats.java_block : Formats.free);
            Project.name = name;
            Project.texture_width = 16;
            Project.texture_height = 16;
            const loaded = {};
            const texture = paint => {
                const key = isColour(paint) ? 'overthought_vehicles:block/palette' : paint;
                if (loaded[key]) {
                    return loaded[key];
                }
                const [namespace, rest] = key.split(':');
                const tex = new Texture({name: rest.split('/').pop() + '.png', namespace: namespace, folder: 'block'});
                const file = ASSETS + 'textures/' + rest + '.png';
                if (key === 'overthought_vehicles:block/palette') {
                    tex.fromDataURL(paletteCanvas(palette).toDataURL('image/png'));
                } else if (namespace === 'car' && fs.existsSync(file)) {
                    tex.fromDataURL('data:image/png;base64,' + fs.readFileSync(file).toString('base64'));
                } else {
                    tex.fromDataURL(paletteCanvas([VANILLA[rest.split('/').pop()] || '#808080']).toDataURL('image/png'));
                }
                tex.add(false);
                loaded[key] = tex;
                return tex;
            };
            const groups = {};
            const groupFor = path => {
                if (groups[path]) {
                    return groups[path];
                }
                const cut = path.lastIndexOf('/');
                const g = new Group({name: cut < 0 ? path : path.slice(cut + 1), origin: [8, 8, 8]});
                if (cut >= 0) {
                    g.addTo(groupFor(path.slice(0, cut)));
                }
                g.init();
                g.isOpen = false;
                groups[path] = g;
                return g;
            };
            m.boxes.forEach((b, n) => {
                const cube = new Cube({
                    name: b.name, from: b.from.map(v => v - lifts[n]), to: b.to.map(v => v + lifts[n]), box_uv: false,
                    autouv: 0, shade: b.shade,
                    origin: b.rot ? b.rot.origin : [8, 8, 8],
                    rotation: b.rot ? [0, 1, 2].map(i => AXES[i] === b.rot.axis ? b.rot.angle : 0) : [0, 0, 0]
                });
                for (const side of SIDES) {
                    const paint = b.faces[side];
                    if (!paint) {
                        cube.faces[side].extend({texture: null});
                        continue;
                    }
                    let uv;
                    if (isColour(paint)) {
                        uv = cellUv(palette.indexOf(paint.toUpperCase()), b, side);
                    } else {
                        uv = b.uv[side] || autoUv(b.from, b.to, side);
                    }
                    cube.faces[side].extend({uv: uv, texture: texture(paint).uuid});
                }
                cube.addTo(groupFor(b.group)).init();
            });
            Canvas.updateAll();
        },

        /** Opens a saved drawing. */
        open(name) {
            const path = DIR + name + '.bbmodel';
            loadModelFile({path: path, content: fs.readFileSync(path, 'utf8')});
            return Project.name;
        },

        /**
         * Photographs the open project from several sides onto one sheet.
         * views: [[x, y, z], ...] where the camera stands, in blocks from
         * what it looks at; o.target is that, in sixteenths (the middle of
         * the block unless given).
         */
        shots(file, views, o) {
            o = o || {};
            const preview = Preview.selected;
            const w = preview.canvas.width, h = preview.canvas.height;
            const cols = o.cols || Math.min(views.length, 2), rows = Math.ceil(views.length / cols);
            const tw = o.tile || 720, th = Math.round(tw * 9 / 16);
            const sw = Math.min(w, h * 16 / 9), sh = sw * 9 / 16;
            const sheet = document.createElement('canvas');
            sheet.width = tw * cols;
            sheet.height = th * rows;
            const ctx = sheet.getContext('2d');
            const off = scene.position;
            const target = (o.target || [8, 8, 8]).map((v, i) => v + [off.x, off.y, off.z][i]);
            const cam = preview.camPers;
            const keep = {pos: cam.position.clone(), target: preview.controls.target.clone(), fov: cam.fov};
            if (preview.isOrtho) {
                preview.setProjectionMode(false);
            }
            views.forEach((v, i) => {
                const fov = v[3] || o.fov || 30;
                cam.fov = 2 * Math.atan(Math.tan(fov * DEG / 2) * h / sh) / DEG;
                cam.updateProjectionMatrix();
                cam.position.set(target[0] + v[0] * 16, target[1] + v[1] * 16, target[2] + v[2] * 16);
                preview.controls.target.set(target[0], target[1], target[2]);
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
            fs.writeFileSync(file, Buffer.from(sheet.toDataURL('image/png').split(',')[1], 'base64'));
            return file + ' ' + sheet.width + 'x' + sheet.height;
        }
    };

    window.BlockKit = BlockKit;
})();
