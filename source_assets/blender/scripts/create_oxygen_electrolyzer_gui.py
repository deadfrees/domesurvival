"""Graphite control panel for a liquid-to-gas process, without item slots."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_gui.py')
s=source.read_text().replace('water_purifier','oxygen_electrolyzer').replace('Water purifier','Electrolyzer').replace('Purifier','Electrolyzer').replace('WATER_PURIFIER','OXYGEN_ELECTROLYZER')
s=s.replace('well(42,107,24,24,False);well(178,107,24,24,False);','')
s=s.replace('well(46,80,24,24,False);well(76,85,58,12,False)','well(49,85,82,12,False)')
exec(compile(s,str(source),'exec'))
