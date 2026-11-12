import json, os, random
from PIL import Image
R='/Users/egementopcu/IdeaProjects/car/src/main/resources/assets/car/'
T=R+'textures/block/'; M=R+'models/block/'
random.seed(7)
def steel(base=(120,126,132)):
    im=Image.new('RGBA',(16,16))
    for x in range(16):
        for y in range(16):
            n=random.randint(-6,6)
            im.putpixel((x,y),(base[0]+n,base[1]+n,base[2]+n,255))
    return im
# frame: brushed steel bar with dark edges and rivets
f=steel((128,134,140))
for i in range(16):
    for (x,y) in [(0,i),(15,i),(i,0),(i,15)]:
        f.putpixel((x,y),(70,74,80,255))
    for (x,y) in [(1,i),(i,1)]:
        f.putpixel((x,y),(160,166,172,255))
for (x,y) in [(4,4),(11,4),(4,11),(11,11)]:
    f.putpixel((x,y),(190,194,198,255)); f.putpixel((x+1,y+1),(60,64,70,255))
f.save(T+'tank_frame.png')
# glass: faint blue tint, two highlight streaks, a gauge strip of ticks near one edge
g=Image.new('RGBA',(16,16),(170,205,220,46))
for k in range(16):
    for d in (0,1):
        x=k; y=15-k-6+d
        if 0<=y<16: g.putpixel((x,y),(235,245,255,110))
    y=15-k+4
    if 0<=y<16: g.putpixel((k,y),(235,245,255,80))
for y in range(1,16,3): 
    g.putpixel((13,y),(40,44,50,200)); g.putpixel((14,y),(40,44,50,200))
g.save(T+'tank_glass.png')
# lid: dark steel with tread pattern
l=steel((92,97,103))
for x in range(16):
    for y in range(16):
        if (x+y)%4==0 and x%2==0: l.putpixel((x,y),(130,136,142,255))
l.save(T+'tank_lid.png')
# hatch: brass-ish cap
h=steel((168,140,70))
for i in range(16):
    for (x,y) in [(0,i),(15,i),(i,0),(i,15)]: h.putpixel((x,y),(110,88,40,255))
for x in range(5,11):
    for y in range(7,9): h.putpixel((x,y),(90,72,30,255))
h.save(T+'tank_hatch.png')
for old in os.listdir(T):
    if old.startswith('fuel_tank_'): os.remove(T+old)
for old in os.listdir(M):
    if old.startswith('fuel_tank_'): os.remove(M+old)

def box(fr,to,tex,faces=('north','south','east','west','up','down')):
    return {'from':fr,'to':to,'faces':{k:{'texture':tex} for k in faces}}
TEX={'particle':'car:block/tank_frame','frame':'car:block/tank_frame','glass':'car:block/tank_glass','lid':'car:block/tank_lid','hatch':'car:block/tank_hatch'}
parts=[]  # (when, elements)
# glass panes per side
P=0.5
parts.append(({'north':'false'},[box([0,0,P],[16,16,P],'#glass',('north',))]))
parts.append(({'south':'false'},[box([0,0,16-P],[16,16,16-P],'#glass',('south',))]))
parts.append(({'west':'false'},[box([P,0,0],[P,16,16],'#glass',('west',))]))
parts.append(({'east':'false'},[box([16-P,0,0],[16-P,16,16],'#glass',('east',))]))
# vertical posts
for (a,b,x0,z0) in [('north','west',0,0),('north','east',14,0),('south','west',0,14),('south','east',14,14)]:
    parts.append(({a:'false',b:'false'},[box([x0,0,z0],[x0+2,16,z0+2],'#frame')]))
# rims
for v,(y0,y1) in [('up',(14,16)),('down',(0,2))]:
    parts.append(({'north':'false',v:'false'},[box([0,y0,0],[16,y1,2],'#frame')]))
    parts.append(({'south':'false',v:'false'},[box([0,y0,14],[16,y1,16],'#frame')]))
    parts.append(({'west':'false',v:'false'},[box([0,y0,0],[2,y1,16],'#frame')]))
    parts.append(({'east':'false',v:'false'},[box([14,y0,0],[16,y1,16],'#frame')]))
# lid and hatch, base plate
parts.append(({'up':'false'},[box([0,14.5,0],[16,15.5,16],'#lid',('up','down')),
    box([5,15.5,5],[11,16.5,11],'#hatch'), box([7,16.5,7],[9,17,9],'#frame')]))
parts.append(({'down':'false'},[box([0,0.5,0],[16,1.5,16],'#lid',('up','down'))]))
ms=[]
inv=[]
for i,(when,els) in enumerate(parts):
    name='tank/part_%d'%i
    os.makedirs(M+'tank',exist_ok=True)
    json.dump({'textures':TEX,'elements':els},open(M+name+'.json','w'),indent=1)
    ms.append({'when':when,'apply':{'model':'car:block/'+name}})
    inv+=els
json.dump({'parent':'minecraft:block/block','textures':TEX,'elements':inv},open(M+'tank/inventory.json','w'),indent=1)
json.dump({'multipart':ms},open(R+'blockstates/fuel_tank.json','w'),indent=1)
json.dump({'parent':'car:block/tank/inventory'},open(R+'models/item/fuel_tank.json','w'),indent=1)
print(len(parts))
