"""Incubation chamber with a voxel double helix in the graphite machine family."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_v2.py')
s=source.read_text().replace('water_purifier','bioincubator').replace('Purifier / ','Incubator / ').replace('WATER_PURIFIER','BIOINCUBATOR')
a=s.index('# Two open sight vessels:');b=s.index('for x in (1.45,14.1):',a)
s=s[:a]+'''# One protected incubation chamber, with a clear view of the DNA.
box('Chamber back',[3,3,8.1],[13,13.2,8.3],'black')
box('Incubation pedestal',[4,2.9,2],[12,3.6,7.6],'steel')
box('Pedestal lining',[4.4,3.6,2.4],[11.6,3.85,7.2],'frame')
box('Pedestal band',[4,3.1,1.85],[12,3.35,2],'brass')
box('Upper chamber holder',[5,12.2,3],[11,12.9,7],'steel')
box('Holder band',[5.4,12.35,2.85],[10.6,12.6,3],'brass')
for x in (2.8,12.85):box('Window edge',[x,3,1.35],[x+.35,13.1,1.75],'trim')
box('Window crown',[2.8,12.8,1.35],[13.2,13.15,1.75],'trim')
box('Low entry threshold',[3.2,2.5,1.25],[12.8,2.9,2],'trim')
box('Sample reader',[6.4,2.9,1.05],[9.6,3.2,1.85],'frame')
'''+s[b:]
a=s.index("box('Pump hub'");b=s.index('moving=elements',a)
s=s[:a]+'''# Connected voxel strands and six stepped base-pair links. Pivot (8, y, 4.8).
import math
for i in range(36):
    angle=math.radians(20)+math.tau*i/35
    y=4.2+7*i/35
    for phase,role in ((0,'input'),(math.pi,'steel')):
        x=8+2.35*math.cos(angle+phase);z=4.8+2.35*math.sin(angle+phase)
        box('DNA strand',[x-.325,y-.325,z-.325],[x+.325,y+.325,z+.325],role)
    if i%7==0:
        for j in range(1,8):
            t=-1+j/4;x=8+2.35*t*math.cos(angle);z=4.8+2.35*t*math.sin(angle)
            box('DNA base pair',[x-.35,y-.11,z-.35],[x+.35,y+.11,z+.35],'brass')
'''+s[b:]
s=s.replace('bioincubator_pump','bioincubator_dna')
exec(compile(s,str(source),'exec'))
state_path=A/'blockstates/bioincubator.json'
state=json.loads(state_path.read_text())
for part in state['multipart']:
    part['apply']['model']=part['apply']['model'].replace('bioincubator_input_port_','coal_generator_input_port_').replace('bioincubator_output_port_','coal_generator_output_port_')
state_path.write_text(json.dumps(state,indent=2)+'\n')
