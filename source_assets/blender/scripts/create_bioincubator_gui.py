"""Two incubation modes with internal side/module tabs."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_gui.py')
s=source.read_text().replace('water_purifier','bioincubator').replace('Water purifier','Bioincubator').replace('Purifier','Incubator').replace('WATER_PURIFIER','BIOINCUBATOR')
s=s.replace('well(14,43,16,86,False);well(42,43,28,56,False);well(178,43,28,56,False)', 'well(14,43,16,86,False);well(38,43,20,52,False)')
s=s.replace('well(42,107,24,24,False);well(178,107,24,24,False);well(79,108,91,14,False)', 'well(66,94,24,24,False);well(94,94,24,24,False);well(66,123,140,10,False)')
a=s.index('for x in (70.5,173.5):');b=s.index("box('Inventory caption'",a);s=s[:a]+s[b:]
# Incubation shows two slots. The repair overlay provides three additional slots.
s=s.replace("render(main,'panel')", "render(main,'panel')\nwell(122,94,24,24,False);well(150,94,24,24,False);well(182,94,24,24,False)\nrender(main,'repair')")
s=s.replace('well(13,32,24,47,False);well(143,32,24,47,False);well(46,80,24,24,False);well(76,85,58,12,False)', 'well(10,77,24,24,False);well(38,77,24,24,False);well(10,28,20,44,False);well(42,62,128,10,False)')
s=s.replace("render(card,'jei')", "well(146,77,24,24,False)\nrender(card,'jei')\nwell(66,77,24,24,False);well(94,77,24,24,False)\nrender(card,'jei_repair')")
exec(compile(s,str(source),'exec'))
