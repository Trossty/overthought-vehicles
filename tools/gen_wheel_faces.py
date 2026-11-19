"""Paints the faces of the wheel texture the vehicle renderer turns side-on.

The wheels in DefinedVehicleModel are rings of identical slabs, each slab's
side face a strip running through the axle. Whatever is painted along the
strip by distance from its middle comes out as a ring round the wheel, so the
sidewall and the rim are drawn here as profiles: radius -> colour.

Run from the project root: python3 tools/gen_wheel_faces.py
"""
from PIL import Image

DIR = 'src/main/resources/assets/car/textures/entity/wheel/'
SCALE = 2  # the 128px texture over a 64-texel model

# Box: texOffs (u, v), size (dx, dy, dz). Its two side faces (normal along the
# axle) are dz wide and dy tall, at u and u+dz+dx, starting at v+dz.
HUB = (0, 34, 5.2, 8.8, 0.87)
WALL = (40, 22, 4.5, 13.8, 1.36)
TREAD = (40, 0, 3.9, 15.08, 1.48)
# For the rims (DefinedVehicleModel.addRim): the hub painted open behind
# spokes, and patches of one colour for the spokes, the nuts and the holes.
HUB_OPEN = (0, 0, 5.2, 8.8, 0.87)
RIM_PATCH, NUT_PATCH, DARK_PATCH, PATCH = (14, 0), (23, 0), (31, 0), 8


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3]) + (255,)


def paint(px, box, profile):
    u, v, dx, dy, dz = box
    centre = v + dz + dy / 2.0
    for face_u in (u, u + dz + dx):
        x0, x1 = int(face_u * SCALE), int(round((face_u + dz) * SCALE))
        y0, y1 = int((v + dz) * SCALE), int(round((v + dz + dy) * SCALE))
        for y in range(y0, y1):
            r = abs((y + 0.5) / SCALE - centre)
            colour = profile(r)
            if colour is None:
                continue
            for x in range(x0, max(x1, x0 + 1)):
                px[x, y] = colour


def wheel(name, rim, rubber=(32, 32, 35), band=None, dish=0.72):
    im = Image.open(DIR + name + '.png').convert('RGBA')
    px = im.load()
    rubber = rubber + (255,)
    rim = rim + (255,)
    band = (band or shade(rubber, 1.55)[:3]) + (255,)

    def hub(r):
        if r < 0.45:
            return shade(rim, 0.55)          # the axle nut in the middle of the cap
        if r < 1.15:
            return shade(rim, 1.12)          # centre cap
        if r < 1.55:
            return shade(rim, 0.62)          # the ring the wheel nuts sit on
        if r < 3.25:
            return mix(shade(rim, dish), rim, (r - 1.55) / 1.7)  # the face, deeper towards the middle
        if r < 3.7:
            return shade(rim, 0.5)           # a dark groove before the lip
        return shade(rim, 1.18)              # the polished lip catching the light

    def wall(r):
        if r < 4.4:
            return None                      # under the hub
        if r < 4.85:
            return shade(rubber, 0.6)        # the bead, in the rim's shadow
        if 5.45 <= r < 5.85:
            return band                      # the raised band where the lettering runs
        if r >= 6.4:
            return shade(rubber, 1.25)       # the shoulder rounding over
        return rubber

    def tread_side(r):
        if r < 6.9:
            return None
        return shade(rubber, 1.12 if r < 7.25 else 0.8)

    def hub_open(r):
        if r < 1.55:
            return hub(r)                    # the cap and the nut ring, as on a dish
        if r < 2.1 or 3.45 <= r < 3.7:
            return (30, 30, 33, 255)         # the dark behind the spokes
        if r < 3.7:
            return (78, 79, 82, 255)         # the brake disc seen through them
        return hub(r)                        # the lip

    paint(px, HUB, hub)
    paint(px, WALL, wall)
    paint(px, TREAD, tread_side)
    # A hub for spoked rims: the plain hub's sides copied, its faces painted open.
    u, v, dx, dy, dz = HUB
    w, h = int(round(2 * (dx + dz) * SCALE)) + 1, int(round((dz + dy) * SCALE)) + 1
    im.paste(im.crop((u * SCALE, v * SCALE, u * SCALE + w, v * SCALE + h)), (HUB_OPEN[0] * SCALE, HUB_OPEN[1] * SCALE))
    paint(px, HUB_OPEN, hub_open)
    # The patches the rim's pieces take their colour from.
    for (pu, pv), colour in ((RIM_PATCH, shade(rim, 1.06)), (NUT_PATCH, shade(rim, 1.2)), (DARK_PATCH, (24, 24, 27, 255))):
        for x in range(pu * SCALE, (pu + PATCH) * SCALE):
            for y in range(pv * SCALE, (pv + PATCH) * SCALE):
                px[x, y] = colour
    im.save(DIR + name + '.png')


wheel('road', (169, 172, 176))
wheel('dirt', (150, 152, 156), band=(58, 52, 44))
wheel('snow', (106, 110, 117), band=(70, 74, 82))
wheel('sport', (198, 201, 205), band=(196, 40, 36), dish=0.6)
