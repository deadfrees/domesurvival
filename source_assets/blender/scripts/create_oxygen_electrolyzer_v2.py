"""Electrode bath, bus bars and gas collector in the approved graphite enclosure."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_v2.py')
s=source.read_text().replace('water_purifier','oxygen_electrolyzer').replace('Purifier / ','Electrolyzer / ').replace('WATER_PURIFIER','OXYGEN_ELECTROLYZER')
a=s.index('# Two open sight vessels:');b=s.index('for x in (1.45,14.1):',a)
s=s[:a]+'''# A deep rectangular electrolysis bath with two electrode banks.
box('Bath rear lining',[3.1,3.0,7.7],[12.9,11.5,8.25],'black')
box('Bath floor',[3.0,2.7,2.2],[13.0,3.3,7.8],'trim')
for x in (3.0,12.5):
    box('Bath side frame',[x,3.3,2.2],[x+.5,11.8,7.8],'frame')
    box('Sight edge',[x+.12,3.5,2.05],[x+.30,11.6,2.25],'steel')
for x in (4.0,5.4,6.8,8.2,9.6,11.0):
    box('Electrode terminal',[x,11.0,4.1],[x+.55,12.3,4.65],'brass')
    box('Ceramic terminal sleeve',[x-.15,10.6,3.95],[x+.70,11.15,4.8],'steel')
    box('Immersed electrode',[x,3.8,4.2],[x+.55,10.6,6.8],'trim')
    box('Electrode bright edge',[x+.05,4.0,4.10],[x+.18,10.35,4.19],'steel')
box('Negative busbar',[3.65,12.3,4.0],[7.55,12.7,4.8],'steel')
box('Positive busbar',[8.0,12.3,4.0],[12.0,12.7,4.8],'brass')
box('Gas collector gasket',[3.0,11.8,2.3],[13.0,12.1,3.7],'black')
box('Gas collection manifold',[3.25,12.1,2.4],[12.75,12.85,3.5],'trim')
box('Collector return riser',[11.9,12.7,2.65],[12.5,13.4,6.6],'steel')
for y in (4.0,5.5,7.0,8.5,10.0):
    box('Sight level mark',[3.5,y,2.12],[4.1,y+.11,2.21],'steel')
box('Lower electrical cabinet',[4.0,1.05,.4],[12.0,2.35,.58],'frame')
for x in (4.4,5.25,6.1,6.95,7.8,8.65,9.5,10.35,11.2):
    box('Power stage vent',[x,1.35,.23],[x+.38,2.05,.39],'trim')
'''+s[b:]
a=s.index("box('Pump hub'");b=s.index('moving=elements',a)
s=s[:a]+'''for x,y in ((4.6,0.0),(6.0,1.9),(7.4,3.8),(8.8,1.0),(10.2,3.0),(11.6,5.3)):
    box('Oxygen bubble',[x,4.0+y,2.75],[x+.26,4.26+y,3.01],'steel')
'''+s[b:]
s=s.replace('oxygen_electrolyzer_pump','oxygen_electrolyzer_bubbles')
# Bubble meshes are dynamic and absent from the inventory static model.
s=s.replace("('oxygen_electrolyzer_inventory',body+moving)","('oxygen_electrolyzer_inventory',body)")
exec(compile(s,str(source),'exec'))
