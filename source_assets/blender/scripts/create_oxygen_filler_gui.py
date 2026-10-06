"""Matched graphite controls with pictogram mode buttons and two item bays."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_gui.py')
s=source.read_text().replace('water_purifier','oxygen_filler').replace('Water purifier','Oxygen filler').replace('Purifier','Filler').replace('WATER_PURIFIER','OXYGEN_FILLER')
s=s.replace('well(14,43,16,86,False);well(42,43,28,56,False);well(178,43,28,56,False)', 'well(14,43,16,86,False);well(46,54,28,44,False)')
s=s.replace('well(42,107,24,24,False);well(178,107,24,24,False);well(79,108,91,14,False)', 'well(48,99,24,24,False);well(176,99,24,24,False);well(79,108,91,14,False)')
s=s.replace("for x in (70.5,173.5):", "for x in (76.5,):").replace('range(47,96,8)','range(57,94,8)')
s=s.replace("render(main,'panel')", """render(main,'panel')
vent=main.copy();vent.name='Filler ventilation controls'
bpy.context.window.scene=vent
# Unlink the tank-mode wells from this scene rather than covering them with
# another empty raised panel. The shared control face remains continuous.
for obj in list(vent.objects):
    if obj.type=='MESH' and 42 <= obj.location.x <= 206 and 99 <= 266-obj.location.y <= 128:
        vent.collection.objects.unlink(obj)
well(178,54,28,44,False)
render(vent,'ventilation')""")
s=s.replace('well(13,32,24,47,False);well(143,32,24,47,False);well(46,80,24,24,False);well(76,85,58,12,False)', 'well(13,48,24,24,False);well(143,48,24,24,False);well(49,85,82,12,False)')
exec(compile(s,str(source),'exec'))
