"""Fuel drum model and textures. Run: python3 tools/gen_drum.py

A steel drum, a full block tall and fourteen pixels across. The body is an
octagon: four slabs, two square to the block and two turned 45 degrees, each
drawing only its two outer faces. The lid sits half a pixel down inside the
rim, an octagon cut out of a square with transparent corners. Two rolled
hoops and the chimes at top and bottom are pressed into the body texture.

The body is drawn pale grey and tinted (tintindex 0) the colour of whatever
liquid is in the drum; the lid, the filler cap and the hazard label are not.
"""
import json, os, random
from model_uv import fold_uvs
from PIL import Image
R = '/Users/egementopcu/IdeaProjects/car/src/main/resources/assets/car/'
T = R + 'textures/block/'; M = R + 'models/block/'
random.seed(23)

# An empty drum's paint, bare steel. ItemLooks.EMPTY_DRUM says the same.
EMPTY_DRUM = 0x9AA1A8


def tex(name, fn):
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = list(fn(x, y))
            im.putpixel((x, y), tuple(c) + ((255,) if len(c) == 3 else ()))
    im.save(T + name + '.png')


def jit(c, n=4):
    d = random.randint(-n, n)
    return [max(0, min(255, v + d)) for v in c]


def grey(v):
    return [v, v, v]


# ---- the body: pale, so the tint gives the colour ----
# Texture rows run top down: row v is model height 16 - v.
HOOPS = {4: 0.70, 5: 0.80, 10: 0.70, 11: 0.80}   # rolled hoops, a shadow under each
HIGHLIGHT = {3: 1.06, 9: 1.06}                    # and light catching the top of each


def body(x, y):
    if y in (0, 15):
        return jit(grey(150), 2)                  # the chimes
    if y in (1, 14):
        return jit(grey(200), 2)
    k = HOOPS.get(y, HIGHLIGHT.get(y, 1.0))
    # Light down the middle of each face, a little shadow at its edges.
    if 7 <= x <= 8:
        k *= 1.04
    elif x <= 5 or x >= 10:
        k *= 0.93
    return jit(grey(min(255, int(232 * k))), 3)


tex('drum_body', body)


def octagon(x, y):
    """Whether a pixel is inside the drum's octagon (apothem 7, about the middle)."""
    dx, dy = abs(x + 0.5 - 8), abs(y + 0.5 - 8)
    return dx <= 7 and dy <= 7 and dx + dy <= 7 * 2 ** 0.5


def lid(x, y):
    if not octagon(x, y):
        return (0, 0, 0, 0)
    dx, dy = abs(x + 0.5 - 8), abs(y + 0.5 - 8)
    r = (dx * dx + dy * dy) ** 0.5
    if not (octagon(x - 1, y) and octagon(x + 1, y) and octagon(x, y - 1) and octagon(x, y + 1)):
        return jit((96, 100, 106), 2) + [255]      # the rim's inner edge
    if 4.2 < r < 5.2:
        return jit((112, 117, 124), 2) + [255]     # a pressed ring
    return jit((150, 155, 162), 3) + [255]


tex('drum_lid', lid)


def label(x, y):
    # Yellow, a black border, a black diamond with a red flame in it.
    if x in (0, 15) or y in (0, 15):
        return jit((28, 28, 30), 1)
    cx, cy = 7.5, 7.5
    d = abs(x - cx) + abs(y - cy)
    flame = (abs(x - cx) <= 1.5 and 6 <= y <= 10) or (abs(x - cx) <= 0.5 and y == 5)
    if d <= 5.0 and flame:
        return [214, 64, 28]
    if d <= 6.0:
        return jit((28, 28, 30), 1)
    return jit((236, 190, 40), 3)


tex('drum_label', label)
tex('drum_cap', lambda x, y: jit((58, 60, 64), 2) if (x + y) % 3 else jit((92, 96, 102), 2))

# A texture the first drum had and this one does not.
if os.path.exists(T + 'drum_rib.png'):
    os.remove(T + 'drum_rib.png')

# ---- the model ----
NEAR, FAR = 5.10051, 10.89949        # one side of a 14-across octagon: 14 * tan(22.5)


def side(face):
    return {face: {'texture': '#body', 'tintindex': 0}}


def slab(along_x, angle):
    if along_x:
        fr, to, faces = [1, 0, NEAR], [15, 16, FAR], {**side('east'), **side('west')}
    else:
        fr, to, faces = [NEAR, 0, 1], [FAR, 16, 15], {**side('north'), **side('south')}
    return {'from': fr, 'to': to,
            'rotation': {'angle': angle, 'axis': 'y', 'origin': [8, 8, 8]},
            'faces': faces}


els = [slab(True, 0), slab(True, 45), slab(False, 0), slab(False, 45)]
# The lid half a pixel down inside the rim, and the bottom flush with the floor.
els.append({'from': [1, 15.5, 1], 'to': [15, 15.5, 15],
            'faces': {'up': {'texture': '#lid'}}})
els.append({'from': [1, 0.01, 1], 'to': [15, 0.01, 15],
            'faces': {'down': {'texture': '#lid'}}})
# The hazard label on the front, between the hoops.
els.append({'from': [5.5, 6, 0.96], 'to': [10.5, 10.5, 0.96],
            'faces': {'north': {'texture': '#label', 'uv': [0, 0, 16, 16]}}})
# The filler cap at the front of the lid and a vent behind it.
CAP_FACES = ('north', 'south', 'east', 'west', 'up')
els.append({'from': [9.5, 15.5, 3], 'to': [11.5, 16.25, 5],
            'faces': {k: {'texture': '#cap'} for k in CAP_FACES}})
els.append({'from': [4.5, 15.5, 10.5], 'to': [5.75, 16, 11.75],
            'faces': {k: {'texture': '#cap'} for k in CAP_FACES}})

json.dump({'parent': 'minecraft:block/block',
           'textures': {'particle': 'car:block/drum_body', 'body': 'car:block/drum_body',
                        'lid': 'car:block/drum_lid', 'label': 'car:block/drum_label',
                        'cap': 'car:block/drum_cap'},
           'elements': fold_uvs(els)},
          open(M + 'fuel_drum.json', 'w'), indent=1)

json.dump({'variants': {f'facing={f}': ({'model': 'car:block/fuel_drum', 'y': y} if y else
                                         {'model': 'car:block/fuel_drum'})
                        for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}},
          open(R + 'blockstates/fuel_drum.json', 'w'), indent=2)
# The paint takes the colour of what is inside: ItemLooks tints face 0.
json.dump({'parent': 'car:block/fuel_drum'}, open(R + 'models/item/fuel_drum.json', 'w'), indent=2)
print('drum written:', len(els), 'elements')
