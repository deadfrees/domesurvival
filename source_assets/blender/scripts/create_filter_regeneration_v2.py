"""Filter cleaning cabinet in the shared graphite machine family.

Minecraft dimensions are sixteenths. The cleaning carriage translates on Y
by 0..4/16 blocks; keep it in front of the static pleated cassette.
"""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_v2.py')
s=source.read_text().replace('water_purifier','filter_regeneration_station').replace('Purifier / ','Regenerator / ').replace('WATER_PURIFIER','FILTER_REGENERATION')
a=s.index('# Two open sight vessels:');b=s.index('for x in (1.45,14.1):',a)
s=s[:a]+'''# Deep removable pleated cartridge, captured by a pair of service rails.
box('Cassette backing',[4.15,3.5,6.2],[11.85,11.9,7.4],'frame')
for x in (4.15,11.45):box('Cassette side frame',[x,3.5,4.8],[x+.4,11.9,6.2],'trim')
for y in (3.5,11.5):box('Cassette end frame',[4.55,y,4.8],[11.45,y+.4,6.2],'trim')
# Broad horizontal warm pleats read as a removable filter, unlike the
# electrolyzer's tall silver electrodes and open liquid bath.
for y in (4.0,4.8,5.6,6.4,7.2,8.0,8.8,9.6,10.4):
    box('Horizontal filter fold',[4.6,y,4.65],[11.4,y+.45,6.2],'brass')
    box('Recessed fold shadow',[4.6,y+.45,5.35],[11.4,y+.72,6.2],'body')
box('Cassette central retaining strap',[7.7,3.9,4.40],[8.3,11.3,4.64],'trim')
for x in (4.15,11.45):
    box('Cassette release catch',[x-.12,10.5,4.1],[x+.52,11.3,4.79],'steel')
box('Cassette pull handle feet',[6.6,11.55,4.0],[9.4,12.0,4.79],'black')
box('Raised cassette pull handle',[6.8,11.65,3.6],[9.2,12.1,3.99],'steel')
for x in (3.25,12.25):
    box('Guide rail seat',[x-.25,3.0,3.8],[x+.75,12.5,4.6],'black')
    box('Polished carriage guide',[x,3.15,3.35],[x+.45,12.35,3.85],'steel')
    for y in (3.15,11.8):box('Guide end clamp',[x-.18,y,3.15],[x+.62,y+.55,4.1],'brass')
box('Upper cleaning feed',[3.7,12.25,3.4],[12.3,12.65,4.2],'trim')
box('Lower collection trough',[3.7,2.9,2.9],[12.3,3.4,6.4],'frame')
box('Trough rim',[3.7,3.4,2.9],[12.3,3.65,3.3],'trim')
box('Dust drawer fascia',[3.25,1.15,.45],[12.75,2.2,.9],'body')
box('Dust drawer handle',[6.5,1.48,.15],[9.5,1.85,.44],'trim')
for x in (3.7,11.9):box('Drawer fastener',[x,1.45,.29],[x+.32,1.8,.44],'bolt')
'''+s[b:]
a=s.index("box('Pump hub'");b=s.index('moving=elements',a)
s=s[:a]+'''box('Cleaning carriage',[3.05,4.5,2.8],[12.95,5.4,3.2],'frame')
box('Carriage polished edge',[3.45,5.22,2.64],[12.55,5.42,3.2],'steel')
box('Suction manifold',[4.1,4.58,2.48],[11.9,5.1,2.79],'brass')
for x in (4.45,5.5,6.55,7.6,8.65,9.7,10.75):
    box('Cleaning nozzle',[x,4.62,3.21],[x+.4,5.03,4.72],'trim')
for x in (3.15,12.45):box('Carriage fastener',[x,4.72,2.55],[x+.38,5.1,2.79],'bolt')
'''+s[b:]
s=s.replace('filter_regeneration_station_lit','filter_regeneration_station_active').replace('filter_regeneration_station_pump','filter_regeneration_station_carriage')
a=s.index("state=json.loads(");b=s.index('# Same studio',a)
s=s[:a]+'''state=json.loads((A/'blockstates/water_purifier.json').read_text())
for part in state['multipart']:
    when=part.get('when',{})
    if 'lit' in when:when['active']=when.pop('lit')
    part['apply']['model']=part['apply']['model'].replace('water_purifier_lit','filter_regeneration_station_active').replace('water_purifier','filter_regeneration_station').replace('block/machine_input_port_','block/coal_generator_input_port_').replace('block/machine_output_port_','block/coal_generator_output_port_')
(A/'blockstates/filter_regeneration_station.json').write_text(json.dumps(state,indent=2)+'\\n')
'''+s[b:]
exec(compile(s,str(source),'exec'))
