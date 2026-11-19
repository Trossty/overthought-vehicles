"""
Writes models/item/valves.json the way the Blockbench kit (tools/blocks/kit.js)
would from the 'valves' model in engine_parts.js, for when Blockbench is not
to hand. Only box() and round() are ported, with the kit's palette UVs; the
display block is the pistons' own, the two being the same size.

    python3 tools/items/valves_json.py
"""
import json
import math
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'overthought_vehicles')
PALETTE = json.load(open(os.path.join(ROOT, 'tools', 'blocks', 'palette.json')))

SIDES = ['north', 'south', 'east', 'west', 'up', 'down']
AXES = 'xyz'
FACING = [['west', 'east'], ['down', 'up'], ['north', 'south']]
STEEL, DARK_STEEL, IRON, ALU, CHROME = '#5B6168', '#3A3F45', '#7C8288', '#A9AEB4', '#C6CACE'


def r3(v):
    n = round(v * 1000) / 1000
    return 0 if n == 0 else n


def auto_uv(f, t, side):
    fx, fy, fz = f
    tx, ty, tz = t
    uv = {
        'down': [fx, 16 - tz, tx, 16 - fz], 'up': [fx, fz, tx, tz],
        'north': [16 - tx, 16 - ty, 16 - fx, 16 - fy], 'south': [fx, 16 - ty, tx, 16 - fy],
        'west': [fz, 16 - ty, tz, 16 - fy], 'east': [16 - tz, 16 - ty, 16 - fz, 16 - fy],
    }[side]

    def fold(a, b):
        if b - a >= 16:
            return [0, 16]
        shift = math.floor(a / 16) * 16
        a -= shift
        b -= shift
        return [a - (b - 16), 16] if b > 16 else [a, b]

    u1, u2 = fold(uv[0], uv[2])
    v1, v2 = fold(uv[1], uv[3])
    return [r3(u1), r3(v1), r3(u2), r3(v2)]


def cell_uv(i, f, t, side):
    u, v = i % 16, i // 16
    r = auto_uv(f, t, side)

    def at(cell, x):
        return round((cell + x / 16) * 10000) / 10000

    return [at(u, r[0]), at(v, r[1]), at(u, r[2]), at(v, r[3])]


elements = []


def box(lo, hi, faces, rot=None):
    f = [min(a, b) for a, b in zip(lo, hi)]
    t = [max(a, b) for a, b in zip(lo, hi)]
    e = {'from': [r3(v) for v in f], 'to': [r3(v) for v in t]}
    if rot:
        e['rotation'] = {'origin': [r3(v) for v in rot[2]], 'axis': rot[0], 'angle': rot[1]}
    e['faces'] = {}
    for side in SIDES:
        paint = faces.get(side)
        if paint:
            e['faces'][side] = {'uv': cell_uv(PALETTE.index(paint), f, t, side), 'texture': '#p'}
    if e['faces']:
        elements.append(e)


def rnd(axis, cx, cy, cz, r, length, paint, sides=8):
    reach, half = r * math.cos(math.pi / sides), r * math.sin(math.pi / sides)
    a = AXES.index(axis)
    u, v = (a + 1) % 3, (a + 2) % 3
    c = [cx, cy, cz]
    turns = [-45, -22.5, 0, 22.5] if sides == 16 else [0, 45]
    for turn in turns:
        for across, along in [(u, v), (v, u)]:
            lo, hi = c[:], c[:]
            lo[a] -= length / 2
            hi[a] += length / 2
            lo[across] -= reach
            hi[across] += reach
            lo[along] -= half
            hi[along] += half
            faces = {side: paint for side in SIDES}
            faces[FACING[along][0]] = None
            faces[FACING[along][1]] = None
            box(lo, hi, faces, [axis, turn, c] if turn else None)


for x in [3.4, 6.5, 9.5, 12.6]:
    rnd('y', x, 2.6, 8, 1.3, 0.5, STEEL)
    rnd('y', x, 3.05, 8, 0.75, 0.4, STEEL)
    rnd('y', x, 7.75, 8, 0.28, 9.5, CHROME)
    rnd('y', x, 9.9, 8, 0.8, 3.2, DARK_STEEL)
    for y in [8.7, 9.5, 10.3, 11.1]:
        rnd('y', x, y, 8, 0.92, 0.3, IRON)
    rnd('y', x, 11.7, 8, 0.75, 0.4, ALU)

pistons = json.load(open(os.path.join(ASSETS, 'models', 'item', 'pistons.json')))
model = {'textures': pistons['textures'], 'elements': elements, 'display': pistons['display']}
with open(os.path.join(ASSETS, 'models', 'item', 'valves.json'), 'w') as out:
    out.write('{\n  "textures": ' + json.dumps(model['textures']) + ',\n  "elements": [\n')
    out.write(',\n'.join('    ' + json.dumps(e, separators=(', ', ': ')) for e in elements))
    out.write('\n  ],\n  "display": ' + json.dumps(model['display']) + '\n}\n')
print(len(elements), 'elements')
