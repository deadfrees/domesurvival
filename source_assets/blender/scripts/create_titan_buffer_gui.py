"""Titan buffer instrument panel with an internal side-configuration page."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_gui.py')
s=source.read_text().replace('water_purifier','titan_buffer').replace('Water purifier','Titan buffer').replace('Purifier','Titan buffer').replace('WATER_PURIFIER','TITAN_BUFFER')
s=s.replace("box('Title gasket',8,6,156,16,3.5,'black',.7)","box('Title gasket',8,6,178,16,3.5,'black',.7)")
s=s.replace("box('Title plate',9,7,154,14,3.8,'case',.5)","box('Title plate',9,7,176,14,3.8,'case',.5)")
a=s.index('well(14,43');b=s.index("box('Inventory caption'",a)
s=s[:a]+'''well(16,44,128,14,False)
well(174,43,24,24,False)
'''+s[b:]
a=s.index('modules=setup(');b=s.index('bpy.context.window.scene=main',a);s=s[:a]+s[b:]
exec(compile(s,str(source),'exec'))
