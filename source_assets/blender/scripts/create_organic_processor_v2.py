"""Wide mixing vat and geared drive within the established graphite shell."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_v2.py')
s=source.read_text().replace('water_purifier','organic_processor').replace('Purifier / ','Biosynthesizer / ').replace('WATER_PURIFIER','ORGANIC_PROCESSOR')
a=s.index('# Two open sight vessels:');b=s.index('for x in (1.45,14.1):',a)
s=s[:a]+'''# One broad open-front vat; the paddle rotor is authored separately below.
box('Vat floor',[3.1,3.0,1.45],[12.9,3.65,8.3],'steel')
box('Vat back',[3.1,3.65,8.1],[12.9,9.5,8.3],'frame')
for x in (3.1,12.4):box('Vat cheek',[x,3.65,1.8],[x+.5,9.5,8.1],'steel')
box('Lower sight rim',[3.6,3.65,1.6],[12.4,4.1,2.0],'trim')
box('Upper sight rim',[3.1,9.15,1.55],[12.9,9.6,2.0],'trim')
for x in (3.45,12.2):box('Sight edge',[x,4.1,1.6],[x+.35,9.15,2.0],'trim')
box('Drive mounting bridge',[3.0,10.1,3.5],[13.0,10.65,6.4],'frame')
box('Geared motor',[6.25,10.65,3.4],[9.75,12.9,6.8],'body')
for y in (11.05,11.65,12.25):box('Motor cooling rib',[6.05,y,3.25],[9.95,y+.2,6.95],'steel')
box('Lower bearing collar',[7.3,9.4,4.3],[8.7,10.7,5.7],'brass')
box('Upper left dosing duct',[3.4,11.1,2.6],[5.4,12.9,4.4],'trim')
box('Dosing chute',[4.0,9.6,3.2],[4.8,11.1,4.0],'steel')
box('Water inlet',[10.7,10.75,3.8],[11.3,12.95,4.4],'steel')
box('Water inlet elbow',[10.7,10.35,3.8],[12.3,10.95,4.4],'brass')
box('Discharge neck',[10.6,2.2,2.7],[12.0,3.0,4.4],'steel')
box('Product tray',[8.6,1.5,1.0],[12.8,2.1,4.6],'trim')
for x in (3.25,12.4):
    for y in (4.6,8.4):box('Sight clamp',[x,y,1.3],[x+.35,y+.5,1.58],'bolt')
'''+s[b:]
a=s.index("box('Pump hub'");b=s.index('moving=elements',a)
s=s[:a]+'''box('Vertical mixing spindle',[7.7,4.4,4.7],[8.3,10.5,5.3],'steel')
box('Lower paddle beam',[5.25,4.65,4.6],[10.75,5.15,5.4],'brass')
box('Upper paddle beam',[7.6,7.15,2.25],[8.4,7.65,7.75],'steel')
for x in (5.1,10.3):box('Lower mixing blade',[x,4.55,4.05],[x+.6,5.5,5.95],'steel')
for z in (2.1,7.3):box('Upper mixing blade',[7.05,6.9,z],[8.95,7.85,z+.6],'brass')
'''+s[b:]
s=s.replace('organic_processor_lit','organic_processor_active').replace('organic_processor_pump','organic_processor_rotor')
# Existing organic multipart has the right facing/active semantics; only replace socket artwork.
exec(compile(s,str(source),'exec'))
