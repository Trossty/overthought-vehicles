import json, random
from PIL import Image
R='/Users/egementopcu/IdeaProjects/car/src/main/resources/assets/car/'
T=R+'textures/block/'; M=R+'models/block/'
random.seed(3)
def noisy(base,n=5,name=None,fn=None):
    im=Image.new('RGBA',(16,16))
    for x in range(16):
        for y in range(16):
            d=random.randint(-n,n); c=[max(0,min(255,b+d)) for b in base]
            if fn: c=fn(x,y,c)
            im.putpixel((x,y),tuple(c)+(255,))
    im.save(T+name+'.png')
noisy((58,63,69),4,'shop_steel')
noisy((176,38,30),4,'shop_red',lambda x,y,c:[int(v*0.8) for v in c] if y in (0,15) else c)
noisy((182,188,194),3,'shop_chrome',lambda x,y,c:[min(255,v+45) for v in c] if x in (5,6) else ([v-40 for v in c] if x in (12,13) else c))
noisy((28,28,30),3,'shop_rubber')
noisy((0,0,0),0,'shop_hazard',lambda x,y,c:[230,180,30] if ((x+y)//4)%2==0 else [30,30,32])
def wood(x,y,c):
    base=[150,104,62] if (y//4)%2==0 else [138,95,56]
    if y%4==3: base=[100,68,40]
    if (x+ (y//4)*5)%16==0: base=[110,76,45]
    return [b+random.randint(-6,6) for b in base]
noisy((0,0,0),0,'shop_wood',wood)

def el(fr,to,tex,uv=None,faces=('north','south','east','west','up','down'),rot=None,full=()):
    f={}
    for k in faces:
        f[k]={'texture':tex}
        if k in full: f[k]['uv']=[0,0,16,16]
    e={'from':fr,'to':to,'faces':f}
    if rot: e['rotation']=rot
    return e
def save(name,textures,els,parent='minecraft:block/block'):
    json.dump({'parent':parent,'textures':textures,'elements':els},open(M+name+'.json','w'),indent=1)

TX={'particle':'car:block/shop_red','steel':'car:block/shop_steel','red':'car:block/shop_red','chrome':'car:block/shop_chrome',
    'rubber':'car:block/shop_rubber','hazard':'car:block/shop_hazard','wood':'car:block/shop_wood',
    'drawers':'car:block/vehicle_workstation_front','screen':'car:block/vehicle_workstation_top','tread':'car:block/vehicle_lift_top',
    'pit':'car:block/vehicle_lift_side','bottom':'car:block/vehicle_lift_bottom'}

# ---- workbench ----
wb=[el([0,13,0],[16,16,16],'#wood')]
for x,z in [(0.5,0.5),(13.5,0.5),(0.5,13.5),(13.5,13.5)]:
    wb.append(el([x,0,z],[x+2,13,z+2],'#red'))
wb.append(el([1,2,1],[15,3,15],'#steel'))
wb.append(el([8,3,1.5],[15,13,14.5],'#steel',full=('north','south')))
wb[-1]['faces']['north']['texture']='#drawers'; wb[-1]['faces']['south']['texture']='#drawers'
# vise
wb+= [el([10.5,16,1.5],[15,17,6],'#steel'), el([11,17,2],[12.2,19.5,5.5],'#steel'), el([13.3,17,2],[14.5,19.5,5.5],'#steel'),
      el([12.2,17.5,0.5],[13.3,18.5,2],'#chrome'), el([10.5,17.7,0],[15,18.3,0.6],'#chrome')]
# socket tray and sockets
wb.append(el([2,16,9.5],[8,16.6,14.5],'#red'))
for i in range(4):
    x=2.5+i*1.4
    wb.append(el([x,16.6,10.5],[x+1,17.6,11.5],'#chrome'))
    wb.append(el([x,16.6,12.5],[x+1,17.2,13.5],'#chrome'))
# spanner
wb+= [el([2,16,3.5],[8,16.4,4.5],'#chrome'), el([7.5,16,2.8],[9.5,16.4,5.2],'#chrome')]
save('vehicle_workbench',dict(TX,particle='car:block/shop_wood'),wb)

# ---- workstation (front = north) ----
ws=[]
for x,z in [(1.5,2.5),(12.5,2.5),(1.5,12.5),(12.5,12.5)]:
    ws.append(el([x,0,z],[x+2,2,z+2],'#rubber'))
ws.append(el([1,2,2],[15,13,15],'#red',full=('north',)))
ws[-1]['faces']['north']['texture']='#drawers'
ws.append(el([0.5,13,1.5],[15.5,14,15.5],'#steel'))
ws+= [el([15,8,5],[16,9,12],'#chrome'), el([0,8,5],[1,9,12],'#chrome')]
ws+= [el([4,14,3],[12,14.5,6.5],'#steel'), el([6,14,9],[10,14.5,13],'#steel'), el([7.5,14.5,11],[8.5,17.5,12],'#steel')]
scr=el([2,16,10],[14,23,11],'#steel',rot={'origin':[8,16,10.5],'axis':'x','angle':22.5},full=('north',))
scr['faces']['north']['texture']='#screen'
ws.append(scr)
save('vehicle_workstation',dict(TX,particle='car:block/shop_red'),ws)

print('ok')
