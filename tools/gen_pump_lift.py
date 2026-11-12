"""Fuel pump and vehicle lift models and textures. Run: python3 tools/gen_pump_lift.py"""
import json, random
from PIL import Image
R='/Users/egementopcu/IdeaProjects/car/src/main/resources/assets/car/'
T=R+'textures/block/'; M=R+'models/block/'
random.seed(11)

def tex(name, fn):
    im=Image.new('RGBA',(16,16))
    for y in range(16):
        for x in range(16):
            c=fn(x,y)
            im.putpixel((x,y),tuple(c)+((255,) if len(c)==3 else ()))
    im.save(T+name+'.png')
def jit(c,n=4):
    d=random.randint(-n,n); return [max(0,min(255,v+d)) for v in c]

WHITE=(226,229,231); RED=(190,34,30); DARK=(34,36,40); STEEL=(70,75,82); YEL=(232,182,32)
# pump body: white enamel with a seam every 8 px and shaded edges
tex('pump_body', lambda x,y: jit([v-30 for v in WHITE],2) if x in (0,15) or y in (0,15) else (jit([v-14 for v in WHITE],2) if x==8 else jit(WHITE,3)))
tex('pump_red', lambda x,y: jit([int(v*0.75) for v in RED],2) if y in (0,15) else jit(RED,3))
# lower door: louvres at the top, a recessed panel and a fuel sticker
def door(x,y):
    if x in (0,15) or y in (0,15): return jit([v-40 for v in WHITE],2)
    if x in (1,14) or y in (1,14): return jit([v-18 for v in WHITE],2)
    if 3<=y<=6 and 3<=x<=12: return [90,94,100] if y%2==1 else [200,204,208]
    if 9<=y<=11 and 4<=x<=6: return [60,170,70]
    if 9<=y<=11 and 9<=x<=11: return [230,160,30]
    if y==12 and 3<=x<=12: return [170,172,176]
    return jit(WHITE,2)
tex('pump_door', door)
# screen: bezel, two rows of green digits, a fuel bar
DIG={'8':['111','101','111','101','111'],'0':['111','101','101','101','111'],'4':['101','101','111','001','001'],'2':['111','001','111','100','111'],'5':['111','100','111','001','111'],'1':['010','110','010','010','111']}
def screen():
    im=Image.new('RGBA',(16,16),DARK+(255,))
    for y in range(1,15):
        for x in range(1,15): im.putpixel((x,y),(16,26,22,255))
    def num(s,ox,oy,col):
        for i,ch in enumerate(s):
            for r,row in enumerate(DIG[ch]):
                for c,b in enumerate(row):
                    if b=='1': im.putpixel((ox+i*4+c,oy+r),col)
    num('245',2,2,(90,240,120,255))
    for x in range(14,15): pass
    for x in range(2,14):
        im.putpixel((x,9),(40,60,50,255))
        im.putpixel((x,10),(230,170,40,255) if x<10 else (60,70,60,255))
    num('105',2,11,(90,200,240,255)) if False else None
    for x in (3,6,9,12):
        for y in (12,13): im.putpixel((x,y),(150,155,160,255))
    im.save(T+'pump_screen.png')
screen()
tex('pump_sign', lambda x,y: [250,250,250] if 5<=y<=10 and 2<=x<=13 and not ((x in (4,8,11)) and 6<=y<=9) else jit(RED,3))
tex('pump_plinth', lambda x,y: jit(YEL,4) if ((x//2+y//2)%2==0 and (y<2 or y>13)) else jit((176,176,170),4))
tex('pump_rubber', lambda x,y: jit((24,24,26),2))
tex('pump_chrome', lambda x,y: jit((225,228,230),2) if x in (4,5) else jit((170,175,180),3))
tex('pump_bollard', lambda x,y: jit(YEL,3) if (y//3)%2==0 else jit(DARK,2))

# lift: housing, top with flush runways, the pit when they are up
def housing(x,y):
    if y<2: return [230,180,30] if ((x+y)//2)%2==0 else [30,30,32]
    if x in (0,15) or y==15: return jit((48,52,58),2)
    if (x,y) in ((2,4),(13,4),(2,13),(13,13)): return [150,155,160]
    return jit(STEEL,3)
tex('lift_housing', housing)
def plate(x,y):  # diamond plate
    return [150,155,160] if (x+2*y)%5==0 or (x-2*y)%5==0 else jit((105,110,116),3)
def top(x,y):
    if x in (0,15) or y in (0,15): return [230,180,30] if ((x+y)//2)%2==0 else [30,30,32]
    if 2<=x<=5 or 10<=x<=13: return plate(x,y)
    return jit((40,42,46),2)
tex('lift_top', top)
def pit(x,y):
    if x in (0,15) or y in (0,15): return [230,180,30] if ((x+y)//2)%2==0 else [30,30,32]
    if x in (1,14) or y in (1,14): return jit((80,84,90),2)
    return jit((28,30,33),2)
tex('lift_pit', pit)
def runway(x,y):
    if y in (0,15) or x in (0,15): return [230,180,30] if ((x+y)//2)%2==0 else [30,30,32]
    return plate(x,y)
tex('lift_runway', runway)

def el(fr,to,t,rot=None,faces=('north','south','east','west','up','down'),uv=None,tex_over=None):
    f={k:{'texture':t} for k in faces}
    for k,v in (uv or {}).items(): f[k]['uv']=v
    for k,v in (tex_over or {}).items(): f[k]['texture']=v
    e={'from':fr,'to':to,'faces':f}
    if rot: e['rotation']=rot
    return e
def save(name,textures,els):
    json.dump({'parent':'minecraft:block/block','textures':textures,'elements':els},open(M+name+'.json','w'),indent=1)

PT={'particle':'car:block/pump_red','body':'car:block/pump_body','red':'car:block/pump_red','door':'car:block/pump_door',
    'screen':'car:block/pump_screen','sign':'car:block/pump_sign','plinth':'car:block/pump_plinth','rubber':'car:block/pump_rubber',
    'chrome':'car:block/pump_chrome','bollard':'car:block/pump_bollard'}
F=[0,0,16,16]
# ---- pump (front = north) ----
# A tall, slim red cabinet in the style of the classic roadside pump: a white
# bezel up the front, a sight window low down where the petrol shows (drawn
# by FuelPumpRenderer), an LCD high up that reads out the fuel, a dark cap,
# and the nozzle hung on the right-hand side with its hose dropping to the
# floor and back into the cabinet.
tex('pump_lcd', lambda x,y: [24,30,28] if x in (0,15) or y in (0,15) else jit((10,18,14),1))
tex('pump_label', lambda x,y: jit((236,236,232),2) if not (6<=y<=9 and 2<=x<=13 and x%3!=1) else [200,40,34])
tex('pump_window', lambda x,y: jit((18,20,22),1))
PT={'particle':'car:block/pump_red','red':'car:block/pump_red','white':'car:block/pump_body','dark':'car:block/shop_steel',
    'hose':'car:block/pump_rubber','chrome':'car:block/pump_chrome','lcd':'car:block/pump_lcd','label':'car:block/pump_label',
    'window':'car:block/pump_window'}
def mx(e):
    # Mirrored across the cabinet: the nozzle side is west in the model,
    # which is the right-hand side seen from the front.
    e['from'][0],e['to'][0]=16-e['to'][0],16-e['from'][0]
    return e
def bezel(y0,y1):
    return [el([3,y0,3.5],[4,y1,4],'#white'), el([12,y0,3.5],[13,y1,4],'#white'),
            el([2.6,y0,4],[3,y1,5.5],'#white'), el([13,y0,4],[13.4,y1,5.5],'#white')]
def back_bezel(y0,y1):
    # the same white edging down the back corners: the pump is seen from both sides
    return [el([3,y0,12],[4,y1,12.5],'#white'), el([12,y0,12],[13,y1,12.5],'#white')]
# The glass: clear, with a light rim and short streaks of shine in two
# corners, like a pane of glass, stretched over the whole opening.
def glass(x,y):
    if x in (0,15) or y in (0,15): return (236,242,246,230)
    d=x+y
    if (d in (4,5,6) and x<=5 and y<=5) or (d==8 and 2<=x<=6 and y<=6): return (244,248,252,190)
    if (d in (23,24) and x>=11 and y>=11): return (244,248,252,190)
    return (210,230,240,18)
tex('pump_glass', glass)
PT['glass']='car:block/pump_glass'
NF=('north',)
lower=[
 el([2,0,3],[14,1.5,13],'#dark'),
 el([3,1.5,4],[13,16,12],'#red',faces=('south','east','west')),
 # the front, round a square opening low down
 el([3,1.5,4],[5,16,4.5],'#red',faces=NF), el([11,1.5,4],[13,16,4.5],'#red',faces=NF),
 el([5,1.5,4],[11,2.5,4.5],'#red',faces=NF), el([5,9,4],[11,16,4.5],'#red',faces=NF),
 el([4,1.5,3.5],[12,2.5,4],'#white'),
]+bezel(1.5,16)+[
 # the opening: a box let into the cabinet, white inside
 el([5,2.5,9],[11,9,9.5],'#white',faces=NF),
 el([4.5,2.5,4],[5,9,9],'#white',faces=('east',)), el([11,2.5,4],[11.5,9,9],'#white',faces=('west',)),
 el([5,2,4],[11,2.5,9],'#white',faces=('up',)), el([5,9,4],[11,9.5,9],'#white',faces=('down',)),
 # a white frame round it and the glass across it, set back a little
 el([4.5,2,3.4],[5,9.5,4],'#white'), el([11,2,3.4],[11.5,9.5,4],'#white'),
 el([5,2,3.4],[11,2.5,4],'#white'), el([5,9,3.4],[11,9.5,4],'#white'),
 el([5,2.5,3.9],[11,9,3.9],'#glass',faces=('north','south'),uv={'north':F,'south':F}),
 # the hose: down the side and back in at the foot
 mx(el([13.4,2.5,9],[14.4,16,10],'#hose')), mx(el([13,1.5,9],[14.4,2.5,10],'#hose')),
]+back_bezel(1.5,16)+[
 # the service door on the back: a raised panel, a louvre at its head, a handle
 el([5,3,12],[11,14.5,12.3],'#red'),
 el([5,3,12.3],[11,3.4,12.4],'#dark'), el([5,14.1,12.3],[11,14.5,12.4],'#dark'),
 el([5,3.4,12.3],[5.4,14.1,12.4],'#dark'), el([10.6,3.4,12.3],[11,14.1,12.4],'#dark'),
]+[el([6,y,12.3],[10,y+0.5,12.45],'#dark') for y in (11.2,12.2,13.2)]+[
 el([9.2,6.5,12.3],[9.8,9,12.7],'#chrome'),
 # the supply pipe coming up out of the island into the back
 el([6.8,1.5,12.3],[8.2,3,13.4],'#dark'),
]
save('fuel_pump_lower',PT,lower)
save('fuel_pump_lower_empty',PT,lower)
upper=[
 el([3,0,4],[13,11,12],'#red',faces=('north','south','east','west')),
]+bezel(0,11)+[
 el([4.5,4,3.5],[11.5,10,4],'#lcd',uv={'north':F}),
 # the cap: a darker band, a white name plate, a roof
 el([2.5,11,3.5],[13.5,13,12.5],'#red',tex_over={'up':'#dark'}),
 el([4,11.4,3.3],[12,12.6,3.5],'#label',uv={'north':F}),
 el([3,13,4],[13,14,12],'#dark'),
 # holster bracket and the hose coming up to it
 mx(el([13,5,6],[14,7,8.5],'#dark')),
 mx(el([13.4,0,9],[14.4,5,10],'#hose')),
]+back_bezel(0,11)+[
 # the back has its own read-out and name plate, for the far lane
 el([4.5,4,12],[11.5,10,12.5],'#lcd',uv={'south':F}),
 el([4,11.4,12.5],[12,12.6,12.7],'#label',uv={'south':F}),
]
nozzle=[
 mx(el([14,1.5,6.4],[15.2,6,7.8],'#hose')),          # grip, hanging
 mx(el([13.9,6,6],[15.3,7.6,8.2],'#red')),           # head
 mx(el([14.25,3.5,5.1],[14.95,7,6],'#chrome')),      # spout, pointing down
 mx(el([14,1.8,7.8],[15.2,2.3,8.8],'#chrome')),      # trigger guard
]
save('fuel_pump_upper',PT,upper+nozzle)
save('fuel_pump_upper_empty',PT,upper)

# ---- lift ----
# A scissor lift with a platform two blocks wide and three long, laid along
# z (the blockstate turns it): the block is the middle of it, under the car's
# middle. The base block is the housing let into the floor.
LT={'particle':'car:block/lift_housing','housing':'car:block/lift_housing','pit':'car:block/lift_pit',
    'plate':'car:block/lift_plate','hazard':'car:block/shop_hazard','red':'car:block/shop_red',
    'chrome':'car:block/shop_chrome','steel':'car:block/shop_steel','bottom':'car:block/vehicle_lift_bottom'}
tex('lift_plate', lambda x,y: plate(x,y))
def base():
    return el([0,0,0],[16,16,16],'#housing',uv={k:F for k in ('north','south','east','west','up','down')},
              tex_over={'up':'#pit','down':'#bottom'})
X0,X1,Z0,Z1=-8,24,-16,32
def platform(bottom,top):
    els=[]
    for x in (X0,8):
        for z in (Z0,0,16):
            els.append(el([x,top-0.5,z],[x+16,top,z+16],'#plate',uv={'up':F,'down':F},faces=('up','down')))
    # a deep skirt all round, hazard paint on the ends, red on the sides
    els+= [el([X0,bottom,Z0],[X1,top-0.5,Z0+1],'#hazard'), el([X0,bottom,Z1-1],[X1,top-0.5,Z1],'#hazard'),
           el([X0,bottom,Z0+1],[X0+1,top-0.5,Z1-1],'#red'), el([X1-1,bottom,Z0+1],[X1,top-0.5,Z1-1],'#red')]
    return els
def floor_frame():
    # the rails the arms stand on, lying on the floor round the housing
    return [el([X0+1,16,Z0+2],[X0+5,17,Z1-2],'#steel'), el([X1-5,16,Z0+2],[X1-1,17,Z1-2],'#steel'),
            el([X0+5,16,Z0+2],[X1-5,17,Z0+4],'#steel'), el([X0+5,16,Z1-4],[X1-5,17,Z1-2],'#steel')]
save('vehicle_lift',LT,[base()]+platform(15.5,16.5))
ARMS={2:7.8,3:18.3,4:28.7}   # all at 22.5 degrees, as long as the height needs
for level in range(1,5):
    deck=12+4*level
    top=deck+4
    els=[base()]
    if level in ARMS:
        import math
        length=(top-2-17)/math.sin(math.radians(22.5))
        mid=17+(top-2-17)/2
        els+=floor_frame()
        for x in (X0+1.5,X0+3.5,X1-5.5,X1-3.5):
            sign=1 if x in (X0+1.5,X1-3.5) else -1
            els.append(el([x,mid-0.75,8-length/2],[x+1.5,mid+0.75,8+length/2],'#steel' if sign>0 else '#red',
                          rot={'origin':[x+0.75,mid,8],'axis':'x','angle':22.5*sign}))
        els.append(el([X0+1,mid-0.75,7.25],[X1-1,mid+0.75,8.75],'#chrome'))
        # the hydraulic ram, slanting from the floor frame up to the cross pin
        els.append(el([6.5,17,6.5],[9.5,mid,9.5],'#chrome'))
        els.append(el([6,16,6],[10,18,10],'#red'))
    # cross members under the platform
    els+= [el([X0+1,top-2,3],[X1-1,top-0.5,5],'#red'), el([X0+1,top-2,11],[X1-1,top-0.5,13],'#red')]
    els+= platform(top-2,top)
    save('vehicle_lift_%d'%level,LT,els)
print('ok')
