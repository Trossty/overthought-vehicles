"""Solid item models for what used to be flat sprites. Run: python3 tools/gen_paint_tar.py

- the four road paints: a round tin with a wire handle, its label and lid in the paint's colour
- tar: a glossy lump
- oil seep: a ragged puddle with gaps (the block) and, for the item, a drop of oil standing in it
- asphalt: the item shows a half-height slab of it, not a sliver
"""
import json, os, random
from PIL import Image

R = '/Users/egementopcu/IdeaProjects/car/src/main/resources/assets/car/'
TI = R + 'textures/item/'
TB = R + 'textures/block/'
MI = R + 'models/item/'
random.seed(7)

# The jerry can's display: these hold and sit the same way.
DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 1.5, 0], "scale": [1.15, 1.15, 1.15]},
    "ground": {"translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
    "fixed": {"scale": [1.2, 1.2, 1.2]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
}


def jit(c, n=4):
    d = random.randint(-n, n)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def octagon_slabs(across, y0, y1, uv, tex):
    """A round body: four slabs, two square and two at 45 degrees, each drawing its two outer faces."""
    c = 8.0
    half = across / 2
    side = across * 0.41421356 / 2
    out = []
    for along_x in (True, False):
        for angle in (0, 45):
            if along_x:
                fr, to, faces = [c - half, y0, c - side], [c + half, y1, c + side], ('east', 'west')
            else:
                fr, to, faces = [c - side, y0, c - half], [c + side, y1, c + half], ('north', 'south')
            out.append({'from': fr, 'to': to,
                        'rotation': {'angle': angle, 'axis': 'y', 'origin': [8, 8, 8]},
                        'faces': {f: {'texture': tex, 'uv': uv} for f in faces}})
    return out


def box(fr, to, tex, uv=None, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    return {'from': fr, 'to': to, 'faces': {f: ({'texture': tex, 'uv': uv} if uv else {'texture': tex})
                                            for f in faces}}


def write_model(name, textures, elements):
    json.dump({'textures': textures, 'elements': elements, 'display': DISPLAY},
              open(MI + name + '.json', 'w'), indent=1)


def octagon(x, y, apothem):
    dx, dy = abs(x + 0.5 - 8), abs(y + 0.5 - 8)
    return dx <= apothem and dy <= apothem and dx + dy <= apothem * 2 ** 0.5


# ---- road paint tins ----
PAINTS = {'white': (236, 236, 230), 'yellow': (240, 192, 36), 'red': (204, 50, 40), 'blue': (44, 98, 200)}
TIN = (176, 182, 189)

for name, colour in PAINTS.items():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    # side, 0-7 across and 0-8 down: tin at top and bottom, the label between, paint run down from the lip
    for y in range(9):
        for x in range(8):
            k = 1.06 if 3 <= x <= 4 else (0.9 if x in (0, 7) else 1.0)
            if y in (0, 8):
                c = shade(TIN, 0.8 * k)
            elif 2 <= y <= 6:
                c = shade(colour + (255,), k * (0.85 if y in (2, 6) else 1.0))
            else:
                c = shade(TIN, k)
            im.putpixel((x, y), jit(c, 3))
    for x, run in ((1, 2), (5, 3)):
        for y in range(1, 1 + run):
            im.putpixel((x, y), shade(colour + (255,), 0.95))
    # lid, 8-15 across and 0-7 down: tin round a pool of paint
    for y in range(8):
        for x in range(8):
            gx, gy = x * 2, y * 2
            if not octagon(gx, gy, 7.2):
                continue
            r = ((gx + 1 - 8) ** 2 + (gy + 1 - 8) ** 2) ** 0.5
            c = shade(TIN, 0.75) if r > 5.6 else (shade(TIN, 0.95) if r > 4.4 else colour + (255,))
            im.putpixel((8 + x, y), jit(c, 3))
    # handle wire, 8-9 / 8-9
    for x in range(8, 10):
        for y in range(8, 10):
            im.putpixel((x, y), (70, 72, 76, 255))
    for x in range(10, 12):
        for y in range(8, 10):
            im.putpixel((x, y), (40, 40, 42, 255))
    im.save(TI + name + '_road_paint.png')

    els = octagon_slabs(10, 0, 9, [0, 0, 8, 9], '#tin')
    els.append(box([3, 9, 3], [13, 9, 13], '#tin', [8, 0, 16, 8], ('up',)))
    els.append(box([3, 0.01, 3], [13, 0.01, 13], '#tin', [8, 0, 16, 8], ('down',)))
    # the wire handle, up from two ears and over
    els.append(box([2.6, 6.5, 7.6], [3.1, 11.5, 8.4], '#tin', [8, 8, 10, 10]))
    els.append(box([12.9, 6.5, 7.6], [13.4, 11.5, 8.4], '#tin', [8, 8, 10, 10]))
    els.append(box([2.6, 11.5, 7.6], [13.4, 12, 8.4], '#tin', [8, 8, 10, 10]))
    els.append(box([6, 11.3, 7.3], [10, 12.3, 8.7], '#tin', [10, 8, 12, 10]))
    write_model(name + '_road_paint', {'particle': f'car:item/{name}_road_paint', 'tin': f'car:item/{name}_road_paint'},
                els)

# ---- tar: a lump with a glossy skin ----
im = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        v = random.randint(20, 32)
        im.putpixel((x, y), (v, v, v + 3, 255))
for x, y in ((3, 3), (4, 3), (3, 4), (10, 9), (11, 9), (12, 2), (6, 12)):
    im.putpixel((x, y), (96, 98, 108, 255))
for x, y in ((4, 4), (11, 10), (7, 12)):
    im.putpixel((x, y), (60, 62, 70, 255))
im.save(TI + 'tar.png')
els = [box([3.5, 0, 4], [12.5, 3, 12], '#tar'),
       box([4.5, 3, 4.5], [11, 5, 11.5], '#tar'),
       box([6, 5, 6], [10, 6.5, 10], '#tar'),
       box([7.2, 6.5, 7.2], [8.8, 7.5, 8.8], '#tar'),
       # a run spreading out at the foot
       box([11.5, 0, 6], [14, 1, 9], '#tar'),
       box([2, 0, 8], [4, 0.8, 11], '#tar')]
write_model('tar', {'particle': 'car:item/tar', 'tar': 'car:item/tar'}, els)

# ---- oil seep: a ragged puddle, the ground showing through the gaps ----
im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
blobs = [(5, 6, 4.2), (10, 9, 4.6), (7, 11, 3.2), (11, 4, 2.6), (3, 11, 2.0)]
for y in range(16):
    for x in range(16):
        inside = any(((x + 0.5 - bx) ** 2 + (y + 0.5 - by) ** 2) ** 0.5 < r for bx, by, r in blobs)
        if not inside:
            continue
        v = random.randint(12, 22)
        im.putpixel((x, y), (v, v, v + 4, 255))
# the sheen oil has: a few bright pixels in blue, purple and green
for (x, y), c in zip(((5, 5), (6, 5), (10, 8), (11, 8), (9, 10), (7, 11)),
                     ((70, 90, 150), (110, 70, 140), (70, 130, 110), (90, 80, 150), (60, 110, 120), (120, 90, 140))):
    im.putpixel((x, y), c + (255,))
im.save(TB + 'oil_seep.png')
json.dump({'parent': 'minecraft:block/block', 'ambientocclusion': False,
           'textures': {'particle': 'car:block/oil_seep', 'oil': 'car:block/oil_seep'},
           'elements': [box([0, 0, 0], [16, 0.5, 16], '#oil', None, ('up', 'down'))]},
          open(R + 'models/block/oil_seep.json', 'w'), indent=1)

im = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        v = random.randint(16, 26)
        im.putpixel((x, y), (v, v, v + 4, 255))
for x, y in ((2, 2), (3, 2), (2, 3)):
    im.putpixel((x, y), (120, 125, 150, 255))
im.save(TB + 'oil_drop.png')  # block atlas: a model may only use one
els = [box([0, 0, 0], [16, 0.5, 16], '#oil', None, ('up', 'down')),
       box([6, 0.5, 6], [10, 3.5, 10], '#drop'),
       box([6.6, 3.5, 6.6], [9.4, 5, 9.4], '#drop'),
       box([7.3, 5, 7.3], [8.7, 6.3, 8.7], '#drop'),
       box([7.7, 6.3, 7.7], [8.3, 7, 8.3], '#drop')]
write_model('oil_seep', {'particle': 'car:block/oil_seep', 'oil': 'car:block/oil_seep', 'drop': 'car:block/oil_drop'},
            els)

# ---- the item definitions that point at the new models ----
for name in [p + '_road_paint' for p in PAINTS] + ['tar', 'oil_seep']:
    path = R + 'items/' + name + '.json'
    d = json.load(open(path))
    def repoint(o):
        if isinstance(o, dict):
            if o.get('type') == 'minecraft:model':
                o['model'] = 'car:item/' + name
            for v in o.values():
                repoint(v)
        elif isinstance(o, list):
            for v in o:
                repoint(v)
    repoint(d)
    json.dump(d, open(path, 'w'), indent=2)
d = json.load(open(R + 'items/asphalt.json'))
d['model']['model'] = 'car:block/asphalt_height8'
json.dump(d, open(R + 'items/asphalt.json', 'w'), indent=2)
print('written')
