"""Internal tabs, two ingredients, purified water and a compact model preview."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_gui.py')
s=source.read_text().replace('water_purifier','organic_processor').replace('Water purifier','Biosynthesizer').replace('Purifier','Biosynthesizer').replace('WATER_PURIFIER','ORGANIC_PROCESSOR')
s=s.replace('well(14,43,16,86,False);well(42,43,28,56,False);well(178,43,28,56,False)', 'well(14,43,16,86,False);well(38,43,20,52,False);well(66,45,24,24,False);well(66,77,24,24,False)')
s=s.replace('well(42,107,24,24,False);well(178,107,24,24,False);well(79,108,91,14,False)', 'well(178,77,24,24,False);well(66,112,136,12,False)')
a=s.index('for x in (70.5,173.5):');b=s.index("box('Inventory caption'",a);s=s[:a]+s[b:]
s=s.replace('well(13,32,24,47,False);well(143,32,24,47,False);well(46,80,24,24,False);well(76,85,58,12,False)', 'well(13,29,24,24,False);well(13,62,24,24,False);well(43,31,20,50,False);well(143,62,24,24,False);well(72,87,95,12,False)')
exec(compile(s,str(source),'exec'))
