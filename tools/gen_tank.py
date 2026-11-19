"""Fuel tank textures: the glass of its sides and the tread plate of its lid.
The tank's model is drawn in tools/blocks/fuel_tank.js. Run: python3 tools/gen_tank.py
"""
import random
from PIL import Image
T = '/Users/egementopcu/IdeaProjects/car/src/main/resources/assets/overthought_vehicles/textures/block/'
random.seed(7)
# glass: faint blue tint, two highlight streaks, a gauge strip of ticks near one edge
g = Image.new('RGBA', (16, 16), (170, 205, 220, 46))
for k in range(16):
    for d in (0, 1):
        x = k; y = 15 - k - 6 + d
        if 0 <= y < 16: g.putpixel((x, y), (235, 245, 255, 110))
    y = 15 - k + 4
    if 0 <= y < 16: g.putpixel((k, y), (235, 245, 255, 80))
for y in range(1, 16, 3):
    g.putpixel((13, y), (40, 44, 50, 200)); g.putpixel((14, y), (40, 44, 50, 200))
g.save(T + 'tank_glass.png')
# lid: dark steel with a tread pattern
l = Image.new('RGBA', (16, 16))
for x in range(16):
    for y in range(16):
        n = random.randint(-6, 6)
        l.putpixel((x, y), (92 + n, 97 + n, 103 + n, 255))
for x in range(16):
    for y in range(16):
        if (x + y) % 4 == 0 and x % 2 == 0: l.putpixel((x, y), (130, 136, 142, 255))
l.save(T + 'tank_lid.png')
