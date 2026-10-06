"""Three-bay regeneration GUI and compact animated recipe card."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_gui.py')
s=source.read_text().replace('water_purifier','filter_regeneration').replace('Water purifier','Filter regeneration').replace('Purifier','Filter regeneration').replace('WATER_PURIFIER','FILTER_REGENERATION')
s=s.replace('well(14,43,16,86,False);well(42,43,28,56,False);well(178,43,28,56,False)', 'well(14,43,16,86,False);well(48,63,24,24,False)')
s=s.replace('well(42,107,24,24,False);well(178,107,24,24,False);well(79,108,91,14,False)', 'well(48,99,24,24,False);well(176,99,24,24,False);well(79,108,91,14,False)')
a=s.index('for x in (70.5,173.5):');b=s.index("box('Inventory caption'",a)
s=s[:a]+s[b:]
s=s.replace('well(13,32,24,47,False);well(143,32,24,47,False);well(46,80,24,24,False);well(76,85,58,12,False)', 'well(13,29,24,24,False);well(13,64,24,24,False);well(143,64,24,24,False);well(49,86,82,14,False)')
exec(compile(s,str(source),'exec'))
